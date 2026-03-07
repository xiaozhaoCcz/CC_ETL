package com.cc.job.executor.compose.core.service;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.context.DataSourceType;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.result.NodeResult;
import com.cc.job.xo.model.result.SqlResult;
import com.cc.job.xo.model.result.ApiResult;
import com.cc.job.xo.model.result.BeanResult;
import com.cc.job.xo.model.result.GlueResult;
import com.xxl.job.core.glue.GlueTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 结果存储服务
 * 
 * <p>负责将任务执行结果存储到数据上下文中，供后续任务使用
 * 
 * <p>存储策略：
 * <ul>
 *   <li>如果结果是 Map，存储整个 Map，并支持 jobName.key 访问</li>
 *   <li>如果结果是 String，存储为 jobName.result</li>
 *   <li>如果结果是 List，存储为 jobName.list，并支持索引访问</li>
 *   <li>如果结果是简单类型，存储为 jobName.value</li>
 * </ul>
 * 
 * @author cc-job-team
 */
@Component
public class ResultStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultStorageService.class);
    
    private final AdminApiClient adminApiClient;
    private final FileStorageService fileStorageService;
    
    public ResultStorageService(AdminApiClient adminApiClient, FileStorageService fileStorageService) {
        this.adminApiClient = adminApiClient;
        this.fileStorageService = fileStorageService;
    }
    
    /**
     * 存储任务执行结果到数据上下文
     * 
     * @param context 执行上下文
     * @param jobInfo 任务信息
     * @param executeResult 执行结果
     */
    public void storeResult(ExecutionContext context, JobInfo jobInfo, Object executeResult) {
        if (context == null || jobInfo == null) {
            logger.warn("[ResultStorage] 上下文或任务信息为空，无法存储结果");
            return;
        }
        
        if (executeResult == null) {
            logger.debug("[ResultStorage] 任务执行结果为空，跳过存储 - jobId: {}", jobInfo.getId());
            return;
        }
        
        // 获取数据上下文
        DataContext dataContext = context.getDataContext();
        if (dataContext == null) {
            logger.warn("[ResultStorage] 数据上下文为空，无法存储结果 - jobId: {}", jobInfo.getId());
            return;
        }
        
        // 规范化任务名称
        String jobName = normalizeJobName(jobInfo.getJobDesc(), jobInfo.getId());
        
        logger.info("[ResultStorage] 开始存储任务结果 - jobId: {}, jobName: {}, resultType: {}", 
                jobInfo.getId(), jobName, executeResult.getClass().getSimpleName());
        
        // 如果已经是NodeResult，直接存储
        if (executeResult instanceof NodeResult) {
            storeNodeResult(dataContext, jobName, (NodeResult) executeResult, jobInfo.getId());
        } else {
            // 尝试转换为NodeResult（向后兼容）
            NodeResult nodeResult = convertToNodeResult(executeResult, jobInfo);
            if (nodeResult != null) {
                storeNodeResult(dataContext, jobName, nodeResult, jobInfo.getId());
            } else {
                // 无法转换，使用旧方式存储
                if (executeResult instanceof Map) {
                    storeMapResult(dataContext, jobName, (Map<String, Object>) executeResult, jobInfo.getId());
                } else if (executeResult instanceof List) {
                    storeListResult(dataContext, jobName, (List<Object>) executeResult, jobInfo.getId());
                } else if (executeResult instanceof String) {
                    storeStringResult(dataContext, jobName, (String) executeResult, jobInfo.getId());
                } else {
                    // 简单类型或其他类型
                    storeSimpleResult(dataContext, jobName, executeResult, jobInfo.getId());
                }
            }
        }
        
        logger.debug("[ResultStorage] 任务结果存储完成 - jobId: {}, jobName: {}", 
                jobInfo.getId(), jobName);
    }
    
    /**
     * 存储NodeResult结果
     */
    private void storeNodeResult(DataContext context, String jobName, NodeResult nodeResult, Long jobId) {
        // 存储整个NodeResult对象
        context.put(jobName, nodeResult, jobId);
        
        // 存储通用属性，方便直接访问
        if (nodeResult.getCode() != null) {
            context.put(jobName + ".code", nodeResult.getCode(), jobId);
        }
        if (nodeResult.getMessage() != null) {
            context.put(jobName + ".message", nodeResult.getMessage(), jobId);
        }
        if (nodeResult.getSuccess() != null) {
            context.put(jobName + ".success", nodeResult.getSuccess(), jobId);
        }
        if (nodeResult.getDuration() != null) {
            context.put(jobName + ".duration", nodeResult.getDuration(), jobId);
        }
        if (nodeResult.getTimestamp() != null) {
            context.put(jobName + ".timestamp", nodeResult.getTimestamp(), jobId);
        }
        if (nodeResult.getData() != null) {
            context.put(jobName + ".data", nodeResult.getData(), jobId);
        }
        
        // 根据节点类型存储特定属性
        if (nodeResult.getSqlResult() != null) {
            storeSqlResult(context, jobName, nodeResult.getSqlResult(), jobId);
        }
        if (nodeResult.getApiResult() != null) {
            storeApiResult(context, jobName, nodeResult.getApiResult(), jobId);
        }
        if (nodeResult.getBeanResult() != null) {
            storeBeanResult(context, jobName, nodeResult.getBeanResult(), jobId);
        }
        if (nodeResult.getGlueResult() != null) {
            storeGlueResult(context, jobName, nodeResult.getGlueResult(), jobId);
        }
        
        logger.debug("[ResultStorage] 存储NodeResult - jobName: {}", jobName);
    }
    
    /**
     * 存储SQL结果特定属性
     */
    private void storeSqlResult(DataContext context, String jobName, SqlResult sqlResult, Long jobId) {
        context.put(jobName + ".sqlResult", sqlResult, jobId);
        if (sqlResult.getData() != null) {
            context.put(jobName + ".data", sqlResult.getData(), jobId);
        }
        if (sqlResult.getCount() != null) {
            context.put(jobName + ".count", sqlResult.getCount(), jobId);
        }
        if (sqlResult.getAffectedRows() != null) {
            context.put(jobName + ".affectedRows", sqlResult.getAffectedRows(), jobId);
        }
        if (sqlResult.getColumns() != null) {
            context.put(jobName + ".columns", sqlResult.getColumns(), jobId);
        }
        if (sqlResult.getColumnTypes() != null) {
            context.put(jobName + ".columnTypes", sqlResult.getColumnTypes(), jobId);
        }
        if (sqlResult.getSqlType() != null) {
            context.put(jobName + ".sqlType", sqlResult.getSqlType(), jobId);
        }
        if (sqlResult.getExecutedSql() != null) {
            context.put(jobName + ".executedSql", sqlResult.getExecutedSql(), jobId);
        }
    }
    
    /**
     * 存储API结果特定属性
     */
    private void storeApiResult(DataContext context, String jobName, ApiResult apiResult, Long jobId) {
        context.put(jobName + ".apiResult", apiResult, jobId);
        context.put(jobName + ".response", apiResult, jobId); // 别名
        if (apiResult.getStatusCode() != null) {
            context.put(jobName + ".response.statusCode", apiResult.getStatusCode(), jobId);
        }
        if (apiResult.getBody() != null) {
            context.put(jobName + ".response.body", apiResult.getBody(), jobId);
        }
        if (apiResult.getRawBody() != null) {
            context.put(jobName + ".response.rawBody", apiResult.getRawBody(), jobId);
        }
        if (apiResult.getHeaders() != null) {
            context.put(jobName + ".response.headers", apiResult.getHeaders(), jobId);
        }
        if (apiResult.getRequestUrl() != null) {
            context.put(jobName + ".request.url", apiResult.getRequestUrl(), jobId);
        }
        if (apiResult.getRequestMethod() != null) {
            context.put(jobName + ".request.method", apiResult.getRequestMethod(), jobId);
        }
        if (apiResult.getResponseTime() != null) {
            context.put(jobName + ".responseTime", apiResult.getResponseTime(), jobId);
        }
    }
    
    /**
     * 存储Bean结果特定属性
     */
    private void storeBeanResult(DataContext context, String jobName, BeanResult beanResult, Long jobId) {
        context.put(jobName + ".beanResult", beanResult, jobId);
        if (beanResult.getValue() != null) {
            context.put(jobName + ".value", beanResult.getValue(), jobId);
        }
        if (beanResult.getMethodName() != null) {
            context.put(jobName + ".methodName", beanResult.getMethodName(), jobId);
        }
        if (beanResult.getClassName() != null) {
            context.put(jobName + ".className", beanResult.getClassName(), jobId);
        }
        if (beanResult.getReturnType() != null) {
            context.put(jobName + ".returnType", beanResult.getReturnType(), jobId);
        }
    }
    
    /**
     * 存储Glue结果特定属性
     */
    private void storeGlueResult(DataContext context, String jobName, GlueResult glueResult, Long jobId) {
        context.put(jobName + ".glueResult", glueResult, jobId);
        if (glueResult.getOutput() != null) {
            context.put(jobName + ".output", glueResult.getOutput(), jobId);
        }
        if (glueResult.getError() != null) {
            context.put(jobName + ".error", glueResult.getError(), jobId);
        }
        if (glueResult.getExitCode() != null) {
            context.put(jobName + ".exitCode", glueResult.getExitCode(), jobId);
        }
        if (glueResult.getResult() != null) {
            context.put(jobName + ".result", glueResult.getResult(), jobId);
        }
        if (glueResult.getScriptType() != null) {
            context.put(jobName + ".scriptType", glueResult.getScriptType(), jobId);
        }
        if (glueResult.getLogOutput() != null) {
            context.put(jobName + ".logOutput", glueResult.getLogOutput(), jobId);
        }
    }
    
    /**
     * 尝试将旧格式结果转换为NodeResult（向后兼容）
     */
    private NodeResult convertToNodeResult(Object executeResult, JobInfo jobInfo) {
        if (jobInfo == null || jobInfo.getGlueType() == null) {
            return null;
        }
        
        GlueTypeEnum glueType = GlueTypeEnum.match(jobInfo.getGlueType());
        if (glueType == null) {
            return null;
        }
        
        long duration = 0; // 无法获取实际执行时间，使用0
        NodeResult nodeResult = NodeResult.success("执行成功", duration);
        nodeResult.setData(executeResult);
        
        // 根据节点类型创建特定的结果对象
        switch (glueType) {
            case SQL:
                // SQL结果已经在JdbcTaskExecutor中转换为NodeResult
                break;
            case API:
                // API结果已经在HttpTaskExecutor中转换为NodeResult
                break;
            case BEAN:
                BeanResult beanResult = new BeanResult(executeResult);
                beanResult.setMethodName(jobInfo.getExecutorHandler());
                nodeResult.setBeanResult(beanResult);
                break;
            case GLUE_GROOVY:
            case GLUE_SHELL:
            case GLUE_PYTHON:
            case GLUE_PHP:
            case GLUE_NODEJS:
            case GLUE_POWERSHELL:
            case GLUE_CSHARP:
                GlueResult glueResult = new GlueResult();
                glueResult.setResult(executeResult);
                glueResult.setScriptType(glueType.getDesc());
                nodeResult.setGlueResult(glueResult);
                break;
            default:
                return null;
        }
        
        return nodeResult;
    }
    
    /**
     * 存储 Map 类型结果
     * 
     * @param context 数据上下文
     * @param jobName 任务名称
     * @param result Map 结果
     * @param jobId 任务ID
     */
    private void storeMapResult(DataContext context, String jobName, Map<String, Object> result, Long jobId) {
        // 存储整个 Map 作为 jobName.result
        context.put(jobName + ".result", result, jobId);
        
        // 同时将 Map 中的每个键值对存储为 jobName.key，方便直接访问
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String dataKey = jobName + "." + key;
            context.put(dataKey, value, jobId);
            logger.debug("[ResultStorage] 存储 Map 属性 - key: {}, value: {}", dataKey, value);
        }
    }
    
    /**
     * 存储 List 类型结果
     * 
     * @param context 数据上下文
     * @param jobName 任务名称
     * @param result List 结果
     * @param jobId 任务ID
     */
    private void storeListResult(DataContext context, String jobName, List<Object> result, Long jobId) {
        // 存储整个 List 作为 jobName.list
        context.put(jobName + ".list", result, jobId);
        
        // 同时存储为 jobName.result（兼容性）
        context.put(jobName + ".result", result, jobId);
        
        // 如果 List 中只有一个元素，也存储为 jobName.value（方便访问）
        if (result.size() == 1) {
            context.put(jobName + ".value", result.get(0), jobId);
        }
        
        logger.debug("[ResultStorage] 存储 List 结果 - key: {}, size: {}", jobName + ".list", result.size());
    }
    
    /**
     * 存储 String 类型结果
     * 
     * @param context 数据上下文
     * @param jobName 任务名称
     * @param result String 结果
     * @param jobId 任务ID
     */
    private void storeStringResult(DataContext context, String jobName, String result, Long jobId) {
        // 尝试解析为 JSON
        if (isJsonString(result)) {
            try {
                Object parsed = JSONUtil.parse(result);
                if (parsed instanceof Map) {
                    storeMapResult(context, jobName, (Map<String, Object>) parsed, jobId);
                    return;
                } else if (parsed instanceof List) {
                    storeListResult(context, jobName, (List<Object>) parsed, jobId);
                    return;
                }
            } catch (Exception e) {
                logger.debug("[ResultStorage] 解析 JSON 字符串失败，作为普通字符串存储: {}", e.getMessage());
            }
        }
        
        // 作为普通字符串存储
        context.put(jobName + ".result", result, jobId);
        context.put(jobName + ".value", result, jobId);
        
        logger.debug("[ResultStorage] 存储 String 结果 - key: {}, value: {}", jobName + ".result", result);
    }
    
    /**
     * 存储简单类型结果
     * 
     * @param context 数据上下文
     * @param jobName 任务名称
     * @param result 结果
     * @param jobId 任务ID
     */
    private void storeSimpleResult(DataContext context, String jobName, Object result, Long jobId) {
        // 存储为 jobName.value 和 jobName.result
        context.put(jobName + ".value", result, jobId);
        context.put(jobName + ".result", result, jobId);
        
        logger.debug("[ResultStorage] 存储简单类型结果 - key: {}, value: {}", jobName + ".value", result);
    }
    
    /**
     * 检查字符串是否为 JSON 格式
     * 
     * @param str 字符串
     * @return 是否为 JSON
     */
    private boolean isJsonString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = str.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) 
            || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
    
    /**
     * 规范化任务名称
     * 
     * <p>将任务名称转换为标准格式，去除特殊字符
     * 如果 jobDesc 为空，使用 job_${jobId} 作为默认名称
     * 
     * @param jobDesc 任务描述
     * @param jobId 任务ID
     * @return 规范化后的任务名称
     */
    private String normalizeJobName(String jobDesc, Long jobId) {
        String jobName;
        
        if (jobDesc == null || jobDesc.trim().isEmpty()) {
            // 如果 jobDesc 为空，使用默认名称
            jobName = "job_" + jobId;
        } else {
            // 去除前后空格
            jobName = jobDesc.trim();
        }
        
        // 将特殊字符替换为下划线（保留中文字符、字母、数字）
        jobName = jobName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        
        // 去除连续的下划线
        jobName = jobName.replaceAll("_{2,}", "_");
        
        // 去除开头和结尾的下划线
        jobName = jobName.replaceAll("^_+|_+$", "");
        
        // 如果规范化后为空，使用默认名称
        if (jobName.isEmpty()) {
            jobName = "job_" + jobId;
        }
        
        return jobName;
    }
    
    /**
     * 持久化任务组执行结果到数据库
     * 
     * <p>任务组执行完成后，将所有节点的执行结果持久化保存到数据库
     * 
     * @param context 执行上下文
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @return 持久化的节点数量
     */
    public int persistResultToDatabase(ExecutionContext context, Long taskGroupId, String executionBatchId) {
        if (context == null || context.getDataContext() == null) {
            logger.warn("[ResultStorage] 执行上下文或数据上下文为空，无法持久化结果");
            return 0;
        }
        
        if (taskGroupId == null || executionBatchId == null || executionBatchId.isEmpty()) {
            logger.warn("[ResultStorage] 任务组ID或批次ID为空，无法持久化结果");
            return 0;
        }
        
        try {
            logger.info("[ResultStorage] 开始持久化任务组执行结果到数据库 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId);
            
            DataContext dataContext = context.getDataContext();
            Map<String, Long> jobNameMap = context.getJobNameMap();
            
            // 收集所有节点的执行结果（只收集当前运行产生的数据，不包含从数据库加载的数据）
            List<Map<String, Object>> nodeResults = new ArrayList<>();
            Map<Long, String> preferredResultKeyByJobId = new HashMap<>();
            Map<Long, Integer> preferredPriorityByJobId = new HashMap<>();
            
            // 遍历数据上下文中的所有数据
            for (String key : dataContext.keySet()) {
                // 只处理当前运行产生的数据
                DataSourceType sourceType = dataContext.getDataSourceType(key);
                if (sourceType != null && sourceType != DataSourceType.CURRENT_RUNNING) {
                    continue; // 跳过从数据库加载的数据
                }
                
                // 解析key，提取jobName
                // key格式：jobName.attr 或 jobName.result
                String[] parts = key.split("\\.", 2);
                if (parts.length < 1) {
                    continue;
                }
                
                String jobName = parts[0];
                String attrPath = parts.length > 1 ? parts[1] : null;
                int priority = getPersistPriority(attrPath);
                if (priority < 0) {
                    continue;
                }
                
                // 查找对应的jobId
                Long jobId = resolveJobId(jobNameMap, jobName);
                if (jobId == null) {
                    logger.debug("[ResultStorage] 未找到对应的jobId，跳过 - key: {}, jobName: {}", key, jobName);
                    continue;
                }

                Integer currentPriority = preferredPriorityByJobId.get(jobId);
                if (currentPriority == null || priority < currentPriority) {
                    preferredPriorityByJobId.put(jobId, priority);
                    preferredResultKeyByJobId.put(jobId, key);
                }
            }

            for (Map.Entry<Long, String> preferredEntry : preferredResultKeyByJobId.entrySet()) {
                Long jobId = preferredEntry.getKey();
                String key = preferredEntry.getValue();
                String[] parts = key.split("\\.", 2);
                String jobName = parts[0];
                Object resultData = dataContext.get(key);
                if (resultData == null) {
                    continue;
                }
                
                // 获取任务名称
                String taskJobName = null;
                if (jobNameMap != null) {
                    for (Map.Entry<String, Long> entry : jobNameMap.entrySet()) {
                        if (entry.getValue().equals(jobId)) {
                            taskJobName = entry.getKey();
                            break;
                        }
                    }
                }
                
                // 序列化为JSON
                String resultDataJson = JSONUtil.toJsonStr(resultData);
                
                // 保存到本地文件
                String filePath = null;
                Long dataSize = (long) resultDataJson.length();
                
                if (fileStorageService != null) {
                    filePath = fileStorageService.saveToFile(taskGroupId, executionBatchId, jobId, resultDataJson);
                    if (filePath == null) {
                        // 文件保存失败，记录警告但继续处理（会回退到数据库存储）
                        logger.warn("[ResultStorage] 保存文件失败，将回退到数据库存储 - jobId: {}", jobId);
                    } else {
                        logger.debug("[ResultStorage] 保存文件成功 - jobId: {}, filePath: {}", jobId, filePath);
                    }
                }
                
                // 添加到结果列表
                Map<String, Object> nodeResult = new HashMap<>();
                nodeResult.put("jobId", jobId);
                nodeResult.put("jobName", taskJobName != null ? taskJobName : jobName);
                nodeResult.put("instanceKey", context.getInstanceKey());
                // 如果文件保存成功，resultData可以为空；如果失败，则保存到数据库
                if (filePath != null) {
                    nodeResult.put("filePath", filePath);
                    nodeResult.put("dataSize", dataSize);
                    nodeResult.put("resultData", null); // 文件存储时，数据库中的resultData为空
                } else {
                    // 文件保存失败，回退到数据库存储
                    nodeResult.put("filePath", null);
                    nodeResult.put("dataSize", dataSize);
                    nodeResult.put("resultData", resultDataJson);
                }
                nodeResults.add(nodeResult);
                logger.debug("[ResultStorage] 收集节点结果 - jobId: {}, jobName: {}, filePath: {}, key: {}", 
                        jobId, taskJobName, filePath, key);
            }
            
            if (nodeResults.isEmpty()) {
                logger.info("[ResultStorage] 没有需要持久化的节点结果 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return 0;
            }
            
            // 批量保存到数据库
            boolean success = adminApiClient.saveNodeResults(taskGroupId, executionBatchId, context.getInstanceKey(), nodeResults);
            
            if (success) {
                logger.info("[ResultStorage] 任务组执行结果持久化成功 - taskGroupId: {}, batchId: {}, 节点数: {}", 
                        taskGroupId, executionBatchId, nodeResults.size());
                return nodeResults.size();
            } else {
                logger.error("[ResultStorage] 任务组执行结果持久化失败 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return 0;
            }
            
        } catch (Exception e) {
            logger.error("[ResultStorage] 持久化任务组执行结果异常 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId, e);
            return 0;
        }
    }

    private Long resolveJobId(Map<String, Long> jobNameMap, String jobName) {
        if (jobNameMap == null || jobName == null) {
            return null;
        }
        for (Map.Entry<String, Long> entry : jobNameMap.entrySet()) {
            String normalizedJobName = normalizeJobName(entry.getKey(), entry.getValue());
            if (normalizedJobName.equals(jobName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private int getPersistPriority(String attrPath) {
        if (attrPath == null || attrPath.isEmpty()) {
            return 0;
        }
        if ("result".equals(attrPath)) {
            return 1;
        }
        if ("value".equals(attrPath)) {
            return 2;
        }
        if ("list".equals(attrPath)) {
            return 3;
        }
        return -1;
    }
}

