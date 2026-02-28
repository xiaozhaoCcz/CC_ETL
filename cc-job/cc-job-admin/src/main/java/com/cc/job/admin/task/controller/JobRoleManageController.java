package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.exception.ForbiddenException;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.mapper.JobUserRoleMapper;
import com.cc.job.xo.model.entity.JobRole;
import com.cc.job.xo.model.entity.JobUserRole;
import com.cc.job.admin.task.service.JobRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理（权限管理页用，仅 permission:manage 可访问）
 * 超级管理员角色（id=1）不可删除
 */
@Tag(name = "角色管理-权限管理")
@RestController
@RequestMapping("/api/v1/roles")
public class JobRoleManageController {

    private final JobRoleService jobRoleService;
    private final JobUserRoleMapper jobUserRoleMapper;

    public JobRoleManageController(JobRoleService jobRoleService, JobUserRoleMapper jobUserRoleMapper) {
        this.jobRoleService = jobRoleService;
        this.jobUserRoleMapper = jobUserRoleMapper;
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<JobRole>> listRoles() {
        List<JobRole> list = jobRoleService.list(new LambdaQueryWrapper<JobRole>().eq(JobRole::getIsDeleted, 0));
        return Result.success(list);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "删除角色（超级管理员角色不可删除）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        if (PermissionConstants.SUPER_ADMIN_ROLE_ID == id) {
            throw new ForbiddenException("超级管理员角色不可删除");
        }
        jobRoleService.removeById(id);
        return Result.success();
    }
}
