package com.alpha.orm.permission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemDataScope 逃生门单测。
 * <p>
 * 覆盖 design.md 交付 3 · TDD 分支 ⑤（critic R2 P2 嵌套回归）：
 * 深度计数器保证嵌套 execute() 时只有最外层退出才真正清空标记。
 */
class SystemDataScopeTest {

    @Test
    void executeShouldActivateContextOnlyDuringExecution() {
        assertThat(SystemDataScopeContext.isActive()).isFalse();

        SystemDataScope.execute(() -> {
            assertThat(SystemDataScopeContext.isActive()).isTrue();
            return null;
        });

        assertThat(SystemDataScopeContext.isActive()).isFalse();
    }

    @Test
    void nestedExecuteShouldKeepOuterContextActiveUntilOutermostExits() {
        SystemDataScope.execute(() -> {
            assertThat(SystemDataScopeContext.isActive()).isTrue();

            SystemDataScope.execute(() -> {
                assertThat(SystemDataScopeContext.isActive()).isTrue();
                return null;
            });

            // 内层 execute 已退出，但外层仍应保持激活 —— 防止布尔标记被内层 finally 提前清掉
            assertThat(SystemDataScopeContext.isActive()).isTrue();
            return null;
        });

        assertThat(SystemDataScopeContext.isActive()).isFalse();
    }

    @Test
    void executeShouldClearContextEvenWhenActionThrows() {
        assertThat(SystemDataScopeContext.isActive()).isFalse();

        try {
            SystemDataScope.execute(() -> {
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException expected) {
            // 断言异常路径下 ThreadLocal 依旧被清理
        }

        assertThat(SystemDataScopeContext.isActive()).isFalse();
    }
}
