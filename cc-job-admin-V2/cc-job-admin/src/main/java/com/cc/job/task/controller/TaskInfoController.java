package com.cc.job.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.entity.TaskLogglue;
import com.cc.job.task.model.form.TaskGlueForm;
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

import java.util.List;

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
public class TaskInfoController {

    private final TaskInfoService taskInfoService;

    @Operation(summary = "task_info分页列表")
    @GetMapping("/page")
    public PageResult<TaskInfoVO> getTaskInfoPage(TaskInfoQuery queryParams) {
        IPage<TaskInfoVO> result = taskInfoService.getTaskInfoPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "task_info分页列表")
    @GetMapping("/list")
    public Result<List<TaskInfo>> getTaskInfoList(Integer jobType) {
        LambdaQueryWrapper<TaskInfo> wrapper = new LambdaQueryWrapper<>();
        if(jobType!=null){
            wrapper.eq(TaskInfo::getJobType, jobType);
        }else{
            wrapper.in(TaskInfo::getJobType,0,2);
        }
        List<TaskInfo> list = taskInfoService.list(wrapper);
        return Result.success(list);
    }

    @Operation(summary = "新增task_info")
    @PostMapping
    public Result<Void> saveTaskInfo(@RequestBody @Valid TaskInfoForm formData) {
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

    @Operation(summary = "执行任务一次")
    @PostMapping("/trigger")
    public Result<Void> triggerJob(@RequestBody TaskInfoTriggerDto taskInfoTriggerDto) {
        boolean result = taskInfoService.triggerJob(taskInfoTriggerDto);
        return Result.judge(result);
    }

    @Operation(summary = "启动")
    @GetMapping("/startTask/{id}")
    public Result<Void> startTask(@PathVariable Long id) {
        boolean result = taskInfoService.startTask(id);
        return Result.judge(result);
    }

    @Operation(summary = "停止")
    @GetMapping("/stopTask/{id}")
    public Result<Void> stopTask(@PathVariable Long id) {
        boolean result = taskInfoService.stopTask(id);
        return Result.judge(result);
    }

    @Operation(summary = "下一次的运行时间")
    @GetMapping("/nextTriggerTime")
    public Result<List<String>> nextTriggerTime(String scheduleType, String scheduleConf){
      List<String> list = taskInfoService.nextTriggerTime(scheduleType, scheduleConf);
      return Result.success(list);
    }


    @Operation(summary = "保存任务运行集")
    @PostMapping("saveTaskSet")
    public Result<Void>  saveTaskSet(@RequestBody @Valid TaskInfoForm formData){
        // 实现任务运行集的保存
        boolean result = taskInfoService.saveTaskSet(formData);
        return Result.judge(result);
    }

    @Operation(summary = "修改任务运行集")
    @PutMapping("updateTaskSet/{id}")
    public Result<Void>  updateTaskSet(@Parameter(description = "task_infoID") @PathVariable Long id,
                                       @RequestBody @Validated TaskInfoForm formData){
        // 实现任务运行集的保存
        boolean result = taskInfoService.updateTaskSet(id,formData);
        return Result.judge(result);
    }

    @Operation(summary = "停止任务集")
    @GetMapping("/stopTaskSet/{id}")
    public Result<Void> stopTaskSet(@PathVariable Long id) {
        boolean result = taskInfoService.stopTaskSet(id);
        return Result.judge(result);
    }

    @Operation(summary = "保存GlueSource")
    @PostMapping("saveGlueSource")
    public Result<Void>  saveGlueSource(@RequestBody @Valid TaskGlueForm formData){
        boolean result = taskInfoService.saveGlueSource(formData);
        return Result.judge(result);
    }


    @GetMapping("getGlueList/{id}")
    public Result<List<TaskLogglue>> getGlueList(@PathVariable Long id){
        List<TaskLogglue> list =  taskInfoService.getGlueList(id);
        return Result.success(list);
    }
}
