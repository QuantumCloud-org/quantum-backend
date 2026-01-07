package com.alpha.system.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 用户详情视图对象（包含角色和权限）
 */
@Data
public class UserDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 角色ID列表
     */
    private Set<Long> roleIds;

    /**
     * 角色key列表
     */
    private Set<String> roleKeys;

    /**
     * 权限标识列表
     */
    private Set<String> permissions;
}
