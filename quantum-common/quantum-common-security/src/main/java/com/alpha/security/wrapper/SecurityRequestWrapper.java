package com.alpha.security.wrapper;

import com.alpha.framework.util.XssCleaner;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.MediaType;
import org.springframework.http.InvalidMediaTypeException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 安全防护核心请求包装器
 * <p>
 * 功能合集：
 * 1. XSS 清洗 (针对 Parameter 和 Header)
 * 2. Body 可重复读 (针对 JSON 请求)
 */
public class SecurityRequestWrapper extends HttpServletRequestWrapper {

    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;

    /**
     * 缓存的请求体 (仅 JSON 请求会初始化)
     */
    private final byte[] body;

    /**
     * 是否开启 XSS 过滤
     */
    private final boolean xssEnabled;

    public SecurityRequestWrapper(HttpServletRequest request, boolean xssEnabled) throws IOException {
        super(request);
        this.xssEnabled = xssEnabled;

        // 只有 JSON 请求才缓存 Body (避免上传文件等大请求占用内存)
        if (isJsonRequest(request)) {
            if (request.getContentLengthLong() > MAX_JSON_BYTES) {
                throw new JsonBodyTooLargeException();
            }
            this.body = request.getInputStream().readNBytes(MAX_JSON_BYTES + 1);
            if (body.length > MAX_JSON_BYTES) {
                throw new JsonBodyTooLargeException();
            }
        } else {
            this.body = null;
        }
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        try {
            var mediaType = MediaType.parseMediaType(contentType);
            return "application".equals(mediaType.getType())
                    && ("json".equals(mediaType.getSubtype()) || mediaType.getSubtype().endsWith("+json"));
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    // ==================== 1. Body 可重复读逻辑 ====================

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 如果缓存了 Body，从缓存读
        if (body != null) {
            final ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }
        // 否则直接返回原始流
        return super.getInputStream();
    }

    public String getBodyString() {
        // 显式 UTF-8：默认平台字符集在不同部署环境下不一致，会导致防重复提交的 body MD5 不稳定
        return body != null ? new String(body, java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return xssEnabled ? XssCleaner.clean(name, value) : value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values != null && xssEnabled) {
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = XssCleaner.clean(name, values[i]);
            }
            return cleanValues;
        }
        return values;
    }

    public static final class JsonBodyTooLargeException extends IOException {
        private static final long serialVersionUID = 1L;
    }
}
