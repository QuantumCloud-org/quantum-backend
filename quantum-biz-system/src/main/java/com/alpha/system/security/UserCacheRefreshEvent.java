package com.alpha.system.security;

import java.util.Set;

public record UserCacheRefreshEvent(Set<Long> userIds) {
    public UserCacheRefreshEvent {
        userIds = userIds == null ? Set.of() : Set.copyOf(userIds);
    }
}
