package com.alpha.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码长度不能超过100个字符")
    private String password;

    /**
     * 验证码
     */
    private String captchaCode;

    /**
     * 验证码 Key
     */
    private String captchaKey;

    /**
     * 记住我
     */
    private Boolean rememberMe = false;

    /**
     * 设备标识
     */
    private String deviceId;
}