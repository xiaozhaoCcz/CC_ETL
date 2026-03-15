package com.cc.job.xo.model.form;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

/**
 * task_info表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Schema(description = "task_info表单对象")
public class JobInfoForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 画布节点ID（前端需要显示的节点唯一标识）
     */
    private String nodeId;

    private Long id;

    @Schema(description = "执行器主键ID")
    @NotNull(message = "执行器不能为空")
    private Long jobGroup;

    @Size(max=255, message="长度不能超过255个字符")
    @NotBlank(message = "任务描述不能为空")
    private String jobDesc;

    @Schema(description = "作者")
    @NotBlank(message = "作者不能为空")
    @Size(max=64, message="作者长度不能超过64个字符")
    private String author;

    @Schema(description = "报警邮件")
    private String alarmEmail;

    @Schema(description = "调度类型")
    @NotBlank(message = "调度类型不能为空")
    @Size(max=50, message="调度类型长度不能超过50个字符")
    private String scheduleType;

    @Schema(description = "调度配置，值含义取决于调度类型")
    private String scheduleConf;

    @Schema(description = "调度过期策略")
    @NotBlank(message = "调度过期策略不能为空")
    private String misfireStrategy;

    @Schema(description = "执行器路由策略")
    @NotBlank(message = "执行器路由策略不能为空")
    private String executorRouteStrategy;

    @Schema(description = "失败策略")
    @NotBlank(message = "失败策略不能为空")
    private String failStrategy;

    @Schema(description = "执行器任务handler")
    private String executorHandler;

    @Schema(description = "执行器任务参数")
    private String executorParam;

    @Schema(description = "阻塞处理策略")
    @NotBlank(message = "阻塞处理策略不能为空")
    private String executorBlockStrategy;

    @Schema(description = "任务执行超时时间，单位秒")
    private Integer executorTimeout;

    @Schema(description = "失败重试次数")
    private Integer executorFailRetryCount;

    @Schema(description = "GLUE类型")
    @NotNull(message = "GLUE类型代码不能为空")
    private String glueType;

    @Schema(description = "GLUE源代码")
    private String glueSource;

    @Schema(description = "GLUE备注")
    private String glueRemark;

    @Schema(description = "GLUE更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime glueUpdateTime;

    @Schema(description = "子任务ID，多个逗号分隔")
    private String childJobId;

    private Integer jobType;

    private Long parentId;

    private String reqType;

    private String reqHeader;

    private String reqBody;

    private String reqUrl;

    private String nodes;

    private String edges;

    private Long jdbcDatasourceId;

    private Integer incrementType;

    private String incrementContent;

    private String incrementParamTemplate;

    private Integer pauseStatus;

    private Integer jobPartId;

    /** 审批等待超时时间（分钟），默认 1440 */
    private Integer approvalWaitMinutes;

    private Double nodePositionX;

    private Double nodePositionY;

    /**
     * 最近一次运行耗时（毫秒），来自 job_info.run_time
     */
    private Long runTime;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

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

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getEdges() {
        return edges;
    }

    public void setEdges(String edges) {
        this.edges = edges;
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

    public String getIncrementParamTemplate() {
        return incrementParamTemplate;
    }

    public void setIncrementParamTemplate(String incrementParamTemplate) {
        this.incrementParamTemplate = incrementParamTemplate;
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

    public Integer getApprovalWaitMinutes() {
        return approvalWaitMinutes;
    }

    public void setApprovalWaitMinutes(Integer approvalWaitMinutes) {
        this.approvalWaitMinutes = approvalWaitMinutes;
    }

    public Double getNodePositionX() {
        return nodePositionX;
    }

    public void setNodePositionX(Double nodePositionX) {
        this.nodePositionX = nodePositionX;
    }

    public Double getNodePositionY() {
        return nodePositionY;
    }

    public void setNodePositionY(Double nodePositionY) {
        this.nodePositionY = nodePositionY;
    }

    public Long getRunTime() {
        return runTime;
    }

    public void setRunTime(Long runTime) {
        this.runTime = runTime;
    }
}
