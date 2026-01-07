package com.alpha.security.annotation;

import java.lang.annotation.*;

/**
 * 匿名访问注解（无需认证）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous {
}