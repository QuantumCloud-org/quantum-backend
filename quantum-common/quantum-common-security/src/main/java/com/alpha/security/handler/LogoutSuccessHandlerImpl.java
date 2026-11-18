package com.alpha.security.handler;

import com.alpha.security.util.BearerTokens;
import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.util.ServletJsonWriter;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.token.TokenService;
import com.alpha.security.util.CookieUtil;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 登出成功处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final TokenService tokenService;
    private final ServletJsonWriter jsonWriter;
    private final SecurityProperties securityProperties;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            if (tokenService.logout(BearerTokens.resolve(request), CookieUtil.readRefreshCookie(request))) {
                CookieUtil.clearRefreshCookie(response, securityProperties.isRefreshCookieSecure(),
                        securityProperties.getRefreshCookiePath());
            }
        } catch (JOSEException | RuntimeException e) {
            log.warn("会话服务暂时无法完成登出 | errorType={}", e.getClass().getName());
            jsonWriter.write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Result.fail(ResultCode.SERVICE_UNAVAILABLE));
            return;
        }
        jsonWriter.write(response, HttpServletResponse.SC_OK, Result.ok(null, "登出成功"));
    }
}
