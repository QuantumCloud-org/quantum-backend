package com.alpha.system.dto.request;

import com.alpha.orm.entity.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictDataQuery extends PageQuery {

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典标签
     */
    private String dictLabel;

    /**
     * 状态
     */
    private Integer status;
}