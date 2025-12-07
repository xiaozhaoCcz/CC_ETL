package com.cc.job.executor.compose.interfaces.dto;

import lombok.Data;

/**
 * 任务结果回调请求
 *
 * @author cc-job-team
 */
@Data
public class TaskResultCallbackRequest {
    
    /** 执行键（格式：jobId:randomId） */
    private String executeKey;
    
    /** 是否成功 */
    private Boolean success;
    
    /** 错误消息 */
    private String errorMessage;
    
    /** 执行耗时（毫秒） */
    private Long duration;
}

