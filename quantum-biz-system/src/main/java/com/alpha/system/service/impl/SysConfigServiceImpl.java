package com.alpha.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysConfig;
import com.alpha.system.dto.request.ConfigQuery;
import com.alpha.system.mapper.SysConfigMapper;
import com.alpha.system.service.ISysConfigService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.alpha.system.domain.table.SysConfigTableDef.SYS_CONFIG;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    @Override
    public Page<SysConfig> selectConfigPage(ConfigQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public List<SysConfig> selectConfigList(ConfigQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return list(wrapper);
    }

    @Override
    public SysConfig selectConfigById(Long configId) {
        return getById(configId);
    }

    @Override
    @Cacheable(value = CacheKeyConstant.SYS_CONFIG, key = "#configKey", unless = "#result == null")
    public String selectConfigByKey(String configKey) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_CONFIG.CONFIG_KEY.eq(configKey))
                .and(SYS_CONFIG.DELETED.eq(0));
        SysConfig config = getOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String selectConfigByKey(String configKey, String defaultValue) {
        String value = selectConfigByKey(configKey);
        return StrUtil.isNotBlank(value) ? value : defaultValue;
    }

    @Override
    public boolean selectConfigByKeyAsBoolean(String configKey, boolean defaultValue) {
        String value = selectConfigByKey(configKey);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_CONFIG, key = "#config.configKey")
    public Long insertConfig(SysConfig config) {
        if (!checkConfigKeyUnique(config.getConfigKey(), null)) {
            throw new BizException("参数键名已存在");
        }

        save(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_CONFIG, key = "#config.configKey")
    public boolean updateConfig(SysConfig config) {
        if (!checkConfigKeyUnique(config.getConfigKey(), config.getId())) {
            throw new BizException("参数键名已存在");
        }

        return updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_CONFIG, allEntries = true)
    public boolean deleteConfigByIds(List<Long> configIds) {
        for (Long configId : configIds) {
            SysConfig config = getById(configId);
            if (config != null && "Y".equals(config.getConfigType())) {
                throw new BizException("系统内置参数【" + config.getConfigName() + "】不能删除");
            }
        }

        return removeByIds(configIds);
    }

    @Override
    public boolean checkConfigKeyUnique(String configKey, Long excludeId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_CONFIG.CONFIG_KEY.eq(configKey))
                .and(SYS_CONFIG.DELETED.eq(0))
                .and(SYS_CONFIG.ID.ne(excludeId != null ? excludeId : 0L));
        return count(wrapper) == 0;
    }

    @Override
    @CacheEvict(value = CacheKeyConstant.SYS_CONFIG, allEntries = true)
    public void refreshCache() {
        log.info("刷新配置缓存");
    }

    @Override
    @CacheEvict(value = CacheKeyConstant.SYS_CONFIG, allEntries = true)
    public void clearCache() {
        log.info("清除配置缓存");
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(ConfigQuery query) {
        QueryWrapper wrapper = QueryWrapper.create();

        if (StrUtil.isNotBlank(query.getConfigName())) {
            wrapper.and(SYS_CONFIG.CONFIG_NAME.like(query.getConfigName()));
        }
        if (StrUtil.isNotBlank(query.getConfigKey())) {
            wrapper.and(SYS_CONFIG.CONFIG_KEY.like(query.getConfigKey()));
        }
        if (StrUtil.isNotBlank(query.getConfigType())) {
            wrapper.and(SYS_CONFIG.CONFIG_TYPE.eq(query.getConfigType()));
        }

        wrapper.orderBy(SYS_CONFIG.CREATE_TIME.desc());
        return wrapper;
    }
}
