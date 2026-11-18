package com.alpha.mcp.security;

import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.util.ServletJsonWriter;
import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.oauth.OAuthTokenService;
import com.alpha.security.util.BearerTokens;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.alpha.mcp.config.McpSecurityConfig.MCP_PATH_PATTERN;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mcp", name = "enabled", havingValue = "true")
public class OAuthBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final PathPatternRequestMatcher MCP_REQUESTS =
            PathPatternRequestMatcher.withDefaults().matcher(MCP_PATH_PATTERN);

    private final OAuthTokenService tokenService;
    private final McpProperties properties;
    private final ServletJsonWriter jsonWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String bearerToken = BearerTokens.resolve(request);
        var access = tokenService.validateAccessToken(bearerToken, properties.getResource());
        if (access == null) {
            reject(response);
            return;
        }

        LoginUser loginUser = access.user();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(McpAuthenticationDetails.from(
                access.token(),
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
        return !MCP_REQUESTS.matches(request);
    }


    private void reject(HttpServletResponse response) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer resource_metadata=\"" + properties.normalizedIssuer()
                        + "/.well-known/oauth-protected-resource\"");
        jsonWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, Result.fail(ResultCode.UNAUTHORIZED, "invalid MCP OAuth token"));
    }
}
