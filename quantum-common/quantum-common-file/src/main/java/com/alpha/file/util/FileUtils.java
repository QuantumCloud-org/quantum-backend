package com.alpha.file.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.file.config.FileProperties;
import com.alpha.framework.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文件工具类
 */
@Slf4j
public final class FileUtils {

    private static final byte[] WEBP_RIFF_PREFIX = new byte[]{0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_BRAND = new byte[]{0x57, 0x45, 0x42, 0x50};

    /**
     * 文件魔术字节映射表（扩展名 → 文件头字节）
     */
    private static final Map<String, byte[]> MAGIC_BYTES = Map.ofEntries(
            Map.entry("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}),
            Map.entry("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
            Map.entry("jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
            Map.entry("gif", new byte[]{0x47, 0x49, 0x46, 0x38}),
            Map.entry("pdf", new byte[]{0x25, 0x50, 0x44, 0x46}),
            Map.entry("zip", new byte[]{0x50, 0x4B, 0x03, 0x04}),
            Map.entry("rar", new byte[]{0x52, 0x61, 0x72, 0x21}),
            Map.entry("bmp", new byte[]{0x42, 0x4D})
    );

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

        // 魔术字节校验（防止扩展名伪造）
        validateMagicBytes(file, extension);
    }

    /**
     * 校验文件魔术字节是否与扩展名匹配
     * <p>
     * 读取文件头字节，对比已知文件签名，防止通过修改扩展名绕过类型校验。
     * 仅对 MAGIC_BYTES 中已注册的类型做校验，未注册类型跳过。
     *
     * @param file      上传的文件
     * @param extension 声明的扩展名
     * @throws BizException 魔术字节不匹配时抛出
     */
    public static void validateMagicBytes(MultipartFile file, String extension) {
        if (StrUtil.isBlank(extension) || file == null || file.isEmpty()) {
            return;
        }

        String normalizedExtension = extension.toLowerCase();
        byte[] expectedMagic = MAGIC_BYTES.get(normalizedExtension);
        int headerLength = getRequiredHeaderLength(normalizedExtension, expectedMagic);
        if (headerLength == 0) {
            return;
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[headerLength];
            int bytesRead = is.read(header);
            if (!matchesMagicBytes(normalizedExtension, expectedMagic, header, bytesRead)) {
                log.warn("文件魔术字节不匹配 | 声明扩展名: {} | 实际头字节: {}", extension, bytesToHex(header));
                throw new BizException("文件内容与扩展名不匹配");
            }
        } catch (IOException e) {
            log.error("读取文件头失败: {}", e.getMessage(), e);
            throw new BizException("文件校验失败");
        }
    }

    private static int getRequiredHeaderLength(String extension, byte[] expectedMagic) {
        if ("webp".equals(extension)) {
            return 12;
        }
        return expectedMagic != null ? expectedMagic.length : 0;
    }

    private static boolean matchesMagicBytes(String extension, byte[] expectedMagic, byte[] header, int bytesRead) {
        if ("webp".equals(extension)) {
            return matchesAtOffset(header, bytesRead, WEBP_RIFF_PREFIX, 0)
                    && matchesAtOffset(header, bytesRead, WEBP_BRAND, 8);
        }
        return expectedMagic != null && matchesAtOffset(header, bytesRead, expectedMagic, 0);
    }

    private static boolean matchesAtOffset(byte[] actual, int bytesRead, byte[] expected, int offset) {
        if (bytesRead < offset + expected.length) {
            return false;
        }
        return Arrays.equals(
                Arrays.copyOfRange(actual, offset, offset + expected.length),
                expected
        );
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 校验文件路径安全性（防止路径穿越攻击）
     *
     * @param path     用户传入的相对路径
     * @param basePath 允许的基础目录
     * @throws BizException 路径非法时抛出异常
     */
    public static void validatePath(String path, String basePath) {
        if (StrUtil.isBlank(path)) {
            return;
        }
        Path normalizedBasePath = Path.of(basePath).toAbsolutePath().normalize();
        Path normalizedTargetPath = normalizedBasePath.resolve(path).normalize();
        if (!normalizedTargetPath.startsWith(normalizedBasePath)) {
            throw new BizException("非法文件路径");
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
