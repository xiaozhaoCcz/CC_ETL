package com.cc.job.executor.compose.core.service;

import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.executor.compose.service.JobExecutionMonitor;
import com.cc.job.executor.compose.service.JobTriggerService;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.cc.job.executor.compose.infrastructure.constant.ExecutorConstants.ExecutionResult.*;

/**
 * 任务执行器
 * 
 * <p>负责具体的任务执行逻辑
 *
 * @author cc-job-team
 */
@Component("jobTaskExecutor")
@RequiredArgsConstructor
public class TaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);
    
    private final AdminApiClient adminApiClient;
    private final JobTriggerService jobTriggerService;
    
    /**
     * 执行单个任务（完整实现）
     * 
     * @param context 执行上下文
     * @param node 任务节点
     * @param jobInfo 任务信息
     * @param retryCount 重试次数
     * @param jobResults 结果映射
     * @return 执行结果
     */
    public String executeTask(ExecutionContext context, JobNode node, JobInfo jobInfo, 
                             int retryCount, Map<String, Boolean> jobResults) {
        logger.info("[TaskExecutor] 开始执行任务 - jobId: {}, nodeId: {}, 重试次数: {}",
                jobInfo.getId(), node.getId(), retryCount);
        
        try {
            // 1. 检查并处理暂停状态
            handlePauseIfNeeded(jobInfo);
            
            // 2. 触发任务执行
            boolean triggerSuccess = jobTriggerService.triggerJob(
                    context.getXxlJobContext(), jobInfo, context.getExecutionBatchId());
            
            if (!triggerSuccess) {
                return handleTriggerFailure(jobInfo);
            }
            
            // 3. 监听任务执行状态
            return monitorExecution(context, node, jobInfo, retryCount, jobResults);
            
        } catch (Exception e) {
            return handleExecutionError(context, node, jobInfo, e);
        }
    }
    
    /**
     * 处理任务暂停
     */
    private void handlePauseIfNeeded(JobInfo jobInfo) {
        JobInfo latestJobInfo = adminApiClient.getJobInfo(jobInfo.getId());
        if (latestJobInfo == null) {
            return;
        }
        
        boolean isPaused = latestJobInfo.getPauseStatus() != null && latestJobInfo.getPauseStatus() == 1;
        if (!isPaused) {
            return;
        }
        
        logger.info("[TaskExecutor] 任务处于暂停状态，等待恢复 - jobId: {}", jobInfo.getId());
        
        long timeout = calculatePauseTimeout(jobInfo);
        long startTime = System.currentTimeMillis();
        
        while (isPaused && !isTimeout(startTime, timeout)) {
            sleepQuietly(5);
            
            JobInfo checkJobInfo = adminApiClient.getJobInfo(jobInfo.getId());
            if (checkJobInfo != null) {
                isPaused = checkJobInfo.getPauseStatus() != null && checkJobInfo.getPauseStatus() == 1;
            }
        }
        
        if (!isPaused) {
            logger.info("[TaskExecutor] 任务恢复执行 - jobId: {}", jobInfo.getId());
        } else {
            logger.warn("[TaskExecutor] 任务暂停等待超时 - jobId: {}", jobInfo.getId());
        }
    }
    
    /**
     * 监听任务执行
     */
    private String monitorExecution(ExecutionContext context, JobNode node, JobInfo jobInfo,
                                   int retryCount, Map<String, Boolean> jobResults) {
        logger.debug("[TaskExecutor] 开始监听任务执行 - jobId: {}, nodeId: {}", 
                jobInfo.getId(), node.getId());
        
        Thread monitorThread = null;
        JobExecutionMonitor monitor = null;
        
        try {
            monitor = new JobExecutionMonitor(jobInfo, node, 
                    context.getExecutionBatchId(), jobResults, retryCount);
            FutureTask<String> futureTask = new FutureTask<>(monitor);
            monitorThread = new Thread(futureTask);
            monitorThread.start();
            
            // 等待任务完成（支持超时）
            String result = waitForCompletion(futureTask, jobInfo);
            logger.info("[TaskExecutor] 任务监听完成 - jobId: {}, 结果: {}", jobInfo.getId(), result);
            return result;
            
        } catch (TimeoutException e) {
            return handleTimeout(context, node, jobInfo);
        } catch (Exception e) {
            return handleMonitorError(context, node, jobInfo, e);
        } finally {
            cleanup(monitor, monitorThread);
        }
    }
    
    /**
     * 等待任务完成
     */
    private String waitForCompletion(FutureTask<String> futureTask, JobInfo jobInfo) 
            throws Exception {
        if (jobInfo.getExecutorTimeout() > 0) {
            return futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.SECONDS);
        } else {
            return futureTask.get();
        }
    }
    
    /**
     * 处理触发失败
     */
    private String handleTriggerFailure(JobInfo jobInfo) {
        logger.error("[TaskExecutor] 任务触发失败 - jobId: {}", jobInfo.getId());
        
        if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
            return FAIL_RETRY;
        } else {
            logger.warn("[TaskExecutor] 任务触发失败但忽略继续执行 - jobId: {}", jobInfo.getId());
            return SUCCESS;
        }
    }
    
    /**
     * 处理超时
     */
    private String handleTimeout(ExecutionContext context, JobNode node, JobInfo jobInfo) {
        logger.error("[TaskExecutor] 任务执行超时 - jobId: {}, nodeId: {}, 超时: {}秒",
                jobInfo.getId(), node.getId(), jobInfo.getExecutorTimeout());
        
        reportStatus(context, jobInfo.getId(), 0, "任务执行超时");
        
        if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
            throw new RuntimeException("任务执行超时");
        } else {
            logger.warn("[TaskExecutor] 任务执行超时但忽略继续执行 - jobId: {}", jobInfo.getId());
            return SUCCESS;
        }
    }
    
    /**
     * 处理执行错误
     */
    private String handleExecutionError(ExecutionContext context, JobNode node, 
                                       JobInfo jobInfo, Exception e) {
        logger.error("[TaskExecutor] 任务执行异常 - jobId: {}, nodeId: {}", 
                jobInfo.getId(), node.getId(), e);
        
        reportStatus(context, jobInfo.getId(), 0, "任务执行异常: " + e.getMessage());
        
        if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
            throw new RuntimeException(e);
        } else {
            logger.warn("[TaskExecutor] 任务执行异常但忽略继续执行 - jobId: {}", jobInfo.getId());
            return SUCCESS;
        }
    }
    
    /**
     * 处理监听错误
     */
    private String handleMonitorError(ExecutionContext context, JobNode node, 
                                     JobInfo jobInfo, Exception e) {
        logger.error("[TaskExecutor] 任务监听异常 - jobId: {}, nodeId: {}", 
                jobInfo.getId(), node.getId(), e);
        
        reportStatus(context, jobInfo.getId(), 0, "任务监听异常");
        
        if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
            throw new RuntimeException(e);
        } else {
            logger.warn("[TaskExecutor] 任务监听异常但忽略继续执行 - jobId: {}", jobInfo.getId());
            return SUCCESS;
        }
    }
    
    /**
     * 计算暂停超时时间
     */
    private long calculatePauseTimeout(JobInfo jobInfo) {
        return jobInfo.getExecutorTimeout() > 0 
                ? jobInfo.getExecutorTimeout() * 1000 
                : 5 * 60 * 1000;
    }
    
    /**
     * 检查是否超时
     */
    private boolean isTimeout(long startTime, long timeout) {
        return System.currentTimeMillis() - startTime >= timeout;
    }
    
    /**
     * 安静地休眠
     */
    private void sleepQuietly(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            logger.error("[TaskExecutor] 休眠被中断", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup(JobExecutionMonitor monitor, Thread thread) {
        if (monitor != null) {
            monitor.stopMonitoring();
        }
        if (thread != null) {
            thread.interrupt();
        }
    }
    
    /**
     * 上报任务状态
     */
    private void reportStatus(ExecutionContext context, Long jobId, Integer status, String message) {
        try {
            adminApiClient.reportStatus(context.getTaskGroupId(), jobId, 
                    context.getExecutionBatchId(), status, message);
        } catch (Exception e) {
            logger.error("[TaskExecutor] 上报状态失败 - jobId: {}, status: {}", 
                    jobId, status, e);
        }
    }
    
    /**
     * 获取任务信息映射
     */
    private Map<Long, JobInfo> getJobInfoMap(List<JobNode> nodes) {
        Map<Long, JobInfo> jobInfoMap = new HashMap<>();
        List<Long> jobIds = nodes.stream().map(JobNode::getJobId).distinct().toList();
        
        for (Long jobId : jobIds) {
            JobInfo jobInfo = adminApiClient.getJobInfo(jobId);
            if (jobInfo != null) {
                jobInfoMap.put(jobId, jobInfo);
            }
        }
        
        return jobInfoMap;
    }
}

