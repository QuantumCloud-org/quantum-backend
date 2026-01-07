package com.alpha.system.service;

import com.alpha.system.domain.SysMenu;
import com.alpha.system.dto.response.RouterVO;
import com.alpha.system.dto.response.TreeSelectVO;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 菜单服务接口
 */
public interface ISysMenuService extends IService<SysMenu> {

    /**
     * 查询菜单树
     */
    List<SysMenu> selectMenuTree(SysMenu query);

    /**
     * 根据用户ID查询菜单树
     */
    List<SysMenu> selectMenuTreeByUserId(Long userId);

    /**
     * 构建菜单下拉树
     */
    List<TreeSelectVO> buildMenuTreeSelect(List<SysMenu> menus);

    /**
     * 根据角色ID查询菜单ID列表
     */
    Set<Long> selectMenuIdsByRoleId(Long roleId);

    /**
     * 根据用户ID查询权限
     */
    Set<String> selectPermsByUserId(Long userId);

    /**
     * 根据ID查询菜单
     */
    SysMenu selectMenuById(Long menuId);

    /**
     * 新增菜单
     */
    Long insertMenu(SysMenu menu);

    /**
     * 修改菜单
     */
    boolean updateMenu(SysMenu menu);

    /**
     * 删除菜单
     */
    boolean deleteMenuById(Long menuId);

    /**
     * 检查菜单名称是否唯一
     */
    boolean checkMenuNameUnique(String menuName, Long parentId, Long excludeId);

    /**
     * 检查菜单是否有子菜单
     */
    boolean hasChildMenu(Long menuId);

    /**
     * 检查菜单是否被角色使用
     */
    boolean isMenuUsed(Long menuId);

    /**
     * 构建路由
     */
    List<RouterVO> buildRouters(List<SysMenu> menus);

}