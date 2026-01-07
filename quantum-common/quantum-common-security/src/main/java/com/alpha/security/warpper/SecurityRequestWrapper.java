package com.alpha.security.warpper;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 安全防护核心请求包装器
 * <p>
 * 功能合集：
 * 1. XSS 清洗 (针对 Parameter 和 Header)
 * 2. Body 可重复读 (针对 JSON 请求)
 */
public class SecurityRequestWrapper extends HttpServletRequestWrapper {

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
            this.body = StreamUtils.copyToByteArray(request.getInputStream());
        } else {
            this.body = null;
        }
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    // ==================== 1. Body 可重复读逻辑 ====================

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
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
        return body != null ? new String(body) : null;
    }

    // ==================== 2. XSS 清洗逻辑 (参数 & Header) ====================

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return xssEnabled ? cleanXss(value) : value;
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return xssEnabled ? cleanXss(value) : value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values != null && xssEnabled) {
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = cleanXss(values[i]);
            }
            return cleanValues;
        }
        return values;
    }

    private String cleanXss(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return HtmlUtil.cleanHtmlTag(value);
    }
}