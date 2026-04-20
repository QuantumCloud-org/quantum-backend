package com.alpha.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户状态更新请求
 */
@Data
public class UserStatusUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "版本号不能为空")
    private Long version;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
