package com.cc.job.admin.task.thread;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.job.xo.mapper.JobApprovalPendingMapper;
import com.cc.job.xo.mapper.JobLogMapper;
import com.cc.job.xo.model.entity.JobApprovalPending;
import com.cc.job.xo.model.entity.JobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批待办超时：扫描 wait_deadline 已过的待办，置为 timeout 并标记对应 job_log 失败
 */
@Component
public class ApprovalTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTimeoutTask.class);

    private final JobApprovalPendingMapper approvalMapper;
    private final JobLogMapper jobLogMapper;

    public ApprovalTimeoutTask(JobApprovalPendingMapper approvalMapper, JobLogMapper jobLogMapper) {
        this.approvalMapper = approvalMapper;
        this.jobLogMapper = jobLogMapper;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void scanAndFailTimeout() {
        LambdaQueryWrapper<JobApprovalPending> q = new LambdaQueryWrapper<>();
        q.eq(JobApprovalPending::getStatus, "pending")
          .eq(JobApprovalPending::getIsDeleted, 0)
          .lt(JobApprovalPending::getWaitDeadline, LocalDateTime.now());
        List<JobApprovalPending> list = approvalMapper.selectList(q);
        if (list == null || list.isEmpty()) return;
        for (JobApprovalPending p : list) {
            try {
                LambdaUpdateWrapper<JobApprovalPending> uw = new LambdaUpdateWrapper<>();
                uw.eq(JobApprovalPending::getId, p.getId())
                  .set(JobApprovalPending::getStatus, "timeout")
                  .set(JobApprovalPending::getUpdateTime, LocalDateTime.now());
                approvalMapper.update(null, uw);
                if (p.getJobLogId() != null) {
                    JobLog logEntity = jobLogMapper.selectById(p.getJobLogId());
                    if (logEntity != null && logEntity.getHandleCode() == 0) {
                        logEntity.setHandleCode(500);
                        logEntity.setHandleMsg("审批超时");
                        logEntity.setHandleTime(LocalDateTime.now());
                        jobLogMapper.updateById(logEntity);
                    }
                }
                log.info("[ApprovalTimeout] 待办超时 - pendingId: {}, jobId: {}, nodeId: {}", p.getId(), p.getJobId(), p.getNodeId());
            } catch (Exception e) {
                log.warn("[ApprovalTimeout] 处理待办超时失败 - id: {}", p.getId(), e);
            }
        }
    }
}
