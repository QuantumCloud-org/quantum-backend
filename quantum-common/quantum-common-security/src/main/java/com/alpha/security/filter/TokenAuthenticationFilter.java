package com.alpha.security.filter;

import cn.hutool.core.util.StrUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.token.TokenService;
import com.alpha.security.util.BearerTokens;
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
import java.text.ParseException;
import com.nimbusds.jose.JOSEException;

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
        String accessToken = BearerTokens.resolve(request);

        try {
            LoginUser loginUser = null;

            if (StrUtil.isNotBlank(accessToken)) {
                loginUser = tokenService.validateToken(accessToken);
            }

            if (loginUser != null) {
                setAuthentication(request, loginUser);
                log.debug("【TokenFilter】认证成功 | UserId: {}", loginUser.getUserId());
            }
        } catch (ParseException | JOSEException e) {
            log.debug("忽略无法验证的访问令牌");
        } catch (RuntimeException e) {
            log.error("【TokenFilter】认证异常: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
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


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        for (String pattern : securityProperties.getWhitelist()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

}
