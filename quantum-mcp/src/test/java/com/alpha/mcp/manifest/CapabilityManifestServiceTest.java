package com.alpha.mcp.manifest;

import com.alpha.framework.util.JsonUtil;
import com.alpha.mcp.config.McpProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityManifestServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void manifestContainsRequiredToolMetadataAndNoPrivateIdentityHeaders() {
        CapabilityManifestService service = new CapabilityManifestService(new McpProperties());

        Map<String, Object> manifest = service.manifest();

        assertThat(manifest).containsKeys("schemaVersion", "resource", "authorizationServers", "transport", "tools");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) manifest.get("tools");
        assertThat(tools)
                .extracting(tool -> tool.get("name"))
                .containsExactly("system.user.search", "system.dept.tree", "system.role.list");
        assertThat(tools)
                .allSatisfy(tool -> assertThat(tool)
                        .containsKeys("readOnly", "permission", "dataScopeMode", "riskLevel", "inputSchema", "outputSchema"));

        String json = new JsonUtil(new ObjectMapper()).toJson(manifest);
        assertThat(json).doesNotContain("X-User-Id", "X-Dept-Ids", "X-Permissions");
    }
}
