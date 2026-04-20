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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 本地文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file", name = "storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final FileProperties fileProperties;

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    public FileInfo upload(MultipartFile file, String path) {
        return upload(file, path, null);
    }

    @Override
    public FileInfo upload(MultipartFile file, String path, String fileName) {
        // 1. 校验文件
        FileUtils.validateFile(file, fileProperties);

        // 2. 生成存储路径（校验路径穿越）
        String basePath = fileProperties.getLocal().getBasePath();
        FileUtils.validatePath(path, basePath);
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String storagePath = StrUtil.isNotBlank(path) ? path : datePath;
        Path directoryPath = Paths.get(basePath, storagePath).normalize();

        // 3. 确保目录存在
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            log.error("创建文件目录失败: {}", directoryPath, e);
            throw new BizException("文件上传失败: 无法创建存储目录");
        }

        // 4. 生成文件名
        String originalName = file.getOriginalFilename();
        String extension = FileUtil.extName(originalName);
        String storageName = StrUtil.isNotBlank(fileName)
                ? fileName
                : IdUtil.fastSimpleUUID() + "." + extension;

        // 5. 保存文件
        Path targetPath = directoryPath.resolve(storageName).normalize();
        String filePath = targetPath.toString();
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new BizException("文件上传失败: " + e.getMessage());
        }

        // 6. 计算 MD5
        String md5 = DigestUtil.md5Hex(targetPath.toFile());

        // 7. 构建返回信息
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalName);
        fileInfo.setStorageName(storageName);
        fileInfo.setFilePath(storagePath + "/" + storageName);
        fileInfo.setUrl(fileProperties.getLocal().getDomain() + "/" + storagePath + "/" + storageName);
        fileInfo.setSize(file.getSize());
        fileInfo.setSizeFormatted(FileUtils.formatFileSize(file.getSize()));
        fileInfo.setExtension(extension);
        fileInfo.setContentType(file.getContentType());
        fileInfo.setMd5(md5);
        fileInfo.setStorageType("local");
        fileInfo.setUploadTime(LocalDateTime.now());
        fileInfo.setUploadBy(UserContext.getUserId());

        log.info("文件上传成功 | Name: {} | Size: {} | Path: {}",
                originalName, fileInfo.getSizeFormatted(), filePath);

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
        FileUtils.validatePath(filePath, fileProperties.getLocal().getBasePath());
        String fullPath = fileProperties.getLocal().getBasePath() + "/" + filePath;
        File file = new File(fullPath);

        if (!file.exists()) {
            throw new BizException("文件不存在");
        }

        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
            throw new BizException("文件下载失败");
        }
    }

    @Override
    public boolean delete(String filePath) {
        FileUtils.validatePath(filePath, fileProperties.getLocal().getBasePath());
        String fullPath = fileProperties.getLocal().getBasePath() + "/" + filePath;
        boolean result = FileUtil.del(fullPath);
        if (result) {
            log.info("文件删除成功: {}", fullPath);
        }
        return result;
    }

    @Override
    public int deleteBatch(List<String> filePaths) {
        int count = 0;
        for (String path : filePaths) {
            if (delete(path)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean exists(String filePath) {
        FileUtils.validatePath(filePath, fileProperties.getLocal().getBasePath());
        String fullPath = fileProperties.getLocal().getBasePath() + "/" + filePath;
        return FileUtil.exist(fullPath);
    }

    @Override
    public String getUrl(String filePath) {
        return fileProperties.getLocal().getDomain() + "/" + filePath;
    }

    @Override
    public String getPresignedUrl(String filePath, int expireSeconds) {
        // 本地存储不支持签名 URL，直接返回普通 URL
        return getUrl(filePath);
    }
}
