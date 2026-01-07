package com.alpha.framework.config;

import com.alpha.framework.constant.CommonConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 全局配置
 * <p>
 * 核心处理：
 * 1. 时间格式统一（LocalDateTime/LocalDate/LocalTime）
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
    public ObjectMapper objectMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                // 模块注册
                .addModule(javaTimeModule())
                .addModule(numericModule())

                // 时区
                .defaultTimeZone(TimeZone.getTimeZone(CommonConstants.TIMEZONE))

                // 日期格式（主要用于 java.util.Date）
                .defaultDateFormat(new java.text.SimpleDateFormat(CommonConstants.DATETIME_PATTERN) {{
                    setTimeZone(TimeZone.getTimeZone(CommonConstants.TIMEZONE));
                }})

                // 反序列化配置
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)

                // 序列化配置
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)

                // 解析器配置
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)

                // 生成器配置
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)

                // null 处理
                .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS
                ));

        // 注册 XSS 防护模块（根据配置开关）
        if (xssEnabled) {
            builder.addModule(xssModule());
        }

        return builder.build();
    }

    /**
     * Java 8+ 时间类型模块
     */
    private JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        // 使用 CommonConstants 中定义的格式，确保全局统一
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(CommonConstants.DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(CommonConstants.TIME_PATTERN);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(CommonConstants.DATETIME_PATTERN);

        // LocalDate
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        // LocalTime
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        // LocalDateTime
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        return module;
    }

    /**
     * 数值类型处理模块
     */
    private SimpleModule numericModule() {
        SimpleModule module = new SimpleModule("NumericModule");

        // Long：超出 JS 安全范围时转 String
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // BigInteger：始终转 String
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);

        // BigDecimal：避免科学计数法
        module.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());

        return module;
    }

    /**
     * XSS 防护模块
     * <p>
     * 通过 Jackson 反序列化时自动清理 XSS 攻击字符串
     * 比 Filter 方式更可靠，能处理 JSON Body
     */
    private SimpleModule xssModule() {
        SimpleModule module = new SimpleModule("XssModule");
        module.addDeserializer(String.class, new XssStringDeserializer());
        return module;
    }

    /**
     * 安全的 Long 序列化器
     * <p>
     * JavaScript Number 类型只能精确表示 -(2^53-1) 到 2^53-1 之间的整数
     * 超出此范围的 Long 值会丢失精度，因此转为 String
     */
    public static class SafeLongSerializer extends JsonSerializer<Long> {
        private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;
        private static final long JS_MIN_SAFE_INTEGER = -9007199254740991L;

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            if (value > JS_MAX_SAFE_INTEGER || value < JS_MIN_SAFE_INTEGER) {
                gen.writeString(value.toString());
            } else {
                gen.writeNumber(value);
            }
        }
    }

    /**
     * BigDecimal 序列化器
     * <p>
     * 使用 toPlainString() 避免科学计数法输出
     */
    public static class BigDecimalPlainSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
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
    public static class XssStringDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return value;
            }
            // 使用 Hutool 的 HTML 清理工具
            return cn.hutool.http.HtmlUtil.cleanHtmlTag(value);
        }
    }
}