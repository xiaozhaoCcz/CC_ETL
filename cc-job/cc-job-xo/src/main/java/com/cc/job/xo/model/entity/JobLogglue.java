package com.cc.job.xo.model.entity;

import com.cc.job.xo.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * task_logglue实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@TableName("job_logglue")
public class JobLogglue extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 任务，主键ID
     */
    private Long jobId;
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


    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
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
}
