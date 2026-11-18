package com.alpha.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alpha.file.config.FileProperties;
import com.alpha.file.entity.FileInfo;
import com.alpha.file.service.FileStorageService;
import com.alpha.file.util.FileUtils;
import com.alpha.framework.context.UserContext;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

abstract class AbstractFileStorageService implements FileStorageService {

    private static final DateTimeFormatter UPLOAD_DIRECTORY = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public FileInfo upload(MultipartFile file, String directory) {
        FileUtils.validateFile(file, properties());
        FileUtils.validateObjectKeyParts(directory, null);
        String path = directory == null || directory.isBlank() ? LocalDate.now().format(UPLOAD_DIRECTORY) : directory;
        String name = file.getOriginalFilename();
        String extension = FileUtil.extName(name);
        String storageName = IdUtil.fastSimpleUUID() + "." + extension;
        String detectedType = FileUtils.getMimeType(extension);

        FileInfo info = new FileInfo();
        info.setOriginalName(name);
        info.setStorageName(storageName);
        info.setFilePath(path + "/" + storageName);
        info.setSize(file.getSize());
        info.setSizeFormatted(FileUtils.formatFileSize(file.getSize()));
        info.setExtension(extension);
        info.setContentType(detectedType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : detectedType);
        info.setUploadTime(LocalDateTime.now());
        info.setUploadBy(UserContext.getUserId());
        store(file, path, info);
        return info;
    }

    protected abstract FileProperties properties();

    protected abstract void store(MultipartFile file, String directory, FileInfo info);
}
