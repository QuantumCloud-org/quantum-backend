package com.alpha.orm.permission;

import com.alpha.orm.enums.DataScopeType;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>
 * 标注在 Service 方法上，由 DataScopeAspect 将权限上下文写入 ThreadLocal。
 * 业务代码需在 QueryWrapper 构建后显式调用
 * {@code DataPermissionInterceptor.applyDataScope(wrapper, alias)} 注入过滤条件。
 * <p>
 * 使用示例：
 * <pre>
 * @DataScope(type = DataScopeType.DEPT_AND_CHILD)
 * public List<User> selectDeptUsers(UserQuery query) {
 *     QueryWrapper wrapper = buildQueryWrapper(query);
 *     DataPermissionInterceptor.applyDataScope(wrapper, "");
 *     return list(wrapper);
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 数据权限类型
     * <p>
     * DEFAULT：使用用户配置的数据权限（UserContext.getDataScope()）
     * 其他：强制使用指定类型
     */
    DataScopeType type() default DataScopeType.DEFAULT;

    /**
     * 部门字段名（默认 dept_id）
     */
    String deptField() default "dept_id";

    /**
     * 用户字段名（默认 create_by，用于 SELF 权限）
     */
    String userField() default "create_by";

    /**
     * 表别名（多表查询时使用）
     */
    String tableAlias() default "";
}