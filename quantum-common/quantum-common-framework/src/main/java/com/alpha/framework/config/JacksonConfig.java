package com.alpha.framework.config;

import com.alpha.framework.constant.CommonConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.TimeZone;

/**
 * Jackson 3.x 全局配置
 * <p>
 * Jackson 3.x 主要变更：
 * 1. 包名变更：com.fasterxml.jackson → tools.jackson（jackson-annotations 例外）
 * 2. Java 8 时间模块已内置到 jackson-databind，无需单独注册 JavaTimeModule
 * 3. 使用不可变的 JsonMapper.builder() 模式构建
 * 4. SerializerProvider 重命名为 SerializationContext
 * 5. JsonParser.Feature 拆分为 JsonReadFeature（JSON特有）和 StreamReadFeature（通用）
 * <p>
 * 核心处理：
 * 1. 时间格式：保持时间戳格式（Long 毫秒值）
 * 2. Long/BigInteger 超出 JS 安全范围时转 String
 * 3. BigDecimal 保留精度不使用科学计数法
 * 4. XSS 防护（可配置开关）
 * 5. 空值处理策略（NON_NULL）
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    /**
     * XSS 防护开关（从配置文件读取）
     */
    @Value("${security.xss-enabled:true}")
    private boolean xssEnabled;

    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                // 时区配置
                .defaultTimeZone(TimeZone.getTimeZone(CommonConstants.TIMEZONE))

                // ==================== 反序列化配置 ====================
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)

                // ==================== 序列化配置 ====================
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

                // ==================== 日期时间配置 ====================
                // 启用时间戳格式
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)

                // ==================== JSON 读取特性（JsonReadFeature - JSON 格式专用）====================
                // 允许单引号（非标准 JSON，但兼容性更好）
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                // 允许不带引号的字段名（非标准 JSON，但 JavaScript 风格常用）
                .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)

                // ==================== JSON/流写入特性 ====================
                // BigDecimal 不使用科学计数法
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)

                // ==================== null 处理策略 ====================
                .changeDefaultPropertyInclusion(inclusion ->
                        inclusion.withValueInclusion(JsonInclude.Include.NON_NULL)
                )

                // ==================== 注册自定义模块 ====================
                .addModule(numericModule());

        // 注册 XSS 防护模块（根据配置开关）
        if (xssEnabled) {
            builder.addModule(xssModule());
        }

        return builder.build();
    }

    /**
     * 数值类型处理模块
     */
    private SimpleModule numericModule() {
        SimpleModule module = new SimpleModule("NumericModule");
        // Long：始终转 String（前端 JS 精度问题）
        module.addSerializer(Long.class, new LongToStringSerializer());
        module.addSerializer(long.class, new LongToStringSerializer());
        // BigInteger：始终转 String
        module.addSerializer(BigInteger.class, new BigIntegerToStringSerializer());
        // BigDecimal：避免科学计数法，转为纯字符串
        module.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());
        return module;
    }

    /**
     * XSS 防护模块
     * <p>
     * 通过 Jackson 反序列化时自动清理 XSS 攻击字符串
     */
    private SimpleModule xssModule() {
        SimpleModule module = new SimpleModule("XssModule");
        module.addDeserializer(String.class, new XssStringDeserializer());
        return module;
    }

    // ==================== 自定义序列化器/反序列化器 ====================

    /**
     * Long 转 String 序列化器
     * <p>
     * JavaScript Number 类型只能精确表示 -(2^53-1) 到 2^53-1 之间的整数
     * 统一转为 String 以避免前端精度丢失
     */
    public static class LongToStringSerializer extends StdSerializer<Long> {
        public LongToStringSerializer() {
            super(Long.class);
        }

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    /**
     * BigInteger 转 String 序列化器
     */
    public static class BigIntegerToStringSerializer extends StdSerializer<BigInteger> {
        public BigIntegerToStringSerializer() {
            super(BigInteger.class);
        }

        @Override
        public void serialize(BigInteger value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    /**
     * BigDecimal 序列化器
     * <p>
     * 使用 toPlainString() 避免科学计数法输出
     */
    public static class BigDecimalPlainSerializer extends StdSerializer<BigDecimal> {
        public BigDecimalPlainSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toPlainString());
            }
        }
    }

    /**
     * XSS 字符串反序列化器
     * <p>
     * 在 JSON 反序列化时自动清理 HTML 标签，防止 XSS 攻击
     */
    public static class XssStringDeserializer extends StdDeserializer<String> {
        public XssStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return value;
            }
            // 使用 Hutool 的 HTML 清理工具
            return cn.hutool.http.HtmlUtil.cleanHtmlTag(value);
        }
    }
}