package com.alpha.orm.permission;

import com.alpha.orm.enums.DataScopeType;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>
 * 标注在 Service/Mapper 方法上，自动添加数据权限过滤条件
 * <p>
 * 使用示例：
 * <pre>
 * // 使用用户配置的数据权限（从 UserContext.getDataScope() 获取）
 * @DataScope
 * List<User> selectList();
 *
 * // 强制使用本部门及子部门
 * @DataScope(type = DataScopeType.DEPT_AND_CHILD)
 * List<User> selectDeptUsers();
 *
 * // 自定义字段名
 * @DataScope(deptField = "department_id", userField = "creator_id")
 * List<Order> selectOrders();
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