package com.alpha.file.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.file.config.FileProperties;
import com.alpha.framework.exception.BizException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件工具类
 */
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的字符串，如 "1.5 MB"
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 校验上传文件
     *
     * @param file       上传的文件
     * @param properties 文件配置
     * @throws BizException 校验失败时抛出异常
     */
    public static void validateFile(MultipartFile file, FileProperties properties) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }

        // 文件大小校验
        long maxSize = properties.getUpload().getMaxSize() * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            throw new BizException("文件大小超过限制（最大 " + properties.getUpload().getMaxSize() + "MB）");
        }

        // 扩展名校验
        String extension = FileUtil.extName(file.getOriginalFilename());
        if (StrUtil.isBlank(extension)) {
            throw new BizException("无法识别文件类型");
        }

        // 禁止的扩展名
        if (properties.getUpload().getForbiddenExtensions().contains(extension.toLowerCase())) {
            throw new BizException("不允许上传该类型文件");
        }

        // 允许的扩展名（如果配置了白名单）
        List<String> allowedExtensions = properties.getUpload().getAllowedExtensions();
        if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(extension.toLowerCase())) {
            throw new BizException("不支持的文件类型");
        }
    }

    /**
     * 获取文件扩展名（小写）
     *
     * @param filename 文件名
     * @return 扩展名（不含点号），如 "jpg"
     */
    public static String getExtension(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "";
        }
        return FileUtil.extName(filename).toLowerCase();
    }

    /**
     * 判断是否为图片文件
     *
     * @param extension 扩展名
     * @return 是否为图片
     */
    public static boolean isImage(String extension) {
        if (StrUtil.isBlank(extension)) {
            return false;
        }
        return List.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico")
                .contains(extension.toLowerCase());
    }

    /**
     * 判断是否为文档文件
     *
     * @param extension 扩展名
     * @return 是否为文档
     */
    public static boolean isDocument(String extension) {
        if (StrUtil.isBlank(extension)) {
            return false;
        }
        return List.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md")
                .contains(extension.toLowerCase());
    }

    /**
     * 判断是否为视频文件
     *
     * @param extension 扩展名
     * @return 是否为视频
     */
    public static boolean isVideo(String extension) {
        if (StrUtil.isBlank(extension)) {
            return false;
        }
        return List.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm")
                .contains(extension.toLowerCase());
    }

    /**
     * 判断是否为音频文件
     *
     * @param extension 扩展名
     * @return 是否为音频
     */
    public static boolean isAudio(String extension) {
        if (StrUtil.isBlank(extension)) {
            return false;
        }
        return List.of("mp3", "wav", "flac", "aac", "ogg", "wma")
                .contains(extension.toLowerCase());
    }

    /**
     * 获取文件的 MIME 类型
     *
     * @param extension 扩展名
     * @return MIME 类型
     */
    public static String getMimeType(String extension) {
        if (StrUtil.isBlank(extension)) {
            return "application/octet-stream";
        }

        return switch (extension.toLowerCase()) {
            // 图片
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            // 文档
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            // 压缩包
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "7z" -> "application/x-7z-compressed";
            case "tar" -> "application/x-tar";
            case "gz" -> "application/gzip";
            // 视频
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "wmv" -> "video/x-ms-wmv";
            case "flv" -> "video/x-flv";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            // 音频
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "flac" -> "audio/flac";
            case "aac" -> "audio/aac";
            case "ogg" -> "audio/ogg";
            // 默认
            default -> "application/octet-stream";
        };
    }
}