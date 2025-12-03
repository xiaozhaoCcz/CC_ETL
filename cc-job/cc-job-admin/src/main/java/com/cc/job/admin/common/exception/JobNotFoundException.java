package com.cc.job.admin.common.exception;

/**
 * 任务未找到异常
 *
 * @author cc-job
 * @since 2025-12-02
 */
public class JobNotFoundException extends RuntimeException {

    private final Long jobId;

    public JobNotFoundException(Long jobId) {
        super("任务不存在: jobId=" + jobId);
        this.jobId = jobId;
    }

    public JobNotFoundException(Long jobId, String message) {
        super(message);
        this.jobId = jobId;
    }

    public JobNotFoundException(Long jobId, String message, Throwable cause) {
        super(message, cause);
        this.jobId = jobId;
    }

    public Long getJobId() {
        return jobId;
    }
}

