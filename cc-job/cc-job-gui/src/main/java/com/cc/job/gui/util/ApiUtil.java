package com.cc.job.gui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class ApiUtil {

    private static ApiUtil instance;

    public static ApiUtil getInstance() {
        if (instance == null) {
            instance = new ApiUtil();
        }
        return instance;
    }

    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;

    public String getBaseUrl() {
        return baseUrl;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public Gson getGson() {
        return gson;
    }

    public ApiUtil() {
        this.baseUrl = AppConfig.getBaseUrl();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(AppConfig.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(AppConfig.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(AppConfig.getReadTimeout(), TimeUnit.SECONDS)
                .build();
        
        // 配置 Gson，添加 LocalDateTime 适配器以解决 Java 模块系统限制
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        System.out.println("✓ API Service 初始化完成");
        System.out.println("  后端地址: " + this.baseUrl);
    }
}



