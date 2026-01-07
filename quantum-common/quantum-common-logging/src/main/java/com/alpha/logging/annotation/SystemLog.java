package com.alpha.logging.annotation;

import com.alpha.logging.enums.BusinessType;
import com.alpha.logging.enums.OperatorType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemLog {

    /**
     * 模块名称
     */
    String title() default "";

    /**
     * 方法描述
     */
    String description() default "";

    /**
     * 业务类型
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作人类型
     */
    OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求参数
     */
    boolean saveParams() default true;

    /**
     * 是否保存响应结果
     */
    boolean saveResult() default true;

    /**
     * 排除的参数名称
     */
    String[] excludeParams() default {"password", "oldPassword", "newPassword", "confirmPassword"};
}