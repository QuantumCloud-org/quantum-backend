package com.alpha.system.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        Map<Long, List<T>> childrenByParent = groupByParent(list);
        Set<Long> ids = new HashSet<>();
        list.forEach(node -> {
            node.setChildren(new ArrayList<>());
            ids.add(node.getId());
        });
        Set<T> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        List<T> roots = new ArrayList<>();
        for (T node : list) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0 || !ids.contains(parentId)) {
                attach(node, roots, childrenByParent, visited);
            }
        }
        for (T node : list) {
            attach(node, roots, childrenByParent, visited);
        }
        return roots;
    }

    public static <T extends TreeNode<T>> Map<Long, List<T>> groupByParent(List<T> nodes) {
        Map<Long, List<T>> groups = new HashMap<>();
        for (T node : nodes) {
            Long parentId = node.getParentId();
            if (parentId != null && !parentId.equals(node.getId())) {
                groups.computeIfAbsent(parentId, id -> new ArrayList<>()).add(node);
            }
        }
        return groups;
    }

    private static <T extends TreeNode<T>> void attach(T root, List<T> roots, Map<Long, List<T>> groups, Set<T> visited) {
        if (!visited.add(root)) {
            return;
        }
        roots.add(root);
        Deque<T> remaining = new ArrayDeque<>();
        remaining.add(root);
        while (!remaining.isEmpty()) {
            T parent = remaining.removeFirst();
            for (T child : groups.getOrDefault(parent.getId(), List.of())) {
                if (visited.add(child)) {
                    parent.getChildren().add(child);
                    remaining.addLast(child);
                }
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
