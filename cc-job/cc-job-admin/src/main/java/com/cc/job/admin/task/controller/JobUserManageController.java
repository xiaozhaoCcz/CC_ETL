package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.service.JobUserService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobUser;
import com.cc.job.xo.model.vo.UserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
}
