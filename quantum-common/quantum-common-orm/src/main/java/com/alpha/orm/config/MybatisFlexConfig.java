package com.alpha.orm.config;

import com.alpha.framework.context.UserContext;
import com.alpha.orm.entity.BaseEntity;
import com.mybatisflex.annotation.InsertListener;
import com.mybatisflex.annotation.UpdateListener;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.spring.boot.ConfigurationCustomizer;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Flex 核心配置
 * <p>
 * 功能：
 * 1. 自动填充（创建人、更新人、创建时间、更新时间）
 * 2. SQL 审计（慢 SQL 告警）
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class MybatisFlexConfig implements MyBatisFlexCustomizer, ConfigurationCustomizer {

    @Value("${mybatis-flex.audit.enabled:true}")
    private boolean auditEnabled;

    @Value("${mybatis-flex.sql-print.enabled:true}")
    private boolean sqlPrintEnabled;

    private final AuditManagerMessage auditMessage;

    /**
     * 自定义配置初始化
     */
    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 1. 注册自动填充监听器
        globalConfig.registerInsertListener(new EntityInsertListener(), BaseEntity.class);
        globalConfig.registerUpdateListener(new EntityUpdateListener(), BaseEntity.class);

        // 2. SQL 审计配置
        if (auditEnabled || sqlPrintEnabled) {
            AuditManager.setAuditEnable(true);
            AuditManager.setMessageCollector(message -> {
                // SQL 打印
                if (sqlPrintEnabled) {
                    log.debug("SQL执行 | {}ms | {}", message.getElapsedTime(), message.getFullSql());
                }
                // 审计收集
                if (auditEnabled) {
                    auditMessage.collect(message);
                }
            });
        }

        log.info("MyBatis-Flex 配置完成 | 审计: {} | SQL打印: {}", auditEnabled, sqlPrintEnabled);
    }

    /**
     * MyBatis 原生配置
     */
    @Override
    public void customize(FlexConfiguration configuration) {
        // 使用 SLF4J 日志
        configuration.setLogImpl(Slf4jImpl.class);
        // 下划线转驼峰
        configuration.setMapUnderscoreToCamelCase(true);
        // 空值也调用 setter
        configuration.setCallSettersOnNulls(true);

    }

    /**
     * 插入监听器 - 自动填充创建人
     */
    public static class EntityInsertListener implements InsertListener {
        @Override
        public void onInsert(Object entity) {
            if (entity instanceof BaseEntity baseEntity) {
                Long currentUserId = UserContext.getUserId();
                baseEntity.setCreateBy(currentUserId);
                baseEntity.setUpdateBy(currentUserId);
                baseEntity.setCreateTime(LocalDateTime.now());
            }
        }
    }

    /**
     * 更新监听器 - 自动填充更新人
     */
    public static class EntityUpdateListener implements UpdateListener {
        @Override
        public void onUpdate(Object entity) {
            if (entity instanceof BaseEntity baseEntity) {
                baseEntity.setUpdateBy(UserContext.getUserId());
                baseEntity.setUpdateTime(LocalDateTime.now());
            }
        }
    }
}