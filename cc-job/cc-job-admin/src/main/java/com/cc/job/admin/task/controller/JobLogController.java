package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.service.JobLogService;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.xo.model.dto.LogArchiveRequest;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * task_log前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Tag(name = "task_log接口")
@RestController
@RequestMapping("/api/v1/jobLogs")
public class JobLogController {

    private final JobLogService taskLogService;

    public JobLogController(JobLogService taskLogService) {
        this.taskLogService = taskLogService;
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "task_log分页列表")
    @GetMapping("/page")
    public PageResult<JobLogVO> getJobLogPage(JobLogQuery queryParams) {
        IPage<JobLogVO> result = taskLogService.getJobLogPage(queryParams);
        return PageResult.success(result);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "删除task_log")
    @DeleteMapping
    public Result<Void> deleteJobLogs(
            JobLogQuery queryParams
    ) {
        boolean result = taskLogService.deleteJobLogs(queryParams);
        return Result.judge(result);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "日志归档：删除早于指定天数的日志")
    @PostMapping("/archive")
    public Result<Integer> archiveLogs(@RequestBody LogArchiveRequest request) {
        int days = request.getOlderThanDays();
        if (days < 1) {
            return Result.failed("olderThanDays 至少为 1");
        }
        int deleted = taskLogService.archiveOlderThanDays(days);
        return Result.success(deleted);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "查看log日志")
    @GetMapping("/logDetailCat")
    public Result<ReturnT<LogResult>> getLogDetailCat(@RequestParam("logId") Long logId, int fromLineNum) {
        ReturnT<LogResult> result = taskLogService.getLogDetailCat(logId, fromLineNum);
        return Result.success(result);
    }

    @Operation(summary = "保存节点执行状态（供执行器调用）")
    @PostMapping("/saveNodeStatus")
    public Result<Void> saveNodeStatus(@RequestBody Map<String, Object> params) {
        try {
            Long taskGroupId = Long.valueOf(params.get("taskGroupId").toString());
            String executionBatchId = params.get("executionBatchId").toString();
            String nodeStatusJson = params.get("nodeStatus").toString();

            boolean success = taskLogService.saveNodeStatus(taskGroupId, executionBatchId, nodeStatusJson);
            return Result.judge(success);
        } catch (Exception e) {
            return Result.failed("保存节点状态失败: " + e.getMessage());
        }
    }
}
