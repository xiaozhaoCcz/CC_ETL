package com.cc.job.admin.common.exception;

/**
 * 任务触发异常
 *
 * @author cc-job
 * @since 2025-12-02
 */
public class TriggerException extends RuntimeException {

    private final Long jobId;
    private final String triggerType;

    public TriggerException(Long jobId, String message) {
        super(message);
        this.jobId = jobId;
        this.triggerType = null;
    }

    public TriggerException(Long jobId, String triggerType, String message) {
        super(message);
        this.jobId = jobId;
        this.triggerType = triggerType;
    }

    public TriggerException(Long jobId, String message, Throwable cause) {
        super(message, cause);
        this.jobId = jobId;
        this.triggerType = null;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getTriggerType() {
        return triggerType;
    }
}

