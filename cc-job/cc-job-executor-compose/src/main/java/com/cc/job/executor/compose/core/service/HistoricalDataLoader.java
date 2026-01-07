package com.cc.job.executor.compose.core.service;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.executor.compose.core.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史数据加载服务
 * 
 * <p>负责从数据库加载历史执行结果数据，并写入到数据上下文中
 * 
 * @author cc-job-team
 * @since 2026-01-06
 */
@Component
public class HistoricalDataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataLoader.class);
    
    private final AdminApiClient adminApiClient;
    private final FileStorageService fileStorageService;
    
    public HistoricalDataLoader(AdminApiClient adminApiClient, FileStorageService fileStorageService) {
        this.adminApiClient = adminApiClient;
        this.fileStorageService = fileStorageService;
    }
    
    /**
     * 从数据库加载指定批次的历史数据
     * 
     * @param context 执行上下文
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @return 加载的节点数量
     */
    public int loadFromDatabase(ExecutionContext context, Long taskGroupId, String executionBatchId) {
        if (context == null || context.getDataContext() == null) {
            logger.warn("[HistoricalDataLoader] 执行上下文或数据上下文为空，无法加载历史数据");
            return 0;
        }
        
        if (executionBatchId == null || executionBatchId.isEmpty()) {
            logger.warn("[HistoricalDataLoader] 执行批次ID为空，无法加载历史数据");
            return 0;
        }
        
        try {
            logger.info("[HistoricalDataLoader] 开始从数据库加载历史数据 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId);
            
            // 从Admin API获取历史数据
            List<Map<String, Object>> nodeResults = adminApiClient.getNodeResultsByBatch(taskGroupId, executionBatchId);
            
            if (nodeResults == null || nodeResults.isEmpty()) {
                logger.info("[HistoricalDataLoader] 未找到历史数据 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return 0;
            }
            
            DataContext dataContext = context.getDataContext();
            
            int loadedCount = 0;
            for (Map<String, Object> nodeResult : nodeResults) {
                try {
                    Long jobId = Long.valueOf(nodeResult.get("jobId").toString());
                    String jobName = nodeResult.get("jobName") != null ? nodeResult.get("jobName").toString() : null;
                    String filePath = nodeResult.get("filePath") != null ? nodeResult.get("filePath").toString() : null;
                    String resultData = nodeResult.get("resultData") != null ? nodeResult.get("resultData").toString() : null;
                    
                    String jsonData = null;
                    String dataSource = null;
                    
                    // 优先从文件读取
                    if (filePath != null && !filePath.isEmpty() && fileStorageService != null) {
                        jsonData = fileStorageService.readFromFile(filePath);
                        if (jsonData != null && !jsonData.isEmpty()) {
                            dataSource = "file";
                            logger.debug("[HistoricalDataLoader] 从文件读取数据成功 - jobId: {}, filePath: {}", jobId, filePath);
                        } else {
                            logger.warn("[HistoricalDataLoader] 文件不存在或读取失败，尝试从数据库读取 - jobId: {}, filePath: {}", jobId, filePath);
                        }
                    }
                    
                    // 如果文件读取失败，回退到数据库的result_data字段
                    if ((jsonData == null || jsonData.isEmpty()) && resultData != null && !resultData.isEmpty()) {
                        jsonData = resultData;
                        dataSource = "database";
                        logger.debug("[HistoricalDataLoader] 从数据库字段读取数据 - jobId: {}", jobId);
                    }
                    
                    if (jsonData == null || jsonData.isEmpty()) {
                        logger.debug("[HistoricalDataLoader] 节点结果数据为空，跳过 - jobId: {}", jobId);
                        continue;
                    }
                    
                    // 规范化任务名称
                    String normalizedJobName = normalizeJobName(jobName, jobId);
                    
                    // 解析结果数据（JSON格式）
                    Object parsedResult = JSONUtil.parse(jsonData);
                    
                    // 将数据存储到DataContext，标记为DATABASE来源
                    storeResultToContext(dataContext, normalizedJobName, parsedResult, jobId);
                    
                    loadedCount++;
                    logger.debug("[HistoricalDataLoader] 加载节点历史数据成功 - jobId: {}, jobName: {}, source: {}", 
                            jobId, normalizedJobName, dataSource);
                } catch (Exception e) {
                    logger.error("[HistoricalDataLoader] 加载单个节点历史数据失败 - nodeResult: {}", 
                            nodeResult, e);
                }
            }
            
            logger.info("[HistoricalDataLoader] 历史数据加载完成 - taskGroupId: {}, batchId: {}, 加载数量: {}", 
                    taskGroupId, executionBatchId, loadedCount);
            return loadedCount;
            
        } catch (Exception e) {
            logger.error("[HistoricalDataLoader] 从数据库加载历史数据异常 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId, e);
            return 0;
        }
    }
    
    /**
     * 加载最近一次执行批次的历史数据
     * 
     * @param context 执行上下文
     * @param taskGroupId 任务组ID
     * @return 加载的节点数量，如果未找到最近一次执行则返回0
     */
    public int loadLatestBatch(ExecutionContext context, Long taskGroupId) {
        if (context == null || context.getDataContext() == null) {
            logger.warn("[HistoricalDataLoader] 执行上下文或数据上下文为空，无法加载历史数据");
            return 0;
        }
        
        try {
            // 获取最近一次执行的批次ID
            String latestBatchId = adminApiClient.getLatestBatchId(taskGroupId);
            
            if (latestBatchId == null || latestBatchId.isEmpty()) {
                logger.info("[HistoricalDataLoader] 未找到最近一次执行的批次 - taskGroupId: {}", taskGroupId);
                return 0;
            }
            
            logger.info("[HistoricalDataLoader] 找到最近一次执行的批次 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, latestBatchId);
            
            return loadFromDatabase(context, taskGroupId, latestBatchId);
            
        } catch (Exception e) {
            logger.error("[HistoricalDataLoader] 加载最近一次执行批次数据异常 - taskGroupId: {}", taskGroupId, e);
            return 0;
        }
    }
    
    /**
     * 将结果数据存储到数据上下文（标记为DATABASE来源）
     */
    private void storeResultToContext(DataContext context, String jobName, Object result, Long jobId) {
        if (result == null) {
            return;
        }
        
        // 根据结果类型存储（与ResultStorageService相同的逻辑）
        if (result instanceof Map) {
            storeMapResult(context, jobName, (Map<String, Object>) result, jobId);
        } else if (result instanceof List) {
            storeListResult(context, jobName, (List<Object>) result, jobId);
        } else if (result instanceof String) {
            storeStringResult(context, jobName, (String) result, jobId);
        } else {
            storeSimpleResult(context, jobName, result, jobId);
        }
    }
    
    /**
     * 存储 Map 类型结果
     */
    private void storeMapResult(DataContext context, String jobName, Map<String, Object> result, Long jobId) {
        context.putFromDatabase(jobName + ".result", result, jobId);
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String dataKey = jobName + "." + key;
            context.putFromDatabase(dataKey, value, jobId);
        }
    }
    
    /**
     * 存储 List 类型结果
     */
    private void storeListResult(DataContext context, String jobName, List<Object> result, Long jobId) {
        context.putFromDatabase(jobName + ".list", result, jobId);
        context.putFromDatabase(jobName + ".result", result, jobId);
        if (result.size() == 1) {
            context.putFromDatabase(jobName + ".value", result.get(0), jobId);
        }
    }
    
    /**
     * 存储 String 类型结果
     */
    private void storeStringResult(DataContext context, String jobName, String result, Long jobId) {
        context.putFromDatabase(jobName + ".result", result, jobId);
        context.putFromDatabase(jobName + ".value", result, jobId);
    }
    
    /**
     * 存储简单类型结果
     */
    private void storeSimpleResult(DataContext context, String jobName, Object result, Long jobId) {
        context.putFromDatabase(jobName + ".value", result, jobId);
        context.putFromDatabase(jobName + ".result", result, jobId);
    }
    
    /**
     * 规范化任务名称
     */
    private String normalizeJobName(String jobDesc, Long jobId) {
        String jobName;
        
        if (jobDesc == null || jobDesc.trim().isEmpty()) {
            jobName = "job_" + jobId;
        } else {
            jobName = jobDesc.trim();
        }
        
        jobName = jobName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        jobName = jobName.replaceAll("_{2,}", "_");
        jobName = jobName.replaceAll("^_+|_+$", "");
        
        if (jobName.isEmpty()) {
            jobName = "job_" + jobId;
        }
        
        return jobName;
    }
}

