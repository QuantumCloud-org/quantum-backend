package com.alpha.orm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据权限类型
 */
@Getter
@RequiredArgsConstructor
public enum DataScopeType {

    /**
     * 默认（使用用户配置的数据权限）
     */
    DEFAULT(0, "默认"),

    /**
     * 全部数据权限
     */
    ALL(1, "全部数据"),

    /**
     * 本部门数据权限
     */
    DEPT(2, "本部门"),

    /**
     * 本部门及子部门数据权限
     */
    DEPT_AND_CHILD(3, "本部门及子部门"),

    /**
     * 自定义数据权限（角色配置的部门）
     */
    CUSTOM(4, "自定义"),

    /**
     * 仅本人数据权限
     */
    SELF(5, "仅本人");

    private final int code;
    private final String desc;

    /**
     * 根据 code 获取枚举
     */
    public static DataScopeType fromCode(Integer code) {
        if (code == null) {
            return SELF;
        }
        for (DataScopeType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return SELF;
    }
}