package com.alpha.system.security;

import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.system.domain.SysMenu;
import com.alpha.system.domain.SysRole;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysMenuMapper;
import com.alpha.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RoleDelegationPolicy {

    private final SysRoleMapper roles;
    private final SysMenuMapper menus;
    private final SysDeptMapper departments;

    public void checkAssignment(List<Long> roleIds, Long targetDepartment) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        LoginUser actor = actor();
        if (actor.isAdmin()) {
            return;
        }
        var ids = new HashSet<>(roleIds);
        if (ids.contains(null)) {
            throw denied();
        }
        var assignedRoles = roles.selectListByIds(ids);
        if (assignedRoles.size() != ids.size()) {
            throw denied();
        }
        checkPermissions(actor, roles.selectDelegatedPermissions(ids));
        for (SysRole role : assignedRoles) {
            checkRole(actor, role);
            switch (DataScopeType.fromCode(role.getDataScope())) {
                case DEPT -> checkDepartment(actor, targetDepartment);
                case DEPT_AND_CHILD -> {
                    checkDepartment(actor, targetDepartment);
                    departments.selectChildDeptIds(targetDepartment).forEach(id -> checkDepartment(actor, id));
                }
                case CUSTOM -> departments.selectDeptIdsByRoleId(role.getId()).forEach(id -> checkDepartment(actor, id));
                default -> { }
            }
        }
    }

    public void checkDefinition(SysRole role, List<Long> menuIds, List<Long> departmentIds) {
        LoginUser actor = actor();
        if (actor.isAdmin()) {
            return;
        }
        checkRole(actor, role);
        if (role.getId() != null) {
            checkPermissions(actor, roles.selectDelegatedPermissions(List.of(role.getId())));
        }
        if (menuIds != null && !menuIds.isEmpty()) {
            var ids = new HashSet<>(menuIds);
            if (ids.contains(null)) {
                throw denied();
            }
            var selected = menus.selectListByIds(ids);
            if (selected.size() != ids.size()) {
                throw denied();
            }
            checkPermissions(actor, selected.stream().map(SysMenu::getPerms).filter(Objects::nonNull).toList());
        }
        switch (DataScopeType.fromCode(role.getDataScope())) {
            case DEPT, DEPT_AND_CHILD -> {
                // A role definition can apply to users in any department.
                if (DataScopeType.fromCode(actor.getDataScope()) != DataScopeType.ALL) {
                    throw denied();
                }
            }
            case CUSTOM -> {
                Collection<Long> ids = departmentIds;
                if (ids == null && role.getId() != null) {
                    ids = departments.selectDeptIdsByRoleId(role.getId());
                }
                if (ids != null) {
                    ids.forEach(id -> checkDepartment(actor, id));
                }
            }
            default -> { }
        }
    }

    public static boolean canAccessDepartment(LoginUser actor, Long department) {
        return switch (DataScopeType.fromCode(actor.getDataScope())) {
            case ALL -> true;
            case DEPT -> department != null && department.equals(actor.getDeptId());
            case CUSTOM, DEPT_AND_CHILD -> department != null && actor.getDeptIds() != null
                    && actor.getDeptIds().contains(department);
            default -> false;
        };
    }

    private static LoginUser actor() {
        LoginUser user = UserContext.getUser();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    private static void checkRole(LoginUser actor, SysRole role) {
        if (!actor.isAdmin() && (CommonConstants.SUPER_ADMIN_ID.equals(role.getId())
                || CommonConstants.SUPER_ADMIN_ROLE.equals(role.getRoleKey())
                || DataScopeType.fromCode(role.getDataScope()) == DataScopeType.ALL)) {
            throw denied();
        }
    }

    private static void checkPermissions(LoginUser actor, Collection<String> permissions) {
        if (permissions.stream().filter(permission -> !permission.isBlank()).anyMatch(permission -> !actor.hasPermission(permission))) {
            throw denied();
        }
    }

    private static void checkDepartment(LoginUser actor, Long department) {
        if (!canAccessDepartment(actor, department)) {
            throw denied();
        }
    }

    private static BizException denied() {
        return new BizException(ResultCode.ACCESS_DENIED, "无权委派该角色、权限或部门");
    }
}
