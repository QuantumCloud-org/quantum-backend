package com.alpha.file.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alpha.file.config.FileProperties;
import com.alpha.file.entity.FileInfo;
import com.alpha.file.service.FileStorageService;
import com.alpha.file.util.FileUtils;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RustFS 文件存储服务（S3 兼容）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file", name = "storage-type", havingValue = "rustfs")
public class RustfsFileStorageServiceImpl implements FileStorageService {

    private final FileProperties fileProperties;

    private S3Client s3Client;

    private S3Client getS3Client() {
        if (s3Client == null) {
            FileProperties.Rustfs rustfs = fileProperties.getRustfs();
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    rustfs.getAccessKey(),
                    rustfs.getSecretKey()
            );

            S3ClientBuilder builder = S3Client.builder()
                    .region(Region.of(rustfs.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .endpointOverride(URI.create(rustfs.getEndpoint()));

            // 根据配置选择路径样式或虚拟托管样式
            if (rustfs.isPathStyleAccess()) {
                builder.serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());
            }

            s3Client = builder.build();
        }
        return s3Client;
    }

    @Override
    public String getStorageType() {
        return "rustfs";
    }

    @Override
    public FileInfo upload(MultipartFile file, String path) {
        return upload(file, path, null);
    }

    @Override
    public FileInfo upload(MultipartFile file, String path, String fileName) {
        // 1. 校验文件
        FileUtils.validateFile(file, fileProperties);

        FileProperties.Rustfs rustfs = fileProperties.getRustfs();
        String bucket = rustfs.getBucket();

        // 2. 生成存储路径
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String storagePath = StrUtil.isNotBlank(path) ? path : datePath;

        // 3. 生成文件名
        String originalName = file.getOriginalFilename();
        String extension = FileUtil.extName(originalName);
        String storageName = StrUtil.isNotBlank(fileName)
                ? fileName
                : IdUtil.fastSimpleUUID() + "." + extension;

        // 4. 构建完整对象键
        String objectKey = storagePath + "/" + storageName;

        // 5. 上传到 S3
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            getS3Client().putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BizException("文件上传失败: " + e.getMessage());
        }

        // 6. 计算 MD5
        String md5;
        try {
            md5 = DigestUtil.md5Hex(file.getInputStream());
        } catch (Exception e) {
            md5 = null;
        }

        // 7. 构建返回信息
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalName);
        fileInfo.setStorageName(storageName);
        fileInfo.setFilePath(objectKey);
        fileInfo.setUrl(buildUrl(objectKey));
        fileInfo.setSize(file.getSize());
        fileInfo.setSizeFormatted(FileUtils.formatFileSize(file.getSize()));
        fileInfo.setExtension(extension);
        fileInfo.setContentType(file.getContentType());
        fileInfo.setMd5(md5);
        fileInfo.setStorageType("rustfs");
        fileInfo.setBucket(bucket);
        fileInfo.setUploadTime(LocalDateTime.now());
        fileInfo.setUploadBy(UserContext.getUserId());

        log.info("文件上传成功 | Name: {} | Size: {} | Bucket: {} | Key: {}",
                originalName, fileInfo.getSizeFormatted(), bucket, objectKey);

        return fileInfo;
    }

    @Override
    public List<FileInfo> uploadBatch(List<MultipartFile> files, String path) {
        List<FileInfo> result = new ArrayList<>();
        for (MultipartFile file : files) {
            result.add(upload(file, path));
        }
        return result;
    }

    @Override
    public InputStream download(String filePath) {
        FileProperties.Rustfs rustfs = fileProperties.getRustfs();
        String bucket = rustfs.getBucket();

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .build();

            return getS3Client().getObject(getRequest);
        } catch (NoSuchKeyException e) {
            throw new BizException("文件不存在: " + filePath);
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            throw new BizException("文件下载失败");
        }
    }

    @Override
    public boolean delete(String filePath) {
        FileProperties.Rustfs rustfs = fileProperties.getRustfs();
        String bucket = rustfs.getBucket();

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .build();

            getS3Client().deleteObject(deleteRequest);
            log.info("文件删除成功: {}", filePath);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int deleteBatch(List<String> filePaths) {
        int count = 0;
        FileProperties.Rustfs rustfs = fileProperties.getRustfs();
        String bucket = rustfs.getBucket();

        List<ObjectIdentifier> keys = new ArrayList<>();
        for (String path : filePaths) {
            keys.add(ObjectIdentifier.builder().key(path).build());
        }

        try {
            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(keys).build())
                    .build();

            DeleteObjectsResponse response = getS3Client().deleteObjects(deleteRequest);
            count = response.deleted().size();
            log.info("批量删除文件成功: {} 个", count);
        } catch (Exception e) {
            log.error("批量删除文件失败: {}", e.getMessage(), e);
        }

        return count;
    }

    @Override
    public boolean exists(String filePath) {
        FileProperties.Rustfs rustfs = fileProperties.getRustfs();
        String bucket = rustfs.getBucket();

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .build();

            getS3Client().headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("检查文件是否存在失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getUrl(String filePath) {
        return buildUrl(filePath);
    }

    @Override
    public String getPresignedUrl(String filePath, int expireSeconds) {
        FileProperties.Rustfs rustfs = fileProperties.getRustfs();

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(rustfs.getRegion()))
                .endpointOverride(URI.create(rustfs.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(rustfs.getAccessKey(), rustfs.getSecretKey())))
                .build()) {

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofSeconds(expireSeconds))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(rustfs.getBucket())
                            .key(filePath)
                            .build())
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("生成预签名 URL 失败: {}", e.getMessage(), e);
            // 如果生成失败，返回普通 URL
            return getUrl(filePath);
        }
    }

    /**
     * 构建文件访问 URL
     */
    private String buildUrl(String objectKey) {
        FileProperties.Rustfs rustfs = fileProperties.getRustfs();
        String endpoint = rustfs.getEndpoint();
        String bucket = rustfs.getBucket();

        if (rustfs.isPathStyleAccess()) {
            // 路径样式: http://endpoint/bucket/key
            return endpoint + "/" + bucket + "/" + objectKey;
        } else {
            // 虚拟托管样式: http://bucket.endpoint/key
            return endpoint.replace("http://", "http://" + bucket + ".");
        }
    }
}