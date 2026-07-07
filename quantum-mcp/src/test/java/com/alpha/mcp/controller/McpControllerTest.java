package com.alpha.mcp.controller;

import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.manifest.CapabilityManifestService;
import com.alpha.mcp.tool.McpToolService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void supportsJsonRpcInitializeAndToolsList() {
        McpController controller = new McpController(new CapabilityManifestService(new McpProperties()), mock(McpToolService.class));

        Map<String, Object> initialized = controller.call(Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize"));
        Map<String, Object> initResult = (Map<String, Object>) initialized.get("result");
        assertThat(initialized).containsEntry("jsonrpc", "2.0").containsEntry("id", 1);
        assertThat(initResult).containsEntry("protocolVersion", "2025-06-18");

        Map<String, Object> tools = controller.call(Map.of(
                "jsonrpc", "2.0",
                "id", 2,
                "method", "tools/list"));
        Map<String, Object> toolsResult = (Map<String, Object>) tools.get("result");
        assertThat(String.valueOf(toolsResult.get("tools"))).contains("system.user.search");
    }

    @Test
    @SuppressWarnings("unchecked")
    void wrapsToolCallOutputAsMcpTextContent() {
        McpToolService toolService = mock(McpToolService.class);
        when(toolService.callTool(eq("system.user.search"), eq(Map.of("pageNum", 1, "pageSize", 10))))
                .thenReturn("{\"records\":[],\"total\":0}");
        McpController controller = new McpController(new CapabilityManifestService(new McpProperties()), toolService);

        Map<String, Object> response = controller.call(Map.of(
                "jsonrpc", "2.0",
                "id", "call-1",
                "method", "tools/call",
                "params", Map.of(
                        "name", "system.user.search",
                        "arguments", Map.of("pageNum", 1, "pageSize", 10))));

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertThat(result).containsEntry("isError", false);
        assertThat(String.valueOf(result.get("content"))).contains("\"records\":[]");
    }
}
