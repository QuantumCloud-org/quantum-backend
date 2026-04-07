package com.alpha.orm.interceptor;

import com.alpha.orm.context.DataPermissionContext;
import com.alpha.orm.enums.DataScopeType;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限工具
 * <p>
 * 配合 @DataScope 注解和 DataScopeAspect 切面使用。
 * Aspect 负责将数据权限上下文写入 ThreadLocal，
 * 业务代码在构建 QueryWrapper 后调用 applyDataScope() 注入条件。
 * <p>
 * 使用示例：
 * <pre>
 * QueryWrapper wrapper = QueryWrapper.create().from(SYS_USER);
 * DataPermissionInterceptor.applyDataScope(wrapper, "u");
 * </pre>
 */
public class DataPermissionInterceptor {

    private DataPermissionInterceptor() {}

    /**
     * 将数据权限条件注入 QueryWrapper
     *
     * @param queryWrapper 查询条件
     * @param tableAlias   表别名（为空时回退到 @DataScope 上下文中的 tableAlias）
     */
    public static void applyDataScope(QueryWrapper queryWrapper, String tableAlias) {
        DataPermissionContext.DataPermission permission = DataPermissionContext.get();
        if (permission == null || permission.getDataScopeType() == DataScopeType.ALL) {
            return;
        }

        String resolvedAlias = resolveTableAlias(tableAlias, permission);
        String prefix = resolvedAlias.isEmpty() ? "" : resolvedAlias + ".";
        String deptField = prefix + permission.getDeptField();
        String userField = prefix + permission.getUserField();

        switch (permission.getDataScopeType()) {
            case DEPT -> {
                if (permission.getDeptId() != null) {
                    queryWrapper.and(deptField + " = " + permission.getDeptId().longValue());
                }
            }
            case DEPT_AND_CHILD, CUSTOM -> {
                Set<Long> deptIds = permission.getDeptIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    String inClause = deptIds.stream()
                            .map(id -> String.valueOf(id.longValue()))
                            .collect(Collectors.joining(","));
                    queryWrapper.and(deptField + " IN (" + inClause + ")");
                } else if (permission.getDeptId() != null) {
                    // fallback: 没有部门集合时退化为本部门过滤，保证 fail-closed
                    queryWrapper.and(deptField + " = " + permission.getDeptId().longValue());
                } else {
                    // 既无部门集合也无本部门 — fail-closed: 添加恒假条件阻止返回数据
                    queryWrapper.and("1 = 0");
                }
            }
            case SELF -> {
                if (permission.getUserId() != null) {
                    queryWrapper.and(userField + " = " + permission.getUserId().longValue());
                }
            }
        }
    }

    private static String resolveTableAlias(String tableAlias, DataPermissionContext.DataPermission permission) {
        if (tableAlias != null && !tableAlias.isBlank()) {
            return tableAlias.trim();
        }
        String contextAlias = permission.getTableAlias();
        return contextAlias == null ? "" : contextAlias.trim();
    }
}
