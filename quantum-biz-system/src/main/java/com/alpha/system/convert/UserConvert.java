package com.alpha.system.convert;

import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.UserCreateRequest;
import com.alpha.system.dto.request.UserUpdateRequest;
import com.alpha.system.dto.response.UserEditVO;
import com.alpha.system.dto.response.UserExportVO;
import com.alpha.system.dto.response.UserVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户转换器
 */
@Component
public class UserConvert {

    /**
     * 创建请求 -> 实体
     */
    public SysUser toEntity(UserCreateRequest request) {
        if (request == null) {
            return null;
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setSex(request.getSex());
        user.setDeptId(request.getDeptId());
        user.setStatus(request.getStatus());
        user.setRemark(request.getRemark());
        return user;
    }

    /**
     * 更新请求 -> 实体
     */
    public SysUser toEntity(UserUpdateRequest request) {
        if (request == null) {
            return null;
        }
        SysUser user = new SysUser();
        user.setId(request.getId());
        user.setVersion(request.getVersion());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setSex(request.getSex());
        user.setDeptId(request.getDeptId());
        user.setStatus(request.getStatus());
        user.setRemark(request.getRemark());
        return user;
    }

    /**
     * 实体 -> VO
     */
    public UserVO toVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setVersion(user.getVersion());
        vo.setUsername(user.getUsername());
        // 敏感字段不返回
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setSex(user.getSex());
        vo.setDeptName(user.getDeptName());
        vo.setStatus(user.getStatus());
        vo.setLoginIp(user.getLoginIp());
        vo.setLoginLocation(user.getLoginLocation());
        vo.setLoginDate(user.getLoginDate());
        vo.setRemark(user.getRemark());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 实体 -> 编辑VO
     */
    public UserEditVO toEditVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserEditVO vo = new UserEditVO();
        vo.setId(user.getId());
        vo.setVersion(user.getVersion());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setSex(user.getSex());
        vo.setDeptId(user.getDeptId());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setLoginIp(user.getLoginIp());
        vo.setLoginLocation(user.getLoginLocation());
        vo.setLoginDate(user.getLoginDate());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 实体列表 -> VO列表
     */
    public List<UserVO> toVOList(List<SysUser> users) {
        return users.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 实体 -> 导出VO
     */
    public UserExportVO toExportVO(SysUser user) {
        if (user == null) return null;
        UserExportVO vo = new UserExportVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setDeptName(user.getDeptName());
        vo.setSexLabel(convertSex(user.getSex()));
        vo.setStatusLabel(convertStatus(user.getStatus()));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 实体列表 -> 导出VO列表
     */
    public List<UserExportVO> toExportVOList(List<SysUser> users) {
        return users.stream().map(this::toExportVO).collect(Collectors.toList());
    }

    private String convertSex(Integer sex) {
        if (sex == null) return "未知";
        return switch (sex) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    private String convertStatus(Integer status) {
        if (status == null) return "未知";
        return status == 1 ? "正常" : "禁用";
    }

    /**
     * 性别文字转数字（导入用）
     */
    public Integer convertSex(String sexLabel) {
        if (sexLabel == null) return 0;
        return switch (sexLabel) {
            case "男" -> 1;
            case "女" -> 2;
            default -> 0;
        };
    }

    /**
     * 状态文字转数字（导入用）
     */
    public Integer convertStatus(String statusLabel) {
        if (statusLabel == null) return 0;
        return "正常".equals(statusLabel) ? 1 : 0;
    }


}
