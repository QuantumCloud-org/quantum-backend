package com.alpha.system.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.file.util.ExcelUtil;
import com.alpha.framework.entity.Result;
import com.alpha.framework.exception.BizException;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.orm.entity.PageResult;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.ResetPasswordRequest;
import com.alpha.system.dto.request.UserCreateRequest;
import com.alpha.system.dto.request.UserImportRequest;
import com.alpha.system.dto.request.UserQuery;
import com.alpha.system.dto.request.UserStatusUpdateRequest;
import com.alpha.system.dto.request.UserUpdateRequest;
import com.alpha.system.dto.response.UserEditVO;
import com.alpha.system.dto.response.UserExportVO;
import com.alpha.system.dto.response.UserVO;
import com.alpha.system.service.ISysRoleService;
import com.alpha.system.service.ISysUserService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理 Controller
 */
@Slf4j
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final UserConvert userConvert;

    @Operation(summary = "分页查询用户")
    @SystemLog(title = "用户管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:user:list")
    @GetMapping("/list")
    public Result<PageResult<UserVO>> list(@Validated UserQuery query) {
        Page<SysUser> pageResult = userService.selectUserPage(query);
        PageResult<UserVO> voPage = PageResult.of(pageResult, userConvert::toVO);
        return Result.ok(voPage);
    }

    @Operation(summary = "查询用户详情")
    @SystemLog(title = "用户管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:user:query")
    @GetMapping("/{userId}")
    public Result<UserEditVO> getInfo(@PathVariable Long userId) {
        SysUser user = userService.selectUserById(userId);
        UserEditVO vo = userConvert.toEditVO(user);
        if (vo != null && vo.getDeptId() != null) {
            vo.setDeptName(userService.selectDeptNameById(vo.getDeptId()));
        }
        return Result.ok(vo);
    }

    @Operation(summary = "新增用户")
    @SystemLog(title = "用户管理", businessType = BusinessType.INSERT)
    @RequiresPermission("system:user:add")
    @PostMapping
    public Result<Long> add(@Validated @RequestBody UserCreateRequest request) {
        SysUser user = userConvert.toEntity(request);
        return Result.ok(userService.insertUser(user, request.getRoleIds()));
    }

    @Operation(summary = "修改用户")
    @SystemLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:user:edit")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody UserUpdateRequest request) {
        SysUser user = userConvert.toEntity(request);
        userService.updateUser(user, request.getRoleIds());
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @SystemLog(title = "用户管理", businessType = BusinessType.DELETE)
    @RequiresPermission("system:user:remove")
    @DeleteMapping("/{userIds}")
    public Result<Void> remove(@PathVariable List<Long> userIds) {
        userService.deleteUserByIds(userIds);
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @SystemLog(title = "用户管理", businessType = BusinessType.IMPORT)
    @RequiresPermission("system:user:resetPwd")
    @PutMapping("/resetPwd")
    public Result<Void> resetPwd(@Validated @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getUserId(), request.getVersion(), request.getPassword());
        return Result.ok();
    }

    @Operation(summary = "修改状态")
    @SystemLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:user:edit")
    @PutMapping("/changeStatus")
    public Result<Void> changeStatus(@Validated @RequestBody UserStatusUpdateRequest request) {
        userService.updateStatus(request.getUserId(), request.getVersion(), request.getStatus());
        return Result.ok();
    }

    @Operation(summary = "查询用户角色")
    @SystemLog(title = "用户管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:user:query")
    @GetMapping("/{userId}/roles")
    public Result<Set<Long>> getUserRoles(@PathVariable Long userId) {
        userService.selectUserById(userId);
        return Result.ok(roleService.selectRoleIdsByUserId(userId));
    }

    @Operation(summary = "导入用户")
    @SystemLog(title = "用户管理", businessType = BusinessType.IMPORT)
    @RequiresPermission("system:user:import")
    @PostMapping("/import")
    public Result<String> importData(MultipartFile file, boolean updateSupport) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要导入的Excel文件");
        }

        try {
            List<UserExportVO> voList = ExcelUtil.read(file.getInputStream(), UserExportVO.class);

            if (CollUtil.isEmpty(voList)) {
                return Result.fail("Excel文件中没有数据");
            }

            List<UserImportRequest> userList = new ArrayList<>();
            for (UserExportVO vo : voList) {
                UserImportRequest user = new UserImportRequest();
                user.setUsername(vo.getUsername());
                user.setNickname(vo.getNickname());
                user.setEmail(vo.getEmail());
                user.setPhone(vo.getPhone());
                user.setSex(convertSexLabel(vo.getSexLabel()));
                user.setStatus(convertStatusLabel(vo.getStatusLabel()));
                user.setDeptId(vo.getDeptId());
                user.setRoleIds(parseRoleIds(vo.getRoleIds()));
                userList.add(user);
            }

            String msg = userService.importUsers(userList, updateSupport);
            return Result.ok(msg);
        } catch (BizException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("导入用户失败", e);
            return Result.fail("导入用户失败，请检查文件格式");
        }
    }

    @Operation(summary = "导出用户")
    @SystemLog(title = "用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermission("system:user:export")
    @PostMapping("/export")
    public void export(UserQuery query, HttpServletResponse response) {
        List<SysUser> userList = userService.selectUserList(query);
        List<UserExportVO> voList = userConvert.toExportVOList(userList);
        Map<Long, Set<Long>> roleIdsByUserId = roleService.selectRoleIdsByUserIds(userList.stream()
                .map(SysUser::getId)
                .toList());
        for (int i = 0; i < userList.size(); i++) {
            voList.get(i).setRoleIds(formatRoleIds(roleIdsByUserId.get(userList.get(i).getId())));
        }

        com.alpha.file.util.ExcelUtil.export(response, "用户列表", UserExportVO.class, voList);
    }

    private Integer convertSexLabel(String sexLabel) {
        return StrUtil.isBlank(sexLabel) ? null : userConvert.convertSex(sexLabel.trim());
    }

    private Integer convertStatusLabel(String statusLabel) {
        return StrUtil.isBlank(statusLabel) ? null : userConvert.convertStatus(statusLabel.trim());
    }

    private List<Long> parseRoleIds(String roleIds) {
        if (StrUtil.isBlank(roleIds)) {
            return null;
        }
        try {
            return Arrays.stream(roleIds.split("[,，;；\\s]+"))
                    .filter(StrUtil::isNotBlank)
                    .map(String::trim)
                    .map(Long::valueOf)
                    .distinct()
                    .toList();
        } catch (NumberFormatException e) {
            throw new BizException("角色ID列表格式不正确");
        }
    }

    private String formatRoleIds(Set<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return "";
        }
        return roleIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
