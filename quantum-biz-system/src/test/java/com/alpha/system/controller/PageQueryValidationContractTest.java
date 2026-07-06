package com.alpha.system.controller;

import com.alpha.logging.dto.LogPageQuery;
import com.alpha.logging.entity.SysOperLog;
import com.alpha.orm.entity.PageQuery;
import com.alpha.system.dto.request.DictDataQuery;
import com.alpha.system.dto.request.DictTypeQuery;
import com.alpha.system.dto.request.LoginLogQuery;
import com.alpha.system.dto.request.RoleQuery;
import com.alpha.system.dto.request.SysFileQuery;
import com.alpha.system.dto.request.UserQuery;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryValidationContractTest {

    @Test
    void allPagedGetEndpointsShouldValidatePageQueryArguments() {
        assertPageQueryValidated(SysUserController.class, "list", UserQuery.class);
        assertPageQueryValidated(SysRoleController.class, "list", RoleQuery.class);
        assertPageQueryValidated(FileController.class, "list", SysFileQuery.class);
        assertPageQueryValidated(SysLoginLogController.class, "list", LoginLogQuery.class);
        assertPageQueryValidated(SysOperLogController.class, "list", LogPageQuery.class, SysOperLog.class, LogPageQuery.class);
        assertPageQueryValidated(SysDictController.class, "listType", DictTypeQuery.class);
        assertPageQueryValidated(SysDictController.class, "listData", DictDataQuery.class);
    }

    private void assertPageQueryValidated(Class<?> controllerClass, String methodName, Class<?> queryType, Class<?>... signature) {
        Method method = findMethod(controllerClass, methodName, signature.length == 0 ? new Class<?>[]{queryType} : signature);
        Parameter parameter = Arrays.stream(method.getParameters())
                .filter(candidate -> candidate.getType().equals(queryType))
                .findFirst()
                .orElseThrow();

        assertThat(hasValidationAnnotation(parameter))
                .as("%s#%s(%s) must trigger PageQuery validation", controllerClass.getSimpleName(), methodName, queryType.getSimpleName())
                .isTrue();
    }

    private Method findMethod(Class<?> controllerClass, String methodName, Class<?>[] signature) {
        try {
            return controllerClass.getDeclaredMethod(methodName, signature);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Missing controller method: " + controllerClass.getSimpleName() + "#" + methodName, e);
        }
    }

    private boolean hasValidationAnnotation(Parameter parameter) {
        if (!PageQuery.class.isAssignableFrom(parameter.getType()) && !LogPageQuery.class.equals(parameter.getType())) {
            return false;
        }
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().equals(Valid.class) || annotation.annotationType().equals(Validated.class)) {
                return true;
            }
        }
        return false;
    }
}
