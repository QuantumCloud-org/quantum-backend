package com.alpha.system.controller;

import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.orm.entity.PageResult;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.domain.SysDictData;
import com.alpha.system.domain.SysDictType;
import com.alpha.system.dto.request.DictDataQuery;
import com.alpha.system.dto.request.DictTypeQuery;
import com.alpha.system.service.ISysDictDataService;
import com.alpha.system.service.ISysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理 Controller
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final ISysDictTypeService dictTypeService;
    private final ISysDictDataService dictDataService;

    // ==================== 字典类型 ====================

    @Operation(summary = "分页查询字典类型")
    @SystemLog(title = "字典类型", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dict:list")
    @GetMapping("/type/list")
    public Result<PageResult<SysDictType>> listType(DictTypeQuery query) {
        return Result.ok(PageResult.of(dictTypeService.selectDictTypePage(query)));
    }

    @Operation(summary = "查询所有字典类型")
    @SystemLog(title = "字典类型", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dict:alltype")
    @GetMapping("/type/all")
    public Result<List<SysDictType>> allTypes() {
        return Result.ok(dictTypeService.selectAllDictTypes());
    }

    @Operation(summary = "查询字典类型详情")
    @SystemLog(title = "字典类型", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dict:detail")
    @GetMapping("/type/{dictId}")
    public Result<SysDictType> getTypeInfo(@PathVariable Long dictId) {
        return Result.ok(dictTypeService.selectDictTypeById(dictId));
    }

    @Operation(summary = "新增字典类型")
    @SystemLog(title = "字典类型", businessType = BusinessType.INSERT)
    @RequiresPermission("system:dict:add")
    @PostMapping("/type")
    public Result<Long> addType(@Validated @RequestBody SysDictType dictType) {
        return Result.ok(dictTypeService.insertDictType(dictType));
    }

    @Operation(summary = "修改字典类型")
    @SystemLog(title = "字典类型", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:dict:edit")
    @PutMapping("/type")
    public Result<Void> editType(@Validated @RequestBody SysDictType dictType) {
        dictTypeService.updateDictType(dictType);
        return Result.ok();
    }

    @Operation(summary = "删除字典类型")
    @SystemLog(title = "字典类型", businessType = BusinessType.DELETE)
    @RequiresPermission("system:dict:remove")
    @DeleteMapping("/type/{dictIds}")
    public Result<Void> removeType(@PathVariable List<Long> dictIds) {
        dictTypeService.deleteDictTypeByIds(dictIds);
        return Result.ok();
    }

    @Operation(summary = "刷新字典缓存")
    @SystemLog(title = "字典类型", businessType = BusinessType.CLEAN)
    @RequiresPermission("system:dict:refreshCache")
    @DeleteMapping("/type/refreshCache")
    public Result<Void> refreshCache() {
        dictTypeService.refreshCache();
        return Result.ok();
    }

    // ==================== 字典数据 ====================

    @Operation(summary = "分页查询字典数据")
    @SystemLog(title = "字典数据", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dict:listData")
    @GetMapping("/data/list")
    public Result<List<SysDictData>> listData(DictDataQuery query) {
        return Result.ok(dictDataService.selectDictDataPage(query));
    }

    @Operation(summary = "根据字典类型查询字典数据")
    @SystemLog(title = "字典数据", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dict:listData")
    @GetMapping("/data/type/{dictType}")
    public Result<List<SysDictData>> getDataByType(@PathVariable String dictType) {
        return Result.ok(dictDataService.selectDictDataByType(dictType));
    }

    @Operation(summary = "查询字典数据详情")
    @SystemLog(title = "字典数据", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dict:detail")
    @GetMapping("/data/{dictCode}")
    public Result<SysDictData> getDataInfo(@PathVariable Long dictCode) {
        return Result.ok(dictDataService.selectDictDataById(dictCode));
    }

    @Operation(summary = "新增字典数据")
    @SystemLog(title = "字典数据", businessType = BusinessType.INSERT)
    @RequiresPermission("system:dict:add")
    @PostMapping("/data")
    public Result<Long> addData(@Validated @RequestBody SysDictData dictData) {
        return Result.ok(dictDataService.insertDictData(dictData));
    }

    @Operation(summary = "修改字典数据")
    @SystemLog(title = "字典数据", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:dict:edit")
    @PutMapping("/data")
    public Result<Void> editData(@Validated @RequestBody SysDictData dictData) {
        dictDataService.updateDictData(dictData);
        return Result.ok();
    }

    @Operation(summary = "删除字典数据")
    @SystemLog(title = "字典数据", businessType = BusinessType.DELETE)
    @RequiresPermission("system:dict:remove")
    @DeleteMapping("/data/{dictCodes}")
    public Result<Void> removeData(@PathVariable List<Long> dictCodes) {
        dictDataService.deleteDictDataByIds(dictCodes);
        return Result.ok();
    }
}
