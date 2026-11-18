package com.alpha.framework.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * JSON 工具类
 * <p>
 * 基于 Spring 容器中的 ObjectMapper（已统一配置）
 * 提供 JSON 字符串序列化
 * <p>
 * 设计原则：
 * 1. 异常不外抛，返回默认值或 null
 * 2. 日志记录错误，便于排查
 */
@Slf4j
@Getter
@Component
public class JsonUtil {

    /**
     * -- GETTER --
     * 获取 ObjectMapper 实例
     * 用于需要直接操作 ObjectMapper 的场景
     */
    private final ObjectMapper objectMapper;

    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== 序列化 ====================

    /**
     * 对象转 JSON 字符串
     */
    public String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            log.error("JSON 序列化失败: {}", object.getClass().getName(), e);
            return null;
        }
    }
}
