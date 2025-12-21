package com.cc.job.xo.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * task_log视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Schema( description = "task_log视图对象")
public class JobLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "执行器主键ID")
    private Integer jobGroup;
    @Schema(description = "任务，主键ID")
    private Integer jobId;
    @Schema(description = "任务名称")
    private String jobDesc;
    @Schema(description = "执行器地址，本次执行的地址")
    private String executorAddress;
    @Schema(description = "执行器任务handler")
    private String executorHandler;
    @Schema(description = "执行器任务参数")
    private String executorParam;
    @Schema(description = "执行器任务分片参数，格式如 1/2")
    private String executorShardingParam;
    @Schema(description = "失败重试次数")
    private Integer executorFailRetryCount;
    @Schema(description = "调度-时间")
    private String triggerTime;
    @Schema(description = "调度-结果")
    private Integer triggerCode;
    @Schema(description = "调度-日志")
    private String triggerMsg;
    @Schema(description = "执行-时间")
    private String handleTime;
    @Schema(description = "执行-状态")
    private Integer handleCode;
    @Schema(description = "执行-日志")
    private String handleMsg;
    @Schema(description = "告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败")
    private Integer alarmStatus;
    @Schema(description = "任务类型")
    private Integer jobType;
    @Schema(description = "节点执行状态（JSON格式）")
    private String nodeStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Integer jobGroup) {
        this.jobGroup = jobGroup;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
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

    public String getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(String triggerTime) {
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

    public String getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(String handleTime) {
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

    public Integer getJobType() {
        return jobType;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }

    public String getNodeStatus() {
        return nodeStatus;
    }

    public void setNodeStatus(String nodeStatus) {
        this.nodeStatus = nodeStatus;
    }
}
