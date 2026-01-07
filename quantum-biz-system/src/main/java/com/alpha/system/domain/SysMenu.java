package com.alpha.system.domain;

import com.alpha.orm.entity.BaseEntity;
import com.alpha.system.support.TreeBuilder;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_menu")
public class SysMenu extends BaseEntity implements Serializable, TreeBuilder.TreeNode<SysMenu> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 子菜单（非数据库字段）
     */
    @Column(ignore = true)
    private List<SysMenu> children;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 路由参数
     */
    private String queryParam;

    /**
     * 是否为外链（0-否 1-是）
     */
    private Integer isFrame;

    /**
     * 是否缓存（0-不缓存 1-缓存）
     */
    private Integer isCache;

    /**
     * 菜单类型（M-目录 C-菜单 F-按钮）
     */
    private String menuType;

    /**
     * 是否显示（0-隐藏 1-显示）
     */
    private Integer visible;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}