package com.alpha.cache.config;

import com.alpha.cache.config.MultiLevelCacheConfig.MultiLevelCache;
import com.alpha.cache.config.MultiLevelCacheConfig.MultiLevelCacheManager;
import com.alpha.cache.config.MultiLevelCacheConfig.SyncMessage;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.cache.constant.CacheConstants;
import com.alpha.cache.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.Codec;
import org.redisson.misc.CompletableFutureWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiLevelCacheSyncTest {
    @Test
    void transfersDictionaryEvictionAndClearBetweenCachesThroughTheTopicCodec() {
        var redisson = mock(RedissonClient.class);
        var data = new RedisUtil(redisson);
        List<Subscription> subscriptions = new ArrayList<>();
        var channel = CacheConstants.SYNC_TOPIC;
        when(redisson.getBucket(anyString())).thenReturn(mock(RBucket.class));
        when(redisson.getKeys()).thenReturn(mock(RKeys.class));
        when(redisson.getTopic(channel)).thenAnswer(call -> topic(new RedisConfig.CacheJsonCodec(), subscriptions));
        when(redisson.getTopic(eq(channel), any(Codec.class)))
                .thenAnswer(call -> topic(call.getArgument(1), subscriptions));
        var publisher = dictionaryCache(redisson, data);
        var peer = dictionaryCache(redisson, data);
        peer.getLocalCache().put("enabled", "stale");
        peer.getLocalCache().put("other", "also stale");

        publisher.evict("enabled");

        assertThat(peer.getLocalCache().getIfPresent("enabled")).isNull();
        assertThat(peer.getLocalCache().getIfPresent("other")).isEqualTo("also stale");

        publisher.clear();

        assertThat(peer.getLocalCache().getIfPresent("other")).isNull();
    }

    private static MultiLevelCache dictionaryCache(RedissonClient redisson, RedisUtil data) {
        return (MultiLevelCache) new MultiLevelCacheManager(data, redisson).getCache(CacheKeyConstant.SYS_DICT);
    }

    private static RTopic topic(Codec codec, List<Subscription> subscriptions) {
        var topic = mock(RTopic.class);
        when(topic.addListener(eq(SyncMessage.class), any()))
                .thenAnswer(call -> {
                    subscriptions.add(new Subscription(codec, call.getArgument(1)));
                    return subscriptions.size();
                });
        when(topic.publishAsync(any(SyncMessage.class))).thenAnswer(call -> {
            var message = call.<SyncMessage>getArgument(0);
            deliver(codec, message, subscriptions);
            return new CompletableFutureWrapper<>((long) subscriptions.size());
        });
        return topic;
    }

    private static void deliver(Codec codec, SyncMessage message, List<Subscription> subscriptions) throws IOException {
        var encoded = codec.getValueEncoder().encode(message);
        try {
            for (var subscription : subscriptions) {
                var decoded = subscription.codec().getValueDecoder().decode(encoded.duplicate(), null);
                subscription.listener().onMessage(CacheConstants.SYNC_TOPIC, SyncMessage.class.cast(decoded));
            }
        } finally {
            encoded.release();
        }
    }

    private record Subscription(Codec codec, MessageListener<SyncMessage> listener) {}
}
