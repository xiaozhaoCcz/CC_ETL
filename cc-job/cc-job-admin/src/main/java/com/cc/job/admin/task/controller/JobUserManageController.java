package com.cc.job.admin.task.controller;

import com.cc.job.admin.exception.ForbiddenException;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.service.JobUserService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobUser;
import com.cc.job.xo.model.vo.UserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理（权限管理页用，仅 permission:manage 可访问）
 */
@Tag(name = "用户管理-权限管理")
@RestController
@RequestMapping("/api/v1/users")
public class JobUserManageController {

    private final JobUserService jobUserService;

    public JobUserManageController(JobUserService jobUserService) {
        this.jobUserService = jobUserService;
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "用户列表（不含密码）")
    @GetMapping
    public Result<List<UserListVO>> listUsers() {
        List<JobUser> list = jobUserService.list();
        List<UserListVO> voList = list.stream().map(u -> {
            UserListVO vo = new UserListVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "管理员创建用户")
    @PostMapping
    public Result<UserListVO> createUser(@RequestBody Map<String, String> body) {
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        JobUser user = jobUserService.createUserByAdmin(username.trim(), password);
        UserListVO vo = new UserListVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setCreateTime(user.getCreateTime());
        return Result.success(vo);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "管理员删除用户（超级管理员不可删除）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        if (PermissionConstants.SUPER_ADMIN_USER_ID == id) {
            throw new ForbiddenException("不能删除超级管理员用户");
        }
        jobUserService.deleteUserById(id);
        return Result.success();
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "管理员重置用户密码")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String password = body != null ? body.get("password") : null;
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        jobUserService.resetPassword(id, password);
        return Result.success();
    }
}
