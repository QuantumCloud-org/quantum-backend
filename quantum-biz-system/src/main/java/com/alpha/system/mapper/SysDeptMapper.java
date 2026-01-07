package com.alpha.system.mapper;

import com.alpha.system.domain.SysDept;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 部门 Mapper
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询所有子部门ID（包含自身）
     */
    Set<Long> selectChildDeptIds(@Param("deptId") Long deptId);

    /**
     * 根据角色ID查询关联的部门ID
     */
    Set<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询部门及其所有子部门
     */
    List<SysDept> selectDeptAndChildren(@Param("deptId") Long deptId);

    /**
     * 检查部门名称是否存在
     */
    int checkDeptNameExists(@Param("deptName") String deptName, @Param("parentId") Long parentId, @Param("excludeId") Long excludeId);

    /**
     * 查询子部门数量
     */
    int selectChildCount(@Param("deptId") Long deptId);

    /**
     * 查询部门下用户数量
     */
    int selectUserCount(@Param("deptId") Long deptId);

    /**
     * 更新子部门的祖级列表
     */
    int updateChildrenAncestors(@Param("oldAncestors") String oldAncestors, @Param("newAncestors") String newAncestors);

    /**
     * 根据ID查询部门名称
     */
    String selectNameById(@Param("deptId") Long deptId);
}
