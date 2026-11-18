package com.alpha.system.service;

import com.alpha.system.domain.SysDictType;
import com.alpha.system.dto.request.DictTypeQuery;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 字典类型服务接口
 */
public interface ISysDictTypeService extends IService<SysDictType> {

    /**
     * 分页查询字典类型
     */
    Page<SysDictType> selectDictTypePage(DictTypeQuery query);

    /**
     * 查询所有字典类型
     */
    List<SysDictType> selectAllDictTypes();

    /**
     * 根据ID查询字典类型
     */
    SysDictType selectDictTypeById(Long dictId);

    /**
     * 新增字典类型
     */
    Long insertDictType(SysDictType dictType);

    /**
     * 修改字典类型
     */
    boolean updateDictType(SysDictType dictType);

    /**
     * 删除字典类型
     */
    boolean deleteDictTypeByIds(List<Long> dictIds);

    /**
     * 检查字典类型是否唯一
     */
    boolean checkDictTypeUnique(String dictType, Long excludeId);

    /**
     * 刷新字典缓存
     */
    void refreshCache();
}