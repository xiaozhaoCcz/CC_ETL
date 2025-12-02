package com.cc.job.executor.infrastructure.exception;

/**
 * 任务执行异常
 *
 * @author cc-job-team
 */
public class TaskExecutionException extends RuntimeException {

    private final Long taskId;

    public TaskExecutionException(Long taskId, String message) {
        super(message);
        this.taskId = taskId;
    }

    public TaskExecutionException(Long taskId, String message, Throwable cause) {
        super(message, cause);
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }
}

