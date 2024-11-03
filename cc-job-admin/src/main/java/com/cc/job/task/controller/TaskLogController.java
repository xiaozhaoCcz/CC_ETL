package com.cc.job.task.controller;

import com.cc.job.task.service.TaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.task.model.form.TaskLogForm;
import com.cc.job.task.model.query.TaskLogQuery;
import com.cc.job.task.model.vo.TaskLogVO;
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
 * task_log前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Tag(name = "task_log接口")
@RestController
@RequestMapping("/api/v1/taskLogs")
@RequiredArgsConstructor
public class TaskLogController  {

    private final TaskLogService taskLogService;

    @Operation(summary = "task_log分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('task:taskLog:query')")
    public PageResult<TaskLogVO> getTaskLogPage(TaskLogQuery queryParams ) {
        IPage<TaskLogVO> result = taskLogService.getTaskLogPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增task_log")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('task:taskLog:add')")
    public Result<Void> saveTaskLog(@RequestBody @Valid TaskLogForm formData ) {
        boolean result = taskLogService.saveTaskLog(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_log表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('task:taskLog:edit')")
    public Result<TaskLogForm> getTaskLogForm(
        @Parameter(description = "task_logID") @PathVariable Long id
    ) {
        TaskLogForm formData = taskLogService.getTaskLogFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_log")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('task:taskLog:edit')")
    public Result<Void> updateTaskLog(
            @Parameter(description = "task_logID") @PathVariable Long id,
            @RequestBody @Validated TaskLogForm formData
    ) {
        boolean result = taskLogService.updateTaskLog(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_log")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('task:taskLog:delete')")
    public Result<Void> deleteTaskLogs(
        @Parameter(description = "task_logID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = taskLogService.deleteTaskLogs(ids);
        return Result.judge(result);
    }
}
