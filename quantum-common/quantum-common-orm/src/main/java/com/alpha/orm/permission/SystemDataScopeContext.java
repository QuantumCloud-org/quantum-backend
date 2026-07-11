package com.alpha.orm.permission;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统级数据权限逃生门上下文（ThreadLocal，独立于 {@code UserContext}）。
 * <p>
 * 用深度计数器而非布尔标记实现，支持 {@link SystemDataScope#execute} 嵌套调用：
 * 仅当最外层调用退出时才真正清空 ThreadLocal，避免内层 finally 提前关闭
 * 外层仍在进行中的系统权限窗口（防泄漏回归见 SystemDataScopeTest 嵌套用例）。
 */
public final class SystemDataScopeContext {

    private static final ThreadLocal<AtomicInteger> DEPTH_HOLDER = new ThreadLocal<>();

    private SystemDataScopeContext() {
    }

    static void enter() {
        AtomicInteger depth = DEPTH_HOLDER.get();
        if (depth == null) {
            depth = new AtomicInteger(0);
            DEPTH_HOLDER.set(depth);
        }
        depth.incrementAndGet();
    }

    static void exit() {
        AtomicInteger depth = DEPTH_HOLDER.get();
        if (depth == null) {
            return;
        }
        if (depth.decrementAndGet() <= 0) {
            DEPTH_HOLDER.remove();
        }
    }

    /**
     * 当前线程是否处于系统级数据权限逃生门内（含嵌套）。
     */
    public static boolean isActive() {
        AtomicInteger depth = DEPTH_HOLDER.get();
        return depth != null && depth.get() > 0;
    }
}
