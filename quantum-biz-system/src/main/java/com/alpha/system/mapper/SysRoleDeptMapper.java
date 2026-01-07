package com.alpha.system.mapper;

import com.alpha.system.domain.SysRoleDept;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色部门关联 Mapper
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /**
     * 删除角色的所有部门关联
     */
    @Delete("DELETE FROM sys_role_dept WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色部门关联
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_dept (role_id, dept_id) VALUES
            <foreach collection="deptIds" item="deptId" separator=",">
                (#{roleId}, #{deptId})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("roleId") Long roleId, @Param("deptIds") List<Long> deptIds);
}
