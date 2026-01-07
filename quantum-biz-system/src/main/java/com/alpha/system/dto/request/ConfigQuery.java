package com.alpha.system.dto.request;

import com.alpha.orm.entity.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigQuery extends PageQuery {

    /**
     * 参数名称
     */
    private String configName;

    /**
     * 参数键名
     */
    private String configKey;

    /**
     * 系统内置（Y-是 N-否）
     */
    private String configType;
}