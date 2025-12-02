package com.cc.job.executor.compose.infrastructure.exception;

/**
 * 任务编排异常
 * 
 * <p>任务编排过程中发生的业务异常
 *
 * @author cc-job-team
 * @since 2025-12-02
 */
public class TaskOrchestrationException extends RuntimeException {

    private final Long taskGroupId;

    public TaskOrchestrationException(Long taskGroupId, String message) {
        super(message);
        this.taskGroupId = taskGroupId;
    }

    public TaskOrchestrationException(Long taskGroupId, String message, Throwable cause) {
        super(message, cause);
        this.taskGroupId = taskGroupId;
    }

    public Long getTaskGroupId() {
        return taskGroupId;
    }

    @Override
    public String toString() {
        return "TaskOrchestrationException{" +
                "taskGroupId=" + taskGroupId +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}

