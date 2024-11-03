package com.cc.job.task.controller;

import com.cc.job.task.service.TaskRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.task.model.form.TaskRegistryForm;
import com.cc.job.task.model.query.TaskRegistryQuery;
import com.cc.job.task.model.vo.TaskRegistryVO;
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
 * 执行器前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Tag(name = "执行器接口")
@RestController
@RequestMapping("/api/v1/taskRegistrys")
@RequiredArgsConstructor
public class TaskRegistryController  {

    private final TaskRegistryService taskRegistryService;

    @Operation(summary = "执行器分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('task:taskRegistry:query')")
    public PageResult<TaskRegistryVO> getTaskRegistryPage(TaskRegistryQuery queryParams ) {
        IPage<TaskRegistryVO> result = taskRegistryService.getTaskRegistryPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增执行器")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('task:taskRegistry:add')")
    public Result<Void> saveTaskRegistry(@RequestBody @Valid TaskRegistryForm formData ) {
        boolean result = taskRegistryService.saveTaskRegistry(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取执行器表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('task:taskRegistry:edit')")
    public Result<TaskRegistryForm> getTaskRegistryForm(
        @Parameter(description = "执行器ID") @PathVariable Long id
    ) {
        TaskRegistryForm formData = taskRegistryService.getTaskRegistryFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改执行器")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('task:taskRegistry:edit')")
    public Result<Void> updateTaskRegistry(
            @Parameter(description = "执行器ID") @PathVariable Long id,
            @RequestBody @Validated TaskRegistryForm formData
    ) {
        boolean result = taskRegistryService.updateTaskRegistry(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除执行器")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('task:taskRegistry:delete')")
    public Result<Void> deleteTaskRegistrys(
        @Parameter(description = "执行器ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = taskRegistryService.deleteTaskRegistrys(ids);
        return Result.judge(result);
    }
}
