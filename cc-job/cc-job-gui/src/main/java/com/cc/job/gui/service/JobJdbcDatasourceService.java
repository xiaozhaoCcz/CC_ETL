package com.cc.job.gui.service;

import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.form.JobJdbcDatasourceForm;
import com.cc.job.xo.model.query.JobJdbcDatasourceQuery;
import com.cc.job.xo.model.vo.JobJdbcDatasourceVO;
import com.google.gson.reflect.TypeToken;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * JDBC数据源服务
 */
public class JobJdbcDatasourceService extends BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobJdbcDatasourceService.class);
    
    private static final String BASE_API = "/api/v1/jobJdbcDatasource";
    private static final String LIST_API = BASE_API + "/list";
    private static final String PAGE_API = BASE_API + "/page";
    private static final String CONNECT_API = BASE_API + "/isConnect";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    /**
     * 获取数据源列表
     * @return 数据源列表
     * @throws IOException 网络异常
     */
    public List<JobJdbcDatasource> getDatasourceList() throws IOException {
        String url = apiUtil.getBaseUrl() + LIST_API;
        
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
            Type resultType = new TypeToken<Result<List<JobJdbcDatasource>>>(){}.getType();
            Result<List<JobJdbcDatasource>> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 分页查询数据源
     */
    public PageResult<JobJdbcDatasourceVO> getDatasourcePage(JobJdbcDatasourceQuery query) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(apiUtil.getBaseUrl() + PAGE_API);
        urlBuilder.append("?pageNum=").append(query.getPageNum());
        urlBuilder.append("&pageSize=").append(query.getPageSize());
        if (query.getDatasourceName() != null && !query.getDatasourceName().isEmpty()) {
            urlBuilder.append("&datasourceName=").append(query.getDatasourceName());
        }
        if (query.getDatasource() != null && !query.getDatasource().isEmpty()) {
            urlBuilder.append("&datasource=").append(query.getDatasource());
        }
        if (query.getDatabaseName() != null && !query.getDatabaseName().isEmpty()) {
            urlBuilder.append("&databaseName=").append(query.getDatabaseName());
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String responseBody = response.body().string();
            Type resultType = new TypeToken<PageResult<JobJdbcDatasourceVO>>(){}.getType();
            return apiUtil.getGson().fromJson(responseBody, resultType);
        }
    }
    
    /**
     * 获取数据源表单数据
     */
    public JobJdbcDatasourceForm getFormData(Long id) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API + "/" + id + "/form";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String responseBody = response.body().string();
            Type resultType = new TypeToken<Result<JobJdbcDatasourceForm>>(){}.getType();
            Result<JobJdbcDatasourceForm> result = apiUtil.getGson().fromJson(responseBody, resultType);
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 新增数据源
     */
    public boolean saveDatasource(JobJdbcDatasourceForm form) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API;
        String json = apiUtil.getGson().toJson(form);
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON))
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
     * 更新数据源
     */
    public boolean updateDatasource(Long id, JobJdbcDatasourceForm form) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API + "/" + id;
        String json = apiUtil.getGson().toJson(form);
        
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(json, JSON))
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
     * 批量删除数据源
     */
    public boolean deleteDatasources(String ids) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API + "/" + ids;
        
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
     * 测试数据源连接
     */
    public boolean testConnection(JobJdbcDatasourceForm form) throws IOException {
        String url = apiUtil.getBaseUrl() + CONNECT_API;
        String json = apiUtil.getGson().toJson(form);
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON))
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String responseBody = response.body().string();
            Type resultType = new TypeToken<Result<Boolean>>(){}.getType();
            Result<Boolean> result = apiUtil.getGson().fromJson(responseBody, resultType);
            return Result.isSuccess(result) && Boolean.TRUE.equals(result.getData());
        }
    }
}

