package com.alpha.framework.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码 (基于 HTTP 标准状态码)
 * <p>
 * 规范参考：
 * - 200: OK - 操作成功
 * - 400: Bad Request - 参数错误/业务逻辑错误
 * - 401: Unauthorized - 未认证/Token无效
 * - 403: Forbidden - 无权限/账号被禁
 * - 404: Not Found - 资源不存在
 * - 409: Conflict - 数据冲突/重复
 * - 429: Too Many Requests - 限流
 * - 500: Internal Server Error - 系统内部错误
 * - 503: Service Unavailable - 服务不可用
 * - 504: Gateway Timeout - 超时
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==================== 成功 2xx ====================
    SUCCESS(200, "操作成功"),

    // 400 Bad Request: 语义宽泛，用于参数校验失败或通用业务错误
    PARAM_ERROR(400, "参数错误"),
    PARAM_MISSING(400, "缺少必要参数"),
    PARAM_INVALID(400, "参数格式不正确"),
    PARAM_TYPE_ERROR(400, "参数类型错误"),

    // 业务错误通常也归类为 400（因为是客户端请求不符合业务规则），也可以用 422
    BIZ_ERROR(400, "业务处理失败"),
    OPERATION_FAILED(400, "操作失败"),

    // 401 Unauthorized: 未登录或 Token 问题
    UNAUTHORIZED(401, "未认证"),
    TOKEN_EXPIRED(401, "Token 已过期"),
    TOKEN_INVALID(401, "Token 无效"),

    // 403 Forbidden: 已登录但无权限，或账号状态异常
    ACCESS_DENIED(403, "无权限访问"),
    ACCOUNT_LOCKED(403, "账号已锁定"),
    ACCOUNT_DISABLED(403, "账号已禁用"),

    // 404 Not Found: 资源不存在
    DATA_NOT_FOUND(404, "数据不存在"),

    // 409 Conflict: 资源状态冲突（通常用于创建重复数据）
    DATA_ALREADY_EXISTS(409, "数据已存在"),
    DATA_CONFLICT(409, "数据冲突"),
    DUPLICATE_REQUEST(409, "重复请求"),

    // 429 Too Many Requests: 限流
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁"),

    // 500 Internal Server Error: 代码崩溃、DB异常等
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试"),
    DATABASE_ERROR(500, "数据库异常"),
    CACHE_ERROR(500, "缓存异常"),
    RPC_ERROR(500, "远程调用失败"),

    // 503 Service Unavailable: 服务停机维护或过载
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 504 Gateway Timeout: 下游超时
    TIMEOUT(504, "请求超时");

    private final int code;
    private final String message;

    /**
     * 根据 code 获取枚举
     * 注意：改为 HTTP 标准后，会有多个枚举对应同一个 code（例如多个 400），
     * 此方法只会返回第一个匹配项，建议仅用于根据 code 获取默认 message 场景。
     */
    public static ResultCode of(int code) {
        for (ResultCode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return SYSTEM_ERROR;
    }
}