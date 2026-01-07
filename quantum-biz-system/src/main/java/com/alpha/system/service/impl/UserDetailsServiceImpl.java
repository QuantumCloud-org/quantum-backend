package com.alpha.system.service.impl;

import com.alpha.framework.entity.LoginUser;
import com.alpha.system.domain.SysUser;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 用户认证服务实现
 * <p>
 * 实现 Spring Security 的 UserDetailsService 接口
 * 由 Spring 自动注入到 SecurityConfig 的 AuthenticationManager
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 2. 检查状态
        if (user.getStatus() == 0) {
            log.warn("用户已禁用: {}", username);
            throw new UsernameNotFoundException("用户已禁用: " + username);
        }

        // 3. 查询角色
        Set<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());

        // 4. 查询权限
        Set<String> permissions;
        if (isAdmin(user.getId())) {
            permissions = Set.of("*:*:*");  // 管理员拥有所有权限
        } else {
            permissions = menuMapper.selectPermsByUserId(user.getId());
        }

        // 5. 查询部门名称
        String deptName = null;
        if (user.getDeptId() != null) {
            deptName = userMapper.selectDeptNameById(user.getDeptId());
        }

        // 6. 构建 LoginUser
        return new LoginUser()
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setNickname(user.getNickname())
                .setPassword(user.getPassword())
                .setDeptId(user.getDeptId())
                .setDeptName(deptName)
                .setStatus(user.getStatus())
                .setRoles(roles)
                .setPermissions(permissions);
    }

    /**
     * 判断是否管理员
     */
    private boolean isAdmin(Long userId) {
        return userId != null && userId == 1L;
    }

}
