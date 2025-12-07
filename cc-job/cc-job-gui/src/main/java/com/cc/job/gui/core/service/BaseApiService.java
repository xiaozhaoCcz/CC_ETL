package com.cc.job.gui.core.service;

import com.cc.job.gui.infrastructure.client.ApiClient;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;

/**
 * 基础 API 服务
 * 
 * <p>所有 API 服务的基类，提供通用的 API 调用能力
 *
 * @author cc-job-team
 */
public abstract class BaseApiService {
    
    protected final ApiClient apiClient;
    protected final String baseUrl;
    protected final Gson gson;
    
    protected BaseApiService() {
        this.apiClient = ApiClient.getInstance();
        this.baseUrl = apiClient.getBaseUrl();
        this.gson = apiClient.getGson();
    }
    
    /**
     * 构建完整URL
     */
    protected String buildUrl(String path) {
        return baseUrl + path;
    }
}

