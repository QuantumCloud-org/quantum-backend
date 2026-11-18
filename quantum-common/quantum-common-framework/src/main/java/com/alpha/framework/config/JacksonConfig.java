package com.alpha.framework.config;

import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.util.XssCleaner;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 3.x 全局配置
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .defaultTimeZone(TimeZone.getTimeZone(CommonConstants.TIMEZONE))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .changeDefaultPropertyInclusion(inclusion ->
                        inclusion.withValueInclusion(JsonInclude.Include.NON_NULL)
                )
                .addModule(customDateTimeModule())
                .addModule(numericModule())
                .addModule(xssModule());

        return builder.build();
    }

    private SimpleModule customDateTimeModule() {
        SimpleModule module = new SimpleModule("CustomDateTimeModule");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(CommonConstants.DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(CommonConstants.TIME_PATTERN);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(CommonConstants.DATETIME_PATTERN);

        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        return module;
    }

    private SimpleModule numericModule() {
        SimpleModule module = new SimpleModule("NumericModule");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(long.class, ToStringSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        module.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());
        return module;
    }

    private SimpleModule xssModule() {
        SimpleModule module = new SimpleModule("XssModule");
        module.addDeserializer(String.class, new XssStringDeserializer());
        return module;
    }

    private static class BigDecimalPlainSerializer extends StdSerializer<BigDecimal> {
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

    private static class XssStringDeserializer extends StdDeserializer<String> {
        public XssStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            RequestAttributes request = RequestContextHolder.getRequestAttributes();
            if (request != null && Boolean.TRUE.equals(request.getAttribute(
                    XssCleaner.ENABLED_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))) {
                return XssCleaner.clean(p.currentName(), value);
            }
            return value;
        }
    }
}
