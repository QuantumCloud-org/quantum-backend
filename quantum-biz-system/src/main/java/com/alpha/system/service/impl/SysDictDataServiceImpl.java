package com.alpha.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.system.domain.SysDictData;
import com.alpha.system.mapper.SysDictDataMapper;
import com.alpha.system.dto.request.DictDataQuery;
import com.alpha.system.service.ISysDictDataService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.alpha.system.domain.table.SysDictDataTableDef.SYS_DICT_DATA;

/**
 * 字典数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService {

    @Override
    public List<SysDictData> selectDictDataPage(DictDataQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return list(wrapper);
    }

    @Override
    @Cacheable(value = CacheKeyConstant.SYS_DICT, key = "#dictType", unless = "#result == null || #result.isEmpty()")
    public List<SysDictData> selectDictDataByType(String dictType) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_DICT_DATA.DICT_TYPE.eq(dictType))
                .and(SYS_DICT_DATA.STATUS.eq(1))
                .and(SYS_DICT_DATA.DELETED.eq(0))
                .orderBy(SYS_DICT_DATA.DICT_SORT.asc());
        return list(wrapper);
    }

    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return getById(dictCode);
    }

    @Override
    public SysDictData selectByTypeAndValue(String dictType, String dictValue) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_DICT_DATA.DICT_TYPE.eq(dictType))
                .and(SYS_DICT_DATA.DICT_VALUE.eq(dictValue))
                .and(SYS_DICT_DATA.DELETED.eq(0));
        return getOne(wrapper);
    }

    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        SysDictData dictData = selectByTypeAndValue(dictType, dictValue);
        return dictData != null ? dictData.getDictLabel() : "";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, key = "#dictData.dictType")
    public Long insertDictData(SysDictData dictData) {
        save(dictData);
        return dictData.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, key = "#dictData.dictType")
    public boolean updateDictData(SysDictData dictData) {
        return updateById(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public boolean deleteDictDataByIds(List<Long> dictCodes) {
        return removeByIds(dictCodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public int deleteByDictType(String dictType) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_DICT_DATA.DICT_TYPE.eq(dictType));
        return remove(wrapper) ? 1 : 0;
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(DictDataQuery query) {
        QueryWrapper wrapper = QueryWrapper.create();

        if (StrUtil.isNotBlank(query.getDictType())) {
            wrapper.and(SYS_DICT_DATA.DICT_TYPE.eq(query.getDictType()));
        }
        if (StrUtil.isNotBlank(query.getDictLabel())) {
            wrapper.and(SYS_DICT_DATA.DICT_LABEL.like(query.getDictLabel()));
        }
        if (query.getStatus() != null) {
            wrapper.and(SYS_DICT_DATA.STATUS.eq(query.getStatus()));
        }

        wrapper.orderBy(SYS_DICT_DATA.DICT_SORT.asc());
        return wrapper;
    }
}
