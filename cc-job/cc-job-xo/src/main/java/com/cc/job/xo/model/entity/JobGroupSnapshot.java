package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cc.job.xo.common.BaseEntity;
import java.time.LocalDateTime;

/**
 * 任务组快照实体
 * 用于保存任务组运行时的节点和边配置，防止运行中编辑影响执行
 *
 * @author ccjob
 * @since 2025-01-XX
 */
@TableName("job_group_snapshot")
public class JobGroupSnapshot extends BaseEntity {

    /**
     * 任务组ID
     */
    private Long jobId;

    /**
     * 执行批次ID（randomId）
     */
    private String randomId;

    /**
     * 节点快照JSON
     */
    private String nodesJson;

    /**
     * 边快照JSON
     */
    private String edgesJson;

    /**
     * 触发用户ID
     */
    private String triggerUserId;


    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getRandomId() {
        return randomId;
    }

    public void setRandomId(String randomId) {
        this.randomId = randomId;
    }

    public String getNodesJson() {
        return nodesJson;
    }

    public void setNodesJson(String nodesJson) {
        this.nodesJson = nodesJson;
    }

    public String getEdgesJson() {
        return edgesJson;
    }

    public void setEdgesJson(String edgesJson) {
        this.edgesJson = edgesJson;
    }

    public String getTriggerUserId() {
        return triggerUserId;
    }

    public void setTriggerUserId(String triggerUserId) {
        this.triggerUserId = triggerUserId;
    }
}

