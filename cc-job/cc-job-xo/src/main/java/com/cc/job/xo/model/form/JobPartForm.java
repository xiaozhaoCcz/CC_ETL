package com.cc.job.xo.model.form;


import io.swagger.v3.oas.annotations.media.Schema;


import java.io.Serial;
import java.io.Serializable;

@Schema(description = "job_part表单对象")
public class JobPartForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String jobPartName;

    private Long sort;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobPartName() {
        return jobPartName;
    }

    public void setJobPartName(String jobPartName) {
        this.jobPartName = jobPartName;
    }

    public Long getSort() {
        return sort;
    }

    public void setSort(Long sort) {
        this.sort = sort;
    }
}
