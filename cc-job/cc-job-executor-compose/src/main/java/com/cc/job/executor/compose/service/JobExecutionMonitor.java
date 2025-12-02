package com.cc.job.executor.compose.service;

import cn.hutool.core.lang.Pair;
import com.cc.job.executor.compose.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行监听器
 * 
 * <p>负责监听任务执行状态，等待任务完成
 * 
 * @author xiaozhao
 */
public class JobExecutionMonitor implements Callable<String> {
    
    private static final Logger logger = LoggerFactory.getLogger(JobExecutionMonitor.class);
    
    private final JobInfo jobInfo;
    private final JobNode node;
    private final String randomId;
    private final Map<String, Boolean> jobResultMap;
    private final int retryCount;
    
    private volatile boolean stop = false;
    
    public JobExecutionMonitor(JobInfo jobInfo, JobNode node, String randomId,
                              Map<String, Boolean> jobResultMap, int retryCount) {
        this.jobInfo = jobInfo;
        this.node = node;
        this.randomId = randomId;
        this.jobResultMap = jobResultMap;
        this.retryCount = retryCount;
    }
    
    /**
     * 停止监听
     */
    public void stopMonitoring() {
        this.stop = true;
    }
    
    @Override
    public String call() {
        String executeKey = buildExecuteKey(jobInfo.getId(), randomId);
        
        logger.debug("[JobMonitor] 开始监听任务 - jobId: {}, randomId: {}", jobInfo.getId(), randomId);
        
        // 循环检查任务是否完成
        while (!stop) {
            if (jobResultMap.containsKey(executeKey)) {
                try {
                    Boolean success = jobResultMap.get(executeKey);
                    logger.info("[JobMonitor] 任务执行完成 - jobId: {}, 成功: {}", jobInfo.getId(), success);
                    
                    // 移除结果
                    jobResultMap.remove(executeKey);
                    
                    // 处理任务完成
                    return handleJobCompletion(success, retryCount);
                    
                } catch (Exception e) {
                    logger.error("[JobMonitor] 处理任务完成异常 - jobId: {}", jobInfo.getId(), e);
                    throw new RuntimeException(e);
                }
            }
            
            // 等待一段时间后再次检查
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("[JobMonitor] 任务监听线程被中断 - jobId: {}", jobInfo.getId());
                break;
            }
        }
        
        logger.debug("[JobMonitor] 监听结束 - jobId: {}", jobInfo.getId());
        return ExecutorConstants.ExecutionResult.SUCCESS;
    }
    
    /**
     * 处理任务完成
     */
    private String handleJobCompletion(boolean success, int currentRetryCount) {
        if (success) {
            logger.info("[JobMonitor] 任务执行成功 - jobId: {}, 任务名称: {}", 
                    jobInfo.getId(), jobInfo.getJobDesc());
            return ExecutorConstants.ExecutionResult.SUCCESS;
        } else {
            // 任务失败
            if (currentRetryCount < jobInfo.getExecutorFailRetryCount()) {
                logger.warn("[JobMonitor] 任务执行失败，准备重试 - jobId: {}, 当前重试次数: {}/{}", 
                        jobInfo.getId(), currentRetryCount, jobInfo.getExecutorFailRetryCount());
                return ExecutorConstants.ExecutionResult.FAIL_RETRY;
            } else {
                logger.error("[JobMonitor] 任务执行失败，已达到最大重试次数 - jobId: {}, 重试次数: {}",
                        jobInfo.getId(), currentRetryCount);
                return ExecutorConstants.ExecutionResult.FAIL_COMPLETE;
            }
        }
    }
    
    /**
     * 构建执行键
     */
    private static String buildExecuteKey(Long jobId, String randomId) {
        return jobId + ":" + randomId;
    }
}
