package com.alpha.cache.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 本地 Caffeine 缓存实现，用于无 Redis 环境（{@code cache.mode=local}）。
 * <p>
 * - KV + 变长 TTL：Caffeine variable expiry + expiryNanos 辅助追踪
 * - Hash：ConcurrentHashMap&lt;field, value&gt;
 * - List：ConcurrentLinkedDeque（thread-safe 双端队列）
 * - Set：ConcurrentHashMap keySet
 * - ZSet：member→score map，查询时按 score 排序
 * - 原子计数：AtomicLong（独立 counters map，通过 get/expire/delete 统一访问）
 * - 限流：TokenBucket（固定窗口令牌桶，纯 Java 无外部依赖）
 * <p>
 * <b>已知限制</b>：Hash/List/Set/ZSet 的过期时间采用懒惰清理（访问时检查）。
 * 后台定时清理不在当前实现范围内，适用于本地开发/测试环境。
 */
@Slf4j
public class LocalCacheClient implements CacheClient {

    private static final Duration DEFAULT_EXPIRE = Duration.ofMinutes(30);

    /** key → 绝对过期时刻（System.nanoTime 纳秒），所有存储类型共享。无过期时不存在。 */
    private final ConcurrentHashMap<String, Long> expiryNanos = new ConcurrentHashMap<>();

