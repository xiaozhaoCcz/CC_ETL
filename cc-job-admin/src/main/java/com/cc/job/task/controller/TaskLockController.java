package com.cc.job.task.controller;

import com.cc.job.task.service.TaskLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.task.model.form.TaskLockForm;
import com.cc.job.task.model.query.TaskLockQuery;
import com.cc.job.task.model.vo.TaskLockVO;
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
 * task_lock前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Tag(name = "task_lock接口")
@RestController
@RequestMapping("/api/v1/taskLocks")
@RequiredArgsConstructor
public class TaskLockController  {

    private final TaskLockService taskLockService;

    @Operation(summary = "task_lock分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('task:taskLock:query')")
    public PageResult<TaskLockVO> getTaskLockPage(TaskLockQuery queryParams ) {
        IPage<TaskLockVO> result = taskLockService.getTaskLockPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增task_lock")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('task:taskLock:add')")
    public Result<Void> saveTaskLock(@RequestBody @Valid TaskLockForm formData ) {
        boolean result = taskLockService.saveTaskLock(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_lock表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('task:taskLock:edit')")
    public Result<TaskLockForm> getTaskLockForm(
        @Parameter(description = "task_lockID") @PathVariable Long id
    ) {
        TaskLockForm formData = taskLockService.getTaskLockFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_lock")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('task:taskLock:edit')")
    public Result<Void> updateTaskLock(
            @Parameter(description = "task_lockID") @PathVariable Long id,
            @RequestBody @Validated TaskLockForm formData
    ) {
        boolean result = taskLockService.updateTaskLock(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_lock")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('task:taskLock:delete')")
    public Result<Void> deleteTaskLocks(
        @Parameter(description = "task_lockID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = taskLockService.deleteTaskLocks(ids);
        return Result.judge(result);
    }
}
