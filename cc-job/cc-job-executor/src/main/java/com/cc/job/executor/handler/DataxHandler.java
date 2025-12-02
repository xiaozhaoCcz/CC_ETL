package com.cc.job.executor.handler;

import com.cc.job.executor.core.service.DataxTaskExecutor;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DataX 任务处理器
 * 
 * <p>负责接收 XXL-Job 调度请求，执行 DataX 数据同步任务
 *
 * @author cc-job-team
 */
@Component
@RequiredArgsConstructor
public class DataxHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DataxHandler.class);
    
    private final DataxTaskExecutor dataxTaskExecutor;
    
    /**
     * 执行 DataX 任务
     */
    @XxlJob("runDataxHandler")
    public void runDataxHandler() {
        Long jobId = XxlJobHelper.getJobId();
        String json = XxlJobHelper.getJobParam();
        
        logger.info("[DataxHandler] 开始执行DataX任务 - jobId: {}", jobId);
        
        try {
            // 委托给 DataxTaskExecutor 执行
            dataxTaskExecutor.execute(jobId, json);
            
            logger.info("[DataxHandler] DataX任务执行完成 - jobId: {}", jobId);
            
        } catch (Exception e) {
            logger.error("[DataxHandler] DataX任务执行失败 - jobId: {}", jobId, e);
            throw new RuntimeException("DataX任务执行失败", e);
        }
    }
}
