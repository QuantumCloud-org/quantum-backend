package com.alpha.cache.util;

import com.alpha.cache.constant.CacheKeyConstant;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** In-process cache with bounded storage and per-key expiration. */
@ConditionalOnProperty(name = "cache.mode", havingValue = "local")
public class LocalCacheClient implements CacheClient {

    private final Cache<String, Entry> cache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .scheduler(Scheduler.systemScheduler())
            .expireAfter(new Expiry<String, Entry>() {
                @Override
                public long expireAfterCreate(String key, Entry entry, long now) {
                    return remaining(entry, now);
                }

                @Override
                public long expireAfterUpdate(String key, Entry entry, long now, long duration) {
                    return remaining(entry, now);
                }

                @Override
                public long expireAfterRead(String key, Entry entry, long now, long duration) {
                    return duration;
                }
            })
            .build();

    private final Cache<String, TokenBucket> limiters = Caffeine.newBuilder()
            .maximumSize(50_000)
            .scheduler(Scheduler.systemScheduler())
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    // Generation loss invalidates the affected sessions. Keep it independent of general cache traffic.
    private final Cache<String, Entry> sessionGenerations = Caffeine.newBuilder().maximumSize(50_000).build();

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        store(key).put(key, entry(value, ttl));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Entry entry = store(key).getIfPresent(key);
        return entry == null ? null : (T) entry.value();
    }

    @Override
    public <T> Map<String, T> getAll(Collection<String> keys) {
        Map<String, T> values = new LinkedHashMap<>();
        for (String key : keys) {
            T value = get(key);
            if (value != null) {
                values.put(key, value);
            }
        }
        return values;
    }

    @Override
    public boolean replaceIfPresent(String key, Object value, Duration ttl) {
        return store(key).asMap().computeIfPresent(key, (name, previous) -> new Entry(value, previous.expiresAt())) != null;
    }

    @Override
    public boolean compareAndSet(String key, String expected, String replacement, Duration ttl) {
        Entry next = entry(replacement, ttl);
        return store(key).asMap().computeIfPresent(key, (name, old) -> expected.equals(old.value()) ? next : old) == next;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAndDelete(String key) {
        Entry old = store(key).asMap().remove(key);
        return old == null ? null : (T) old.value();
    }

    @Override
    public void delete(String key) {
        store(key).invalidate(key);
    }

    @Override
    public boolean exists(String key) {
        return store(key).getIfPresent(key) != null;
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        return store(key).asMap().computeIfPresent(key, (name, existing) -> entry(existing.value(), ttl)) != null;
    }

    @Override
    public long getExpire(String key) {
        Entry current = store(key).getIfPresent(key);
        return current == null || current.expiresAt() == Long.MAX_VALUE
                ? -1 : TimeUnit.NANOSECONDS.toSeconds(Math.max(0, current.expiresAt() - System.nanoTime()));
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        return store(key).asMap().putIfAbsent(key, entry(value, ttl)) == null;
    }

    @Override
    public long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public long increment(String key, long delta) {
        Entry updated = cache.asMap().compute(key, (name, old) -> {
            long value = old == null ? delta : ((Number) old.value()).longValue() + delta;
            Number number;
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                number = (int) value;
            } else {
                number = value;
            }
            return new Entry(number, old == null ? Long.MAX_VALUE : old.expiresAt());
        });
        return ((Number) updated.value()).longValue();
    }

    @Override
    @SafeVarargs
    public final <T> void sAdd(String key, T... values) {
        cache.asMap().compute(key, (name, old) -> {
            Entry updated = old == null ? new Entry(ConcurrentHashMap.newKeySet(), Long.MAX_VALUE) : old;
            asSet(updated.value()).addAll(Arrays.asList(values));
            return updated;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key) {
        Object members = get(key);
        return members == null ? Set.of() : Set.copyOf((Set<T>) members);
    }

    @Override
    public <T> boolean sRemove(String key, T value) {
        Set<Object> members = get(key);
        return members != null && members.remove(value);
    }

    @Override
    public <T> void zAdd(String key, T value, double score) {
        cache.asMap().compute(key, (name, old) -> {
            Entry updated = old == null ? new Entry(new ConcurrentHashMap<>(), Long.MAX_VALUE) : old;
            asScores(updated.value()).put(value, score);
            return updated;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> zRangeByScore(String key, double min, double max) {
        Map<Object, Double> scores = get(key);
        if (scores == null) {
            return List.of();
        }
        return scores.entrySet().stream()
                .filter(e -> e.getValue() >= min && e.getValue() <= max)
                .sorted(Map.Entry.comparingByValue())
                .map(e -> (T) e.getKey())
                .toList();
    }

    @Override
    public boolean zRemove(String key, Object value) {
        Map<Object, Double> scores = get(key);
        return scores != null && scores.remove(value) != null;
    }

    @Override
    public long zSize(String key) {
        Map<Object, Double> scores = get(key);
        return scores == null ? 0 : scores.size();
    }

    @Override
    public void zRemoveThrough(String key, double maxScore) {
        Map<Object, Double> scores = get(key);
        if (scores != null) {
            scores.forEach((value, score) -> {
                if (score <= maxScore) {
                    scores.remove(value, score);
                }
            });
        }
    }

    @Override
    public boolean tryAcquire(String key, long rate, long interval) {
        return limiters.get(key, name -> new TokenBucket(rate, Duration.ofSeconds(interval).toNanos()))
                .tryAcquire();
    }

    private static Entry entry(Object value, Duration ttl) {
        return new Entry(value, ttl == null ? Long.MAX_VALUE : System.nanoTime() + ttl.toNanos());
    }

    private Cache<String, Entry> store(String key) {
        return key.startsWith(CacheKeyConstant.AUTH_SESSION_GENERATION) ? sessionGenerations : cache;
    }

    private static long remaining(Entry entry, long now) {
        return entry.expiresAt() == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, entry.expiresAt() - now);
    }

    @SuppressWarnings("unchecked")
    private static Set<Object> asSet(Object value) {
        return (Set<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Double> asScores(Object value) {
        return (Map<Object, Double>) value;
    }

    private record Entry(Object value, long expiresAt) {
    }

    private static final class TokenBucket {
        private final long rate;
        private final long windowNanos;
        private long remaining;
        private long startedAt;

        private TokenBucket(long rate, long windowNanos) {
            this.rate = rate;
            this.windowNanos = windowNanos;
            this.remaining = rate;
            this.startedAt = System.nanoTime();
        }

        private synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            if (now - startedAt >= windowNanos) {
                startedAt = now;
                remaining = rate;
            }
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}
