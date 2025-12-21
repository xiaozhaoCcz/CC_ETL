package com.cc.job.executor.compose.core.service;

import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.executor.compose.engine.callback.ICallback;
import com.cc.job.executor.compose.engine.callback.IWorker;
import com.cc.job.executor.compose.engine.worker.WorkResult;
import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;
import com.cc.job.executor.compose.service.JobExecutionMonitor;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.cc.job.executor.compose.infrastructure.constant.ExecutorConstants.ExecutionResult.*;

/**
 * 任务包装器工厂
 * 
 * <p>负责创建 WorkerWrapper 实例
 *
 * @author cc-job-team
 */
@Component
public class TaskWrapperFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskWrapperFactory.class);
    
    private final AdminApiClient adminApiClient;
    private final TaskExecutor taskExecutor;

    public TaskWrapperFactory(AdminApiClient adminApiClient, TaskExecutor taskExecutor) {
        this.adminApiClient = adminApiClient;
        this.taskExecutor = taskExecutor;
    }
    
    /** 存储任务执行结果 */
    private static final Map<String, Boolean> JOB_RESULTS = new ConcurrentHashMap<>();
    
    /** 存储任务执行监听器 */
    private static final Map<String, JobExecutionMonitor> MONITOR_MAP = new ConcurrentHashMap<>();
    
    /**
     * 创建 WorkerWrapper 列表
     * 
     * @param context 执行上下文
     * @return WorkerWrapper列表
     */
    public List<WorkerWrapper<Long, String>> createWorkerWrappers(ExecutionContext context) {
        logger.debug("[TaskWrapperFactory] 开始创建 WorkerWrapper - 节点数: {}", 
                context.getNodes().size());
        
        List<WorkerWrapper<Long, String>> result = new ArrayList<>();
        Map<Long, JobInfo> jobInfoMap = getJobInfoMap(context.getNodes());
        
        for (JobNode node : context.getNodes()) {
            JobInfo jobInfo = jobInfoMap.get(node.getJobId());
            if (jobInfo == null) {
                logger.warn("[TaskWrapperFactory] 节点对应的任务不存在 - nodeId: {}, jobId: {}", 
                        node.getId(), node.getJobId());
                continue;
            }
            
            WorkerWrapper<Long, String> worker = createWorker(
                    context, node, jobInfo, jobInfoMap);
            result.add(worker);
        }
        
        logger.info("[TaskWrapperFactory] WorkerWrapper创建完成 - 数量: {}", result.size());
        return result;
    }
    
    /**
     * 创建单个 Worker
     */
    private WorkerWrapper<Long, String> createWorker(ExecutionContext context, JobNode node, 
                                                    JobInfo jobInfo, Map<Long, JobInfo> jobInfoMap) {
        return new WorkerWrapper<Long, String>()
                .id(String.valueOf(node.getId()))
                .param(node.getJobId())
                .timeout(jobInfo.getExecutorTimeout())
                .retryCount(jobInfo.getExecutorFailRetryCount())
                .worker(createWorkerAction(context, node, jobInfo))
                .callback(createWorkerCallback(context, node, jobInfo));
    }
    
    /**
     * 创建 Worker 执行动作
     */
    private IWorker<Long, String> createWorkerAction(ExecutionContext context, 
                                                     JobNode node, JobInfo jobInfo) {
        return new IWorker<Long, String>() {
            @Override
            public String action(Long jobId, Map<String, WorkerWrapper> allWrappers) {
                WorkerWrapper currentWrapper = allWrappers.get(String.valueOf(node.getId()));
                int retryCount = currentWrapper != null ? currentWrapper.getCount() : 0;
                
                return taskExecutor.executeTask(context, node, jobInfo, retryCount, JOB_RESULTS);
            }
            
            @Override
            public String defaultValue() {
                reportStatus(context, jobInfo.getId(), 0, "任务执行失败");
                return FAIL_COMPLETE;
            }
        };
    }
    
    /**
     * 创建 Worker 回调
     */
    private ICallback<Long, String> createWorkerCallback(ExecutionContext context, 
                                                         JobNode node, JobInfo jobInfo) {
        return new ICallback<Long, String>() {
            private long startTime;
            
            @Override
            public void begin(Long jobId) {
                startTime = System.currentTimeMillis();
                logger.info("[TaskWorker] 任务开始执行 - jobId: {}, nodeId: {}", 
                        jobId, node.getId());
                XxlJobHelper.log(context.getXxlJobContext(), 
                        "任务开始执行 - jobId: {}, nodeId: {}", jobId, node.getId());
                
                reportStatus(context, jobId, 2, "任务开始执行");
            }
            
            @Override
            public void result(boolean success, Long jobId, WorkResult<String> workResult) {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("[TaskWorker] 任务执行完成 - jobId: {}, 成功: {}, 耗时: {}ms", 
                        jobId, success, duration);
                XxlJobHelper.log(context.getXxlJobContext(), 
                        "任务执行完成 - jobId: {}, 成功: {}, 耗时: {}ms", jobId, success, duration);
                
                int status = success ? 1 : 0;
                reportStatus(context, jobId, status, success ? "任务执行成功" : "任务执行失败");
            }
        };
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
    
    /**
     * 上报任务状态
     */
    private void reportStatus(ExecutionContext context, Long jobId, Integer status, String message) {
        try {
            adminApiClient.reportStatus(context.getTaskGroupId(), jobId, 
                    context.getExecutionBatchId(), status, message);
        } catch (Exception e) {
            logger.error("[TaskWrapperFactory] 上报状态失败 - jobId: {}, status: {}", 
                    jobId, status, e);
        }
    }
    
    /**
     * 获取任务执行结果
     */
    public static Map<String, Boolean> getJobResults() {
        return JOB_RESULTS;
    }
    
    /**
     * 注册任务监听器
     */
    public static void registerMonitor(String executeKey, JobExecutionMonitor monitor) {
        MONITOR_MAP.put(executeKey, monitor);
        logger.debug("[TaskWrapperFactory] 注册任务监听器 - executeKey: {}", executeKey);
    }
    
    /**
     * 移除任务监听器
     */
    public static void unregisterMonitor(String executeKey) {
        MONITOR_MAP.remove(executeKey);
        logger.debug("[TaskWrapperFactory] 移除任务监听器 - executeKey: {}", executeKey);
    }
    
    /**
     * 通知任务完成
     */
    public static void notifyTaskComplete(String executeKey) {
        JobExecutionMonitor monitor = MONITOR_MAP.get(executeKey);
        if (monitor != null) {
            monitor.getLatch().countDown();
            logger.debug("[TaskWrapperFactory] 通知任务完成 - executeKey: {}", executeKey);
        }
    }
}

