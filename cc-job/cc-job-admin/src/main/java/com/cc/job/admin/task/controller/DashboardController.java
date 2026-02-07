package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobLogService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘/任务报表统计接口
 */
@Tag(name = "仪表盘接口")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final JobInfoService jobInfoService;
    private final JobLogService jobLogService;

    public DashboardController(JobInfoService jobInfoService, JobLogService jobLogService) {
        this.jobInfoService = jobInfoService;
        this.jobLogService = jobLogService;
    }

    @Operation(summary = "获取任务报表统计")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTaskGroupCount(jobInfoService.countTaskGroups());
        vo.setJobCount(jobInfoService.countJobs());
        vo.setLogSuccessCount(jobLogService.countLogSuccess());
        vo.setLogFailCount(jobLogService.countLogFail());
        vo.setLogRunningCount(jobLogService.countLogRunning());
        return Result.success(vo);
    }
}
