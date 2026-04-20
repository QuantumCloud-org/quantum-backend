package com.alpha.system.schedule;

import com.alpha.logging.properties.OperLogProperties;
import com.alpha.logging.service.ISysOperLogService;
import com.alpha.system.service.ISysLoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 日志定时清理任务(Sprint 2 P2-B)。
 * <p>
 * 按 {@code logging.oper-log.retentionDays / loginLogRetentionDays} 配置清理
 * {@code sys_oper_log} / {@code sys_login_log} 过期记录。默认凌晨 3:00 跑,
 * 可通过 {@code logging.oper-log.retentionCron} 覆盖(Spring cron 语法)。
 * 若把 retentionDays / loginLogRetentionDays 改为 0 可分别关闭对应日志的清理。
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class LogRetentionScheduler {

    private final OperLogProperties operLogProperties;
    private final ISysOperLogService operLogService;
    private final ISysLoginLogService loginLogService;

    /**
     * 定时清理过期日志。
     * <p>
     * cron 从 properties 动态读取, 使用 Spring 的 <code>#{}</code> 占位符绑定。
     */
    @Scheduled(cron = "#{@operLogProperties.resolvedRetentionCron}")
    public void cleanExpiredLogs() {
        int operDays = operLogProperties.getRetentionDays();
        int loginDays = operLogProperties.getLoginLogRetentionDays();

        if (operDays > 0) {
            try {
                int removed = operLogService.deleteOperLogByDays(operDays);
                log.info("操作日志清理完成 | 保留天数: {} | 删除条数: {}", operDays, removed);
            } catch (Exception e) {
                log.error("操作日志清理失败", e);
            }
        }

        if (loginDays > 0) {
            try {
                int removed = loginLogService.cleanExpiredLogs(loginDays);
                log.info("登录日志清理完成 | 保留天数: {} | 删除条数: {}", loginDays, removed);
            } catch (Exception e) {
                log.error("登录日志清理失败", e);
            }
        }
    }
}
