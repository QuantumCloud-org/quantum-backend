package com.alpha.logging.filter;

import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 敏感字段过滤器
 */
public class SensitiveFieldFilter extends SimpleBeanPropertyFilter {

    private final Set<String> sensitiveFields;

    public SensitiveFieldFilter(Collection<String> sensitiveFields) {
        this.sensitiveFields = normalize(sensitiveFields);
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

    private Set<String> normalize(Collection<String> fields) {
        Set<String> normalized = new LinkedHashSet<>();
        if (fields == null) {
            return normalized;
        }
        for (String field : fields) {
            if (field != null && !field.isBlank()) {
                normalized.add(field.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }
}
