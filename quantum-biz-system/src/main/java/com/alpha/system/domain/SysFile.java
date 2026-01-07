package com.alpha.system.domain;

import com.alpha.orm.entity.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息实体（数据库存储）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_file")
public class SysFile extends BaseEntity implements Serializable {
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
     * 访问 URL
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long size;

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
     * 存储类型（local-本地 minio-MinIO oss-阿里云）
     */
    private String storageType;

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务 ID
     */
    private Long bizId;

    /**
     * 上传人 ID
     */
    private Long uploadBy;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
}