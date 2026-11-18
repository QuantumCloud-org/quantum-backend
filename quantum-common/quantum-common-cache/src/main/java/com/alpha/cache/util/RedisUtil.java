package com.alpha.cache.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.*;
import org.redisson.api.bucket.CompareAndSetArgs;
import org.redisson.client.RedisException;

import java.time.Duration;
import java.util.*;

/** Redis implementation of the cache client and distributed cache synchronization. */
public class RedisUtil implements CacheClient {

    private final RedissonClient redisson;
    private final Cache<String, Boolean> initializedLimiters = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(1))
            .build();

    public RedisUtil(RedissonClient redisson) {
        this.redisson = redisson;
    }


    // ==================== 基础 KV 操作 ====================

    @Override
    public boolean setIfAbsent(String key, Object value, Duration expire) {
        RBucket<Object> bucket = redisson.getBucket(key);
        return expire == null ? bucket.setIfAbsent(value) : bucket.setIfAbsent(value, expire);
    }

    @Override
    public <T> void set(String key, T value, Duration expire) {
        RBucket<T> bucket = redisson.getBucket(key);
        if (expire == null) {
            bucket.set(value);
        } else {
            bucket.set(value, expire);
        }
    }

    @Override
    public <T> T getAndDelete(String key) {
        RBucket<T> bucket = redisson.getBucket(key);
        return bucket.getAndDelete();
    }

    @Override
    public <T> T get(String key) {
        RBucket<T> bucket = redisson.getBucket(key);
        return bucket.get();
    }

    @Override
    public <T> Map<String, T> getAll(Collection<String> keys) {
        return keys.isEmpty() ? Map.of() : redisson.getBuckets().get(keys.toArray(String[]::new));
    }

    @Override
    public boolean replaceIfPresent(String key, Object value, Duration ttl) {
        return redisson.getBucket(key).setIfExists(value, ttl);
    }

    @Override
    public boolean compareAndSet(String key, String expected, String replacement, Duration ttl) {
        return redisson.<String>getBucket(key)
                .compareAndSet(CompareAndSetArgs.expected(expected).set(replacement).timeToLive(ttl));
    }

    @Override
    public void delete(String key) {
        redisson.getBucket(key).delete();
    }

    public void deleteKeysByPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return;
        }
        redisson.getKeys().deleteByPattern(pattern);
    }

    @Override
    public boolean exists(String key) {
        return redisson.getBucket(key).isExists();
    }

    @Override
    public boolean expire(String key, Duration expire) {
        return redisson.getBucket(key).expire(expire);
    }

    @Override
    public long getExpire(String key) {
        // remainTimeToLive: >=0 ms remaining, -1 = no TTL, -2 = key not found
        // interface contract: -1 for both "no TTL" and "not found"
        long ttl = redisson.getBucket(key).remainTimeToLive();
        return ttl < 0 ? -1L : ttl / 1000;
    }

    @Override
    public long increment(String key) {
        return redisson.getAtomicLong(key).incrementAndGet();
    }

    @Override
    public long increment(String key, long delta) {
        return redisson.getAtomicLong(key).addAndGet(delta);
    }



    // ==================== Set 操作 ====================

    @Override
    @SafeVarargs
    public final <T> void sAdd(String key, T... values) {
        redisson.<T>getSet(key).addAll(Arrays.asList(values));
    }

    @Override
    public <T> Set<T> sMembers(String key) {
        return redisson.<T>getSet(key).readAll();
    }

    @Override
    public <T> boolean sRemove(String key, T value) {
        return redisson.<T>getSet(key).remove(value);
    }

    // ==================== ZSet 有序集合 ====================

    @Override
    public <T> void zAdd(String key, T value, double score) {
        redisson.<T>getScoredSortedSet(key).add(score, value);
    }

    @Override
    public <T> Collection<T> zRangeByScore(String key, double min, double max) {
        return redisson.<T>getScoredSortedSet(key).valueRange(min, true, max, true);
    }

    @Override
    public boolean zRemove(String key, Object value) {
        return redisson.getScoredSortedSet(key).remove(value);
    }

    @Override
    public long zSize(String key) {
        return redisson.getScoredSortedSet(key).size();
    }

    @Override
    public void zRemoveThrough(String key, double maxScore) {
        redisson.getScoredSortedSet(key).removeRangeByScore(Double.NEGATIVE_INFINITY, true, maxScore, true);
    }

    // ==================== 限流器 ====================

    @Override
    public boolean tryAcquire(String key, long rate, long interval) {
        RRateLimiter limiter = redisson.getRateLimiter(key);
        initializeLimiter(key, rate, interval, limiter);
        try {
            return limiter.tryAcquire();
        } catch (RedisException e) {
            // Redis may have evicted or expired the limiter independently of this process.
            if (e.getMessage() == null || !e.getMessage().contains("RateLimiter is not initialized")) {
                throw e;
            }
            initializedLimiters.invalidate(key);
            initializeLimiter(key, rate, interval, limiter);
            return limiter.tryAcquire();
        }
    }

    private void initializeLimiter(String key, long rate, long interval, RRateLimiter limiter) {
        initializedLimiters.get(key, name -> {
            limiter.trySetRate(RateType.OVERALL, rate, Duration.ofSeconds(interval),
                    Duration.ofSeconds(Math.max(120, interval * 2)));
            return Boolean.TRUE;
        });
    }

}
