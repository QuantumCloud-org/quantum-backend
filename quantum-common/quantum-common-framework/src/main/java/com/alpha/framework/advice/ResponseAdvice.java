package com.alpha.framework.advice;

import com.alpha.framework.entity.Result;
import com.alpha.framework.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应自动包装
 * <p>
 * 默认开启，将 Controller 返回值自动包装为 Result<T>
 * </p>
 *
 * <p>排除规则（不包装）：</p>
 * <ul>
 *   <li>返回值已是 Result 类型</li>
 *   <li>Knife4j/Swagger 相关路径</li>
 *   <li>Actuator 端点</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "com.alpha")
@RequiredArgsConstructor
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    private final JsonUtil jsonUtil;

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();
        // 排除 Result 和 ResponseEntity
        if (Result.class.isAssignableFrom(type)
                || ResponseEntity.class.isAssignableFrom(type)) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 排除特定路径（Swagger、Actuator 等）
        String path = request.getURI().getPath();
        if (shouldSkipPath(path)) {
            return body;
        }

        // null 值处理
        if (body == null) {
            // void 方法返回 Result.ok()
            if (void.class.equals(returnType.getParameterType())) {
                return Result.ok();
            }
            return Result.ok(null);
        }

        // String 类型特殊处理（Spring 默认用 StringHttpMessageConverter）
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return jsonUtil.toJson(Result.ok(body));
        }

        return Result.ok(body);
    }

    /**
     * 判断是否跳过包装的路径
     */
    private boolean shouldSkipPath(String path) {
        return path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/doc.html")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator")
                || path.startsWith("/error");
    }
}