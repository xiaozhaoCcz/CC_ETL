package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.service.JobNodeResultService;
import com.cc.job.xo.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 节点执行结果控制器
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
@Tag(name = "节点执行结果管理")
@RestController
@RequestMapping("/api/v1/jobNodeResults")
public class JobNodeResultController {

    private final JobNodeResultService jobNodeResultService;

    public JobNodeResultController(JobNodeResultService jobNodeResultService) {
        this.jobNodeResultService = jobNodeResultService;
    }

    @Operation(summary = "批量保存节点执行结果")
    @PostMapping("/batchSave")
    public Result<Boolean> batchSaveNodeResults(@RequestBody Map<String, Object> params) {
        try {
            Long taskGroupId = Long.valueOf(params.get("taskGroupId").toString());
            String executionBatchId = params.get("executionBatchId").toString();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) params.get("results");

            int savedCount = jobNodeResultService.batchSaveNodeResults(taskGroupId, executionBatchId, results);
            return Result.success(savedCount > 0);
        } catch (Exception e) {
            return Result.failed("保存节点结果失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据批次ID获取节点执行结果")
    @GetMapping("/byBatch")
    public Result<List<com.cc.job.xo.model.entity.JobNodeResult>> getNodeResultsByBatch(
            @Parameter(description = "任务组ID") @RequestParam("taskGroupId") Long taskGroupId,
            @Parameter(description = "执行批次ID") @RequestParam("executionBatchId") String executionBatchId) {
        try {
            List<com.cc.job.xo.model.entity.JobNodeResult> results = 
                    jobNodeResultService.getNodeResultsByBatch(taskGroupId, executionBatchId);
            return Result.success(results);
        } catch (Exception e) {
            return Result.failed("获取节点结果失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取最近一次执行的批次ID")
    @GetMapping("/latestBatchId")
    public Result<String> getLatestBatchId(
            @Parameter(description = "任务组ID") @RequestParam("taskGroupId") Long taskGroupId) {
        try {
            String batchId = jobNodeResultService.getLatestBatchId(taskGroupId);
            return Result.success(batchId);
        } catch (Exception e) {
            return Result.failed("获取最近一次批次ID失败: " + e.getMessage());
        }
    }
}

