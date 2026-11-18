package com.alpha.system.controller;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.BizException;
import com.alpha.security.aspect.PermissionAspect;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.service.ICaptchaService;
import com.alpha.security.token.TokenService;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.domain.SysUser;
import com.alpha.system.service.ISysUserService;
import com.alpha.system.service.impl.LoginServiceImpl;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.alpha.system.service.ISysLoginLogService;
import com.alpha.logging.service.ISysOperLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AuditLogClearAuthorizationTest {
    private final ISysLoginLogService loginLogs = mock(ISysLoginLogService.class);
    private final ISysOperLogService operationLogs = mock(ISysOperLogService.class);

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void onlySuperAdministratorCanClearEitherAuditTable() {
        var login = (SysLoginLogController) secured(new SysLoginLogController(loginLogs));
        var operation = (SysOperLogController) secured(new SysOperLogController(operationLogs));

        UserContext.setUser(new LoginUser().setUserId(2L).setRoles(Set.of("admin"))
                .setPermissions(Set.of("monitor:loginlog:remove", "monitor:operlog:remove")));
        assertThatThrownBy(login::clean).isInstanceOf(BizException.class);
        assertThatThrownBy(operation::clean).isInstanceOf(BizException.class);
        verifyNoInteractions(loginLogs, operationLogs);

        UserContext.setUser(new LoginUser().setUserId(1L));
        login.clean();
        operation.clean();
        verify(loginLogs).cleanLoginLog();
        verify(operationLogs).cleanOperLog();
    }

    @Test
    void authInfoAdvertisesOnlyTheAuthorityUsedByTheAuditControllers() throws Exception {
        var users = mock(ISysUserService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(mock(LoginServiceImpl.class),
                mock(ICaptchaService.class), users, new UserConvert(), mock(TokenService.class),
                new SecurityProperties())).build();

        for (long id : new long[]{2L, 1L}) {
            var user = new SysUser();
            user.setId(id);
            when(users.selectUserById(id)).thenReturn(user);
            UserContext.setUser(new LoginUser().setUserId(id)
                    .setRoles(id == 2L ? Set.of("admin") : Set.of()));
            mvc.perform(get("/auth/info"))
                    .andExpect(jsonPath("$.data.canClearAuditLogs").value(id == 1L));
        }
    }

    private Object secured(Object target) {
        var factory = new AspectJProxyFactory(target);
        factory.addAspect(new PermissionAspect());
        return factory.getProxy();
    }
}
