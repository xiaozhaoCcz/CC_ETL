package com.cc.job.admin.task.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.auth.AuthContext;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.mapper.JobApprovalPendingMapper;
import com.cc.job.xo.mapper.JobApprovalSatisfiedMapper;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.entity.JobApprovalPending;
import com.cc.job.xo.model.entity.JobApprovalSatisfied;
import com.cc.job.xo.model.entity.JobInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批/人工节点：待办列表、通过、拒绝、审批通过后恢复执行
 */
@Tag(name = "审批待办")
@RestController
@RequestMapping("/api/v1/approvals")
public class JobApprovalController {

    private final JobApprovalPendingMapper approvalMapper;
    private final JobApprovalSatisfiedMapper satisfiedMapper;
    private final JobInfoService jobInfoService;

    public JobApprovalController(JobApprovalPendingMapper approvalMapper,
                                 JobApprovalSatisfiedMapper satisfiedMapper,
                                 JobInfoService jobInfoService) {
        this.approvalMapper = approvalMapper;
        this.satisfiedMapper = satisfiedMapper;
        this.jobInfoService = jobInfoService;
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "待审批列表")
    @GetMapping("/pending")
    public Result<List<JobApprovalPending>> listPending(
            @Parameter(description = "任务组ID") @RequestParam(required = false) Long jobId) {
        LambdaQueryWrapper<JobApprovalPending> q = new LambdaQueryWrapper<>();
        q.eq(JobApprovalPending::getStatus, "pending").eq(JobApprovalPending::getIsDeleted, 0);
        if (jobId != null) q.eq(JobApprovalPending::getJobId, jobId);
        q.orderByDesc(JobApprovalPending::getCreateTime);
        List<JobApprovalPending> list = approvalMapper.selectList(q);
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId != null && list != null && !list.isEmpty()) {
            list = list.stream().filter(p -> {
                if (StringUtils.isBlank(p.getApproverUserIds())) return true;
                try {
                    List<Long> ids = JSONUtil.toList(JSONUtil.parseArray(p.getApproverUserIds()), Long.class);
                    return ids != null && ids.contains(currentUserId);
                } catch (Exception e) {
                    return true;
                }
            }).collect(Collectors.toList());
        }
        return Result.success(list);
    }

    /**
     * 执行器调用：创建审批待办（执行到审批节点时）
     */
    @Operation(summary = "创建审批待办（执行器调用）")
    @PostMapping("/pending")
    public Result<Long> createPending(@RequestBody CreatePendingBody body) {
        if (body == null || body.getJobId() == null || body.getNodeId() == null) {
            return Result.failed("jobId、nodeId 不能为空");
        }
        JobApprovalPending p = new JobApprovalPending();
        p.setJobLogId(body.getJobLogId());
        p.setJobId(body.getJobId());
        p.setNodeId(body.getNodeId());
        p.setBatchId(body.getBatchId());
        p.setApproverUserIds(body.getApproverUserIds());
        p.setWaitDeadline(body.getWaitDeadline());
        p.setStatus("pending");
        approvalMapper.insert(p);
        return Result.success(p.getId());
    }

    /**
     * 执行器调用：查询定时任务该节点是否已审批通过过（仅审批一次）
     */
    @Operation(summary = "是否已审批通过（执行器调用）")
    @GetMapping("/satisfied")
    public Result<Boolean> isSatisfied(
            @Parameter(description = "任务组ID") @RequestParam Long jobId,
            @Parameter(description = "节点ID") @RequestParam Long nodeId) {
        LambdaQueryWrapper<JobApprovalSatisfied> q = new LambdaQueryWrapper<>();
        q.eq(JobApprovalSatisfied::getJobParentId, jobId)
          .eq(JobApprovalSatisfied::getNodeId, nodeId)
          .eq(JobApprovalSatisfied::getIsDeleted, 0);
        long count = satisfiedMapper.selectCount(q);
        return Result.success(count > 0);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_EDIT)
    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(
            @Parameter(description = "待办ID") @PathVariable Long id,
            @RequestBody(required = false) ApprovalBody body) {
        JobApprovalPending p = approvalMapper.selectById(id);
        if (p == null || !"pending".equals(p.getStatus())) {
            return Result.failed("记录不存在或已处理");
        }
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId != null && StringUtils.isNotBlank(p.getApproverUserIds())) {
            try {
                List<Long> ids = JSONUtil.toList(JSONUtil.parseArray(p.getApproverUserIds()), Long.class);
                if (ids != null && !ids.contains(currentUserId)) {
                    return Result.failed("您不在审批人列表中");
                }
            } catch (Exception ignored) { }
        }
        p.setStatus("approved");
        if (body != null && body.getRemark() != null) p.setRemark(body.getRemark());
        p.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(p);

        if (p.getJobId() != null && p.getNodeId() != null) {
            JobInfo taskGroup = jobInfoService.getById(p.getJobId());
            if (taskGroup != null && "CRON".equalsIgnoreCase(taskGroup.getScheduleType())) {
                LambdaQueryWrapper<JobApprovalSatisfied> sq = new LambdaQueryWrapper<>();
                sq.eq(JobApprovalSatisfied::getJobParentId, p.getJobId())
                  .eq(JobApprovalSatisfied::getNodeId, p.getNodeId())
                  .eq(JobApprovalSatisfied::getIsDeleted, 0);
                if (satisfiedMapper.selectCount(sq) == 0) {
                    JobApprovalSatisfied satisfied = new JobApprovalSatisfied();
                    satisfied.setJobParentId(p.getJobId());
                    satisfied.setNodeId(p.getNodeId());
                    satisfiedMapper.insert(satisfied);
                }
            }
            String resumeParam = "{\"resumeBatchId\":\"" + (p.getBatchId() != null ? p.getBatchId() : "") + "\",\"fromNodeId\":\"" + p.getNodeId() + "\"}";
            JobInfoTriggerDto dto = new JobInfoTriggerDto();
            dto.setId(p.getJobId());
            dto.setExecutorParam(resumeParam);
            try {
                jobInfoService.triggerJob(dto);
            } catch (Exception e) {
                return Result.failed("审批通过但恢复执行失败: " + e.getMessage());
            }
        }
        return Result.success();
    }

    @RequirePermission(PermissionConstants.JOB_INFO_EDIT)
    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(
            @Parameter(description = "待办ID") @PathVariable Long id,
            @RequestBody(required = false) ApprovalBody body) {
        JobApprovalPending p = approvalMapper.selectById(id);
        if (p == null || !"pending".equals(p.getStatus())) {
            return Result.failed("记录不存在或已处理");
        }
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId != null && StringUtils.isNotBlank(p.getApproverUserIds())) {
            try {
                List<Long> ids = JSONUtil.toList(JSONUtil.parseArray(p.getApproverUserIds()), Long.class);
                if (ids != null && !ids.contains(currentUserId)) {
                    return Result.failed("您不在审批人列表中");
                }
            } catch (Exception ignored) { }
        }
        p.setStatus("rejected");
        if (body != null && body.getRemark() != null) p.setRemark(body.getRemark());
        p.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(p);
        return Result.success();
    }

    public static class ApprovalBody {
        private String remark;
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class CreatePendingBody {
        private Long jobLogId;
        private Long jobId;
        private Long nodeId;
        private String batchId;
        private String approverUserIds;
        private LocalDateTime waitDeadline;
        public Long getJobLogId() { return jobLogId; }
        public void setJobLogId(Long jobLogId) { this.jobLogId = jobLogId; }
        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public Long getNodeId() { return nodeId; }
        public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getApproverUserIds() { return approverUserIds; }
        public void setApproverUserIds(String approverUserIds) { this.approverUserIds = approverUserIds; }
        public LocalDateTime getWaitDeadline() { return waitDeadline; }
        public void setWaitDeadline(LocalDateTime waitDeadline) { this.waitDeadline = waitDeadline; }
    }
}
