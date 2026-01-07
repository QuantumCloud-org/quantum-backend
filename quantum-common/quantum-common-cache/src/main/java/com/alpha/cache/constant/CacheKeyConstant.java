package com.alpha.cache.constant;

/**
 * 缓存键常量类
 * 设计原则：
 * 1. 所有 Key 集中管理，避免硬编码
 * 2. 前缀统一，便于批量删除
 * 3. 注释说明用途和过期策略
 */
public class CacheKeyConstant {
    /**
     * 验证码：auth:captcha:{uuid} → code (5分钟过期)
     */
    public static final String AUTH_CAPTCHA = "auth:captcha:";

    /**
     * 登录失败次数：auth:fail:{username} → count (锁定时间内)
     */
    public static final String AUTH_LOGIN_FAIL = "auth:fail:";

    /**
     * 用户信息：user:info:{userId} → UserVO
     */
    public static final String USER_INFO = "user:info:";

    /**
     * 用户在线状态：user:online:{userId} → timestamp
     */
    public static final String USER_ONLINE = "user:online:";

    /**
     * 系统参数：sys:config:{configKey} → configValue
     */
    public static final String SYS_CONFIG = "sys:config:";

    /**
     * 字典数据：sys:dict:{dictType} → List<DictData>
     */
    public static final String SYS_DICT = "sys:dict:";

    /**
     * 通用业务锁：lock:biz:{业务标识}
     */
    public static final String LOCK_BIZ = "lock:biz:";

    /**
     * 用户操作锁：lock:user:{userId}:{操作类型}
     */
    public static final String LOCK_USER = "lock:user:";

    /**
     * 订单锁：lock:order:{orderId}
     */
    public static final String LOCK_ORDER = "lock:order:";

    /**
     * 接口限流缓存键前缀
     * * 格式：limit:{接口路径}
     * * 过期时间：1分钟
     */
    public static final String LIMIT_KEY_PREFIX = "limit:";

    /**
     * 接口限流：limit:api:{接口标识}
     */
    public static final String LIMIT_API = "limit:api:";

    /**
     * 用户限流：limit:user:{userId}:{接口标识}
     */
    public static final String LIMIT_USER = "limit:user:";

    /**
     * IP 限流：limit:ip:{ip}
     */
    public static final String LIMIT_IP = "limit:ip:";

    /**
     * 用户名布隆过滤器（判断用户名是否存在）
     */
    public static final String BLOOM_USERNAME = "bloom:username";

    /**
     * 订单号布隆过滤器（防止重复订单）
     */
    public static final String BLOOM_ORDER_NO = "bloom:orderno";

    /**
     * 订单超时取消队列
     */
    public static final String DELAY_ORDER_CANCEL = "delay:order:cancel";

    /**
     * 消息延迟发送队列
     */
    public static final String DELAY_MESSAGE_SEND = "delay:message:send";

    /**
     * 幂等 Token：idempotent:{token} → 1 (使用后删除)
     */
    public static final String IDEMPOTENT_TOKEN = "idempotent:";

}