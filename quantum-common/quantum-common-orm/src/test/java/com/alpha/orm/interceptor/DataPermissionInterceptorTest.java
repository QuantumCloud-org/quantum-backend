package com.alpha.orm.interceptor;

import com.alpha.orm.context.DataPermissionContext;
import com.alpha.orm.enums.DataScopeType;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DataPermissionInterceptorTest {

    @AfterEach
    void tearDown() {
        DataPermissionContext.clear();
    }

    @Test
    void applyDataScope_usesContextAlias_whenMethodAliasIsBlank() {
        DataPermissionContext.DataPermission p = new DataPermissionContext.DataPermission();
        p.setDataScopeType(DataScopeType.DEPT);
        p.setDeptId(42L);
        p.setTableAlias("u");
        DataPermissionContext.set(p);

        QueryWrapper wrapper = QueryWrapper.create();
        DataPermissionInterceptor.applyDataScope(wrapper, "");

        assertTrue(wrapper.toSQL().contains("u.dept_id = 42"));
    }

    @Test
    void applyDataScope_prefersExplicitAlias_overContextAlias() {
        DataPermissionContext.DataPermission p = new DataPermissionContext.DataPermission();
        p.setDataScopeType(DataScopeType.SELF);
        p.setUserId(7L);
        p.setTableAlias("ctx");
        DataPermissionContext.set(p);

        QueryWrapper wrapper = QueryWrapper.create();
        DataPermissionInterceptor.applyDataScope(wrapper, "manual");

        String sql = wrapper.toSQL();
        assertTrue(sql.contains("manual.create_by = 7"));
        assertFalse(sql.contains("ctx.create_by = 7"));
    }

    @Test
    void applyDataScope_failClosed_whenDeptIdsAndDeptIdBothNull() {
        DataPermissionContext.DataPermission p = new DataPermissionContext.DataPermission();
        p.setDataScopeType(DataScopeType.DEPT_AND_CHILD);
        p.setDeptId(null);
        p.setDeptIds(null);
        DataPermissionContext.set(p);

        QueryWrapper wrapper = QueryWrapper.create();
        DataPermissionInterceptor.applyDataScope(wrapper, "");

        assertTrue(wrapper.toSQL().contains("1 = 0"));
    }

    @Test
    void applyDataScope_fallbackToDeptId_whenDeptIdsEmpty() {
        DataPermissionContext.DataPermission p = new DataPermissionContext.DataPermission();
        p.setDataScopeType(DataScopeType.DEPT_AND_CHILD);
        p.setDeptId(10L);
        p.setDeptIds(Set.of());
        DataPermissionContext.set(p);

        QueryWrapper wrapper = QueryWrapper.create();
        DataPermissionInterceptor.applyDataScope(wrapper, "");

        assertTrue(wrapper.toSQL().contains("dept_id = 10"));
    }

    @Test
    void applyDataScope_noFilter_forAllScope() {
        DataPermissionContext.DataPermission p = new DataPermissionContext.DataPermission();
        p.setDataScopeType(DataScopeType.ALL);
        p.setUserId(1L);
        DataPermissionContext.set(p);

        QueryWrapper wrapper = QueryWrapper.create();
        DataPermissionInterceptor.applyDataScope(wrapper, "");

        // ALL scope should add no conditions
        String sql = wrapper.toSQL();
        assertFalse(sql.contains("dept_id"));
        assertFalse(sql.contains("create_by"));
    }

    @Test
    void applyDataScope_noFilter_whenContextNull() {
        // No context set
        QueryWrapper wrapper = QueryWrapper.create();
        DataPermissionInterceptor.applyDataScope(wrapper, "");

        String sql = wrapper.toSQL();
        assertFalse(sql.contains("dept_id"));
        assertFalse(sql.contains("create_by"));
    }
}
