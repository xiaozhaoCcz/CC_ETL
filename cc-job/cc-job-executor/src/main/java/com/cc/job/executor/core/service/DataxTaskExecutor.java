package com.cc.job.executor.core.service;

import com.cc.job.executor.core.service.datax.DataxCommandBuilder;
import com.cc.job.executor.core.service.datax.DataxProcessRunner;
import com.cc.job.executor.core.service.datax.IncrementalDataRefresher;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.executor.utils.DataxUtils;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DataX 任务执行器
 * 
 * <p>负责执行 DataX 数据同步任务
 *
 * @author cc-job-team
 */
@Component
@RequiredArgsConstructor
public class DataxTaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(DataxTaskExecutor.class);
    
    private final JobInfoMapper jobInfoMapper;
    private final DataxCommandBuilder commandBuilder;
    private final DataxProcessRunner processRunner;
    private final IncrementalDataRefresher dataRefresher;
    
    @Value("${cc-job.executor.jsonpath}")
    private String jsonPath;
    
    @Value("${cc-job.pypath}")
    private String dataxPy;
    
    /**
     * 执行 DataX 任务
     * 
     * @param jobId 任务ID
     * @param json DataX配置JSON
     */
    public void execute(Long jobId, String json) {
        logger.info("[DataxTaskExecutor] 开始执行DataX任务 - jobId: {}", jobId);
        
        JobInfo jobInfo = getJobInfo(jobId);
        String tempJsonFile = null;
        
        try {
            // 1. 生成临时JSON文件
            tempJsonFile = DataxUtils.generateTemJsonFile(jsonPath, json);
            
            // 2. 构建DataX命令
            String[] command = commandBuilder.buildCommand(dataxPy, tempJsonFile, jobInfo);
            
            // 3. 执行DataX进程
            int exitCode = processRunner.runProcess(command);
            
            // 4. 处理执行结果
            handleExecutionResult(jobInfo, exitCode);
            
        } catch (Exception e) {
            logger.error("[DataxTaskExecutor] DataX任务执行失败 - jobId: {}", jobId, e);
            XxlJobHelper.log("错误: {}", e.getMessage());
            throw new RuntimeException("DataX任务执行失败", e);
        } finally {
            // 5. 清理临时文件
            if (tempJsonFile != null) {
                DataxUtils.deleteTemJsonFile(tempJsonFile);
            }
        }
    }
    
    /**
     * 获取任务信息
     */
    private JobInfo getJobInfo(Long jobId) {
        JobInfo jobInfo = jobInfoMapper.selectById(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException(ExecutorConstants.ErrorMessage.JOB_NOT_FOUND);
        }
        return jobInfo;
    }
    
    /**
     * 处理执行结果
     */
    private void handleExecutionResult(JobInfo jobInfo, int exitCode) {
        if (exitCode == 0) {
            XxlJobHelper.log("DataX任务执行成功");
            logger.info("[DataxTaskExecutor] DataX任务执行成功 - jobId: {}", jobInfo.getId());
            
            // 如果是增量同步，更新增量标记
            if (jobInfo.getIncrementType() == ExecutorConstants.DataxType.INCREMENTAL) {
                dataRefresher.refreshIncrementalData(jobInfo);
            }
        } else {
            String errorMsg = "DataX任务执行失败，退出码: " + exitCode;
            XxlJobHelper.log(errorMsg);
            logger.error("[DataxTaskExecutor] {} - jobId: {}", errorMsg, jobInfo.getId());
            throw new RuntimeException(errorMsg);
        }
    }
}

