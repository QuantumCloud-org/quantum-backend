package com.alpha.system.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 树形结构构建器
 */
public final class TreeBuilder {

    private TreeBuilder() {
    }

    /**
     * 构建树形结构（对象列表）
     *
     * @param list 平铺列表
     * @param <T>  节点类型，必须实现 TreeNode 接口
     * @return 树形结构列表
     */
    public static <T extends TreeNode<T>> List<T> buildTree(List<T> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有 ID
        var allIds = list.stream()
                .map(TreeNode::getId)
                .collect(Collectors.toSet());

        // 找出根节点（父节点不在列表中的都是根节点）
        List<T> roots = new ArrayList<>();
        for (T item : list) {
            Long parentId = item.getParentId();
            if (parentId == null || parentId == 0L || !allIds.contains(parentId)) {
                roots.add(item);
            }
        }

        // 递归构建子树
        for (T root : roots) {
            buildChildren(root, list);
        }

        return roots;
    }

    /**
     * 递归构建子节点
     */
    private static <T extends TreeNode<T>> void buildChildren(T parent, List<T> all) {
        List<T> children = getChildList(all, parent);
        if (!children.isEmpty()) {
            parent.setChildren(children);
            for (T child : children) {
                buildChildren(child, all);
            }
        }
    }

    /**
     * 获取直接子节点列表
     *
     * @param list   所有节点
     * @param parent 父节点
     * @return 子节点列表
     */
    public static <T extends TreeNode<T>> List<T> getChildList(List<T> list, T parent) {
        List<T> children = new ArrayList<>();
        Long parentId = parent.getId();
        for (T node : list) {
            if (parentId.equals(node.getParentId())) {
                children.add(node);
            }
        }
        return children;
    }

    // ==================== Map 结构的树构建 ====================

    /**
     * 构建树形结构（Map 列表）
     */
    public static <T extends Map<String, Object>> List<T> buildTree(
            List<T> list, String idKey, String parentIdKey, String childrenKey) {

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> roots = new ArrayList<>();

        for (T item : list) {
            Object parentId = item.get(parentIdKey);
            if (parentId == null || "0".equals(parentId.toString())) {
                roots.add(item);
            }
        }

        for (T root : roots) {
            buildMapChildren(root, list, idKey, parentIdKey, childrenKey);
        }

        return roots;
    }

    private static <T extends Map<String, Object>> void buildMapChildren(
            T parent, List<T> all, String idKey, String parentIdKey, String childrenKey) {

        Object parentId = parent.get(idKey);
        List<T> children = new ArrayList<>();

        for (T item : all) {
            Object itemParentId = item.get(parentIdKey);
            if (parentId != null && parentId.equals(itemParentId)) {
                children.add(item);
            }
        }

        if (!children.isEmpty()) {
            parent.put(childrenKey, children);
            for (T child : children) {
                buildMapChildren(child, all, idKey, parentIdKey, childrenKey);
            }
        }
    }

    /**
     * 树节点接口（泛型化）
     *
     * @param <T> 节点类型
     */
    public interface TreeNode<T> {

        Long getId();

        Long getParentId();

        List<T> getChildren();

        void setChildren(List<T> children);
    }
}