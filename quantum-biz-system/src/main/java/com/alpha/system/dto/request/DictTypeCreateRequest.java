package com.alpha.system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictTypeCreateRequest {
    @NotBlank
    private String dictName;

    @NotBlank
    private String dictType;

    private String business;

    @Min(0) @Max(1)
    private Integer status;

    private String remark;
}
