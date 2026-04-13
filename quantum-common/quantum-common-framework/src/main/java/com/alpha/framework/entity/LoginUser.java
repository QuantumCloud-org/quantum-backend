package com.alpha.framework.entity;

import com.alpha.framework.constant.CommonConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户信息
 * <p>
 * 位于 framework 模块，作为全系统通用的用户模型
 */
@Data
@Accessors(chain = true)
public class LoginUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码 (序列化时忽略)
     */
    @JsonIgnore
    private String password;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;

    /**
     * 角色集合
     */
    private Set<String> roles;

    /**
     * 权限集合
     */
    private Set<String> permissions;

    /**
     * 可访问的部门 ID 集合（数据权限使用）
     */
    private Set<Long> deptIds;

    /**
     * 数据权限类型
     */
    private Integer dataScope;

    /**
     * Access Token ID（不存储完整 JWT，仅存 tokenId 用于 Redis key 关联）
     */
    private String tokenId;

    /**
     * Refresh Token ID（不存储完整 JWT，仅存 refreshTokenId 用于注销时清理）
     */
    private String refreshTokenId;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 登录 IP
     */
    private String loginIp;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;


    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return CommonConstants.SUPER_ADMIN_ID.equals(userId);
    }

    /**
     * 是否拥有角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 是否拥有权限
     */
    public boolean hasPermission(String permission) {
        if (isAdmin()) {
            return true;
        }
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 是否拥有任意权限
     */
    public boolean hasAnyPermission(String... permissions) {
        if (isAdmin()) {
            return true;
        }
        if (this.permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    // ==================== UserDetails 接口实现 ====================

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        // 账号锁定由 Redis 登录失败计数管理 (LoginServiceImpl.checkAccountLock)，不靠 status 字段
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return status != null && status == 1;
    }

}
