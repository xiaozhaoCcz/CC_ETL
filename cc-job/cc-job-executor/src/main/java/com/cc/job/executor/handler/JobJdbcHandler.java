package com.cc.job.executor.handler;

import com.cc.job.executor.core.service.JdbcTaskExecutor;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobJdbcDatasourceMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JDBC 任务处理器
 * 
 * <p>负责接收 XXL-Job 调度请求，执行 JDBC SQL 任务
 *
 * @author cc-job-team
 */
@Component
public class JobJdbcHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(JobJdbcHandler.class);
    
    private final JobJdbcDatasourceMapper datasourceMapper;
    private final JobInfoMapper jobInfoMapper;
    private final JdbcTaskExecutor jdbcTaskExecutor;

    public JobJdbcHandler(JobJdbcDatasourceMapper datasourceMapper, JobInfoMapper jobInfoMapper, JdbcTaskExecutor jdbcTaskExecutor) {
        this.datasourceMapper = datasourceMapper;
        this.jobInfoMapper = jobInfoMapper;
        this.jdbcTaskExecutor = jdbcTaskExecutor;
    }
    
    /**
     * 执行 JDBC 任务
     */
    @XxlJob("runJobJdbcXxlJob")
    public void runJobJdbcXxlJob() {
        long jobId = XxlJobHelper.getJobId();
        logger.info("[JobJdbcHandler] 开始执行JDBC任务 - jobId: {}", jobId);
        
        try {
            // 1. 获取任务信息
            JobInfo jobInfo = getJobInfo(jobId);
            
            // 2. 获取数据源信息
            JobJdbcDatasource datasource = getDatasource(jobInfo);
            
            // 3. 委托给 JdbcTaskExecutor 执行
            jdbcTaskExecutor.execute(jobInfo, datasource);
            
            XxlJobHelper.log("JDBC任务执行成功");
            logger.info("[JobJdbcHandler] JDBC任务执行完成 - jobId: {}", jobId);
            
        } catch (Exception e) {
            logger.error("[JobJdbcHandler] JDBC任务执行失败 - jobId: {}", jobId, e);
            XxlJobHelper.log("错误: {}", e.getMessage());
            throw new RuntimeException("JDBC任务执行失败", e);
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
                    throw new BusinessException(ExecutorConstants.ErrorMessage.JOB_NOT_FOUND + ": " + jobId);
                }
                
                // 使用 TriggerParam 中已解析的参数覆盖数据库中的参数
                // 优先级：TriggerParam 中的参数 > 数据库中的参数
                if (triggerParam.getExecutorParams() != null) {
                    jobInfo.setExecutorParam(triggerParam.getExecutorParams());
                    logger.debug("[JobJdbcHandler] 使用 TriggerParam 中的 executorParam: {}", triggerParam.getExecutorParams());
                }
                
                return jobInfo;
            }
        }
        
        // 2. 如果没有 TriggerParam，从数据库获取（向后兼容）
        JobInfo jobInfo = jobInfoMapper.selectById(jobId);
        if (jobInfo == null) {
            throw new BusinessException(ExecutorConstants.ErrorMessage.JOB_NOT_FOUND + ": " + jobId);
        }
        logger.debug("[JobJdbcHandler] 从数据库获取任务信息 - jobId: {}", jobId);
        return jobInfo;
    }
    
    /**
     * 获取数据源信息
     */
    private JobJdbcDatasource getDatasource(JobInfo jobInfo) {
        JobJdbcDatasource datasource = datasourceMapper.selectById(jobInfo.getJdbcDatasourceId());
        if (datasource == null) {
            throw new BusinessException(ExecutorConstants.ErrorMessage.DATASOURCE_NOT_FOUND);
        }
        return datasource;
    }
}
