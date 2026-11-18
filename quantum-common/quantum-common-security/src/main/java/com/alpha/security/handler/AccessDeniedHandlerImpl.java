package com.alpha.security.handler;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.util.ServletJsonWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 无权限处理器（403）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ServletJsonWriter jsonWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        log.warn("权限不足 | URI: {} | User: {} | Error: {}", request.getRequestURI(), UserContext.getUserId(), accessDeniedException.getMessage());

        jsonWriter.write(response, HttpServletResponse.SC_FORBIDDEN, Result.fail(ResultCode.ACCESS_DENIED));
    }
}