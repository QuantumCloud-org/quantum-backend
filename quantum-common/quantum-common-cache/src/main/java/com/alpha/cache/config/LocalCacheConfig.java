package com.alpha.cache.config;

import com.alpha.cache.constant.CacheConstants;
import com.alpha.cache.util.CacheClient;
import com.alpha.cache.util.LocalCacheClient;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 本地缓存配置（{@code cache.mode=local}）
 * <p>
 * 激活条件：配置 {@code cache.mode=local}，通常用于无 Redis 的开发/测试环境。
 * 通过 {@code @ImportAutoConfiguration(exclude=...)} 禁用 Redisson/Redis 自动装配，
 * 避免 Spring Boot 在找不到 Redis 时尝试建立连接。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableCaching
@ConditionalOnProperty(name = "cache.mode", havingValue = "local")
@ImportAutoConfiguration(exclude = {
        RedissonAutoConfiguration.class
})
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

        // 预注册已知 cache，使用各自的独立配置
        List<String> knownCaches = List.of("user", "permission", "dict", "config");
        for (String name : knownCaches) {
            manager.registerCustomCache(name,
                    Caffeine.newBuilder()
                            .maximumSize(CacheConstants.getCaffeineMaxSize(name))
                            .expireAfterWrite(CacheConstants.getCaffeineExpire(name))
                            .recordStats()
                            .build());
        }

        // 其余 cache 使用默认配置
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(CacheConstants.CAFFEINE_MAX_SIZE)
                        .expireAfterWrite(CacheConstants.CAFFEINE_EXPIRE)
        );
        return manager;
    }
}
