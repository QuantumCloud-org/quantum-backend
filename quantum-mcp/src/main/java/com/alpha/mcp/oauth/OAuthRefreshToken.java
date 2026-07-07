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
public class OAuthRefreshToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String refreshTokenId;

    private String accessTokenId;

    private LoginUser loginUser;

    private Long userId;

    private String clientId;

    private String resource;

    private Set<String> scopes;

    private LocalDateTime issuedAt;
}
