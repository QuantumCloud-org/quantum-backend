package com.alpha.system.dto.response;

import com.alpha.framework.enums.SensitiveStrategy;
import com.alpha.security.annotation.Sensitive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户视图对象
 */
@Data
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 版本号
     */
    private Long version;

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
    @Sensitive(SensitiveStrategy.EMAIL)
    private String email;

    /**
     * 手机号
     */
    @Sensitive(SensitiveStrategy.PHONE)
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
     * 登录地点
     */
    private String loginLocation;

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

    /** 实时审计日志清空权限，由认证信息接口提供。 */
    private Boolean canClearAuditLogs;

}
