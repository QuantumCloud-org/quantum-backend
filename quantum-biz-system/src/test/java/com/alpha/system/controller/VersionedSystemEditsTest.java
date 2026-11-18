package com.alpha.system.controller;

import com.alpha.framework.exception.GlobalExceptionHandler;
import com.alpha.system.convert.DeptConvert;
import com.alpha.system.convert.MenuConvert;
import com.alpha.system.convert.RoleConvert;
import com.alpha.system.domain.SysDept;
import com.alpha.system.domain.SysMenu;
import com.alpha.system.domain.SysRole;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleDeptMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysRoleMenuMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.RoleDelegationPolicy;
import com.alpha.system.service.ISysMenuService;
import com.alpha.system.service.impl.SysDeptServiceImpl;
import com.alpha.system.service.impl.SysMenuServiceImpl;
import com.alpha.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VersionedSystemEditsTest {

    private static final long ID = 1889234567890123456L;
    private static final String CURRENT = """
            {"id":"1889234567890123456","version":"4","parentId":"0","menuName":"menu",
             "menuType":"M","roleName":"reviewer","roleKey":"reviewer","orderNum":0,
             "dataScope":1,"status":1,"deptName":"department"}
            """;

    @Test
    void menuEditAcceptsStringIdAndVersionAndRejectsStaleSave() throws Exception {
        var service = spy(new SysMenuServiceImpl(mock(SysMenuMapper.class), mock(SysRoleMenuMapper.class),
                mock(SysUserRoleMapper.class), mock(ApplicationEventPublisher.class)));
        var existing = new SysMenu();
        existing.setId(ID);
        existing.setParentId(0L);
        existing.setMenuType("M");
        existing.setStatus(1);
        doReturn(existing).when(service).getById(ID);
        doReturn(true).when(service).checkMenuNameUnique("menu", 0L, ID);
        doAnswer(call -> ((SysMenu) call.getArgument(0)).getVersion() == 4L)
                .when(service).updateById(any(SysMenu.class));

        expectVersionedEdit(new SysMenuController(service, new MenuConvert()), "/system/menu");
    }

    @Test
    void roleEditAcceptsStringIdAndVersionAndRejectsStaleSave() throws Exception {
        var deptMapper = mock(SysDeptMapper.class);
        var service = spy(new SysRoleServiceImpl(mock(SysRoleMapper.class), mock(SysRoleMenuMapper.class),
                mock(SysRoleDeptMapper.class), mock(SysUserRoleMapper.class),
                mock(ApplicationEventPublisher.class), mock(RoleDelegationPolicy.class)));
        var existing = new SysRole();
        existing.setId(ID);
        doReturn(existing).when(service).getById(ID);
        doReturn(true).when(service).checkRoleNameUnique("reviewer", ID);
        doReturn(true).when(service).checkRoleKeyUnique("reviewer", ID);
        doAnswer(call -> ((SysRole) call.getArgument(0)).getVersion() == 4L)
                .when(service).updateById(any(SysRole.class));

        expectVersionedEdit(new SysRoleController(service, mock(ISysMenuService.class), deptMapper,
                new RoleConvert()), "/system/role");
    }

    @Test
    void departmentEditAcceptsStringIdAndVersionAndRejectsStaleSave() throws Exception {
        expectVersionedEdit(new SysDeptController(departmentService(), new DeptConvert()), "/system/dept");
    }

    @Test
    void departmentEditPreservesOmittedContactsAndIgnoresDisplayedEmailMask() throws Exception {
        var service = departmentService();
        var mvc = MockMvcBuilders.standaloneSetup(new SysDeptController(service, new DeptConvert()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var existing = service.getById(ID);

        mvc.perform(put("/system/dept").contentType(MediaType.APPLICATION_JSON).content(CURRENT))
                .andExpect(status().isOk());
        assertThat(existing.getPhone()).isEqualTo("13800138000");
        assertThat(existing.getEmail()).isEqualTo("alice@example.com");

        mvc.perform(put("/system/dept").contentType(MediaType.APPLICATION_JSON).content(
                        CURRENT.replace("\"deptName\"", "\"email\":\"a****@example.com\",\"deptName\"")))
                .andExpect(status().isOk());
        assertThat(existing.getEmail()).isEqualTo("alice@example.com");

        mvc.perform(put("/system/dept").contentType(MediaType.APPLICATION_JSON).content(
                        CURRENT.replace("\"deptName\"", "\"phone\":\"13900139000\","
                                + "\"email\":\"bob@example.com\",\"deptName\"")))
                .andExpect(status().isOk());
        assertThat(existing.getPhone()).isEqualTo("13900139000");
        assertThat(existing.getEmail()).isEqualTo("bob@example.com");
    }

    private SysDeptServiceImpl departmentService() {
        var service = spy(new SysDeptServiceImpl(mock(SysDeptMapper.class)));
        var existing = new SysDept();
        existing.setId(ID);
        existing.setParentId(0L);
        existing.setPhone("13800138000");
        existing.setEmail("alice@example.com");
        doReturn(existing).when(service).getById(ID);
        doReturn(true).when(service).checkDeptNameUnique("department", 0L, ID);
        doAnswer(call -> ((SysDept) call.getArgument(0)).getVersion() == 4L)
                .when(service).updateById(any(SysDept.class));
        return service;
    }

    private void expectVersionedEdit(Object controller, String path) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(CURRENT))
                .andExpect(status().isOk());
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(CURRENT.replace("\"4\"", "\"3\"")))
                .andExpect(status().isConflict());
    }
}
