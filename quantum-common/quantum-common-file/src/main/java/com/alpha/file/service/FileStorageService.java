package com.alpha.file.service;

import com.alpha.file.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储服务接口
 * <p>
 * 支持多种存储实现（本地、MinIO、OSS 等）
 */
public interface FileStorageService {

    /**
     * 获取存储类型标识
     */
    String getStorageType();

    /**
     * 上传文件
     *
     * @param file 文件
     * @param path 存储路径（可选）
     * @return 文件信息
     */
    FileInfo upload(MultipartFile file, String path);

    /**
     * 上传文件（指定文件名）
     *
     * @param file     文件
     * @param path     存储路径
     * @param fileName 文件名
     * @return 文件信息
     */
    FileInfo upload(MultipartFile file, String path, String fileName);

    /**
     * 批量上传
     *
     * @param files 文件列表
     * @param path  存储路径
     * @return 文件信息列表
     */
    List<FileInfo> uploadBatch(List<MultipartFile> files, String path);

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
     * 批量删除
     *
     * @param filePaths 文件路径列表
     * @return 成功删除的数量
     */
    int deleteBatch(List<String> filePaths);

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    boolean exists(String filePath);

    /**
     * 获取文件访问 URL
     *
     * @param filePath 文件路径
     * @return 访问 URL
     */
    String getUrl(String filePath);

    /**
     * 获取临时访问 URL（带签名）
     *
     * @param filePath      文件路径
     * @param expireSeconds 有效期（秒）
     * @return 临时 URL
     */
    String getPresignedUrl(String filePath, int expireSeconds);
}