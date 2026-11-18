package com.alpha.system.controller;

import com.alpha.cache.util.LocalCacheClient;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.GlobalExceptionHandler;
import com.alpha.security.config.SecurityConfig;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.token.SessionGeneration;
import com.alpha.security.token.TokenProperties;
import com.alpha.security.token.TokenService;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.domain.SysUser;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.ForceLogoutEvent;
import com.alpha.system.security.RoleDelegationPolicy;
import com.alpha.system.security.UserSessionInvalidationService;
import com.alpha.system.service.ISysRoleService;
import com.alpha.system.service.impl.SysUserServiceImpl;
import com.alpha.system.service.impl.UserDetailsServiceImpl;
import jakarta.validation.Validation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Set;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordMigrationTest {

    private final SecurityConfig config = new SecurityConfig(new SecurityProperties(), null, null, null, null, null, null);

    @Test
    void authenticatesStandardHashAfterUsernameChanges() {
        String raw = "test-password";
        String standard = "{bcrypt}" + new BCryptPasswordEncoder().encode(raw);
        var manager = config.authenticationManager(username ->
                new LoginUser().setUserId(2L).setStatus(1).setUsername(username).setPassword(standard), config.passwordEncoder(), UserDetailsPasswordService.NOOP);

        assertThat(config.passwordEncoder().encode(raw)).startsWith("{bcrypt}");
        var original = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("alice", raw));
        var renamed = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("alice-renamed", raw));
        assertThat(original.isAuthenticated()).isTrue();
        assertThat(renamed.isAuthenticated()).isTrue();
    }

    @Test
    void upgradesAValidLegacyHashViaThePasswordOnlyMapperWrite() {
        String raw = "test-password";
        String legacy = new BCryptPasswordEncoder().encode("alice" + raw);
        var mapper = mock(SysUserMapper.class);
        var row = new SysUser();
        row.setId(2L);
        row.setUsername("alice");
        row.setPassword(legacy);
        row.setStatus(1);
        when(mapper.selectByUsername("alice")).thenReturn(row);
        when(mapper.upgradePassword(eq(2L), eq(legacy), org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        var users = new UserDetailsServiceImpl(mapper, mock(SysRoleMapper.class), mock(SysMenuMapper.class), mock(SysDeptMapper.class));
        var encoder = config.passwordEncoder();
        var manager = config.authenticationManager(users, encoder, users);

        var authentication = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("alice", raw));

        assertThat(authentication.isAuthenticated()).isTrue();
        var upgraded = ArgumentCaptor.forClass(String.class);
        verify(mapper).upgradePassword(eq(2L), eq(legacy), upgraded.capture());
        assertThat(upgraded.getValue()).startsWith("{bcrypt}");
        assertThat(encoder.matches(raw, upgraded.getValue())).isTrue();
    }

    @Test
    void userEditRenamesTheAuthenticatedAccountAndRevokesTheOldSessions() throws Exception {
        var mapper = mock(SysUserMapper.class);
        var departments = mock(SysDeptMapper.class);
        var publisher = mock(ApplicationEventPublisher.class);
        var encoder = config.passwordEncoder();
        var row = new SysUser();
        row.setId(2L);
        row.setVersion(4L);
        row.setUsername("alice");
        row.setStatus(1);
        row.setDeptId(10L);
        String raw = "test-password";
        row.setPassword(encoder.encode(raw));
        when(mapper.selectByUsername(anyString()))
                .thenAnswer(call -> row.getUsername().equals(call.getArgument(0)) ? row : null);
        var users = new UserDetailsServiceImpl(mapper, mock(SysRoleMapper.class), mock(SysMenuMapper.class), departments);
        var manager = config.authenticationManager(users, encoder, users);
        var cache = new LocalCacheClient();
        var generations = new SessionGeneration(cache);
        var tokenProperties = new TokenProperties();
        tokenProperties.setSecret("test-fake-signing-key-".repeat(4));
        var tokens = new TokenService(cache, tokenProperties, users, generations);
        var original = (LoginUser) manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("alice", raw)).getPrincipal();
        var previous = tokens.createToken(original, generations.open(2L), "test-device", null, null, false);
        var invalidation = new UserSessionInvalidationService(tokens);
        doAnswer(call -> {
            invalidation.onForceLogout(call.getArgument(0));
            return null;
        }).when(publisher).publishEvent(any(ForceLogoutEvent.class));
        assertThat(tokens.validateToken(previous.getAccessToken())).isNotNull();

        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var service = spy(new SysUserServiceImpl(mapper, departments, mock(SysUserRoleMapper.class),
                    encoder, new SecurityProperties(), publisher, validators.getValidator(),
                    mock(RoleDelegationPolicy.class)));
            doReturn(row).when(service).getById(2L);
            doAnswer(call -> {
                SysUser update = call.getArgument(0);
                if (update.getUsername() != null) row.setUsername(update.getUsername());
                return true;
            }).when(service).updateById(any(SysUser.class));
            var mvc = MockMvcBuilders.standaloneSetup(new SysUserController(service, mock(ISysRoleService.class),
                    new UserConvert())).setControllerAdvice(new GlobalExceptionHandler()).build();

            UserContext.setUser(new LoginUser().setUserId(1L));
            try {
                mvc.perform(put("/system/user").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":"2","version":"4","username":"alice_new","nickname":"Alice",
                         "phone":"13800138000","email":"alice@example.com","sex":1,
                         "deptId":"10","status":1,"roleIds":["2"]}
                        """)).andExpect(status().isOk());
            } finally {
                UserContext.clear();
            }

            verify(mapper).checkUsernameExists("alice_new");
            verify(publisher).publishEvent(new ForceLogoutEvent(Set.of(2L)));
            assertThat(tokens.validateToken(previous.getAccessToken())).isNull();
            assertThat(tokens.refreshToken(previous.getRefreshToken(), "test-device", null, null)).isNull();
            assertThat(manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("alice_new", raw))
                    .isAuthenticated()).isTrue();
            assertThatThrownBy(() -> manager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated("alice", raw)))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Test
    void measuresUtf8BytesAtTheBcryptBoundary() {
        String max = "密".repeat(24);
        var encoder = config.passwordEncoder();
        String hash = encoder.encode(max);
        var manager = config.authenticationManager(username ->
                new LoginUser().setUserId(2L).setStatus(1).setUsername(username).setPassword(hash),
                encoder, UserDetailsPasswordService.NOOP);

        assertThat(manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("alice", max)).isAuthenticated()).isTrue();
        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("alice", max + "密")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
