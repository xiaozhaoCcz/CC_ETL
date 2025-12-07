package com.cc.job.executor.compose.infrastructure.exception;

/**
 * 任务触发异常
 * 
 * <p>任务触发过程中发生的异常
 *
 * @author cc-job-team
 * @since 2025-12-02
 */
public class TaskTriggerException extends RuntimeException {

    private final Long taskId;
    private final String triggerType;

    public TaskTriggerException(Long taskId, String message) {
        super(message);
        this.taskId = taskId;
        this.triggerType = null;
    }

    public TaskTriggerException(Long taskId, String triggerType, String message) {
        super(message);
        this.taskId = taskId;
        this.triggerType = triggerType;
    }

    public TaskTriggerException(Long taskId, String message, Throwable cause) {
        super(message, cause);
        this.taskId = taskId;
        this.triggerType = null;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    @Override
    public String toString() {
        return "TaskTriggerException{" +
                "taskId=" + taskId +
                ", triggerType='" + triggerType + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}

