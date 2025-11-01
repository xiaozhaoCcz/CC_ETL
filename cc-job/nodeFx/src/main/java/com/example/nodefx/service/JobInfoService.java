package com.example.nodefx.service;

import com.cc.job.xo.common.result.Result;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务信息服务
 */
public class JobInfoService extends BaseService {
    
    /**
     * 触发任务执行
     * @param jobId 任务组ID
     * @param executorParam 执行参数（randomId）
     * @return 执行日志ID
     * @throws IOException 网络异常
     */
    public Long triggerJob(Long jobId, String executorParam) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/trigger";
        
        // 构建请求参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", jobId);
        requestMap.put("executorParam", executorParam);
        
        String jsonBody = apiUtil.getGson().toJson(requestMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            System.out.println("triggerJob API 响应: " + responseBody);
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<String>>(){}.getType();
            Result<String> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                // 返回的数据是日志ID（字符串格式）
                return Long.parseLong(result.getData());
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 停止任务组
     * @param jobId 任务组ID
     * @param randomId 执行批次ID
     * @throws IOException 网络异常
     */
    public void stopJobCompose(Long jobId, String randomId) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/stopJobCompose/" + jobId + "/" + randomId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            System.out.println("stopJobCompose API 响应: " + responseBody);
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (!Result.isSuccess(result)) {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 获取任务运行状态
     * @param jobId 任务组ID
     * @return 是否正在运行
     * @throws IOException 网络异常
     */
    public boolean getJobStatus(Long jobId) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/getJobStatus/" + jobId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            System.out.println("getJobStatus API 响应: " + responseBody);
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<Boolean>>(){}.getType();
            Result<Boolean> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData() != null && result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
}

