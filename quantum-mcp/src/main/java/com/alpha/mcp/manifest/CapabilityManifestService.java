package com.alpha.mcp.manifest;

import com.alpha.mcp.config.McpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CapabilityManifestService {

    private final McpProperties properties;

    public Map<String, Object> manifest() {
        return Map.of(
                "schemaVersion", "quantum.mcp.manifest.v1",
                "resource", properties.getResource(),
                "authorizationServers", List.of(properties.normalizedIssuer()),
                "transport", "streamable-http",
                "tools", tools()
        );
    }

    public List<Map<String, Object>> tools() {
        return List.of(
                tool(
                        "system.user.search",
                        "查询用户",
                        "按关键词读取用户列表, 返回结果受当前操作者数据权限过滤",
                        "system:user:list",
                        "required",
                        "low",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "keyword", Map.of("type", "string", "maxLength", 50),
                                        "pageNum", Map.of("type", "integer", "minimum", 1),
                                        "pageSize", Map.of("type", "integer", "minimum", 1, "maximum", 50)
                                ),
                                "required", List.of("pageNum", "pageSize")
                        ),
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "records", Map.of("type", "array"),
                                        "total", Map.of("type", "integer")
                                )
                        )
                ),
                tool(
                        "system.dept.tree",
                        "查询部门树",
                        "读取部门树, 仅暴露部门只读字段",
                        "system:dept:list",
                        "permission-only",
                        "low",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "deptName", Map.of("type", "string", "maxLength", 50),
                                        "status", Map.of("type", "integer", "enum", List.of(0, 1))
                                )
                        ),
                        Map.of("type", "array")
                ),
                tool(
                        "system.role.list",
                        "查询角色",
                        "读取角色列表, 不包含角色授权修改能力",
                        "system:role:query",
                        "permission-only",
                        "low",
                        Map.of("type", "object", "properties", Map.of()),
                        Map.of("type", "array")
                )
        );
    }

    public Map<String, Object> toolByName(String name) {
        return tools().stream()
                .filter(tool -> name.equals(tool.get("name")))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> tool(
            String name,
            String title,
            String description,
            String permission,
            String dataScopeMode,
            String riskLevel,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema) {
        return Map.ofEntries(
                Map.entry("name", name),
                Map.entry("title", title),
                Map.entry("description", description),
                Map.entry("readOnly", true),
                Map.entry("permission", permission),
                Map.entry("dataScopeMode", dataScopeMode),
                Map.entry("riskLevel", riskLevel),
                Map.entry("auditEvent", "MCP_TOOL_CALL"),
                Map.entry("inputSchema", inputSchema),
                Map.entry("outputSchema", outputSchema)
        );
    }
}
