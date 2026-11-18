package com.alpha.system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DictDataCreateRequest {
    @NotBlank
    private String dictType;

    @NotBlank
    private String dictLabel;

    @NotBlank
    private String dictValue;

    private Integer dictSort;

    private String cssClass;

    private String listClass;

    @Pattern(regexp = "[YN]")
    private String isDefault;

    @Min(0) @Max(1)
    private Integer status;

    private String remark;
}
