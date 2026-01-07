package com.alpha.system.controller;

import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.convert.DeptConvert;
import com.alpha.system.dto.request.DeptCreateRequest;
import com.alpha.system.dto.request.DeptQuery;
import com.alpha.system.dto.request.DeptUpdateRequest;
import com.alpha.system.dto.response.DeptVO;
import com.alpha.system.dto.response.TreeSelectVO;
import com.alpha.system.domain.SysDept;
import com.alpha.system.service.ISysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理 Controller
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final ISysDeptService deptService;
    private final DeptConvert deptConvert;

    @Operation(summary = "查询部门列表")
    @SystemLog(title = "部门管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dept:list")
    @GetMapping("/list")
    public Result<List<DeptVO>> list(DeptQuery query) {
        SysDept deptQuery = new SysDept();
        deptQuery.setDeptName(query.getDeptName());
        deptQuery.setStatus(query.getStatus());
        return Result.ok(deptConvert.toVOList(deptService.selectDeptTree(deptQuery)));
    }

    @Operation(summary = "查询部门下拉树")
    @SystemLog(title = "部门管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dept:treeselect")
    @GetMapping("/treeselect")
    public Result<List<TreeSelectVO>> treeSelect(DeptQuery query) {
        SysDept deptQuery = new SysDept();
        deptQuery.setDeptName(query.getDeptName());
        deptQuery.setStatus(query.getStatus());
        List<SysDept> depts = deptService.selectDeptTree(deptQuery);
        return Result.ok(deptConvert.toTreeSelectList(depts));
    }

    @Operation(summary = "查询部门详情")
    @SystemLog(title = "部门管理", businessType = BusinessType.SELECT)
    @RequiresPermission("system:dept:query")
    @GetMapping("/{deptId}")
    public Result<DeptVO> getInfo(@PathVariable Long deptId) {
        return Result.ok(deptConvert.toVO(deptService.selectDeptById(deptId)));
    }

    @Operation(summary = "新增部门")
    @SystemLog(title = "部门管理", businessType = BusinessType.INSERT)
    @RequiresPermission("system:dept:add")
    @PostMapping
    public Result<Long> add(@Validated @RequestBody DeptCreateRequest request) {
        SysDept dept = deptConvert.toEntity(request);
        return Result.ok(deptService.insertDept(dept));
    }

    @Operation(summary = "修改部门")
    @SystemLog(title = "部门管理", businessType = BusinessType.UPDATE)
    @RequiresPermission("system:dept:edit")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody DeptUpdateRequest request) {
        SysDept dept = deptConvert.toEntity(request);
        deptService.updateDept(dept);
        return Result.ok();
    }

    @Operation(summary = "删除部门")
    @SystemLog(title = "部门管理", businessType = BusinessType.DELETE)
    @RequiresPermission("system:dept:remove")
    @DeleteMapping("/{deptId}")
    public Result<Void> remove(@PathVariable Long deptId) {
        deptService.deleteDeptById(deptId);
        return Result.ok();
    }
}