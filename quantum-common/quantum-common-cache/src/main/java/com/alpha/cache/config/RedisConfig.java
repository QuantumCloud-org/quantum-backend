package com.alpha.cache.config;

import com.alpha.cache.util.RedisUtil;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class RedisConfig {

    private final RedissonPoolProperties poolProperties;

    /**
     * Redisson 配置定制
     * <p>
     * Redisson starter 只从 spring.data.redis.* 读取基础连接信息，
     * 连接池/超时/重试等高级参数通过此 customizer 从 RedissonPoolProperties 注入。
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            // 基于 Redisson 默认 codec（含类型信息），注册 JavaTimeModule 以正确序列化 LocalDateTime
            JsonJacksonCodec codec = new JsonJacksonCodec();
            codec.getObjectMapper().registerModule(new JavaTimeModule());
            codec.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            config.setCodec(codec);

            SingleServerConfig ssc = config.useSingleServer();
            ssc.setConnectionMinimumIdleSize(poolProperties.getConnectionMinimumIdleSize());
            ssc.setConnectionPoolSize(poolProperties.getConnectionPoolSize());
            ssc.setConnectTimeout(poolProperties.getConnectTimeout());
            ssc.setTimeout(poolProperties.getTimeout());
            ssc.setRetryAttempts(poolProperties.getRetryAttempts());
            ssc.setRetryInterval(poolProperties.getRetryInterval());
            ssc.setSubscriptionConnectionMinimumIdleSize(poolProperties.getSubscriptionConnectionMinimumIdleSize());
            ssc.setSubscriptionConnectionPoolSize(poolProperties.getSubscriptionConnectionPoolSize());
            ssc.setPingConnectionInterval(poolProperties.getPingConnectionInterval());

            log.info("Redisson 配置完成 | codec=JsonJacksonCodec | pool={}/{} | timeout={}ms | retry={}x{}ms",
                    poolProperties.getConnectionMinimumIdleSize(),
                    poolProperties.getConnectionPoolSize(),
                    poolProperties.getTimeout(),
                    poolProperties.getRetryAttempts(),
                    poolProperties.getRetryInterval());
        };
    }

    @Bean
    public RedisUtil redisUtil(RedissonClient redissonClient) {
        return new RedisUtil(redissonClient);
    }
}
