package com.alpha.file.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alpha.file.config.FileProperties;
import com.alpha.file.entity.FileInfo;
import com.alpha.framework.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.time.Duration;
import java.util.HexFormat;
import java.net.URI;

/**
 * RustFS 文件存储服务（S3 兼容）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file", name = "storage-type", havingValue = "rustfs")
public class RustfsFileStorageServiceImpl extends AbstractFileStorageService {

    private final FileProperties fileProperties;

    private volatile S3Client s3Client;

    private S3Client getS3Client() {
        S3Client client = s3Client;
        if (client == null) {
            synchronized (this) {
                client = s3Client;
                if (client == null) {
                    FileProperties.Rustfs rustfs = fileProperties.getRustfs();
                    AwsBasicCredentials credentials = AwsBasicCredentials.create(
                            rustfs.getAccessKey(),
                            rustfs.getSecretKey()
                    );

                    S3ClientBuilder builder = S3Client.builder()
                            .region(Region.of(rustfs.getRegion()))
                            .credentialsProvider(StaticCredentialsProvider.create(credentials))
                            .endpointOverride(URI.create(rustfs.getEndpoint()))
                            .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofMinutes(2))
                                    .apiCallAttemptTimeout(Duration.ofSeconds(45)));

                    if (rustfs.isPathStyleAccess()) {
                        builder.serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build());
                    }

                    client = builder.build();
                    s3Client = client;
                }
            }
        }
        return client;
    }

    @Override
    protected FileProperties properties() {
        return fileProperties;
    }

    @Override
    protected void store(MultipartFile file, String directory, FileInfo info) {
        String bucket = fileProperties.getRustfs().getBucket();
        var digest = DigestUtil.digester("MD5").getDigest();
        ContentStreamProvider streams = ContentStreamProvider.fromInputStreamSupplier(() -> {
            digest.reset();
            try {
                return new DigestInputStream(file.getInputStream(), digest) {
                    @Override
                    public boolean markSupported() {
                        return false;
                    }
                };
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(info.getFilePath())
                    .contentType(info.getContentType())
                    .contentDisposition("attachment")
                    .build();
            getS3Client().putObject(request, RequestBody.fromContentProvider(streams, file.getSize(), info.getContentType()));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BizException("文件上传失败");
        }
        info.setMd5(HexFormat.of().formatHex(digest.digest()));
        info.setStorageType("rustfs");
        info.setBucket(bucket);
        info.setUrl(buildUrl(info.getFilePath()));
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


    /**
     * 构建文件访问 URL
     */
    private String buildUrl(String objectKey) {
        FileProperties.Rustfs rustfs = fileProperties.getRustfs();
        URI endpoint = URI.create(rustfs.getEndpoint());
        var uri = UriComponentsBuilder.fromUri(endpoint);
        if (rustfs.isPathStyleAccess()) {
            uri.pathSegment(rustfs.getBucket());
        } else {
            uri.host(rustfs.getBucket() + "." + endpoint.getHost());
        }
        return uri.pathSegment(objectKey.split("/")).build().encode().toUriString();
    }
}
