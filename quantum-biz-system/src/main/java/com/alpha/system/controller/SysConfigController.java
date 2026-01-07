package com.alpha.system.controller;

import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.orm.entity.PageResult;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.domain.SysConfig;
import com.alpha.system.dto.request.ConfigQuery;
import com.alpha.system.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置 Controller
 */
@Tag(name = "系统配置")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final ISysConfigService configService;

    @Operation(summary = "分页查询配置")
    @SystemLog(title = "系统配置", businessType = BusinessType.SELECT)
    @RequiresPermission("system:config:list")
    @GetMapping("/list")
    public Result<PageResult<SysConfig>> list(ConfigQuery query) {
        return Result.ok(PageResult.of(configService.selectConfigPage(query)));
    }

    @Operation(summary = "查询配置详情")
    @SystemLog(title = "系统配置", businessType = BusinessType.SELECT)
    @RequiresPermission("system:config:query")
    @GetMapping("/{configId}")
    public Result<SysConfig> getInfo(@PathVariable Long configId) {
        return Result.ok(configService.selectConfigById(configId));
    }

    @Operation(summary = "根据键名查询配置值")
    @SystemLog(title = "系统配置", businessType = BusinessType.SELECT)
    @GetMapping("/configKey/{configKey}")
    public Result<String> getConfigKey(@PathVariable String configKey) {
        return Result.ok(configService.selectConfigByKey(configKey));
    }

    @Operation(summary = "新增配置")
    @SystemLog(title = "系统配置", businessType = BusinessType.INSERT)
    @RequiresPermission("system:config:add")
    @PostMapping
    public Result<Long> add(@Validated @RequestBody SysConfig config) {
        return Result.ok(configService.insertConfig(config));
    }

    @Operation(summary = "修改配置")
    @SystemLog(title = "系统配置", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:config:edit")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysConfig config) {
        configService.updateConfig(config);
        return Result.ok();
    }

    @Operation(summary = "删除配置")
    @SystemLog(title = "系统配置", businessType = BusinessType.DELETE)
    @RequiresPermission("system:config:remove")
    @DeleteMapping("/{configIds}")
    public Result<Void> remove(@PathVariable List<Long> configIds) {
        configService.deleteConfigByIds(configIds);
        return Result.ok();
    }

    @Operation(summary = "刷新配置缓存")
    @SystemLog(title = "系统配置", businessType = BusinessType.CLEAN)
    @RequiresPermission("system:config:refresh")
    @DeleteMapping("/refreshCache")
    public Result<Void> refreshCache() {
        configService.refreshCache();
        return Result.ok();
    }
}