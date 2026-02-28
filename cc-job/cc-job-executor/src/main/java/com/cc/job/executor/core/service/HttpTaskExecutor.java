package com.cc.job.executor.core.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.result.ApiResult;
import com.cc.job.xo.model.result.NodeResult;
import com.xxl.job.core.context.XxlJobHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 任务执行器
 * 
 * <p>负责执行 HTTP API 请求任务
 *
 * @author cc-job-team
 */
@Component
public class HttpTaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpTaskExecutor.class);
    
    /**
     * 执行 HTTP 请求任务
     * 
     * @param jobInfo 任务信息
     * @return 响应结果（保持向后兼容）
     */
    public String execute(JobInfo jobInfo) {
        logger.info("[HttpTaskExecutor] 开始执行HTTP任务 - jobId: {}, url: {}, method: {}", 
                jobInfo.getId(), jobInfo.getReqUrl(), jobInfo.getReqType());
        
        long startTime = System.currentTimeMillis();
        validateHttpTask(jobInfo);
        
        Map<String, String> headers = parseHeaders(jobInfo.getReqHeader());
        NodeResult nodeResult = executeHttpRequest(jobInfo, headers, startTime);
        
        // 设置执行结果
        if (nodeResult != null) {
            XxlJobHelper.executeResult(nodeResult);
        }
        
        // 返回原始响应体以保持向后兼容
        String result = null;
        if (nodeResult != null && nodeResult.getApiResult() != null) {
            result = nodeResult.getApiResult().getRawBody();
        }
        
        logger.info("[HttpTaskExecutor] HTTP任务执行完成 - jobId: {}, 响应长度: {}", 
                jobInfo.getId(), result != null ? result.length() : 0);
        
        return result;
    }
    
    /**
     * 验证 HTTP 任务参数
     */
    private void validateHttpTask(JobInfo jobInfo) {
        if (StringUtils.isBlank(jobInfo.getReqUrl())) {
            throw new IllegalArgumentException("HTTP请求URL不能为空");
        }
        if (StringUtils.isBlank(jobInfo.getReqType())) {
            throw new IllegalArgumentException("HTTP请求类型不能为空");
        }
    }
    
    /**
     * 解析请求头
     */
    private Map<String, String> parseHeaders(String reqHeader) {
        Map<String, String> headers = new HashMap<>();
        
        if (StringUtils.isBlank(reqHeader)) {
            return headers;
        }
        
        try {
            JSONArray jsonArray = JSONUtil.parseArray(reqHeader);
            List<Map> headerList = jsonArray.toList(Map.class);
            
            for (Map data : headerList) {
                String key = (String) data.get("columnKey");
                String value = (String) data.get("columnValue");
                if (key != null && value != null) {
                    headers.put(key, value);
                }
            }
        } catch (Exception e) {
            logger.warn("[HttpTaskExecutor] 解析请求头失败，使用空headers - error: {}", 
                    e.getMessage());
        }
        
        return headers;
    }
    
    /**
     * 执行 HTTP 请求
     */
    private NodeResult executeHttpRequest(JobInfo jobInfo, Map<String, String> headers, long startTime) {
        String reqType = jobInfo.getReqType().toUpperCase();
        String reqUrl = jobInfo.getReqUrl();
        String reqBody = jobInfo.getReqBody();
        long requestTime = System.currentTimeMillis();
        
        ApiResult apiResult = new ApiResult();
        apiResult.setRequestUrl(reqUrl);
        apiResult.setRequestMethod(reqType);
        apiResult.setRequestHeaders(headers);
        apiResult.setRequestBody(reqBody);
        apiResult.setRequestTime(requestTime);
        
        try {
            HttpResponse response;
            long responseStartTime = System.currentTimeMillis();
            
            if (ExecutorConstants.HttpMethod.GET.equals(reqType)) {
                response = HttpRequest.get(reqUrl)
                        .addHeaders(headers)
                        .execute();
                        
            } else if (ExecutorConstants.HttpMethod.POST.equals(reqType)) {
                response = HttpRequest.post(reqUrl)
                        .addHeaders(headers)
                        .body(reqBody)
                        .execute();
                        
            } else {
                throw new IllegalArgumentException("不支持的HTTP请求类型: " + reqType);
            }
            
            long responseTime = System.currentTimeMillis() - responseStartTime;
            long totalDuration = System.currentTimeMillis() - startTime;
            
            // 填充响应信息
            String rawBody = response.body();
            apiResult.setStatusCode(response.getStatus());
            apiResult.setRawBody(rawBody);
            apiResult.setResponseTime(responseTime);
            
            // 解析响应头
            Map<String, String> responseHeaders = new HashMap<>();
            response.headers().forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    responseHeaders.put(key, values.get(0));
                }
            });
            apiResult.setHeaders(responseHeaders);
            
            // 获取内容类型
            String contentType = response.header("Content-Type");
            if (contentType != null && contentType.contains(";")) {
                contentType = contentType.split(";")[0].trim();
            }
            apiResult.setContentType(contentType);
            
            // 尝试解析JSON响应体
            Object body = null;
            if (rawBody != null && !rawBody.isEmpty()) {
                if (contentType != null && contentType.contains("application/json")) {
                    try {
                        body = JSONUtil.parse(rawBody);
                    } catch (Exception e) {
                        logger.debug("[HttpTaskExecutor] JSON解析失败，使用原始字符串 - error: {}", e.getMessage());
                        body = rawBody;
                    }
                } else {
                    body = rawBody;
                }
            }
            apiResult.setBody(body);
            
            XxlJobHelper.log("HTTP请求完成 - 状态码: {}, 响应时间: {}ms", response.getStatus(), responseTime);
            logger.info("[HttpTaskExecutor] HTTP请求完成 - jobId: {}, 状态码: {}, 响应时间: {}ms", 
                    jobInfo.getId(), response.getStatus(), responseTime);
            
            NodeResult nodeResult = NodeResult.success("HTTP请求成功", totalDuration);
            nodeResult.setApiResult(apiResult);
            nodeResult.setData(body != null ? body : rawBody);
            
            return nodeResult;
            
        } catch (Exception e) {
            logger.error("[HttpTaskExecutor] HTTP请求执行失败 - url: {}, method: {}", 
                    reqUrl, reqType, e);
            long totalDuration = System.currentTimeMillis() - startTime;
            NodeResult errorResult = NodeResult.failure("HTTP请求执行失败: " + e.getMessage(), totalDuration);
            errorResult.setApiResult(apiResult);
            XxlJobHelper.executeResult(errorResult);
            throw new RuntimeException("HTTP请求执行失败: " + e.getMessage(), e);
        }
    }
}

