package com.cc.job.executor.compose.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务执行结果
 *
 * @author cc-job-team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionResult {
    
    /** 是否成功 */
    private boolean success;
    
    /** 结果消息 */
    private String message;
    
    /** 执行耗时（毫秒） */
    private long duration;
    
    public static TaskExecutionResult success(String message, long duration) {
        return new TaskExecutionResult(true, message, duration);
    }
    
    public static TaskExecutionResult failure(String message, long duration) {
        return new TaskExecutionResult(false, message, duration);
    }
}

