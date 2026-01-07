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
public class RedisUtil {

    private final RedissonClient redisson;

    public RedisUtil(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * 默认缓存过期时间：30分钟
     */
    private static final Duration DEFAULT_EXPIRE = Duration.ofMinutes(30);

    /**
     * 锁默认等待时间：3秒
     */
    private static final long DEFAULT_WAIT_TIME = 3L;

    /**
     * 锁默认持有时间：10秒
     */
    private static final long DEFAULT_LEASE_TIME = 10L;

    // ==================== 基础 KV 操作 ====================

    /**
     * 设置缓存（默认30分钟过期）
     */
    public <T> void set(String key, T value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    /**
     * 设置值（如果不存在）
     */
    public boolean setIfAbsent(String key, Object value, Duration expire) {
        RBucket<Object> bucket = redisson.getBucket(key);
        return bucket.setIfAbsent(value, expire);
    }

    /**
     * 设置缓存（指定过期时间）
     */
    public <T> void set(String key, T value, Duration expire) {
        RBucket<T> bucket = redisson.getBucket(key);
        bucket.set(value, expire);
    }

    /**
     * 设置缓存（秒为单位）
     */
    public <T> void set(String key, T value, long expireSeconds) {
        set(key, value, Duration.ofSeconds(expireSeconds));
    }

    /**
     * 从 Set 中移除元素
     */
    @SafeVarargs
    public final <T> boolean sRemove(String key, T... values) {
        RSet<T> set = redisson.getSet(key);
        return set.removeAll(Arrays.asList(values));
    }

    /**
     * 获取缓存
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redisson.getBucket(key);
        return bucket.get();
    }

    /**
     * 获取缓存，不存在时返回默认值
     */
    public <T> T getOrDefault(String key, T defaultValue) {
        T value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取缓存，不存在时执行加载函数并缓存
     *
     * @param key    缓存键
     * @param loader 加载函数（缓存未命中时执行）
     * @param expire 过期时间
     */
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

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisson.getBucket(key).delete();
    }

    /**
     * 批量删除（支持通配符 *）
     */
    public void deleteKeysByPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return;
        }
        RKeys keys = redisson.getKeys();
        keys.deleteByPattern(pattern);
    }

    /**
     * 判断 key 是否存在
     */
    public boolean exists(String key) {
        return redisson.getBucket(key).isExists();
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, Duration expire) {
        return redisson.getBucket(key).expire(expire);
    }

    /**
     * 获取剩余过期时间（秒）
     */
    public long getExpire(String key) {
        return redisson.getBucket(key).remainTimeToLive() / 1000;
    }

    /**
     * 原子自增
     */
    public long increment(String key) {
        return redisson.getAtomicLong(key).incrementAndGet();
    }

    /**
     * 原子自增指定值
     */
    public long increment(String key, long delta) {
        return redisson.getAtomicLong(key).addAndGet(delta);
    }

    // ==================== Hash 操作 ====================

    public <T> void hSet(String key, String field, T value) {
        RMap<String, T> map = redisson.getMap(key);
        map.put(field, value);
    }

    public <T> T hGet(String key, String field) {
        RMap<String, T> map = redisson.getMap(key);
        return map.get(field);
    }

    public <T> Map<String, T> hGetAll(String key) {
        RMap<String, T> map = redisson.getMap(key);
        return new HashMap<>(map.readAllMap());
    }

    public boolean hDelete(String key, String... fields) {
        RMap<String, Object> map = redisson.getMap(key);
        return map.fastRemove(fields) > 0;
    }

    public boolean hExists(String key, String field) {
        RMap<String, Object> map = redisson.getMap(key);
        return map.containsKey(field);
    }

    // ==================== List 操作 ====================

    public <T> void lPush(String key, T value) {
        RDeque<T> deque = redisson.getDeque(key);
        deque.addFirst(value);
    }

    public <T> void rPush(String key, T value) {
        RDeque<T> deque = redisson.getDeque(key);
        deque.addLast(value);
    }

    public <T> T lPop(String key) {
        RDeque<T> deque = redisson.getDeque(key);
        return deque.pollFirst();
    }

    public <T> T rPop(String key) {
        RDeque<T> deque = redisson.getDeque(key);
        return deque.pollLast();
    }

    public <T> List<T> lRange(String key, int start, int end) {
        RList<T> list = redisson.getList(key);
        return list.range(start, end);
    }

    public long lSize(String key) {
        return redisson.getList(key).size();
    }

    // ==================== Set 操作 ====================

    @SafeVarargs
    public final <T> void sAdd(String key, T... values) {
        RSet<T> set = redisson.getSet(key);
        set.addAll(Arrays.asList(values));
    }

    public <T> Set<T> sMembers(String key) {
        RSet<T> set = redisson.getSet(key);
        return set.readAll();
    }

    public <T> boolean sIsMember(String key, T value) {
        RSet<T> set = redisson.getSet(key);
        return set.contains(value);
    }

