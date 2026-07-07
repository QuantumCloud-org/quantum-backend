package com.alpha.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "ai.mcp")
public class McpProperties {

    private boolean enabled = false;

    private String issuer = "http://localhost:8080";

    private String resource = "http://localhost:8080/mcp";

    private String endpoint = "/mcp";

    private Duration authorizationCodeTtl = Duration.ofMinutes(5);

    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration refreshTokenTtl = Duration.ofDays(30);

    private List<String> defaultScopes = new ArrayList<>(List.of(
            "system.user.read",
            "system.dept.read",
            "system.role.read"
    ));

    private Map<String, Client> clients = defaultClients();

    public Client requireClient(String clientId) {
        Client client = clients.get(clientId);
        if (client == null) {
            throw new IllegalArgumentException("OAuth client is not allowed");
        }
        return client;
    }

    public String normalizedResource(String requestedResource) {
        return requestedResource == null || requestedResource.isBlank() ? resource : requestedResource.trim();
    }

    public String normalizedIssuer() {
        return stripTrailingSlash(issuer);
    }

    public String normalizedEndpoint() {
        return endpoint == null || endpoint.isBlank() ? "/mcp" : endpoint;
    }

    private static Map<String, Client> defaultClients() {
        Client localAgent = new Client();
        localAgent.setName("Quantum Local Agent");
        localAgent.setRedirectUris(new ArrayList<>(List.of(
                "http://127.0.0.1/callback",
                "http://localhost/callback"
        )));
        localAgent.setScopes(new ArrayList<>(List.of(
                "system.user.read",
                "system.dept.read",
                "system.role.read"
        )));

        Map<String, Client> defaults = new LinkedHashMap<>();
        defaults.put("quantum-local-agent", localAgent);
        return defaults;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Data
    public static class Client {

        private String name = "MCP Client";

        private List<String> redirectUris = new ArrayList<>();

        private List<String> scopes = new ArrayList<>();
    }
}
