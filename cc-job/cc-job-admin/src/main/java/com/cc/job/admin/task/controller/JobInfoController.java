package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.service.JobComposeService;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.entity.JobEdge;
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
    
    private final com.cc.job.admin.task.service.JobNodeService jobNodeService;
    
    private final com.cc.job.admin.task.service.JobEdgeService jobEdgeService;

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
    
    @GetMapping("getGlueList/{id}/{glueType}")
    public Result<List<JobLogglue>> getGlueListByType(
            @PathVariable Long id,
            @PathVariable(required = false) String glueType){
        List<JobLogglue> list = jobInfoService.getGlueList(id, glueType);
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

    @Operation(summary = "添加任务节点和边")
    @PostMapping("saveJobNodeAndJobEdges")
    public Result<Map<String,Object>>  saveJobNodeAndJobEdges(@RequestBody Map<String,Object> formMap){
        Map<String,Object> data = jobComposeService.saveJobNodeAndJobEdges(formMap);
        return Result.success(data);
    }

    @Operation(summary = "保存连线")
    @PostMapping("saveJobEdge")
    public Result<com.cc.job.xo.model.entity.JobEdge> saveJobEdge(@RequestBody @Valid com.cc.job.xo.model.form.JobEdgeForm formData) {
        com.cc.job.xo.model.entity.JobEdge jobEdge = jobComposeService.saveJobEdge(formData);
        return Result.success(jobEdge);
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

    @Operation(summary = "删除任务节点")
    @GetMapping("deleteJobNode/{nodeId}")
    public Result<Void>  deleteJobNode(@PathVariable Long nodeId){
        jobComposeService.deleteJobNode(nodeId);
        return Result.success();
    }
    
    @Operation(summary = "更新节点运行状态")
    @PostMapping("updateNodeStatus")
    public Result<Boolean> updateNodeStatus(
            @Parameter(description = "任务ID") @RequestParam Long jobId,
            @Parameter(description = "运行状态：0=失败, 1=成功, 2=运行中") @RequestParam Integer triggerStatus
    ) {
        try {
            boolean success = jobNodeService.updateNodeStatus(jobId, triggerStatus);
            return Result.judge(success);
        } catch (Exception e) {
            return Result.failed("更新节点状态失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "批量更新节点运行状态")
    @PostMapping("batchUpdateNodeStatus")
    public Result<Integer> batchUpdateNodeStatus(
            @Parameter(description = "节点状态映射 {jobId: triggerStatus}") @RequestBody Map<String, Integer> statusMap
    ) {
        try {
            // 将String类型的key转换为Long
            Map<Long, Integer> convertedMap = new java.util.HashMap<>();
            for (Map.Entry<String, Integer> entry : statusMap.entrySet()) {
                try {
                    Long jobId = Long.parseLong(entry.getKey());
                    convertedMap.put(jobId, entry.getValue());
                } catch (NumberFormatException e) {
                    // 跳过无效的jobId
                    continue;
                }
            }
            
            int successCount = jobNodeService.batchUpdateNodeStatus(convertedMap);
            return Result.success(successCount);
        } catch (Exception e) {
            return Result.failed("批量更新节点状态失败: " + e.getMessage());
        }
    }
    
    // ============= 以下为任务组执行器调用的API接口 =============
    
    @Operation(summary = "获取任务信息（供执行器调用）")
    @GetMapping("/{id}")
    public Result<JobInfo> getJobInfoById(
            @Parameter(description = "任务ID") @PathVariable Long id
    ) {
        JobInfo jobInfo = jobInfoService.getById(id);
        if (jobInfo == null) {
            return Result.failed("任务不存在");
        }
        return Result.success(jobInfo);
    }
    
    @Operation(summary = "获取任务组的所有节点（供执行器调用）")
    @GetMapping("/nodes/{jobId}")
    public Result<List<JobNode>> getJobNodes(
            @Parameter(description = "任务组ID") @PathVariable Long jobId
    ) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JobNode> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(JobNode::getJobParentId, jobId);
        List<JobNode> nodes = jobNodeService.list(wrapper);
        return Result.success(nodes);
    }
    
    @Operation(summary = "获取任务组的所有边（供执行器调用）")
    @GetMapping("/edges/{jobId}")
    public Result<List<JobEdge>> getJobEdges(
            @Parameter(description = "任务组ID") @PathVariable Long jobId
    ) {
        LambdaQueryWrapper<JobEdge> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(JobEdge::getJobParentId, jobId);
        List<JobEdge> edges = jobEdgeService.list(wrapper);
        return Result.success(edges);
    }
    
    @Operation(summary = "上报任务执行状态（供执行器调用）")
    @PostMapping("/status")
    public Result<Void> reportStatus(
            @RequestBody Map<String, Object> statusData
    ) {
        try {
            Long jobId = Long.valueOf(statusData.get("jobId").toString());
            String randomId = statusData.get("randomId").toString();
            Integer status = Integer.valueOf(statusData.get("status").toString());
            String message = statusData.getOrDefault("message", "").toString();
            
            // 这里可以添加状态保存逻辑,例如保存到数据库或发送SSE消息
            // 目前先简单记录日志
            org.slf4j.LoggerFactory.getLogger(getClass()).info(
                    "收到任务状态上报 - jobId: {}, randomId: {}, status: {}, message: {}", 
                    jobId, randomId, status, message);
            
            // 可以调用SSE服务推送状态更新
            // sseService.sendJobStatus(jobId, randomId, status, message);
            
            return Result.success();
        } catch (Exception e) {
            return Result.failed("上报状态失败: " + e.getMessage());
        }
    }
}
