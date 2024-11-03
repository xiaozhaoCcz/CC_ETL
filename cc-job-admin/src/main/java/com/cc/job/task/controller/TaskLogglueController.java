package com.cc.job.task.controller;

import com.cc.job.task.service.TaskLogglueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.task.model.form.TaskLogglueForm;
import com.cc.job.task.model.query.TaskLogglueQuery;
import com.cc.job.task.model.vo.TaskLogglueVO;
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
 * task_logglue前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Tag(name = "task_logglue接口")
@RestController
@RequestMapping("/api/v1/taskLogglues")
@RequiredArgsConstructor
public class TaskLogglueController  {

    private final TaskLogglueService taskLogglueService;

    @Operation(summary = "task_logglue分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('task:taskLogglue:query')")
    public PageResult<TaskLogglueVO> getTaskLoggluePage(TaskLogglueQuery queryParams ) {
        IPage<TaskLogglueVO> result = taskLogglueService.getTaskLoggluePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增task_logglue")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('task:taskLogglue:add')")
    public Result<Void> saveTaskLogglue(@RequestBody @Valid TaskLogglueForm formData ) {
        boolean result = taskLogglueService.saveTaskLogglue(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_logglue表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('task:taskLogglue:edit')")
    public Result<TaskLogglueForm> getTaskLogglueForm(
        @Parameter(description = "task_logglueID") @PathVariable Long id
    ) {
        TaskLogglueForm formData = taskLogglueService.getTaskLogglueFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_logglue")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('task:taskLogglue:edit')")
    public Result<Void> updateTaskLogglue(
            @Parameter(description = "task_logglueID") @PathVariable Long id,
            @RequestBody @Validated TaskLogglueForm formData
    ) {
        boolean result = taskLogglueService.updateTaskLogglue(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_logglue")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('task:taskLogglue:delete')")
    public Result<Void> deleteTaskLogglues(
        @Parameter(description = "task_logglueID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = taskLogglueService.deleteTaskLogglues(ids);
        return Result.judge(result);
    }
}
