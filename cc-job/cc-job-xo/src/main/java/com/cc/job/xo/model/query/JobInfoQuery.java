package com.cc.job.xo.model.query;

import com.cc.job.xo.common.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * task_info分页查询对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Schema(description ="task_info查询对象")
public class JobInfoQuery extends BasePageQuery {

    private Long jobGroup;

    private Integer triggerStatus;

    private String jobDesc;

    private String executorHandler;

    private String author;

    /**
     * 任务类型：0-单任务，2-任务组。可选，不传则查 0 和 2。
     */
    private Integer jobType;

    public Long getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Long jobGroup) {
        this.jobGroup = jobGroup;
    }

    public Integer getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(Integer triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getJobType() {
        return jobType;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }
}
