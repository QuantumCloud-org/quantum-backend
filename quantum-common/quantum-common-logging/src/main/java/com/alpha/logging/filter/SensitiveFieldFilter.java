package com.alpha.logging.filter;

import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

import java.util.Locale;
import java.util.Set;

/**
 * 敏感字段过滤器
 */
public class SensitiveFieldFilter extends SimpleBeanPropertyFilter {

    private final Set<String> sensitiveFields;

    public SensitiveFieldFilter(Set<String> sensitiveFields) {
        this.sensitiveFields = sensitiveFields;
    }

    @Override
    protected boolean include(BeanPropertyWriter writer) {
        return shouldInclude(writer.getName());
    }

    @Override
    protected boolean include(PropertyWriter writer) {
        return shouldInclude(writer.getName());
    }

    private boolean shouldInclude(String fieldName) {
        return fieldName == null || !sensitiveFields.contains(fieldName.toLowerCase(Locale.ROOT));
    }
}
