package com.alpha.system.dto.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 部门查询参数
 */
@Data
public class DeptQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;
}
