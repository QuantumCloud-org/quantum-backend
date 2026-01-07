package com.alpha.security.annotation;

import java.lang.annotation.*;

/**
 * 需要角色注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * 角色标识
     */
    String[] value();

    /**
     * 逻辑关系（AND/OR）
     */
    RequiresPermission.Logical logical() default RequiresPermission.Logical.AND;
}