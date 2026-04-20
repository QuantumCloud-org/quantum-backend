package com.alpha.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.cache.util.RedisUtil;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.framework.util.IpUtil;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.service.ICaptchaService;
import com.alpha.security.token.TokenInfo;
import com.alpha.security.token.TokenService;
import com.alpha.system.domain.SysLoginLog;
import com.alpha.system.dto.request.LoginRequest;
import com.alpha.system.dto.response.LoginResponse;
import com.alpha.system.service.ISysLoginLogService;
import com.alpha.system.service.ISysUserService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 登录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RedisUtil redisUtil;
    private final SecurityProperties securityProperties;
    private final ISysLoginLogService loginLogService;
    private final ICaptchaService captchaService;
    private final ISysUserService sysUserService;

    /**
     * 登录
     */
    public LoginResponse login(LoginRequest request) throws JOSEException {
        String username = request.getUsername();
        String password = request.getPassword();

        String clientIpEarly = UserContext.getIp();
        UserAgent uaEarly = parseUserAgent(UserContext.getUserAgent());

        // 1. 校验验证码
        if (securityProperties.isCaptchaRequired()) {
            if (StrUtil.isBlank(request.getCaptchaKey()) || StrUtil.isBlank(request.getCaptchaCode())) {
                recordLoginFail(username);
                recordLoginLog(username, clientIpEarly, uaEarly, false, "验证码错误");
                throw new BizException(ResultCode.UNAUTHORIZED, "验证码错误");
            }
            if (!captchaService.verify(request.getCaptchaKey(), request.getCaptchaCode())) {
                recordLoginFail(username);
                recordLoginLog(username, clientIpEarly, uaEarly, false, "验证码错误");
                throw new BizException(ResultCode.UNAUTHORIZED, "验证码错误");
            }
        }

        // 2. 检查账号是否被锁定
        try {
            checkAccountLock(username);
        } catch (BizException e) {
            recordLoginLog(username, clientIpEarly, uaEarly, false, e.getMessage());
            throw e;
        }

        // 3. 执行认证
        LoginUser loginUser;
        String ip = UserContext.getIp();
        String userAgent = UserContext.getUserAgent();
        UserAgent ua = parseUserAgent(userAgent);
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, username + password));
            loginUser = (LoginUser) authentication.getPrincipal();
        } catch (BadCredentialsException e) {
            recordLoginFail(username);
            // 记录登录失败日志
            recordLoginLog(username, ip, ua, false, "用户名或密码错误");
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        } catch (DisabledException e) {
            recordLoginLog(username, ip, ua, false, "账号已禁用");
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            recordLoginLog(username, ip, ua, false, "账号已锁定");
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        } catch (Exception e) {
            log.error("登录异常: {}", e.getMessage(), e);
            recordLoginLog(username, ip, ua, false, "登录异常");
            throw new BizException(ResultCode.SYSTEM_ERROR, "登录失败，请稍后重试");
        }

        // 4. 清除登录失败记录
        clearLoginFail(username);

        // 5. 设置登录信息
        setLoginInfo(loginUser);

        // 6. 记录登录成功日志
        recordLoginLog(username, ip, ua, true, "登录成功");

        // 7. 生成 Token
        String deviceId = request.getDeviceId();
        String clientIp = UserContext.getIp();
        TokenInfo tokenInfo = tokenService.createToken(
                loginUser,
                deviceId,
                clientIp,
                userAgent,
                Boolean.TRUE.equals(request.getRememberMe())
        );

        return LoginResponse.of(loginUser, tokenInfo);
    }


    /**
     * 检查账号是否锁定
     */
    private void checkAccountLock(String username) {
        String lockKey = CacheKeyConstant.AUTH_LOGIN_FAIL + username;
        Integer failCount = redisUtil.get(lockKey);

        if (failCount != null && failCount >= securityProperties.getMaxLoginFailCount()) {
            long ttl = redisUtil.getExpire(lockKey);
            throw new BizException(ResultCode.ACCOUNT_LOCKED, String.format("账号已锁定，请 %d 分钟后重试", ttl / 60 + 1));
        }
    }

    /**
     * 记录登录失败
     */
    private void recordLoginFail(String username) {
        String lockKey = CacheKeyConstant.AUTH_LOGIN_FAIL + username;
        long count = redisUtil.increment(lockKey);

        if (count == 1) {
            redisUtil.expire(lockKey, Duration.ofMinutes(securityProperties.getLockTime()));
        }

        int remaining = securityProperties.getMaxLoginFailCount() - (int) count;
        if (remaining > 0) {
            log.warn("登录失败 | Username: {} | 剩余尝试次数: {}", username, remaining);
        } else {
            log.warn("账号锁定 | Username: {} | 锁定时间: {}分钟", username, securityProperties.getLockTime());
        }
    }

    /**
     * 清除登录失败记录
     */
    private void clearLoginFail(String username) {
        redisUtil.delete(CacheKeyConstant.AUTH_LOGIN_FAIL + username);
    }

    /**
     * 设置登录信息
     */
    private void setLoginInfo(LoginUser loginUser) {
        String ip = UserContext.getIp();

        loginUser.setLoginIp(ip);
        loginUser.setLoginLocation(IpUtil.getIpAddress(ip));

        String userAgent = UserContext.getUserAgent();
        if (StrUtil.isNotBlank(userAgent)) {
            UserAgent ua = UserAgentUtil.parse(userAgent);
            if (ua != null) {
                loginUser.setBrowser(ua.getBrowser().getName());
                loginUser.setOs(ua.getPlatform().getName());
            }
        }
        try {
            sysUserService.updateLoginInfo(loginUser.getUserId(), loginUser);
        } catch (Exception e) {
            log.warn("更新用户最近登录信息失败 | UserId: {}", loginUser.getUserId(), e);
        }
    }

    /**
     * 解析 UserAgent
     */
    private UserAgent parseUserAgent(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return null;
        }
        return UserAgentUtil.parse(userAgent);
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(String username, String ip, UserAgent ua, boolean success, String message) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username);
        loginLog.setIpaddr(ip);
        loginLog.setLoginLocation(IpUtil.getIpAddress(ip));
        loginLog.setStatus(success ? 0 : 1);
        loginLog.setMsg(message);
        loginLog.setLoginTime(LocalDateTime.now());
        if (ua != null) {
            loginLog.setBrowser(ua.getBrowser().getName());
            loginLog.setOs(ua.getPlatform().getName());
        }
        loginLogService.insertLoginLog(loginLog);
    }

}
