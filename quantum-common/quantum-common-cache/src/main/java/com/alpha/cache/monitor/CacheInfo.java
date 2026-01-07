package com.alpha.cache.monitor;

import com.alpha.cache.config.MultiLevelCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控端点
 * <p>
 * 访问方式：
 * - GET /actuator/cacheInfo      - 获取所有缓存统计
 * - GET /actuator/cacheInfo/{name} - 获取指定缓存统计
 */
@Component
@Endpoint(id = "cacheInfo")
@RequiredArgsConstructor
public class CacheInfo {

    private final CacheManager cacheManager;

    /**
     * 获取所有缓存统计信息
     */
    @ReadOperation
    public Map<String, Object> stats() {
        var result = new HashMap<String, Object>();
        result.put("cacheNames", cacheManager.getCacheNames());

        var stats = cacheManager.getCacheNames().stream()
                .map(name -> {
                    var cache = cacheManager.getCache(name);
                    if (cache instanceof MultiLevelCacheConfig.MultiLevelCache mlc) {
                        var s = mlc.getLocalCache().stats();
                        return Map.of(
                                "name", name,
                                "hitCount", s.hitCount(),
                                "missCount", s.missCount(),
                                "hitRate", String.format("%.2f%%", s.hitRate() * 100),
                                "size", mlc.getLocalCache().estimatedSize()
                        );
                    }
                    return Map.of("name", name);
                })
                .toList();
        result.put("stats", stats);
        return result;
    }

    /**
     * 获取指定缓存的统计信息
     */
    @ReadOperation
    public Map<String, Object> stat(@Selector String name) {
        var cache = cacheManager.getCache(name);
        if (cache instanceof MultiLevelCacheConfig.MultiLevelCache mlc) {
            var s = mlc.getLocalCache().stats();
            return Map.of(
                    "name", name,
                    "hitCount", s.hitCount(),
                    "missCount", s.missCount(),
                    "hitRate", String.format("%.2f%%", s.hitRate() * 100),
                    "evictionCount", s.evictionCount(),
                    "loadCount", s.loadCount(),
                    "size", mlc.getLocalCache().estimatedSize()
            );
        }
        return Map.of("error", "Cache not found: " + name);
    }
}