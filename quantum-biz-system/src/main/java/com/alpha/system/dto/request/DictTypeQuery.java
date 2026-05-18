package com.alpha.system.dto.request;

import com.alpha.orm.entity.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeQuery extends PageQuery {

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 业务类型(admin 管理员，talent 人才)
     */
    private String business;

    /**
     * 状态
     */
    private Integer status;
}