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
                        return statOf(name, mlc, false);
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
        var cache = cacheManager.getCacheNames().contains(name) ? cacheManager.getCache(name) : null;
        if (cache instanceof MultiLevelCacheConfig.MultiLevelCache mlc) {
            return statOf(name, mlc, true);
        }
        return Map.of("error", "Cache not found: " + name);
    }

    private Map<String, Object> statOf(String name, MultiLevelCacheConfig.MultiLevelCache cache, boolean detailed) {
        var stats = cache.getLocalCache().stats();
        var row = new HashMap<String, Object>();
        row.put("name", name);
        row.put("hitCount", stats.hitCount());
        row.put("missCount", stats.missCount());
        row.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
        row.put("size", cache.getLocalCache().estimatedSize());
        if (detailed) {
            row.put("evictionCount", stats.evictionCount());
            row.put("loadCount", stats.loadCount());
        }
        return row;
    }
}
