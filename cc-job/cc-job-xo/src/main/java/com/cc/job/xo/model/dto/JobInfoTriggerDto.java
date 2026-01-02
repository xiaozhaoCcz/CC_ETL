package com.cc.job.xo.model.dto;

import java.util.List;
import java.util.Objects;

public class JobInfoTriggerDto {

    private Long id;

    private String executorParam;

    private String addressList;
    
    /**
     * 触发任务的用户ID
     */
    private Integer triggerUserId;

    private List<Integer> jobFlowPositionIds;

    private List<Integer> jobPauseStatusIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public String getAddressList() {
        return addressList;
    }

    public void setAddressList(String addressList) {
        this.addressList = addressList;
    }

    public Integer getTriggerUserId() {
        return triggerUserId;
    }

    public void setTriggerUserId(Integer triggerUserId) {
        this.triggerUserId = triggerUserId;
    }

    public List<Integer> getJobFlowPositionIds() {
        return jobFlowPositionIds;
    }

    public void setJobFlowPositionIds(List<Integer> jobFlowPositionIds) {
        this.jobFlowPositionIds = jobFlowPositionIds;
    }

    public List<Integer> getJobPauseStatusIds() {
        return jobPauseStatusIds;
    }

    public void setJobPauseStatusIds(List<Integer> jobPauseStatusIds) {
        this.jobPauseStatusIds = jobPauseStatusIds;
    }
}
