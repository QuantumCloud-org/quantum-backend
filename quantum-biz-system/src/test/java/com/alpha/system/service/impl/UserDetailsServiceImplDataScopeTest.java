package com.alpha.system.service.impl;

import com.alpha.framework.entity.LoginUser;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.system.domain.SysRole;
import com.alpha.system.domain.SysUser;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplDataScopeTest {

    private SysUserMapper userMapper;
    private SysRoleMapper roleMapper;
    private SysMenuMapper menuMapper;
    private SysDeptMapper deptMapper;
    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        deptMapper = mock(SysDeptMapper.class);
        service = new UserDetailsServiceImpl(userMapper, roleMapper, menuMapper, deptMapper);
    }

    @Test
    void loadUserShouldResolveEffectiveDataScopeFromRolesInsteadOfUserColumn() {
        // 用户列写 ALL, 但有效范围应由角色聚合 (DEPT_AND_CHILD ∪ CUSTOM)
        stubUser(user(2L, "alice", 10L, DataScopeType.ALL));
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of(
                role(2L, "dept_child", DataScopeType.DEPT_AND_CHILD),
                role(3L, "custom", DataScopeType.CUSTOM)
        ));
        when(deptMapper.selectChildDeptIds(10L)).thenReturn(Set.of(10L, 11L));
        when(deptMapper.selectDeptIdsByRoleIds(List.of(3L))).thenReturn(Set.of(20L));

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("alice");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.CUSTOM.getCode());
        assertThat(loginUser.getDeptIds()).containsExactlyInAnyOrder(10L, 11L, 20L);
        assertThat(loginUser.getRoles()).containsExactlyInAnyOrder("dept_child", "custom");
    }

    @Test
    void superAdminShouldAlwaysGetAllScope() {
        stubUser(user(1L, "admin", 10L, DataScopeType.SELF));
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of());

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("admin");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.ALL.getCode());
        assertThat(loginUser.getDeptIds()).isEmpty();
        assertThat(loginUser.getPermissions()).containsExactly("*:*:*");
    }

    @Test
    void allScopeRoleShouldShortCircuitAggregation() {
        stubUser(user(2L, "alice", 10L, DataScopeType.SELF));
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of(
                role(2L, "all", DataScopeType.ALL),
                role(3L, "dept", DataScopeType.DEPT)
        ));

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("alice");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.ALL.getCode());
        assertThat(loginUser.getDeptIds()).isEmpty();
    }

    @Test
    void deptScopeRoleShouldGrantOwnDeptOnly() {
        stubUser(user(2L, "alice", 10L, DataScopeType.SELF));
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of(
                role(2L, "dept", DataScopeType.DEPT)
        ));

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("alice");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.CUSTOM.getCode());
        assertThat(loginUser.getDeptIds()).containsExactly(10L);
    }

    @Test
    void selfScopeRolesShouldFailClosedToSelf() {
        stubUser(user(2L, "alice", 10L, DataScopeType.ALL));
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of(
                role(2L, "self", DataScopeType.SELF)
        ));

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("alice");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.SELF.getCode());
        assertThat(loginUser.getDeptIds()).isEmpty();
    }

    @Test
    void noRolesShouldFallbackToUserColumnScope() {
        stubUser(user(2L, "alice", 10L, DataScopeType.DEPT_AND_CHILD));
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of());
        when(deptMapper.selectChildDeptIds(10L)).thenReturn(Set.of(10L, 11L, 12L));

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("alice");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.CUSTOM.getCode());
        assertThat(loginUser.getDeptIds()).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void noRolesAndNoUserScopeShouldFailClosedToSelf() {
        SysUser user = user(2L, "alice", 10L, null);
        user.setDataScope(null);
        stubUser(user);
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of());

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("alice");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.SELF.getCode());
        assertThat(loginUser.getDeptIds()).isEmpty();
    }

    private void stubUser(SysUser user) {
        when(userMapper.selectByUsername(user.getUsername())).thenReturn(user);
        when(userMapper.selectDeptNameById(user.getDeptId())).thenReturn("研发部");
        when(menuMapper.selectPermsByUserId(user.getId())).thenReturn(Set.of("system:user:list"));
    }

    private SysUser user(Long id, String username, Long deptId, DataScopeType dataScopeType) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encoded");
        user.setDeptId(deptId);
        user.setStatus(1);
        user.setDataScope(dataScopeType == null ? null : dataScopeType.getCode());
        return user;
    }

    private SysRole role(Long id, String roleKey, DataScopeType dataScopeType) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleKey(roleKey);
        role.setDataScope(dataScopeType.getCode());
        role.setStatus(1);
        return role;
    }
}
