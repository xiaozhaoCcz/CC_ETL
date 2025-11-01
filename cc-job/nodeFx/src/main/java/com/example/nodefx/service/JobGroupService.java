package com.example.nodefx.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobGroup;
import com.google.gson.reflect.TypeToken;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * JobGroup服务类
 */
public class JobGroupService extends BaseService {
    
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
            System.out.println("getAllJobGroupList API 响应: " + responseBody);
            
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
}

