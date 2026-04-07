package com.alpha.logging.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.util.IpUtil;
import com.alpha.framework.util.JsonUtil;
import com.alpha.logging.entity.LogInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求上下文过滤器
 * <p>
 * 职责：
 * 1. 初始化 UserContext
 * 2. 提取请求信息
 * 3. 确保在 finally 记录访问日志中并且彻底清理 ThreadLocal
 * <p>
 * Order = HIGHEST_PRECEDENCE + 10，仅次于 RequestWrapperFilter
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class TraceFilter extends OncePerRequestFilter {

    private final JsonUtil jsonUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 0. 记录请求开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 1. 设置 TraceId
            String traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
            if (StrUtil.isBlank(traceId)) {
                traceId = IdUtil.fastSimpleUUID().substring(0, 16);
            }
            UserContext.setTraceId(traceId);

            // 2. 设置 IP
            String ip = IpUtil.getClientIp(request);
            UserContext.setIp(ip);

            // 3. 设置 UserAgent
            String userAgent = request.getHeader("User-Agent");
            UserContext.setUserAgent(userAgent);

            // 4. 设置请求 URI
            UserContext.setRequestUri(request.getRequestURI());

            // 5. 设置 MDC（日志链路追踪）
            MDC.put(CommonConstants.MDC_TRACE_ID, traceId);
            MDC.put(CommonConstants.MDC_IP, ip);
            MDC.put(CommonConstants.MDC_REQUEST_URI, request.getRequestURI());

            // 6. 响应头返回 TraceId
            response.setHeader(CommonConstants.HEADER_TRACE_ID, traceId);

            // 7. 继续处理
            filterChain.doFilter(request, response);

        } finally {
            // 8. 记录访问日志 (输出到文件)
            recordAccessLog(request, response, startTime);
            // 彻底清理所有 ThreadLocal
            UserContext.clear();
            MDC.clear();
        }
    }

    private void recordAccessLog(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            // 排除静态资源和健康检查
            if (shouldNotFilter(request)) {
                return;
            }
            LogInfo info = buildContextInfo(request, startTime);
            // 打印 JSON 日志 (会被 log4j2 收集到 logs/app.log)
            log.info(jsonUtil.toJson(info));
        } catch (Exception e) {
            log.error("记录访问日志失败", e);
        }
    }

    private LogInfo buildContextInfo(HttpServletRequest request, long startTime) {
        LogInfo info = new LogInfo();

        String traceId = UserContext.getTraceId();
        if (StrUtil.isBlank(traceId)) {
            traceId = MDC.get(CommonConstants.MDC_TRACE_ID);
        }
        info.setTraceId(traceId);

        info.setUserId(UserContext.getUserId());
        info.setUsername(UserContext.getUsername());

        // 租户 ID
        String tenantIdStr = request.getHeader(CommonConstants.HEADER_TENANT_ID);
        if (StrUtil.isNotBlank(tenantIdStr)) {
            try {
                info.setTenantId(Long.parseLong(tenantIdStr));
            } catch (NumberFormatException ignored) {
            }
        }

        // IP
        info.setIp(UserContext.getIp());
        info.setIpAddress(IpUtil.getIpAddress(UserContext.getIp()));

        // User-Agent
        String uaString = UserContext.getUserAgent();
        info.setUserAgent(uaString);
        if (StrUtil.isNotBlank(uaString)) {
            UserAgent ua = UserAgentUtil.parse(uaString);
            if (ua != null) {
                info.setDeviceType(ua.getPlatform().getName());
            }
        }

        // 请求信息
        info.setRequestUri(UserContext.getRequestUri());
        info.setRequestMethod(request.getMethod());
        info.setStartTime(startTime);

        return info;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/static")
                || path.startsWith("/actuator/health")
                || path.endsWith(".ico")
                || path.endsWith(".css")
                || path.endsWith(".js");
    }
}
