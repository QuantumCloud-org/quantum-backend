package com.alpha.system.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户角色关联实体
 */
@Data
@Accessors(chain = true)
@Table("sys_user_role")
public class SysUserRole implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @Id
    private Long userId;

    /**
     * 角色 ID
     */
    @Id
    private Long roleId;
}