package com.alpha.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysRole;
import com.alpha.system.dto.request.RoleQuery;
import com.alpha.system.mapper.SysRoleDeptMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysRoleMenuMapper;
import com.alpha.system.service.ISysRoleService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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

    @Override
    public Page<SysRole> selectRolePage(RoleQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public List<SysRole> selectRoleList(RoleQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        return list(wrapper);
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
    public List<SysRole> selectRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public Set<Long> selectRoleIdsByUserId(Long userId) {
        return roleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertRole(SysRole role, List<Long> menuIds, List<Long> deptIds) {
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
        if (role.getDataScope() != null && role.getDataScope() == 4 && CollUtil.isNotEmpty(deptIds)) {
            roleDeptMapper.batchInsert(role.getId(), deptIds);
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole role, List<Long> menuIds, List<Long> deptIds) {
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

        // 更新角色
        updateById(role);

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
            if (role.getDataScope() != null && role.getDataScope() == 4 && CollUtil.isNotEmpty(deptIds)) {
                roleDeptMapper.batchInsert(role.getId(), deptIds);
            }
        }

        return true;
    }

    @Override
    public boolean updateStatus(Long roleId, Integer status) {
        checkRoleAllowed(roleId);

        SysRole role = new SysRole();
        role.setId(roleId);
        role.setStatus(status);
        return updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDataScope(Long roleId, Integer dataScope, List<Long> deptIds) {
        checkRoleAllowed(roleId);

        SysRole role = new SysRole();
        role.setId(roleId);
        role.setDataScope(dataScope);
        updateById(role);

        // 更新角色部门关联
        roleDeptMapper.deleteByRoleId(roleId);
        if (dataScope == 4 && CollUtil.isNotEmpty(deptIds)) {
            roleDeptMapper.batchInsert(roleId, deptIds);
        }

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

            // 删除角色菜单关联
            roleMenuMapper.deleteByRoleId(roleId);
            // 删除角色部门关联
            roleDeptMapper.deleteByRoleId(roleId);
        }

        return removeByIds(roleIds);
    }

    @Override
    public boolean checkRoleNameUnique(String roleName, Long excludeId) {
        return roleMapper.checkRoleNameExists(roleName, excludeId != null ? excludeId : 0L) == 0;
    }

    @Override
    public boolean checkRoleKeyUnique(String roleKey, Long excludeId) {
        return roleMapper.checkRoleKeyExists(roleKey, excludeId != null ? excludeId : 0L) == 0;
    }

    @Override
    public boolean isRoleUsed(Long roleId) {
        return roleMapper.countUserByRoleId(roleId) > 0;
    }

    /**
     * 检查是否允许操作角色
     */
    private void checkRoleAllowed(Long roleId) {
        if (roleId == 1L) {
            throw new BizException("不允许操作超级管理员角色");
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