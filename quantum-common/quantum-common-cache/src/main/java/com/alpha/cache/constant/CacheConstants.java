package com.alpha.cache.constant;

import java.time.Duration;

/**
 * 多级缓存常量配置
 * <p>
 * 所有缓存相关配置集中管理，修改后需重启生效
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /**
     * Redis Key 前缀
     */
    public static final String REDIS_KEY_PREFIX = "cache:";

    /**
     * Pub/Sub 同步 Topic
     */
    public static final String SYNC_TOPIC = "cache:sync:topic";

    /**
     * 最大容量
     */
    public static final long CAFFEINE_MAX_SIZE = 1000L;

    /**
     * 写入后过期时间
     */
    public static final Duration CAFFEINE_EXPIRE = Duration.ofMinutes(10);

    // ==================== Redis L2 默认配置 ====================

    /**
     * 过期时间
     */
    public static final Duration REDIS_EXPIRE = Duration.ofHours(1);

    /**
     * 获取指定缓存的 Caffeine 最大容量
     */
    public static long getCaffeineMaxSize(String cacheName) {
        return switch (cacheName) {
            case CacheKeyConstant.SYS_DICT -> 200L;
            default -> CAFFEINE_MAX_SIZE;
        };
    }

    /**
     * 获取指定缓存的 Caffeine 过期时间
     */
    public static Duration getCaffeineExpire(String cacheName) {
        return switch (cacheName) {
            case CacheKeyConstant.SYS_DICT -> Duration.ofMinutes(30);
            default -> CAFFEINE_EXPIRE;
        };
    }

    /**
     * 获取指定缓存的 Redis 过期时间
     */
    public static Duration getRedisExpire(String cacheName) {
        return switch (cacheName) {
            case CacheKeyConstant.SYS_DICT -> Duration.ofHours(24);
            default -> REDIS_EXPIRE;
        };
    }
}