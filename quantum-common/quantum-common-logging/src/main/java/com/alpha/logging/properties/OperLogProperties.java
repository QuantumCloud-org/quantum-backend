package com.alpha.logging.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

/**
 * 操作日志配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "logging.oper-log")
public class OperLogProperties {

    /**
     * 敏感字段列表。
     * <p>
     * 作用:
     * 1. 请求参数日志: 按参数名及嵌套字段名匹配，将敏感值改写为 {@code [PROTECTED]}
     * 2. 响应结果日志: 通过 Jackson Filter 直接移除 key
     * <p>
     * {@code @SystemLog.excludeParams} 可为当前方法指定额外的顶层参数名。
     */
    private List<String> sensitiveFields = new ArrayList<>(List.of(
            "password", "pwd", "oldPassword", "newPassword",
            "secret", "token", "accessToken", "refreshToken",
            "creditCard", "cardNo", "idCard", "idNumber"
    ));

    /**
     * 操作日志保留天数(超过则由 {@code LogRetentionScheduler} 定时清理)。
     * 0 = 不清理(永久保留)。默认 90 天。
     */
    private int retentionDays = 90;

    /**
     * 登录日志保留天数。0 = 不清理, 默认 180 天(登录审计需要更长)。
     */
    private int loginLogRetentionDays = 180;

    /**
     * 清理任务 cron 表达式(秒 分 时 日 月 周)。默认凌晨 3:00。
     * 空值或 "off" 关闭定时任务。
     */
    private String retentionCron = "0 0 3 * * ?";

    public String getResolvedRetentionCron() {
        if (retentionCron == null) {
            return Scheduled.CRON_DISABLED;
        }
        String normalized = retentionCron.trim();
        if (normalized.isEmpty() || "off".equalsIgnoreCase(normalized)) {
            return Scheduled.CRON_DISABLED;
        }
        return normalized;
    }

    public static Set<String> normalizeFields(Collection<String> fields) {
        return fields.stream().filter(field -> field != null && !field.isBlank())
                .map(field -> field.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
