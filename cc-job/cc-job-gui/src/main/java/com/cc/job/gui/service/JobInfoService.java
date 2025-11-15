package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobLogglue;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.xo.model.form.JobInfoForm;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务信息服务
 */
public class JobInfoService extends BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobInfoService.class);
    
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
                logger.debug("✓ 传递触发用户ID: {}", triggerUserId);
            } catch (NumberFormatException e) {
                logger.warn("⚠ 用户ID格式错误: {}", session.getUserId());
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
            logger.debug("triggerJob API 响应: {}", responseBody);
            
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
            logger.debug("stopJobCompose API 响应: {}", responseBody);
            
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
            logger.debug("getJobStatus API 响应: {}", responseBody);
            
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
            logger.debug("saveJobCompose API 响应: {}", responseBody);
            
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
            logger.debug("updateJobCompose API 响应: {}", responseBody);
            
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
            logger.debug("saveJobNode API 响应: {}", responseBody);
            
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
            logger.debug("updateJobNode API 响应: {}", responseBody);
            
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
            logger.debug("getJobNodeFormData API 响应: {}", responseBody);
            
            Type resultType = new TypeToken<Result<JobInfoForm>>(){}.getType();
            Result<JobInfoForm> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 更新节点运行状态
     * @param jobId 任务ID
     * @param triggerStatus 运行状态：0=失败, 1=成功, 2=运行中
     * @throws IOException 网络异常
     */
    public void updateNodeStatus(Long jobId, Integer triggerStatus) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/updateNodeStatus?jobId=" + jobId + "&triggerStatus=" + triggerStatus;
        
        RequestBody body = RequestBody.create("", MediaType.get("application/x-www-form-urlencoded"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.debug("updateNodeStatus API 响应: {}", responseBody);
            
            Type resultType = new TypeToken<Result<Boolean>>(){}.getType();
            Result<Boolean> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (!Result.isSuccess(result)) {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
            
            logger.debug("✓ 节点状态更新成功: jobId={}, triggerStatus={}", jobId, triggerStatus);
        }
    }
    
    /**
     * 批量更新节点运行状态
     * @param statusMap 节点状态映射 Map<jobId, triggerStatus>
     * @throws IOException 网络异常
     */
    public void batchUpdateNodeStatus(java.util.Map<Long, Integer> statusMap) throws IOException {
        if (statusMap == null || statusMap.isEmpty()) {
            logger.warn("⚠ 批量更新节点状态 - 参数为空");
            return;
        }
        
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/batchUpdateNodeStatus";
        
        // 将Map转换为JSON
        String jsonBody = apiUtil.getGson().toJson(statusMap);
        
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
            logger.debug("batchUpdateNodeStatus API 响应: {}", responseBody);
            
            Type resultType = new TypeToken<Result<Integer>>(){}.getType();
            Result<Integer> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (!Result.isSuccess(result)) {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
            
            logger.debug("✓ 批量更新节点状态成功: {}/{} 个节点", result.getData(), statusMap.size());
        }
    }
    
    /**
     * 暂停/启用任务
     * @param jobId 任务ID
     * @param isPause 是否暂停：0=启用, 1=禁用
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean pauseJob(Long jobId, Integer isPause) throws IOException {
        if (jobId == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (isPause == null || (isPause != 0 && isPause != 1)) {
            throw new IllegalArgumentException("isPause 参数必须为 0（启用）或 1（禁用）");
        }
        
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/pauseJob/" + jobId + "?isPause=" + isPause;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.debug("pauseJob API 响应: {}", responseBody);
            
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (!Result.isSuccess(result)) {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
            
            logger.debug("✓ 任务{}成功: jobId={}, isPause={}", isPause == 1 ? "禁用" : "启用", jobId, isPause);
            return true;
        }
    }
    
    /**
     * 保存GLUE源代码
     * @param formData GLUE表单数据
     * @return 是否保存成功
     * @throws IOException 网络异常
     */
    public boolean saveGlueSource(JobGlueForm formData) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/saveGlueSource";
        
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
            logger.debug("saveGlueSource API 响应: {}", responseBody);
            
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (!Result.isSuccess(result)) {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
            
            logger.debug("✓ GLUE源代码保存成功: taskId={}", formData.getTaskId());
            return true;
        }
    }
    
    /**
     * 获取GLUE历史记录列表
     * @param id 任务ID
     * @return GLUE历史记录列表
     * @throws IOException 网络异常
     */
    public List<JobLogglue> getGlueList(Long id) throws IOException {
        return getGlueList(id, null);
    }
    
    /**
     * 根据任务ID和GLUE类型获取历史记录列表
     * @param id 任务ID
     * @param glueType GLUE类型（可选，如果为空则返回所有类型）
     * @return GLUE历史记录列表
     * @throws IOException 网络异常
     */
    public List<JobLogglue> getGlueList(Long id, String glueType) throws IOException {
        String url;
        if (glueType != null && !glueType.trim().isEmpty()) {
            // 使用带GLUE类型的接口
            url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/getGlueList/" + id + "/" + java.net.URLEncoder.encode(glueType, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            // 使用不带类型的接口（返回所有类型）
            url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/getGlueList/" + id;
        }
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.debug("getGlueList API 响应: {}", responseBody);
            
            Type resultType = new TypeToken<Result<List<JobLogglue>>>(){}.getType();
            Result<List<JobLogglue>> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 保存连线
     * @param formData 连线表单数据
     * @return 保存后的连线实体
     * @throws IOException 网络异常
     */
    public com.cc.job.xo.model.entity.JobEdge saveJobEdge(com.cc.job.xo.model.form.JobEdgeForm formData) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/saveJobEdge";
        
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
            logger.debug("saveJobEdge API 响应: {}", responseBody);
            
            Type resultType = new TypeToken<Result<com.cc.job.xo.model.entity.JobEdge>>(){}.getType();
            Result<com.cc.job.xo.model.entity.JobEdge> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
}

