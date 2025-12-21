package com.cc.job.executor.compose.core.model;

import java.util.Objects;

/**
 * 任务执行结果
 *
 * @author cc-job-team
 */
public class TaskExecutionResult {
    
    /** 是否成功 */
    private boolean success;
    
    /** 结果消息 */
    private String message;
    
    /** 执行耗时（毫秒） */
    private long duration;

    public TaskExecutionResult() {
    }

    public TaskExecutionResult(boolean success, String message, long duration) {
        this.success = success;
        this.message = message;
        this.duration = duration;
    }
    
    public static TaskExecutionResult success(String message, long duration) {
        return new TaskExecutionResult(true, message, duration);
    }
    
    public static TaskExecutionResult failure(String message, long duration) {
        return new TaskExecutionResult(false, message, duration);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskExecutionResult that = (TaskExecutionResult) o;
        return success == that.success && duration == that.duration && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message, duration);
    }

    @Override
    public String toString() {
        return "TaskExecutionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", duration=" + duration +
                '}';
    }
}

