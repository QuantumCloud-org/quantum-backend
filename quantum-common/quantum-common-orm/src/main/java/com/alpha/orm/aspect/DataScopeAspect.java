package com.alpha.orm.aspect;

import com.alpha.orm.context.DataPermissionContext;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.orm.permission.DataScope;
import com.alpha.framework.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 数据权限切面
 * <p>
 * 使用方式：在 Service 方法上添加 @DataScope 注解即可，对业务完全无感
 * <pre>
 * {@code @DataScope(type = DataScopeType.DEPT_AND_CHILD)}
 * public List<SysUser> selectUserList(UserQuery query) {
 *     return userMapper.selectList(query);  // 自动添加数据权限条件
 * }
 * </pre>
 * <p>
 * 工作原理：
 * 1. 切面在方法执行前设置 DataPermissionContext
 * 2. MyBatis-Flex 的 DataPermissionInterceptor 拦截 SQL 执行
 * 3. 拦截器读取 DataPermissionContext 自动拼接 WHERE 条件
 * 4. 切面在方法执行后清理 DataPermissionContext
 */
@Slf4j
@Aspect
@Order(2)
@Component
public class DataScopeAspect {

    @Pointcut("@annotation(com.alpha.orm.permission.DataScope)")
    public void dataScopePointCut() {
    }

    @Before("dataScopePointCut()")
    public void before(JoinPoint point) {
        DataScope scope = getDataScope(point);
        if (scope == null) {
            return;
        }

        Long userId = UserContext.getUserId();
        if (userId == null) {
            log.debug("数据权限：用户未登录，跳过");
            return;
        }

        // 管理员跳过数据权限
        if (UserContext.isAdmin()) {
            DataPermissionContext.DataPermission permission = new DataPermissionContext.DataPermission();
            permission.setUserId(userId);
            permission.setDataScopeType(DataScopeType.ALL);
            DataPermissionContext.set(permission);
            return;
        }

        // 构建数据权限
        DataPermissionContext.DataPermission permission = buildPermission(userId, scope);
        DataPermissionContext.set(permission);

        log.debug("数据权限设置 | User: {} | Type: {} | DeptId: {} | DeptIds: {}",
                userId, permission.getDataScopeType(),
                permission.getDeptId(), permission.getDeptIds());
    }

    @After("dataScopePointCut()")
    public void after() {
        DataPermissionContext.clear();
    }

    /**
     * 构建数据权限
     */
    private DataPermissionContext.DataPermission buildPermission(Long userId, DataScope scope) {
        DataPermissionContext.DataPermission permission = new DataPermissionContext.DataPermission();
        permission.setUserId(userId);
        permission.setDeptId(UserContext.getDeptId());
        permission.setDeptField(scope.deptField());
        permission.setUserField(scope.userField());
        permission.setTableAlias(scope.tableAlias());

        // 确定数据权限类型
        DataScopeType scopeType = scope.type();
        if (scopeType == DataScopeType.DEFAULT) {
            // 使用用户配置的数据权限
            Integer dataScope = UserContext.getDataScope();
            scopeType = DataScopeType.fromCode(dataScope);
        }
        permission.setDataScopeType(scopeType);

        // 设置部门 ID 集合（从 UserContext 获取，登录时已计算好）
        Set<Long> deptIds = UserContext.getDeptIds();
        if (deptIds != null && !deptIds.isEmpty()) {
            permission.setDeptIds(deptIds);
        }

        return permission;
    }

    private DataScope getDataScope(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        return AnnotationUtils.findAnnotation(method, DataScope.class);
    }
}