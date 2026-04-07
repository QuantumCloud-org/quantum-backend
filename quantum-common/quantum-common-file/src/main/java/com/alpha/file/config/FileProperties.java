package com.alpha.file.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文件存储配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /**
     * 存储类型：local / rustfs
     */
    private String storageType = "local";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * RustFS（S3 兼容）配置
     */
    private Rustfs rustfs = new Rustfs();

    /**
     * 上传配置
     */
    private Upload upload = new Upload();

    @PostConstruct
    public void validate() {
        if ("rustfs".equalsIgnoreCase(storageType)) {
            if (rustfs.getAccessKey() == null || rustfs.getAccessKey().isBlank()) {
                throw new IllegalStateException("file.rustfs.access-key 未配置");
            }
            if (rustfs.getSecretKey() == null || rustfs.getSecretKey().isBlank()) {
                throw new IllegalStateException("file.rustfs.secret-key 未配置");
            }
        }
    }

    @Data
    public static class Local {
        /**
         * 存储路径
         */
        private String basePath = "/data/files";

        /**
         * 访问域名
         */
        private String domain = "http://localhost:8080/api/file";
    }

    @Data
    public static class Rustfs {
        /**
         * 服务地址
         */
        private String endpoint = "http://localhost:9000";

        /**
         * Access Key
         */
        private String accessKey;

        /**
         * Secret Key
         */
        private String secretKey;

        /**
         * 默认 Bucket
         */
        private String bucket = "quantum";

        /**
         * 区域
         */
        private String region = "cn-north-1";

        /**
         * 是否使用路径样式访问（true: path-style, false: virtual-hosted-style）
         */
        private boolean pathStyleAccess = true;
    }

    @Data
    public static class Upload {
        /**
         * 最大文件大小（MB）
         */
        private int maxSize = 100;

        /**
         * 允许的文件扩展名
         */
        private List<String> allowedExtensions = Arrays.asList(
                // 图片
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico",
                // 文档
                "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md",
                // 压缩包
                "zip", "rar", "7z", "tar", "gz",
                // 视频
                "mp4", "avi", "mov", "wmv", "flv", "mkv",
                // 音频
                "mp3", "wav", "flac", "aac"
        );

        /**
         * 禁止的文件扩展名
         */
        private List<String> forbiddenExtensions = Arrays.asList(
                "exe", "bat", "cmd", "sh", "php", "jsp", "asp", "aspx", "dll", "so"
        );
    }
}
