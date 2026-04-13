package com.alpha.system.service;

import com.alpha.system.domain.SysDept;
import com.alpha.system.dto.response.TreeSelectVO;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 部门服务接口
 */
public interface ISysDeptService extends IService<SysDept> {

    /**
     * 查询部门树
     */
    List<SysDept> selectDeptTree(SysDept query);

    /**
     * 根据ID查询部门
     */
    SysDept selectDeptById(Long deptId);

    /**
     * 查询子部门ID集合（包含自身）
     */
    Set<Long> selectChildDeptIds(Long deptId);

    /**
     * 新增部门
     */
    Long insertDept(SysDept dept);

    /**
     * 修改部门
     */
    boolean updateDept(SysDept dept);

    /**
     * 删除部门
     */
    boolean deleteDeptById(Long deptId);

    /**
     * 检查部门名称是否唯一
     */
    boolean checkDeptNameUnique(String deptName, Long parentId, Long excludeId);

    /**
     * 检查部门是否有子部门
     */
    boolean hasChildDept(Long deptId);

    /**
     * 检查部门是否有用户
     */
    boolean hasUserInDept(Long deptId);
}