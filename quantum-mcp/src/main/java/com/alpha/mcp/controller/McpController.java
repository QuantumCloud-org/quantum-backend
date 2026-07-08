package com.alpha.mcp.controller;

import com.alpha.framework.exception.BizException;
import com.alpha.mcp.manifest.CapabilityManifestService;
import com.alpha.mcp.tool.McpToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mcp", name = "enabled", havingValue = "true")
public class McpController {

    private final CapabilityManifestService manifestService;
    private final McpToolService toolService;

    @GetMapping("/mcp")
    public Map<String, Object> manifest() {
        return manifestService.manifest();
    }

    @PostMapping("/mcp")
    public Map<String, Object> call(@RequestBody Map<String, Object> request) {
        if ("2.0".equals(String.valueOf(request.get("jsonrpc")))) {
            return jsonRpc(request);
        }

        String method = String.valueOf(request.getOrDefault("method", ""));
        if ("tools/list".equals(method)) {
            return Map.of("tools", manifestService.tools());
        }
        if (!"tools/call".equals(method)) {
            return Map.of("manifest", manifestService.manifest());
        }
        String name = String.valueOf(request.get("name"));
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = request.get("arguments") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        return legacyCallTool(name, arguments);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonRpc(Map<String, Object> request) {
        Object id = request.get("id");
        String method = String.valueOf(request.getOrDefault("method", ""));
        Map<String, Object> params = request.get("params") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        return switch (method) {
            case "initialize" -> jsonRpcResult(id, Map.of(
                    "protocolVersion", "2025-06-18",
                    "capabilities", Map.of("tools", Map.of("listChanged", false)),
                    "serverInfo", Map.of("name", "quantum-mcp", "version", "1.0.0")
            ));
            case "tools/list" -> jsonRpcResult(id, Map.of("tools", manifestService.tools()));
            case "tools/call" -> {
                String name = String.valueOf(params.get("name"));
                Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> map
                        ? (Map<String, Object>) map
                        : Map.of();
                try {
                    yield jsonRpcResult(id, Map.of(
                            "content", java.util.List.of(Map.of(
                                    "type", "text",
                                    "text", toolService.callTool(name, arguments)
                            )),
                            "isError", false
                    ));
                } catch (BizException e) {
                    yield jsonRpcError(id, jsonRpcCode(e), e.getMessage());
                } catch (RuntimeException e) {
                    yield jsonRpcError(id, -32603, "Internal error");
                }
            }
            default -> jsonRpcError(id, -32601, "Method not found");
        };
    }

    private static int jsonRpcCode(BizException e) {
        return switch (e.getCode()) {
            case 401, 403 -> -32003;
            case 400, 404 -> -32602;
            default -> -32603;
        };
    }

    private Map<String, Object> legacyCallTool(String name, Map<String, Object> arguments) {
        try {
            return Map.of(
                    "name", name,
                    "contentType", "application/json",
                    "ok", true,
                    "output", toolService.callTool(name, arguments)
            );
        } catch (BizException e) {
            return Map.of(
                    "name", name,
                    "contentType", "application/json",
                    "ok", false,
                    "error", Map.of("code", e.getCode(), "message", e.getMessage())
            );
        } catch (RuntimeException e) {
            return Map.of(
                    "name", name,
                    "contentType", "application/json",
                    "ok", false,
                    "error", Map.of("code", 500, "message", "Internal error")
            );
        }
    }

    private static Map<String, Object> jsonRpcResult(Object id, Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> jsonRpcError(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }
}
