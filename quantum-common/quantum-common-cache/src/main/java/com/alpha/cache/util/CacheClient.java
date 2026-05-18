package com.alpha.cache.util;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 缓存客户端统一接口
 * <p>
 * 两个实现通过 {@code cache.mode} 属性自动切换：
 * <ul>
 *   <li>{@code cache.mode=redis}（默认）→ {@link RedisUtil}，L1 Caffeine + L2 Redis 双级缓存</li>
 *   <li>{@code cache.mode=local} → {@link LocalCacheClient}，纯内存，无需 Redis</li>
 * </ul>
 *
 * <b>不在此接口中的能力</b>（Redis 独有，无合理本地替代）：
 * 分布式锁、Pub/Sub、布隆过滤器、延迟队列 → 请直接注入 {@link RedisUtil}。
 */
public interface CacheClient {

    // ==================== KV ====================

    <T> void set(String key, T value);

    <T> void set(String key, T value, Duration expire);

    <T> void set(String key, T value, long expireSeconds);

    <T> T get(String key);

    <T> T getOrDefault(String key, T defaultValue);

    <T> T getOrLoad(String key, Supplier<T> loader, Duration expire);

    /** 原子获取并删除，用于一次性消费（如 Refresh Token 防重放）。 */
    <T> T getAndDelete(String key);

    void delete(String key);

    /** 按 glob 模式批量删除（支持 {@code *} 通配符）。 */
    void deleteKeysByPattern(String pattern);

    boolean exists(String key);

    boolean expire(String key, Duration expire);

    /** 返回剩余过期秒数，key 不存在或永不过期返回 -1。 */
    long getExpire(String key);

    /** key 不存在时写入，返回 true 表示写入成功。 */
    boolean setIfAbsent(String key, Object value, Duration expire);

    // ==================== 原子计数 ====================

    long increment(String key);

    long increment(String key, long delta);

    // ==================== Hash ====================

    <T> void hSet(String key, String field, T value);

    /** 批量写入 Hash 字段。 */
    void hSetAll(String key, Map<String, Object> map);

    <T> T hGet(String key, String field);

    <T> Map<String, T> hGetAll(String key);

    boolean hDelete(String key, String... fields);

    boolean hExists(String key, String field);

    // ==================== List ====================

    /** 从列表左侧插入（头部）。 */
    <T> void lPush(String key, T value);

    /** 从列表右侧插入（尾部）。 */
    <T> void rPush(String key, T value);

    /** 从列表左侧弹出（头部），列表为空返回 null。 */
    <T> T lPop(String key);

    /** 从列表右侧弹出（尾部），列表为空返回 null。 */
    <T> T rPop(String key);

    /** 返回 [start, end] 范围内的元素，支持负数索引（-1 表示最后一个）。 */
    <T> List<T> lRange(String key, int start, int end);

    long lSize(String key);

    // ==================== Set ====================

    @SuppressWarnings("unchecked")
    <T> void sAdd(String key, T... values);

    <T> Set<T> sMembers(String key);

    <T> boolean sIsMember(String key, T value);

    <T> boolean sRemove(String key, T value);

    @SuppressWarnings("unchecked")
    <T> boolean sRemove(String key, T... values);

    // ==================== ZSet（有序集合）====================

    /** 添加元素，score 越小排名越靠前。 */
    <T> void zAdd(String key, T value, double score);

    /** 按排名范围返回元素（升序），支持负数索引。 */
    <T> Collection<T> zRange(String key, int start, int end);

    /** 按 score 区间返回元素（升序，闭区间）。 */
    <T> Collection<T> zRangeByScore(String key, double min, double max);

    /** 返回元素的 score，不存在返回 null。 */
    <T> Double zScore(String key, T value);

    /** 移除指定元素，成功返回 true。 */
    boolean zRemove(String key, Object value);

    long zSize(String key);

    // ==================== 限流 ====================

    /**
     * 尝试获取令牌（限流）。
     *
     * @param rate     时间窗口内允许的最大请求数
     * @param interval 时间窗口（秒）
     */
    boolean tryAcquire(String key, long rate, long interval);

    /** 默认每秒 10 次。 */
    boolean tryAcquire(String key);
}
