package com.alpha.system.dto.response;

import com.alpha.system.domain.SysDept;
import com.alpha.system.domain.SysMenu;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 树形选择结构
 */
@Data
public class TreeSelectVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 节点名称
     */
    private String label;

    /**
     * 子节点
     */
    private List<TreeSelectVO> children;

    public TreeSelectVO() {
        this.children = new ArrayList<>();
    }

    public TreeSelectVO(Long id, String label) {
        this.id = id;
        this.label = label;
        this.children = new ArrayList<>();
    }

    /**
     * 从 SysMenu 构建
     */
    public static TreeSelectVO fromMenu(SysMenu menu) {
        return new TreeSelectVO(menu.getId(), menu.getMenuName());
    }

    /**
     * 从 SysDept 构建
     */
    public static TreeSelectVO fromDept(SysDept dept) {
        return new TreeSelectVO(dept.getId(), dept.getDeptName());
    }
}