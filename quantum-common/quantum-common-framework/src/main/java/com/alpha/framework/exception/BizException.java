package com.alpha.framework.exception;

import com.alpha.framework.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 使用场景：业务逻辑校验失败时抛出
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;

    public BizException(String message) {
        this(ResultCode.BIZ_ERROR.getCode(), message);
    }

    public BizException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    public BizException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 快捷抛出异常
     */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BizException(message);
        }
    }

    public static void throwIf(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BizException(resultCode);
        }
    }

    public static void throwIfNull(Object obj, String message) {
        throwIf(obj == null, message);
    }

    public static void throwIfBlank(String str, String message) {
        throwIf(str == null || str.isBlank(), message);
    }

    /**
     * 不记录堆栈（性能优化）
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}