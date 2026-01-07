package com.alpha.security.filter;

import com.alpha.cache.util.RedisUtil;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.util.JsonUtil;
import com.alpha.security.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 限流过滤器
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "security", name = "rate-limit-enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_KEY = "rate:limit:";

    private final RedisUtil redisUtil;
    private final JsonUtil jsonUtil;
    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String key = generateKey(request);
        boolean allowed = redisUtil.tryAcquire(key, securityProperties.getDefaultRateLimit(), 1);

        if (!allowed) {
            log.warn("请求限流 | URI: {} | IP: {}", request.getRequestURI(), UserContext.getIp());
            responseError(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 生成限流 Key
     */
    private String generateKey(HttpServletRequest request) {
        // 优先使用用户 ID，否则使用 IP
        Long userId = UserContext.getUserId();
        String identifier = userId != null ? String.valueOf(userId) : UserContext.getIp();
        return RATE_LIMIT_KEY + identifier + ":" + request.getRequestURI();
    }

    /**
     * 响应错误
     */
    private void responseError(HttpServletResponse response) throws IOException {
        response.setStatus(ResultCode.RATE_LIMIT_EXCEEDED.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonUtil.toJson(Result.fail(ResultCode.RATE_LIMIT_EXCEEDED)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 静态资源不限流
        return path.startsWith("/static") || path.endsWith(".ico");
    }
}