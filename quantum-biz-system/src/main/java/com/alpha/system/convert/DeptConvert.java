package com.alpha.system.convert;

import com.alpha.system.dto.request.DeptCreateRequest;
import com.alpha.system.dto.request.DeptUpdateRequest;
import com.alpha.system.dto.response.DeptVO;
import com.alpha.system.dto.response.TreeSelectVO;
import com.alpha.system.domain.SysDept;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门转换器
 */
@Component
public class DeptConvert {

    /**
     * 创建请求 -> 实体
     */
    public SysDept toEntity(DeptCreateRequest request) {
        if (request == null) {
            return null;
        }
        SysDept dept = new SysDept();
        dept.setParentId(request.getParentId());
        dept.setDeptName(request.getDeptName());
        dept.setOrderNum(request.getOrderNum());
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setEmail(request.getEmail());
        dept.setStatus(request.getStatus());
        return dept;
    }

    /**
     * 更新请求 -> 实体
     */
    public SysDept toEntity(DeptUpdateRequest request) {
        if (request == null) {
            return null;
        }
        SysDept dept = new SysDept();
        dept.setId(request.getId());
        dept.setParentId(request.getParentId());
        dept.setDeptName(request.getDeptName());
        dept.setOrderNum(request.getOrderNum());
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setEmail(request.getEmail());
        dept.setStatus(request.getStatus());
        return dept;
    }

    /**
     * 实体 -> VO
     */
    public DeptVO toVO(SysDept dept) {
        if (dept == null) {
            return null;
        }
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setDeptName(dept.getDeptName());
        vo.setParentId(dept.getParentId());
        vo.setAncestors(dept.getAncestors());
        vo.setOrderNum(dept.getOrderNum());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setEmail(dept.getEmail());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        vo.setCreateBy(dept.getCreateBy());
        vo.setUpdateTime(dept.getUpdateTime());
        vo.setUpdateBy(dept.getUpdateBy());
        vo.setDeleted(dept.getDeleted());
        return vo;
    }

    /**
     * 实体列表 -> VO列表
     */
    public List<DeptVO> toVOList(List<SysDept> depts) {
        if (depts == null) {
            return Collections.emptyList();
        }
        return depts.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 实体 -> TreeSelectVO
     */
    public TreeSelectVO toTreeSelect(SysDept dept) {
        if (dept == null) {
            return null;
        }
        return TreeSelectVO.fromDept(dept);
    }

    /**
     * 实体列表 -> TreeSelectVO列表
     */
    public List<TreeSelectVO> toTreeSelectList(List<SysDept> depts) {
        if (depts == null) {
            return Collections.emptyList();
        }
        return depts.stream()
                .map(this::toTreeSelect)
                .collect(Collectors.toList());
    }
}
