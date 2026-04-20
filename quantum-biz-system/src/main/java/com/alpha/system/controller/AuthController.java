package com.alpha.system.controller;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.entity.Result;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.security.service.ICaptchaService;
import com.alpha.security.token.TokenService;
import com.alpha.security.util.CookieUtil;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.LoginRequest;
import com.alpha.system.dto.request.ProfileUpdateRequest;
import com.alpha.system.dto.response.LoginResponse;
import com.alpha.system.dto.response.UserVO;
import com.alpha.system.service.ISysUserService;
import com.alpha.system.service.impl.LoginServiceImpl;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginServiceImpl loginService;
    private final ICaptchaService captchaService;
    private final ISysUserService userService;
    private final UserConvert userConvert;
    private final TokenService tokenService;

    @GetMapping("/captcha")
    @Operation(summary = "创建验证码")
    public Result<ICaptchaService.CaptchaResult> createCaptcha() {
        return Result.ok(captchaService.generate());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            LoginResponse loginResponse = loginService.login(request);
            CookieUtil.writeRefreshCookie(
                    response,
                    loginResponse.getRefreshToken(),
                    Boolean.TRUE.equals(request.getRememberMe()),
                    tokenService.getRefreshTokenExpireSeconds()
            );
            loginResponse.setRefreshToken(null);
            return Result.ok(loginResponse);
        } catch (JOSEException e) {
            log.error("登录Token生成异常", e);
            return Result.fail("登录失败，请稍后重试");
        }
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> info() {
        Long userId = UserContext.getUserId();
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "当前用户不存在或已被删除");
        }
        UserVO userVO = userConvert.toVO(user);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            userVO.setDeptName(loginUser.getDeptName());
            userVO.setRoles(loginUser.getRoles());
            userVO.setPermissions(loginUser.getPermissions());
        }
        return Result.ok(userVO);
    }

    @PutMapping("/info")
    @Operation(summary = "更新当前用户个人资料")
    public Result<Void> updateInfo(@Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = UserContext.getUserId();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setVersion(request.getVersion());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSex(request.getSex());
        user.setRemark(request.getRemark());
        userService.updateUser(user, null);
        return Result.ok();
    }

}
