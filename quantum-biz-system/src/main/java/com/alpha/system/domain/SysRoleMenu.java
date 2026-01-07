package com.alpha.system.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色菜单关联实体
 */
@Data
@Accessors(chain = true)
@Table("sys_role_menu")
public class SysRoleMenu implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID
     */
    @Id
    private Long roleId;

    /**
     * 菜单 ID
     */
    @Id
    private Long menuId;
}