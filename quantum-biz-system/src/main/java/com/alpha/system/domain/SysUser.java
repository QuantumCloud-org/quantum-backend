package com.alpha.system.domain;

import com.alpha.orm.entity.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
public class SysUser extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

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
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称（非数据库字段，列表/导出回填）
     */
    @Column(ignore = true)
    private String deptName;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;

    /**
     * 数据权限（1-全部 2-本部门 3-本部门及子部门 4-自定义 5-仅本人）
     */
    private Integer dataScope;

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
}
