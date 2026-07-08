package com.alpha.mcp.security;

import com.alpha.mcp.oauth.OAuthAccessToken;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

public record McpAuthenticationDetails(
        Object webDetails,
        Set<String> scopes,
        String clientId,
        String resource
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public McpAuthenticationDetails {
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    public static McpAuthenticationDetails from(OAuthAccessToken token, Object webDetails) {
        return new McpAuthenticationDetails(webDetails, token.getScopes(), token.getClientId(), token.getResource());
    }

    public boolean hasScope(String scope) {
        return scope != null && !scope.isBlank() && scopes.contains(scope);
    }
}
