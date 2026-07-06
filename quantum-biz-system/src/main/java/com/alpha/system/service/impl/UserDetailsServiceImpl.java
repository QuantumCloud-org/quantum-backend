package com.alpha.system.service.impl;

import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.system.domain.SysRole;
import com.alpha.system.domain.SysUser;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<SysRole> assignedRoles = safeRoles(roleMapper.selectRolesByUserId(user.getId()));
        Set<String> roles = assignedRoles.stream()
                .map(SysRole::getRoleKey)
                .filter(roleKey -> roleKey != null && !roleKey.isBlank())
                .collect(Collectors.toSet());

        Set<String> permissions;
        if (isAdmin(user.getId())) {
            permissions = Set.of("*:*:*");
        } else {
            permissions = menuMapper.selectPermsByUserId(user.getId());
        }

        String deptName = null;
        if (user.getDeptId() != null) {
            deptName = userMapper.selectDeptNameById(user.getDeptId());
        }

        DataScopeResolution dataScope = resolveDataScope(user, assignedRoles);

        return new LoginUser()
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setNickname(user.getNickname())
                .setPassword(user.getPassword())
                .setDeptId(user.getDeptId())
                .setDeptName(deptName)
                .setStatus(user.getStatus())
                .setRoles(roles)
                .setPermissions(permissions)
                .setDataScope(dataScope.type().getCode())
                .setDeptIds(dataScope.deptIds());
    }

    private boolean isAdmin(Long userId) {
        return CommonConstants.SUPER_ADMIN_ID.equals(userId);
    }

    private List<SysRole> safeRoles(List<SysRole> roles) {
        return roles == null ? Collections.emptyList() : roles;
    }

    private DataScopeResolution resolveDataScope(SysUser user, List<SysRole> assignedRoles) {
        if (isAdmin(user.getId())) {
            return new DataScopeResolution(DataScopeType.ALL, Collections.emptySet());
        }

        if (assignedRoles.isEmpty()) {
            return resolveFallbackUserScope(user);
        }

        Set<Long> deptIds = new LinkedHashSet<>();
        List<Long> customRoleIds = new ArrayList<>();
        for (SysRole role : assignedRoles) {
            DataScopeType roleScope = DataScopeType.fromCode(role.getDataScope());
            switch (roleScope) {
                case ALL -> {
                    return new DataScopeResolution(DataScopeType.ALL, Collections.emptySet());
                }
                case DEPT -> addDeptId(deptIds, user.getDeptId());
                case DEPT_AND_CHILD -> deptIds.addAll(resolveDeptAndChildren(user.getDeptId()));
                case CUSTOM -> customRoleIds.add(role.getId());
                default -> {
                    // SELF/DEFAULT do not add cross-user departments.
                }
            }
        }

        deptIds.addAll(resolveCustomDeptIds(customRoleIds));
        if (!deptIds.isEmpty()) {
            return new DataScopeResolution(DataScopeType.CUSTOM, deptIds);
        }
        return new DataScopeResolution(DataScopeType.SELF, Collections.emptySet());
    }

    private DataScopeResolution resolveFallbackUserScope(SysUser user) {
        DataScopeType fallback = DataScopeType.fromCode(user.getDataScope());
        return switch (fallback) {
            case ALL -> new DataScopeResolution(DataScopeType.ALL, Collections.emptySet());
            case DEPT -> new DataScopeResolution(DataScopeType.CUSTOM, user.getDeptId() == null ? Collections.emptySet() : Set.of(user.getDeptId()));
            case DEPT_AND_CHILD -> new DataScopeResolution(DataScopeType.CUSTOM, resolveDeptAndChildren(user.getDeptId()));
            default -> new DataScopeResolution(DataScopeType.SELF, Collections.emptySet());
        };
    }

    private Set<Long> resolveDeptAndChildren(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = deptMapper.selectChildDeptIds(deptId);
        return deptIds == null || deptIds.isEmpty() ? Set.of(deptId) : deptIds;
    }

    private Set<Long> resolveCustomDeptIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = deptMapper.selectDeptIdsByRoleIds(roleIds);
        return deptIds == null ? Collections.emptySet() : deptIds;
    }

    private void addDeptId(Set<Long> deptIds, Long deptId) {
        if (deptId != null) {
            deptIds.add(deptId);
        }
    }

    private record DataScopeResolution(DataScopeType type, Set<Long> deptIds) {
    }
}
