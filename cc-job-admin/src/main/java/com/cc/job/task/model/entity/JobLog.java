package com.cc.job.task.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * task_log实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Getter
@Setter
@TableName("job_log")
public class JobLog {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 执行器主键ID
     */
    private Long jobGroup;
    /**
     * 任务，主键ID
     */
    private Long jobId;
    /**
     * 执行器地址，本次执行的地址
     */
    private String executorAddress;
    /**
     * 执行器任务handler
     */
    private String executorHandler;
    /**
     * 执行器任务参数
     */
    private String executorParam;
    /**
     * 执行器任务分片参数，格式如 1/2
     */
    private String executorShardingParam;
    /**
     * 失败重试次数
     */
    private Integer executorFailRetryCount;
    /**
     * 调度-时间
     */
    private LocalDateTime triggerTime;
    /**
     * 调度-结果
     */
    private Integer triggerCode;
    /**
     * 调度-日志
     */
    private String triggerMsg;
    /**
     * 执行-时间
     */
    private LocalDateTime handleTime;
    /**
     * 执行-状态
     */
    private Integer handleCode;
    /**
     * 执行-日志
     */
    private String handleMsg;
    /**
     * 告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败
     */
    private Integer alarmStatus;
}
