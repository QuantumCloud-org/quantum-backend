package com.alpha.security.annotation;

import com.alpha.framework.enums.SensitiveStrategy;
import com.alpha.security.serializer.SensitiveSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解（Jackson 3.0）
 * <p>
 * 使用方法：在 VO 类的 String 字段上添加 @Sensitive(SensitiveStrategy.PHONE)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /**
     * 脱敏策略
     */
    SensitiveStrategy value();
}
