package com.alpha.orm.interceptor;

import com.alpha.orm.context.DataPermissionContext;
import com.alpha.orm.enums.DataScopeType;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataPermissionInterceptor 单测。
 * <p>
 * 覆盖 design.md 交付 3 · TDD 分支 ①④：null-user fail-closed 数据面 +
 * DataScopeType 枚举穷尽（critic F2 P0：防未来新增枚举值再造"静默放行"哑弹）。
 */
class DataPermissionInterceptorTest {

    private static final String TABLE = "sys_user";

    @AfterEach
    void cleanup() {
        DataPermissionContext.clear();
    }

    @Test
    void denyAllShouldInjectAlwaysFalseCondition() {
        DataPermissionContext.set(permissionOf(DataScopeType.DENY_ALL));
        QueryWrapper wrapper = QueryWrapper.create().from(TABLE);

        DataPermissionInterceptor.applyDataScope(wrapper, "");

        assertThat(wrapper.toSQL()).contains("1 = 0");
    }

    @Test
    void allShouldNotInjectAnyFilterCondition() {
        DataPermissionContext.set(permissionOf(DataScopeType.ALL));
        QueryWrapper wrapper = QueryWrapper.create().from(TABLE);
        String baseSql = QueryWrapper.create().from(TABLE).toSQL();

        DataPermissionInterceptor.applyDataScope(wrapper, "");

        assertThat(wrapper.toSQL()).isEqualTo(baseSql);
    }

    @Test
    void selfShouldFilterByUserId() {
        DataPermissionContext.set(permissionOf(DataScopeType.SELF));
        QueryWrapper wrapper = QueryWrapper.create().from(TABLE);

        DataPermissionInterceptor.applyDataScope(wrapper, "");

        assertThat(wrapper.toSQL()).contains("create_by").contains("9");
    }

    /**
     * 枚举穷尽回归（critic F2 P0）：
     * 对 DataScopeType 的每一个值，applyDataScope 都必须有确定处理 ——
     * 要么注入具体过滤条件，要么落入 fail-closed 的 "1 = 0" 兜底；
     * 不允许存在"某枚举值不落入任何 case，静默不注入过滤"的哑弹（等价于放行全部数据）。
     * ALL 是唯一合法的"不过滤"例外，由方法开头显式 early-return 处理。
     */
    @ParameterizedTest
    @EnumSource(DataScopeType.class)
    void applyDataScopeShouldHandleEveryEnumValueDeterministically(DataScopeType type) {
        DataPermissionContext.set(permissionOf(type));
        QueryWrapper wrapper = QueryWrapper.create().from(TABLE);
        String baseSql = QueryWrapper.create().from(TABLE).toSQL();

        DataPermissionInterceptor.applyDataScope(wrapper, "");

        if (type == DataScopeType.ALL) {
            assertThat(wrapper.toSQL())
                    .as("ALL 类型是唯一合法的不过滤例外")
                    .isEqualTo(baseSql);
        } else {
            assertThat(wrapper.toSQL())
                    .as("类型 %s 必须注入具体过滤条件或 fail-closed 兜底，不允许静默放行", type)
                    .isNotEqualTo(baseSql);
        }
    }

    private DataPermissionContext.DataPermission permissionOf(DataScopeType type) {
        DataPermissionContext.DataPermission permission = new DataPermissionContext.DataPermission();
        permission.setUserId(9L);
        permission.setDeptId(10L);
        permission.setDeptIds(Set.of(10L, 11L));
        permission.setDataScopeType(type);
        return permission;
    }
}
