package com.alpha.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alpha.file.config.FileProperties;
import com.alpha.file.entity.FileInfo;
import com.alpha.file.util.FileUtils;
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
import java.security.DigestInputStream;
import java.util.HexFormat;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file", name = "storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageServiceImpl extends AbstractFileStorageService {

    private final FileProperties fileProperties;

    @Override
    protected FileProperties properties() {
        return fileProperties;
    }

    @Override
    protected void store(MultipartFile file, String directory, FileInfo info) {
        Path target = resolveLocation(info.getFilePath());
        var digest = DigestUtil.digester("MD5").getDigest();
        try {
            Files.createDirectories(target.getParent());
            Path staging = Files.createTempFile(target.getParent(), "upload-", ".part");
            try {
                try (var input = new DigestInputStream(file.getInputStream(), digest)) {
                    Files.copy(input, staging, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(staging, target);
            } finally {
                Files.deleteIfExists(staging);
            }
            info.setMd5(HexFormat.of().formatHex(digest.digest()));
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException("文件上传失败");
        }
        info.setStorageType("local");
        info.setUrl(fileProperties.getLocal().getDomain() + "/" + info.getFilePath());
    }

    @Override
    public InputStream download(String filePath) {
        File file = resolveLocation(filePath).toFile();

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
        String fullPath = resolveLocation(filePath).toString();
        boolean result = FileUtil.del(fullPath);
        if (result) {
            log.info("文件删除成功: {}", fullPath);
        }
        return result;
    }

    @Override
    public boolean exists(String filePath) {
        String fullPath = resolveLocation(filePath).toString();
        return FileUtil.exist(fullPath);
    }

    private Path resolveLocation(String filePath) {
        String base = fileProperties.getLocal().getBasePath();
        FileUtils.validatePath(filePath, base);
        Path root = Paths.get(base).toAbsolutePath().normalize();
        Path target = root.resolve(filePath).normalize();
        Path current = root;
        for (Path segment : root.relativize(target)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw new BizException("非法文件路径");
            }
        }
        return target;
    }
}
