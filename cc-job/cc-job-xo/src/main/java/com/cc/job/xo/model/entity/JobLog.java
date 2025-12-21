package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * task_log实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
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
    
    /**
     * 节点执行状态（JSON格式）
     * 格式：{"nodeId1": {"status": 1, "jobId": 123, "jobDesc": "节点名称"}, "nodeId2": {...}}
     * status: 0-失败, 1-成功, 2-运行中
     */
    private String nodeStatus;

    @TableLogic
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Long jobGroup) {
        this.jobGroup = jobGroup;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getExecutorAddress() {
        return executorAddress;
    }

    public void setExecutorAddress(String executorAddress) {
        this.executorAddress = executorAddress;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public String getExecutorShardingParam() {
        return executorShardingParam;
    }

    public void setExecutorShardingParam(String executorShardingParam) {
        this.executorShardingParam = executorShardingParam;
    }

    public Integer getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(Integer executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public LocalDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(LocalDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }

    public Integer getTriggerCode() {
        return triggerCode;
    }

    public void setTriggerCode(Integer triggerCode) {
        this.triggerCode = triggerCode;
    }

    public String getTriggerMsg() {
        return triggerMsg;
    }

    public void setTriggerMsg(String triggerMsg) {
        this.triggerMsg = triggerMsg;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }

    public Integer getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(Integer handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public Integer getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Integer alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    public String getNodeStatus() {
        return nodeStatus;
    }

    public void setNodeStatus(String nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
