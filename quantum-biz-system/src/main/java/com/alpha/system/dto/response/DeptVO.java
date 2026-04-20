package com.alpha.system.dto.response;

import com.alpha.framework.enums.SensitiveStrategy;
import com.alpha.security.annotation.Sensitive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门视图对象
 */
@Data
public class DeptVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long id;

    /**
     * 子部门（非数据库字段）
     */
    private List<DeptVO> children;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    @Sensitive(SensitiveStrategy.PHONE)
    private String phone;

    /**
     * 邮箱
     */
    @Sensitive(SensitiveStrategy.EMAIL)
    private String email;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
