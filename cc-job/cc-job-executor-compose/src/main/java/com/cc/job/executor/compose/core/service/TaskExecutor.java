package com.cc.job.executor.compose.core.service;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.exception.WaitingApprovalException;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.executor.compose.service.JobExecutionMonitor;
import com.cc.job.executor.compose.service.JobTriggerService;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
public class TaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);
    
    private final AdminApiClient adminApiClient;
    private final JobTriggerService jobTriggerService;
    private final ResultStorageService resultStorageService;

    public TaskExecutor(AdminApiClient adminApiClient, JobTriggerService jobTriggerService, ResultStorageService resultStorageService) {
        this.adminApiClient = adminApiClient;
        this.jobTriggerService = jobTriggerService;
        this.resultStorageService = resultStorageService;
    }
    
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
            if (context.isPausedNode(node.getId())) {
                logger.info("[TaskExecutor] 节点被阻塞，本次跳过执行 - jobId: {}, nodeId: {}",
                        jobInfo.getId(), node.getId());
                XxlJobHelper.log(context.getXxlJobContext(),
                        "节点被阻塞，本次跳过执行 - jobId: {}, nodeId: {}", jobInfo.getId(), node.getId());
                return SUCCESS;
            }
            // 1. 审批节点：恢复执行时 fromNode 视为已通过，直接跳过执行（由编排器 no-op 处理）；否则检查需审批则创建待办并挂起
            if (context.getResumeFromNodeId() != null && context.getResumeFromNodeId().equals(node.getId())) {
                return SUCCESS;
            }
            boolean requireApproval = parseRequireApproval(node.getProperties());
            if (requireApproval) {
                JobInfo taskGroup = context.getTaskGroupInfo();
                boolean cronAndSatisfied = taskGroup != null && "CRON".equalsIgnoreCase(taskGroup.getScheduleType())
                        && adminApiClient.isApprovalSatisfied(context.getTaskGroupId(), node.getId());
                if (!cronAndSatisfied) {
                    Long jobLogId = null;
                    if (context.getXxlJobContext() != null && context.getXxlJobContext().getTriggerParam() != null) {
                        jobLogId = context.getXxlJobContext().getTriggerParam().getLogId();
                    }
                    int waitMinutes = (taskGroup != null && taskGroup.getApprovalWaitMinutes() != null) ? taskGroup.getApprovalWaitMinutes() : 1440;
                    LocalDateTime waitDeadline = LocalDateTime.now().plusMinutes(waitMinutes);
                    String approverUserIds = parseApproverUserIds(node.getProperties());
                    Long pendingId = adminApiClient.createPending(jobLogId, context.getTaskGroupId(), node.getId(),
                            context.getExecutionBatchId(), approverUserIds, waitDeadline);
                    if (pendingId != null) {
                        logger.info("[TaskExecutor] 审批节点已创建待办 - pendingId: {}, nodeId: {}", pendingId, node.getId());
                    }
                    throw new WaitingApprovalException("等待审批");
                }
            }
            // 2. 触发任务执行（传入执行上下文，用于参数解析）
            boolean triggerSuccess = jobTriggerService.triggerJob(
                    context.getXxlJobContext(), jobInfo, context.getExecutionBatchId(), context);
            
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
     * 处理任务暂停(废弃功能)
     */
    @Deprecated
    private void handlePauseIfNeeded(JobNode jobNode, List<Integer> jobPauseStatusIds) {
       JobInfo latestJobInfo = adminApiClient.getJobInfo(jobNode.getJobId());
        if (latestJobInfo == null) {
            return;
        }
        if(jobPauseStatusIds==null||jobPauseStatusIds.isEmpty()){
            return;
        }
        
        boolean isPaused = jobPauseStatusIds.contains(jobNode.getId().intValue());
        if (!isPaused) {
            return;
        }
        
        logger.info("[TaskExecutor] 任务处于暂停状态，等待恢复 - jobId: {}", latestJobInfo.getId());
        
        long timeout = calculatePauseTimeout(latestJobInfo);
        long deadline = System.currentTimeMillis() + timeout;
        
        while (isPaused && System.currentTimeMillis() < deadline) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("[TaskExecutor] 暂停等待被中断 - jobId: {}", latestJobInfo.getId());
                throw new RuntimeException("暂停等待被中断", e);
            }
            
//            JobInfo checkJobInfo = adminApiClient.getJobInfo(latestJobInfo.getId());
//            if (checkJobInfo != null) {
//                isPaused = checkJobInfo.getPauseStatus() != null && checkJobInfo.getPauseStatus() == 1;
//            }
        }
        
//        if (!isPaused) {
//            logger.info("[TaskExecutor] 任务恢复执行 - jobId: {}", latestJobInfo.getId());
//        } else {
//            logger.warn("[TaskExecutor] 任务暂停等待超时 - jobId: {}", latestJobInfo.getId());
//        }
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
                    context, jobResults, retryCount, resultStorageService);
            
            TaskWrapperFactory.registerMonitor(monitor.getExecuteKey(), monitor);
            
            FutureTask<String> futureTask = new FutureTask<>(monitor);
            monitorThread = new Thread(futureTask);
            monitorThread.start();
            
            String result = waitForCompletion(futureTask, jobInfo);
            logger.info("[TaskExecutor] 任务监听完成 - jobId: {}, 结果: {}", jobInfo.getId(), result);
            return result;
            
        } catch (TimeoutException e) {
            return handleTimeout(context, node, jobInfo);
        } catch (Exception e) {
            return handleMonitorError(context, node, jobInfo, e);
        } finally {
            if (monitor != null) {
                TaskWrapperFactory.unregisterMonitor(monitor.getExecuteKey());
            }
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
        
        if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getFailStrategy())) {
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

        if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getFailStrategy())) {
            throw new RuntimeException("任务执行超时");
        } else {
            logger.warn("[TaskExecutor] 任务执行超时但忽略继续执行 - jobId: {}", jobInfo.getId());
            return DO_NOTHING;
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

    private boolean parseRequireApproval(String propertiesJson) {
        if (propertiesJson == null || propertiesJson.trim().isEmpty()) return false;
        try {
            Map<String, Object> map = JSONUtil.toBean(propertiesJson, Map.class);
            Object v = map.get("requireApproval");
            if (v instanceof Boolean) return (Boolean) v;
            if (v != null) return "true".equalsIgnoreCase(String.valueOf(v));
        } catch (Exception ignored) { }
        return false;
    }

    private String parseApproverUserIds(String propertiesJson) {
        if (propertiesJson == null || propertiesJson.trim().isEmpty()) return null;
        try {
            Map<String, Object> map = JSONUtil.toBean(propertiesJson, Map.class);
            Object v = map.get("approverUserIds");
            if (v == null) return null;
            if (v instanceof String) return (String) v;
            return JSONUtil.toJsonStr(v);
        } catch (Exception ignored) { }
        return null;
    }
}

