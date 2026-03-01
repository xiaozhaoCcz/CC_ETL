package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.controller.dto.SaveVersionRequest;
import com.cc.job.admin.task.service.JobGroupSnapshotService;
import com.cc.job.admin.task.utils.JwtUtil;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobGroupSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 任务组快照/版本接口：版本列表、保存为版本、按ID获取快照内容（用于回滚）
 */
@Tag(name = "任务组快照与版本")
@RestController
@RequestMapping("/api/v1/jobGroupSnapshots")
public class JobGroupSnapshotController {

    private final JobGroupSnapshotService jobGroupSnapshotService;

    public JobGroupSnapshotController(JobGroupSnapshotService jobGroupSnapshotService) {
        this.jobGroupSnapshotService = jobGroupSnapshotService;
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "列出任务组的手动版本列表")
    @GetMapping("/versions")
    public Result<List<JobGroupSnapshot>> listVersions(
            @Parameter(description = "任务组ID") @RequestParam Long jobId,
            @Parameter(description = "最多返回条数") @RequestParam(defaultValue = "50") int limit) {
        List<JobGroupSnapshot> list = jobGroupSnapshotService.listVersions(jobId, limit);
        return Result.success(list);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_EDIT)
    @Operation(summary = "保存当前画布为版本")
    @PostMapping("/saveVersion")
    public Result<Long> saveVersion(@RequestBody SaveVersionRequest req, HttpServletRequest request) {
        if (req.getJobId() == null || req.getNodesJson() == null || req.getEdgesJson() == null) {
            return Result.failed("jobId、nodesJson、edgesJson 不能为空");
        }
        String userId = String.valueOf(getUserIdFromRequest(request));
        Long id = jobGroupSnapshotService.saveAsVersion(
                req.getJobId(),
                req.getVersionName(),
                req.getNodesJson(),
                req.getEdgesJson(),
                userId);
        return Result.success(id);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "根据快照ID获取内容（用于回滚到该版本）")
    @GetMapping("/{id}")
    public Result<JobGroupSnapshot> getById(@Parameter(description = "快照ID") @PathVariable Long id) {
        JobGroupSnapshot snapshot = jobGroupSnapshotService.getById(id);
        if (snapshot == null) return Result.failed("快照不存在");
        return Result.success(snapshot);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        if (request == null) return null;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return JwtUtil.getUserIdFromToken(token);
        }
        return null;
    }
}
