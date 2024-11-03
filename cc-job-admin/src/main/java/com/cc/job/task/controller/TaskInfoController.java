package com.cc.job.task.controller;

import com.cc.job.task.service.TaskInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.task.model.form.TaskInfoForm;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;
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
 * task_info前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Tag(name = "task_info接口")
@RestController
@RequestMapping("/api/v1/taskInfos")
@RequiredArgsConstructor
public class TaskInfoController  {

    private final TaskInfoService taskInfoService;

    @Operation(summary = "task_info分页列表")
    @GetMapping("/page")
    public PageResult<TaskInfoVO> getTaskInfoPage(TaskInfoQuery queryParams ) {
        IPage<TaskInfoVO> result = taskInfoService.getTaskInfoPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增task_info")
    @PostMapping
    public Result<Void> saveTaskInfo(@RequestBody @Valid TaskInfoForm formData ) {
        boolean result = taskInfoService.saveTaskInfo(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_info表单数据")
    @GetMapping("/{id}/form")
    public Result<TaskInfoForm> getTaskInfoForm(
        @Parameter(description = "task_infoID") @PathVariable Long id
    ) {
        TaskInfoForm formData = taskInfoService.getTaskInfoFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_info")
    @PutMapping(value = "/{id}")
    public Result<Void> updateTaskInfo(
            @Parameter(description = "task_infoID") @PathVariable Long id,
            @RequestBody @Validated TaskInfoForm formData
    ) {
        boolean result = taskInfoService.updateTaskInfo(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_info")
    @DeleteMapping("/{ids}")
    public Result<Void> deleteTaskInfos(
        @Parameter(description = "task_infoID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = taskInfoService.deleteTaskInfos(ids);
        return Result.judge(result);
    }
}
