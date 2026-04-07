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

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

        if (user.getStatus() == 0) {
            log.warn("用户已禁用: {}", username);
            throw new UsernameNotFoundException("用户已禁用: " + username);
        }

        Set<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());

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
                .setDataScope(user.getDataScope())
                .setDeptIds(resolveDeptIds(user));
    }

    private boolean isAdmin(Long userId) {
        return CommonConstants.SUPER_ADMIN_ID.equals(userId);
    }

    private Set<Long> resolveDeptIds(SysUser user) {
        if (user.getDeptId() == null) {
            return Collections.emptySet();
        }

        DataScopeType dataScopeType = DataScopeType.fromCode(user.getDataScope());
        return switch (dataScopeType) {
            case DEPT -> Set.of(user.getDeptId());
            case DEPT_AND_CHILD -> {
                Set<Long> deptIds = deptMapper.selectChildDeptIds(user.getDeptId());
                yield deptIds == null || deptIds.isEmpty() ? Set.of(user.getDeptId()) : deptIds;
            }
            case CUSTOM -> resolveCustomDeptIds(user.getId());
            default -> Collections.emptySet();
        };
    }

    private Set<Long> resolveCustomDeptIds(Long userId) {
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> roleIds = roles.stream()
                .filter(role -> role.getDataScope() != null && DataScopeType.CUSTOM.getCode() == role.getDataScope())
                .map(SysRole::getId)
                .toList();
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> deptIds = deptMapper.selectDeptIdsByRoleIds(roleIds);
        return deptIds == null ? Collections.emptySet() : deptIds;
    }
}
