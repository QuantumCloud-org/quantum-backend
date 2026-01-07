package com.alpha.framework.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * <p>
 * 基于 Spring 容器中的 ObjectMapper（已统一配置）
 * 提供类型安全的序列化/反序列化方法
 * <p>
 * 设计原则：
 * 1. 异常不外抛，返回默认值或 null
 * 2. 支持泛型，类型安全
 * 3. 日志记录错误，便于排查
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
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败: {}", object.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串（格式化输出）
     */
    public String toPrettyJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败: {}", object.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 对象转 byte[]
     */
    public byte[] toBytes(Object object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败: {}", object.getClass().getName(), e);
            return new byte[0];
        }
    }

    // ==================== 反序列化 ====================

    /**
     * JSON 字符串转对象
     */
    public <T> T parse(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化失败: {} -> {}", json, clazz.getName(), e);
            return null;
        }
    }

    /**
     * JSON 字符串转泛型对象
     * <p>
     * 使用示例：
     * List<UserVO> users = jsonUtil.parse(json, new TypeReference<List<UserVO>>() {});
     * Map<String, Object> map = jsonUtil.parse(json, new TypeReference<>() {});
     */
    public <T> T parse(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化失败: {}", typeRef.getType().getTypeName(), e);
            return null;
        }
    }

    /**
     * JSON 转 List
     */
    public <T> List<T> parseList(String json, Class<T> elementClass) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (JsonProcessingException e) {
            log.error("JSON 转 List 失败: {} -> List<{}>", json, elementClass.getName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * JSON 转 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("JSON 转 Map 失败: {}", json, e);
            return Collections.emptyMap();
        }
    }

    /**
     * byte[] 转对象
     */
    public <T> T parse(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (Exception e) {
            log.error("JSON 反序列化失败: byte[] -> {}", clazz.getName(), e);
            return null;
        }
    }

    // ==================== 对象转换 ====================

    /**
     * 对象转换（如 Map 转 POJO）
     */
    public <T> T convert(Object fromValue, Class<T> toClass) {
        if (fromValue == null) {
            return null;
        }
        return objectMapper.convertValue(fromValue, toClass);
    }

    /**
     * 对象转换（泛型版本）
     */
    public <T> T convert(Object fromValue, TypeReference<T> typeRef) {
        if (fromValue == null) {
            return null;
        }
        return objectMapper.convertValue(fromValue, typeRef);
    }

    // ==================== JsonNode 操作 ====================

    /**
     * 解析为 JsonNode（用于动态 JSON 处理）
     */
    public JsonNode parseTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析为 JsonNode 失败: {}", json, e);
            return null;
        }
    }

    /**
     * 创建空的 ObjectNode
     */
    public ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    /**
     * 创建空的 ArrayNode
     */
    public ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    /**
     * 从 JsonNode 提取值
     */
    public String getString(JsonNode node, String fieldName) {
        return getString(node, fieldName, null);
    }

    public String getString(JsonNode node, String fieldName, String defaultValue) {
        if (node == null || !node.has(fieldName)) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        return valueNode.isNull() ? defaultValue : valueNode.asText();
    }

    public Integer getInt(JsonNode node, String fieldName) {
        return getInt(node, fieldName, null);
    }

    public Integer getInt(JsonNode node, String fieldName, Integer defaultValue) {
        if (node == null || !node.has(fieldName)) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        return valueNode.isNull() ? defaultValue : valueNode.asInt();
    }

    public Long getLong(JsonNode node, String fieldName) {
        return getLong(node, fieldName, null);
    }

    public Long getLong(JsonNode node, String fieldName, Long defaultValue) {
        if (node == null || !node.has(fieldName)) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        return valueNode.isNull() ? defaultValue : valueNode.asLong();
    }

    public Boolean getBoolean(JsonNode node, String fieldName) {
        return getBoolean(node, fieldName, null);
    }

    public Boolean getBoolean(JsonNode node, String fieldName, Boolean defaultValue) {
        if (node == null || !node.has(fieldName)) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        return valueNode.isNull() ? defaultValue : valueNode.asBoolean();
    }

    // ==================== 校验方法 ====================

    /**
     * 判断是否为有效的 JSON 字符串
     */
    public boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断是否为 JSON 对象（而非数组）
     */
    public boolean isJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        String trimmed = json.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    /**
     * 判断是否为 JSON 数组
     */
    public boolean isJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        String trimmed = json.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    // ==================== 获取 ObjectMapper ====================

}