package com.alpha.file.service;

import com.alpha.file.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务接口
 * <p>
 * 支持多种存储实现（本地、MinIO、OSS 等）
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param path 存储路径（可选）
     * @return 文件信息
     */
    FileInfo upload(MultipartFile file, String path);


    /**
     * 下载文件
     *
     * @param filePath 文件路径
     * @return 文件流
     */
    InputStream download(String filePath);

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否成功
     */
    boolean delete(String filePath);

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    boolean exists(String filePath);
}