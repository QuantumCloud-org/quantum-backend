package com.alpha.logging.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class LogInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 租户 ID（多租户场景）
     */
    private Long tenantId;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * IP 归属地
     */
    private String ipAddress;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 请求 URI
     */
    private String requestUri;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求开始时间
     */
    private Long startTime;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 扩展属性（自定义数据）
     */
    private java.util.Map<String, Object> extra;

    /**
     * 设置扩展属性
     */
    public LogInfo putExtra(String key, Object value) {
        if (this.extra == null) {
            this.extra = new java.util.HashMap<>();
        }
        this.extra.put(key, value);
        return this;
    }

    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        return this.extra != null ? (T) this.extra.get(key) : null;
    }

}