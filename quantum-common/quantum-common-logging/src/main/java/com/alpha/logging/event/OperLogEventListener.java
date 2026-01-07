package com.alpha.logging.event;

import com.alpha.logging.entity.SysOperLog;
import com.alpha.logging.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 操作日志事件监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogEventListener {

    private final ISysOperLogService operLogService;

    @EventListener
    public void handleOperLogEvent(OperLogEvent event) {
        SysOperLog operLog = event.getOperLog();
        operLogService.insertOperLog(operLog);
    }
}