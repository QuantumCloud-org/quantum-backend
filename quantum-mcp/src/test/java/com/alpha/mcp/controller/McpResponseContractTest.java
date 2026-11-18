package com.alpha.mcp.controller;

import com.alpha.framework.advice.ResponseAdvice;
import com.alpha.framework.util.JsonUtil;
import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.manifest.CapabilityManifestService;
import com.alpha.mcp.tool.McpToolService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpResponseContractTest {
    @Test
    void preservesJsonRpcEnvelopeThroughMvc() throws Exception {
        var controller = new McpController(new CapabilityManifestService(new McpProperties()), mock(McpToolService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ResponseAdvice(new JsonUtil(JsonMapper.builder().build())))
                .build();
        mvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":1,"method":"initialize"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result.protocolVersion").exists());
    }
}
