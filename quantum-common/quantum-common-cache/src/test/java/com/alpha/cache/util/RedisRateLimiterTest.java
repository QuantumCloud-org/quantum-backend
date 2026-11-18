package com.alpha.cache.util;

import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimiterTest {
    @Test
    void initializesOnlyOnceWhileUsingRedisToEnforceTheThreshold() {
        var client = mock(RedissonClient.class);
        var limiter = mock(RRateLimiter.class);
        when(client.getRateLimiter("route")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true, true, false);
        var cache = new RedisUtil(client);

        assertThat(cache.tryAcquire("route", 2, 1)).isTrue();
        assertThat(cache.tryAcquire("route", 2, 1)).isTrue();
        assertThat(cache.tryAcquire("route", 2, 1)).isFalse();
        verify(limiter).trySetRate(eq(RateType.OVERALL), eq(2L), eq(Duration.ofSeconds(1)), eq(Duration.ofMinutes(2)));
    }
}
