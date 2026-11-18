package com.alpha.cache.config;

import com.alpha.cache.constant.CacheConstants;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.cache.util.CacheClient;
import com.alpha.cache.util.LocalCacheClient;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地缓存配置（{@code cache.mode=local}）
 * <p>
 * 激活条件：配置 {@code cache.mode=local}，通常用于无 Redis 的开发/测试环境。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "cache.mode", havingValue = "local")
public class LocalCacheConfig {

    @Bean
    public CacheClient cacheClient() {
        log.info("缓存模式: LOCAL (Caffeine-only)，无需 Redis");
        return new LocalCacheClient();
    }

    /**
     * 纯 Caffeine CacheManager，支持 {@code @Cacheable} 等 Spring Cache 注解。
     * per-cache 的容量和过期时间与 {@link CacheConstants} 保持一致。
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);

        String dictionary = CacheKeyConstant.SYS_DICT;
        manager.registerCustomCache(dictionary, Caffeine.newBuilder()
                .maximumSize(CacheConstants.getCaffeineMaxSize(dictionary))
                .expireAfterWrite(CacheConstants.getCaffeineExpire(dictionary))
                .recordStats()
                .build());

        // 其余 cache 使用默认配置
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(CacheConstants.CAFFEINE_MAX_SIZE)
                        .expireAfterWrite(CacheConstants.CAFFEINE_EXPIRE)
        );
        return manager;
    }
}
