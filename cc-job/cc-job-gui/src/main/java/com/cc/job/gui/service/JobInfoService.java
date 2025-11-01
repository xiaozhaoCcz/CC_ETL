package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.form.JobInfoForm;
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
        
        // 添加触发用户ID（从SessionManager获取）
        com.cc.job.gui.util.SessionManager session = com.cc.job.gui.util.SessionManager.getInstance();
        if (session.isLoggedIn() && session.getUserId() != null) {
            try {
                Integer triggerUserId = Integer.parseInt(session.getUserId());
                requestMap.put("triggerUserId", triggerUserId);
                System.out.println("✓ 传递触发用户ID: " + triggerUserId);
            } catch (NumberFormatException e) {
                System.err.println("⚠ 用户ID格式错误: " + session.getUserId());
            }
        }
        
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
    
    /**
     * 保存任务组
     * @param formData 任务组表单数据
     * @return 是否保存成功
     * @throws IOException 网络异常
     */
    public boolean saveJobCompose(JobInfoForm formData) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/saveJobCompose";
        
        String jsonBody = apiUtil.getGson().toJson(formData);
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
            System.out.println("saveJobCompose API 响应: " + responseBody);
            
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 更新任务组
     * @param id 任务组ID
     * @param formData 任务组表单数据
     * @return 是否更新成功
     * @throws IOException 网络异常
     */
    public boolean updateJobCompose(Long id, JobInfoForm formData) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/updateJobCompose/" + id;
        
        String jsonBody = apiUtil.getGson().toJson(formData);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            System.out.println("updateJobCompose API 响应: " + responseBody);
            
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 保存任务节点
     * @param formData 任务节点表单数据
     * @return 节点信息（包含节点ID和jobId）
     * @throws IOException 网络异常
     */
    public com.cc.job.xo.model.entity.JobNode saveJobNode(JobInfoForm formData) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/saveJobNode";
        
        String jsonBody = apiUtil.getGson().toJson(formData);
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
            System.out.println("saveJobNode API 响应: " + responseBody);
            
            Type resultType = new TypeToken<Result<com.cc.job.xo.model.entity.JobNode>>(){}.getType();
            Result<com.cc.job.xo.model.entity.JobNode> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 更新任务节点
     * @param id 任务ID
     * @param formData 任务节点表单数据
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean updateJobNode(Long id, JobInfoForm formData) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/" + id;
        
        String jsonBody = apiUtil.getGson().toJson(formData);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            System.out.println("updateJobNode API 响应: " + responseBody);
            
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 获取任务表单数据（用于保存时获取现有数据）
     * @param id 任务ID
     * @return 任务表单数据
     * @throws IOException 网络异常
     */
    public JobInfoForm getFormData(Long id) throws IOException {
        return getJobNodeFormData(id);
    }
    
    /**
     * 获取任务节点表单数据
     * @param id 任务ID
     * @return 任务表单数据
     * @throws IOException 网络异常
     */
    public JobInfoForm getJobNodeFormData(Long id) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/" + id + "/form";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            System.out.println("getJobNodeFormData API 响应: " + responseBody);
            
            Type resultType = new TypeToken<Result<JobInfoForm>>(){}.getType();
            Result<JobInfoForm> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
}

