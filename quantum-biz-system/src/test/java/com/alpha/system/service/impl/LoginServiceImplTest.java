package com.alpha.system.service.impl;

import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.cache.util.CacheClient;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.service.ICaptchaService;
import com.alpha.security.token.SessionGeneration;
import com.alpha.security.token.TokenProperties;
import com.alpha.security.token.TokenService;
import com.alpha.system.dto.request.LoginRequest;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.service.ISysLoginLogService;
import com.alpha.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginServiceImplTest {
    private static final String USERNAME = "test-user";
    private static final String FAIL_KEY = CacheKeyConstant.AUTH_LOGIN_FAIL + USERNAME;

    private final AuthenticationManager authentication = mock(AuthenticationManager.class);
    private final TokenService tokens = mock(TokenService.class);
    private final CacheClient cache = mock(CacheClient.class);
    private final ISysLoginLogService loginLogs = mock(ISysLoginLogService.class);
    private final ISysUserService users = mock(ISysUserService.class);
    private final SessionGeneration generations = mock(SessionGeneration.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);

    @Test
    void captchaRejectionsDoNotCountTowardAccountLockout() {
        var cache = mock(CacheClient.class);
        var properties = new SecurityProperties();
        properties.setCaptchaRequired(true);
        var service = new LoginServiceImpl(mock(AuthenticationManager.class), mock(TokenService.class), cache,
                properties, mock(ISysLoginLogService.class), mock(ICaptchaService.class), mock(ISysUserService.class),
                mock(SessionGeneration.class), mock(SysUserMapper.class));
        var request = new LoginRequest();
        request.setUsername("test-user");
        assertThatThrownBy(() -> service.login(request)).isInstanceOf(BizException.class);
        request.setCaptchaKey("test-key");
        request.setCaptchaCode("abcd");
        assertThatThrownBy(() -> service.login(request)).isInstanceOf(BizException.class);
        verifyNoInteractions(cache);
    }

    @Test
    void missingGenerationAfterValidCredentialsDoesNotCountTowardAccountLockout() {
        validLogin();

        assertThatThrownBy(() -> login(tokens)).isInstanceOf(BizException.class)
                .hasMessage("会话切换中，请重试");
        verify(cache, never()).increment(FAIL_KEY);
        verify(cache, never()).delete(FAIL_KEY);
        verifyNoInteractions(tokens);
        verify(loginLogs, never()).insertLoginLog(argThat(log -> log.getStatus() == 0));
    }

    @Test
    void rejectedTokenIssuanceDoesNotClearFailureCountOrRecordSuccessfulLogin() throws Exception {
        validLogin();
        when(generations.open(2L)).thenReturn("old-generation");
        when(generations.matches(2L, "old-generation")).thenReturn(false);
        var tokenProperties = new TokenProperties();
        tokenProperties.setSecret("test-fake-signing-key-".repeat(4));
        var issuingTokens = new TokenService(cache, tokenProperties, mock(UserDetailsService.class), generations);

        assertThatThrownBy(() -> login(issuingTokens)).isInstanceOfSatisfying(BizException.class, error -> {
            assertThat(error.getCode()).isEqualTo(ResultCode.SESSION_TRANSITION.getCode());
            assertThat(error.getMessage()).isEqualTo("会话切换中，请重试");
        });
        verify(cache, never()).delete(FAIL_KEY);
        verify(cache, never()).increment(FAIL_KEY);
        verify(loginLogs, never()).insertLoginLog(argThat(log -> log.getStatus() == 0));
        verifyNoInteractions(users);
    }

    private void validLogin() {
        var login = new LoginUser().setUserId(2L).setUsername(USERNAME);
        when(userMapper.selectIdByUsername(USERNAME)).thenReturn(2L);
        when(authentication.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(login, null, List.of()));
    }

    private void login(TokenService tokenService) throws Exception {
        var properties = new SecurityProperties();
        properties.setCaptchaRequired(false);
        var service = new LoginServiceImpl(authentication, tokenService, cache, properties, loginLogs,
                mock(ICaptchaService.class), users, generations, userMapper);
        var request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword("test-fake-password");
        service.login(request);
    }
}
