package com.alpha.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysDept;
import com.alpha.system.dto.response.TreeSelectVO;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.service.ISysDeptService;
import com.alpha.system.support.TreeBuilder;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.alpha.system.domain.table.SysDeptTableDef.SYS_DEPT;

/**
 * 部门服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<SysDept> selectDeptTree(SysDept query) {
        QueryWrapper wrapper = QueryWrapper.create();

        if (StrUtil.isNotBlank(query.getDeptName())) {
            wrapper.and(SYS_DEPT.DEPT_NAME.like(query.getDeptName()));
        }
        if (query.getStatus() != null) {
            wrapper.and(SYS_DEPT.STATUS.eq(query.getStatus()));
        }

        wrapper.orderBy(SYS_DEPT.PARENT_ID.asc(), SYS_DEPT.ORDER_NUM.asc());

        List<SysDept> depts = list(wrapper);
        return TreeBuilder.buildTree(depts);  // ✅ 使用 TreeBuilder
    }

    @Override
    public SysDept selectDeptById(Long deptId) {
        return getById(deptId);
    }

    @Override
    public Set<Long> selectChildDeptIds(Long deptId) {
        return deptMapper.selectChildDeptIds(deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertDept(SysDept dept) {
        // 检查名称唯一性
        if (!checkDeptNameUnique(dept.getDeptName(), dept.getParentId(), null)) {
            throw new BizException("部门名称已存在");
        }

        // 设置祖级列表
        if (dept.getParentId() != 0) {
            SysDept parent = getById(dept.getParentId());
            if (parent == null) {
                throw new BizException("父部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + dept.getParentId());
        } else {
            dept.setAncestors("0");
        }

        save(dept);
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDept(SysDept dept) {
        // 检查名称唯一性
        if (!checkDeptNameUnique(dept.getDeptName(), dept.getParentId(), dept.getId())) {
            throw new BizException("部门名称已存在");
        }

        // 不能设置自己为父部门
        if (java.util.Objects.equals(dept.getId(), dept.getParentId())) {
            throw new BizException("父部门不能是自己");
        }

        SysDept oldDept = getById(dept.getId());
        if (oldDept == null) {
            throw new BizException("部门不存在");
        }

        Long originalParentId = oldDept.getParentId();
        String originalAncestors = oldDept.getAncestors();

        // 如果父部门变更，更新祖级列表
        if (!java.util.Objects.equals(originalParentId, dept.getParentId())) {
            String newAncestors;
            if (dept.getParentId() == 0) {
                newAncestors = "0";
            } else {
                SysDept newParent = getById(dept.getParentId());
                if (newParent == null) {
                    throw new BizException("父部门不存在");
                }
                // 不能设置子部门为父部门
                if (newParent.getAncestors().contains(String.valueOf(dept.getId()))) {
                    throw new BizException("不能设置子部门为父部门");
                }
                newAncestors = newParent.getAncestors() + "," + dept.getParentId();
            }
            oldDept.setAncestors(newAncestors);

            // 更新子部门的祖级列表
            String oldAncestors = originalAncestors + "," + oldDept.getId();
            String newChildAncestors = newAncestors + "," + dept.getId();
            deptMapper.updateChildrenAncestors(oldAncestors, newChildAncestors);
        }
        oldDept.setParentId(dept.getParentId());
        oldDept.setDeptName(dept.getDeptName());
        oldDept.setOrderNum(dept.getOrderNum());
        oldDept.setLeader(dept.getLeader());
        oldDept.setPhone(dept.getPhone());
        oldDept.setEmail(dept.getEmail());
        oldDept.setStatus(dept.getStatus());

        boolean updated = updateById(oldDept);
        if (!updated) {
            throw new BizException("部门信息已变更，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeptById(Long deptId) {
        // 检查是否有子部门
        if (hasChildDept(deptId)) {
            throw new BizException("存在子部门，不能删除");
        }
        // 检查是否有用户
        if (hasUserInDept(deptId)) {
            throw new BizException("部门下存在用户，不能删除");
        }

        return removeById(deptId);
    }

    @Override
    public boolean checkDeptNameUnique(String deptName, Long parentId, Long excludeId) {
        return deptMapper.checkDeptNameExists(deptName, parentId, excludeId != null ? excludeId : 0L) == 0;
    }

    @Override
    public boolean hasChildDept(Long deptId) {
        return deptMapper.selectChildCount(deptId) > 0;
    }

    @Override
    public boolean hasUserInDept(Long deptId) {
        return deptMapper.selectUserCount(deptId) > 0;
    }

    /**
     * 转换为下拉树节点（递归）
     */
    private TreeSelectVO convertToTreeSelect(SysDept dept) {
        TreeSelectVO treeSelect = TreeSelectVO.fromDept(dept);
        if (CollUtil.isNotEmpty(dept.getChildren())) {
            List<TreeSelectVO> children = dept.getChildren().stream().map(this::convertToTreeSelect).toList();
            treeSelect.setChildren(children);
        }
        return treeSelect;
    }
}
