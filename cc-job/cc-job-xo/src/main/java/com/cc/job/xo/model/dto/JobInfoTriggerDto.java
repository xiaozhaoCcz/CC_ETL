package com.cc.job.xo.model.dto;

import java.util.Objects;

public class JobInfoTriggerDto {

    private Long id;

    private String executorParam;

    private String addressList;
    
    /**
     * 触发任务的用户ID
     */
    private Integer triggerUserId;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobInfoTriggerDto that = (JobInfoTriggerDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(executorParam, that.executorParam) &&
                Objects.equals(addressList, that.addressList) &&
                Objects.equals(triggerUserId, that.triggerUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, executorParam, addressList, triggerUserId);
    }

    @Override
    public String toString() {
        return "JobInfoTriggerDto{" +
                "id=" + id +
                ", executorParam='" + executorParam + '\'' +
                ", addressList='" + addressList + '\'' +
                ", triggerUserId=" + triggerUserId +
                '}';
    }
}
