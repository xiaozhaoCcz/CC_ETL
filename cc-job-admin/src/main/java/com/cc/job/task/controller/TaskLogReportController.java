package com.cc.job.task.controller;

import com.cc.job.task.service.TaskLogReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.task.model.form.TaskLogReportForm;
import com.cc.job.task.model.query.TaskLogReportQuery;
import com.cc.job.task.model.vo.TaskLogReportVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.job.common.result.PageResult;
import com.cc.job.common.result.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * task_log_report前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Tag(name = "task_log_report接口")
@RestController
@RequestMapping("/api/v1/taskLogReports")
@RequiredArgsConstructor
public class TaskLogReportController  {

    private final TaskLogReportService taskLogReportService;

    @Operation(summary = "task_log_report分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('task:taskLogReport:query')")
    public PageResult<TaskLogReportVO> getTaskLogReportPage(TaskLogReportQuery queryParams ) {
        IPage<TaskLogReportVO> result = taskLogReportService.getTaskLogReportPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增task_log_report")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('task:taskLogReport:add')")
    public Result<Void> saveTaskLogReport(@RequestBody @Valid TaskLogReportForm formData ) {
        boolean result = taskLogReportService.saveTaskLogReport(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_log_report表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('task:taskLogReport:edit')")
    public Result<TaskLogReportForm> getTaskLogReportForm(
        @Parameter(description = "task_log_reportID") @PathVariable Long id
    ) {
        TaskLogReportForm formData = taskLogReportService.getTaskLogReportFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_log_report")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('task:taskLogReport:edit')")
    public Result<Void> updateTaskLogReport(
            @Parameter(description = "task_log_reportID") @PathVariable Long id,
            @RequestBody @Validated TaskLogReportForm formData
    ) {
        boolean result = taskLogReportService.updateTaskLogReport(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_log_report")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('task:taskLogReport:delete')")
    public Result<Void> deleteTaskLogReports(
        @Parameter(description = "task_log_reportID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = taskLogReportService.deleteTaskLogReports(ids);
        return Result.judge(result);
    }
}
