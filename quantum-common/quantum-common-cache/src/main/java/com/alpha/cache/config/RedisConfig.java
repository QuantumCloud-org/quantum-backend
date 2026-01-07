package com.alpha.cache.config;

import com.alpha.cache.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    /**
     * Redisson 序列化配置
     * 使用全局 ObjectMapper，保证 Web 和 Redis 序列化一致
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer(ObjectMapper objectMapper) {
        return config -> {
            config.setCodec(new JsonJacksonCodec(objectMapper));
            log.info("Redisson Codec 配置完成，使用 Jackson 序列化");
        };
    }

    /**
     * Redis 工具类
     */
    @Bean
    public RedisUtil redisUtil(RedissonClient redissonClient) {
        return new RedisUtil(redissonClient);
    }

}