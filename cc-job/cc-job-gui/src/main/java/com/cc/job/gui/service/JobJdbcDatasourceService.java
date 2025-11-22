package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.google.gson.reflect.TypeToken;
import okhttp3.Request;
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
    
    private static final String LIST_API = "/api/v1/jobJdbcDatasource/list";
    
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
}

