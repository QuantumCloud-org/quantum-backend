package com.alpha.system.controller;

import com.alpha.cache.monitor.CacheInfo;
import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.dto.response.OnlineUser;
import com.alpha.system.dto.response.ServerInfo;
import com.alpha.system.service.ISysOnlineService;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * 系统监控控制器
 */
@Tag(name = "系统监控")
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ISysOnlineService onlineService;
    private final ObjectProvider<CacheInfo> cacheEndpointProvider;

    @GetMapping("/online/list")
    @SystemLog(title = "在线用户管理", businessType = BusinessType.SELECT)
    @Operation(summary = "获取在线用户列表")
    @RequiresPermission("monitor:online:list")
    public Result<List<OnlineUser>> onlineList() {
        List<OnlineUser> list = onlineService.getOnlineUsers();
        return Result.ok(list);
    }

    @GetMapping("/online/count")
    @SystemLog(title = "在线用户管理", businessType = BusinessType.SELECT)
    @Operation(summary = "获取在线用户数量")
    public Result<Long> onlineCount() {
        long count = onlineService.getOnlineCount();
        return Result.ok(count);
    }

    @DeleteMapping("/online/{token}")
    @SystemLog(title = "在线用户管理", businessType = BusinessType.DELETE)
    @Operation(summary = "强制下线用户（按Token）")
    @RequiresPermission("monitor:online:forceLogout")
    public Result<Void> forceLogout(@PathVariable String token) throws ParseException, JOSEException {
        onlineService.forceLogout(token);
        return Result.ok();
    }

    @DeleteMapping("/online/user/{userId}")
    @SystemLog(title = "在线用户管理", businessType = BusinessType.DELETE)
    @Operation(summary = "强制下线用户（按用户ID）")
    @RequiresPermission("monitor:online:forceLogout")
    public Result<Void> forceLogoutByUserId(@PathVariable Long userId) {
        onlineService.forceLogoutByUserId(userId);
        return Result.ok();
    }

    @GetMapping("/server")
    @SystemLog(title = "服务器监控", businessType = BusinessType.SELECT)
    @Operation(summary = "获取服务器信息")
    @RequiresPermission("monitor:server:list")
    public Result<ServerInfo> serverInfo() {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.collect();
        return Result.ok(serverInfo);
    }

    @GetMapping("/cache/list")
    @SystemLog(title = "缓存监控", businessType = BusinessType.SELECT)
    @Operation(summary = "获取缓存列表")
    @RequiresPermission("monitor:cache:list")
    public Result<List<CacheInfo>> cacheList() {
        CacheInfo cacheEndpoint = cacheEndpointProvider.getIfAvailable();
        if (cacheEndpoint == null) {
            return Result.ok(List.of());
        }
        Map<String, Object> stats = cacheEndpoint.stats();
        return Result.ok((List<CacheInfo>) stats.get("stats"));
    }

    @GetMapping("/cache/{cacheName}")
    @SystemLog(title = "缓存监控", businessType = BusinessType.SELECT)
    @Operation(summary = "获取指定缓存信息")
    @RequiresPermission("monitor:cache:list")
    public Result<CacheInfo> cacheInfo(@PathVariable String cacheName) {
        CacheInfo cacheEndpoint = cacheEndpointProvider.getIfAvailable();
        if (cacheEndpoint == null) {
            return Result.fail("缓存服务未就绪");
        }
        Map<String, Object> stat = cacheEndpoint.stat(cacheName);
        return Result.ok((CacheInfo) stat);
    }
}