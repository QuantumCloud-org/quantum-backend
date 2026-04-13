package com.alpha.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 白名单路径（不需要认证）
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * 是否启用 CSRF
     */
    private boolean csrfEnabled = false;

    /**
     * 是否启用 XSS 过滤
     */
    private boolean xssEnabled = true;

    /**
     * XSS 排除路径
     */
    private List<String> xssExcludes = new ArrayList<>();

    /**
     * 是否启用防重复提交
     */
    private boolean repeatSubmitEnabled = true;

    /**
     * 重复提交间隔（秒）
     */
    private int repeatSubmitInterval = 5;

    /**
     * 是否启用限流
     */
    private boolean rateLimitEnabled = true;

    /**
     * 默认限流（每秒请求数）
     */
    private int defaultRateLimit = 100;

    /**
     * 密码最小长度
     */
    private int passwordMinLength = 8;

    /**
     * 密码最大长度
     */
    private int passwordMaxLength = 20;

    /**
     * CORS 允许的来源
     */
    private List<String> corsAllowedOrigins = List.of("http://localhost:5173");

    /**
     * 登录失败最大次数
     */
    private int maxLoginFailCount = 5;

    /**
     * 是否强制要求验证码
     */
    private boolean captchaRequired = true;

    /**
     * 账号锁定时间（分钟）
     */
    private int lockTime = 15;
}
