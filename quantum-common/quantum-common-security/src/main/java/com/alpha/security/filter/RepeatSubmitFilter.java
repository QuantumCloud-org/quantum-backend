package com.alpha.security.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alpha.cache.util.RedisUtil;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.util.JsonUtil;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.warpper.SecurityRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * 防重复提交过滤器
 * <p>
 * 修复内容：
 * 1. 支持 JSON Body 的 MD5 计算
 * 2. 使用 RepeatableReadRequestWrapper 解决流重复读问题
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "security", name = "repeat-submit-enabled", havingValue = "true", matchIfMissing = true)
public class RepeatSubmitFilter extends OncePerRequestFilter {

    private static final String REPEAT_SUBMIT_KEY = "repeat:submit:";

    private final RedisUtil redisUtil;
    private final JsonUtil jsonUtil;
    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 生成请求唯一标识
        String key = generateKey(request);

        boolean isRepeat = !redisUtil.setIfAbsent(key, "1", Duration.ofSeconds(securityProperties.getRepeatSubmitInterval()));

        if (isRepeat) {
            log.warn("重复提交 | URI: {} | User: {} | IP: {}", request.getRequestURI(), UserContext.getUserId(), UserContext.getIp());
            responseError(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 生成请求唯一 Key（包含 Body MD5）
     */
    private String generateKey(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();

        // 1. URI
        sb.append(request.getRequestURI());
        sb.append(":");

        // 2. 用户标识（优先用户ID，否则用 IP）
        Long userId = UserContext.getUserId();
        if (userId != null) {
            sb.append("u:").append(userId);
        } else {
            sb.append("ip:").append(UserContext.getIp());
        }
        sb.append(":");

        // 3. 请求参数
        String queryString = request.getQueryString();
        if (StrUtil.isNotBlank(queryString)) {
            sb.append(DigestUtil.md5Hex(queryString));
        }

        // 4. 请求体 (非 JSON 请求 getBodyString 可能返回 null)
        if (request instanceof SecurityRequestWrapper wrapper) {
            String bodyString = wrapper.getBodyString();
            if (StrUtil.isNotBlank(bodyString)) {
                byte[] body = bodyString.getBytes();
                sb.append(":").append(DigestUtil.md5Hex(body));
            }
        }

        return REPEAT_SUBMIT_KEY + DigestUtil.md5Hex(sb.toString());
    }

    private void responseError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonUtil.toJson(Result.fail(ResultCode.DUPLICATE_REQUEST)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        // GET 和 OPTIONS 不过滤
        return HttpMethod.GET.name().equals(method) || HttpMethod.OPTIONS.name().equals(method);
    }
}