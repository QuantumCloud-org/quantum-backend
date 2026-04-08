package com.alpha.logging.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * logging 模块自有分页查询参数
 * <p>
 * 该类仅服务于日志模块对外接口，避免为通用分页参数反向依赖 ORM 模块。
 */
@Data
public class LogPageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 1000, message = "每页条数最大为 1000")
    private Integer pageSize = 10;
}
