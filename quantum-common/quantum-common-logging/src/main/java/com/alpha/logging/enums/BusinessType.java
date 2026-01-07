package com.alpha.logging.enums;

/**
 * 业务操作类型
 */
public enum BusinessType {

    /**
     * 其他
     */
    OTHER(0, "其他"),

    /**
     * 新增
     */
    INSERT(1, "新增"),

    /**
     * 修改
     */
    UPDATE(2, "修改"),

    /**
     * 删除
     */
    DELETE(3, "删除"),

    /**
     * 查询
     */
    SELECT(4, "查询"),

    /**
     * 授权
     */
    GRANT(5, "授权"),

    /**
     * 导出
     */
    EXPORT(6, "导出"),

    /**
     * 导入
     */
    IMPORT(7, "导入"),

    /**
     * 强退
     */
    FORCE(8, "强退"),

    /**
     * 清空
     */
    CLEAN(9, "清空");

    private final int code;
    private final String desc;

    BusinessType(int code, String desc) {
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