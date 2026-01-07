package com.alpha.orm.interceptor;

import com.alpha.orm.context.DataPermissionContext;
import com.alpha.orm.enums.DataScopeType;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限 MyBatis 拦截器
 * <p>
 * 自动拦截 SELECT 语句，根据 DataPermissionContext 添加数据权限条件
 * 配合 @DataScope 注解和 DataScopeAspect 切面使用
 * <p>
 * 对业务完全透明，只需在 Service 方法上加 @DataScope 注解
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class DataPermissionInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取数据权限上下文
        DataPermissionContext.DataPermission permission = DataPermissionContext.get();

        // 没有数据权限要求，直接放行
        if (permission == null) {
            return invocation.proceed();
        }

        // 全部数据权限，直接放行
        if (permission.getDataScopeType() == DataScopeType.ALL) {
            return invocation.proceed();
        }

        // 获取 MappedStatement
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];

        // 只处理 SELECT 语句
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return invocation.proceed();
        }

        // 获取参数
        Object parameter = invocation.getArgs()[1];

        // 获取原始 SQL
        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        // 构建数据权限条件
        String dataPermissionSql = buildDataPermissionSql(permission);

        if (dataPermissionSql != null && !dataPermissionSql.isEmpty()) {
            // 修改 SQL（在 WHERE 条件中添加数据权限条件）
            String newSql = injectDataPermission(originalSql, dataPermissionSql);

            // 使用反射修改 BoundSql 中的 SQL
            java.lang.reflect.Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, newSql);

            log.debug("数据权限注入 | Original: {} | Permission: {} | New: {}",
                    originalSql.replaceAll("\\s+", " ").trim(),
                    dataPermissionSql,
                    newSql.replaceAll("\\s+", " ").trim());
        }

        return invocation.proceed();
    }

    /**
     * 构建数据权限 SQL 条件
     */
    private String buildDataPermissionSql(DataPermissionContext.DataPermission permission) {
        DataScopeType scopeType = permission.getDataScopeType();
        String deptField = permission.getFullDeptField();
        String userField = permission.getFullUserField();

        return switch (scopeType) {
            case ALL -> null; // 全部数据，不添加条件

            case DEPT -> {
                // 本部门
                if (permission.getDeptId() != null) {
                    yield deptField + " = " + permission.getDeptId();
                }
                yield null;
            }

            case DEPT_AND_CHILD, CUSTOM -> {
                // 本部门及子部门 / 自定义部门
                Set<Long> deptIds = permission.getDeptIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    String inClause = deptIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    yield deptField + " IN (" + inClause + ")";
                }
                // 如果没有部门集合，退化为本部门
                if (permission.getDeptId() != null) {
                    yield deptField + " = " + permission.getDeptId();
                }
                yield null;
            }

            case SELF -> {
                // 仅本人
                if (permission.getUserId() != null) {
                    yield userField + " = " + permission.getUserId();
                }
                yield null;
            }

            default -> null;
        };
    }

    /**
     * 将数据权限条件注入到 SQL 中
     */
    private String injectDataPermission(String originalSql, String dataPermissionSql) {
        String upperSql = originalSql.toUpperCase();

        // 查找 WHERE 关键字位置
        int whereIndex = upperSql.indexOf(" WHERE ");

        if (whereIndex > 0) {
            // 已有 WHERE 条件，在 WHERE 后添加 AND 条件
            int insertIndex = whereIndex + 7; // " WHERE " 长度
            return originalSql.substring(0, insertIndex)
                    + "(" + dataPermissionSql + ") AND "
                    + originalSql.substring(insertIndex);
        } else {
            // 没有 WHERE 条件
            // 查找 GROUP BY、ORDER BY、LIMIT 等关键字
            int groupByIndex = upperSql.indexOf(" GROUP BY ");
            int orderByIndex = upperSql.indexOf(" ORDER BY ");
            int limitIndex = upperSql.indexOf(" LIMIT ");

            // 找到最早出现的关键字位置
            int insertIndex = originalSql.length();
            if (groupByIndex > 0 && groupByIndex < insertIndex) {
                insertIndex = groupByIndex;
            }
            if (orderByIndex > 0 && orderByIndex < insertIndex) {
                insertIndex = orderByIndex;
            }
            if (limitIndex > 0 && limitIndex < insertIndex) {
                insertIndex = limitIndex;
            }

            // 在适当位置插入 WHERE 条件
            return originalSql.substring(0, insertIndex)
                    + " WHERE " + dataPermissionSql
                    + originalSql.substring(insertIndex);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以通过配置文件传入参数
    }

    /**
     * 手动构建数据权限条件（用于复杂查询场景）
     * <p>
     * 使用示例：
     * <pre>
     * QueryWrapper wrapper = QueryWrapper.create().from(SYS_USER);
     * DataPermissionInterceptor.applyDataScope(wrapper, "u");
     * </pre>
     */
    public static void applyDataScope(QueryWrapper queryWrapper, String tableAlias) {
        DataPermissionContext.DataPermission permission = DataPermissionContext.get();
        if (permission == null || permission.getDataScopeType() == DataScopeType.ALL) {
            return;
        }

        String prefix = (tableAlias != null && !tableAlias.isEmpty()) ? tableAlias + "." : "";
        String deptField = prefix + permission.getDeptField();
        String userField = prefix + permission.getUserField();

        switch (permission.getDataScopeType()) {
            case DEPT -> {
                if (permission.getDeptId() != null) {
                    queryWrapper.and(deptField + " = " + permission.getDeptId());
                }
            }
            case DEPT_AND_CHILD, CUSTOM -> {
                Set<Long> deptIds = permission.getDeptIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    String inClause = deptIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    queryWrapper.and(deptField + " IN (" + inClause + ")");
                }
            }
            case SELF -> {
                if (permission.getUserId() != null) {
                    queryWrapper.and(userField + " = " + permission.getUserId());
                }
            }
        }
    }
}