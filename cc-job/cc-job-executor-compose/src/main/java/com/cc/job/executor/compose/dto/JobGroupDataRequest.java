package com.cc.job.executor.compose.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 任务组数据请求DTO
 * 用于接收子任务执行完成后的回调数据
 * 
 * @author xiaozhao
 */
public class JobGroupDataRequest {
    
    /**
     * 执行键（格式：jobId:randomId）
     */
    @JsonProperty("key")
    private String key;
    
    /**
     * 是否成功
     */
    @JsonProperty("value")
    private Boolean value;
    
    /**
     * 执行结果
     */
    @JsonProperty("executeResult")
    private Object executeResult;
    
    public JobGroupDataRequest() {
        // 默认构造函数，Jackson需要
    }
    
    public JobGroupDataRequest(String key, Boolean value) {
        this.key = key;
        this.value = value;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public Boolean getValue() {
        return value;
    }
    
    public void setValue(Boolean value) {
        this.value = value;
    }
    
    public Object getExecuteResult() {
        return executeResult;
    }
    
    public void setExecuteResult(Object executeResult) {
        this.executeResult = executeResult;
    }
    
    @Override
    public String toString() {
        return "JobGroupDataRequest{" +
                "key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}

