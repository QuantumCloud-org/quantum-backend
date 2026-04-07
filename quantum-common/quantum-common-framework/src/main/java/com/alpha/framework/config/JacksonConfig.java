package com.alpha.framework.config;

import com.alpha.framework.constant.CommonConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Value;
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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

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

    @Value("${security.xss-enabled:true}")
    private boolean xssEnabled;

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
                .addModule(numericModule());

        if (xssEnabled) {
            builder.addModule(xssModule());
        }

        return builder.build();
    }

    private SimpleModule customDateTimeModule() {
        SimpleModule module = new SimpleModule("CustomDateTimeModule");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(CommonConstants.DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(CommonConstants.TIME_PATTERN);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(CommonConstants.DATETIME_PATTERN);

        module.addSerializer(LocalDate.class, new CustomLocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new CustomLocalDateDeserializer(dateFormatter));
        module.addSerializer(LocalTime.class, new CustomLocalTimeSerializer(timeFormatter));
        module.addDeserializer(LocalTime.class, new CustomLocalTimeDeserializer(timeFormatter));
        module.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer(dateTimeFormatter));

        return module;
    }

    private SimpleModule numericModule() {
        SimpleModule module = new SimpleModule("NumericModule");
        module.addSerializer(Long.class, new LongToStringSerializer());
        module.addSerializer(long.class, new LongToStringSerializer());
        module.addSerializer(BigInteger.class, new BigIntegerToStringSerializer());
        module.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());
        return module;
    }

    private SimpleModule xssModule() {
        SimpleModule module = new SimpleModule("XssModule");
        module.addDeserializer(String.class, new XssStringDeserializer());
        return module;
    }

    private static class CustomLocalDateSerializer extends StdSerializer<LocalDate> {
        private final DateTimeFormatter formatter;

        public CustomLocalDateSerializer(DateTimeFormatter formatter) {
            super(LocalDate.class);
            this.formatter = formatter;
        }

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.format(formatter));
            }
        }
    }

    private static class CustomLocalDateDeserializer extends StdDeserializer<LocalDate> {
        private final DateTimeFormatter formatter;

        public CustomLocalDateDeserializer(DateTimeFormatter formatter) {
            super(LocalDate.class);
            this.formatter = formatter;
        }

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return LocalDate.parse(value, formatter);
        }
    }

    private static class CustomLocalTimeSerializer extends StdSerializer<LocalTime> {
        private final DateTimeFormatter formatter;

        public CustomLocalTimeSerializer(DateTimeFormatter formatter) {
            super(LocalTime.class);
            this.formatter = formatter;
        }

        @Override
        public void serialize(LocalTime value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.format(formatter));
            }
        }
    }

    private static class CustomLocalTimeDeserializer extends StdDeserializer<LocalTime> {
        private final DateTimeFormatter formatter;

        public CustomLocalTimeDeserializer(DateTimeFormatter formatter) {
            super(LocalTime.class);
            this.formatter = formatter;
        }

        @Override
        public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return LocalTime.parse(value, formatter);
        }
    }

    private static class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
        private final DateTimeFormatter formatter;

        public CustomLocalDateTimeSerializer(DateTimeFormatter formatter) {
            super(LocalDateTime.class);
            this.formatter = formatter;
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.format(formatter));
            }
        }
    }

    private static class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        private final DateTimeFormatter formatter;

        public CustomLocalDateTimeDeserializer(DateTimeFormatter formatter) {
            super(LocalDateTime.class);
            this.formatter = formatter;
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(value, formatter);
        }
    }

    private static class LongToStringSerializer extends StdSerializer<Long> {
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

    private static class BigIntegerToStringSerializer extends StdSerializer<BigInteger> {
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
            if (value == null || value.isEmpty()) {
                return value;
            }
            return cn.hutool.http.HtmlUtil.cleanHtmlTag(value);
        }
    }
}
