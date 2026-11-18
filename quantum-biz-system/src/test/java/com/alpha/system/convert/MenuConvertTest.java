package com.alpha.system.convert;

import com.alpha.system.domain.SysMenu;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenuConvertTest {
    @Test
    void includesChildrenInMenuTreeSelect() {
        var root = new SysMenu();
        root.setId(3L);
        var child = new SysMenu();
        child.setId(4L);
        child.setMenuName("Child");
        root.setChildren(List.of(child));
        var tree = new MenuConvert().toTreeSelectList(List.of(root));
        assertThat(tree.getFirst().getChildren()).singleElement().satisfies(node -> {
            assertThat(node.getId()).isEqualTo(4L);
            assertThat(node.getLabel()).isEqualTo("Child");
        });
    }
}
