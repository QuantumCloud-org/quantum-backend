package com.alpha.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.system.domain.SysRole;
import com.alpha.system.domain.SysUserRole;
import com.alpha.system.dto.request.RoleQuery;
import com.alpha.system.mapper.SysRoleDeptMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysRoleMenuMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.UserCacheRefreshEvent;
import com.alpha.system.security.RoleDelegationPolicy;
import com.alpha.system.service.ISysRoleService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alpha.system.domain.table.SysRoleTableDef.SYS_ROLE;

/**
 * 角色服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RoleDelegationPolicy roleDelegation;

    @Override
    public Page<SysRole> selectRolePage(RoleQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public List<SysRole> selectAllRoles() {
        return roleMapper.selectAllRoles();
    }

    @Override
    public SysRole selectRoleById(Long roleId) {
        return getById(roleId);
    }

    @Override
    public Set<Long> selectRoleIdsByUserId(Long userId) {
        return roleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public Map<Long, Set<Long>> selectRoleIdsByUserIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRoleMapper.selectByUserIds(normalizedUserIds).stream()
                .collect(Collectors.groupingBy(
                        SysUserRole::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toCollection(LinkedHashSet::new))
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertRole(SysRole role, List<Long> menuIds, List<Long> deptIds) {
        validateDataScope(role.getDataScope());
        roleDelegation.checkDefinition(role, menuIds, deptIds);
        // 检查名称唯一性
        if (!checkRoleNameUnique(role.getRoleName(), null)) {
            throw new BizException("角色名称已存在");
        }
        // 检查标识唯一性
        if (!checkRoleKeyUnique(role.getRoleKey(), null)) {
            throw new BizException("角色标识已存在");
        }

        // 保存角色
        save(role);

        // 保存角色菜单关联
        if (CollUtil.isNotEmpty(menuIds)) {
            roleMenuMapper.batchInsert(role.getId(), menuIds);
        }

        // 保存角色部门关联（自定义数据权限）
        if (DataScopeType.fromCode(role.getDataScope()) == DataScopeType.CUSTOM && CollUtil.isNotEmpty(deptIds)) {
            roleDeptMapper.batchInsert(role.getId(), deptIds);
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole role, List<Long> menuIds, List<Long> deptIds) {
        validateDataScope(role.getDataScope());
        // 检查是否为管理员角色
        checkRoleAllowed(role.getId());

        // 检查名称唯一性
        if (!checkRoleNameUnique(role.getRoleName(), role.getId())) {
            throw new BizException("角色名称已存在");
        }
        // 检查标识唯一性
        if (!checkRoleKeyUnique(role.getRoleKey(), role.getId())) {
            throw new BizException("角色标识已存在");
        }

        SysRole oldRole = getById(role.getId());
        if (oldRole == null) {
            throw new BizException("角色不存在");
        }

        roleDelegation.checkDefinition(oldRole, null, null);
        roleDelegation.checkDefinition(role, menuIds, deptIds);
        oldRole.setRoleName(role.getRoleName());
        oldRole.setRoleKey(role.getRoleKey());
        oldRole.setOrderNum(role.getOrderNum());
        oldRole.setDataScope(role.getDataScope());
        oldRole.setStatus(role.getStatus());
        oldRole.setRemark(role.getRemark());
        oldRole.setVersion(role.getVersion());

        boolean updated = updateById(oldRole);
        if (!updated) {
            throw new BizException(ResultCode.DATA_CONFLICT, "角色信息已变更，请刷新后重试");
        }

        // 更新角色菜单关联
        if (menuIds != null) {
            roleMenuMapper.deleteByRoleId(role.getId());
            if (CollUtil.isNotEmpty(menuIds)) {
                roleMenuMapper.batchInsert(role.getId(), menuIds);
            }
        }

        // 更新角色部门关联
        if (deptIds != null) {
            roleDeptMapper.deleteByRoleId(role.getId());
            if (DataScopeType.fromCode(oldRole.getDataScope()) == DataScopeType.CUSTOM && CollUtil.isNotEmpty(deptIds)) {
                roleDeptMapper.batchInsert(role.getId(), deptIds);
            }
        }

        // 角色变更后刷新关联用户缓存
        refreshAssignedUsers(role.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long roleId, Integer status) {
        if (!CommonConstants.STATUS_DISABLE.equals(status) && !CommonConstants.STATUS_NORMAL.equals(status)) {
            throw new BizException("状态值无效，仅支持0(禁用)和1(正常)");
        }
        checkRoleAllowed(roleId);

        SysRole oldRole = getById(roleId);
        if (oldRole == null) {
            throw new BizException("角色不存在");
        }
        roleDelegation.checkDefinition(oldRole, null, null);
        oldRole.setStatus(status);
        boolean result = updateById(oldRole);
        if (!result) {
            throw new BizException("角色状态更新失败，请刷新后重试");
        }
        refreshAssignedUsers(roleId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDataScope(Long roleId, Integer dataScope, List<Long> deptIds) {
        validateDataScope(dataScope);
        checkRoleAllowed(roleId);

        SysRole oldRole = getById(roleId);
        if (oldRole == null) {
            throw new BizException("角色不存在");
        }
        roleDelegation.checkDefinition(oldRole, null, null);
        oldRole.setDataScope(dataScope);
        roleDelegation.checkDefinition(oldRole, null, deptIds == null ? List.of() : deptIds);
        boolean updated = updateById(oldRole);
        if (!updated) {
            throw new BizException("数据权限更新失败，请刷新后重试");
        }

        // 更新角色部门关联
        roleDeptMapper.deleteByRoleId(roleId);
        if (DataScopeType.fromCode(dataScope) == DataScopeType.CUSTOM && CollUtil.isNotEmpty(deptIds)) {
            roleDeptMapper.batchInsert(roleId, deptIds);
        }

        // 数据权限变更后刷新关联用户缓存
        refreshAssignedUsers(roleId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleByIds(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            checkRoleAllowed(roleId);

            if (isRoleUsed(roleId)) {
                SysRole role = getById(roleId);
                throw new BizException("角色 " + role.getRoleName() + " 已分配给用户，不能删除");
            }

            var role = getById(roleId);
            if (role != null) {
                roleDelegation.checkDefinition(role, null, null);
            }

            // 删除角色菜单关联
            roleMenuMapper.deleteByRoleId(roleId);
            // 删除角色部门关联
            roleDeptMapper.deleteByRoleId(roleId);
        }

        return removeByIds(roleIds);
    }

    @Override
    public boolean checkRoleNameUnique(String roleName, Long excludeId) {
        return roleMapper.checkRoleNameExists(roleName, excludeId) == 0;
    }

    @Override
    public boolean checkRoleKeyUnique(String roleKey, Long excludeId) {
        return roleMapper.checkRoleKeyExists(roleKey, excludeId) == 0;
    }

    @Override
    public boolean isRoleUsed(Long roleId) {
        return roleMapper.countUserByRoleId(roleId) > 0;
    }

    /**
     * 检查是否允许操作角色
     */
    private void refreshAssignedUsers(Long roleId) {
        Set<Long> affected = userRoleMapper.selectUserIdsByRoleIds(Set.of(roleId));
        if (!affected.isEmpty()) {
            eventPublisher.publishEvent(new UserCacheRefreshEvent(affected));
        }
    }

    private void checkRoleAllowed(Long roleId) {
        if (CommonConstants.SUPER_ADMIN_ID.equals(roleId)) {
            throw new BizException("不允许操作超级管理员角色");
        }
    }

    private void validateDataScope(Integer dataScope) {
        if (dataScope != null && (dataScope < 1 || dataScope > 5)) {
            throw new BizException("数据权限范围无效，仅支持1-5");
        }
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(RoleQuery query) {
        QueryWrapper wrapper = QueryWrapper.create();

        if (StrUtil.isNotBlank(query.getRoleName())) {
            wrapper.and(SYS_ROLE.ROLE_NAME.like(query.getRoleName()));
        }
        if (StrUtil.isNotBlank(query.getRoleKey())) {
            wrapper.and(SYS_ROLE.ROLE_KEY.like(query.getRoleKey()));
        }
        if (query.getStatus() != null) {
            wrapper.and(SYS_ROLE.STATUS.eq(query.getStatus()));
        }
        if (query.getBeginTime() != null) {
            wrapper.and(SYS_ROLE.CREATE_TIME.ge(query.getBeginTime()));
        }
        if (query.getEndTime() != null) {
            wrapper.and(SYS_ROLE.CREATE_TIME.le(query.getEndTime()));
        }

        wrapper.orderBy(SYS_ROLE.ORDER_NUM.asc());
        return wrapper;
    }
}
