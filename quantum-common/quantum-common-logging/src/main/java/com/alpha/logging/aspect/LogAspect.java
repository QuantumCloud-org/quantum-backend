package com.alpha.logging.aspect;

import cn.hutool.core.util.StrUtil;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.util.IpUtil;
import com.alpha.framework.util.JsonUtil;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.entity.SysOperLog;
import com.alpha.logging.event.OperLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作日志切面
 * <p>
 * 直接使用 UserContext 获取用户信息
 */
@Slf4j
@Aspect
@Order(3)
@Component
@RequiredArgsConstructor
public class LogAspect {

    private static final Set<String> EXCLUDE_PROPERTIES = Set.of(
            "password", "oldPassword", "newPassword", "confirmPassword",
            "credentials", "secret", "token", "accessToken", "refreshToken"
    );

    private static final int MAX_PARAM_LENGTH = 2000;
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    private final JsonUtil jsonUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Before("@annotation(systemLog)")
    public void before(JoinPoint point, SystemLog systemLog) {
        START_TIME.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(systemLog)", returning = "result")
    public void afterReturning(JoinPoint point, SystemLog systemLog, Object result) {
        publishLogEvent(point, systemLog, null, result);
    }

    @AfterThrowing(pointcut = "@annotation(systemLog)", throwing = "e")
    public void afterThrowing(JoinPoint point, SystemLog systemLog, Exception e) {
        publishLogEvent(point, systemLog, e, null);
    }

    /**
     * 构建日志并发布事件
     */
    private void publishLogEvent(JoinPoint point, SystemLog systemLog, Exception e, Object result) {
        try {
            SysOperLog operLog = buildOperLog(point, systemLog, e, result);
            eventPublisher.publishEvent(new OperLogEvent(operLog));
        } catch (Exception ex) {
            log.error("操作日志构建失败", ex);
        } finally {
            START_TIME.remove();
        }
    }

    /**
     * 构建操作日志实体
     */
    private SysOperLog buildOperLog(JoinPoint point, SystemLog systemLog, Exception e, Object result) {
        SysOperLog operLog = new SysOperLog();

        // 基本信息
        operLog.setTitle(systemLog.title());
        operLog.setBusinessType(systemLog.businessType().getCode());
        operLog.setOperatorType(systemLog.operatorType().getCode());
        operLog.setOperTime(LocalDateTime.now());
        operLog.setCostTime(calculateCostTime());

        // 方法信息
        setMethodInfo(operLog, point);

        // 请求信息
        setRequestInfo(operLog);

        // 用户信息
        setUserInfo(operLog);

        // 请求参数
        if (systemLog.saveParams()) {
            String params = extractRequestParams(point, systemLog.excludeParams());
            operLog.setOperParam(StrUtil.sub(params, 0, MAX_PARAM_LENGTH));
        }

        // 响应结果
        if (systemLog.saveResult() && result != null) {
            String resultJson = jsonUtil.toJson(result);
            operLog.setJsonResult(StrUtil.sub(resultJson, 0, MAX_PARAM_LENGTH));
        }

        // 异常信息
        if (e != null) {
            operLog.setStatus(1);
            operLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, MAX_PARAM_LENGTH));
        } else {
            operLog.setStatus(0);
        }

        return operLog;
    }

    private long calculateCostTime() {
        Long startTime = START_TIME.get();
        return startTime != null ? System.currentTimeMillis() - startTime : 0;
    }

    private void setMethodInfo(SysOperLog operLog, JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getName();
        String methodName = signature.getName();
        operLog.setMethod(className + "." + methodName + "()");
    }

    private void setRequestInfo(SysOperLog operLog) {
        HttpServletRequest request = getRequest();
        if (request != null) {
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(StrUtil.sub(request.getRequestURI(), 0, 255));
            operLog.setOperIp(UserContext.getIp());
            operLog.setOperLocation(IpUtil.getCityInfo(UserContext.getIp()));
        }
    }

    /**
     * 设置用户信息（直接从 UserContext 获取）
     */
    private void setUserInfo(SysOperLog operLog) {
        if (UserContext.isAuthenticated()) {
            operLog.setOperName(UserContext.getUsername());
            operLog.setDeptName(UserContext.getDeptName());
        }
    }

    private String extractRequestParams(JoinPoint point, String[] excludeParams) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = point.getArgs();

        if (paramNames == null || args == null) {
            return "";
        }

        Set<String> excludeSet = new HashSet<>(EXCLUDE_PROPERTIES);
        Collections.addAll(excludeSet, excludeParams);

        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            Object arg = args[i];

            if (excludeSet.contains(paramName)) {
                params.put(paramName, "[PROTECTED]");
                continue;
            }

            if (isExcludedType(arg)) {
                continue;
            }

            if (arg instanceof MultipartFile[] files) {
                params.put(paramName, Arrays.stream(files)
                        .map(MultipartFile::getOriginalFilename)
                        .collect(Collectors.toList()));
                continue;
            }

            if (arg instanceof MultipartFile file) {
                params.put(paramName, file.getOriginalFilename());
                continue;
            }

            params.put(paramName, arg);
        }

        return jsonUtil.toJson(params);
    }

    private boolean isExcludedType(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof BindingResult;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}