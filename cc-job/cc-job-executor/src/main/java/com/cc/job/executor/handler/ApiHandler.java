package com.cc.job.executor.handler;

import com.cc.job.executor.core.service.HttpTaskExecutor;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HTTP API 任务处理器
 * 
 * <p>负责接收 XXL-Job 调度请求，执行 HTTP API 任务
 *
 * @author cc-job-team
 */
@Component
@RequiredArgsConstructor
public class ApiHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    
    private final JobInfoMapper jobInfoMapper;
    private final HttpTaskExecutor httpTaskExecutor;
    
    /**
     * 执行 HTTP API 任务
     */
    @XxlJob("runApiHandler")
    public void runApiHandler() {
        long jobId = XxlJobHelper.getJobId();
        logger.info("[ApiHandler] 开始执行HTTP任务 - jobId: {}", jobId);
        
        try {
            // 1. 获取任务信息
            JobInfo jobInfo = getJobInfo(jobId);
            
            // 2. 执行 HTTP 请求
            String result = httpTaskExecutor.execute(jobInfo);
            
            // 3. 输出结果
            XxlJobHelper.log("HTTP请求执行成功，响应: {}", result);
            logger.info("[ApiHandler] HTTP任务执行完成 - jobId: {}", jobId);
            
        } catch (Exception e) {
            logger.error("[ApiHandler] HTTP任务执行失败 - jobId: {}", jobId, e);
            XxlJobHelper.log("错误: {}", e.getMessage());
            throw new RuntimeException("HTTP任务执行失败", e);
        }
    }
    
    /**
     * 获取任务信息
     */
    private JobInfo getJobInfo(long jobId) {
        JobInfo jobInfo = jobInfoMapper.selectById(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException(ExecutorConstants.ErrorMessage.JOB_NOT_FOUND);
        }
        return jobInfo;
    }
}
