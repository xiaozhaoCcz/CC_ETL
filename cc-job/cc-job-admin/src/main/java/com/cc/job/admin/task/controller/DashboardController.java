package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobLogService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 仪表盘/任务报表统计接口
 */
@Tag(name = "仪表盘接口")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JobInfoService jobInfoService;
    private final JobLogService jobLogService;

    public DashboardController(JobInfoService jobInfoService, JobLogService jobLogService) {
        this.jobInfoService = jobInfoService;
        this.jobLogService = jobLogService;
    }

    @Operation(summary = "获取任务报表统计，支持时间范围与任务组筛选")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> getStats(
            @RequestParam(required = false) String filterTimeStart,
            @RequestParam(required = false) String filterTimeEnd,
            @RequestParam(required = false) Long jobId) {
        LocalDateTime start = parseDateTime(filterTimeStart);
        LocalDateTime end = parseDateTime(filterTimeEnd);

        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTaskGroupCount(jobInfoService.countTaskGroups());
        vo.setJobCount(jobInfoService.countJobs());
        vo.setLogSuccessCount(jobLogService.countLogSuccess(start, end, jobId));
        vo.setLogFailCount(jobLogService.countLogFail(start, end, jobId));
        vo.setLogRunningCount(jobLogService.countLogRunning(start, end, jobId));
        return Result.success(vo);
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), ISO_LOCAL);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
