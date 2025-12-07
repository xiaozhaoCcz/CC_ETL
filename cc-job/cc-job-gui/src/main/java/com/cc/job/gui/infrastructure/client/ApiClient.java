package com.cc.job.gui.infrastructure.client;

import com.cc.job.gui.infrastructure.config.ApplicationProperties;
import com.cc.job.gui.infrastructure.constant.GuiConstants;
import com.cc.job.gui.infrastructure.exception.ApiException;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * API 客户端
 * 
 * <p>统一的 HTTP API 调用客户端
 *
 * @author cc-job-team
 */
public class ApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private static volatile ApiClient instance;
    
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    private ApiClient() {
        HttpClientFactory factory = HttpClientFactory.getInstance();
        this.baseUrl = ApplicationProperties.Api.getBaseUrl();
        this.httpClient = factory.getHttpClient();
        this.gson = factory.getGson();
    }
    
    public static ApiClient getInstance() {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取基础URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * 获取 Gson 实例
     */
    public Gson getGson() {
        return gson;
    }
    
    /**
     * 执行 GET 请求
     */
    public Response get(String url) throws IOException {
        logger.debug("{} GET请求 - url: {}", GuiConstants.Logging.TAG_API, url);
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * 执行 GET 请求（带请求头）
     */
    public Response get(String url, Headers headers) throws IOException {
        logger.debug("{} GET请求 - url: {}", GuiConstants.Logging.TAG_API, url);
        
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .get()
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * 执行 POST 请求
     */
    public Response post(String url, String jsonBody) throws IOException {
        logger.debug("{} POST请求 - url: {}", GuiConstants.Logging.TAG_API, url);
        
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * 执行 POST 请求（带请求头）
     */
    public Response post(String url, String jsonBody, Headers headers) throws IOException {
        logger.debug("{} POST请求 - url: {}", GuiConstants.Logging.TAG_API, url);
        
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .post(body)
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * 执行 PUT 请求
     */
    public Response put(String url, String jsonBody, Headers headers) throws IOException {
        logger.debug("{} PUT请求 - url: {}", GuiConstants.Logging.TAG_API, url);
        
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .put(body)
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * 执行 DELETE 请求
     */
    public Response delete(String url, Headers headers) throws IOException {
        logger.debug("{} DELETE请求 - url: {}", GuiConstants.Logging.TAG_API, url);
        
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .delete()
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * 执行请求
     */
    private Response executeRequest(Request request) throws IOException {
        try {
            Response response = httpClient.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                logger.error("{} 请求失败 - code: {}, url: {}", 
                        GuiConstants.Logging.TAG_API, response.code(), request.url());
                throw new ApiException(response.code(), 
                        GuiConstants.ErrorMessage.REQUEST_FAILED + ": " + response.code());
            }
            
            return response;
            
        } catch (IOException e) {
            logger.error("{} 网络请求异常 - url: {}", 
                    GuiConstants.Logging.TAG_API, request.url(), e);
            throw new IOException(GuiConstants.ErrorMessage.CONNECTION_FAILED, e);
        }
    }
}

