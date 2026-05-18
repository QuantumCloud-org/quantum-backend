package com.alpha.cache.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalCacheClient 单元测试")
class LocalCacheClientTest {

    private LocalCacheClient cache;

    @BeforeEach
    void setUp() {
        cache = new LocalCacheClient();
    }

    // ==================== KV 基础操作 ====================

    @Test
    @DisplayName("set/get 基本读写")
    void testSetGet() {
        cache.set("k1", "hello");
        assertThat(cache.<String>get("k1")).isEqualTo("hello");
    }

    @Test
    @DisplayName("get 不存在的 key 返回 null")
    void testGetMissing() {
        assertThat(cache.<String>get("missing")).isNull();
    }

    @Test
    @DisplayName("delete 后 get 返回 null")
    void testDelete() {
        cache.set("k1", "v1");
        cache.delete("k1");
        assertThat(cache.<String>get("k1")).isNull();
    }

    @Test
    @DisplayName("exists 判断 key 是否存在")
    void testExists() {
        cache.set("k1", "v1");
        assertThat(cache.exists("k1")).isTrue();
        assertThat(cache.exists("missing")).isFalse();
    }

    @Test
    @DisplayName("getOrDefault 缺失时返回默认值")
    void testGetOrDefault() {
        assertThat(cache.getOrDefault("missing", "default")).isEqualTo("default");
        cache.set("k1", "v1");
        assertThat(cache.getOrDefault("k1", "default")).isEqualTo("v1");
    }

    @Test
    @DisplayName("getExpire 返回剩余秒数")
    void testGetExpire() {
        cache.set("k1", "v1", Duration.ofMinutes(10));
        long ttl = cache.getExpire("k1");
        assertThat(ttl).isBetween(590L, 600L);
    }

    @Test
    @DisplayName("getExpire 无过期 key 返回 -1")
    void testGetExpireNoKey() {
        assertThat(cache.getExpire("missing")).isEqualTo(-1L);
    }

    @Test
    @DisplayName("expire 重置已有 key 的过期时间")
    void testExpire() {
        cache.set("k1", "v1", Duration.ofSeconds(5));
        boolean ok = cache.expire("k1", Duration.ofMinutes(10));
        assertThat(ok).isTrue();
        assertThat(cache.getExpire("k1")).isBetween(590L, 600L);
    }

    @Test
    @DisplayName("deleteKeysByPattern 通配符删除")
    void testDeleteKeysByPattern() {
        cache.set("user:1", "a");
        cache.set("user:2", "b");
        cache.set("order:1", "c");
        cache.deleteKeysByPattern("user:*");
        assertThat(cache.exists("user:1")).isFalse();
        assertThat(cache.exists("user:2")).isFalse();
        assertThat(cache.exists("order:1")).isTrue();
    }

    // ==================== getAndDelete 原子性 ====================

    @Test
    @DisplayName("getAndDelete 返回值并删除 key")
    void testGetAndDelete() {
        cache.set("token:abc", "value123");
        String v = cache.getAndDelete("token:abc");
        assertThat(v).isEqualTo("value123");
        assertThat(cache.exists("token:abc")).isFalse();
    }

    @Test
    @DisplayName("getAndDelete 并发：同一 key 只有一个线程拿到值")
    void testGetAndDeleteConcurrent() throws InterruptedException {
        cache.set("refresh:token", "tokenId-001");

        int threads = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                String v = cache.getAndDelete("refresh:token");
                if (v != null) successCount.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();
        assertThat(successCount.get()).isEqualTo(1);
    }

    // ==================== setIfAbsent ====================

    @Test
    @DisplayName("setIfAbsent key 不存在时写入返回 true")
    void testSetIfAbsentNewKey() {
        boolean ok = cache.setIfAbsent("lock:k", "1", Duration.ofSeconds(30));
        assertThat(ok).isTrue();
        assertThat(cache.<String>get("lock:k")).isEqualTo("1");
    }

    @Test
    @DisplayName("setIfAbsent key 已存在时不覆盖返回 false")
    void testSetIfAbsentExistingKey() {
        cache.set("lock:k", "original", Duration.ofSeconds(30));
        boolean ok = cache.setIfAbsent("lock:k", "new", Duration.ofSeconds(30));
        assertThat(ok).isFalse();
        assertThat(cache.<String>get("lock:k")).isEqualTo("original");
    }

