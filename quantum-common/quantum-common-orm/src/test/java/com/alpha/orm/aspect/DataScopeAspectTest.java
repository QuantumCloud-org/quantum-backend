package com.alpha.orm.aspect;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.orm.context.DataPermissionContext;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.orm.interceptor.DataPermissionInterceptor;
import com.alpha.orm.permission.DataScope;
import com.alpha.orm.permission.SystemDataScope;
import com.mybatisflex.core.query.QueryWrapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DataScopeAspect 单测。
 * <p>
 * 覆盖 design.md 交付 3 · TDD 分支 ①②③：
 * ① null-user fail-closed（DENY_ALL，数据面结果为空）
 * ② SystemDataScope.execute 内注入 ALL（不过滤 + 审计）
 * ③ admin 回归（既有行为不变）
 */
class DataScopeAspectTest {

    private static final String TABLE = "sys_user";

    private final DataScopeAspect aspect = new DataScopeAspect();

    @AfterEach
    void cleanup() {
        UserContext.clear();
        DataPermissionContext.clear();
    }

    @DataScope(type = DataScopeType.DEPT_AND_CHILD)
    private void annotatedMethod() {
        // 仅用于反射取注解，无需真实逻辑
    }

    @Test
    void beforeShouldInjectDenyAllWhenNoUserContext() throws NoSuchMethodException {
        UserContext.clear();

        aspect.before(joinPointFor("annotatedMethod"));

        DataPermissionContext.DataPermission permission = DataPermissionContext.get();
        assertThat(permission).isNotNull();
        assertThat(permission.getDataScopeType()).isEqualTo(DataScopeType.DENY_ALL);

        // 数据面验证：DENY_ALL 必须让 interceptor 注入恒假条件，查询面 fail-closed
        QueryWrapper wrapper = QueryWrapper.create().from(TABLE);
        DataPermissionInterceptor.applyDataScope(wrapper, "");
        assertThat(wrapper.toSQL()).contains("1 = 0");
    }

    @Test
    void beforeShouldInjectAllWhenSystemDataScopeActive() throws NoSuchMethodException {
        UserContext.clear();
        JoinPoint joinPoint = joinPointFor("annotatedMethod");
        String baseSql = QueryWrapper.create().from(TABLE).toSQL();

        SystemDataScope.execute(() -> {
            aspect.before(joinPoint);

            DataPermissionContext.DataPermission permission = DataPermissionContext.get();
            assertThat(permission).isNotNull();
            assertThat(permission.getDataScopeType()).isEqualTo(DataScopeType.ALL);

            // 数据面验证：系统逃生门下不注入任何过滤条件
            QueryWrapper wrapper = QueryWrapper.create().from(TABLE);
            DataPermissionInterceptor.applyDataScope(wrapper, "");
            assertThat(wrapper.toSQL()).isEqualTo(baseSql);
            return null;
        });
    }

    @Test
    void beforeShouldInjectAllForAdmin() throws NoSuchMethodException {
        LoginUser admin = new LoginUser();
        admin.setUserId(1L); // CommonConstants.SUPER_ADMIN_ID
        UserContext.setUser(admin);

        aspect.before(joinPointFor("annotatedMethod"));

        DataPermissionContext.DataPermission permission = DataPermissionContext.get();
        assertThat(permission).isNotNull();
        assertThat(permission.getDataScopeType()).isEqualTo(DataScopeType.ALL);
    }

    private JoinPoint joinPointFor(String methodName) throws NoSuchMethodException {
        Method method = DataScopeAspectTest.class.getDeclaredMethod(methodName);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        return joinPoint;
    }
}
