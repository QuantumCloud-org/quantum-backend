package com.alpha.system.dto.response;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户导出视图对象
 */
@Data
public class UserExportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @ExcelProperty("用户名")
    private String username;

    /**
     * 部门名称
     */
    @ExcelProperty("部门名称")
    private String deptName;

    /**
     * 部门ID（导入必填）
     */
    @ExcelProperty("部门ID")
    private Long deptId;

    /**
     * 角色ID列表（导入新增必填, 多个用英文逗号分隔）
     */
    @ExcelProperty("角色ID列表")
    private String roleIds;

    /**
     * 昵称
     */
    @ExcelProperty("昵称")
    private String nickname;

    /**
     * 邮箱
     */
    @ExcelProperty("邮箱")
    private String email;

    /**
     * 手机号
     */
    @ExcelProperty("手机号")
    private String phone;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    @ExcelProperty("性别")
    private String sexLabel;

    /**
     * 状态（0-禁用 1-正常）
     */
    @ExcelProperty("状态")
    private String statusLabel;

    /**
     * 创建时间
     */
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
