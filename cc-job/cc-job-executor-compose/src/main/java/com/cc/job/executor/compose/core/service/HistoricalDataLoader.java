package com.cc.job.executor.compose.core.service;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
            List<Map<String, Object>> nodeResults = adminApiClient.getNodeResultsByBatch(taskGroupId, executionBatchId, context.getInstanceKey());
            
            if (nodeResults == null || nodeResults.isEmpty()) {
                logger.info("[HistoricalDataLoader] 未找到历史数据 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return 0;
            }
            
            DataContext dataContext = context.getDataContext();
            
            int loadedCount = 0;
            for (Map<String, Object> nodeResult : nodeResults) {
                Long logJobId = null;
                String logJsonData = null;
                try {
                    Long jobId = Long.valueOf(nodeResult.get("jobId").toString());
                    logJobId = jobId;
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
                    logJsonData = jsonData;
                    
                    // 规范化任务名称
                    String normalizedJobName = normalizeJobName(jobName, jobId);
                    
                    // 规范化 JSON 字符串：trim 并去掉 UTF-8 BOM，避免从文件读取时解析失败
                    String trimmed = jsonData.trim().replaceFirst("^\uFEFF", "");
                    if (trimmed.isEmpty()) {
                        logger.debug("[HistoricalDataLoader] 节点结果数据为空（trim 后），跳过 - jobId: {}", jobId);
                        continue;
                    }
                    // 按首字符分流解析：对象 / 数组 / 其它（字符串、数字等）
                    Object parsedResult;
                    if (trimmed.startsWith("{")) {
                        parsedResult = JSONUtil.parseObj(trimmed);
                    } else if (trimmed.startsWith("[")) {
                        parsedResult = JSONUtil.parseArray(trimmed);
                    } else {
                        try {
                            parsedResult = JSONUtil.parse(trimmed);
                        } catch (Exception parseEx) {
                            // 非 JSON 格式的普通数据（如纯文本），直接当字符串存储，避免解析报错
                            parsedResult = trimmed;
                            logger.debug("[HistoricalDataLoader] 内容非标准 JSON，按原文字符串存储 - jobId: {}", jobId);
                        }
                    }
                    
                    // 将数据存储到DataContext，标记为DATABASE来源
                    storeResultToContext(dataContext, normalizedJobName, parsedResult, jobId);
                    
                    loadedCount++;
                    logger.debug("[HistoricalDataLoader] 加载节点历史数据成功 - jobId: {}, jobName: {}, source: {}", 
                            jobId, normalizedJobName, dataSource);
                } catch (Exception e) {
                    String dataSnippet = logJsonData != null && !logJsonData.isEmpty()
                            ? (logJsonData.length() > 100 ? logJsonData.substring(0, 100) + "..." : logJsonData)
                            : "null";
                    logger.error("[HistoricalDataLoader] 加载单个节点历史数据失败 - jobId: {}, 数据摘要: {} - nodeResult: {}", 
                            logJobId, dataSnippet, nodeResult, e);
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
     * 加载单条节点结果到上下文（用于按节点补全缺失上游）
     *
     * @param dataContext 数据上下文
     * @param taskGroupId 任务组ID
     * @param nodeResult 单条节点结果 Map（含 jobId, jobName, resultData, filePath）
     * @return 是否加载成功
     */
    public boolean loadSingleNodeResult(DataContext dataContext, Long taskGroupId, Map<String, Object> nodeResult) {
        if (dataContext == null || nodeResult == null) {
            return false;
        }
        try {
            Long jobId = Long.valueOf(nodeResult.get("jobId").toString());
            String jobName = nodeResult.get("jobName") != null ? nodeResult.get("jobName").toString() : null;
            String filePath = nodeResult.get("filePath") != null ? nodeResult.get("filePath").toString() : null;
            String resultData = nodeResult.get("resultData") != null ? nodeResult.get("resultData").toString() : null;

            String jsonData = null;
            if (filePath != null && !filePath.isEmpty() && fileStorageService != null) {
                jsonData = fileStorageService.readFromFile(filePath);
            }
            if ((jsonData == null || jsonData.isEmpty()) && resultData != null && !resultData.isEmpty()) {
                jsonData = resultData;
            }
            if (jsonData == null || jsonData.isEmpty()) {
                return false;
            }

            String normalizedJobName = normalizeJobName(jobName, jobId);
            String trimmed = jsonData.trim().replaceFirst("^\uFEFF", "");
            if (trimmed.isEmpty()) {
                return false;
            }
            Object parsedResult;
            if (trimmed.startsWith("{")) {
                parsedResult = JSONUtil.parseObj(trimmed);
            } else if (trimmed.startsWith("[")) {
                parsedResult = JSONUtil.parseArray(trimmed);
            } else {
                try {
                    parsedResult = JSONUtil.parse(trimmed);
                } catch (Exception parseEx) {
                    parsedResult = trimmed;
                }
            }
            storeResultToContext(dataContext, normalizedJobName, parsedResult, jobId);
            logger.debug("[HistoricalDataLoader] 单节点结果加载成功 - jobId: {}, jobName: {}", jobId, normalizedJobName);
            return true;
        } catch (Exception e) {
            logger.warn("[HistoricalDataLoader] 单节点结果加载失败 - nodeResult: {}", nodeResult, e);
            return false;
        }
    }

    /**
     * 补全缺失的上游节点结果（方案二：按节点拉取最近一次结果填入上下文）
     *
     * @param context 执行上下文
     * @param taskGroupId 任务组ID
     * @param ancestorJobIdToJobName 上游 jobId -> 节点名称（用于规范化与校验是否存在）
     */
    public int fillMissingAncestors(ExecutionContext context, Long taskGroupId,
                                    Map<Long, String> ancestorJobIdToJobName) {
        if (context == null || context.getDataContext() == null || ancestorJobIdToJobName == null || ancestorJobIdToJobName.isEmpty()) {
            return 0;
        }
        DataContext dataContext = context.getDataContext();
        int filled = 0;
        for (Map.Entry<Long, String> entry : ancestorJobIdToJobName.entrySet()) {
            Long jobId = entry.getKey();
            String jobName = entry.getValue();
            String normalizedJobName = normalizeJobName(jobName, jobId);
            if (dataContext.get(normalizedJobName + ".result") != null) {
                continue;
            }
            Map<String, Object> nodeResult = adminApiClient.getLatestNodeResult(taskGroupId, jobId, context.getInstanceKey());
            if (nodeResult != null && loadSingleNodeResult(dataContext, taskGroupId, nodeResult)) {
                filled++;
            }
        }
        if (filled > 0) {
            logger.info("[HistoricalDataLoader] 补全缺失上游节点结果 - taskGroupId: {}, 补全数量: {}", taskGroupId, filled);
        }
        return filled;
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
            String latestBatchId = adminApiClient.getLatestBatchId(taskGroupId, context.getInstanceKey());
            
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
     * 从指定批次中只加载给定节点的历史结果。
     */
    public int loadSpecificJobResults(ExecutionContext context, Long taskGroupId,
                                      String executionBatchId, Set<Long> jobIds) {
        if (context == null || context.getDataContext() == null || jobIds == null || jobIds.isEmpty()) {
            return 0;
        }
        List<Map<String, Object>> nodeResults = adminApiClient.getNodeResultsByBatch(taskGroupId, executionBatchId, context.getInstanceKey());
        if (nodeResults == null || nodeResults.isEmpty()) {
            return 0;
        }

        int loadedCount = 0;
        for (Map<String, Object> nodeResult : nodeResults) {
            Object jobIdObj = nodeResult.get("jobId");
            if (jobIdObj == null) {
                continue;
            }
            Long jobId = Long.valueOf(jobIdObj.toString());
            if (!jobIds.contains(jobId)) {
                continue;
            }
            if (loadSingleNodeResult(context.getDataContext(), taskGroupId, nodeResult)) {
                loadedCount++;
            }
        }
        return loadedCount;
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
            Map<String, Object> resultMap = (Map<String, Object>) result;
            if (isNodeResultMap(resultMap)) {
                storeNodeResultMap(context, jobName, resultMap, jobId);
            } else {
                storeMapResult(context, jobName, resultMap, jobId);
            }
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

    private boolean isNodeResultMap(Map<String, Object> result) {
        return result.containsKey("success")
                || result.containsKey("data")
                || result.containsKey("apiResult")
                || result.containsKey("sqlResult")
                || result.containsKey("beanResult")
                || result.containsKey("glueResult");
    }

    private void storeNodeResultMap(DataContext context, String jobName, Map<String, Object> result, Long jobId) {
        context.putFromDatabase(jobName, result, jobId);
        context.putFromDatabase(jobName + ".result", result, jobId);

        putIfPresent(context, jobName + ".code", result.get("code"), jobId);
        putIfPresent(context, jobName + ".message", result.get("message"), jobId);
        putIfPresent(context, jobName + ".success", result.get("success"), jobId);
        putIfPresent(context, jobName + ".duration", result.get("duration"), jobId);
        putIfPresent(context, jobName + ".timestamp", result.get("timestamp"), jobId);
        putIfPresent(context, jobName + ".data", result.get("data"), jobId);

        Object sqlResult = result.get("sqlResult");
        if (sqlResult instanceof Map) {
            storeSqlResultMap(context, jobName, (Map<String, Object>) sqlResult, jobId);
        }

        Object apiResult = result.get("apiResult");
        if (apiResult instanceof Map) {
            storeApiResultMap(context, jobName, (Map<String, Object>) apiResult, jobId);
        }

        Object beanResult = result.get("beanResult");
        if (beanResult instanceof Map) {
            storeBeanResultMap(context, jobName, (Map<String, Object>) beanResult, jobId);
        }

        Object glueResult = result.get("glueResult");
        if (glueResult instanceof Map) {
            storeGlueResultMap(context, jobName, (Map<String, Object>) glueResult, jobId);
        }
    }

    private void storeSqlResultMap(DataContext context, String jobName, Map<String, Object> sqlResult, Long jobId) {
        context.putFromDatabase(jobName + ".sqlResult", sqlResult, jobId);
        putIfPresent(context, jobName + ".data", sqlResult.get("data"), jobId);
        putIfPresent(context, jobName + ".count", sqlResult.get("count"), jobId);
        putIfPresent(context, jobName + ".affectedRows", sqlResult.get("affectedRows"), jobId);
        putIfPresent(context, jobName + ".columns", sqlResult.get("columns"), jobId);
        putIfPresent(context, jobName + ".columnTypes", sqlResult.get("columnTypes"), jobId);
        putIfPresent(context, jobName + ".sqlType", sqlResult.get("sqlType"), jobId);
        putIfPresent(context, jobName + ".executedSql", sqlResult.get("executedSql"), jobId);
    }

    private void storeApiResultMap(DataContext context, String jobName, Map<String, Object> apiResult, Long jobId) {
        context.putFromDatabase(jobName + ".apiResult", apiResult, jobId);
        context.putFromDatabase(jobName + ".response", apiResult, jobId);
        putIfPresent(context, jobName + ".response.statusCode", apiResult.get("statusCode"), jobId);
        putIfPresent(context, jobName + ".response.body", apiResult.get("body"), jobId);
        putIfPresent(context, jobName + ".response.rawBody", apiResult.get("rawBody"), jobId);
        putIfPresent(context, jobName + ".response.headers", apiResult.get("headers"), jobId);
        putIfPresent(context, jobName + ".request.url", apiResult.get("requestUrl"), jobId);
        putIfPresent(context, jobName + ".request.method", apiResult.get("requestMethod"), jobId);
        putIfPresent(context, jobName + ".responseTime", apiResult.get("responseTime"), jobId);
    }

    private void storeBeanResultMap(DataContext context, String jobName, Map<String, Object> beanResult, Long jobId) {
        context.putFromDatabase(jobName + ".beanResult", beanResult, jobId);
        putIfPresent(context, jobName + ".value", beanResult.get("value"), jobId);
        putIfPresent(context, jobName + ".methodName", beanResult.get("methodName"), jobId);
        putIfPresent(context, jobName + ".className", beanResult.get("className"), jobId);
        putIfPresent(context, jobName + ".returnType", beanResult.get("returnType"), jobId);
    }

    private void storeGlueResultMap(DataContext context, String jobName, Map<String, Object> glueResult, Long jobId) {
        context.putFromDatabase(jobName + ".glueResult", glueResult, jobId);
        putIfPresent(context, jobName + ".output", glueResult.get("output"), jobId);
        putIfPresent(context, jobName + ".error", glueResult.get("error"), jobId);
        putIfPresent(context, jobName + ".exitCode", glueResult.get("exitCode"), jobId);
        putIfPresent(context, jobName + ".result", glueResult.get("result"), jobId);
        putIfPresent(context, jobName + ".scriptType", glueResult.get("scriptType"), jobId);
        putIfPresent(context, jobName + ".logOutput", glueResult.get("logOutput"), jobId);
    }

    private void putIfPresent(DataContext context, String key, Object value, Long jobId) {
        if (value != null) {
            context.putFromDatabase(key, value, jobId);
        }
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

