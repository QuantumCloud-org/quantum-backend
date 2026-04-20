package com.alpha.security.token;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Token 信息
 */
@Data
@Accessors(chain = true)
public class TokenInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Access Token
     */
    private String accessToken;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * Token 类型
     */
    private String tokenType = "Bearer";

    // ==================== 以下为 Redis 中存储的 Token 元数据 ====================

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * Token ID（JWT 的 jti 字段）
     */
    private String tokenId;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * Refresh Token ID（JWT 的 jti 字段）
     */
    private String refreshTokenId;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 创建时间（毫秒）
     */
    private Long createdAt;

    /**
     * 刷新次数
     */
    private Integer refreshCount;

    /**
     * 是否持久化会话。
     * true: refresh cookie 带 Max-Age
     * false: Session Cookie
     */
    private Boolean rememberMe;
}
