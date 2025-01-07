package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLogglue;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.admin.task.service.JobInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;
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
 * task_info前端控制层
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Tag(name = "task_info接口")
@RestController
@RequestMapping("/api/v1/jobInfos")
@RequiredArgsConstructor
public class JobInfoController {

    private final JobInfoService jobInfoService;

    @Operation(summary = "task_info分页列表")
    @GetMapping("/page")
    public PageResult<JobInfoVO> getTaskInfoPage(JobInfoQuery queryParams) {
        IPage<JobInfoVO> result = jobInfoService.getTaskInfoPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "task_info分页列表")
    @GetMapping("/list")
    public Result<List<JobInfo>> getTaskInfoList(Integer jobType) {
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        if(jobType!=null){
            wrapper.eq(JobInfo::getJobType, jobType);
        }else{
            wrapper.in(JobInfo::getJobType,0,2);
        }
        wrapper.eq(JobInfo::getIsNode,"N");
        List<JobInfo> list = jobInfoService.list(wrapper);
        return Result.success(list);
    }

    @Operation(summary = "新增task_info")
    @PostMapping
    public Result<Void> saveTaskInfo(@RequestBody @Valid JobInfoForm formData) {
        boolean result = jobInfoService.saveTaskInfo(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取task_info表单数据")
    @GetMapping("/{id}/form")
    public Result<JobInfoForm> getTaskInfoForm(
            @Parameter(description = "task_infoID") @PathVariable Long id
    ) {
        JobInfoForm formData = jobInfoService.getTaskInfoFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_info")
    @PutMapping(value = "/{id}")
    public Result<Void> updateTaskInfo(
            @Parameter(description = "task_infoID") @PathVariable Long id,
            @RequestBody @Validated JobInfoForm formData
    ) {
        boolean result = jobInfoService.updateTaskInfo(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_info")
    @DeleteMapping("/{ids}")
    public Result<Void> deleteTaskInfos(
            @Parameter(description = "task_infoID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = jobInfoService.deleteTaskInfos(ids);
        return Result.judge(result);
    }

    @Operation(summary = "执行任务一次")
    @PostMapping("/trigger")
    public Result<Void> triggerJob(@RequestBody JobInfoTriggerDto taskInfoTriggerDto) {
        boolean result = jobInfoService.triggerJob(taskInfoTriggerDto);
        return Result.judge(result);
    }

    @Operation(summary = "启动")
    @GetMapping("/startTask/{id}")
    public Result<Void> startTask(@PathVariable Long id) {
        boolean result = jobInfoService.startTask(id);
        return Result.judge(result);
    }

    @Operation(summary = "停止")
    @GetMapping("/stopTask/{id}")
    public Result<Void> stopTask(@PathVariable Long id) {
        boolean result = jobInfoService.stopTask(id);
        return Result.judge(result);
    }

    @Operation(summary = "下一次的运行时间")
    @GetMapping("/nextTriggerTime")
    public Result<List<String>> nextTriggerTime(String scheduleType, String scheduleConf){
      List<String> list = jobInfoService.nextTriggerTime(scheduleType, scheduleConf);
      return Result.success(list);
    }


    @Operation(summary = "保存任务运行集")
    @PostMapping("saveTaskSet")
    public Result<Void>  saveTaskSet(@RequestBody @Valid JobInfoForm formData){
        // 实现任务运行集的保存
        boolean result = jobInfoService.saveTaskSet(formData);
        return Result.judge(result);
    }

    @Operation(summary = "修改任务运行集")
    @PutMapping("updateTaskSet/{id}")
    public Result<Void>  updateTaskSet(@Parameter(description = "task_infoID") @PathVariable Long id,
                                       @RequestBody @Validated JobInfoForm formData){
        // 实现任务运行集的保存
        boolean result = jobInfoService.updateTaskSet(id,formData);
        return Result.judge(result);
    }

    @Operation(summary = "停止任务集")
    @GetMapping("/stopTaskSet/{id}/{randomId}")
    public Result<Void> stopTaskSet(@PathVariable Long id,@PathVariable String randomId) {
        boolean result = jobInfoService.stopTaskSet(id,randomId);
        return Result.judge(result);
    }

    @Operation(summary = "保存GlueSource")
    @PostMapping("saveGlueSource")
    public Result<Void>  saveGlueSource(@RequestBody @Valid JobGlueForm formData){
        boolean result = jobInfoService.saveGlueSource(formData);
        return Result.judge(result);
    }


    @GetMapping("getGlueList/{id}")
    public Result<List<JobLogglue>> getGlueList(@PathVariable Long id){
        List<JobLogglue> list =  jobInfoService.getGlueList(id);
        return Result.success(list);
    }
}
