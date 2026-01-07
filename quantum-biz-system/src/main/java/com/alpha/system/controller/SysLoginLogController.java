package com.alpha.system.controller;

import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.orm.entity.PageResult;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.domain.SysLoginLog;
import com.alpha.system.dto.request.LoginLogQuery;
import com.alpha.system.service.ISysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录日志 Controller
 */
@Tag(name = "登录日志")
@RestController
@RequestMapping("/monitor/login/log")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final ISysLoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志")
    @SystemLog(title = "登录日志", businessType = BusinessType.SELECT)
    @RequiresPermission("monitor:logininfor:list")
    @GetMapping("/list")
    public Result<PageResult<SysLoginLog>> list(LoginLogQuery query) {
        return Result.ok(PageResult.of(loginLogService.selectLoginLogPage(query)));
    }

    @Operation(summary = "删除登录日志")
    @SystemLog(title = "登录日志", businessType = BusinessType.DELETE)
    @RequiresPermission("monitor:logininfor:remove")
    @DeleteMapping("/{infoIds}")
    public Result<Void> remove(@PathVariable List<Long> infoIds) {
        loginLogService.deleteLoginLogByIds(infoIds);
        return Result.ok();
    }

    @Operation(summary = "清空登录日志")
    @SystemLog(title = "登录日志", businessType = BusinessType.CLEAN)
    @RequiresPermission("monitor:logininfor:remove")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        loginLogService.cleanLoginLog();
        return Result.ok();
    }

}