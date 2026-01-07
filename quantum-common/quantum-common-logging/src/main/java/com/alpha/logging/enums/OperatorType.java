package com.alpha.logging.enums;

/**
 * 操作人类型
 */
public enum OperatorType {

    /**
     * 其他
     */
    OTHER(0, "其他"),

    /**
     * 后台用户
     */
    MANAGE(1, "后台用户"),

    /**
     * 手机端用户
     */
    MOBILE(2, "手机端用户"),

    /**
     * 第三方接口
     */
    API(3, "第三方接口");

    private final int code;
    private final String desc;

    OperatorType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}