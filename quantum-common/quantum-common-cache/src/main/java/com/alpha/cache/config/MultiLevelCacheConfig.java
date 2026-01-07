package com.alpha.cache.config;

import com.alpha.cache.constant.CacheConstants;
import com.alpha.cache.util.RedisUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置（L1 Caffeine + L2 Redis + Pub/Sub 同步）
 * <p>
 * 使用方式：
 * - @Cacheable(cacheNames = "user", key = "#id")  // 自动多级缓存
 * - @CacheEvict(cacheNames = "user", key = "#id") // 自动同步清除
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableCaching
@ConditionalOnBean(RedissonClient.class)
@DependsOn("redisson")
public class MultiLevelCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisUtil redisUtil, RedissonClient redissonClient) {
        return new MultiLevelCacheManager(redisUtil, redissonClient);
    }

    /**
     * 缓存同步消息
     */
    public record SyncMessage(String instanceId, String cacheName, String key, boolean clearAll) {
    }

    /**
     * 多级缓存管理器
     */
    public static class MultiLevelCacheManager implements CacheManager {

        private final String instanceId;
        private final ConcurrentMap<String, MultiLevelCache> caches = new ConcurrentHashMap<>();
        private final RedisUtil redisUtil;
        private final RedissonClient redissonClient;

        public MultiLevelCacheManager(RedisUtil redisUtil, RedissonClient redissonClient) {
            this.instanceId = UUID.randomUUID().toString().substring(0, 8);
            this.redisUtil = redisUtil;
            this.redissonClient = redissonClient;
            subscribeSyncMessage();
        }

        @Override
        public org.springframework.cache.Cache getCache(@NonNull String name) {
            return caches.computeIfAbsent(name, n ->
                    new MultiLevelCache(n, redisUtil, redissonClient, instanceId));
        }

        @Override
        @NonNull
        public Collection<String> getCacheNames() {
            return caches.keySet();
        }

        /**
         * 订阅缓存同步消息
         */
        private void subscribeSyncMessage() {
            redisUtil.subscribe(CacheConstants.SYNC_TOPIC, SyncMessage.class, (channel, msg) -> {
                // 忽略自己发送的消息
                if (instanceId.equals(msg.instanceId())) {
                    return;
                }

                MultiLevelCache cache = caches.get(msg.cacheName());
                if (cache != null) {
                    if (msg.clearAll()) {
                        cache.getLocalCache().invalidateAll();
                    } else {
                        cache.getLocalCache().invalidate(msg.key());
                    }
                    log.debug("同步清除本地缓存: {} -> {}", msg.cacheName(), msg.key());
                }
            });
            log.info("缓存同步订阅完成, topic={}, instanceId={}", CacheConstants.SYNC_TOPIC, instanceId);
        }
    }

    /**
     * 多级缓存实现（静态内部类，避免持有外部类引用）
     */
    @Getter
    public static class MultiLevelCache extends AbstractValueAdaptingCache {

        private static final Object NULL_PLACEHOLDER = new Object();

        private final String name;
        private final String instanceId;
        private final String keyPrefix;
        private final Cache<Object, Object> localCache;
        private final RedisUtil redis;
        private final RedissonClient redissonClient;

        public MultiLevelCache(String name, RedisUtil redis, RedissonClient redissonClient, String instanceId) {
            super(false);
            this.name = name;
            this.redis = redis;
            this.redissonClient = redissonClient;
            this.instanceId = instanceId;
            this.keyPrefix = CacheConstants.REDIS_KEY_PREFIX + name + ":";
            this.localCache = Caffeine.newBuilder()
                    .maximumSize(CacheConstants.getCaffeineMaxSize(name))
                    .expireAfterWrite(CacheConstants.getCaffeineExpire(name))
                    .recordStats()
                    .build();
        }

        @Override
        protected Object lookup(@NonNull Object key) {
            // L1: Caffeine
            Object value = localCache.getIfPresent(key);
            if (value != null) {
                log.trace("L1 命中: {} -> {}", name, key);
                return value == NULL_PLACEHOLDER ? null : value;
            }

            // L2: Redis
            value = redis.get(keyPrefix + key);
            if (value != null) {
                log.trace("L2 命中: {} -> {}", name, key);
                localCache.put(key, value);
                return value;
            }

            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
            // L1 检查
            Object value = localCache.getIfPresent(key);
            if (value != null) {
                return value == NULL_PLACEHOLDER ? null : (T) value;
            }

            // L2 检查
            String redisKey = keyPrefix + key;
            value = redis.get(redisKey);
            if (value != null) {
                localCache.put(key, value);
                return (T) value;
            }

            // 缓存击穿保护：分布式锁
            String lockKey = "lock:" + redisKey;
            RLock lock = redissonClient.getLock(lockKey);

            try {
                // 尝试获取锁，最多等待 3 秒
                if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                    try {
                        // Double Check
                        value = redis.get(redisKey);
                        if (value != null) {
                            localCache.put(key, value);
                            return (T) value;
                        }

                        // 加载数据
                        T loaded = valueLoader.call();
                        if (loaded != null) {
                            redis.set(redisKey, loaded, CacheConstants.getRedisExpire(name));
                            localCache.put(key, loaded);
                        } else {
                            // 缓存穿透保护：缓存空值
                            redis.set(redisKey, NULL_PLACEHOLDER, java.time.Duration.ofMinutes(1));
                            localCache.put(key, NULL_PLACEHOLDER);
                        }
                        return loaded;
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {
                    // 获取锁失败，直接查询（降级）
                    return valueLoader.call();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ValueRetrievalException(key, valueLoader, e);
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(@NonNull Object key, Object value) {
            if (value == null) {
                evict(key);
                return;
            }
            localCache.put(key, value);
            redis.set(keyPrefix + key, value, CacheConstants.getRedisExpire(name));
            publishSync(key, false);
        }

        @Override
        public void evict(@NonNull Object key) {
            localCache.invalidate(key);
            redis.delete(keyPrefix + key);
            publishSync(key, false);
        }

        @Override
        public void clear() {
            localCache.invalidateAll();
            redis.deleteKeysByPattern(keyPrefix + "*");
            publishSync(null, true);
        }

        @Override
        @NonNull
        public String getName() {
            return name;
        }

        @Override
        @NonNull
        public Object getNativeCache() {
            return this;
        }

        /**
         * 发布缓存同步消息
         */
        private void publishSync(Object key, boolean clearAll) {
            // 异步发布，不阻塞主流程
            Thread.startVirtualThread(() -> {
                try {
                    redis.publish(CacheConstants.SYNC_TOPIC,
                            new SyncMessage(instanceId, name, key != null ? key.toString() : null, clearAll));
                } catch (Exception e) {
                    log.warn("缓存同步消息发布失败", e);
                }
            });
        }
    }
}