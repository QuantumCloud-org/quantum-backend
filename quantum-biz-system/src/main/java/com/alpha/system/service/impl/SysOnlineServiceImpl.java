package com.alpha.system.service.impl;

import com.alpha.cache.util.CacheClient;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.alpha.security.token.TokenService;
import com.alpha.system.dto.response.OnlineUser;
import com.alpha.system.service.ISysOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 在线用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOnlineServiceImpl implements ISysOnlineService {

    private final CacheClient cacheClient;
    private final TokenService tokenService;

    @Override
    public List<OnlineUser> getOnlineUsers() {
        List<OnlineUser> onlineUsers = new ArrayList<>();

        for (String tokenKey : cacheClient.<String>sMembers(CommonConstants.ONLINE_TOKENS_KEY)) {
            try {
                LoginUser loginUser = cacheClient.get(tokenKey);
                if (loginUser != null) {
                    onlineUsers.add(convertToOnlineUser(loginUser, extractTokenId(tokenKey)));
                } else {
                    cacheClient.sRemove(CommonConstants.ONLINE_TOKENS_KEY, tokenKey);
                }
            } catch (Exception e) {
                log.warn("解析在线用户信息失败: {}", tokenKey, e);
            }
        }
        return onlineUsers;
    }

    @Override
    public long getOnlineCount() {
        return cacheClient.sMembers(CommonConstants.ONLINE_TOKENS_KEY).size();
    }

    @Override
    public void forceLogout(String tokenId) {
        tokenService.logoutByTokenId(tokenId);
        log.info("强制下线用户 | TokenId: {}", maskTokenId(tokenId));
    }

    @Override
    public void forceLogoutByUserId(Long userId) {
        tokenService.kickOut(userId);
        log.info("强制下线用户 | UserId: {}", userId);
    }

    private OnlineUser convertToOnlineUser(LoginUser loginUser, String tokenId) {
        return new OnlineUser()
                .setUserId(loginUser.getUserId())
                .setUsername(loginUser.getUsername())
                .setNickname(loginUser.getNickname())
                .setDeptName(loginUser.getDeptName())
                .setTokenId(tokenId)
                .setLoginIp(loginUser.getLoginIp())
                .setLoginLocation(loginUser.getLoginLocation())
                .setBrowser(loginUser.getBrowser())
                .setOs(loginUser.getOs())
                .setLoginTime(loginUser.getLoginTime());
    }

    private String maskTokenId(String tokenId) {
        if (tokenId == null || tokenId.length() < 8) {
            return "***";
        }
        return tokenId.substring(0, 4) + "****" + tokenId.substring(tokenId.length() - 4);
    }

    private String extractTokenId(String tokenKey) {
        String prefix = CommonConstants.REDIS_TOKEN_PREFIX + "access:";
        return tokenKey != null && tokenKey.startsWith(prefix)
                ? tokenKey.substring(prefix.length())
                : tokenKey;
    }
}
