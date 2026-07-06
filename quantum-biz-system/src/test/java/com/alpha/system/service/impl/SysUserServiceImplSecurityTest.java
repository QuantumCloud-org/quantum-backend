package com.alpha.system.service.impl;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.BizException;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.security.config.SecurityProperties;
import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.UserImportRequest;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.UserCacheRefreshEvent;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    void importUsersShouldRejectInvalidPhoneByCreateContract() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserServiceImpl service = spy(newService(userMapper, userRoleMapper, mock(ApplicationEventPublisher.class)));
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        UserContext.setUser(operator());

        assertThatThrownBy(() -> service.importUsers(List.of(importRequest("alice", "bad-phone", "alice@example.com", 10L, List.of(2L))), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("手机号格式不正确");

        verify(service, never()).save(any(SysUser.class));
        verify(userRoleMapper, never()).batchInsert(any(), any());
    }

    @Test
    void importUsersShouldPersistRoleRelationsOnCreate() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserServiceImpl service = spy(newService(userMapper, userRoleMapper, mock(ApplicationEventPublisher.class)));
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(userMapper.checkUsernameExists("alice")).thenReturn(0);
        when(userMapper.checkPhoneExists("13800000002", 0L)).thenReturn(0);
        when(userMapper.checkEmailExists("alice@example.com", 0L)).thenReturn(0);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(20L);
            return true;
        }).when(service).save(any(SysUser.class));
        UserContext.setUser(operator());

        service.importUsers(List.of(importRequest("alice", "13800000002", "alice@example.com", 10L, List.of(2L, 3L))), false);

        verify(userRoleMapper).batchInsert(20L, List.of(2L, 3L));
    }

    @Test
    void insertUserShouldRejectMissingAuthenticationContext() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserServiceImpl service = spy(newService(userMapper, mock(ApplicationEventPublisher.class)));
        SysUser user = user(20L, "alice", 10L);
        user.setPassword("Init@12345");
        user.setPhone("13800000002");
        user.setEmail("alice@example.com");
        when(userMapper.checkUsernameExists("alice")).thenReturn(0);
        when(userMapper.checkPhoneExists("13800000002", 0L)).thenReturn(0);
        when(userMapper.checkEmailExists("alice@example.com", 0L)).thenReturn(0);

        assertThatThrownBy(() -> service.insertUser(user, List.of(2L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未认证");
    }

    private SysUserServiceImpl newService(SysUserMapper userMapper, ApplicationEventPublisher publisher) {
        return newService(userMapper, mock(SysUserRoleMapper.class), publisher);
    }

    private SysUserServiceImpl newService(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper, ApplicationEventPublisher publisher) {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        SecurityProperties properties = new SecurityProperties();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return new SysUserServiceImpl(
                userMapper,
                mock(SysDeptMapper.class),
                userRoleMapper,
                passwordEncoder,
                properties,
                publisher,
                validator
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

    private UserImportRequest importUser(String username, String phone, String email) {
        UserImportRequest user = new UserImportRequest();
        user.setUsername(username);
        user.setNickname(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setSex(1);
        user.setDeptId(10L);
        user.setStatus(1);
        user.setRoleIds(List.of(2L));
        return user;
    }

    private UserImportRequest importRequest(String username, String phone, String email, Long deptId, List<Long> roleIds) {
        UserImportRequest request = new UserImportRequest();
        request.setUsername(username);
        request.setNickname(username);
        request.setPhone(phone);
        request.setEmail(email);
        request.setSex(1);
        request.setStatus(1);
        request.setDeptId(deptId);
        request.setRoleIds(roleIds);
        return request;
    }
}
