package com.alpha.system.service;

import com.alpha.system.dto.response.OnlineUser;
import com.nimbusds.jose.JOSEException;

import java.text.ParseException;
import java.util.List;

/**
 * 在线用户服务接口
 */
public interface ISysOnlineService {

    /**
     * 获取在线用户列表
     */
    List<OnlineUser> getOnlineUsers();

    /**
     * 获取在线用户数量
     */
    long getOnlineCount();

    /**
     * 强制下线用户
     */
    void forceLogout(String token) throws ParseException, JOSEException;

    /**
     * 强制下线用户（按用户ID）
     */
    void forceLogoutByUserId(Long userId);
}