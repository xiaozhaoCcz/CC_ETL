package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobGroupForm;
import com.cc.job.xo.model.query.JobGroupQuery;
import com.cc.job.xo.model.vo.JobGroupVO;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * JobGroup服务类
 */
public class JobGroupService extends BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupService.class);
    
    /**
     * 获取所有JobGroup列表
     * @return JobGroup列表
     * @throws IOException 网络异常
     */
    public List<JobGroup> getAllJobGroupList() throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobGroups/getAllJobGroupList";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<List<JobGroup>>>(){}.getType();
            Result<List<JobGroup>> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 分页查询执行器列表
     * @param query 查询参数
     * @return 分页结果
     * @throws IOException 网络异常
     */
    public PageResult<JobGroupVO> getJobGroupPage(JobGroupQuery query) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobGroups/page";
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (query != null) {
            urlBuilder.addQueryParameter("pageNum", String.valueOf(query.getPageNum()));
            urlBuilder.addQueryParameter("pageSize", String.valueOf(query.getPageSize()));
            if (query.getAppName() != null && !query.getAppName().isEmpty()) {
                urlBuilder.addQueryParameter("appName", query.getAppName());
            }
            if (query.getTitle() != null && !query.getTitle().isEmpty()) {
                urlBuilder.addQueryParameter("title", query.getTitle());
            }
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            
            Type resultType = new TypeToken<PageResult<JobGroupVO>>(){}.getType();
            return apiUtil.getGson().fromJson(responseBody, resultType);
        }
    }
    
    /**
     * 查看执行器注册节点地址列表
     * @param id 执行器ID
     * @return 地址列表
     * @throws IOException 网络异常
     */
    public List<String> findAddressList(Long id) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobGroups/findAddressList/" + id;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            
            Type resultType = new TypeToken<Result<List<String>>>(){}.getType();
            Result<List<String>> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 新增执行器
     * @param form 表单数据
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean saveJobGroup(JobGroupForm form) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobGroups";
        
        String jsonBody = apiUtil.getGson().toJson(form);
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
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 更新执行器
     * @param id 执行器ID
     * @param form 表单数据
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean updateJobGroup(Long id, JobGroupForm form) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobGroups/" + id;
        
        String jsonBody = apiUtil.getGson().toJson(form);
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
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 删除执行器
     * @param ids 执行器ID，多个以逗号分隔
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean deleteJobGroups(String ids) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobGroups/" + ids;
        
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 获取执行器表单数据
     * @param id 执行器ID
     * @return 表单数据
     * @throws IOException 网络异常
     */
    public JobGroupForm getJobGroupForm(Long id) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobGroups/" + id + "/form";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            
            Type resultType = new TypeToken<Result<JobGroupForm>>(){}.getType();
            Result<JobGroupForm> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
}

