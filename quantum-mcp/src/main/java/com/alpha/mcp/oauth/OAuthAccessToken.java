package com.alpha.mcp.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties("loginUser")
public class OAuthAccessToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tokenId;

    private String refreshTokenId;

    private Long userId;

    private String sessionVersion;

    private String clientId;

    private String resource;

    private Set<String> scopes;

    private LocalDateTime issuedAt;
}
