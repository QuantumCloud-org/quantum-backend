package com.alpha.system.convert;

import com.alpha.system.dto.request.RoleCreateRequest;
import com.alpha.system.dto.request.RoleUpdateRequest;
import com.alpha.system.dto.response.RoleVO;
import com.alpha.system.domain.SysRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色转换器
 */
@Component
public class RoleConvert {

    /**
     * 创建请求 -> 实体
     */
    public SysRole toEntity(RoleCreateRequest request) {
        if (request == null) {
            return null;
        }
        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setOrderNum(request.getOrderNum());
        role.setDataScope(request.getDataScope());
        role.setStatus(request.getStatus());
        role.setRemark(request.getRemark());
        return role;
    }

    /**
     * 更新请求 -> 实体
     */
    public SysRole toEntity(RoleUpdateRequest request) {
        if (request == null) {
            return null;
        }
        SysRole role = new SysRole();
        role.setId(request.getId());
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setOrderNum(request.getOrderNum());
        role.setDataScope(request.getDataScope());
        role.setStatus(request.getStatus());
        role.setRemark(request.getRemark());
        return role;
    }

    /**
     * 实体 -> VO
     */
    public RoleVO toVO(SysRole role) {
        if (role == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleKey(role.getRoleKey());
        vo.setOrderNum(role.getOrderNum());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        vo.setCreateBy(role.getCreateBy());
        vo.setUpdateTime(role.getUpdateTime());
        vo.setUpdateBy(role.getUpdateBy());
        vo.setDeleted(role.getDeleted());
        return vo;
    }

    /**
     * 实体列表 -> VO列表
     */
    public List<RoleVO> toVOList(List<SysRole> roles) {
        return roles.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }
}