package com.alpha.security.service;

public interface ICaptchaService {
    /**
     * 生成验证码
     *
     * @return 验证码结果（包含 key 和 base64 图片）
     */
    CaptchaResult generate();

    /**
     * 校验验证码
     *
     * @param key  验证码 key
     * @param code 用户输入的验证码
     * @return 是否正确
     */
    boolean verify(String key, String code);

    /**
     * 验证码结果
     */
    record CaptchaResult(
            /** 验证码 Key（用于后续验证） */
            String key,
            /** Base64 编码的图片 */
            String image,
            /** 验证码长度 */
            int length
    ) {
    }
}