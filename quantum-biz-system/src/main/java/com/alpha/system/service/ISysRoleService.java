package com.alpha.system.service;

import com.alpha.system.domain.SysRole;
import com.alpha.system.dto.request.RoleQuery;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 角色服务接口
 */
public interface ISysRoleService extends IService<SysRole> {

    /**
     * 分页查询角色
     */
    Page<SysRole> selectRolePage(RoleQuery query);

    /**
     * 查询角色列表
     */
    List<SysRole> selectRoleList(RoleQuery query);

    /**
     * 查询所有角色
     */
    List<SysRole> selectAllRoles();

    /**
     * 根据ID查询角色
     */
    SysRole selectRoleById(Long roleId);

    /**
     * 根据用户ID查询角色
     */
    List<SysRole> selectRolesByUserId(Long userId);

    /**
     * 根据用户ID查询角色ID集合
     */
    Set<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 新增角色
     */
    Long insertRole(SysRole role, List<Long> menuIds, List<Long> deptIds);

    /**
     * 修改角色
     */
    boolean updateRole(SysRole role, List<Long> menuIds, List<Long> deptIds);

    /**
     * 修改角色状态
     */
    boolean updateStatus(Long roleId, Integer status);

    /**
     * 修改数据权限
     */
    boolean updateDataScope(Long roleId, Integer dataScope, List<Long> deptIds);

    /**
     * 删除角色
     */
    boolean deleteRoleByIds(List<Long> roleIds);

    /**
     * 检查角色名称是否唯一
     */
    boolean checkRoleNameUnique(String roleName, Long excludeId);

    /**
     * 检查角色标识是否唯一
     */
    boolean checkRoleKeyUnique(String roleKey, Long excludeId);

    /**
     * 检查角色是否被用户使用
     */
    boolean isRoleUsed(Long roleId);
}