package com.alpha.orm.config;

import com.alpha.framework.context.UserContext;
import com.mybatisflex.core.audit.AuditMessage;
import com.mybatisflex.core.audit.MessageCollector;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SQL 审计管理器
 * <p>
 * 1. 慢 SQL 告警 → app.log + error.log
 * 2. SQL 审计日志 → sql-audit.log (结构化 JSON，通过 traceId 关联请求链路)
 */
@Slf4j
@Component
public class AuditManagerMessage implements MessageCollector {

    private static final long SLOW_SQL_THRESHOLD = 1000L;

    private static final Logger sqlAuditLogger = LogManager.getLogger("com.alpha.sql.audit");

    @Value("${mybatis-flex.audit.log-all-sql:false}")
    private boolean logAllSql;

    @Override
    public void collect(AuditMessage message) {
        long elapsed = message.getElapsedTime();
        String sql = message.getFullSql();

        // 慢 SQL 告警（写入 app.log + error.log）
        if (elapsed >= SLOW_SQL_THRESHOLD) {
            log.warn("慢SQL告警 | {}ms | TraceId: {} | SQL: {}",
                    elapsed,
                    UserContext.getTraceId(),
                    sql);
        }

        // SQL 审计日志（写入 sql-audit.log）
        if (logAllSql || elapsed >= SLOW_SQL_THRESHOLD) {
            sqlAuditLogger.info("{\"traceId\":\"{}\",\"userId\":{},\"username\":\"{}\","
                            + "\"sql\":\"{}\",\"elapsedMs\":{},\"dsName\":\"{}\"}",
                    escape(UserContext.getTraceId()),
                    UserContext.getUserId(),
                    escape(UserContext.getUsername()),
                    escapeSql(sql),
                    elapsed,
                    escape(message.getDsName()));
        }
    }

    private static String escapeSql(String sql) {
        if (sql == null) return "";
        return sql.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", " ")
                  .replace("\r", "");
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
