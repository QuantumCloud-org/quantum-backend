package com.alpha.system.mapper;

import com.alpha.system.domain.SysRole;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 角色 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询角色标识集合
     */
    Set<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询角色ID集合
     */
    Set<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询所有正常状态的角色
     */
    @Select("SELECT * FROM sys_role WHERE status = 1 AND deleted = 0 ORDER BY order_num")
    List<SysRole> selectAllRoles();

    /**
     * 检查角色名称是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_role WHERE role_name = #{roleName} AND deleted = 0 AND id != #{excludeId}")
    int checkRoleNameExists(@Param("roleName") String roleName, @Param("excludeId") Long excludeId);

    /**
     * 检查角色标识是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_role WHERE role_key = #{roleKey} AND deleted = 0 AND id != #{excludeId}")
    int checkRoleKeyExists(@Param("roleKey") String roleKey, @Param("excludeId") Long excludeId);

    /**
     * 查询角色关联的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countUserByRoleId(@Param("roleId") Long roleId);
}
