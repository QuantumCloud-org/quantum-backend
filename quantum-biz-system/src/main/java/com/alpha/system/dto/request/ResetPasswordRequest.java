package com.alpha.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 重置密码请求
 */
@Data
public class ResetPasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20之间")
    private String password;
}
