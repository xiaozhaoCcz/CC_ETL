package com.cc.job.admin.task.controller;

import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.admin.task.service.JobGroupService;
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
@RequestMapping("/api/v1/jobGroups")
@RequiredArgsConstructor
public class JobGroupController {

    private final JobGroupService jobGroupService;


    @Operation(summary = "task_group分页列表")
    @GetMapping("/{id}")
    public Result<JobGroup> getJobGroup(@Parameter(description = "执行器ID") @PathVariable("id") Long id) {
        JobGroup jobGroup= jobGroupService.getById(id);
        return Result.success(jobGroup);
    }


    @Operation(summary = "task_group分页列表")
    @GetMapping("/page")
    public PageResult<JobGroupVO> getJobGroupPage(JobGroupQuery queryParams ) {
        IPage<JobGroupVO> result = jobGroupService.getJobGroupPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增task_group")
    @PostMapping
    public Result<Void> saveJobGroup(@RequestBody @Valid JobGroupForm formData ) {
        boolean result = jobGroupService.saveJobGroup(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_group表单数据")
    @GetMapping("/{id}/form")
    public Result<JobGroupForm> getTaskGroupForm(
        @Parameter(description = "task_groupID") @PathVariable Long id
    ) {
        JobGroupForm formData = jobGroupService.getJobGroupFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_group")
    @PutMapping(value = "/{id}")
    public Result<Void> updateJobGroup(
            @Parameter(description = "task_groupID") @PathVariable Long id,
            @RequestBody @Validated JobGroupForm formData
    ) {
        boolean result = jobGroupService.updateJobGroup(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_group")
    @DeleteMapping("/{ids}")
    public Result<Void> deleteJobGroups(
        @Parameter(description = "task_groupID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = jobGroupService.deleteJobGroups(ids);
        return Result.judge(result);
    }


    @Operation(summary = "查看地址")
    @GetMapping("/findAddressList/{id}")
    public Result<List<String>> findAddressList(@PathVariable Long id){
        List<String> list = jobGroupService.findAddressList(id);
        return Result.success(list);
    }

    @Operation(summary = "获取所有taskGroup")
    @GetMapping("/getAllJobGroupList")
    public Result<List<JobGroup>> getAllTaskGroupList(){
        List<JobGroup> list = jobGroupService.list();
        return Result.success(list);
    }
}
