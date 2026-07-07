package com.alpha.mcp.controller;

import com.alpha.mcp.config.McpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mcp", name = "enabled", havingValue = "true")
public class OAuthMetadataController {

    private final McpProperties properties;

    @GetMapping("/.well-known/oauth-protected-resource")
    public Map<String, Object> protectedResourceMetadata() {
        return Map.of(
                "resource", properties.getResource(),
                "authorization_servers", List.of(properties.normalizedIssuer()),
                "bearer_methods_supported", List.of("header"),
                "scopes_supported", properties.getDefaultScopes()
        );
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String, Object> authorizationServerMetadata() {
        String issuer = properties.normalizedIssuer();
        return Map.of(
                "issuer", issuer,
                "authorization_endpoint", issuer + "/oauth/authorize",
                "token_endpoint", issuer + "/oauth/token",
                "revocation_endpoint", issuer + "/oauth/revoke",
                "response_types_supported", List.of("code"),
                "grant_types_supported", List.of("authorization_code", "refresh_token"),
                "code_challenge_methods_supported", List.of("S256"),
                "scopes_supported", properties.getDefaultScopes()
        );
    }
}
