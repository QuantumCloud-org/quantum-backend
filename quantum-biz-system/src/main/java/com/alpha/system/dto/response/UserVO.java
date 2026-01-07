package com.alpha.system.dto.response;

import com.alpha.orm.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserVO extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer sex;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    private LocalDateTime loginDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 角色列表
     */
    private Set<String> roles;

    /**
     * 权限标识列表
     */
    private Set<String> permissions;

}