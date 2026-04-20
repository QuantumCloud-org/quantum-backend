package com.alpha.system.dto.request;

import com.alpha.orm.entity.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文件管理分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysFileQuery extends PageQuery {

    /** 原始文件名（模糊） */
    private String originalName;

    /** 扩展名（精确小写） */
    private String extension;

    /** 业务类型 */
    private String bizType;

    /** 业务 ID */
    private Long bizId;

    /** 上传人 ID */
    private Long uploadBy;

    /** 上传时间起 */
    private LocalDateTime beginTime;

    /** 上传时间止 */
    private LocalDateTime endTime;
}
