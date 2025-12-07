package com.cc.job.executor.compose.handler;

import com.cc.job.executor.compose.core.orchestrator.TaskGroupOrchestrator;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 任务组编排执行器（完整版）
 * 
 * <p>职责：接收 XXL-Job 调度请求，委托给 TaskGroupOrchestrator 执行
 * 
 * <p>这是一个薄层适配器，主要负责：
 * <ul>
 *   <li>1. 接收 XXL-Job 的调度请求</li>
 *   <li>2. 验证参数</li>
 *   <li>3. 委托给编排器执行</li>
 *   <li>4. 处理结果</li>
 * </ul>
 * 
 * @author cc-job-team
 */
@Component("jobGroupExecutorComplete")
@RequiredArgsConstructor
public class JobGroupExecutorComplete {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupExecutorComplete.class);
    
    private final TaskGroupOrchestrator orchestrator;
    
    /**
     * 任务组执行入口
     * 
     * <p>XXL-Job 会调用此方法来执行任务组
     */
    @XxlJob("runJobGroupXxlJob")
    public void execute() {
        //注册
        if(!orchestrator.updateRegistryWithHttpPort()){
            return;
        }
        // 1. 获取参数
        long taskGroupId = XxlJobHelper.getJobId();
        String executeParam = XxlJobHelper.getJobParam();
        
        logger.info("[JobGroupExecutor] ========== 开始执行任务组 ==========");
        logger.info("[JobGroupExecutor] 任务组ID: {}, 执行参数: {}", taskGroupId, executeParam);
        
        // 2. 验证参数
        if (StringUtils.isBlank(executeParam)) {
            logger.error("[JobGroupExecutor] 执行参数为空");
            XxlJobHelper.handleFail("执行参数为空");
            return;
        }
        
        // 3. 生成批次ID
        String executionBatchId = generateBatchId(taskGroupId, executeParam);
        logger.info("[JobGroupExecutor] 批次ID: {}", executionBatchId);
        
        // 4. 委托给编排器执行
        orchestrator.execute(taskGroupId, executionBatchId);
    }
    
    /**
     * 停止任务组执行
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 批次ID
     */
    public void stopJobGroup(Long taskGroupId, String executionBatchId) {
        logger.info("[JobGroupExecutor] 停止任务组 - taskGroupId: {}, batchId: {}", 
                taskGroupId, executionBatchId);
        orchestrator.stopTaskGroup(taskGroupId, executionBatchId);
    }
    
    /**
     * 查询任务组是否正在运行
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 批次ID
     * @return 是否正在运行
     */
    public boolean isJobGroupRunning(Long taskGroupId, String executionBatchId) {
        return orchestrator.isTaskGroupRunning(taskGroupId, executionBatchId);
    }
    
    /**
     * 获取所有运行中的任务组
     * 
     * @return 运行中的任务组映射
     */
    public Map<String, Boolean> getAllRunningJobGroups() {
        return orchestrator.getAllRunningTaskGroups();
    }
    
    /**
     * 生成批次ID
     * 
     * @param taskGroupId 任务组ID
     * @param executeParam 执行参数
     * @return 批次ID
     */
    private String generateBatchId(long taskGroupId, String executeParam) {
        return String.valueOf(taskGroupId).equals(executeParam) 
                ? UUID.randomUUID().toString() 
                : executeParam;
    }
}
