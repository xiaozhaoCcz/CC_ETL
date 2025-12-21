package com.cc.job.executor.compose.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin API 客户端
 * 用于与 Admin 模块通信，获取任务组信息
 * 
 * @author xiaozhao
 */
@Component
@SuppressWarnings("unchecked")
public class AdminApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminApiClient.class);
    
    @Value("${cc-job.job.admin.addresses}")
    private String adminAddresses;
    
    @Value("${cc-job.job.accessToken}")
    private String accessToken;
    
    private static final int TIMEOUT = 30000; // 30秒超时
    
    /**
     * Admin地址列表（已规范化，只包含协议+主机+端口）
     */
    private List<String> normalizedAdminAddresses = new ArrayList<>();
    
    /**
     * 初始化，解析和规范化Admin地址
     */
    @PostConstruct
    public void init() {
        if (adminAddresses == null || adminAddresses.trim().isEmpty()) {
            logger.warn("[AdminApiClient] 未配置Admin地址，使用默认地址: http://127.0.0.1:8989");
            normalizedAdminAddresses.add("http://127.0.0.1:8989");
            return;
        }
        
        // 按逗号分割地址
        String[] addresses = adminAddresses.split(",");
        normalizedAdminAddresses = Arrays.stream(addresses)
                .map(String::trim)
                .filter(addr -> !addr.isEmpty())
                .map(this::normalizeAddress)
                .filter(addr -> addr != null)
                .collect(Collectors.toList());
        
        if (normalizedAdminAddresses.isEmpty()) {
            logger.warn("[AdminApiClient] 解析后的Admin地址列表为空，使用默认地址: http://127.0.0.1:8989");
            normalizedAdminAddresses.add("http://127.0.0.1:8989");
        } else {
            logger.info("[AdminApiClient] 初始化Admin地址列表: {}", normalizedAdminAddresses);
        }
    }
    
    /**
     * 规范化地址，移除路径部分，只保留协议+主机+端口
     * 例如: http://127.0.0.1:8989/xxl-job-admin -> http://127.0.0.1:8989
     * 
     * @param address 原始地址
     * @return 规范化后的地址
     */
    private String normalizeAddress(String address) {
        try {
            URI uri = URI.create(address);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            
            if (scheme == null || host == null) {
                logger.warn("[AdminApiClient] 无效的地址格式: {}", address);
                return null;
            }
            
            // 构建规范化地址：协议://主机:端口
            if (port > 0) {
                return scheme + "://" + host + ":" + port;
            } else {
                // 如果没有端口，使用默认端口
                if ("https".equals(scheme)) {
                    return scheme + "://" + host + ":443";
                } else {
                    return scheme + "://" + host + ":80";
                }
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 解析地址失败: {}", address, e);
            return null;
        }
    }
    
    /**
     * 执行GET请求，尝试所有地址直到成功
     * 
     * @param path API路径（如 /api/v1/jobInfos/123）
     * @return HttpResponse，如果所有地址都失败则返回null
     */
    private HttpResponse executeGet(String path) {
        for (String baseAddress : normalizedAdminAddresses) {
            try {
                String url = baseAddress + path;
                HttpResponse response = HttpRequest.get(url)
                        .header("Authorization", accessToken)
                        .timeout(TIMEOUT)
                        .execute();
                
                if (response.isOk()) {
                    logger.debug("[AdminApiClient] GET请求成功 - url: {}", url);
                    return response;
                } else {
                    logger.warn("[AdminApiClient] GET请求失败 - url: {}, status: {}", url, response.getStatus());
                }
            } catch (Exception e) {
                logger.warn("[AdminApiClient] GET请求异常 - address: {}, path: {}, error: {}", 
                        baseAddress, path, e.getMessage());
            }
        }
        
        logger.error("[AdminApiClient] 所有Admin地址的GET请求都失败 - path: {}", path);
        return null;
    }
    
    /**
     * 执行POST请求，尝试所有地址直到成功
     * 
     * @param path API路径
     * @param body 请求体（JSON字符串），如果为空则不设置body
     * @return HttpResponse，如果所有地址都失败则返回null
     */
    private HttpResponse executePost(String path, String body) {
        for (String baseAddress : normalizedAdminAddresses) {
            try {
                String url = baseAddress + path;
                HttpRequest request = HttpRequest.post(url)
                        .header("Authorization", accessToken)
                        .timeout(TIMEOUT);
                
                // 只有当body不为空时才设置Content-Type和body
                if (body != null && !body.trim().isEmpty()) {
                    request.header("Content-Type", "application/json")
                           .body(body);
                }
                
                HttpResponse response = request.execute();
                
                if (response.isOk()) {
                    logger.debug("[AdminApiClient] POST请求成功 - url: {}", url);
                    return response;
                } else {
                    logger.warn("[AdminApiClient] POST请求失败 - url: {}, status: {}", url, response.getStatus());
                }
            } catch (Exception e) {
                logger.warn("[AdminApiClient] POST请求异常 - address: {}, path: {}, error: {}", 
                        baseAddress, path, e.getMessage());
            }
        }
        
        logger.error("[AdminApiClient] 所有Admin地址的POST请求都失败 - path: {}", path);
        return null;
    }
    
    /**
     * 获取任务信息
     * 
     * @param jobId 任务ID
     * @return 任务信息
     */
    public JobInfo getJobInfo(Long jobId) {
        try {
            HttpResponse response = executeGet("/api/v1/jobInfos/" + jobId);
            if (response != null && response.isOk()) {
                // 解析Result包装的响应
                Map<String, Object> resultMap = JSONUtil.toBean(response.body(), Map.class);
                Object data = resultMap.get("data");
                if (data != null) {
                    return JSONUtil.toBean(JSONUtil.toJsonStr(data), JobInfo.class);
                }
                return null;
            } else {
                logger.error("[AdminApiClient] 获取任务信息失败 - jobId: {}", jobId);
                return null;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 获取任务信息异常 - jobId: {}", jobId, e);
            return null;
        }
    }
    
    /**
     * 获取执行器组信息
     * 
     * @param jobGroupId 执行器组ID
     * @return 执行器组信息
     */
    public JobGroup getJobGroup(Long jobGroupId) {
        try {
            HttpResponse response = executeGet("/api/v1/jobGroups/" + jobGroupId);
            if (response != null && response.isOk()) {
                // 解析Result包装的响应
                Map<String, Object> resultMap = JSONUtil.toBean(response.body(), Map.class);
                Object data = resultMap.get("data");
                if (data != null) {
                    JobGroup jobGroup = JSONUtil.toBean(JSONUtil.toJsonStr(data), JobGroup.class);
                    logger.debug("[AdminApiClient] 获取执行器组成功 - jobGroupId: {}, appName: {}, addressList: {}", 
                            jobGroupId, jobGroup != null ? jobGroup.getAppName() : null, 
                            jobGroup != null ? jobGroup.getAddressList() : null);
                    return jobGroup;
                }
                logger.warn("[AdminApiClient] 响应中data字段为空 - jobGroupId: {}, response: {}", 
                        jobGroupId, response.body());
                return null;
            } else {
                logger.error("[AdminApiClient] 获取执行器组失败 - jobGroupId: {}", jobGroupId);
                return null;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 获取执行器组异常 - jobGroupId: {}", jobGroupId, e);
            return null;
        }
    }
    
    /**
     * 获取任务组的所有节点
     * 
     * @param jobId 任务组ID
     * @return 节点列表
     */
    public List<JobNode> getJobNodes(Long jobId) {
        try {
            HttpResponse response = executeGet("/api/v1/jobInfos/nodes/" + jobId);
            if (response != null && response.isOk()) {
                // 解析Result包装的响应
                Map<String, Object> resultMap = JSONUtil.toBean(response.body(), Map.class);
                Object data = resultMap.get("data");
                if (data != null) {
                    return JSONUtil.toList(JSONUtil.toJsonStr(data), JobNode.class);
                }
                return new ArrayList<>();
            } else {
                logger.error("[AdminApiClient] 获取任务节点失败 - jobId: {}", jobId);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 获取任务节点异常 - jobId: {}", jobId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取任务组的所有边（依赖关系）
     * 
     * @param jobId 任务组ID
     * @return 边列表
     */
    public List<JobEdge> getJobEdges(Long jobId) {
        try {
            HttpResponse response = executeGet("/api/v1/jobInfos/edges/" + jobId);
            if (response != null && response.isOk()) {
                // 解析Result包装的响应
                Map<String, Object> resultMap = JSONUtil.toBean(response.body(), Map.class);
                Object data = resultMap.get("data");
                if (data != null) {
                    return JSONUtil.toList(JSONUtil.toJsonStr(data), JobEdge.class);
                }
                return new ArrayList<>();
            } else {
                logger.error("[AdminApiClient] 获取任务边失败 - jobId: {}", jobId);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 获取任务边异常 - jobId: {}", jobId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 上报任务执行状态
     * 
     * @param parentJobId 父任务ID（任务组ID）
     * @param jobId 任务ID（子任务ID）
     * @param randomId 批次ID
     * @param status 状态（0=失败，1=成功，2=执行中，5=完成）
     * @param message 状态消息
     * @return 是否上报成功
     */
    public boolean reportStatus(Long parentJobId, Long jobId, String randomId, Integer status, String message) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("parentJobId", parentJobId);
            params.put("jobId", jobId);
            params.put("randomId", randomId);
            params.put("status", status);
            params.put("message", message);
            
            HttpResponse response = executePost("/api/v1/jobInfos/status", JSONUtil.toJsonStr(params));
            if (response != null && response.isOk()) {
                logger.debug("[AdminApiClient] 上报状态成功 - jobId: {}, status: {}", jobId, status);
                return true;
            } else {
                logger.error("[AdminApiClient] 上报状态失败 - jobId: {}, status: {}", jobId, status);
                return false;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 上报状态异常 - jobId: {}, status: {}", jobId, status, e);
            return false;
        }
    }
    
    /**
     * 更新任务组运行状态
     * 
     * @param jobId 任务组ID
     * @param status 运行状态：0=未运行, 1=运行中
     * @return 是否更新成功
     */
    public boolean updateRankTriggerStatus(Long jobId, Integer status) {
        try {
            String path = "/api/v1/jobInfos/updateRankTriggerStatus/" + jobId + "?status=" + status;
            HttpResponse response = executePost(path, "");
            if (response != null && response.isOk()) {
                logger.debug("[AdminApiClient] 更新任务组运行状态成功 - jobId: {}, status: {}", jobId, status);
                return true;
            } else {
                logger.error("[AdminApiClient] 更新任务组运行状态失败 - jobId: {}, status: {}", jobId, status);
                return false;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 更新任务组运行状态异常 - jobId: {}, status: {}", jobId, status, e);
            return false;
        }
    }
    
    /**
     * 获取任务组批次的快照信息（如果有）
     * 
     * @param jobId 任务组ID
     * @param randomId 批次ID
     * @return 快照数据（节点和边的JSON）
     */
    public Map<String, String> getSnapshot(Long jobId, String randomId) {
        try {
            HttpResponse response = executeGet("/api/job/snapshot/" + jobId + "/" + randomId);
            if (response != null && response.isOk()) {
                return JSONUtil.toBean(response.body(), Map.class);
            } else {
                logger.debug("[AdminApiClient] 获取快照失败或不存在 - jobId: {}, randomId: {}", jobId, randomId);
                return null;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 获取快照异常 - jobId: {}, randomId: {}", jobId, randomId, e);
            return null;
        }
    }

    /**
     * 批量获取任务信息
     *
     * @param jobIds 任务ID列表
     * @return 任务信息列表
     */
    public List<JobInfo> getJobInfos(List<Long> jobIds) {
        try {
            if (jobIds == null || jobIds.isEmpty()) {
                logger.warn("[AdminApiClient] 任务ID列表为空");
                return new ArrayList<>();
            }

            HttpResponse response = executePost("/api/v1/jobInfos/batch", JSONUtil.toJsonStr(jobIds));
            if (response != null && response.isOk()) {
                // 解析Result包装的响应
                Map<String, Object> resultMap = JSONUtil.toBean(response.body(), Map.class);
                Object data = resultMap.get("data");
                if (data != null) {
                    List<JobInfo> jobInfos = JSONUtil.toList(JSONUtil.toJsonStr(data), JobInfo.class);
                    logger.debug("[AdminApiClient] 批量获取任务信息成功 - jobIds: {}, 返回数量: {}",
                            jobIds, jobInfos != null ? jobInfos.size() : 0);
                    return jobInfos != null ? jobInfos : new ArrayList<>();
                }
                logger.warn("[AdminApiClient] 响应中data字段为空 - jobIds: {}, response: {}",
                        jobIds, response.body());
                return new ArrayList<>();
            } else {
                logger.error("[AdminApiClient] 批量获取任务信息失败 - jobIds: {}", jobIds);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 批量获取任务信息异常 - jobIds: {}", jobIds, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存节点执行状态到 job_log 表
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @param nodeStatusJson 节点状态JSON字符串
     * @return 是否保存成功
     */
    public boolean saveNodeStatus(Long taskGroupId, String executionBatchId, String nodeStatusJson) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("taskGroupId", taskGroupId);
            params.put("executionBatchId", executionBatchId);
            params.put("nodeStatus", nodeStatusJson);
            
            HttpResponse response = executePost("/api/v1/jobLogs/saveNodeStatus", JSONUtil.toJsonStr(params));
            if (response != null && response.isOk()) {
                logger.debug("[AdminApiClient] 保存节点状态成功 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return true;
            } else {
                logger.error("[AdminApiClient] 保存节点状态失败 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return false;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 保存节点状态异常 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId, e);
            return false;
        }
    }
}
