package com.alpha.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysMenu;
import com.alpha.system.dto.response.RouterVO;
import com.alpha.system.dto.response.TreeSelectVO;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMenuMapper;
import com.alpha.system.service.ISysMenuService;
import com.alpha.system.support.TreeBuilder;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alpha.system.domain.table.SysMenuTableDef.SYS_MENU;
import static com.alpha.system.domain.table.SysRoleMenuTableDef.SYS_ROLE_MENU;

/**
 * 菜单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * 目录类型
     */
    private static final String TYPE_DIR = "M";
    /**
     * 菜单类型
     */
    private static final String TYPE_MENU = "C";
    /**
     * 按钮类型
     */
    private static final String TYPE_BUTTON = "F";
    /**
     * Layout组件
     */
    private static final String LAYOUT = "Layout";
    /**
     * ParentView组件
     */
    private static final String PARENT_VIEW = "ParentView";
    /**
     * InnerLink组件
     */
    private static final String INNER_LINK = "InnerLink";

    @Override
    public List<SysMenu> selectMenuTree(SysMenu query) {
        QueryWrapper wrapper = QueryWrapper.create();

        if (StrUtil.isNotBlank(query.getMenuName())) {
            wrapper.and(SYS_MENU.MENU_NAME.like(query.getMenuName()));
        }
        if (query.getStatus() != null) {
            wrapper.and(SYS_MENU.STATUS.eq(query.getStatus()));
        }

        wrapper.orderBy(SYS_MENU.PARENT_ID.asc(), SYS_MENU.ORDER_NUM.asc());

        List<SysMenu> menus = list(wrapper);
        return TreeBuilder.buildTree(menus);
    }

    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> menus;
        // 管理员显示所有菜单
        if (userId == 1L) {
            QueryWrapper wrapper = QueryWrapper.create()
                    .where(SYS_MENU.MENU_TYPE.in("M", "C"))
                    .and(SYS_MENU.STATUS.eq(1))
                    .and(SYS_MENU.DELETED.eq(0))
                    .orderBy(SYS_MENU.PARENT_ID.asc(), SYS_MENU.ORDER_NUM.asc());
            menus = list(wrapper);
        } else {
            menus = menuMapper.selectMenusByUserId(userId);
        }
        return TreeBuilder.buildTree(menus);
    }

    @Override
    public List<TreeSelectVO> buildMenuTreeSelect(List<SysMenu> menus) {
        List<SysMenu> menuTrees = TreeBuilder.buildTree(menus);
        return menuTrees.stream().map(this::buildTreeSelect).collect(Collectors.toList());
    }

    @Override
    public Set<Long> selectMenuIdsByRoleId(Long roleId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select(SYS_ROLE_MENU.MENU_ID)
                .where(SYS_ROLE_MENU.ROLE_ID.eq(roleId));
        return roleMenuMapper.selectListByQueryAs(wrapper, Long.class).stream().collect(Collectors.toSet());
    }

    @Override
    public Set<String> selectPermsByUserId(Long userId) {
        // 管理员拥有所有权限
        if (userId == 1L) {
            return Set.of("*:*:*");
        }
        return menuMapper.selectPermsByUserId(userId);
    }

    @Override
    public SysMenu selectMenuById(Long menuId) {
        return getById(menuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertMenu(SysMenu menu) {
        // 检查名称唯一
        if (!checkMenuNameUnique(menu.getMenuName(), menu.getParentId(), null)) {
            throw new BizException("菜单名称已存在");
        }

        save(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(SysMenu menu) {
        // 检查名称唯一
        if (!checkMenuNameUnique(menu.getMenuName(), menu.getParentId(), menu.getId())) {
            throw new BizException("菜单名称已存在");
        }

        // 不能设置自己为父菜单
        if (menu.getId().equals(menu.getParentId())) {
            throw new BizException("父菜单不能是自己");
        }

        return updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenuById(Long menuId) {
        // 检查是否有子菜单
        if (hasChildMenu(menuId)) {
            throw new BizException("存在子菜单，不能删除");
        }
        // 检查是否被角色使用
        if (isMenuUsed(menuId)) {
            throw new BizException("菜单已分配给角色，不能删掉");
        }

        return removeById(menuId);
    }

    @Override
    public boolean checkMenuNameUnique(String menuName, Long parentId, Long excludeId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_MENU.MENU_NAME.eq(menuName))
                .and(SYS_MENU.PARENT_ID.eq(parentId))
                .and(SYS_MENU.DELETED.eq(0))
                .and(excludeId != null ? SYS_MENU.ID.ne(excludeId) : null);
        return count(wrapper) == 0;
    }

    @Override
    public boolean hasChildMenu(Long menuId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_MENU.PARENT_ID.eq(menuId))
                .and(SYS_MENU.DELETED.eq(0));
        return count(wrapper) > 0;
    }

    @Override
    public boolean isMenuUsed(Long menuId) {
        QueryWrapper wrapper = QueryWrapper.create().where(SYS_ROLE_MENU.MENU_ID.eq(menuId));
        return count(wrapper) > 0;
    }

    @Override
    public List<RouterVO> buildRouters(List<SysMenu> menus) {
        List<RouterVO> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            RouterVO router = new RouterVO();
            router.setHidden(menu.getVisible() == 0);
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setQuery(menu.getQueryParam());
            router.setMeta(buildMeta(menu));

            List<SysMenu> childMenus = menu.getChildren();
            if (CollUtil.isNotEmpty(childMenus)) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildRouters(childMenus));
            } else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVO> childrenList = new ArrayList<>();
                RouterVO children = new RouterVO();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(StrUtil.upperFirst(menu.getPath()));
                children.setMeta(buildMeta(menu));
                children.setQuery(menu.getQueryParam());
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId() == 0 && isInnerLink(menu)) {
                router.setMeta(buildMeta(menu));
                router.setPath("/");
                List<RouterVO> childrenList = new ArrayList<>();
                RouterVO children = new RouterVO();
                String routerPath = innerLinkReplaceEach(menu.getPath());
                children.setPath(routerPath);
                children.setComponent(INNER_LINK);
                children.setName(StrUtil.upperFirst(routerPath));
                children.setMeta(buildMeta(menu));
                childrenList.add(children);
                router.setChildren(childrenList);
            }

            routers.add(router);
        }
        return routers;
    }

    /**
     * 构建路由元信息
     */
    private RouterVO.MetaVO buildMeta(SysMenu menu) {
        boolean requiresAuth = StrUtil.isNotBlank(menu.getPerms());
        return new RouterVO.MetaVO(
                menu.getMenuName(),
                menu.getIcon(),
                menu.getIsCache() == 0,
                menu.getPath(),
                requiresAuth,
                menu.getPerms(),
                null
        );
    }

    /**
     * 构建下拉树
     */
    private TreeSelectVO buildTreeSelect(SysMenu menu) {
        TreeSelectVO treeSelect = TreeSelectVO.fromMenu(menu);
        if (CollUtil.isNotEmpty(menu.getChildren())) {
            List<TreeSelectVO> children = menu.getChildren().stream()
                    .map(this::buildTreeSelect)
                    .collect(Collectors.toList());
            treeSelect.setChildren(children);
        }
        return treeSelect;
    }

    /**
     * 获取路由名称
     */
    private String getRouteName(SysMenu menu) {
        String routerName = StrUtil.upperFirst(menu.getPath());
        if (isMenuFrame(menu)) {
            routerName = StrUtil.EMPTY;
        }
        return routerName;
    }

    /**
     * 获取路由地址
     */
    private String getRouterPath(SysMenu menu) {
        String routerPath = menu.getPath();
        // 内链打开外网方式
        if (menu.getParentId() != 0 && isInnerLink(menu)) {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // 非外链并且是一级目录
        if (menu.getParentId() == 0 && TYPE_DIR.equals(menu.getMenuType()) && menu.getIsFrame() == 0) {
            routerPath = "/" + menu.getPath();
        } else if (isMenuFrame(menu)) {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     */
    private String getComponent(SysMenu menu) {
        String component = LAYOUT;
        if (StrUtil.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu)) {
            component = menu.getComponent();
        } else if (StrUtil.isEmpty(menu.getComponent()) && menu.getParentId() != 0 && isInnerLink(menu)) {
            component = INNER_LINK;
        } else if (StrUtil.isEmpty(menu.getComponent()) && isParentView(menu)) {
            component = PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     */
    private boolean isMenuFrame(SysMenu menu) {
        return menu.getParentId() == 0 && TYPE_MENU.equals(menu.getMenuType()) && menu.getIsFrame() == 0;
    }

    /**
     * 是否为内链组件
     */
    private boolean isInnerLink(SysMenu menu) {
        return menu.getIsFrame() == 0 && StrUtil.startWith(menu.getPath(), "http");
    }

    /**
     * 是否为parent_view组件
     */
    private boolean isParentView(SysMenu menu) {
        return menu.getParentId() != 0 && TYPE_DIR.equals(menu.getMenuType());
    }

    /**
     * 内链域名特殊字符替换
     */
    private String innerLinkReplaceEach(String path) {
        return StringUtils.replaceEach(path, new String[]{"http://", "https://", "www.", "."}, new String[]{"", "", "", "/"});
    }

}