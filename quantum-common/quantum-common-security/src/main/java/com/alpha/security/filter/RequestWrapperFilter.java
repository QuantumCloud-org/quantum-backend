package com.alpha.security.filter;

import com.alpha.security.config.SecurityProperties;
import com.alpha.security.warpper.SecurityRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求包装过滤器
 * <p>
 * 职责：
 * 1. 统一包装 Request 为 QuantumRequestWrapper
 * 2. 启用 Body 可重复读
 * 3. 启用 XSS 清洗
 * <p>
 * Order: 必须是所有过滤器中最早的 (HIGHEST_PRECEDENCE)
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestWrapperFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestWrapperFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 判断是否开启 XSS 且不在排除列表中
        boolean enableXss = securityProperties.isXssEnabled();
        if (enableXss) {
            String uri = request.getRequestURI();
            for (String exclude : securityProperties.getXssExcludes()) {
                if (pathMatcher.match(exclude, uri)) {
                    enableXss = false;
                    break;
                }
            }
        }

        // 2. 包装请求 (Wrapper 内部会根据 Content-Type 自动决定是否缓存 Body)
        SecurityRequestWrapper requestWrapper = new SecurityRequestWrapper(request, enableXss);

        // 3. 放行包装后的请求
        filterChain.doFilter(requestWrapper, response);

    }
}