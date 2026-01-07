package com.alpha.system.dto.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单查询参数
 */
@Data
public class MenuQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;

    /**
     * 是否显示（0-隐藏 1-显示）
     */
    private Integer visible;
}
