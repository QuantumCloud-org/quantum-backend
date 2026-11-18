package com.alpha.cache.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheInfoTest {

    @Test
    void readingStatsForAnUnknownNameDoesNotCreateACache() {
        var manager = new CaffeineCacheManager();
        manager.getCache("known");
        var endpoint = new CacheInfo(manager);

        assertThat(endpoint.stat("unknown")).containsKey("error");
        assertThat(manager.getCacheNames()).containsExactly("known");
    }
}
