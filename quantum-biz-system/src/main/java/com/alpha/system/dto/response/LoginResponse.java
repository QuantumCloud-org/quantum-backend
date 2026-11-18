package com.alpha.system.dto.response;

import com.alpha.framework.entity.LoginUser;
import com.alpha.security.token.TokenInfo;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 登录响应
 */
@Data
@Accessors(chain = true)
public class LoginResponse {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * Access Token
     */
    private String accessToken;

    /** Access token lifetime in seconds. */
    private int expiresIn;

    /** Identifies the same Web session across refreshes. */
    private String sessionId;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 从 LoginUser 和 TokenInfo 构建
     */
    public static LoginResponse of(LoginUser loginUser, TokenInfo tokenInfo) {
        return new LoginResponse()
                .setUserId(loginUser.getUserId())
                .setUsername(loginUser.getUsername())
                .setNickname(loginUser.getNickname())
                .setAccessToken(tokenInfo.getAccessToken())
                .setExpiresIn(tokenInfo.getExpiresIn())
                .setSessionId(tokenInfo.getSessionId())
                .setRefreshToken(tokenInfo.getRefreshToken())
                .setExpireTime(tokenInfo.getExpireTime());
    }
}
