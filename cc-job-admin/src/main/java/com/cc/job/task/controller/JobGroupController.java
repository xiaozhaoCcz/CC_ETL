package com.cc.job.task.controller;

import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.task.service.JobGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.xo.model.form.JobGroupForm;
import com.cc.job.xo.model.query.JobGroupQuery;
import com.cc.job.xo.model.vo.JobGroupVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.common.result.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * task_group前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Tag(name = "task_group接口")
@RestController
@RequestMapping("/api/v1/taskGroups")
@RequiredArgsConstructor
public class JobGroupController {

    private final JobGroupService taskGroupService;

    @Operation(summary = "task_group分页列表")
    @GetMapping("/page")
    //@PreAuthorize("@ss.hasPerm('task:taskGroup:query')")
    public PageResult<JobGroupVO> getTaskGroupPage(JobGroupQuery queryParams ) {
        IPage<JobGroupVO> result = taskGroupService.getTaskGroupPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增task_group")
    @PostMapping
    //@PreAuthorize("@ss.hasPerm('task:taskGroup:add')")
    public Result<Void> saveTaskGroup(@RequestBody @Valid JobGroupForm formData ) {
        boolean result = taskGroupService.saveTaskGroup(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_group表单数据")
    @GetMapping("/{id}/form")
    //@PreAuthorize("@ss.hasPerm('task:taskGroup:edit')")
    public Result<JobGroupForm> getTaskGroupForm(
        @Parameter(description = "task_groupID") @PathVariable Long id
    ) {
        JobGroupForm formData = taskGroupService.getTaskGroupFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_group")
    @PutMapping(value = "/{id}")
    //@PreAuthorize("@ss.hasPerm('task:taskGroup:edit')")
    public Result<Void> updateTaskGroup(
            @Parameter(description = "task_groupID") @PathVariable Long id,
            @RequestBody @Validated JobGroupForm formData
    ) {
        boolean result = taskGroupService.updateTaskGroup(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_group")
    @DeleteMapping("/{ids}")
    //@PreAuthorize("@ss.hasPerm('task:taskGroup:delete')")
    public Result<Void> deleteTaskGroups(
        @Parameter(description = "task_groupID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = taskGroupService.deleteTaskGroups(ids);
        return Result.judge(result);
    }


    @Operation(summary = "查看地址")
    @GetMapping("/findAddressList/{id}")
    public Result<List<String>> findAddressList(@PathVariable Long id){
        List<String> list = taskGroupService.findAddressList(id);
        return Result.success(list);
    }

    @Operation(summary = "获取所有taskGroup")
    @GetMapping("/getAllTaskGroupList")
    public Result<List<JobGroup>> getAllTaskGroupList(){
        List<JobGroup> list = taskGroupService.list();
        return Result.success(list);
    }
}
