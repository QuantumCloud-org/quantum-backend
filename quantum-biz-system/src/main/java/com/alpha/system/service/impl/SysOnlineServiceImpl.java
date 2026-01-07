package com.alpha.system.service.impl;

import com.alpha.cache.util.RedisUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.alpha.security.token.TokenService;
import com.alpha.system.dto.response.OnlineUser;
import com.alpha.system.service.ISysOnlineService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.text.ParseException;
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
                    onlineUsers.add(convertToOnlineUser(loginUser));
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
        return redissonClient.getSet(CommonConstants.ONLINE_TOKENS_KEY).size();
    }

    @Override
    public void forceLogout(String token) throws ParseException, JOSEException {
        tokenService.logout(token);
        log.info("强制下线用户 | Token: {}", maskToken(token));
    }

    @Override
    public void forceLogoutByUserId(Long userId) {
        tokenService.kickOut(userId);
        log.info("强制下线用户 | UserId: {}", userId);
    }

    /**
     * 转换为在线用户信息
     */
    private OnlineUser convertToOnlineUser(LoginUser loginUser) {
        return new OnlineUser()
                .setUserId(loginUser.getUserId())
                .setUsername(loginUser.getUsername())
                .setNickname(loginUser.getNickname())
                .setDeptName(loginUser.getDeptName())
                .setToken(loginUser.getToken())
                .setLoginIp(loginUser.getLoginIp())
                .setLoginLocation(loginUser.getLoginLocation())
                .setBrowser(loginUser.getBrowser())
                .setOs(loginUser.getOs())
                .setLoginTime(loginUser.getLoginTime());
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}