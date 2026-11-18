package com.alpha.system.dto.response;

import com.alpha.system.support.TreeBuilder.TreeNode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    public static <T extends TreeNode<T>> List<TreeSelectVO> fromTree(List<T> nodes, Function<T, String> label) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream().map(node -> {
            var option = new TreeSelectVO(node.getId(), label.apply(node));
            option.setChildren(fromTree(node.getChildren(), label));
            return option;
        }).toList();
    }
}
