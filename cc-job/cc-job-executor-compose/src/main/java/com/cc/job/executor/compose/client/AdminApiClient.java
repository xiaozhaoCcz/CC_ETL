package com.cc.job.executor.compose.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API 客户端
 * 用于与 Admin 模块通信，获取任务组信息
 * 
 * @author xiaozhao
 */
@Component
public class AdminApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminApiClient.class);
    
    @Value("${cc-job.admin.address}")
    private String adminAddress;
    
    @Value("${cc-job.access-token:default_token}")
    private String accessToken;
    
    private static final int TIMEOUT = 30000; // 30秒超时
    
    /**
     * 获取任务信息
     * 
     * @param jobId 任务ID
     * @return 任务信息
     */
    public JobInfo getJobInfo(Long jobId) {
        try {
            String url = adminAddress + "/api/job/info/" + jobId;
            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", accessToken)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (response.isOk()) {
                return JSONUtil.toBean(response.body(), JobInfo.class);
            } else {
                logger.error("[AdminApiClient] 获取任务信息失败 - jobId: {}, status: {}, body: {}", 
                        jobId, response.getStatus(), response.body());
                return null;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 获取任务信息异常 - jobId: {}", jobId, e);
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
            String url = adminAddress + "/api/job/nodes/" + jobId;
            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", accessToken)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (response.isOk()) {
                return JSONUtil.toList(response.body(), JobNode.class);
            } else {
                logger.error("[AdminApiClient] 获取任务节点失败 - jobId: {}, status: {}", 
                        jobId, response.getStatus());
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
            String url = adminAddress + "/api/job/edges/" + jobId;
            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", accessToken)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (response.isOk()) {
                return JSONUtil.toList(response.body(), JobEdge.class);
            } else {
                logger.error("[AdminApiClient] 获取任务边失败 - jobId: {}, status: {}", 
                        jobId, response.getStatus());
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
     * @param jobId 任务ID
     * @param randomId 批次ID
     * @param status 状态（0=失败，1=成功，2=执行中）
     * @param message 状态消息
     * @return 是否上报成功
     */
    public boolean reportStatus(Long jobId, String randomId, Integer status, String message) {
        try {
            String url = adminAddress + "/api/job/status";
            Map<String, Object> params = new HashMap<>();
            params.put("jobId", jobId);
            params.put("randomId", randomId);
            params.put("status", status);
            params.put("message", message);
            
            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", accessToken)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(params))
                    .timeout(TIMEOUT)
                    .execute();
            
            if (response.isOk()) {
                logger.debug("[AdminApiClient] 上报状态成功 - jobId: {}, status: {}", jobId, status);
                return true;
            } else {
                logger.error("[AdminApiClient] 上报状态失败 - jobId: {}, status: {}, responseStatus: {}", 
                        jobId, status, response.getStatus());
                return false;
            }
        } catch (Exception e) {
            logger.error("[AdminApiClient] 上报状态异常 - jobId: {}, status: {}", jobId, status, e);
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
            String url = adminAddress + "/api/job/snapshot/" + jobId + "/" + randomId;
            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", accessToken)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (response.isOk()) {
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
}
