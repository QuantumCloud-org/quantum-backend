package com.alpha.system.mapper;

import com.alpha.system.domain.SysRoleMenu;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

/**
 * 角色菜单关联 Mapper
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 删除角色的所有菜单关联
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除菜单的所有角色关联
     */
    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * 批量删除菜单的所有角色关联
     */
    @Delete("""
            <script>
            DELETE FROM sys_role_menu
            WHERE menu_id IN
            <foreach collection="menuIds" item="menuId" open="(" separator="," close=")">
                #{menuId}
            </foreach>
            </script>
            """)
    int deleteByMenuIds(@Param("menuIds") List<Long> menuIds);

    /**
     * 批量插入角色菜单关联
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="menuIds" item="menuId" separator=",">
                (#{roleId}, #{menuId})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /**
     * 根据菜单ID查询关联的角色ID集合
     */
    @Select("SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id = #{menuId}")
    Set<Long> selectRoleIdsByMenuId(@Param("menuId") Long menuId);

    /**
     * 根据菜单ID集合查询关联的角色ID集合
     */
    @Select("""
            <script>
            SELECT DISTINCT role_id
            FROM sys_role_menu
            WHERE menu_id IN
            <foreach collection="menuIds" item="menuId" open="(" separator="," close=")">
                #{menuId}
            </foreach>
            </script>
            """)
    Set<Long> selectRoleIdsByMenuIds(@Param("menuIds") List<Long> menuIds);
}
