package com.alpha.orm.context;

import com.alpha.orm.enums.DataScopeType;
import lombok.Data;

import java.util.Set;

/**
 * 数据权限上下文
 * <p>
 * 在 DataScopeAspect 切面和业务代码之间传递数据权限信息。
 * <p>
 * 工作流程：
 * 1. Service 方法标注 @DataScope 注解
 * 2. DataScopeAspect 在方法执行前设置 DataPermissionContext
 * 3. 业务代码调用 DataPermissionInterceptor.applyDataScope(wrapper, alias) 将条件注入 QueryWrapper
 * 4. DataScopeAspect 在方法执行后清理 DataPermissionContext
 */
public class DataPermissionContext {

    private static final ThreadLocal<DataPermission> PERMISSION_HOLDER = new ThreadLocal<>();

    private DataPermissionContext() {
    }

    /**
     * 设置数据权限
     */
    public static void set(DataPermission permission) {
        PERMISSION_HOLDER.set(permission);
    }

    /**
     * 获取数据权限
     */
    public static DataPermission get() {
        return PERMISSION_HOLDER.get();
    }

    /**
     * 清除数据权限
     */
    public static void clear() {
        PERMISSION_HOLDER.remove();
    }

    /**
     * 判断是否有数据权限设置
     */
    public static boolean hasPermission() {
        return PERMISSION_HOLDER.get() != null;
    }

    /**
     * 数据权限信息
     */
    @Data
    public static class DataPermission {
        /**
         * 用户 ID
         */
        private Long userId;

        /**
         * 部门 ID
         */
        private Long deptId;

        /**
         * 可访问的部门 ID 集合
         */
        private Set<Long> deptIds;

        /**
         * 数据权限类型
         */
        private DataScopeType dataScopeType;

        /**
         * 部门字段名（默认 dept_id）
         */
        private String deptField = "dept_id";

        /**
         * 用户字段名（默认 create_by，用于 SELF 权限）
         */
        private String userField = "create_by";

        /**
         * 表别名（多表查询时使用）
         */
        private String tableAlias = "";

        /**
         * 获取完整的部门字段名（带表别名）
         */
        public String getFullDeptField() {
            if (tableAlias != null && !tableAlias.isEmpty()) {
                return tableAlias + "." + deptField;
            }
            return deptField;
        }

        /**
         * 获取完整的用户字段名（带表别名）
         */
        public String getFullUserField() {
            if (tableAlias != null && !tableAlias.isEmpty()) {
                return tableAlias + "." + userField;
            }
            return userField;
        }

        /**
         * 是否需要过滤数据
         */
        public boolean needFilter() {
            return dataScopeType != null && dataScopeType != DataScopeType.ALL;
        }
    }
}