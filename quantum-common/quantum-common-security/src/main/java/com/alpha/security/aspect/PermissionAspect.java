package com.alpha.security.aspect;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.security.annotation.RequiresRole;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Order(0)
@Component
public class PermissionAspect {

    /**
     * 权限校验
     */
    @Before("@annotation(com.alpha.security.annotation.RequiresPermission)")
    public void checkPermission(JoinPoint point) {
        RequiresPermission annotation = getAnnotation(point, RequiresPermission.class);
        if (annotation == null) {
            return;
        }

        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 管理员跳过权限校验
        if (loginUser.isAdmin()) {
            return;
        }

        String[] permissions = annotation.value();
        RequiresPermission.Logical logical = annotation.logical();

        boolean hasPermission = checkPermissions(loginUser, permissions, logical);
        if (!hasPermission) {
            log.warn("权限不足 | User: {} | Required: {} | Has: {}", loginUser.getUserId(), permissions, loginUser.getPermissions());
            throw new BizException(ResultCode.ACCESS_DENIED);
        }
    }

    /**
     * 角色校验
     */
    @Before("@annotation(com.alpha.security.annotation.RequiresRole)")
    public void checkRole(JoinPoint point) {
        RequiresRole annotation = getAnnotation(point, RequiresRole.class);
        if (annotation == null) {
            return;
        }

        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 管理员跳过角色校验
        if (loginUser.isAdmin()) {
            return;
        }

        String[] roles = annotation.value();
        RequiresPermission.Logical logical = annotation.logical();

        boolean hasRole = checkRoles(loginUser, roles, logical);
        if (!hasRole) {
            log.warn("角色不足 | User: {} | Required: {} | Has: {}", loginUser.getUserId(), roles, loginUser.getRoles());
            throw new BizException(ResultCode.ACCESS_DENIED);
        }
    }

    /**
     * 检查权限
     */
    private boolean checkPermissions(LoginUser user, String[] permissions, RequiresPermission.Logical logical) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        if (logical == RequiresPermission.Logical.AND) {
            for (String permission : permissions) {
                if (!user.hasPermission(permission)) {
                    return false;
                }
            }
            return true;
        } else {
            return user.hasAnyPermission(permissions);
        }
    }

    /**
     * 检查角色
     */
    private boolean checkRoles(LoginUser user, String[] roles, RequiresPermission.Logical logical) {
        if (roles == null || roles.length == 0) {
            return true;
        }

        if (logical == RequiresPermission.Logical.AND) {
            for (String role : roles) {
                if (!user.hasRole(role)) {
                    return false;
                }
            }
            return true;
        } else {
            for (String role : roles) {
                if (user.hasRole(role)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 获取注解
     */
    private <T extends java.lang.annotation.Annotation> T getAnnotation(JoinPoint point, Class<T> annotationType) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        return AnnotationUtils.findAnnotation(method, annotationType);
    }

}