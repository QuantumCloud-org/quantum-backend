package com.alpha.system.service;

import com.alpha.system.domain.SysDictData;
import com.alpha.system.dto.request.DictDataQuery;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 字典数据服务接口
 */
public interface ISysDictDataService extends IService<SysDictData> {

    /**
     * 分页查询字典数据
     */
    List<SysDictData> selectDictDataPage(DictDataQuery query);

    /**
     * 根据字典类型查询字典数据
     */
    List<SysDictData> selectDictDataByType(String dictType);

    /**
     * 根据ID查询字典数据
     */
    SysDictData selectDictDataById(Long dictCode);

    /**
     * 根据字典类型和字典值查询
     */
    SysDictData selectByTypeAndValue(String dictType, String dictValue);

    /**
     * 根据字典类型和字典值获取标签
     */
    String selectDictLabel(String dictType, String dictValue);

    /**
     * 新增字典数据
     */
    Long insertDictData(SysDictData dictData);

    /**
     * 修改字典数据
     */
    boolean updateDictData(SysDictData dictData);

    /**
     * 删除字典数据
     */
    boolean deleteDictDataByIds(List<Long> dictCodes);

    /**
     * 根据字典类型删除所有字典数据
     */
    int deleteByDictType(String dictType);
}
