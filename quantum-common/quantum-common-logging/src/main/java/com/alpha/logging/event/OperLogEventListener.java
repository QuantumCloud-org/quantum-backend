package com.alpha.logging.event;

import com.alpha.logging.entity.SysOperLog;
import com.alpha.logging.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志事件监听器
 * <p>
 * 异步落库（虚拟线程执行器），避免日志写库耗时叠加到请求响应时间上。
 * 事件对象在发布前已携带全部上下文，不依赖请求线程的 ThreadLocal。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogEventListener {

    private final ISysOperLogService operLogService;

    @Async("asyncExecutor")
    @EventListener
    public void handleOperLogEvent(OperLogEvent event) {
        SysOperLog operLog = event.getOperLog();
        try {
            operLogService.insertOperLog(operLog);
        } catch (Exception e) {
            log.error("操作日志异步落库失败 | title: {}", operLog.getTitle(), e);
        }
    }
}