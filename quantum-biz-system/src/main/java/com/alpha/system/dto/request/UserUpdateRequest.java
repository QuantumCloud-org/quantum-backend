package com.alpha.system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户更新请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 版本号
     */
    @NotNull(message = "版本号不能为空")
    private Long version;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 10, message = "昵称长度不能超过10个字符")
    private String nickname;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "邮箱格式不正确")
    private String email;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别取值不正确")
    @Max(value = 2, message = "性别取值不正确")
    private Integer sex;

    /**
     * 部门ID
     */
    @NotNull(message = "部门不能为空")
    private Long deptId;

    /**
     * 状态（0-禁用 1-正常）
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值不正确")
    @Max(value = 1, message = "状态取值不正确")
    private Integer status;

    /**
     * 角色ID列表
     */
    @NotEmpty(message = "请至少选择一个角色")
    private List<Long> roleIds;

    /**
     * 备注
     */
    private String remark;

    public void setUsername(String username) {
        this.username = trim(username);
    }

    public void setNickname(String nickname) {
        this.nickname = trim(nickname);
    }

    public void setPhone(String phone) {
        this.phone = trim(phone);
    }

    public void setEmail(String email) {
        this.email = trim(email);
    }

    public void setRemark(String remark) {
        this.remark = trim(remark);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
