package com.alpha.system.service;

import com.alpha.system.domain.SysDictData;
import com.alpha.system.dto.request.DictDataQuery;
import com.mybatisflex.core.service.IService;

import java.util.List;
import com.mybatisflex.core.paginate.Page;

/**
 * 字典数据服务接口
 */
public interface ISysDictDataService extends IService<SysDictData> {

    /**
     * 分页查询字典数据
     */
    Page<SysDictData> selectDictDataPage(DictDataQuery query);

    /**
     * 根据字典类型查询字典数据
     */
    List<SysDictData> selectDictDataByType(String dictType);

    /**
     * 根据ID查询字典数据
     */
    SysDictData selectDictDataById(Long dictCode);

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
}
