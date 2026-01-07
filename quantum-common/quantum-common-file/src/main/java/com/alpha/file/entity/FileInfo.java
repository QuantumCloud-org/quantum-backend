package com.alpha.file.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息
 */
@Data
@Accessors(chain = true)
public class FileInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storageName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 完整访问 URL
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 文件大小（格式化，如 "1.5 MB"）
     */
    private String sizeFormatted;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * MIME 类型
     */
    private String contentType;

    /**
     * 文件 MD5
     */
    private String md5;

    /**
     * 存储类型：local / minio / oss
     */
    private String storageType;

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传人 ID
     */
    private Long uploadBy;
}