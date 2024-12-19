package com.cc.job.task.controller;

import com.cc.job.task.service.JobLogService;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.job.common.result.PageResult;
import com.cc.job.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

/**
 * task_log前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Tag(name = "task_log接口")
@RestController
@RequestMapping("/api/v1/taskLogs")
@RequiredArgsConstructor
public class JobLogController {

    private final JobLogService taskLogService;

    @Operation(summary = "task_log分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('task:taskLog:query')")
    public PageResult<JobLogVO> getTaskLogPage(JobLogQuery queryParams ) {
        IPage<JobLogVO> result = taskLogService.getTaskLogPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "删除task_log")
    @DeleteMapping
    //@PreAuthorize("@ss.hasPerm('task:taskLog:delete')")
    public Result<Void> deleteTaskLogs(
            JobLogQuery queryParams
    ) {
        boolean result = taskLogService.deleteTaskLogs(queryParams);
        return Result.judge(result);
    }

    @Operation(summary = "查看log日志")
    @GetMapping("/logDetailCat")
    public Result<ReturnT<LogResult>> getLogDetailCat(@RequestParam("logId") Long logId, int fromLineNum){
        ReturnT<LogResult> result = taskLogService.getLogDetailCat(logId,fromLineNum);
        return Result.success(result);
    }
}