    private final Cache<String, Object> kvCache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfter(new Expiry<String, Object>() {
                @Override
                public long expireAfterCreate(@NonNull String key, @NonNull Object value, long currentTime) {
                    Long abs = expiryNanos.get(key);
                    return abs == null ? Long.MAX_VALUE : Math.max(0L, abs - currentTime);
                }

                @Override
                public long expireAfterUpdate(@NonNull String key, @NonNull Object value,
                                              long currentTime, long currentDuration) {
                    Long abs = expiryNanos.get(key);
                    return abs == null ? Long.MAX_VALUE : Math.max(0L, abs - currentTime);
                }

                @Override
                public long expireAfterRead(@NonNull String key, @NonNull Object value,
                                            long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .removalListener((key, value, cause) -> {
                if (key != null) expiryNanos.remove(key);
            })
            .build();

    /** Hash 存储：redisKey → (field → value) */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> hashStore = new ConcurrentHashMap<>();

    /** List 存储：redisKey → deque（左=头部=lPush端，右=尾部=rPush端） */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Object>> listStore = new ConcurrentHashMap<>();

    /** Set 集合存储：redisKey → 线程安全 Set */
    private final ConcurrentHashMap<String, Set<Object>> setStore = new ConcurrentHashMap<>();

    /** ZSet 存储：redisKey → (member → score) */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Object, Double>> zsetStore = new ConcurrentHashMap<>();

    /**
     * 原子计数器。与 kvCache 相互独立，但共享 expiryNanos 做 TTL 追踪。
     * get/expire/delete/exists 会统一查询两者，保证调用方视角一致。
     */
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /** 限流器 */
    private final ConcurrentHashMap<String, TokenBucket> rateLimiters = new ConcurrentHashMap<>();

    // ==================== 基础 KV ====================

    @Override
    public <T> void set(String key, T value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    @Override
    public <T> void set(String key, T value, Duration expire) {
        recordExpiry(key, expire);
        kvCache.put(key, value);
    }

    @Override
    public <T> void set(String key, T value, long expireSeconds) {
        set(key, value, Duration.ofSeconds(expireSeconds));
    }

    /**
     * 先查 kvCache，再查 counters（返回 Integer 以兼容 `Integer x = cacheClient.get(key)` 调用）。
     * Counter 过期时惰性清理。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object val = kvCache.getIfPresent(key);
        if (val != null) return (T) val;

        AtomicLong counter = counters.get(key);
        if (counter != null) {
            if (isCounterExpired(key)) return null;
            // 以 Integer 返回以兼容 Redis 小整数反序列化行为
            return (T) (Integer) (int) counter.get();
        }
        return null;
    }

    @Override
    public <T> T getOrDefault(String key, T defaultValue) {
        T v = get(key);
        return v != null ? v : defaultValue;
    }

    @Override
    public <T> T getOrLoad(String key, Supplier<T> loader, Duration expire) {
        T v = get(key);
        if (v == null) {
            v = loader.get();
            if (v != null) set(key, v, expire);
        }
        return v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAndDelete(String key) {
        Object value = kvCache.asMap().remove(key);
        expiryNanos.remove(key);
        counters.remove(key);
        return (T) value;
    }

    @Override
    public void delete(String key) {
        kvCache.invalidate(key);
        expiryNanos.remove(key);
        // 清理所有次级存储，保证 delete 语义一致
        counters.remove(key);
        hashStore.remove(key);
        listStore.remove(key);
        setStore.remove(key);
        zsetStore.remove(key);
    }

    @Override
    public void deleteKeysByPattern(String pattern) {
        Pattern p = globToPattern(pattern);
        kvCache.asMap().keySet().removeIf(k -> p.matcher(k).matches());
        expiryNanos.keySet().removeIf(k -> p.matcher(k).matches());
        counters.keySet().removeIf(k -> p.matcher(k).matches());
        hashStore.keySet().removeIf(k -> p.matcher(k).matches());
        listStore.keySet().removeIf(k -> p.matcher(k).matches());
        setStore.keySet().removeIf(k -> p.matcher(k).matches());
        zsetStore.keySet().removeIf(k -> p.matcher(k).matches());
    }

    @Override
    public boolean exists(String key) {
        if (kvCache.getIfPresent(key) != null) return true;
        if (counters.containsKey(key) && !isCounterExpired(key)) return true;
        return hashStore.containsKey(key) || listStore.containsKey(key)
                || setStore.containsKey(key) || zsetStore.containsKey(key);
    }

    @Override
    public boolean expire(String key, Duration expire) {
        boolean existsInKv = kvCache.getIfPresent(key) != null;
        boolean existsInCounters = counters.containsKey(key);
        boolean existsInSecondary = hashStore.containsKey(key) || listStore.containsKey(key)
                || setStore.containsKey(key) || zsetStore.containsKey(key);

        if (!existsInKv && !existsInCounters && !existsInSecondary) return false;
        recordExpiry(key, expire);
        // kvCache 需要 re-put 才能触发 expireAfterUpdate
        Object value = kvCache.getIfPresent(key);
        if (value != null) kvCache.put(key, value);
        return true;
    }

    @Override
    public long getExpire(String key) {
        Long abs = expiryNanos.get(key);
        if (abs == null) return -1L;
        long remaining = abs - System.nanoTime();
        return remaining > 0 ? TimeUnit.NANOSECONDS.toSeconds(remaining) : -1L;
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration expire) {
        boolean[] wrote = {false};
        kvCache.asMap().compute(key, (k, existing) -> {
            if (existing != null) {
                Long abs = expiryNanos.get(k);
                if (abs != null && System.nanoTime() > abs) {
                    recordExpiry(k, expire);
                    wrote[0] = true;
                    return value;
                }
                return existing;
            }
            recordExpiry(k, expire);
            wrote[0] = true;
            return value;
        });
        return wrote[0];
    }

    // ==================== 原子计数 ====================

    @Override
    public long increment(String key) {
        return increment(key, 1L);
    }

    /**
     * 若 key 在 expiryNanos 中已过期，先重置计数器再递增（惰性过期）。
     */
    @Override
    public long increment(String key, long delta) {
        if (isCounterExpired(key)) {
            counters.remove(key);
        }
        return counters.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(delta);
    }

    // ==================== Hash 操作 ====================

    @Override
    public <T> void hSet(String key, String field, T value) {
        hashStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(field, value);
    }

    @Override
    public void hSetAll(String key, Map<String, Object> map) {
        hashStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).putAll(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        if (checkAndCleanExpiredSecondary(key, hashStore)) return null;
        ConcurrentHashMap<String, Object> hash = hashStore.get(key);
        return hash == null ? null : (T) hash.get(field);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hGetAll(String key) {
        if (checkAndCleanExpiredSecondary(key, hashStore)) return Collections.emptyMap();
        ConcurrentHashMap<String, Object> hash = hashStore.get(key);
        return hash == null ? Collections.emptyMap() : (Map<String, T>) new HashMap<>(hash);
    }

    @Override
    public boolean hDelete(String key, String... fields) {
        ConcurrentHashMap<String, Object> hash = hashStore.get(key);
        if (hash == null) return false;
        boolean removed = false;
        for (String field : fields) {
            removed |= hash.remove(field) != null;
        }
        return removed;
    }

    @Override
    public boolean hExists(String key, String field) {
        if (checkAndCleanExpiredSecondary(key, hashStore)) return false;
        ConcurrentHashMap<String, Object> hash = hashStore.get(key);
        return hash != null && hash.containsKey(field);
    }

    // ==================== List 操作 ====================

    @Override
    public <T> void lPush(String key, T value) {
        listStore.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addFirst(value);
    }

    @Override
    public <T> void rPush(String key, T value) {
        listStore.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addLast(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lPop(String key) {
        if (checkAndCleanExpiredSecondary(key, listStore)) return null;
        ConcurrentLinkedDeque<Object> deque = listStore.get(key);
        return deque == null ? null : (T) deque.pollFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T rPop(String key) {
        if (checkAndCleanExpiredSecondary(key, listStore)) return null;
        ConcurrentLinkedDeque<Object> deque = listStore.get(key);
        return deque == null ? null : (T) deque.pollLast();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, int start, int end) {
        if (checkAndCleanExpiredSecondary(key, listStore)) return Collections.emptyList();
        ConcurrentLinkedDeque<Object> deque = listStore.get(key);
        if (deque == null) return Collections.emptyList();

        List<Object> snapshot = new ArrayList<>(deque);
        int size = snapshot.size();
        int s = start < 0 ? Math.max(0, size + start) : start;
        int e = end < 0 ? size + end : Math.min(end, size - 1);

        if (s > e || s >= size) return Collections.emptyList();
        return (List<T>) new ArrayList<>(snapshot.subList(s, e + 1));
    }

    @Override
    public long lSize(String key) {
        if (checkAndCleanExpiredSecondary(key, listStore)) return 0L;
        ConcurrentLinkedDeque<Object> deque = listStore.get(key);
        return deque == null ? 0L : deque.size();
    }

    // ==================== Set 集合 ====================

    @Override
    @SafeVarargs
    public final <T> void sAdd(String key, T... values) {
        setStore.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .addAll(Arrays.asList(values));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key) {
        if (checkAndCleanExpiredSecondary(key, setStore)) return Collections.emptySet();
        Set<Object> s = setStore.get(key);
        return s == null ? Collections.emptySet() : (Set<T>) new HashSet<>(s);
    }

    @Override
    public <T> boolean sIsMember(String key, T value) {
        if (checkAndCleanExpiredSecondary(key, setStore)) return false;
        Set<Object> s = setStore.get(key);
        return s != null && s.contains(value);
    }

    @Override
    public <T> boolean sRemove(String key, T value) {
        Set<Object> s = setStore.get(key);
        return s != null && s.remove(value);
    }

    @Override
    @SafeVarargs
    public final <T> boolean sRemove(String key, T... values) {
        Set<Object> s = setStore.get(key);
        return s != null && s.removeAll(Arrays.asList(values));
    }

    // ==================== ZSet 有序集合 ====================

    @Override
    public <T> void zAdd(String key, T value, double score) {
        zsetStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(value, score);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> zRange(String key, int start, int end) {
        if (checkAndCleanExpiredSecondary(key, zsetStore)) return Collections.emptyList();
        ConcurrentHashMap<Object, Double> scores = zsetStore.get(key);
        if (scores == null) return Collections.emptyList();

        List<Map.Entry<Object, Double>> entries = new ArrayList<>(scores.entrySet());
        entries.sort(Map.Entry.comparingByValue());

        int size = entries.size();
        int s = start < 0 ? Math.max(0, size + start) : start;
        int e = end < 0 ? size + end : Math.min(end, size - 1);

        if (s > e || s >= size) return Collections.emptyList();

        List<T> result = new ArrayList<>();
        for (int i = s; i <= e; i++) {
            result.add((T) entries.get(i).getKey());
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> zRangeByScore(String key, double min, double max) {
        if (checkAndCleanExpiredSecondary(key, zsetStore)) return Collections.emptyList();
        ConcurrentHashMap<Object, Double> scores = zsetStore.get(key);
        if (scores == null) return Collections.emptyList();

        return scores.entrySet().stream()
                .filter(e -> e.getValue() >= min && e.getValue() <= max)
                .sorted(Map.Entry.comparingByValue())
                .map(e -> (T) e.getKey())
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Double zScore(String key, T value) {
        if (checkAndCleanExpiredSecondary(key, zsetStore)) return null;
        ConcurrentHashMap<Object, Double> scores = zsetStore.get(key);
        return scores == null ? null : scores.get(value);
    }

    @Override
    public boolean zRemove(String key, Object value) {
        ConcurrentHashMap<Object, Double> scores = zsetStore.get(key);
        return scores != null && scores.remove(value) != null;
    }

    @Override
    public long zSize(String key) {
        if (checkAndCleanExpiredSecondary(key, zsetStore)) return 0L;
        ConcurrentHashMap<Object, Double> scores = zsetStore.get(key);
        return scores == null ? 0L : scores.size();
    }

    // ==================== 限流 ====================

    @Override
    public boolean tryAcquire(String key, long rate, long interval) {
        return rateLimiters.computeIfAbsent(key, k -> new TokenBucket(rate, interval)).tryAcquire();
    }

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 10, 1);
    }

    // ==================== 内部工具 ====================

    private void recordExpiry(String key, Duration expire) {
        if (expire != null && !expire.isNegative() && !expire.isZero()) {
            expiryNanos.put(key, System.nanoTime() + expire.toNanos());
        } else {
            expiryNanos.remove(key);
        }
    }

    /**
     * 判断 counter key 是否已过期，并惰性清理 counters + expiryNanos。
     */
    private boolean isCounterExpired(String key) {
        Long abs = expiryNanos.get(key);
        if (abs == null) return false;
        if (System.nanoTime() > abs) {
            counters.remove(key);
            expiryNanos.remove(key);
            return true;
        }
        return false;
    }

    /**
     * 对次级存储（Hash/List/Set/ZSet）做惰性过期检查。
     * 若 key 已过期，从该存储中移除并返回 true。
     */
    private <V> boolean checkAndCleanExpiredSecondary(String key, ConcurrentHashMap<String, V> store) {
        Long abs = expiryNanos.get(key);
        if (abs != null && System.nanoTime() > abs) {
            store.remove(key);
            expiryNanos.remove(key);
            return true;
        }
        return false;
    }

    /** 将 Redis glob 模式转为 Java 正则（仅处理 {@code *} 和 {@code ?}）。 */
    private static Pattern globToPattern(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.', '(', ')', '[', ']', '{', '}', '+', '|', '^', '$', '\\' ->
                        sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString());
    }

    /**
     * 纯 Java 固定窗口令牌桶，无外部依赖。
     * 每 intervalSeconds 秒重置为 rate 个令牌。
     */
    private static final class TokenBucket {
        private final long maxTokens;
        private final long intervalNanos;
        private final AtomicLong tokens;
        private final AtomicLong windowStart;

        TokenBucket(long rate, long intervalSeconds) {
            this.maxTokens = rate;
            this.intervalNanos = intervalSeconds * 1_000_000_000L;
            this.tokens = new AtomicLong(rate);
            this.windowStart = new AtomicLong(System.nanoTime());
        }

        boolean tryAcquire() {
            refillIfNeeded();
            return tokens.getAndUpdate(t -> t > 0 ? t - 1 : 0) > 0;
        }

        private void refillIfNeeded() {
            long now = System.nanoTime();
            long start = windowStart.get();
            if (now - start >= intervalNanos && windowStart.compareAndSet(start, now)) {
                tokens.set(maxTokens);
            }
        }
    }
}