    @Test
    @DisplayName("setIfAbsent 并发：同一 key 只有一个线程写入成功")
    void testSetIfAbsentConcurrent() throws InterruptedException {
        int threads = 20;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                boolean ok = cache.setIfAbsent("concurrent:key", "v", Duration.ofSeconds(10));
                if (ok) successCount.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();
        assertThat(successCount.get()).isEqualTo(1);
    }

    // ==================== Set 操作 ====================

    @Test
    @DisplayName("sAdd / sMembers / sRemove 基本操作")
    void testSetOperations() {
        cache.sAdd("set:1", "a", "b", "c");
        Set<String> members = cache.sMembers("set:1");
        assertThat(members).containsExactlyInAnyOrder("a", "b", "c");

        boolean removed = cache.sRemove("set:1", "b");
        assertThat(removed).isTrue();
        assertThat(cache.<String>sMembers("set:1")).containsExactlyInAnyOrder("a", "c");
    }

    @Test
    @DisplayName("sMembers 空集合返回 emptySet")
    void testsMembersEmpty() {
        assertThat(cache.sMembers("missing:set")).isEmpty();
    }

    // ==================== 原子计数 ====================

    @Test
    @DisplayName("increment 自增")
    void testIncrement() {
        assertThat(cache.increment("counter")).isEqualTo(1L);
        assertThat(cache.increment("counter")).isEqualTo(2L);
        assertThat(cache.increment("counter", 5)).isEqualTo(7L);
    }

    @Test
    @DisplayName("increment + get 结果可读（登录锁定场景验证）")
    void testIncrementVisibleViaGet() {
        cache.increment("fail:user1");
        cache.increment("fail:user1");

        // get() 必须能读到计数器值，否则 checkAccountLock 永远无法触发
        Integer count = cache.get("fail:user1");
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("increment + expire + getExpire 联动（登录锁定 TTL 验证）")
    void testIncrementExpireTtl() {
        cache.increment("fail:user2");
        boolean ok = cache.expire("fail:user2", Duration.ofMinutes(10));
        assertThat(ok).isTrue();
        assertThat(cache.getExpire("fail:user2")).isBetween(590L, 600L);
    }

    @Test
    @DisplayName("delete 同时清除计数器（登录成功后清除失败记录）")
    void testDeleteClearsCounter() {
        cache.increment("fail:user3");
        cache.increment("fail:user3");
        cache.delete("fail:user3");

        // 删除后计数器重置，再次 increment 从 1 开始
        assertThat(cache.increment("fail:user3")).isEqualTo(1L);
    }

    // ==================== 限流 ====================

    @Test
    @DisplayName("tryAcquire 限流：高频请求被限制")
    void testTryAcquire() {
        // 每秒允许 2 次
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(cache.tryAcquire("api:/test", 2, 1));
        }
        // 前 2 次应通过，后续被限流（Guava RateLimiter token bucket，初始有 burst）
        long passed = results.stream().filter(b -> b).count();
        assertThat(passed).isLessThanOrEqualTo(3); // 考虑 burst 容忍
    }

    @Test
    @DisplayName("tryAcquire 不同 key 互不影响")
    void testTryAcquireIsolation() {
        assertThat(cache.tryAcquire("key:A")).isTrue();
        assertThat(cache.tryAcquire("key:B")).isTrue();
    }

    // ==================== getOrLoad ====================

    @Test
    @DisplayName("getOrLoad 缓存未命中时执行 loader")
    void testGetOrLoad() {
        AtomicInteger callCount = new AtomicInteger(0);
        String v1 = cache.getOrLoad("loaded:key", () -> {
            callCount.incrementAndGet();
            return "loaded-value";
        }, Duration.ofMinutes(5));

        String v2 = cache.getOrLoad("loaded:key", () -> {
            callCount.incrementAndGet();
            return "should-not-load";
        }, Duration.ofMinutes(5));

        assertThat(v1).isEqualTo("loaded-value");
        assertThat(v2).isEqualTo("loaded-value");
        assertThat(callCount.get()).isEqualTo(1); // loader 只调用一次
    }

    // ==================== Hash 操作 ====================

