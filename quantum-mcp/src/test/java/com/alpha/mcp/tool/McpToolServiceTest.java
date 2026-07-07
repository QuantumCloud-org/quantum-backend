package com.alpha.mcp.tool;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.BizException;
import com.alpha.framework.util.JsonUtil;
import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.manifest.CapabilityManifestService;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.service.ISysDeptService;
import com.alpha.system.service.ISysRoleService;
import com.alpha.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class McpToolServiceTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void toolCallFailsClosedWithoutUserContext() {
        McpToolService service = newService();

        assertThatThrownBy(() -> service.callTool("system.user.search", Map.of("pageNum", 1, "pageSize", 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("authenticated user context");
    }

    @Test
    void toolCallRequiresDeclaredPermission() {
        UserContext.setUser(new LoginUser()
                .setUserId(2L)
                .setUsername("alice")
                .setStatus(1)
                .setPermissions(Set.of("system:dept:list")));
        McpToolService service = newService();

        assertThatThrownBy(() -> service.callTool("system.user.search", Map.of("pageNum", 1, "pageSize", 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("permission denied");
    }

    private static McpToolService newService() {
        return new McpToolService(
                new CapabilityManifestService(new McpProperties()),
                mock(ISysUserService.class),
                mock(ISysDeptService.class),
                mock(ISysRoleService.class),
                new UserConvert(),
                new JsonUtil(new ObjectMapper()));
    }
}
