package com.cc.job.executor.handler;

import com.cc.job.executor.core.service.HttpTaskExecutor;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
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
public class ApiHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    
    private final JobInfoMapper jobInfoMapper;
    private final HttpTaskExecutor httpTaskExecutor;

    public ApiHandler(JobInfoMapper jobInfoMapper, HttpTaskExecutor httpTaskExecutor) {
        this.jobInfoMapper = jobInfoMapper;
        this.httpTaskExecutor = httpTaskExecutor;
    }
    
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
     * 
     * <p>优先从 TriggerParam 中获取已解析的参数，如果没有则从数据库获取（保持向后兼容）
     */
    private JobInfo getJobInfo(long jobId) {
        // 1. 尝试从 XxlJobContext 的 TriggerParam 中获取已解析的参数
        XxlJobContext context = XxlJobContext.getXxlJobContext();
        if (context != null) {
            TriggerParam triggerParam = context.getTriggerParam();
            if (triggerParam != null) {
                // 如果 TriggerParam 中有已解析的参数，使用它们
                JobInfo jobInfo = jobInfoMapper.selectById(jobId);
                if (jobInfo == null) {
                    throw new IllegalArgumentException(ExecutorConstants.ErrorMessage.JOB_NOT_FOUND);
                }
                
                // 使用 TriggerParam 中已解析的参数覆盖数据库中的参数
                // 优先级：TriggerParam 中的参数 > 数据库中的参数
                if (triggerParam.getReqUrl() != null) {
                    jobInfo.setReqUrl(triggerParam.getReqUrl());
                    logger.debug("[ApiHandler] 使用 TriggerParam 中的 reqUrl: {}", triggerParam.getReqUrl());
                }
                if (triggerParam.getReqBody() != null) {
                    jobInfo.setReqBody(triggerParam.getReqBody());
                    logger.debug("[ApiHandler] 使用 TriggerParam 中的 reqBody: {}", triggerParam.getReqBody());
                }
                if (triggerParam.getReqHeader() != null) {
                    jobInfo.setReqHeader(triggerParam.getReqHeader());
                    logger.debug("[ApiHandler] 使用 TriggerParam 中的 reqHeader: {}", triggerParam.getReqHeader());
                }
                if (triggerParam.getReqType() != null) {
                    jobInfo.setReqType(triggerParam.getReqType());
                    logger.debug("[ApiHandler] 使用 TriggerParam 中的 reqType: {}", triggerParam.getReqType());
                }
                
                return jobInfo;
            }
        }
        
        // 2. 如果没有 TriggerParam，从数据库获取（向后兼容）
        JobInfo jobInfo = jobInfoMapper.selectById(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException(ExecutorConstants.ErrorMessage.JOB_NOT_FOUND);
        }
        logger.debug("[ApiHandler] 从数据库获取任务信息 - jobId: {}", jobId);
        return jobInfo;
    }
}