    public <T> boolean sRemove(String key, T value) {
        RSet<T> set = redisson.getSet(key);
        return set.remove(value);
    }

    // ==================== ZSet 有序集合 ====================

    public <T> void zAdd(String key, T value, double score) {
        RScoredSortedSet<T> zset = redisson.getScoredSortedSet(key);
        zset.add(score, value);
    }

    public <T> Collection<T> zRange(String key, int start, int end) {
        RScoredSortedSet<T> zset = redisson.getScoredSortedSet(key);
        return zset.valueRange(start, end);
    }

    public <T> Double zScore(String key, T value) {
        RScoredSortedSet<T> zset = redisson.getScoredSortedSet(key);
        return zset.getScore(value);
    }

    // ==================== 分布式锁 ====================

    /**
     * 获取锁（阻塞式，默认等待3秒，持有10秒）
     *
     * @param lockKey 锁的 Key
     * @return 是否获取成功
     */
    public boolean lock(String lockKey) {
        return lock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME);
    }

    /**
     * 获取锁（阻塞式）
     *
     * @param lockKey   锁的 Key
     * @param waitTime  最大等待时间（秒）
     * @param leaseTime 持有时间（秒），-1 表示自动续期
     */
    public boolean lock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redisson.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        RLock lock = redisson.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 带锁执行（自动获取和释放锁）
     *
     * @param lockKey  锁的 Key
     * @param supplier 需要执行的业务逻辑
     * @return 业务执行结果，获取锁失败返回 null
     */
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

    /**
     * 带锁执行（无返回值版本）
     */
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

    // ==================== 限流器 ====================

    /**
     * 尝试获取令牌（限流）
     *
     * @param key      限流器标识
     * @param rate     速率（每个时间窗口允许的请求数）
     * @param interval 时间窗口（秒）
     * @return 是否允许通过
     */
    public boolean tryAcquire(String key, long rate, long interval) {
        RRateLimiter limiter = redisson.getRateLimiter(key);

        // 首次使用时初始化（幂等操作）
        limiter.trySetRate(
                RateType.OVERALL,
                rate,
                Duration.ofSeconds(interval),
                Duration.ofSeconds(interval)
        );

        return limiter.tryAcquire(1);
    }

    /**
     * 接口限流（默认每秒10次）
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 10, 1);
    }

    // ==================== 布隆过滤器 ====================

    /**
     * 初始化布隆过滤器
     *
     * @param key                过滤器标识
     * @param expectedInsertions 预期插入数量
     * @param falseProbability   误判率（如 0.01 表示 1%）
     */
    public void initBloomFilter(String key, long expectedInsertions, double falseProbability) {
        RBloomFilter<Object> filter = redisson.getBloomFilter(key);
        filter.tryInit(expectedInsertions, falseProbability);
    }

    /**
     * 布隆过滤器添加元素
     */
    public <T> boolean bloomAdd(String key, T value) {
        RBloomFilter<T> filter = redisson.getBloomFilter(key);
        return filter.add(value);
    }

    /**
     * 布隆过滤器判断元素是否存在
     * <p>
     * 注意：返回 true 表示可能存在，返回 false 表示一定不存在
     */
    public <T> boolean bloomContains(String key, T value) {
        RBloomFilter<T> filter = redisson.getBloomFilter(key);
        return filter.contains(value);
    }

    // ==================== 发布订阅 ====================

    /**
     * 发布消息
     */
    public <T> void publish(String channel, T message) {
        RTopic topic = redisson.getTopic(channel);
        topic.publish(message);
    }

    /**
     * 订阅消息
     *
     * @param channel  频道
     * @param type     消息类型
     * @param listener 消息监听器
     * @return 监听器 ID（用于取消订阅）
     */
    public <T> int subscribe(String channel, Class<T> type, MessageListener<T> listener) {
        RTopic topic = redisson.getTopic(channel);
        return topic.addListener(type, listener);
    }

    // ==================== 延迟队列 ====================

    /**
     * 添加延迟任务
     *
     * @param queueName    队列名称
     * @param value        任务数据
     * @param delaySeconds 延迟时间（秒）
     */
    public <T> void addDelayedTask(String queueName, T value, long delaySeconds) {
        String zsetKey = queueName + ":delay";
        long executeAt = System.currentTimeMillis() + delaySeconds * 1000;

        RScoredSortedSet<T> delayZSet = redisson.getScoredSortedSet(zsetKey);
        delayZSet.add(executeAt, value);
    }

    /**
     * 获取延迟队列（阻塞获取）
     */
    public <T> T takeDelayedTask(String queueName) throws InterruptedException {
        RBlockingQueue<T> blockingQueue = redisson.getBlockingQueue(queueName);
        return blockingQueue.take();
    }

    /**
     * 获取延迟队列（非阻塞）
     */
    public <T> T pollDelayedTask(String queueName) {
        RBlockingQueue<T> blockingQueue = redisson.getBlockingQueue(queueName);
        return blockingQueue.poll();
    }
}