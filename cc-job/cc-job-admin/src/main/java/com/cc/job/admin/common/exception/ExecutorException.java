package com.cc.job.admin.common.exception;

/**
 * 执行器异常
 *
 * @author cc-job
 * @since 2025-12-02
 */
public class ExecutorException extends RuntimeException {

    private final String executorAddress;

    public ExecutorException(String executorAddress, String message) {
        super(message);
        this.executorAddress = executorAddress;
    }

    public ExecutorException(String executorAddress, String message, Throwable cause) {
        super(message, cause);
        this.executorAddress = executorAddress;
    }

    public String getExecutorAddress() {
        return executorAddress;
    }
}

