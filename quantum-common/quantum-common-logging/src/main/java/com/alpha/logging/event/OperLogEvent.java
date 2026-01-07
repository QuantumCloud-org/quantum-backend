package com.alpha.logging.event;

import com.alpha.logging.entity.SysOperLog;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 操作日志事件
 */
@Getter
public class OperLogEvent extends ApplicationEvent {

    private final SysOperLog operLog;

    public OperLogEvent(SysOperLog operLog) {
        super(operLog);
        this.operLog = operLog;
    }
}