package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

/**
 * 任务同步后数据质量校验配置
 */
@TableName("job_validation")
public class JobValidation extends BaseEntity {

    private Long jobId;
    private String validationSql;
    private Integer expectedMinRows;
    private Long jdbcDatasourceId;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getValidationSql() {
        return validationSql;
    }

    public void setValidationSql(String validationSql) {
        this.validationSql = validationSql;
    }

    public Integer getExpectedMinRows() {
        return expectedMinRows;
    }

    public void setExpectedMinRows(Integer expectedMinRows) {
        this.expectedMinRows = expectedMinRows;
    }

    public Long getJdbcDatasourceId() {
        return jdbcDatasourceId;
    }

    public void setJdbcDatasourceId(Long jdbcDatasourceId) {
        this.jdbcDatasourceId = jdbcDatasourceId;
    }
}
