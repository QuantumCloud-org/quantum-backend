package com.alpha.security.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.cache.util.RedisUtil;
import com.alpha.security.service.ICaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 验证码服务实现
 * <p>
 * 基于 Hutool Captcha + Redis 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements ICaptchaService {

    private final RedisUtil redisUtil;

    /**
     * 验证码长度
     */
    @Value("${security.captcha.length:4}")
    private int captchaLength;

    /**
     * 验证码宽度
     */
    @Value("${security.captcha.width:120}")
    private int captchaWidth;

    /**
     * 验证码高度
     */
    @Value("${security.captcha.height:40}")
    private int captchaHeight;

    /**
     * 验证码有效期（秒）
     */
    @Value("${security.captcha.expire:300}")
    private int captchaExpire;

    /**
     * 干扰线数量
     */
    @Value("${security.captcha.line-count:50}")
    private int lineCount;

    @Override
    public CaptchaResult generate() {
        // 生成验证码
        RandomGenerator generator = new RandomGenerator("0123456789abcdefghjkmnpqrstuvwxyz", captchaLength);
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(captchaWidth, captchaHeight, captchaLength, lineCount);
        captcha.setGenerator(generator);

        // 获取验证码文本
        String code = captcha.getCode();

        // 生成唯一 Key
        String key = IdUtil.fastSimpleUUID();

        // 存入 Redis
        String cacheKey = CacheKeyConstant.AUTH_CAPTCHA + key;
        redisUtil.set(cacheKey, code.toLowerCase(), Duration.ofSeconds(captchaExpire));

        // 返回 Base64 图片
        String imageBase64 = captcha.getImageBase64Data();

        log.debug("生成验证码 | Key: {} | Code: {}", key, code);

        return new CaptchaResult(key, imageBase64, captchaLength);
    }

    @Override
    public boolean verify(String key, String code) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(code)) {
            return false;
        }

        String cacheKey = CacheKeyConstant.AUTH_CAPTCHA + key;
        String cachedCode = redisUtil.get(cacheKey);

        // 验证后删除（一次性使用）
        redisUtil.delete(cacheKey);

        if (StrUtil.isBlank(cachedCode)) {
            log.debug("验证码已过期 | Key: {}", key);
            return false;
        }

        boolean result = cachedCode.equalsIgnoreCase(code);
        log.debug("验证码校验 | Key: {} | Input: {} | Cached: {} | Result: {}", key, code, cachedCode, result);

        return result;
    }
}