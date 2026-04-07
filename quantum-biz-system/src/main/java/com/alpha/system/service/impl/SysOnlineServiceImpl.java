package com.alpha.system.service.impl;

import com.alpha.cache.util.RedisUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.alpha.security.token.TokenService;
import com.alpha.system.dto.response.OnlineUser;
import com.alpha.system.service.ISysOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
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

    private final RedisUtil redisUtil;
    private final RedissonClient redissonClient;
    private final TokenService tokenService;

    @Override
    public List<OnlineUser> getOnlineUsers() {
        List<OnlineUser> onlineUsers = new ArrayList<>();

        for (String tokenKey : redissonClient.<String>getSet(CommonConstants.ONLINE_TOKENS_KEY)) {
            try {
                LoginUser loginUser = redisUtil.get(tokenKey);
                if (loginUser != null) {
                    onlineUsers.add(convertToOnlineUser(loginUser, extractTokenId(tokenKey)));
                } else {
                    redissonClient.getSet(CommonConstants.ONLINE_TOKENS_KEY).remove(tokenKey);
                }
            } catch (Exception e) {
                log.warn("解析在线用户信息失败: {}", tokenKey, e);
            }
        }
        return onlineUsers;
    }


    @Override
    public long getOnlineCount() {
        return redissonClient.<String>getSet(CommonConstants.ONLINE_TOKENS_KEY).size();
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

    /**
     * 转换为在线用户信息
     */
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
