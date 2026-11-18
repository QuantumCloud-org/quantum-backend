package com.alpha.security.token;

import cn.hutool.core.util.IdUtil;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.cache.util.CacheClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/** Shared revocation authority for Web sessions and MCP grants. */
@Component
@RequiredArgsConstructor
public class SessionGeneration {
    private final CacheClient cache;

    /** Establishes the generation before checking credentials for a new login. */
    public String open(Long userId) {
        String key = key(userId);
        String current = cache.get(key);
        if (current != null) {
            return current;
        }
        String candidate = IdUtil.fastSimpleUUID();
        return cache.setIfAbsent(key, candidate, null) ? candidate : cache.get(key);
    }

    /** A missing generation invalidates the grant, including after cache eviction. */
    public boolean matches(Long userId, String generation) {
        return userId != null && generation != null && generation.equals(cache.<String>get(key(userId)));
    }

    Map<String, String> current(Collection<Long> userIds) {
        return cache.getAll(userIds.stream().map(SessionGeneration::key).toList());
    }

    /** Future logins use a newly generated value, so older credentials remain invalid. */
    public void revoke(Long userId) {
        cache.delete(key(userId));
    }

    static String key(Long userId) {
        return CacheKeyConstant.AUTH_SESSION_GENERATION + userId;
    }
}
