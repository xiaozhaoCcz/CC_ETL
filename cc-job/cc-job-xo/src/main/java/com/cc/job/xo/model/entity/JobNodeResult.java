package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

/**
 * 节点执行结果实体对象
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
@TableName("job_node_result")
public class JobNodeResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 任务组ID
     */
    private Long taskGroupId;

    /**
     * 执行批次ID
     */
    private String executionBatchId;

    /**
     * 节点任务ID
     */
    private Long jobId;

    /**
     * 节点任务名称
     */
    private String jobName;

    /**
     * 执行结果数据（JSON格式，用于兼容小数据或作为后备）
     */
    private String resultData;

    /**
     * 文件路径（相对路径，相对于basePath）
     */
    private String filePath;

    /**
     * 数据大小（字节）
     */
    private Long dataSize;

    public Long getTaskGroupId() {
        return taskGroupId;
    }

    public void setTaskGroupId(Long taskGroupId) {
        this.taskGroupId = taskGroupId;
    }

    public String getExecutionBatchId() {
        return executionBatchId;
    }

    public void setExecutionBatchId(String executionBatchId) {
        this.executionBatchId = executionBatchId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getDataSize() {
        return dataSize;
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }
}

