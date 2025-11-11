package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cc.job.xo.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 任务组快照实体
 * 用于保存任务组运行时的节点和边配置，防止运行中编辑影响执行
 *
 * @author ccjob
 * @since 2025-01-XX
 */
@Getter
@Setter
@ToString
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


}

