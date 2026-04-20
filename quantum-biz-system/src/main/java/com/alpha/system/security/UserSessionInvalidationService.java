package com.alpha.system.security;

import cn.hutool.core.collection.CollUtil;
import com.alpha.security.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户会话事件处理器
 * <p>
 * - UserCacheRefreshEvent: 资料/角色/部门/菜单变更 → 刷新 Redis LoginUser 缓存，用户无感
 * - ForceLogoutEvent: 改密码/删除/禁用 → 踢下线，要求重新登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionInvalidationService {

    private final TokenService tokenService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCacheRefresh(UserCacheRefreshEvent event) {
        if (CollUtil.isEmpty(event.userIds())) return;
        log.info("用户缓存刷新 | userIds: {}", event.userIds());
        event.userIds().forEach(tokenService::refreshUserCache);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onForceLogout(ForceLogoutEvent event) {
        if (CollUtil.isEmpty(event.userIds())) return;
        log.info("强制下线 | userIds: {}", event.userIds());
        event.userIds().forEach(tokenService::kickOut);
    }
}
