package com.alpha.security.serializer;

import com.alpha.security.annotation.Sensitive;
import com.alpha.framework.enums.SensitiveStrategy;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * 脱敏序列化器（Jackson 3.0）
 * <p>
 * 基于 Jackson 3.0 实现，在 JSON 序列化时自动对指定字段进行脱敏处理。
 * 通过 {@link Sensitive} 注解 + {@code @JsonSerialize(using = ...)} 触发。
 */
public class SensitiveSerializer extends StdSerializer<String> {

    private final SensitiveStrategy strategy;

    public SensitiveSerializer() {
        super(String.class);
        this.strategy = null;
    }

    public SensitiveSerializer(SensitiveStrategy strategy) {
        super(String.class);
        this.strategy = strategy;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
        if (strategy != null && value != null) {
            gen.writeString(strategy.desensitize(value));
        } else {
            gen.writeString(value);
        }
    }

    /**
     * Jackson 3.0 contextual serializer 支持
     * <p>
     * 当 Jackson 扫描到字段时调用此方法，读取 {@link Sensitive} 注解并创建带策略的序列化器实例。
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }

        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation != null) {
            return new SensitiveSerializer(annotation.value());
        }

        return this;
    }
}
