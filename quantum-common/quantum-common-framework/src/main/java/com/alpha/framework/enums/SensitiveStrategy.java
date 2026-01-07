package com.alpha.framework.enums;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;

import java.util.function.Function;

/**
 * 脱敏策略枚举
 * <p>
 * 定义了常见的隐私数据脱敏规则
 */
@AllArgsConstructor
public enum SensitiveStrategy {

    /**
     * 中文名：只显示第一个汉字，其他隐藏为2个星号
     * 例：李雷 -> 李**，张三丰 -> 张**
     */
    CHINESE_NAME(s -> StrUtil.hide(s, 1, s.length())),

    /**
     * 身份证号：保留前4位，后4位，中间用星号替换
     * 例：110101199003073838 -> 1101**********3838
     */
    ID_CARD(s -> StrUtil.hide(s, 4, s.length() - 4)),

    /**
     * 手机号：保留前3位，后4位，中间4位用星号替换
     * 例：13812345678 -> 138****5678
     */
    PHONE(s -> StrUtil.hide(s, 3, s.length() - 4)),

    /**
     * 固定电话：保留前4位，后2位
     * 例：010-12345678 -> 010-******78
     */
    FIXED_PHONE(s -> StrUtil.hide(s, 4, s.length() - 2)),

    /**
     * 地址：只显示到第8位，后面全部隐藏
     * 例：北京市海淀区中关村大街1号 -> 北京市海淀区****
     */
    ADDRESS(s -> StrUtil.hide(s, 8, s.length())),

    /**
     * 电子邮箱：邮箱前缀仅显示第一个字符，前缀其他隐藏，后缀保持不变
     * 例：zhangsan@163.com -> z*******@163.com
     */
    EMAIL(DesensitizedUtil::email),

    /**
     * 银行卡：保留前4位，后4位
     * 例：6222600000001234 -> 6222********1234
     */
    BANK_CARD(DesensitizedUtil::bankCard),

    /**
     * 密码：全部字符隐藏为6个星号
     * 例：123456 -> ******
     */
    PASSWORD(s -> "******"),

    /**
     * 自定义：不处理（备用）
     */
    NONE(s -> s);

    /**
     * 脱敏处理函数
     */
    private final Function<String, String> desensitizer;

    /**
     * 执行脱敏
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return desensitizer.apply(value);
    }
}