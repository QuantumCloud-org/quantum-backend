package com.alpha.system.security;

import java.util.Set;

public record ForceLogoutEvent(Set<Long> userIds) {
    public ForceLogoutEvent {
        userIds = userIds == null ? Set.of() : Set.copyOf(userIds);
    }
}
