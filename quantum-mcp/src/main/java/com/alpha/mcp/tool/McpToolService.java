package com.alpha.mcp.tool;

import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.framework.util.JsonUtil;
import com.alpha.mcp.manifest.CapabilityManifestService;
import com.alpha.mcp.security.McpAuthenticationDetails;
import com.alpha.orm.entity.PageResult;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.domain.SysDept;
import com.alpha.system.domain.SysRole;
import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.UserQuery;
import com.alpha.system.service.ISysDeptService;
import com.alpha.system.service.ISysRoleService;
import com.alpha.system.service.ISysUserService;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McpToolService {

    private final CapabilityManifestService manifestService;
    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysRoleService roleService;
    private final UserConvert userConvert;
    private final JsonUtil jsonUtil;

    public String callTool(String name, Map<String, Object> arguments) {
        LoginUser loginUser = requireAuthenticatedUser();
        Map<String, Object> tool = manifestService.toolByName(name);
        if (tool == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "MCP tool is not registered");
        }
        requireOAuthScope(String.valueOf(tool.get("scope")));
        requirePermission(loginUser, String.valueOf(tool.get("permission")));

        Object result = switch (name) {
            case "system.user.search" -> searchUsers(arguments);
            case "system.dept.tree" -> deptTree(arguments);
            case "system.role.list" -> roleList();
            default -> throw new BizException(ResultCode.DATA_NOT_FOUND, "MCP tool is not registered");
        };
        return jsonUtil.toJson(result);
    }

    private PageResult<?> searchUsers(Map<String, Object> arguments) {
        UserQuery query = new UserQuery();
        String keyword = stringArg(arguments, "keyword");
        if (keyword != null) {
            query.setUsername(keyword);
        }
        query.setPageNum(intArg(arguments, "pageNum", 1, 1, Integer.MAX_VALUE));
        query.setPageSize(intArg(arguments, "pageSize", 10, 1, 50));
        Page<SysUser> page = userService.selectUserPage(query);
        return PageResult.of(page, userConvert::toVO);
    }

    private List<SysDept> deptTree(Map<String, Object> arguments) {
        SysDept query = new SysDept();
        query.setDeptName(stringArg(arguments, "deptName"));
        query.setStatus(optionalIntArg(arguments, "status"));
        return deptService.selectDeptTree(query);
    }

    private List<SysRole> roleList() {
        return roleService.selectAllRoles();
    }

    private static LoginUser requireAuthenticatedUser() {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "MCP tool call requires authenticated user context");
        }
        return loginUser;
    }

    private static void requirePermission(LoginUser loginUser, String permission) {
        if (!loginUser.hasPermission(permission)) {
            throw new BizException(ResultCode.ACCESS_DENIED, "MCP tool permission denied");
        }
    }

    private static void requireOAuthScope(String scope) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication == null ? null : authentication.getDetails();
        if (!(details instanceof McpAuthenticationDetails mcpDetails) || !mcpDetails.hasScope(scope)) {
            throw new BizException(ResultCode.ACCESS_DENIED, "MCP OAuth scope denied");
        }
    }

    private static String stringArg(Map<String, Object> arguments, String name) {
        if (arguments == null) {
            return null;
        }
        Object value = arguments.get(name);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static Integer optionalIntArg(Map<String, Object> arguments, String name) {
        if (arguments == null || !arguments.containsKey(name) || arguments.get(name) == null) {
            return null;
        }
        return intValue(arguments.get(name), name);
    }

    private static int intArg(Map<String, Object> arguments, String name, int defaultValue, int min, int max) {
        if (arguments == null || !arguments.containsKey(name) || arguments.get(name) == null) {
            return defaultValue;
        }
        int value = intValue(arguments.get(name), name);
        return Math.max(min, Math.min(max, value));
    }

    private static int intValue(Object value, String name) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.PARAM_INVALID, "invalid MCP tool argument: " + name);
        }
    }
}
