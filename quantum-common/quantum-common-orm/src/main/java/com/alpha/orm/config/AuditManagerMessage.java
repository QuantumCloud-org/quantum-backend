package com.alpha.orm.config;

import com.alpha.framework.context.UserContext;
import com.mybatisflex.core.audit.AuditMessage;
import com.mybatisflex.core.audit.MessageCollector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SQL 审计管理器
 * <p>
 * 功能：
 * 1. 收集 SQL 执行信息
 * 2. 记录慢 SQL
 * 3. 异步写入审计日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditManagerMessage implements MessageCollector {

    /**
     * 慢 SQL 阈值（毫秒）
     */
    private static final long SLOW_SQL_THRESHOLD = 1000L;

    /**
     * 审计消息队列
     */
    private final BlockingQueue<SqlAuditLog> auditQueue = new LinkedBlockingQueue<>(10000);

    @Override
    public void collect(AuditMessage message) {
        long elapsed = message.getElapsedTime();
        String sql = message.getFullSql();

        // 慢 SQL 告警
        if (elapsed >= SLOW_SQL_THRESHOLD) {
            log.warn("慢SQL告警 | {}ms | TraceId: {} | SQL: {}",
                    elapsed,
                    UserContext.getTraceId(),
                    sql);
        }

        // 构建审计日志
        SqlAuditLog auditLog = new SqlAuditLog();
        auditLog.setTraceId(UserContext.getTraceId());
        auditLog.setUserId(UserContext.getUserId());
        auditLog.setUsername(UserContext.getUsername());
        auditLog.setSql(sql);
        auditLog.setElapsedTime(elapsed);
        auditLog.setDsName(message.getDsName());
        auditLog.setExecuteTime(LocalDateTime.now());

        // 非阻塞入队
        if (!auditQueue.offer(auditLog)) {
            log.warn("审计队列已满，丢弃日志: {}", sql);
        }
    }

    /**
     * SQL 审计日志实体
     */
    @Data
    public static class SqlAuditLog {
        private String traceId;
        private Long userId;
        private String username;
        private String sql;
        private Long elapsedTime;
        private String dsName;
        private LocalDateTime executeTime;
    }
}