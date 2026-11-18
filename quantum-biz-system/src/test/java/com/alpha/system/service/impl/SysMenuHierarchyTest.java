package com.alpha.system.service.impl;

import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysMenu;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMenuMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static com.alpha.system.domain.table.SysMenuTableDef.SYS_MENU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SysMenuHierarchyTest {
    private final SysMenuServiceImpl service = spy(new SysMenuServiceImpl(mock(SysMenuMapper.class),
            mock(SysRoleMenuMapper.class), mock(SysUserRoleMapper.class), mock(ApplicationEventPublisher.class)));

    @Test
    void cannotReparentMenuBelowItsGrandchild() {
        doReturn(menu(1L, 0L)).when(service).getById(1L);
        doReturn(menu(3L, 2L), menu(2L, 1L)).when(service).getOne(any(QueryWrapper.class));
        doReturn(true).when(service).checkMenuNameUnique(anyString(), anyLong(), anyLong());
        doReturn(true).when(service).updateById(any(SysMenu.class));

        assertThatThrownBy(() -> service.updateMenu(menu(1L, 3L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("子孙");

        verify(service, never()).updateById(any(SysMenu.class));
    }

    @Test
    void locksEachAncestorBeforeReparenting() {
        doReturn(menu(1L, 0L)).when(service).getById(1L);
        doReturn(menu(2L, 3L), menu(3L, 0L)).when(service).getOne(any(QueryWrapper.class));
        doReturn(true).when(service).checkMenuNameUnique(anyString(), anyLong(), anyLong());
        doReturn(true).when(service).updateById(any(SysMenu.class));

        assertThat(service.updateMenu(menu(1L, 2L))).isTrue();

        var queries = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(service, times(2)).getOne(queries.capture());
        assertThat(queries.getAllValues()).allSatisfy(query ->
                assertThat(query.from(SYS_MENU).toSQL(DbType.POSTGRE_SQL)).contains("FOR UPDATE"));
    }

    @Test
    void canRepairHistoricalCycleByMovingAMenuToRoot() {
        var stored = menu(1L, 2L);
        doReturn(stored).when(service).getById(1L);
        doReturn(true).when(service).checkMenuNameUnique(anyString(), anyLong(), anyLong());
        doReturn(true).when(service).updateById(any(SysMenu.class));

        assertThat(service.updateMenu(menu(1L, 0L))).isTrue();
        assertThat(stored.getParentId()).isZero();
    }

    private static SysMenu menu(long id, long parentId) {
        var menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName("menu");
        menu.setVersion(1L);
        return menu;
    }
}
