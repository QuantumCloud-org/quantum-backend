package com.alpha.mcp.security;

import cn.hutool.core.util.StrUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.util.JsonUtil;
import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.oauth.OAuthAccessToken;
import com.alpha.mcp.oauth.OAuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mcp", name = "enabled", havingValue = "true")
public class OAuthBearerAuthenticationFilter extends OncePerRequestFilter {

    private final OAuthTokenService tokenService;
    private final McpProperties properties;
    private final JsonUtil jsonUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String bearerToken = extractBearerToken(request);
        OAuthAccessToken accessToken = tokenService.validateAccessToken(bearerToken, properties.getResource());
        if (accessToken == null || accessToken.getLoginUser() == null) {
            reject(response);
            return;
        }

        LoginUser loginUser = accessToken.getLoginUser();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(McpAuthenticationDetails.from(
                accessToken,
                new WebAuthenticationDetailsSource().buildDetails(request)
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserContext.setUser(loginUser);
        MDC.put(CommonConstants.MDC_USER_ID, String.valueOf(loginUser.getUserId()));
        MDC.put(CommonConstants.MDC_USERNAME, loginUser.getUsername());

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String endpoint = properties.normalizedEndpoint();
        String path = request.getRequestURI();
        return !(path.equals(endpoint) || path.startsWith(endpoint + "/"));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.HEADER_AUTHORIZATION);
        if (StrUtil.isNotBlank(header) && header.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return header.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer resource_metadata=\"" + properties.normalizedIssuer()
                        + "/.well-known/oauth-protected-resource\"");
        response.getWriter().write(jsonUtil.toJson(Result.fail(ResultCode.UNAUTHORIZED, "invalid MCP OAuth token")));
    }
}
