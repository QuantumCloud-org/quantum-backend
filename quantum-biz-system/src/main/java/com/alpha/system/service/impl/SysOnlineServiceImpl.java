package com.alpha.system.service.impl;

import com.alpha.framework.entity.LoginUser;
import com.alpha.security.token.TokenService;
import com.alpha.system.dto.response.OnlineUser;
import com.alpha.system.security.ForceLogoutEvent;
import com.alpha.system.service.ISysOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 在线用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOnlineServiceImpl implements ISysOnlineService {

    private final TokenService tokenService;
    private final ApplicationEventPublisher events;

    @Override
    public List<OnlineUser> getOnlineUsers() {
        return tokenService.getOnlineSessions().entrySet().stream()
                .map(entry -> convertToOnlineUser(entry.getValue(), TokenService.tokenIdOf(entry.getKey())))
                .toList();
    }

    @Override
    public long getOnlineCount() {
        return tokenService.getOnlineCount();
    }

    @Override
    public void forceLogout(String tokenId) {
        tokenService.logoutByTokenId(tokenId);
        log.info("强制下线用户 | TokenId: {}", maskTokenId(tokenId));
    }

    @Override
    public void forceLogoutByUserId(Long userId) {
        events.publishEvent(new ForceLogoutEvent(Set.of(userId)));
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

}
