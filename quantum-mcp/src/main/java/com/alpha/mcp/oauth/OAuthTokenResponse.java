package com.alpha.mcp.oauth;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OAuthTokenResponse {

    private String accessToken;

    private String tokenType = "Bearer";

    private long expiresIn;

    private String refreshToken;

    private String scope;

    private String resource;
}
