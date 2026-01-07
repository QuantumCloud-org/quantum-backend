package com.alpha.system.domain;

import com.alpha.orm.entity.BaseEntity;
import com.alpha.system.support.TreeBuilder;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 部门实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dept")
public class SysDept extends BaseEntity implements Serializable, TreeBuilder.TreeNode<SysDept> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 子部门（非数据库字段）
     */
    @Column(ignore = true)
    private List<SysDept> children;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 祖级列表（如：0,100,101）
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
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;
}