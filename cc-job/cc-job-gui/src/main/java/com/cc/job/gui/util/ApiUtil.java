package com.cc.job.gui.util;

import com.cc.job.gui.infrastructure.client.HttpClientFactory;
import com.cc.job.gui.infrastructure.config.ApplicationProperties;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API 工具类
 * 
 * @author xiaozhao
 * @deprecated 使用 {@link com.cc.job.gui.infrastructure.client.ApiClient} 替代
 */
@Deprecated
public class ApiUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiUtil.class);

    private static ApiUtil instance;

    public static ApiUtil getInstance() {
        if (instance == null) {
            instance = new ApiUtil();
        }
        return instance;
    }

    private final HttpClientFactory clientFactory;

    public String getBaseUrl() {
        return ApplicationProperties.Api.getBaseUrl();
    }

    public OkHttpClient getClient() {
        return clientFactory.getHttpClient();
    }

    public Gson getGson() {
        return clientFactory.getGson();
    }

    public ApiUtil() {
        this.clientFactory = HttpClientFactory.getInstance();
    }
}



