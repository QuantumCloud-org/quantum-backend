package com.alpha.system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 个人资料更新请求
 */
@Data
public class ProfileUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本号
     */
    @NotNull(message = "版本号不能为空")
    private Long version;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 10, message = "昵称长度不能超过10个字符")
    private String nickname;

    /**
     * 邮箱
     */
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别取值不正确")
    @Max(value = 2, message = "性别取值不正确")
    private Integer sex;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    public void setNickname(String nickname) {
        this.nickname = trim(nickname);
    }

    public void setEmail(String email) {
        this.email = trimToNull(email);
    }

    public void setPhone(String phone) {
        this.phone = trimToNull(phone);
    }

    public void setRemark(String remark) {
        this.remark = trimToNull(remark);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
