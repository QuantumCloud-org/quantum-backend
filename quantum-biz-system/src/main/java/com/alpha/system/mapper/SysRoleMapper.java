package com.alpha.system.mapper;

import com.alpha.system.domain.SysRole;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
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
     * 根据用户ID查询角色ID集合
     */
    Set<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT DISTINCT m.perms FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            WHERE m.deleted = 0 AND m.perms IS NOT NULL AND m.perms != ''
            AND rm.role_id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">#{roleId}</foreach>
            </script>
            """)
    Set<String> selectDelegatedPermissions(@Param("roleIds") Collection<Long> roleIds);

    /**
     * 查询所有正常状态的角色
     */
    @Select("SELECT * FROM sys_role WHERE status = 1 AND deleted = 0 ORDER BY order_num")
    List<SysRole> selectAllRoles();

    /**
     * 检查角色名称是否存在
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_role WHERE role_name = #{roleName} AND deleted = 0
            <if test="excludeId != null">AND id != #{excludeId}</if>
            </script>
            """)
    int checkRoleNameExists(@Param("roleName") String roleName, @Param("excludeId") Long excludeId);

    /**
     * 检查角色标识是否存在
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_role WHERE role_key = #{roleKey} AND deleted = 0
            <if test="excludeId != null">AND id != #{excludeId}</if>
            </script>
            """)
    int checkRoleKeyExists(@Param("roleKey") String roleKey, @Param("excludeId") Long excludeId);

    /**
     * 查询角色关联的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countUserByRoleId(@Param("roleId") Long roleId);
}
