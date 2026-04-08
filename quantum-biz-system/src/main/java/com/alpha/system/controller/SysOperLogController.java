package com.alpha.system.controller;

import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.dto.LogPageQuery;
import com.alpha.logging.entity.SysOperLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.logging.service.ISysOperLogService;
import com.alpha.orm.entity.PageResult;
import com.alpha.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志 Controller
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/monitor/operlog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final ISysOperLogService operLogService;

    @Operation(summary = "分页查询操作日志")
    @SystemLog(title = "操作日志", businessType = BusinessType.SELECT)
    @RequiresPermission("monitor:operlog:list")
    @GetMapping("/list")
    public Result<PageResult<SysOperLog>> list(SysOperLog query, LogPageQuery pageQuery) {
        return Result.ok(PageResult.of(operLogService.selectOperLogPage(query, pageQuery)));
    }

    @Operation(summary = "查询操作日志详情")
    @SystemLog(title = "操作日志", businessType = BusinessType.SELECT)
    @RequiresPermission("monitor:operlog:query")
    @GetMapping("/{operId}")
    public Result<SysOperLog> getInfo(@PathVariable Long operId) {
        return Result.ok(operLogService.selectOperLogById(operId));
    }

    @SystemLog(title = "删除操作日志", businessType = BusinessType.DELETE)
    @Operation(summary = "删除操作日志")
    @RequiresPermission("monitor:operlog:remove")
    @DeleteMapping("/{operIds}")
    public Result<Void> remove(@PathVariable List<Long> operIds) {
        operLogService.deleteOperLogByIds(operIds);
        return Result.ok();
    }

    @Operation(summary = "清空操作日志")
    @SystemLog(title = "清空操作日志", businessType = BusinessType.CLEAN)
    @RequiresPermission("monitor:operlog:remove")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        operLogService.cleanOperLog();
        return Result.ok();
    }

}
