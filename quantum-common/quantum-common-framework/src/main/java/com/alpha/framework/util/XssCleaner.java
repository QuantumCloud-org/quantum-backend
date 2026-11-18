package com.alpha.framework.util;

import cn.hutool.http.HtmlUtil;

import java.util.Locale;

public final class XssCleaner {

    public static final String ENABLED_REQUEST_ATTRIBUTE = XssCleaner.class.getName() + ".enabled";

    private XssCleaner() {
    }

    public static String clean(String field, String value) {
        if (value == null || value.isBlank() || isPasswordField(field)) {
            return value;
        }
        return HtmlUtil.cleanHtmlTag(value);
    }

    private static boolean isPasswordField(String field) {
        return field != null && field.toLowerCase(Locale.ROOT).contains("password");
    }
}
