package com.alpha.security.token;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Token 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.token")
public class TokenProperties {

    /**
     * JWT 签名密钥（HS512 需要至少64字符）
     * JWT Secret Key 512位
     */
    private String secret;

    /**
     * Access Token 有效期（分钟）
     */
    private int accessTokenExpire = 10;

    /**
     * Refresh Token 有效期（分钟）
     * 默认30分钟
     */
    private int refreshTokenExpire = 30;

    /**
     * Token 自动续期阈值（百分比 0-1）
     * 当剩余有效期小于此比例时自动续期
     */
    private double renewThreshold = 0.3;

    /**
     * 是否启用单设备登录
     * 启用后同一用户新登录会踢掉其他设备的 Token
     */
    private boolean singleDevice = false;

    /**
     * 是否验证 IP 和 User-Agent
     * 启用后，RefreshToken 使用时的 IP/UA 必须与登录时一致
     */
    private boolean verifyClientInfo = true;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.token.secret 未配置");
        }
        if (secret.length() < 64) {
            throw new IllegalStateException("security.token.secret 长度不足，HS512 至少需要 64 个字符");
        }
        if (secret.contains("change-me")) {
            throw new IllegalStateException("security.token.secret 使用了占位默认值，禁止启动");
        }
    }

}
