package com.alpha.orm.permission;

import java.util.function.Supplier;

/**
 * 系统级数据权限逃生门。
 * <p>
 * 用于定时任务 / MQ listener 等无用户上下文（{@code UserContext.getUserId() == null}）
 * 但确实需要访问全部数据的系统内部调用场景。显式声明该调用需要绕过数据权限过滤
 * （等价于 {@code DataScopeType.ALL}），避免落入 fail-closed 的 {@code DENY_ALL} 默认行为。
 * <p>
 * 否决备选：伪造 synthetic system {@code LoginUser} 塞入 {@code UserContext} —
 * 会污染 {@code isAdmin()} 语义与操作审计字段。本方案用独立 ThreadLocal 标记，
 * 侵入面最小、审计可 grep（见 {@code DataScopeAspect} 的 log.info 审计行）。
 * <pre>
 * {@code
 * SystemDataScope.execute(() -> sysUserService.selectUserList(query));
 * }
 * </pre>
 */
public final class SystemDataScope {

    private SystemDataScope() {
    }

    /**
     * 在系统级数据权限窗口内执行 {@code action} 并返回结果。
     * <p>
     * try/finally 保证 ThreadLocal 必清；深度计数器支持嵌套调用
     * （仅最外层退出时才真正清空，见 {@link SystemDataScopeContext}）。
     */
    public static <T> T execute(Supplier<T> action) {
        SystemDataScopeContext.enter();
        try {
            return action.get();
        } finally {
            SystemDataScopeContext.exit();
        }
    }

    /**
     * 无返回值版本，等价于 {@link #execute(Supplier)}。
     */
    public static void execute(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }
}
