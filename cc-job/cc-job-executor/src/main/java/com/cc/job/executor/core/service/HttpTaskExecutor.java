package com.cc.job.executor.core.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.model.entity.JobInfo;
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
     * @return 响应结果
     */
    public String execute(JobInfo jobInfo) {
        logger.info("[HttpTaskExecutor] 开始执行HTTP任务 - jobId: {}, url: {}, method: {}", 
                jobInfo.getId(), jobInfo.getReqUrl(), jobInfo.getReqType());
        
        validateHttpTask(jobInfo);
        
        Map<String, String> headers = parseHeaders(jobInfo.getReqHeader());
        String result = executeHttpRequest(jobInfo, headers);
        
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
    private String executeHttpRequest(JobInfo jobInfo, Map<String, String> headers) {
        String reqType = jobInfo.getReqType().toUpperCase();
        String reqUrl = jobInfo.getReqUrl();
        String reqBody = jobInfo.getReqBody();
        
        try {
            if (ExecutorConstants.HttpMethod.GET.equals(reqType)) {
                return HttpRequest.get(reqUrl)
                        .addHeaders(headers)
                        .execute()
                        .body();
                        
            } else if (ExecutorConstants.HttpMethod.POST.equals(reqType)) {
                return HttpRequest.post(reqUrl)
                        .addHeaders(headers)
                        .body(reqBody)
                        .execute()
                        .body();
                        
            } else {
                throw new IllegalArgumentException("不支持的HTTP请求类型: " + reqType);
            }
        } catch (Exception e) {
            logger.error("[HttpTaskExecutor] HTTP请求执行失败 - url: {}, method: {}", 
                    reqUrl, reqType, e);
            throw new RuntimeException("HTTP请求执行失败: " + e.getMessage(), e);
        }
    }
}

