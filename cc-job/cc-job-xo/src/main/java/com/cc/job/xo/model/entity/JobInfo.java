package com.cc.job.xo.model.entity;

import com.cc.job.xo.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * task_info实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@TableName("job_info")
public class JobInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 执行器主键ID
     */
    private Long jobGroup;

    private String jobDesc;
    /**
     * 作者
     */
    private String author;
    /**
     * 报警邮件
     */
    private String alarmEmail;
    /**
     * 调度类型
     */
    private String scheduleType;
    /**
     * 调度配置，值含义取决于调度类型
     */
    private String scheduleConf;
    /**
     * 调度过期策略
     */
    private String misfireStrategy;
    /**
     * 执行器路由策略
     */
    private String executorRouteStrategy;
    /**
     * 失败策略
     */
    private String failStrategy;
    /**
     * 执行器任务handler
     */
    private String executorHandler;
    /**
     * 执行器任务参数
     */
    private String executorParam;
    /**
     * 阻塞处理策略
     */
    private String executorBlockStrategy;
    /**
     * 任务执行超时时间，单位秒
     */
    private Integer executorTimeout;
    /**
     * 失败重试次数
     */
    private Integer executorFailRetryCount;
    /**
     * GLUE类型
     */
    private String glueType;
    /**
     * GLUE源代码
     */
    private String glueSource;
    /**
     * GLUE备注
     */
    private String glueRemark;
    /**
     * GLUE更新时间
     */
    private LocalDateTime glueUpdateTime;
    /**
     * 子任务ID，多个逗号分隔
     */
    private String childJobId;
    /**
     * 调度状态：0-停止，1-运行
     */
    private Integer triggerStatus;
    /**
     * 一次调度状态：0-停止，1-运行
     */
    private Integer triggerOneStatus;
    /**
     * 上次调度时间
     */
    private Long triggerLastTime;
    /**
     * 下次调度时间
     */
    private Long triggerNextTime;
    /**
     * 任务类型：0-普通任务，2-任务组
     */
    private Integer jobType;
    /**
     * 父任务ID
     */
    private Long parentId;
    /**
     * 请求类型（API任务专用）
     */
    private String reqType;
    /**
     * 请求头（API任务专用）
     */
    private String reqHeader;
    /**
     * 请求体（API任务专用）
     */
    private String reqBody;
    /**
     * 请求URL（API任务专用）
     */
    private String reqUrl;
    /**
     * 节点标识：Y-是节点，N-不是节点
     */
    private String nodeFlag;
    /**
     * JDBC数据源ID
     */
    private Long jdbcDatasourceId;
    /**
     * 增量类型：0-全量，1-增量
     */
    private Integer incrementType;
    /**
     * 增量字段配置（JSON格式）
     */
    private String incrementContent;
    /**
     * 最近一次运行耗时（毫秒）
     */
    private Long runTime;
    /**
     * 暂停状态：0-运行，1-暂停
     */
    private Integer pauseStatus;
    /**
     * 任务分区ID
     */
    private Integer jobPartId;
    /**
     * 触发用户ID
     */
    private Integer triggerUserId;

    public Long getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Long jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAlarmEmail() {
        return alarmEmail;
    }

    public void setAlarmEmail(String alarmEmail) {
        this.alarmEmail = alarmEmail;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    public String getMisfireStrategy() {
        return misfireStrategy;
    }

    public void setMisfireStrategy(String misfireStrategy) {
        this.misfireStrategy = misfireStrategy;
    }

    public String getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(String executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
    }

    public String getFailStrategy() {
        return failStrategy;
    }

    public void setFailStrategy(String failStrategy) {
        this.failStrategy = failStrategy;
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

    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public Integer getExecutorTimeout() {
        return executorTimeout;
    }

    public void setExecutorTimeout(Integer executorTimeout) {
        this.executorTimeout = executorTimeout;
    }

    public Integer getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(Integer executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public String getGlueRemark() {
        return glueRemark;
    }

    public void setGlueRemark(String glueRemark) {
        this.glueRemark = glueRemark;
    }

    public LocalDateTime getGlueUpdateTime() {
        return glueUpdateTime;
    }

    public void setGlueUpdateTime(LocalDateTime glueUpdateTime) {
        this.glueUpdateTime = glueUpdateTime;
    }

    public String getChildJobId() {
        return childJobId;
    }

    public void setChildJobId(String childJobId) {
        this.childJobId = childJobId;
    }

    public Integer getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(Integer triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public Integer getTriggerOneStatus() {
        return triggerOneStatus;
    }

    public void setTriggerOneStatus(Integer triggerOneStatus) {
        this.triggerOneStatus = triggerOneStatus;
    }

    public Long getTriggerLastTime() {
        return triggerLastTime;
    }

    public void setTriggerLastTime(Long triggerLastTime) {
        this.triggerLastTime = triggerLastTime;
    }

    public Long getTriggerNextTime() {
        return triggerNextTime;
    }

    public void setTriggerNextTime(Long triggerNextTime) {
        this.triggerNextTime = triggerNextTime;
    }

    public Integer getJobType() {
        return jobType;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getReqType() {
        return reqType;
    }

    public void setReqType(String reqType) {
        this.reqType = reqType;
    }

    public String getReqHeader() {
        return reqHeader;
    }

    public void setReqHeader(String reqHeader) {
        this.reqHeader = reqHeader;
    }

    public String getReqBody() {
        return reqBody;
    }

    public void setReqBody(String reqBody) {
        this.reqBody = reqBody;
    }

    public String getReqUrl() {
        return reqUrl;
    }

    public void setReqUrl(String reqUrl) {
        this.reqUrl = reqUrl;
    }

    public String getNodeFlag() {
        return nodeFlag;
    }

    public void setNodeFlag(String nodeFlag) {
        this.nodeFlag = nodeFlag;
    }

    public Long getJdbcDatasourceId() {
        return jdbcDatasourceId;
    }

    public void setJdbcDatasourceId(Long jdbcDatasourceId) {
        this.jdbcDatasourceId = jdbcDatasourceId;
    }

    public Integer getIncrementType() {
        return incrementType;
    }

    public void setIncrementType(Integer incrementType) {
        this.incrementType = incrementType;
    }

    public String getIncrementContent() {
        return incrementContent;
    }

    public void setIncrementContent(String incrementContent) {
        this.incrementContent = incrementContent;
    }

    public Long getRunTime() {
        return runTime;
    }

    public void setRunTime(Long runTime) {
        this.runTime = runTime;
    }

    public Integer getPauseStatus() {
        return pauseStatus;
    }

    public void setPauseStatus(Integer pauseStatus) {
        this.pauseStatus = pauseStatus;
    }

    public Integer getJobPartId() {
        return jobPartId;
    }

    public void setJobPartId(Integer jobPartId) {
        this.jobPartId = jobPartId;
    }

    public Integer getTriggerUserId() {
        return triggerUserId;
    }

    public void setTriggerUserId(Integer triggerUserId) {
        this.triggerUserId = triggerUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JobInfo jobInfo = (JobInfo) o;
        return Objects.equals(jobGroup, jobInfo.jobGroup) &&
                Objects.equals(jobDesc, jobInfo.jobDesc) &&
                Objects.equals(author, jobInfo.author) &&
                Objects.equals(alarmEmail, jobInfo.alarmEmail) &&
                Objects.equals(scheduleType, jobInfo.scheduleType) &&
                Objects.equals(scheduleConf, jobInfo.scheduleConf) &&
                Objects.equals(misfireStrategy, jobInfo.misfireStrategy) &&
                Objects.equals(executorRouteStrategy, jobInfo.executorRouteStrategy) &&
                Objects.equals(failStrategy, jobInfo.failStrategy) &&
                Objects.equals(executorHandler, jobInfo.executorHandler) &&
                Objects.equals(executorParam, jobInfo.executorParam) &&
                Objects.equals(executorBlockStrategy, jobInfo.executorBlockStrategy) &&
                Objects.equals(executorTimeout, jobInfo.executorTimeout) &&
                Objects.equals(executorFailRetryCount, jobInfo.executorFailRetryCount) &&
                Objects.equals(glueType, jobInfo.glueType) &&
                Objects.equals(glueSource, jobInfo.glueSource) &&
                Objects.equals(glueRemark, jobInfo.glueRemark) &&
                Objects.equals(glueUpdateTime, jobInfo.glueUpdateTime) &&
                Objects.equals(childJobId, jobInfo.childJobId) &&
                Objects.equals(triggerStatus, jobInfo.triggerStatus) &&
                Objects.equals(triggerOneStatus, jobInfo.triggerOneStatus) &&
                Objects.equals(triggerLastTime, jobInfo.triggerLastTime) &&
                Objects.equals(triggerNextTime, jobInfo.triggerNextTime) &&
                Objects.equals(jobType, jobInfo.jobType) &&
                Objects.equals(parentId, jobInfo.parentId) &&
                Objects.equals(reqType, jobInfo.reqType) &&
                Objects.equals(reqHeader, jobInfo.reqHeader) &&
                Objects.equals(reqBody, jobInfo.reqBody) &&
                Objects.equals(reqUrl, jobInfo.reqUrl) &&
                Objects.equals(nodeFlag, jobInfo.nodeFlag) &&
                Objects.equals(jdbcDatasourceId, jobInfo.jdbcDatasourceId) &&
                Objects.equals(incrementType, jobInfo.incrementType) &&
                Objects.equals(incrementContent, jobInfo.incrementContent) &&
                Objects.equals(runTime, jobInfo.runTime) &&
                Objects.equals(pauseStatus, jobInfo.pauseStatus) &&
                Objects.equals(jobPartId, jobInfo.jobPartId) &&
                Objects.equals(triggerUserId, jobInfo.triggerUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), jobGroup, jobDesc, author, alarmEmail, scheduleType, scheduleConf, misfireStrategy, executorRouteStrategy, failStrategy, executorHandler, executorParam, executorBlockStrategy, executorTimeout, executorFailRetryCount, glueType, glueSource, glueRemark, glueUpdateTime, childJobId, triggerStatus, triggerOneStatus, triggerLastTime, triggerNextTime, jobType, parentId, reqType, reqHeader, reqBody, reqUrl, nodeFlag, jdbcDatasourceId, incrementType, incrementContent, runTime, pauseStatus, jobPartId, triggerUserId);
    }

    @Override
    public String toString() {
        return "JobInfo{" +
                "jobGroup=" + jobGroup +
                ", jobDesc='" + jobDesc + '\'' +
                ", author='" + author + '\'' +
                ", alarmEmail='" + alarmEmail + '\'' +
                ", scheduleType='" + scheduleType + '\'' +
                ", scheduleConf='" + scheduleConf + '\'' +
                ", misfireStrategy='" + misfireStrategy + '\'' +
                ", executorRouteStrategy='" + executorRouteStrategy + '\'' +
                ", failStrategy='" + failStrategy + '\'' +
                ", executorHandler='" + executorHandler + '\'' +
                ", executorParam='" + executorParam + '\'' +
                ", executorBlockStrategy='" + executorBlockStrategy + '\'' +
                ", executorTimeout=" + executorTimeout +
                ", executorFailRetryCount=" + executorFailRetryCount +
                ", glueType='" + glueType + '\'' +
                ", glueSource='" + glueSource + '\'' +
                ", glueRemark='" + glueRemark + '\'' +
                ", glueUpdateTime=" + glueUpdateTime +
                ", childJobId='" + childJobId + '\'' +
                ", triggerStatus=" + triggerStatus +
                ", triggerOneStatus=" + triggerOneStatus +
                ", triggerLastTime=" + triggerLastTime +
                ", triggerNextTime=" + triggerNextTime +
                ", jobType=" + jobType +
                ", parentId=" + parentId +
                ", reqType='" + reqType + '\'' +
                ", reqHeader='" + reqHeader + '\'' +
                ", reqBody='" + reqBody + '\'' +
                ", reqUrl='" + reqUrl + '\'' +
                ", nodeFlag='" + nodeFlag + '\'' +
                ", jdbcDatasourceId=" + jdbcDatasourceId +
                ", incrementType=" + incrementType +
                ", incrementContent='" + incrementContent + '\'' +
                ", runTime=" + runTime +
                ", pauseStatus=" + pauseStatus +
                ", jobPartId=" + jobPartId +
                ", triggerUserId=" + triggerUserId +
                '}';
    }
}
