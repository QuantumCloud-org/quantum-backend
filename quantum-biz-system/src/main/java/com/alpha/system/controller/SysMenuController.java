package com.alpha.system.controller;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.convert.MenuConvert;
import com.alpha.system.dto.request.MenuCreateRequest;
import com.alpha.system.dto.request.MenuQuery;
import com.alpha.system.dto.request.MenuUpdateRequest;
import com.alpha.system.dto.response.RouterVO;
import com.alpha.system.dto.response.TreeSelectVO;
import com.alpha.system.domain.SysMenu;
import com.alpha.system.service.ISysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理 Controller
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final ISysMenuService menuService;
    private final MenuConvert menuConvert;

    @Operation(summary = "获取路由信息")
    @GetMapping("/getRouters")
    public Result<List<RouterVO>> getRouters() {
        Long userId = UserContext.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return Result.ok(menuService.buildRouters(menus));
    }

    @Operation(summary = "查询菜单列表（树形）")
    @SystemLog(title = "菜单管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:menu:list")
    @GetMapping("/list")
    public Result<List<SysMenu>> list(MenuQuery query) {
        SysMenu menuQuery = new SysMenu();
        menuQuery.setMenuName(query.getMenuName());
        menuQuery.setStatus(query.getStatus());
        menuQuery.setVisible(query.getVisible());
        return Result.ok(menuService.selectMenuTree(menuQuery));
    }

    @Operation(summary = "查询菜单下拉树")
    @SystemLog(title = "菜单管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:menu:treeselect")
    @GetMapping("/treeselect")
    public Result<List<TreeSelectVO>> treeSelect(MenuQuery query) {
        SysMenu menuQuery = new SysMenu();
        menuQuery.setMenuName(query.getMenuName());
        menuQuery.setStatus(query.getStatus());
        menuQuery.setVisible(query.getVisible());
        List<SysMenu> menus = menuService.selectMenuTree(menuQuery);
        return Result.ok(menuConvert.toTreeSelectList(menus));
    }

    @Operation(summary = "查询菜单详情")
    @SystemLog(title = "菜单管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:menu:query")
    @GetMapping("/{menuId}")
    public Result<SysMenu> getInfo(@PathVariable Long menuId) {
        return Result.ok(menuService.selectMenuById(menuId));
    }

    @Operation(summary = "新增菜单")
    @SystemLog(title = "菜单管理", businessType = BusinessType.INSERT)
    @RequiresPermission("system:menu:add")
    @PostMapping
    public Result<Long> add(@Validated @RequestBody MenuCreateRequest request) {
        SysMenu menu = menuConvert.toEntity(request);
        return Result.ok(menuService.insertMenu(menu));
    }

    @Operation(summary = "修改菜单")
    @SystemLog(title = "菜单管理", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:menu:edit")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody MenuUpdateRequest request) {
        SysMenu menu = menuConvert.toEntity(request);
        menuService.updateMenu(menu);
        return Result.ok();
    }

    @Operation(summary = "删除菜单")
    @SystemLog(title = "菜单管理", businessType = BusinessType.DELETE)
    @RequiresPermission("system:menu:remove")
    @DeleteMapping("/{menuId}")
    public Result<Void> remove(@PathVariable Long menuId) {
        menuService.deleteMenuById(menuId);
        return Result.ok();
    }
}