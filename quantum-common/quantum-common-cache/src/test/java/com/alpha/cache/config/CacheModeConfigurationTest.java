package com.alpha.cache.config;

import com.alpha.cache.constant.CacheConstants;
import com.alpha.cache.util.CacheClient;
import com.alpha.cache.util.LocalCacheClient;
import com.alpha.cache.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheModeConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(RedisConfig.class, LocalCacheConfig.class, MultiLevelCacheConfig.class)
            .withBean(RedissonPoolProperties.class, RedissonPoolProperties::new);

    @Test
    void localModeDoesNotInstantiateRedisson() {
        context.withPropertyValues("cache.mode=local").run(app -> {
            assertThat(app).hasNotFailed().doesNotHaveBean(RedissonClient.class).hasSingleBean(LocalCacheClient.class);
        });
    }

    @Test
    void redisModeCanBuildItsCacheWithTheStarterConfiguration() {
        var client = mock(RedissonClient.class);
        when(client.getTopic(eq(CacheConstants.SYNC_TOPIC), any(TypedJsonJacksonCodec.class)))
                .thenReturn(mock(RTopic.class));
        context.withPropertyValues("cache.mode=redis")
                .withBean("redisson", RedissonClient.class, () -> client)
                .run(app -> {
                    assertThat(app).hasNotFailed()
                            .hasSingleBean(org.redisson.spring.starter.RedissonAutoConfigurationV4.class)
                            .hasSingleBean(CacheClient.class).hasSingleBean(RedisUtil.class).hasSingleBean(CacheManager.class);
                });
    }
}
