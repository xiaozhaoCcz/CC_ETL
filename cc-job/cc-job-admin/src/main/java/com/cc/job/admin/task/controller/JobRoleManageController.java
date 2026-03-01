package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.exception.ForbiddenException;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.mapper.JobRolePermissionMapper;
import com.cc.job.xo.mapper.JobUserRoleMapper;
import com.cc.job.xo.model.entity.JobRole;
import com.cc.job.xo.model.entity.JobRolePermission;
import com.cc.job.xo.model.entity.JobUserRole;
import com.cc.job.admin.task.service.JobRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final JobRolePermissionMapper jobRolePermissionMapper;

    public JobRoleManageController(JobRoleService jobRoleService, JobUserRoleMapper jobUserRoleMapper,
                                   JobRolePermissionMapper jobRolePermissionMapper) {
        this.jobRoleService = jobRoleService;
        this.jobUserRoleMapper = jobUserRoleMapper;
        this.jobRolePermissionMapper = jobRolePermissionMapper;
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<JobRole>> listRoles() {
        List<JobRole> list = jobRoleService.list(new LambdaQueryWrapper<JobRole>().eq(JobRole::getIsDeleted, 0));
        return Result.success(list);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "新增角色")
    @PostMapping
    public Result<JobRole> createRole(@RequestBody Map<String, String> body) {
        String roleName = body != null ? body.get("roleName") : null;
        String roleCode = body != null ? body.get("roleCode") : null;
        String description = body != null ? body.get("description") : null;
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        roleCode = roleCode.trim();
        long count = jobRoleService.count(new LambdaQueryWrapper<JobRole>().eq(JobRole::getRoleCode, roleCode).eq(JobRole::getIsDeleted, 0));
        if (count > 0) {
            throw new ForbiddenException("角色编码已存在");
        }
        JobRole role = new JobRole();
        role.setRoleName(roleName != null && !roleName.isBlank() ? roleName.trim() : roleCode);
        role.setRoleCode(roleCode);
        role.setDescription(description != null ? description.trim() : null);
        jobRoleService.save(role);
        return Result.success(role);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "编辑角色（超级管理员角色仅允许改描述）")
    @PutMapping("/{id}")
    public Result<Void> updateRole(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        JobRole role = jobRoleService.getById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (PermissionConstants.SUPER_ADMIN_ROLE_ID == id) {
            String description = body != null ? body.get("description") : null;
            role.setDescription(description != null ? description.trim() : null);
        } else {
            if (body != null && body.containsKey("roleName")) {
                String v = body.get("roleName");
                role.setRoleName(v != null ? v.trim() : null);
            }
            if (body != null && body.containsKey("roleCode")) {
                String v = body.get("roleCode");
                if (v != null && !v.isBlank()) {
                    v = v.trim();
                    long c = jobRoleService.count(new LambdaQueryWrapper<JobRole>().eq(JobRole::getRoleCode, v).eq(JobRole::getIsDeleted, 0).ne(JobRole::getId, id));
                    if (c > 0) throw new ForbiddenException("角色编码已存在");
                    role.setRoleCode(v);
                }
            }
            if (body != null && body.containsKey("description")) {
                role.setDescription(body.get("description") != null ? body.get("description").trim() : null);
            }
        }
        jobRoleService.updateById(role);
        return Result.success();
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

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "获取角色拥有的权限ID列表")
    @GetMapping("/{roleId}/permissions")
    public Result<List<Long>> getRolePermissions(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<JobRolePermission> list = jobRolePermissionMapper.selectList(
                new LambdaQueryWrapper<JobRolePermission>().eq(JobRolePermission::getRoleId, roleId));
        List<Long> permissionIds = list.stream().map(JobRolePermission::getPermissionId).distinct().toList();
        return Result.success(permissionIds);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "更新角色权限")
    @PutMapping("/{roleId}/permissions")
    public Result<Void> updateRolePermissions(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds) {
        jobRolePermissionMapper.delete(new LambdaQueryWrapper<JobRolePermission>().eq(JobRolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Long permissionId : permissionIds) {
                JobRolePermission rp = new JobRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rp.setCreateTime(now);
                jobRolePermissionMapper.insert(rp);
            }
        }
        return Result.success();
    }
}
