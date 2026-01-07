package com.alpha.logging.config;

import com.alpha.logging.properties.ApiDocProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private final ApiDocProperties apiDocProperties;

    public OpenApiConfig(ApiDocProperties apiDocProperties) {
        this.apiDocProperties = apiDocProperties;
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(components())
                .addSecurityItem(securityRequirement());
    }

    /**
     * API 信息
     */
    private Info apiInfo() {
        return new Info()
                .title(apiDocProperties.getTitle())
                .description(apiDocProperties.getDescription())
                .version(apiDocProperties.getVersion())
                .contact(new Contact()
                        .name(apiDocProperties.getContactName())
                        .email(apiDocProperties.getContactEmail())
                        .url(apiDocProperties.getContactUrl()))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    /**
     * 安全组件（Token 认证）
     */
    private Components components() {
        return new Components()
                .addSecuritySchemes("Authorization", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .name("Authorization")
                        .description("请输入 Token（不需要 Bearer 前缀）")
                        .in(SecurityScheme.In.HEADER));
    }

    /**
     * 安全要求
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("Authorization");
    }
}