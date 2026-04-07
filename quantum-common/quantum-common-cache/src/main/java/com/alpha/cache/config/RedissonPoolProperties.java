package com.alpha.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redisson 连接池与高级配置
 * <p>
 * Redisson starter 的 RedissonProperties 只支持 config/file 两个属性，
 * 不直接绑定连接池、超时、重试等参数。本类补充这些配置项，
 * 通过 RedisConfig 中的 RedissonAutoConfigurationCustomizer 应用到 Redisson Config。
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.data.redis.redisson")
public class RedissonPoolProperties {

    /** 最小空闲连接数 */
    private int connectionMinimumIdleSize = 8;

    /** 连接池大小 */
    private int connectionPoolSize = 32;

    /** 连接超时（毫秒） */
    private int connectTimeout = 3000;

    /** 命令等待超时（毫秒） */
    private int timeout = 3000;

    /** 命令失败重试次数 */
    private int retryAttempts = 3;

    /** 重试间隔（毫秒） */
    private int retryInterval = 1500;

    /** 订阅最小空闲连接数 */
    private int subscriptionConnectionMinimumIdleSize = 1;

    /** 订阅连接池大小 */
    private int subscriptionConnectionPoolSize = 8;

    /** 心跳检测间隔（毫秒），0 禁用 */
    private int pingConnectionInterval = 3000;
}
