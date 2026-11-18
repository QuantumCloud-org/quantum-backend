package com.alpha.system.convert;

import com.alpha.system.dto.request.MenuCreateRequest;
import com.alpha.system.dto.request.MenuUpdateRequest;
import com.alpha.system.dto.response.TreeSelectVO;
import com.alpha.system.domain.SysMenu;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 菜单转换器
 */
@Component
public class MenuConvert {

    /**
     * 创建请求 -> 实体
     */
    public SysMenu toEntity(MenuCreateRequest request) {
        if (request == null) {
            return null;
        }
        SysMenu menu = new SysMenu();
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setOrderNum(request.getOrderNum());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setQueryParam(request.getQueryParam());
        menu.setIsFrame(request.getIsFrame());
        menu.setIsCache(request.getIsCache());
        menu.setMenuType(request.getMenuType());
        menu.setVisible(request.getVisible());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setStatus(request.getStatus());
        menu.setRemark(request.getRemark());
        return menu;
    }

    /**
     * 更新请求 -> 实体
     */
    public SysMenu toEntity(MenuUpdateRequest request) {
        if (request == null) {
            return null;
        }
        SysMenu menu = new SysMenu();
        menu.setId(request.getId());
        menu.setVersion(request.getVersion());
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setOrderNum(request.getOrderNum());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setQueryParam(request.getQueryParam());
        menu.setIsFrame(request.getIsFrame());
        menu.setIsCache(request.getIsCache());
        menu.setMenuType(request.getMenuType());
        menu.setVisible(request.getVisible());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setStatus(request.getStatus());
        menu.setRemark(request.getRemark());
        return menu;
    }

    public List<TreeSelectVO> toTreeSelectList(List<SysMenu> menus) {
        return TreeSelectVO.fromTree(menus, SysMenu::getMenuName);
    }
}
