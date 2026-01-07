package com.alpha.logging.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API 文档配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "springdoc")
public class ApiDocProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 标题
     */
    private String title =  "开发框架接口文档";

    /**
     * 描述
     */
    private String description = "这是基于 Spring Boot 4 + Java 25 的企业级开发框架接口文档";

    /**
     * 版本
     */
    private String version = "1.0.0";

    /**
     * 联系人
     */
    private String contactName = "Alpha Team";
    private String contactEmail = "alpha@example.com";
    private String contactUrl = "https://www.alpha.com";

    /**
     * 分组配置
     */
    private Group group = new Group();

    @Data
    public static class Group {
        /**
         * 系统管理
         */
        private String system = "com.alpha.module.system";
        /**
         * 业务模块
         */
        private String business = "com.alpha.module.biz";
    }
}