package com.alpha.system.service.impl;

import com.alpha.cache.util.CacheClient;
import com.alpha.framework.entity.LoginUser;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.service.ICaptchaService;
import com.alpha.security.token.SessionGeneration;
import com.alpha.security.token.TokenInfo;
import com.alpha.security.token.TokenService;
import com.alpha.system.domain.SysLoginLog;
import com.alpha.system.dto.request.LoginRequest;
import com.alpha.system.mapper.SysLoginLogMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.service.ISysLoginLogService;
import com.alpha.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAuditSaturationTest {
    @EnableAsync
    static class AsyncSetup {}

    @Test
    void saturatedAuditQueuePreservesSuccessfulLoginAndItsAuditRecord() throws Exception {
        var mapper = mock(SysLoginLogMapper.class);
        var release = new CountDownLatch(1);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(AsyncSetup.class, SysLoginLogServiceImpl.class);
            context.registerBean(SysLoginLogMapper.class, () -> mapper);
            context.registerBean("loginAuditExecutor", ThreadPoolTaskExecutor.class, () -> {
                var executor = new ThreadPoolTaskExecutor();
                executor.setCorePoolSize(1);
                executor.setMaxPoolSize(1);
                executor.setQueueCapacity(0);
                return executor;
            });
            context.refresh();
            var executor = context.getBean(ThreadPoolTaskExecutor.class);
            var occupied = new CountDownLatch(1);
            executor.execute(() -> {
                occupied.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            try {
                assertThat(occupied.await(5, TimeUnit.SECONDS)).isTrue();
                var logs = context.getBean(ISysLoginLogService.class);

                assertThat(successfulLogin(logs).login(loginRequest()).getAccessToken()).isEqualTo("test-access-token");
                verify(mapper).insert(argThat((SysLoginLog event) ->
                        "test-user".equals(event.getUsername()) && event.getStatus() == 0));
            } finally {
                release.countDown();
            }
        }
    }

    private static LoginServiceImpl successfulLogin(ISysLoginLogService logs) throws Exception {
        var user = new LoginUser().setUserId(2L).setUsername("test-user");
        var manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        var tokens = mock(TokenService.class);
        when(tokens.createToken(eq(user), eq("test-generation"), nullable(String.class), nullable(String.class),
                nullable(String.class), eq(false)))
                .thenReturn(new TokenInfo().setAccessToken("test-access-token"));
        var users = mock(SysUserMapper.class);
        when(users.selectIdByUsername("test-user")).thenReturn(2L);
        var generations = mock(SessionGeneration.class);
        when(generations.open(2L)).thenReturn("test-generation");
        var properties = new SecurityProperties();
        properties.setCaptchaRequired(false);
        return new LoginServiceImpl(manager, tokens, mock(CacheClient.class), properties, logs,
                mock(ICaptchaService.class), mock(ISysUserService.class), generations, users);
    }

    private static LoginRequest loginRequest() {
        var request = new LoginRequest();
        request.setUsername("test-user");
        request.setPassword("test-fake-password");
        return request;
    }
}
