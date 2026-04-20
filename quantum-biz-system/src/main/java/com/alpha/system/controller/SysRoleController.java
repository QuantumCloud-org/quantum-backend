package com.alpha.system.controller;

import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.orm.entity.PageResult;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.convert.RoleConvert;
import com.alpha.system.dto.request.RoleCreateRequest;
import com.alpha.system.dto.request.RoleQuery;
import com.alpha.system.dto.request.RoleUpdateRequest;
import com.alpha.system.dto.response.RoleVO;
import com.alpha.system.domain.SysRole;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.service.ISysMenuService;
import com.alpha.system.service.ISysRoleService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色管理 Controller
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService roleService;
    private final ISysMenuService menuService;
    private final SysDeptMapper deptMapper;
    private final RoleConvert roleConvert;

    @Operation(summary = "分页查询角色")
    @SystemLog(title = "角色管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:role:list")
    @GetMapping("/list")
    public Result<PageResult<RoleVO>> list(RoleQuery query) {
        Page<SysRole> pageResult = roleService.selectRolePage(query);
        PageResult<RoleVO> voPage = PageResult.of(pageResult, roleConvert::toVO);
        return Result.ok(voPage);
    }

    @Operation(summary = "查询所有角色")
    @SystemLog(title = "角色管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:role:query")
    @GetMapping("/all")
    public Result<List<RoleVO>> all() {
        return Result.ok(roleConvert.toVOList(roleService.selectAllRoles()));
    }

    @Operation(summary = "查询角色详情")
    @SystemLog(title = "角色管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:role:query")
    @GetMapping("/{roleId}")
    public Result<RoleVO> getInfo(@PathVariable Long roleId) {
        return Result.ok(roleConvert.toVO(roleService.selectRoleById(roleId)));
    }

    @Operation(summary = "查询角色关联的菜单ID")
    @SystemLog(title = "角色管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:role:query")
    @GetMapping("/{roleId}/menus")
    public Result<Set<Long>> getRoleMenus(@PathVariable Long roleId) {
        return Result.ok(menuService.selectMenuIdsByRoleId(roleId));
    }

    @Operation(summary = "查询角色关联的部门ID")
    @SystemLog(title = "角色管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:role:query")
    @GetMapping("/{roleId}/depts")
    public Result<Set<Long>> getRoleDepts(@PathVariable Long roleId) {
        Set<Long> deptIds = deptMapper.selectDeptIdsByRoleId(roleId);
        return Result.ok(deptIds == null ? java.util.Collections.emptySet() : deptIds);
    }

    @Operation(summary = "新增角色")
    @SystemLog(title = "角色管理", businessType = BusinessType.INSERT)
    @RequiresPermission("system:role:add")
    @PostMapping
    public Result<Long> add(@Validated @RequestBody RoleCreateRequest request) {
        SysRole role = roleConvert.toEntity(request);
        return Result.ok(roleService.insertRole(role, request.getMenuIds(), request.getDeptIds()));
    }

    @Operation(summary = "修改角色")
    @SystemLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:role:edit")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody RoleUpdateRequest request) {
        SysRole role = roleConvert.toEntity(request);
        roleService.updateRole(role, request.getMenuIds(), request.getDeptIds());
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @SystemLog(title = "角色管理", businessType = BusinessType.DELETE)
    @RequiresPermission("system:role:remove")
    @DeleteMapping("/{roleIds}")
    public Result<Void> remove(@PathVariable List<Long> roleIds) {
        roleService.deleteRoleByIds(roleIds);
        return Result.ok();
    }

    @Operation(summary = "修改角色状态")
    @SystemLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:role:edit")
    @PutMapping("/changeStatus")
    public Result<Void> changeStatus(@RequestParam Long roleId, @RequestParam Integer status) {
        roleService.updateStatus(roleId, status);
        return Result.ok();
    }

    @Operation(summary = "修改数据权限")
    @SystemLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:role:edit")
    @PutMapping("/dataScope")
    public Result<Void> dataScope(@RequestParam Long roleId,
                                  @RequestParam Integer dataScope,
                                  @RequestParam(required = false) List<Long> deptIds) {
        roleService.updateDataScope(roleId, dataScope, deptIds);
        return Result.ok();
    }
}
