package com.alpha.system.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色部门关联实体（用于自定义数据权限）
 */
@Data
@Accessors(chain = true)
@Table("sys_role_dept")
public class SysRoleDept implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID
     */
    @Id
    private Long roleId;

    /**
     * 部门 ID
     */
    @Id
    private Long deptId;
}