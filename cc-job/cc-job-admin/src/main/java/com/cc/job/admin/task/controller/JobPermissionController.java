package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.mapper.JobPermissionMapper;
import com.cc.job.xo.model.entity.JobPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限列表（权限管理页用，用于角色权限配置）
 */
@Tag(name = "权限列表-权限管理")
@RestController
@RequestMapping("/api/v1/permissions")
public class JobPermissionController {

    private final JobPermissionMapper jobPermissionMapper;

    public JobPermissionController(JobPermissionMapper jobPermissionMapper) {
        this.jobPermissionMapper = jobPermissionMapper;
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "获取所有权限列表")
    @GetMapping
    public Result<List<JobPermission>> list() {
        List<JobPermission> list = jobPermissionMapper.selectList(
                new LambdaQueryWrapper<JobPermission>().eq(JobPermission::getIsDeleted, 0)
                        .orderByAsc(JobPermission::getId));
        return Result.success(list);
    }
}
