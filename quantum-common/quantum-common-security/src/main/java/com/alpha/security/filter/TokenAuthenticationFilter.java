package com.alpha.security.filter;

import cn.hutool.core.util.StrUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.util.IpUtil;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.token.TokenInfo;
import com.alpha.security.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token 认证过滤器
 */
@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TokenAuthenticationFilter(TokenService tokenService, SecurityProperties securityProperties) {
        this.tokenService = tokenService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = extractToken(request);
        String refreshToken = extractRefreshToken(request);

        if (StrUtil.isNotBlank(accessToken)) {
            try {
                LoginUser loginUser = tokenService.validateToken(accessToken);

                // AccessToken 无效，尝试用 RefreshToken 续期
                if (loginUser == null && StrUtil.isNotBlank(refreshToken)) {
                    loginUser = tryRefreshToken(request, refreshToken, response);
                }

                if (loginUser != null) {
                    setAuthentication(request, loginUser);
                    log.debug("【TokenFilter】认证成功 | UserId: {}", loginUser.getUserId());
                }
            } catch (Exception e) {
                log.error("【TokenFilter】认证异常: {}", e.getMessage(), e);
            }
        } else {
            log.debug("【TokenFilter】Token 为空");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 尝试用 RefreshToken 续期
     */
    private LoginUser tryRefreshToken(HttpServletRequest request, String refreshToken, HttpServletResponse response) {
        try {
            String deviceId = getDeviceId(request);
            String clientIp = IpUtil.getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            TokenInfo newTokenInfo = tokenService.refreshToken(refreshToken, deviceId, clientIp, userAgent);
            if (newTokenInfo != null) {
                // 将新的 Token 放到响应头返回给前端
                response.setHeader(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + newTokenInfo.getAccessToken());
                response.setHeader(CommonConstants.HEADER_REFRESH_TOKEN, newTokenInfo.getRefreshToken());

                LoginUser loginUser = tokenService.validateToken(newTokenInfo.getAccessToken());
                if (loginUser != null) {
                    log.info("【TokenFilter】Token 自动续期成功 | UserId: {}", loginUser.getUserId());
                    return loginUser;
                }
            }
        } catch (Exception e) {
            log.error("【TokenFilter】Token 续期失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 设置认证信息
     */
    private void setAuthentication(HttpServletRequest request, LoginUser loginUser) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserContext.setUser(loginUser);
        MDC.put(CommonConstants.MDC_USER_ID, String.valueOf(loginUser.getUserId()));
        MDC.put(CommonConstants.MDC_USERNAME, loginUser.getUsername());
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.HEADER_AUTHORIZATION);
        if (StrUtil.isNotBlank(header) && header.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return header.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private String extractRefreshToken(HttpServletRequest request) {
        // 优先从请求头获取
        String header = request.getHeader("X-Refresh-Token");
        if (StrUtil.isNotBlank(header)) {
            return header;
        }
        return null;
    }

    private String getDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        return StrUtil.isNotBlank(deviceId) ? deviceId : "default";
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        for (String pattern : securityProperties.getWhitelist()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

}