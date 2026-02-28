package com.cc.job.xo.model.dto;

import java.io.Serializable;

public class JobComposeDto implements Serializable {

    private Integer jobFlowPosition;

    private Integer jobPauseStatus;

    public Integer getJobFlowPosition() {
        return jobFlowPosition;
    }

    public void setJobFlowPosition(Integer jobFlowPosition) {
        this.jobFlowPosition = jobFlowPosition;
    }

    public Integer getJobPauseStatus() {
        return jobPauseStatus;
    }

    public void setJobPauseStatus(Integer jobPauseStatus) {
        this.jobPauseStatus = jobPauseStatus;
    }
}
