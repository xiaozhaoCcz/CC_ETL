package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

/**
 * 定时任务审批节点“仅审批一次”：记录(任务组,节点)已审批通过，后续定时跑不再挂起
 */
@TableName("job_approval_satisfied")
public class JobApprovalSatisfied extends BaseEntity {

    /** 任务组ID */
    private Long jobParentId;
    /** 节点ID(JobNode.id) */
    private Long nodeId;

    public Long getJobParentId() {
        return jobParentId;
    }

    public void setJobParentId(Long jobParentId) {
        this.jobParentId = jobParentId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }
}