    @Test
    @DisplayName("hSet / hGet / hExists / hDelete 基本操作")
    void testHashBasic() {
        cache.hSet("user:1", "name", "Alice");
        cache.hSet("user:1", "age", 30);

        assertThat(cache.<String>hGet("user:1", "name")).isEqualTo("Alice");
        assertThat(cache.<Integer>hGet("user:1", "age")).isEqualTo(30);
        assertThat(cache.hExists("user:1", "name")).isTrue();
        assertThat(cache.hExists("user:1", "missing")).isFalse();

        boolean deleted = cache.hDelete("user:1", "name");
        assertThat(deleted).isTrue();
        assertThat(cache.hExists("user:1", "name")).isFalse();
    }

    @Test
    @DisplayName("hSetAll / hGetAll 批量操作")
    void testHashBatch() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("city", "Beijing");
        fields.put("score", 99.5);
        cache.hSetAll("profile:1", fields);

        Map<String, Object> all = cache.hGetAll("profile:1");
        assertThat(all).containsEntry("city", "Beijing").containsEntry("score", 99.5);
    }

    @Test
    @DisplayName("hGet 不存在的 key 返回 null")
    void testHashMissing() {
        assertThat(cache.<String>hGet("no:key", "field")).isNull();
        assertThat(cache.<String>hGetAll("no:key")).isEmpty();
    }

    // ==================== List 操作 ====================

    @Test
    @DisplayName("lPush / rPush / lPop / rPop 基本操作")
    void testListBasic() {
        cache.rPush("list:1", "a");
        cache.rPush("list:1", "b");
        cache.rPush("list:1", "c");

        assertThat(cache.lSize("list:1")).isEqualTo(3L);
        assertThat(cache.<String>lPop("list:1")).isEqualTo("a");
        assertThat(cache.<String>rPop("list:1")).isEqualTo("c");
        assertThat(cache.lSize("list:1")).isEqualTo(1L);
    }

    @Test
    @DisplayName("lRange 返回指定范围元素")
    void testListRange() {
        for (int i = 1; i <= 5; i++) cache.rPush("list:2", "v" + i);

        assertThat(cache.<String>lRange("list:2", 0, 2)).containsExactly("v1", "v2", "v3");
        assertThat(cache.<String>lRange("list:2", 0, -1)).containsExactly("v1", "v2", "v3", "v4", "v5");
        assertThat(cache.<String>lRange("list:2", -2, -1)).containsExactly("v4", "v5");
    }

    @Test
    @DisplayName("lPop 空 list 返回 null")
    void testListPopEmpty() {
        assertThat(cache.<String>lPop("missing:list")).isNull();
    }

    // ==================== ZSet 有序集合 ====================

    @Test
    @DisplayName("zAdd / zRange / zScore / zRemove / zSize 基本操作")
    void testZSetBasic() {
        cache.zAdd("rank", "Alice", 100.0);
        cache.zAdd("rank", "Bob", 80.0);
        cache.zAdd("rank", "Carol", 90.0);

        assertThat(cache.zSize("rank")).isEqualTo(3L);
        assertThat(cache.zScore("rank", "Alice")).isEqualTo(100.0);

        // zRange 按 score 升序：Bob(80) < Carol(90) < Alice(100)
        assertThat(cache.<String>zRange("rank", 0, -1))
                .containsExactly("Bob", "Carol", "Alice");

        boolean removed = cache.zRemove("rank", "Bob");
        assertThat(removed).isTrue();
        assertThat(cache.zSize("rank")).isEqualTo(2L);
    }

    @Test
    @DisplayName("zRangeByScore 按 score 区间返回元素")
    void testZSetRangeByScore() {
        cache.zAdd("scores", "p1", 1.0);
        cache.zAdd("scores", "p2", 3.0);
        cache.zAdd("scores", "p3", 5.0);
        cache.zAdd("scores", "p4", 7.0);

        assertThat(cache.<String>zRangeByScore("scores", 2.0, 6.0))
                .containsExactly("p2", "p3");
    }

    @Test
    @DisplayName("zScore 不存在的元素返回 null")
    void testZSetScoreMissing() {
        assertThat(cache.zScore("empty:zset", "nobody")).isNull();
    }

    // ==================== sIsMember ====================

    @Test
    @DisplayName("sIsMember 成员判断")
    void testSIsMember() {
        cache.sAdd("fruits", "apple", "banana");
        assertThat(cache.sIsMember("fruits", "apple")).isTrue();
        assertThat(cache.sIsMember("fruits", "grape")).isFalse();
        assertThat(cache.sIsMember("missing:set", "x")).isFalse();
    }
}
