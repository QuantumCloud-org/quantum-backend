package com.alpha.mcp.oauth;

import com.alpha.framework.entity.LoginUser;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Accessors(chain = true)
public class OAuthAuthorizationCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String codeId;

    private Long userId;

    private LoginUser loginUser;

    private String clientId;

    private String redirectUri;

    private String codeChallenge;

    private String codeChallengeMethod;

    private String resource;

    private Set<String> scopes;

    private String nonce;

    private LocalDateTime issuedAt;
}
