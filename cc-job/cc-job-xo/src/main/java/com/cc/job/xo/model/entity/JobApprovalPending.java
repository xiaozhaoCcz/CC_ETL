package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

/**
 * 审批/人工节点待办：执行到审批节点时创建，审批后可通过 API 继续流程
 */
@TableName("job_approval_pending")
public class JobApprovalPending extends BaseEntity {

    private Long jobLogId;
    private Long jobId;
    private Long nodeId;
    private String batchId;
    /** 审批人用户ID JSON数组，如 [1,2,3]，空表示不限制 */
    private String approverUserIds;
    /** 等待审批截止时间，超时未审批则任务组失败 */
    private java.time.LocalDateTime waitDeadline;
    /** 状态：pending, approved, rejected, timeout */
    private String status;
    private String remark;

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
    public java.time.LocalDateTime getWaitDeadline() { return waitDeadline; }
    public void setWaitDeadline(java.time.LocalDateTime waitDeadline) { this.waitDeadline = waitDeadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
