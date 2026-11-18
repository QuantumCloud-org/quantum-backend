package com.alpha.system.convert;

import com.alpha.system.domain.SysDictData;
import com.alpha.system.domain.SysDictType;
import com.alpha.system.dto.request.DictDataCreateRequest;
import com.alpha.system.dto.request.DictDataUpdateRequest;
import com.alpha.system.dto.request.DictTypeCreateRequest;
import com.alpha.system.dto.request.DictTypeUpdateRequest;

public final class DictConvert {

    private DictConvert() {
    }

    public static SysDictType toEntity(DictTypeCreateRequest request) {
        var entity = new SysDictType();
        entity.setDictName(request.getDictName());
        entity.setDictType(request.getDictType());
        entity.setBusiness(request.getBusiness());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        return entity;
    }

    public static SysDictType toEntity(DictTypeUpdateRequest request) {
        var entity = toEntity((DictTypeCreateRequest) request);
        entity.setId(request.getId());
        entity.setVersion(request.getVersion());
        return entity;
    }

    public static SysDictData toEntity(DictDataCreateRequest request) {
        var entity = new SysDictData();
        entity.setDictType(request.getDictType());
        entity.setDictLabel(request.getDictLabel());
        entity.setDictValue(request.getDictValue());
        entity.setDictSort(request.getDictSort());
        entity.setCssClass(request.getCssClass());
        entity.setListClass(request.getListClass());
        entity.setIsDefault(request.getIsDefault());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        return entity;
    }

    public static SysDictData toEntity(DictDataUpdateRequest request) {
        var entity = toEntity((DictDataCreateRequest) request);
        entity.setId(request.getId());
        entity.setVersion(request.getVersion());
        return entity;
    }
}
