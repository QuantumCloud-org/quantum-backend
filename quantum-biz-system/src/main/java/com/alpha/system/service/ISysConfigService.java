package com.alpha.system.service;

import com.alpha.system.domain.SysConfig;
import com.alpha.system.dto.request.ConfigQuery;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 系统配置服务接口
 */
public interface ISysConfigService extends IService<SysConfig> {

    /**
     * 分页查询配置
     */
    Page<SysConfig> selectConfigPage(ConfigQuery query);

    /**
     * 查询配置列表
     */
    List<SysConfig> selectConfigList(ConfigQuery query);

    /**
     * 根据ID查询配置
     */
    SysConfig selectConfigById(Long configId);

    /**
     * 根据键名查询配置值
     */
    String selectConfigByKey(String configKey);

    /**
     * 根据键名查询配置值（带默认值）
     */
    String selectConfigByKey(String configKey, String defaultValue);

    /**
     * 根据键名查询配置值（布尔类型）
     */
    boolean selectConfigByKeyAsBoolean(String configKey, boolean defaultValue);

    /**
     * 新增配置
     */
    Long insertConfig(SysConfig config);

    /**
     * 修改配置
     */
    boolean updateConfig(SysConfig config);

    /**
     * 删除配置
     */
    boolean deleteConfigByIds(List<Long> configIds);

    /**
     * 检查键名是否唯一
     */
    boolean checkConfigKeyUnique(String configKey, Long excludeId);

    /**
     * 刷新配置缓存
     */
    void refreshCache();

    /**
     * 清除配置缓存
     */
    void clearCache();
}