package com.alpha.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户创建请求
 */
@Data
public class UserCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20之间")
    private String password;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 10, message = "昵称长度不能超过10个字符")
    private String nickname;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "邮箱格式不正确")
    private String email;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer sex;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 备注
     */
    private String remark;
}
