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

}