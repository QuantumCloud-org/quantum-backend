package com.alpha.system.security;

import java.util.Set;

public record SessionInvalidationEvent(Set<Long> userIds) {
    public SessionInvalidationEvent {
        userIds = userIds == null ? Set.of() : Set.copyOf(userIds);
    }
}
