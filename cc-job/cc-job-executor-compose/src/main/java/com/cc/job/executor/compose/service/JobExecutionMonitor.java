package com.cc.job.executor.compose.service;

import com.cc.job.executor.compose.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

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
    private final String executeKey;
    private final CountDownLatch latch;
    
    private volatile boolean stop = false;
    
    public JobExecutionMonitor(JobInfo jobInfo, JobNode node, String randomId,
                              Map<String, Boolean> jobResultMap, int retryCount) {
        this.jobInfo = jobInfo;
        this.node = node;
        this.randomId = randomId;
        this.jobResultMap = jobResultMap;
        this.retryCount = retryCount;
        this.executeKey = buildExecuteKey(jobInfo.getId(), randomId);
        this.latch = new CountDownLatch(1);
    }
    
    public String getExecuteKey() {
        return executeKey;
    }
    
    public CountDownLatch getLatch() {
        return latch;
    }
    
    /**
     * 停止监听
     */
    public void stopMonitoring() {
        this.stop = true;
        this.latch.countDown();
    }
    
    @Override
    public String call() {
        logger.debug("[JobMonitor] 开始监听任务 - jobId: {}, randomId: {}, executeKey: {}", 
                jobInfo.getId(), randomId, executeKey);
        
        try {
            latch.await();
            
            if (stop) {
                logger.debug("[JobMonitor] 监听被停止 - jobId: {}", jobInfo.getId());
                return ExecutorConstants.ExecutionResult.SUCCESS;
            }
            
            Boolean success = jobResultMap.get(executeKey);
            if (success != null) {
                logger.info("[JobMonitor] 任务执行完成 - jobId: {}, 成功: {}", jobInfo.getId(), success);
                jobResultMap.remove(executeKey);
                return handleJobCompletion(success, retryCount);
            } else {
                logger.warn("[JobMonitor] 任务结果不存在 - jobId: {}, executeKey: {}", jobInfo.getId(), executeKey);
                return ExecutorConstants.ExecutionResult.SUCCESS;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("[JobMonitor] 任务监听线程被中断 - jobId: {}", jobInfo.getId());
            return ExecutorConstants.ExecutionResult.SUCCESS;
        } catch (Exception e) {
            logger.error("[JobMonitor] 处理任务完成异常 - jobId: {}", jobInfo.getId(), e);
            throw new RuntimeException(e);
        }
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
                if(ExecutorConstants.ExecutionResult.DO_NOTHING.equals(jobInfo.getExecutorBlockStrategy())) {
                    return ExecutorConstants.ExecutionResult.DO_NOTHING;
                }
                return ExecutorConstants.ExecutionResult.FAIL_RETRY;
            } else {
                if(ExecutorConstants.ExecutionResult.DO_NOTHING.equals(jobInfo.getExecutorBlockStrategy())) {
                    return ExecutorConstants.ExecutionResult.DO_NOTHING;
                }
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
