package com.cc.job.xo.model.query;

import com.cc.job.xo.common.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * task_log分页查询对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Schema(description ="task_log查询对象")
public class JobLogQuery extends BasePageQuery {

    private Long jobId;

    private Long jobGroup;

    private Integer logStatus;

    private String[] filterTime;

    /** 关键词检索（匹配 handle_msg） */
    private String keyword;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Long jobGroup) {
        this.jobGroup = jobGroup;
    }

    public Integer getLogStatus() {
        return logStatus;
    }

    public void setLogStatus(Integer logStatus) {
        this.logStatus = logStatus;
    }

    public String[] getFilterTime() {
        return filterTime;
    }

    public void setFilterTime(String[] filterTime) {
        this.filterTime = filterTime;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
