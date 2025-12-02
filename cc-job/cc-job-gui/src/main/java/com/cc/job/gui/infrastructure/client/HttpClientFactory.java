package com.cc.job.gui.infrastructure.client;

import com.cc.job.gui.infrastructure.config.ApplicationProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import com.cc.job.gui.util.LocalDateTimeAdapter;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 客户端工厂
 * 
 * <p>负责创建和管理 HTTP 客户端实例
 *
 * @author cc-job-team
 */
public class HttpClientFactory {
    
    private static volatile HttpClientFactory instance;
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    private HttpClientFactory() {
        this.httpClient = createHttpClient();
        this.gson = createGson();
    }
    
    public static HttpClientFactory getInstance() {
        if (instance == null) {
            synchronized (HttpClientFactory.class) {
                if (instance == null) {
                    instance = new HttpClientFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * 创建 HTTP 客户端
     */
    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(ApplicationProperties.Api.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(ApplicationProperties.Api.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(ApplicationProperties.Api.getWriteTimeout(), TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 创建 Gson 实例
     */
    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }
    
    /**
     * 获取 HTTP 客户端
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * 获取 Gson 实例
     */
    public Gson getGson() {
        return gson;
    }
}

