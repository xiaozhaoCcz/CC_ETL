package com.cc.job.executor.compose.core.orchestrator;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.executor.compose.core.service.TaskDependencyBuilder;
import com.cc.job.executor.compose.core.service.TaskGraphBuilder;
import com.cc.job.executor.compose.core.service.TaskWrapperFactory;
import com.cc.job.executor.compose.engine.Async;
import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;
import com.cc.job.executor.compose.handler.JobGroupUtils;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 任务组编排器
 * 
 * <p>负责任务组的编排和执行
 *
 * @author cc-job-team
 */
@Component
@RequiredArgsConstructor
public class TaskGroupOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskGroupOrchestrator.class);
    
    private final AdminApiClient adminApiClient;
    private final TaskGraphBuilder graphBuilder;
    private final TaskWrapperFactory wrapperFactory;
    private final TaskDependencyBuilder dependencyBuilder;
    private final JobGroupUtils jobGroupUtils;
    
    /** 存储正在执行的任务组 */
    private static final Map<String, List<WorkerWrapper<Long, String>>> RUNNING_JOBS = new ConcurrentHashMap<>();
    
    /** 线程本地变量，用于存储 XxlJobContext */
    private static final InheritableThreadLocal<XxlJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();
    
    /**
     * 执行任务组
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     */
    public void execute(Long taskGroupId, String executionBatchId) {
        logger.info("[Orchestrator] ========== 开始执行任务组 ==========");
        logger.info("[Orchestrator] 任务组ID: {}, 批次ID: {}", taskGroupId, executionBatchId);
        
        try {
            // 1. 准备执行上下文
            ExecutionContext context = prepareExecution(taskGroupId, executionBatchId);
            
            // 2. 构建执行计划
            List<WorkerWrapper<Long, String>> workerWrappers = buildExecutionPlan(context);
            
            // 3. 执行任务组
            executeTaskGroup(context, workerWrappers);
            
            logger.info("[Orchestrator] ========== 任务组执行完成 ==========");
            XxlJobHelper.handleSuccess("任务组执行完成");
            
        } catch (ExecutionException e) {
            handleExecutionException(taskGroupId, executionBatchId, e);
        } catch (Exception e) {
            handleGeneralException(taskGroupId, executionBatchId, e);
        } finally {
            cleanup(taskGroupId, executionBatchId);
        }
    }
    
    /**
     * 准备执行上下文
     */
    private ExecutionContext prepareExecution(Long taskGroupId, String executionBatchId) {
        logger.debug("[Orchestrator] 准备执行上下文 - taskGroupId: {}", taskGroupId);
        
        // 获取任务组信息
        JobInfo taskGroupInfo = adminApiClient.getJobInfo(taskGroupId);
        if (taskGroupInfo == null) {
            throw new RuntimeException("获取任务组信息失败");
        }
        
        // 获取节点和边
        List<JobNode> nodes = adminApiClient.getJobNodes(taskGroupId);
        List<JobEdge> edges = adminApiClient.getJobEdges(taskGroupId);
        
        if (nodes.isEmpty()) {
            logger.warn("[Orchestrator] 任务组没有节点 - taskGroupId: {}", taskGroupId);
            XxlJobHelper.handleSuccess("任务组没有节点，跳过执行");
            throw new RuntimeException("任务组没有节点");
        }
        
        logger.info("[Orchestrator] 获取到 {} 个节点，{} 条边", nodes.size(), edges.size());
        
        // 构建任务图
        graphBuilder.buildGraph(taskGroupId, nodes, edges);
        
        // 保存上下文
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        CONTEXT_HOLDER.set(xxlJobContext);
        
        return ExecutionContext.builder()
                .taskGroupId(taskGroupId)
                .executionBatchId(executionBatchId)
                .taskGroupInfo(taskGroupInfo)
                .nodes(nodes)
                .edges(edges)
                .xxlJobContext(xxlJobContext)
                .executeKey(buildExecuteKey(taskGroupId, executionBatchId))
                .build();
    }
    
    /**
     * 构建执行计划
     */
    private List<WorkerWrapper<Long, String>> buildExecutionPlan(ExecutionContext context) {
        logger.debug("[Orchestrator] 构建执行计划");
        
        // 创建 WorkerWrapper
        List<WorkerWrapper<Long, String>> workerWrappers = 
                wrapperFactory.createWorkerWrappers(context);
        
        if (workerWrappers.isEmpty()) {
            throw new RuntimeException("没有可执行的任务");
        }
        
        // 构建依赖关系
        dependencyBuilder.buildDependencies(workerWrappers, context.getNodes(), context.getEdges());
        
        // 保存到运行中的任务映射
        RUNNING_JOBS.put(context.getExecuteKey(), workerWrappers);
        
        return workerWrappers;
    }
    
    /**
     * 执行任务组
     */
    private void executeTaskGroup(ExecutionContext context, 
                                  List<WorkerWrapper<Long, String>> workerWrappers) 
            throws ExecutionException, IOException {
        logger.info("[Orchestrator] 开始执行任务组 - 任务数: {}, 超时: {}秒", 
                workerWrappers.size(), context.getTaskGroupInfo().getExecutorTimeout());
        
        // 预测运行时间
        predictExecutionTime(context, workerWrappers);
        
        // 调用编排引擎执行
        Async.beginWork(context.getTaskGroupInfo().getExecutorTimeout(), 
                (List<WorkerWrapper>) (List<?>) workerWrappers);
    }
    
    /**
     * 预测执行时间
     */
    private void predictExecutionTime(ExecutionContext context, 
                                     List<WorkerWrapper<Long, String>> workerWrappers) 
            throws IOException {
        logger.debug("[Orchestrator] 开始预测执行时间");
        
        List<Long> jobIds = context.getNodes().stream()
                .map(JobNode::getJobId)
                .toList();
        List<JobInfo> jobInfos = adminApiClient.getJobInfos(jobIds);
        
        Map<Long, JobInfo> jobInfoMap = jobInfos.stream()
                .collect(Collectors.toMap(JobInfo::getId, t -> t));
        
        Map<Long, JobInfo> nodeJobInfoMap = new HashMap<>();
        for (JobNode node : context.getNodes()) {
            nodeJobInfoMap.put(node.getId(), jobInfoMap.get(node.getJobId()));
        }
        
        List<Long> startNodeIds = context.getNodes().stream()
                .filter(v -> v.getNodeInDegree().equals(0L))
                .map(JobNode::getId)
                .toList();
        
        String[][] nextRunTime = jobGroupUtils.getNextRunTime(
                workerWrappers, nodeJobInfoMap, 
                context.getTaskGroupInfo().getExecutorTimeout(), 
                startNodeIds, context.getTaskGroupId());
        
        adminApiClient.reportStatus(context.getTaskGroupId(), context.getTaskGroupId(), 
                context.getExecutionBatchId(), 9, JSONUtil.toJsonStr(nextRunTime));
    }
    
    /**
     * 处理执行异常
     */
    private void handleExecutionException(Long taskGroupId, String executionBatchId, ExecutionException e) {
        logger.error("[Orchestrator] 任务组执行异常 - taskGroupId: {}", taskGroupId, e);
        XxlJobHelper.handleFail("任务组执行异常: " + e.getMessage());
        updateStatusOnError(taskGroupId);
    }
    
    /**
     * 处理通用异常
     */
    private void handleGeneralException(Long taskGroupId, String executionBatchId, Exception e) {
        logger.error("[Orchestrator] 任务组执行失败 - taskGroupId: {}", taskGroupId, e);
        XxlJobHelper.handleFail("任务组执行失败: " + e.getMessage());
        updateStatusOnError(taskGroupId);
    }
    
    /**
     * 错误时更新状态
     */
    private void updateStatusOnError(Long taskGroupId) {
        try {
            adminApiClient.updateRankTriggerStatus(taskGroupId, 0);
        } catch (Exception ex) {
            logger.error("[Orchestrator] 异常情况下更新状态失败 - taskGroupId: {}", taskGroupId, ex);
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup(Long taskGroupId, String executionBatchId) {
        logger.info("[Orchestrator] 开始清理资源 - taskGroupId: {}", taskGroupId);
        
        String executeKey = buildExecuteKey(taskGroupId, executionBatchId);
        
        // 移除运行中的任务
        RUNNING_JOBS.remove(executeKey);
        
        // 清理结果映射
        Map<String, Boolean> jobResults = TaskWrapperFactory.getJobResults();
        jobResults.entrySet().removeIf(entry -> entry.getKey().startsWith(taskGroupId + ":"));
        
        // ⚠️ 重要：等待一段时间，确保所有子任务的日志都已经写入完成
        // 子任务执行完成后，日志通过异步回调写入，需要给日志写入留出时间
        // 否则前端收到完成状态后会停止日志轮询，导致日志未完全显示
        waitForLogsToFlush();
        
        // 更新任务组运行状态
        updateTaskGroupStatus(taskGroupId, executionBatchId);
        
        // 清理线程本地变量
        CONTEXT_HOLDER.remove();
        
        logger.info("[Orchestrator] 资源清理完成 - 剩余任务组数量: {}", RUNNING_JOBS.size());
    }
    
    /**
     * 等待日志刷新完成
     * 
     * <p>给子任务的异步日志写入留出时间，确保所有日志都已经写入到文件
     * 这样可以避免前端收到完成状态后停止日志轮询，但日志还未完全写入的问题
     */
    private void waitForLogsToFlush() {
        try {
            // 等待3秒，确保所有子任务的日志回调都已经完成并写入文件
            // 这个时间应该足够大部分异步日志写入完成
            long waitTime = 3000L; // 3秒
            logger.debug("[Orchestrator] 等待日志刷新完成，延迟 {}ms", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[Orchestrator] 等待日志刷新被中断", e);
        }
    }
    
    /**
     * 更新任务组状态
     */
    private void updateTaskGroupStatus(Long taskGroupId, String executionBatchId) {
        try {
            boolean success = adminApiClient.updateRankTriggerStatus(taskGroupId, 0);
            if (success) {
                logger.info("[Orchestrator] 任务组运行状态已更新 - taskGroupId: {}", taskGroupId);
            } else {
                logger.error("[Orchestrator] 任务组运行状态更新失败 - taskGroupId: {}", taskGroupId);
            }
            
            // 上报完成状态
            adminApiClient.reportStatus(taskGroupId, taskGroupId, executionBatchId, 5, "任务组执行完成");
        } catch (Exception e) {
            logger.error("[Orchestrator] 更新任务组状态异常 - taskGroupId: {}", taskGroupId, e);
        }
    }
    
    /**
     * 停止任务组
     */
    public void stopTaskGroup(Long taskGroupId, String executionBatchId) {
        String executeKey = buildExecuteKey(taskGroupId, executionBatchId);
        List<WorkerWrapper<Long, String>> workerWrappers = RUNNING_JOBS.get(executeKey);
        
        if (workerWrappers != null) {
            logger.info("[Orchestrator] 停止任务组 - taskGroupId: {}", taskGroupId);
            Async.stopWork((List<WorkerWrapper>) (List<?>) workerWrappers);
            RUNNING_JOBS.remove(executeKey);
            
            updateTaskGroupStatus(taskGroupId, executionBatchId);
        } else {
            logger.warn("[Orchestrator] 任务组不存在或已完成 - taskGroupId: {}", taskGroupId);
        }
    }
    
    /**
     * 检查任务组是否正在运行
     */
    public boolean isTaskGroupRunning(Long taskGroupId, String executionBatchId) {
        String executeKey = buildExecuteKey(taskGroupId, executionBatchId);
        return RUNNING_JOBS.containsKey(executeKey);
    }
    
    /**
     * 获取所有运行中的任务组
     */
    public Map<String, Boolean> getAllRunningTaskGroups() {
        Map<String, Boolean> result = new HashMap<>();
        for (String executeKey : RUNNING_JOBS.keySet()) {
            result.put(executeKey, true);
        }
        return result;
    }
    
    /**
     * 构建执行键
     */
    private static String buildExecuteKey(Long taskGroupId, String executionBatchId) {
        return taskGroupId + ":" + executionBatchId;
    }
    
    /**
     * 获取 XxlJobContext
     */
    public static XxlJobContext getContext() {
        return CONTEXT_HOLDER.get();
    }
}

