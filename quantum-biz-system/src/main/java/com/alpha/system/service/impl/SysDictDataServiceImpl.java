package com.alpha.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.system.domain.SysDictData;
import com.alpha.system.mapper.SysDictTypeMapper;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.mybatisflex.core.query.QueryCondition;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.alpha.system.mapper.SysDictDataMapper;
import com.alpha.system.dto.request.DictDataQuery;
import com.alpha.system.service.ISysDictDataService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import com.mybatisflex.core.paginate.Page;

import static com.alpha.system.domain.table.SysDictDataTableDef.SYS_DICT_DATA;
import static com.alpha.system.domain.table.SysDictTypeTableDef.SYS_DICT_TYPE;

/**
 * 字典数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService {

    private final SysDictTypeMapper dictTypeMapper;
    private final CacheManager cacheManager;

    @Override
    public Page<SysDictData> selectDictDataPage(DictDataQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return page(query.getPage(), wrapper);
    }

    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        if (!canReadType(dictType)) {
            return List.of();
        }
        var cache = cacheManager.getCache(CacheKeyConstant.SYS_DICT);
        return cache.get(dictType, () -> list(QueryWrapper.create()
                .where(SYS_DICT_DATA.DICT_TYPE.eq(dictType))
                .and(SYS_DICT_DATA.STATUS.eq(1))
                .orderBy(SYS_DICT_DATA.DICT_SORT.asc())));
    }

    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return getOne(QueryWrapper.create().where(SYS_DICT_DATA.ID.eq(dictCode)).and(visibleDataCondition()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, key = "#dictData.dictType")
    public Long insertDictData(SysDictData dictData) {
        assertWritableType(dictData.getDictType());
        save(dictData);
        return dictData.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictData(SysDictData dictData) {
        var old = getById(dictData.getId());
        if (old == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "字典数据不存在");
        }
        assertWritableType(old.getDictType());
        assertWritableType(dictData.getDictType());
        if (!updateById(dictData)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "字典数据已变更，请刷新后重试");
        }
        evictAfterCommit(old.getDictType(), dictData.getDictType());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public boolean deleteDictDataByIds(List<Long> dictCodes) {
        var types = listByIds(dictCodes).stream().map(SysDictData::getDictType).collect(Collectors.toSet());
        if (!types.isEmpty()) {
            var visibleTypes = dictTypeMapper.selectListByQueryAs(QueryWrapper.create()
                    .select(SYS_DICT_TYPE.DICT_TYPE)
                    .where(SYS_DICT_TYPE.DICT_TYPE.in(types))
                    .and(SysDictTypeServiceImpl.visibleTypeCondition()), String.class);
            if (!new HashSet<>(visibleTypes).containsAll(types)) {
                throw new BizException(ResultCode.ACCESS_DENIED, "无权操作该字典类型");
            }
        }
        return removeByIds(dictCodes);
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(DictDataQuery query) {
        QueryWrapper wrapper = QueryWrapper.create().where(visibleDataCondition());

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

    private QueryCondition visibleDataCondition() {
        return SYS_DICT_DATA.DICT_TYPE.in(QueryWrapper.create().select(SYS_DICT_TYPE.DICT_TYPE)
                .from(SYS_DICT_TYPE).where(SysDictTypeServiceImpl.visibleTypeCondition()));
    }

    private boolean canReadType(String dictType) {
        return dictTypeMapper.selectCountByQuery(QueryWrapper.create().where(SYS_DICT_TYPE.DICT_TYPE.eq(dictType))
                .and(SysDictTypeServiceImpl.visibleTypeCondition())) > 0;
    }

    private void assertWritableType(String dictType) {
        if (!canReadType(dictType)) {
            throw new BizException(ResultCode.ACCESS_DENIED, "无权操作该字典类型");
        }
    }

    private void evictAfterCommit(String oldType, String newType) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                var cache = cacheManager.getCache(CacheKeyConstant.SYS_DICT);
                cache.evict(oldType);
                if (!oldType.equals(newType)) {
                    cache.evict(newType);
                }
            }
        });
    }
}
