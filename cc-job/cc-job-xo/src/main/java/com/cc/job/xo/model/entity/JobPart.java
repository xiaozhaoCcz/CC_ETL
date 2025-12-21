package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

import java.io.Serial;

@TableName("job_part")
public class JobPart extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String jobPartName;

    private Integer sort;

    public String getJobPartName() {
        return jobPartName;
    }

    public void setJobPartName(String jobPartName) {
        this.jobPartName = jobPartName;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
