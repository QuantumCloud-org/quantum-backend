package com.alpha.logging.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "logging.oper-log")
public class OperLogProperties {

    /**
     * 敏感字段列表
     */
    private List<String> sensitiveFields = new ArrayList<>(List.of(
            "password", "pwd", "oldPassword", "newPassword",
            "secret", "token", "accessToken", "refreshToken",
            "creditCard", "cardNo", "idCard", "idNumber"
    ));
}
