package com.alpha.framework.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    // ==================== 时间格式 ====================

    /**
     * 日期格式
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式
     */
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 日期时间格式
     */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时区
     */
    public static final String TIMEZONE = "Asia/Shanghai";

    // ==================== HTTP Header ====================

    /**
     * 链路追踪 ID Header
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 跨度 ID Header（分布式追踪）
     */
    public static final String HEADER_SPAN_ID = "X-Span-Id";

    /**
     * 父跨度 ID Header
     */
    public static final String HEADER_PARENT_SPAN_ID = "X-Parent-Span-Id";

    /**
     * 租户 ID
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * 刷新token Header
     */
    public static final String HEADER_REFRESH_TOKEN = "X-Refresh-Token";

    /**
     * Token Header
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    // ==================== MDC Keys ====================

    /**
     * MDC - 链路追踪 ID
     */
    public static final String MDC_TRACE_ID = "traceId";

    /**
     * MDC - 跨度 ID
     */
    public static final String MDC_SPAN_ID = "spanId";

    /**
     * MDC - 客户端 IP
     */
    public static final String MDC_IP = "ip";

    /**
     * MDC - 请求 URI
     */
    public static final String MDC_REQUEST_URI = "requestUri";

    /**
     * MDC - 用户 ID
     */
    public static final String MDC_USER_ID = "userId";

    /**
     * MDC - 用户名
     */
    public static final String MDC_USERNAME = "username";

    // ==================== Redis Key 前缀 ====================

    /**
     * Token 缓存前缀
     */
    public static final String REDIS_TOKEN_PREFIX = "auth:token:";

    /**
     * 用户 Token 映射前缀
     */
    public static final String USER_TOKEN_PREFIX = "auth:user:tokens:";

    /**
     * 在线用户 Token 索引集合
     */
    public static final String ONLINE_TOKENS_KEY = "online:tokens";

    /**
     * Token 黑名单前缀
     */
    public static final String BLACKLIST_PREFIX = "auth:blacklist:";

    /**
     * 用户权限缓存前缀
     */
    public static final String REDIS_PERM_PREFIX = "auth:perm:";

    /**
     * 限流前缀
     */
    public static final String REDIS_RATE_LIMIT_PREFIX = "rate:";

    /**
     * 防重复提交前缀
     */
    public static final String REDIS_REPEAT_SUBMIT_PREFIX = "repeat:";

    // ==================== 系统常量 ====================

    /**
     * 超级管理员 ID
     */
    public static final Long SUPER_ADMIN_ID = 1L;

    /**
     * 超级管理员角色标识
     */
    public static final String SUPER_ADMIN_ROLE = "admin";

    /**
     * 顶级部门/菜单父 ID
     */
    public static final Long ROOT_PARENT_ID = 0L;

    /**
     * 状态：正常
     */
    public static final Integer STATUS_NORMAL = 1;

    /**
     * 状态：禁用
     */
    public static final Integer STATUS_DISABLE = 0;

    /**
     * 是
     */
    public static final String YES = "Y";

    /**
     * 否
     */
    public static final String NO = "N";
}