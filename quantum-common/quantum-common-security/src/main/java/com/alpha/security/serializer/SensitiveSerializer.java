package com.alpha.security.serializer;

import com.alpha.security.annotation.Sensitive;
import com.alpha.framework.enums.SensitiveStrategy;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * 脱敏序列化器
 * <p>
 * 基于 Jackson 实现，在 JSON 序列化时自动对指定字段进行脱敏处理
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveStrategy strategy;

    /**
     * 序列化逻辑
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (strategy != null) {
            // 执行脱敏
            gen.writeString(strategy.desensitize(value));
        } else {
            // 无策略则原样输出
            gen.writeString(value);
        }
    }

    /**
     * 获取上下文信息（注解）
     * <p>
     * 当 Jackson 扫描到字段时会调用此方法，我们在这里读取 @Sensitive 注解
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        // 如果是独立的 String 值（非对象字段），直接返回默认
        if (property == null) {
            return prov.findValueSerializer(String.class);
        }

        // 获取字段上的注解
        Sensitive annotation = property.getAnnotation(Sensitive.class);

        // 如果字段上确实有 @Sensitive 注解
        if (Objects.nonNull(annotation)) {
            // 创建一个新的序列化器实例，并注入策略
            SensitiveSerializer serializer = new SensitiveSerializer();
            serializer.strategy = annotation.value();
            return serializer;
        }

        // 没有注解，返回默认序列化器
        return prov.findValueSerializer(property.getType(), property);
    }
}