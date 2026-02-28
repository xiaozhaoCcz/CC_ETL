package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.service.JobResourcePermissionService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobResourcePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源级权限管理：给用户授权某分区/某任务
 */
@Tag(name = "资源权限管理")
@RestController
@RequestMapping("/api/v1/resourcePermissions")
public class JobResourcePermissionController {

    private final JobResourcePermissionService jobResourcePermissionService;

    public JobResourcePermissionController(JobResourcePermissionService jobResourcePermissionService) {
        this.jobResourcePermissionService = jobResourcePermissionService;
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "按用户查询资源授权列表")
    @GetMapping("/list")
    public Result<List<JobResourcePermission>> list(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "资源类型：PART/JOB_INFO/JOB_NODE，不传则全部") @RequestParam(required = false) String resourceType
    ) {
        List<JobResourcePermission> list = jobResourcePermissionService.listByUserId(userId, resourceType);
        return Result.success(list);
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "授予资源权限")
    @PostMapping("/grant")
    public Result<Void> grant(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.parseLong(String.valueOf(body.get("userId"))) : null;
        String resourceType = (String) body.get("resourceType");
        Long resourceId = body.get("resourceId") != null ? Long.parseLong(String.valueOf(body.get("resourceId"))) : null;
        String permissionType = (String) body.get("permissionType");
        jobResourcePermissionService.grant(userId, resourceType, resourceId, permissionType);
        return Result.success();
    }

    @RequirePermission(PermissionConstants.PERMISSION_MANAGE)
    @Operation(summary = "撤销资源权限")
    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        jobResourcePermissionService.revoke(id);
        return Result.success();
    }
}
