package com.cc.job.executor.compose.interfaces.dto;

import java.util.Objects;

/**
 * 任务结果回调请求
 *
 * @author cc-job-team
 */
public class TaskResultCallbackRequest {
    
    /** 执行键（格式：jobId:randomId） */
    private String executeKey;
    
    /** 是否成功 */
    private Boolean success;
    
    /** 错误消息 */
    private String errorMessage;
    
    /** 执行耗时（毫秒） */
    private Long duration;

    /** 执行结果 */
    private Object executeResult;

    public String getExecuteKey() {
        return executeKey;
    }

    public void setExecuteKey(String executeKey) {
        this.executeKey = executeKey;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Object getExecuteResult() {
        return executeResult;
    }

    public void setExecuteResult(Object executeResult) {
        this.executeResult = executeResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskResultCallbackRequest that = (TaskResultCallbackRequest) o;
        return Objects.equals(executeKey, that.executeKey) &&
                Objects.equals(success, that.success) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(duration, that.duration) &&
                Objects.equals(executeResult, that.executeResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executeKey, success, errorMessage, duration, executeResult);
    }

    @Override
    public String toString() {
        return "TaskResultCallbackRequest{" +
                "executeKey='" + executeKey + '\'' +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", duration=" + duration +
                ", executeResult=" + executeResult +
                '}';
    }
}

