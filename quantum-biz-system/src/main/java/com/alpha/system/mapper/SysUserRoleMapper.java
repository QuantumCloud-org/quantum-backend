package com.alpha.system.mapper;

import com.alpha.system.domain.SysUserRole;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 删除用户的所有角色关联
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除角色的所有用户关联
     */
    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入用户角色关联
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="roleIds" item="roleId" separator=",">
                (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 根据角色ID查询关联的用户ID集合
     */
    @Select("SELECT DISTINCT user_id FROM sys_user_role WHERE role_id = #{roleId}")
    Set<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询角色ID集合
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    Set<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID集合查询用户角色关联
     */
    @Select("""
            <script>
            SELECT user_id, role_id FROM sys_user_role
            WHERE user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
            </foreach>
            </script>
            """)
    List<SysUserRole> selectByUserIds(@Param("userIds") List<Long> userIds);
}
