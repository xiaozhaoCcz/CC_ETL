package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.service.JobComposeService;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLogglue;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.xo.model.vo.JobNodeVo;
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
import java.util.Map;

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

    private final JobComposeService jobComposeService;

    @Operation(summary = "initData")
    @GetMapping("initData")
    public Result< List<Long>> initData() {
        List<Long> list = jobInfoService.initData();
        return Result.success(list);
    }

    @Operation(summary = "task_info分页列表")
    @GetMapping("/page")
    public PageResult<JobInfoVO> getJobInfoPage(JobInfoQuery queryParams) {
        IPage<JobInfoVO> result = jobInfoService.getJobInfoPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "task_info分页列表")
    @GetMapping("/list")
    public Result<List<JobInfo>> getJobInfoList(Integer jobType) {
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
    public Result<Long> saveJobInfo(@RequestBody @Valid JobInfoForm formData) {
        long id = jobInfoService.saveJobInfo(formData);
        return Result.success(id);
    }

    @Operation(summary = "获取task_info表单数据")
    @GetMapping("/{id}/form")
    public Result<JobInfoForm> getJobInfoForm(
            @Parameter(description = "task_infoID") @PathVariable Long id
    ) {
        JobInfoForm formData = jobInfoService.getJobInfoForm(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改task_info")
    @PutMapping(value = "/{id}")
    public Result<Void> updateJobInfo(
            @Parameter(description = "task_infoID") @PathVariable Long id,
            @RequestBody @Validated JobInfoForm formData
    ) {
        boolean result = jobInfoService.updateJobInfo(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除task_info")
    @DeleteMapping("/{ids}")
    public Result<Void> deleteJobInfos(
            @Parameter(description = "task_infoID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = jobInfoService.deleteJobInfos(ids);
        return Result.judge(result);
    }

    @Operation(summary = "执行任务一次")
    @PostMapping("/trigger")
    public Result<String> triggerJob(@RequestBody JobInfoTriggerDto taskInfoTriggerDto) {
        String result = jobInfoService.triggerJob(taskInfoTriggerDto);
        return Result.success(result);
    }

    @Operation(summary = "启动")
    @GetMapping("/startJob/{id}")
    public Result<Void> startJob(@PathVariable Long id) {
        boolean result = jobInfoService.startJob(id);
        return Result.judge(result);
    }

    @Operation(summary = "停止")
    @GetMapping("/stopJob/{id}")
    public Result<Void> stopJob(@PathVariable Long id) {
        boolean result = jobInfoService.stopJob(id);
        return Result.judge(result);
    }

    @Operation(summary = "下一次的运行时间")
    @GetMapping("/nextTriggerTime")
    public Result<List<String>> nextTriggerTime(String scheduleType, String scheduleConf){
      List<String> list = jobInfoService.nextTriggerTime(scheduleType, scheduleConf);
      return Result.success(list);
    }


    @Operation(summary = "保存任务运行集")
    @PostMapping("saveJobCompose")
    public Result<Void>  saveJobCompose(@RequestBody @Valid JobInfoForm formData){
        // 实现任务运行集的保存
        boolean result = jobComposeService.saveJobCompose(formData);
        return Result.judge(result);
    }

    @Operation(summary = "修改任务运行集")
    @PutMapping("updateJobCompose/{id}")
    public Result<Void>  updateJobCompose(@Parameter(description = "task_infoID") @PathVariable Long id,
                                       @RequestBody @Validated JobInfoForm formData){
        // 实现任务运行集的保存
        boolean result = jobComposeService.updateJobCompose(id,formData);
        return Result.judge(result);
    }

    @Operation(summary = "停止任务集")
    @GetMapping("/stopJobCompose/{id}/{randomId}")
    public Result<Void> stopJobCompose(@PathVariable Long id,@PathVariable String randomId) {
        boolean result = jobInfoService.stopJobCompose(id,randomId);
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

    @PostMapping("getJobCompose")
    public Result<Map<String,Object>> getJobCompose(@RequestBody Map<String,Object> formMap){
        Map<String,Object> map =  jobComposeService.getJobCompose(formMap);
        return Result.success(map);
    }

    @PostMapping("validateJobComposeEdge")
    public Result<Boolean> validateJobComposeEdge(@RequestBody Map<String,String> formMap){
        boolean result =  jobComposeService.validateJobComposeEdge(formMap.get("nodes"),formMap.get("edges"));
        return Result.success(result);
    }

    @Operation(summary = "暂停任务")
    @GetMapping("pauseJob/{id}")
    public Result<Void>  pauseJob(@PathVariable Long id,Integer isPause){
        boolean result = jobInfoService.pauseJob(id,isPause);
        return Result.judge(result);
    }

    @Operation(summary = "添加任务节点")
    @PostMapping("saveJobNode")
    public Result<JobNode>  saveJobNode(@RequestBody @Valid JobInfoForm formData){
        JobNode jobNode = jobComposeService.saveJobNode(formData);
        return Result.success(jobNode);
    }

    @Operation(summary = "任务运行状态")
    @GetMapping("getJobStatus/{id}")
    public Result<Boolean>  getJobStatus(@PathVariable Long id){
        JobInfo jobInfo = jobInfoService.getById(id);
        return Result.success(jobInfo.getRankTriggerStatus()>0);
    }

    @Operation(summary = "修改任务节点")
    @GetMapping("updateJobNode/{jobId}/{nodeId}")
    public Result<Long>  updateJobNode(@PathVariable Long jobId,@PathVariable Long nodeId){
        Long id = jobComposeService.updateJobNode(jobId,nodeId);
        return Result.success(id);
    }

    @Operation(summary = "查找暂停中的任务节点")
    @PostMapping("pauseJobs")
    public Result<List<Long>>  pauseJobs(@RequestBody Long[] jobIds){
        List<Long> list  = jobComposeService.pauseJobs(jobIds);
        return Result.success(list);
    }
}
