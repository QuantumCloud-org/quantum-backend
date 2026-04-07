package com.alpha.system.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 在线用户信息
 */
@Data
@Accessors(chain = true)
public class OnlineUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
     * 部门名称
     */
    private String deptName;

    /**
     * Token 标识
     */
    private String tokenId;

    /**
     * 登录 IP
     */
    private String loginIp;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;
}
