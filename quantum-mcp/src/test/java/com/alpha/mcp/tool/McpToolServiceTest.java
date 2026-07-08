package com.alpha.mcp.tool;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.exception.BizException;
import com.alpha.framework.util.JsonUtil;
import com.alpha.mcp.config.McpProperties;
import com.alpha.mcp.manifest.CapabilityManifestService;
import com.alpha.mcp.security.McpAuthenticationDetails;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.service.ISysDeptService;
import com.alpha.system.service.ISysRoleService;
import com.alpha.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class McpToolServiceTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
        SecurityContextHolder.clearContext();
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
        setOAuthScopes("system.user.read");
        McpToolService service = newService();

        assertThatThrownBy(() -> service.callTool("system.user.search", Map.of("pageNum", 1, "pageSize", 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("permission denied");
    }

    @Test
    void toolCallRequiresMatchingOAuthScope() {
        UserContext.setUser(new LoginUser()
                .setUserId(2L)
                .setUsername("alice")
                .setStatus(1)
                .setPermissions(Set.of("system:user:list")));
        setOAuthScopes("system.dept.read");
        McpToolService service = newService();

        assertThatThrownBy(() -> service.callTool("system.user.search", Map.of("pageNum", 1, "pageSize", 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("scope denied");
    }

    @Test
    void toolCallRejectsInvalidIntegerArguments() {
        UserContext.setUser(new LoginUser()
                .setUserId(2L)
                .setUsername("alice")
                .setStatus(1)
                .setPermissions(Set.of("system:user:list")));
        setOAuthScopes("system.user.read");
        McpToolService service = newService();

        assertThatThrownBy(() -> service.callTool("system.user.search", Map.of("pageNum", "bad", "pageSize", 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("invalid MCP tool argument: pageNum");
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

    private static void setOAuthScopes(String... scopes) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(UserContext.getUser(), null, UserContext.getUser().getAuthorities());
        authentication.setDetails(new McpAuthenticationDetails(null, Set.of(scopes), "quantum-local-agent", "http://localhost:8080/mcp"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
