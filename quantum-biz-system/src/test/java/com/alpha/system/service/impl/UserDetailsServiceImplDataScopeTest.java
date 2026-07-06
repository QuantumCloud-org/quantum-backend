package com.alpha.system.service.impl;

import com.alpha.framework.entity.LoginUser;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.system.domain.SysRole;
import com.alpha.system.domain.SysUser;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplDataScopeTest {

    @Test
    void loadUserShouldResolveEffectiveDataScopeFromRolesInsteadOfUserColumn() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userMapper, roleMapper, menuMapper, deptMapper);

        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("alice");
        user.setPassword("encoded");
        user.setDeptId(10L);
        user.setStatus(1);
        user.setDataScope(DataScopeType.ALL.getCode());
        when(userMapper.selectByUsername("alice")).thenReturn(user);
        when(userMapper.selectDeptNameById(10L)).thenReturn("研发部");
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of(
                role(2L, "dept_child", DataScopeType.DEPT_AND_CHILD),
                role(3L, "custom", DataScopeType.CUSTOM)
        ));
        when(menuMapper.selectPermsByUserId(2L)).thenReturn(Set.of("system:user:list"));
        when(deptMapper.selectChildDeptIds(10L)).thenReturn(Set.of(10L, 11L));
        when(deptMapper.selectDeptIdsByRoleIds(List.of(3L))).thenReturn(Set.of(20L));

        LoginUser loginUser = (LoginUser) service.loadUserByUsername("alice");

        assertThat(loginUser.getDataScope()).isEqualTo(DataScopeType.CUSTOM.getCode());
        assertThat(loginUser.getDeptIds()).containsExactlyInAnyOrder(10L, 11L, 20L);
        assertThat(loginUser.getRoles()).containsExactlyInAnyOrder("dept_child", "custom");
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
