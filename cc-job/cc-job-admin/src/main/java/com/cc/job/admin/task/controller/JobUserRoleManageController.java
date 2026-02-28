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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户-角色绑定（权限管理页用）
 * 不可取消超级管理员（userId=1）的管理员角色（roleId=1）
 */
@Tag(name = "用户角色-权限管理")
@RestController
@RequestMapping("/api/v1/users")
public class JobUserRoleManageController {

    private final JobUserRoleMapper jobUserRoleMapper;
    private final JobRoleService jobRoleService;

    public JobUserRoleManageController(JobUserRoleMapper jobUserRoleMapper, JobRoleService jobRoleService) {
        this.jobUserRoleMapper = jobUserRoleMapper;
        this.jobRoleService = jobRoleService;
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "获取用户的角色ID列表")
    @GetMapping("/{userId}/roles")
    public Result<List<Long>> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<JobUserRole> list = jobUserRoleMapper.selectList(
                new LambdaQueryWrapper<JobUserRole>().eq(JobUserRole::getUserId, userId));
        List<Long> roleIds = list.stream().map(JobUserRole::getRoleId).collect(Collectors.toList());
        return Result.success(roleIds);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "为用户分配角色")
    @PostMapping("/{userId}/roles")
    public Result<Void> assignRole(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody java.util.Map<String, Object> body
    ) {
        Object rid = body.get("roleId");
        if (rid == null) {
            throw new IllegalArgumentException("roleId 不能为空");
        }
        Long roleId = rid instanceof Number ? ((Number) rid).longValue() : Long.parseLong(rid.toString());
        long c = jobUserRoleMapper.selectCount(new LambdaQueryWrapper<JobUserRole>()
                .eq(JobUserRole::getUserId, userId)
                .eq(JobUserRole::getRoleId, roleId));
        if (c > 0) {
            return Result.success();
        }
        JobUserRole ur = new JobUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setCreateTime(LocalDateTime.now());
        jobUserRoleMapper.insert(ur);
        return Result.success();
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "取消用户的某角色（超级管理员不可取消管理员角色）")
    @DeleteMapping("/{userId}/roles/{roleId}")
    public Result<Void> removeRole(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "角色ID") @PathVariable Long roleId
    ) {
        if (userId == PermissionConstants.SUPER_ADMIN_USER_ID && roleId == PermissionConstants.SUPER_ADMIN_ROLE_ID) {
            throw new ForbiddenException("超级管理员不可取消管理员角色");
        }
        jobUserRoleMapper.delete(new LambdaQueryWrapper<JobUserRole>()
                .eq(JobUserRole::getUserId, userId)
                .eq(JobUserRole::getRoleId, roleId));
        return Result.success();
    }
}
