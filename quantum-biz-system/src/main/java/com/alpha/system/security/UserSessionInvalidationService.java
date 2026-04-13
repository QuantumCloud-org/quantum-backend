package com.alpha.system.security;

import cn.hutool.core.collection.CollUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.security.token.TokenService;
import com.alpha.system.mapper.SysRoleMenuMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户会话失效服务
 * <p>
 * 监听用户/角色/菜单变更事件，在事务提交后踢下线受影响用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionInvalidationService {

    private final TokenService tokenService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionInvalidation(SessionInvalidationEvent event) {
        invalidateByUserIds(event.userIds());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleSessionInvalidation(RoleSessionInvalidationEvent event) {
        invalidateByRoleId(event.roleId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMenuSessionInvalidation(MenuSessionInvalidationEvent event) {
        invalidateByMenuId(event.menuId());
    }

    private void invalidateByUserIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Set<Long> unique = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        log.info("会话失效 | 踢下线用户: {}", unique);
        unique.forEach(tokenService::kickOut);
    }

    private void invalidateByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        Set<Long> userIds = filterSuperAdmin(userRoleMapper.selectUserIdsByRoleId(roleId));
        log.info("角色变更会话失效 | roleId: {} | 受影响用户: {}", roleId, userIds);
        invalidateByUserIds(userIds);
    }

    private void invalidateByMenuId(Long menuId) {
        if (menuId == null) {
            return;
        }
        Set<Long> roleIds = roleMenuMapper.selectRoleIdsByMenuId(menuId);
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            Set<Long> roleUserIds = userRoleMapper.selectUserIdsByRoleId(roleId);
            if (CollUtil.isNotEmpty(roleUserIds)) {
                userIds.addAll(roleUserIds);
            }
        }
        userIds = filterSuperAdmin(userIds);
        log.info("菜单变更会话失效 | menuId: {} | 受影响用户: {}", menuId, userIds);
        invalidateByUserIds(userIds);
    }

    private Set<Long> filterSuperAdmin(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Set.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !CommonConstants.SUPER_ADMIN_ID.equals(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
