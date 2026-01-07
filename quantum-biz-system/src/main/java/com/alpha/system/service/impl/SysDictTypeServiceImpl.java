package com.alpha.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysDictType;
import com.alpha.system.dto.request.DictTypeQuery;
import com.alpha.system.mapper.SysDictDataMapper;
import com.alpha.system.mapper.SysDictTypeMapper;
import com.alpha.system.service.ISysDictTypeService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.alpha.system.domain.table.SysDictDataTableDef.SYS_DICT_DATA;
import static com.alpha.system.domain.table.SysDictTypeTableDef.SYS_DICT_TYPE;

/**
 * 字典类型服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements ISysDictTypeService {

    private final SysDictDataMapper dictDataMapper;

    @Override
    public Page<SysDictType> selectDictTypePage(DictTypeQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public List<SysDictType> selectDictTypeList(DictTypeQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return list(wrapper);
    }

    @Override
    public List<SysDictType> selectAllDictTypes() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_DICT_TYPE.DELETED.eq(0))
                .orderBy(SYS_DICT_TYPE.CREATE_TIME.desc());
        return list(wrapper);
    }

    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return getById(dictId);
    }

    @Override
    public SysDictType selectByDictType(String dictType) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_DICT_TYPE.DICT_TYPE.eq(dictType))
                .and(SYS_DICT_TYPE.DELETED.eq(0));
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public Long insertDictType(SysDictType dictType) {
        if (!checkDictTypeUnique(dictType.getDictType(), null)) {
            throw new BizException("字典类型已存在");
        }

        save(dictType);
        return dictType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public boolean updateDictType(SysDictType dictType) {
        if (!checkDictTypeUnique(dictType.getDictType(), dictType.getId())) {
            throw new BizException("字典类型已存在");
        }

        SysDictType oldDict = getById(dictType.getId());
        // 如果字典类型变更，同步更新字典数据
        if (!oldDict.getDictType().equals(dictType.getDictType())) {
            // 这里可以考虑更新字典数据的 dict_type 字段
            // 为简化处理，暂不支持修改字典类型
            throw new BizException("不支持修改字典类型");
        }

        return updateById(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public boolean deleteDictTypeByIds(List<Long> dictIds) {
        for (Long dictId : dictIds) {
            SysDictType dictType = getById(dictId);
            // 检查是否有字典数据
            QueryWrapper wrapper = QueryWrapper.create()
                    .where(SYS_DICT_DATA.DICT_TYPE.eq(dictType.getDictType()))
                    .and(SYS_DICT_DATA.DELETED.eq(0));
            long count = mapper.selectCountByQuery(wrapper);
            if (count > 0) {
                throw new BizException("字典类型【" + dictType.getDictName() + "】下存在字典数据，不能删除");
            }
        }

        return removeByIds(dictIds);
    }

    @Override
    public boolean checkDictTypeUnique(String dictType, Long excludeId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_DICT_TYPE.DICT_TYPE.eq(dictType))
                .and(SYS_DICT_TYPE.DELETED.eq(0))
                .and(SYS_DICT_TYPE.ID.ne(excludeId != null ? excludeId : 0L));
        return count(wrapper) == 0;
    }

    @Override
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public void refreshCache() {
        log.info("刷新字典缓存");
    }

    @Override
    @CacheEvict(value = CacheKeyConstant.SYS_DICT, allEntries = true)
    public void clearCache() {
        log.info("清除字典缓存");
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(DictTypeQuery query) {
        QueryWrapper wrapper = QueryWrapper.create();

        if (StrUtil.isNotBlank(query.getDictName())) {
            wrapper.and(SYS_DICT_TYPE.DICT_NAME.like(query.getDictName()));
        }
        if (StrUtil.isNotBlank(query.getDictType())) {
            wrapper.and(SYS_DICT_TYPE.DICT_TYPE.like(query.getDictType()));
        }
        if (query.getStatus() != null) {
            wrapper.and(SYS_DICT_TYPE.STATUS.eq(query.getStatus()));
        }

        wrapper.orderBy(SYS_DICT_TYPE.CREATE_TIME.desc());
        return wrapper;
    }
}