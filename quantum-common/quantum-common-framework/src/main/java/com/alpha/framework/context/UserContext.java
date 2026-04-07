package com.alpha.framework.context;

import com.alpha.framework.entity.LoginUser;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;

/**
 * 用户上下文
 * <p>
 * 统一持有 LoginUser 对象，消除 UserInfo 内部类冗余
 */
@Slf4j
public class UserContext {

    /**
     * 用户信息（支持异步传递）
     */
    private static final ThreadLocal<LoginUser> USER_HOLDER = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> IP_HOLDER = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT_HOLDER = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_URI_HOLDER = new InheritableThreadLocal<>();

    // ==================== 用户操作 ====================

    public static void setUser(LoginUser user) {
        USER_HOLDER.set(user);
    }

    public static LoginUser getUser() {
        return USER_HOLDER.get();
    }

    public static void clearUser() {
        USER_HOLDER.remove();
    }

    // ==================== 属性获取便捷方法 ====================

    public static Long getUserId() {
        LoginUser user = getUser();
        return user != null ? user.getUserId() : null;
    }

    public static String getUsername() {
        LoginUser user = getUser();
        return user != null ? user.getUsername() : null;
    }

    public static String getNickname() {
        LoginUser user = getUser();
        return user != null ? user.getNickname() : null;
    }

    public static Long getDeptId() {
        LoginUser user = getUser();
        return user != null ? user.getDeptId() : null;
    }

    public static String getDeptName() {
        LoginUser user = getUser();
        return user != null ? user.getDeptName() : null;
    }

    public static Set<String> getRoles() {
        LoginUser user = getUser();
        return user != null && user.getRoles() != null ? user.getRoles() : Collections.emptySet();
    }

    public static Set<String> getPermissions() {
        LoginUser user = getUser();
        return user != null && user.getPermissions() != null ? user.getPermissions() : Collections.emptySet();
    }

    public static boolean isAdmin() {
        LoginUser user = getUser();
        return user != null && user.isAdmin();
    }

    public static boolean isAuthenticated() {
        return getUser() != null;
    }

    // ==================== 数据权限相关 ====================

    public static Set<Long> getDeptIds() {
        LoginUser user = getUser();
        return user != null ? user.getDeptIds() : null;
    }

    public static Integer getDataScope() {
        LoginUser user = getUser();
        return user != null ? user.getDataScope() : null;
    }

    public static void setDeptIds(Set<Long> deptIds) {
        LoginUser user = getUser();
        if (user != null) {
            user.setDeptIds(deptIds);
        }
    }

    // ==================== 其他上下文操作 (保持不变) ====================

    public static void setIp(String ip) {
        IP_HOLDER.set(ip);
    }

    public static String getIp() {
        return IP_HOLDER.get();
    }

    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    public static void setUserAgent(String userAgent) {
        USER_AGENT_HOLDER.set(userAgent);
    }

    public static String getUserAgent() {
        return USER_AGENT_HOLDER.get();
    }

    public static void setRequestUri(String uri) {
        REQUEST_URI_HOLDER.set(uri);
    }

    public static String getRequestUri() {
        return REQUEST_URI_HOLDER.get();
    }

    /**
     * 清除所有
     */
    public static void clear() {
        USER_HOLDER.remove();
        IP_HOLDER.remove();
        TRACE_ID_HOLDER.remove();
        USER_AGENT_HOLDER.remove();
        REQUEST_URI_HOLDER.remove();
    }
}