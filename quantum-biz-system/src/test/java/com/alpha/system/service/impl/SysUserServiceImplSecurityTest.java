package com.alpha.system.service.impl;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.BizException;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.security.config.SecurityProperties;
import com.alpha.system.domain.SysUser;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.UserCacheRefreshEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserServiceImplSecurityTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void selectUserByIdShouldRejectOutOfScopeUser() {
        SysUserServiceImpl service = spy(newService(mock(SysUserMapper.class), mock(ApplicationEventPublisher.class)));
        doReturn(user(2L, "bob", 99L)).when(service).getById(2L);
        UserContext.setUser(operator());

        assertThatThrownBy(() -> service.selectUserById(2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void importUsersShouldRejectOutOfScopeUpdate() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserServiceImpl service = spy(newService(userMapper, mock(ApplicationEventPublisher.class)));
        when(userMapper.selectByUsername("bob")).thenReturn(user(2L, "bob", 99L));
        doReturn(true).when(service).updateById(any(SysUser.class));
        UserContext.setUser(operator());

        assertThatThrownBy(() -> service.importUsers(List.of(importUser("bob", "13800000001", "bob@example.com")), true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void importUsersShouldRejectDuplicatePhoneOnCreate() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserServiceImpl service = spy(newService(userMapper, mock(ApplicationEventPublisher.class)));
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(userMapper.checkUsernameExists("alice")).thenReturn(0);
        when(userMapper.checkPhoneExists("13800000001", 0L)).thenReturn(1);
        doReturn(true).when(service).save(any(SysUser.class));
        UserContext.setUser(operator());

        assertThatThrownBy(() -> service.importUsers(List.of(importUser("alice", "13800000001", "alice@example.com")), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("手机号已存在");
    }

    @Test
    void importUsersShouldPublishCacheRefreshEventAfterUpdate() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        SysUserServiceImpl service = spy(newService(userMapper, publisher));
        when(userMapper.selectByUsername("bob")).thenReturn(user(2L, "bob", 10L));
        when(userMapper.checkPhoneExists("13800000001", 2L)).thenReturn(0);
        when(userMapper.checkEmailExists("bob@example.com", 2L)).thenReturn(0);
        doReturn(true).when(service).updateById(any(SysUser.class));
        UserContext.setUser(operator());

        service.importUsers(List.of(importUser("bob", "13800000001", "bob@example.com")), true);

        verify(publisher).publishEvent(new UserCacheRefreshEvent(Set.of(2L)));
    }

    private SysUserServiceImpl newService(SysUserMapper userMapper, ApplicationEventPublisher publisher) {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        SecurityProperties properties = new SecurityProperties();
        return new SysUserServiceImpl(
                userMapper,
                mock(SysDeptMapper.class),
                mock(SysUserRoleMapper.class),
                passwordEncoder,
                properties,
                publisher
        );
    }

    private LoginUser operator() {
        return new LoginUser()
                .setUserId(10L)
                .setUsername("operator")
                .setDeptId(10L)
                .setDeptIds(Set.of(10L))
                .setDataScope(DataScopeType.DEPT_AND_CHILD.getCode());
    }

    private SysUser user(Long id, String username, Long deptId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setVersion(1L);
        user.setDeptId(deptId);
        return user;
    }

    private SysUser importUser(String username, String phone, String email) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setDeptId(10L);
        user.setStatus(1);
        return user;
    }
}
