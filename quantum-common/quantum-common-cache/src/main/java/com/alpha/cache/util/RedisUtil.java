package com.alpha.cache.util;

import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 工具类
 * <p>
 * 基于 Redisson 封装，提供：
 * 1. 基础 KV 操作
 * 2. 分布式锁
 * 3. 限流器
 * 4. 布隆过滤器
 * 5. 延迟队列
 * <p>
 * 设计原则：
 * - 方法命名直观，贴近业务语义
 * - 自动处理序列化，调用方无感知
 * - 提供安全的默认值，避免 NPE
 * <p>
 */
public class RedisUtil implements CacheClient {

    private final RedissonClient redisson;

    public RedisUtil(RedissonClient redisson) {
        this.redisson = redisson;
    }

    private static final Duration DEFAULT_EXPIRE = Duration.ofMinutes(30);
    private static final long DEFAULT_WAIT_TIME = 3L;
    private static final long DEFAULT_LEASE_TIME = 10L;

    // ==================== 基础 KV 操作 ====================

    @Override
    public <T> void set(String key, T value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration expire) {
        RBucket<Object> bucket = redisson.getBucket(key);
        return bucket.setIfAbsent(value, expire);
    }

    @Override
    public <T> void set(String key, T value, Duration expire) {
        RBucket<T> bucket = redisson.getBucket(key);
        bucket.set(value, expire);
    }

    @Override
    public <T> void set(String key, T value, long expireSeconds) {
        set(key, value, Duration.ofSeconds(expireSeconds));
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
    public <T> T getOrDefault(String key, T defaultValue) {
        T value = get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public <T> T getOrLoad(String key, Supplier<T> loader, Duration expire) {
        RBucket<T> bucket = redisson.getBucket(key);
        T value = bucket.get();
        if (value == null) {
            value = loader.get();
            if (value != null) {
                bucket.set(value, expire);
            }
        }
        return value;
    }

    @Override
    public void delete(String key) {
        redisson.getBucket(key).delete();
    }

    @Override
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

    // ==================== Hash 操作 ====================

    @Override
    public <T> void hSet(String key, String field, T value) {
        redisson.<String, T>getMap(key).put(field, value);
    }

    @Override
    public void hSetAll(String key, Map<String, Object> map) {
        redisson.<String, Object>getMap(key).putAll(map);
    }

    @Override
    public <T> T hGet(String key, String field) {
        return redisson.<String, T>getMap(key).get(field);
    }

    @Override
    public <T> Map<String, T> hGetAll(String key) {
        return new HashMap<>(redisson.<String, T>getMap(key).readAllMap());
    }

    @Override
    public boolean hDelete(String key, String... fields) {
        return redisson.<String, Object>getMap(key).fastRemove(fields) > 0;
    }

    @Override
    public boolean hExists(String key, String field) {
        return redisson.<String, Object>getMap(key).containsKey(field);
    }

    // ==================== List 操作 ====================

    @Override
    public <T> void lPush(String key, T value) {
        redisson.<T>getDeque(key).addFirst(value);
    }

    @Override
    public <T> void rPush(String key, T value) {
        redisson.<T>getDeque(key).addLast(value);
    }

    @Override
    public <T> T lPop(String key) {
        return redisson.<T>getDeque(key).pollFirst();
    }

    @Override
    public <T> T rPop(String key) {
        return redisson.<T>getDeque(key).pollLast();
    }

    @Override
    public <T> List<T> lRange(String key, int start, int end) {
        return redisson.<T>getList(key).range(start, end);
    }

    @Override
    public long lSize(String key) {
        return redisson.getList(key).size();
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
    public <T> boolean sIsMember(String key, T value) {
        return redisson.<T>getSet(key).contains(value);
    }

    @Override
    public <T> boolean sRemove(String key, T value) {
        return redisson.<T>getSet(key).remove(value);
    }

    @Override
    @SafeVarargs
    public final <T> boolean sRemove(String key, T... values) {
        return redisson.<T>getSet(key).removeAll(Arrays.asList(values));
    }

    // ==================== ZSet 有序集合 ====================

    @Override
    public <T> void zAdd(String key, T value, double score) {
        redisson.<T>getScoredSortedSet(key).add(score, value);
    }

    @Override
    public <T> Collection<T> zRange(String key, int start, int end) {
        return redisson.<T>getScoredSortedSet(key).valueRange(start, end);
    }

    @Override
    public <T> Collection<T> zRangeByScore(String key, double min, double max) {
        return redisson.<T>getScoredSortedSet(key).valueRange(min, true, max, true);
    }

    @Override
    public <T> Double zScore(String key, T value) {
        return redisson.<T>getScoredSortedSet(key).getScore(value);
    }

    @Override
    public boolean zRemove(String key, Object value) {
        return redisson.getScoredSortedSet(key).remove(value);
    }

    @Override
    public long zSize(String key) {
        return redisson.getScoredSortedSet(key).size();
    }

    // ==================== 限流器 ====================

    @Override
    public boolean tryAcquire(String key, long rate, long interval) {
        RRateLimiter limiter = redisson.getRateLimiter(key);
        limiter.trySetRate(
                RateType.OVERALL,
                rate,
                Duration.ofSeconds(interval),
                Duration.ofSeconds(interval)
        );
        return limiter.tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 10, 1);
    }

    // ==================== 分布式锁 ====================

    public boolean lock(String lockKey) {
        return lock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME);
    }

    public boolean lock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redisson.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(String lockKey) {
        RLock lock = redisson.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, supplier);
    }

    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        boolean locked = lock(lockKey, waitTime, leaseTime);
        if (!locked) {
            return null;
        }
        try {
            return supplier.get();
        } finally {
            unlock(lockKey);
        }
    }

    public boolean executeWithLock(String lockKey, Runnable runnable) {
        boolean locked = lock(lockKey);
        if (!locked) {
            return false;
        }
        try {
            runnable.run();
            return true;
        } finally {
            unlock(lockKey);
        }
    }

    // ==================== 布隆过滤器 ====================

    public void initBloomFilter(String key, long expectedInsertions, double falseProbability) {
        redisson.<Object>getBloomFilter(key).tryInit(expectedInsertions, falseProbability);
    }

    public <T> boolean bloomAdd(String key, T value) {
        return redisson.<T>getBloomFilter(key).add(value);
    }

    public <T> boolean bloomContains(String key, T value) {
        return redisson.<T>getBloomFilter(key).contains(value);
    }

    // ==================== 发布订阅 ====================

    public <T> void publish(String channel, T message) {
        redisson.getTopic(channel).publish(message);
    }

    public <T> int subscribe(String channel, Class<T> type, MessageListener<T> listener) {
        return redisson.getTopic(channel).addListener(type, listener);
    }

    // ==================== 延迟队列 ====================

    public <T> void addDelayedTask(String queueName, T value, long delaySeconds) {
        String zsetKey = queueName + ":delay";
        long executeAt = System.currentTimeMillis() + delaySeconds * 1000;
        redisson.<T>getScoredSortedSet(zsetKey).add(executeAt, value);
    }

    public <T> T takeDelayedTask(String queueName) throws InterruptedException {
        return redisson.<T>getBlockingQueue(queueName).take();
    }

    public <T> T pollDelayedTask(String queueName) {
        return redisson.<T>getBlockingQueue(queueName).poll();
    }
}
