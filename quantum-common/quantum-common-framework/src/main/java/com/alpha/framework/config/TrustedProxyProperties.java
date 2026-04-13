package com.alpha.framework.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 可信代理配置
 */
@Getter
@Component
@ConfigurationProperties(prefix = "security.trusted-proxies")
public class TrustedProxyProperties {

    private static volatile List<String> trustedProxies = List.of();

    /**
     * 可信代理 CIDR 列表
     */
    private List<String> cidrs = List.of();

    public void setCidrs(List<String> cidrs) {
        List<String> normalized = cidrs == null ? List.of() : List.copyOf(cidrs);
        this.cidrs = normalized;
        trustedProxies = normalized;
    }

    public static List<String> getTrustedProxies() {
        return trustedProxies;
    }
}
