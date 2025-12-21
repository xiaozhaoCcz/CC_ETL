package com.cc.job.gui.infrastructure.client;

import com.cc.job.gui.infrastructure.config.ApplicationProperties;
import com.cc.job.gui.infrastructure.constant.GuiConstants;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API 客户端
 * 
 * <p>统一的 HTTP API 调用客户端，支持多地址配置和自动重试
 *
 * @author cc-job-team
 */
public class ApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private static volatile ApiClient instance;
    
    /**
     * 规范化后的地址列表（只包含协议+主机+端口）
     */
    private final List<String> normalizedBaseUrls;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    private ApiClient() {
        HttpClientFactory factory = HttpClientFactory.getInstance();
        this.normalizedBaseUrls = parseAndNormalizeUrls(ApplicationProperties.Api.getBaseUrls());
        this.httpClient = factory.getHttpClient();
        this.gson = factory.getGson();
        
        if (!normalizedBaseUrls.isEmpty()) {
            logger.info("[ApiClient] 初始化API地址列表: {}", normalizedBaseUrls);
        }
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
     * 解析和规范化URL地址列表
     * 支持逗号分隔的多个地址，自动移除路径部分
     * 
     * @param urls 逗号分隔的URL字符串
     * @return 规范化后的地址列表
     */
    private List<String> parseAndNormalizeUrls(String urls) {
        List<String> normalizedUrls = new ArrayList<>();
        
        if (urls == null || urls.trim().isEmpty()) {
            logger.warn("[ApiClient] 未配置API地址，使用默认地址: {}", GuiConstants.Api.DEFAULT_BASE_URL);
            normalizedUrls.add(normalizeAddress(GuiConstants.Api.DEFAULT_BASE_URL));
            return normalizedUrls;
        }
        
        // 按逗号分割地址
        String[] addresses = urls.split(",");
        normalizedUrls = Arrays.stream(addresses)
                .map(String::trim)
                .filter(addr -> !addr.isEmpty())
                .map(this::normalizeAddress)
                .filter(addr -> addr != null)
                .collect(Collectors.toList());
        
        if (normalizedUrls.isEmpty()) {
            logger.warn("[ApiClient] 解析后的API地址列表为空，使用默认地址: {}", GuiConstants.Api.DEFAULT_BASE_URL);
            normalizedUrls.add(normalizeAddress(GuiConstants.Api.DEFAULT_BASE_URL));
        }
        
        return normalizedUrls;
    }
    
    /**
     * 规范化地址，移除路径部分，只保留协议+主机+端口
     * 例如: http://127.0.0.1:8989/xxl-job-admin -> http://127.0.0.1:8989
     * 
     * @param address 原始地址
     * @return 规范化后的地址
     */
    private String normalizeAddress(String address) {
        try {
            URI uri = URI.create(address);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            
            if (scheme == null || host == null) {
                logger.warn("[ApiClient] 无效的地址格式: {}", address);
                return null;
            }
            
            // 构建规范化地址：协议://主机:端口
            if (port > 0) {
                return scheme + "://" + host + ":" + port;
            } else {
                // 如果没有端口，使用默认端口
                if ("https".equals(scheme)) {
                    return scheme + "://" + host + ":443";
                } else {
                    return scheme + "://" + host + ":80";
                }
            }
        } catch (Exception e) {
            logger.error("[ApiClient] 解析地址失败: {}", address, e);
            return null;
        }
    }
    
    /**
     * 获取基础URL（单个，兼容旧代码）
     * @deprecated 使用多地址功能，此方法返回第一个地址
     */
    @Deprecated
    public String getBaseUrl() {
        return normalizedBaseUrls.isEmpty() ? GuiConstants.Api.DEFAULT_BASE_URL : normalizedBaseUrls.get(0);
    }
    
    /**
     * 获取 Gson 实例
     */
    public Gson getGson() {
        return gson;
    }
    
    /**
     * 执行 GET 请求
     * @param path API路径（如 "/api/v1/jobInfos/123"）
     */
    public Response get(String path) throws IOException {
        return executeGet(path, null);
    }
    
    /**
     * 执行 GET 请求（带请求头）
     * @param path API路径
     * @param headers 请求头
     */
    public Response get(String path, Headers headers) throws IOException {
        return executeGet(path, headers);
    }
    
    /**
     * 执行 GET 请求，尝试所有地址直到成功
     * @param path API路径
     * @param headers 请求头（可为null）
     * @return Response，如果所有地址都失败则抛出异常
     */
    private Response executeGet(String path, Headers headers) throws IOException {
        IOException lastException = null;
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                logger.debug("{} GET请求 - url: {}", GuiConstants.Logging.TAG_API, url);
                
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .get();
                
                if (headers != null) {
                    requestBuilder.headers(headers);
                }
                
                Request request = requestBuilder.build();
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    logger.debug("{} GET请求成功 - url: {}", GuiConstants.Logging.TAG_API, url);
                    return response;
                } else {
                    logger.warn("{} GET请求失败 - code: {}, url: {}", 
                            GuiConstants.Logging.TAG_API, response.code(), url);
                    response.close();
                }
            } catch (IOException e) {
                logger.warn("{} GET请求异常 - address: {}, path: {}, error: {}", 
                        GuiConstants.Logging.TAG_API, baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("{} 所有API地址的GET请求都失败 - path: {}", GuiConstants.Logging.TAG_API, path);
        throw lastException != null ? lastException : 
                new IOException(GuiConstants.ErrorMessage.CONNECTION_FAILED);
    }
    
    /**
     * 执行 POST 请求
     * @param path API路径
     * @param jsonBody JSON请求体
     */
    public Response post(String path, String jsonBody) throws IOException {
        return executePost(path, jsonBody, null);
    }
    
    /**
     * 执行 POST 请求（带请求头）
     * @param path API路径
     * @param jsonBody JSON请求体
     * @param headers 请求头
     */
    public Response post(String path, String jsonBody, Headers headers) throws IOException {
        return executePost(path, jsonBody, headers);
    }
    
    /**
     * 执行 POST 请求，尝试所有地址直到成功
     * @param path API路径
     * @param jsonBody JSON请求体
     * @param headers 请求头（可为null）
     * @return Response，如果所有地址都失败则抛出异常
     */
    private Response executePost(String path, String jsonBody, Headers headers) throws IOException {
        IOException lastException = null;
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                logger.debug("{} POST请求 - url: {}", GuiConstants.Logging.TAG_API, url);
                
                RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .post(body);
                
                if (headers != null) {
                    requestBuilder.headers(headers);
                }
                
                Request request = requestBuilder.build();
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    logger.debug("{} POST请求成功 - url: {}", GuiConstants.Logging.TAG_API, url);
                    return response;
                } else {
                    logger.warn("{} POST请求失败 - code: {}, url: {}", 
                            GuiConstants.Logging.TAG_API, response.code(), url);
                    response.close();
                }
            } catch (IOException e) {
                logger.warn("{} POST请求异常 - address: {}, path: {}, error: {}", 
                        GuiConstants.Logging.TAG_API, baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("{} 所有API地址的POST请求都失败 - path: {}", GuiConstants.Logging.TAG_API, path);
        throw lastException != null ? lastException : 
                new IOException(GuiConstants.ErrorMessage.CONNECTION_FAILED);
    }
    
    /**
     * 执行 PUT 请求
     * @param path API路径
     * @param jsonBody JSON请求体
     * @param headers 请求头
     */
    public Response put(String path, String jsonBody, Headers headers) throws IOException {
        return executePut(path, jsonBody, headers);
    }
    
    /**
     * 执行 PUT 请求，尝试所有地址直到成功
     * @param path API路径
     * @param jsonBody JSON请求体
     * @param headers 请求头（可为null）
     * @return Response，如果所有地址都失败则抛出异常
     */
    private Response executePut(String path, String jsonBody, Headers headers) throws IOException {
        IOException lastException = null;
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                logger.debug("{} PUT请求 - url: {}", GuiConstants.Logging.TAG_API, url);
                
                RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .put(body);
                
                if (headers != null) {
                    requestBuilder.headers(headers);
                }
                
                Request request = requestBuilder.build();
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    logger.debug("{} PUT请求成功 - url: {}", GuiConstants.Logging.TAG_API, url);
                    return response;
                } else {
                    logger.warn("{} PUT请求失败 - code: {}, url: {}", 
                            GuiConstants.Logging.TAG_API, response.code(), url);
                    response.close();
                }
            } catch (IOException e) {
                logger.warn("{} PUT请求异常 - address: {}, path: {}, error: {}", 
                        GuiConstants.Logging.TAG_API, baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("{} 所有API地址的PUT请求都失败 - path: {}", GuiConstants.Logging.TAG_API, path);
        throw lastException != null ? lastException : 
                new IOException(GuiConstants.ErrorMessage.CONNECTION_FAILED);
    }
    
    /**
     * 执行 DELETE 请求
     * @param path API路径
     * @param headers 请求头
     */
    public Response delete(String path, Headers headers) throws IOException {
        return executeDelete(path, headers);
    }
    
    /**
     * 执行 DELETE 请求，尝试所有地址直到成功
     * @param path API路径
     * @param headers 请求头（可为null）
     * @return Response，如果所有地址都失败则抛出异常
     */
    private Response executeDelete(String path, Headers headers) throws IOException {
        IOException lastException = null;
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                logger.debug("{} DELETE请求 - url: {}", GuiConstants.Logging.TAG_API, url);
                
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .delete();
                
                if (headers != null) {
                    requestBuilder.headers(headers);
                }
                
                Request request = requestBuilder.build();
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    logger.debug("{} DELETE请求成功 - url: {}", GuiConstants.Logging.TAG_API, url);
                    return response;
                } else {
                    logger.warn("{} DELETE请求失败 - code: {}, url: {}", 
                            GuiConstants.Logging.TAG_API, response.code(), url);
                    response.close();
                }
            } catch (IOException e) {
                logger.warn("{} DELETE请求异常 - address: {}, path: {}, error: {}", 
                        GuiConstants.Logging.TAG_API, baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("{} 所有API地址的DELETE请求都失败 - path: {}", GuiConstants.Logging.TAG_API, path);
        throw lastException != null ? lastException : 
                new IOException(GuiConstants.ErrorMessage.CONNECTION_FAILED);
    }
}

