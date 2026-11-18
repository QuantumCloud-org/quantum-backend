package com.alpha.security.filter;

import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.util.ServletJsonWriter;
import com.alpha.framework.util.XssCleaner;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.wrapper.SecurityRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Wraps JSON for repeated reads and applies the request's XSS policy. */
public class RequestWrapperFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ServletJsonWriter jsonWriter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestWrapperFilter(SecurityProperties securityProperties, ServletJsonWriter jsonWriter) {
        this.securityProperties = securityProperties;
        this.jsonWriter = jsonWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean enableXss = securityProperties.isXssEnabled()
                && securityProperties.getXssExcludes().stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
        request.setAttribute(XssCleaner.ENABLED_REQUEST_ATTRIBUTE, enableXss);

        SecurityRequestWrapper wrapper;
        try {
            wrapper = new SecurityRequestWrapper(request, enableXss);
        } catch (SecurityRequestWrapper.JsonBodyTooLargeException e) {
            jsonWriter.write(response, ResultCode.CONTENT_TOO_LARGE.getCode(), Result.fail(ResultCode.CONTENT_TOO_LARGE));
            return;
        }
        filterChain.doFilter(wrapper, response);
    }
}
