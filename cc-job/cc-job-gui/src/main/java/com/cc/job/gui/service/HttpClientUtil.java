package com.cc.job.gui.service;

import com.cc.job.gui.util.ApiUtil;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.util.SessionManager;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.common.result.Result;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HTTP客户端工具类
 * 封装通用的HTTP请求和响应处理逻辑，减少代码重复
 * 支持多地址配置和自动重试
 * 
 * @author xiaozhao
 */
public class HttpClientUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    
    private final ApiUtil apiUtil;
    /**
     * 规范化后的地址列表（只包含协议+主机+端口）
     */
    private final List<String> normalizedBaseUrls;
    
    public HttpClientUtil(ApiUtil apiUtil) {
        this.apiUtil = apiUtil;
        this.normalizedBaseUrls = parseAndNormalizeUrls(apiUtil.getBaseUrls());
        
        if (!normalizedBaseUrls.isEmpty()) {
            logger.info("[HttpClientUtil] 初始化API地址列表: {}", normalizedBaseUrls);
        }
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
            logger.warn("[HttpClientUtil] 未配置API地址，使用默认地址");
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
            logger.warn("[HttpClientUtil] 解析后的API地址列表为空");
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
                logger.warn("[HttpClientUtil] 无效的地址格式: {}", address);
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
            logger.error("[HttpClientUtil] 解析地址失败: {}", address, e);
            return null;
        }
    }
    
    /**
     * 构建完整的API URL（兼容旧代码，返回第一个地址）
     * @deprecated 使用多地址功能，此方法返回第一个地址
     * @param path API路径（如 "/api/v1/jobInfos/page"）
     * @return 完整的URL
     */
    @Deprecated
    public String buildUrl(String path) {
        if (normalizedBaseUrls.isEmpty()) {
            return apiUtil.getBaseUrl() + path;
        }
        return normalizedBaseUrls.get(0) + path;
    }
    
    /**
     * 执行GET请求并返回Result<T>
     * @param path API路径
     * @param responseType 响应数据类型
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> get(String path, Class<T> responseType) throws IOException {
        return get(path, responseType, null);
    }
    
    /**
     * 执行GET请求并返回Result<T>（带查询参数）
     * @param path API路径
     * @param responseType 响应数据类型
     * @param queryParams 查询参数映射
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> get(String path, Class<T> responseType, Map<String, String> queryParams) throws IOException {
        return executeRequestWithRetry(path, queryParams, null, "GET", responseType, null);
    }
    
    /**
     * 执行GET请求并返回Result<T>（使用TypeToken指定复杂类型）
     * @param path API路径
     * @param responseTypeToken 响应数据类型Token
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> get(String path, TypeToken<T> responseTypeToken) throws IOException {
        return get(path, responseTypeToken, null);
    }
    
    /**
     * 执行GET请求并返回Result<T>（使用TypeToken，带查询参数）
     * @param path API路径
     * @param responseTypeToken 响应数据类型Token
     * @param queryParams 查询参数映射
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> get(String path, TypeToken<T> responseTypeToken, Map<String, String> queryParams) throws IOException {
        return executeRequestWithRetry(path, queryParams, null, "GET", null, responseTypeToken);
    }

    /**
     * 执行 GET 请求并返回原始响应体（如 Excel 文件流）
     * @param path API 路径
     * @param queryParams 查询参数，可为 null
     * @return 响应体字节数组，失败返回 null 或抛异常
     */
    public byte[] getRaw(String path, Map<String, String> queryParams) throws IOException {
        IOException lastException = null;
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
                if (queryParams != null) {
                    queryParams.forEach(urlBuilder::addQueryParameter);
                }
                Request.Builder reqBuilder = new Request.Builder().url(urlBuilder.build()).get();
                String auth = SessionManager.getInstance().getAuthorizationHeader();
                if (auth != null && !auth.isEmpty()) {
                    reqBuilder.addHeader("Authorization", auth);
                }
                try (Response response = apiUtil.getClient().newCall(reqBuilder.build()).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        continue;
                    }
                    return response.body().bytes();
                }
            } catch (IOException e) {
                lastException = e;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return null;
    }
    
    /**
     * 执行GET请求并返回PageResult<T>
     * @param path API路径
     * @param itemType 分页项类型
     * @param queryParams 查询参数映射
     * @return PageResult包装的分页数据
     * @throws IOException 网络异常
     */
    public <T> PageResult<T> getPage(String path, Class<T> itemType, Map<String, String> queryParams) throws IOException {
        IOException lastException = null;
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
                
                if (queryParams != null) {
                    queryParams.forEach(urlBuilder::addQueryParameter);
                }
                
                Request.Builder reqBuilder = new Request.Builder().url(urlBuilder.build()).get();
                String auth = SessionManager.getInstance().getAuthorizationHeader();
                if (auth != null && !auth.isEmpty()) {
                    reqBuilder.addHeader("Authorization", auth);
                }
                Request request = reqBuilder.build();
                
                PageResult<T> result = executePageRequest(request, itemType);
                logger.debug("[HttpClientUtil] GET请求成功 - url: {}", url);
                return result;
            } catch (IOException e) {
                logger.warn("[HttpClientUtil] GET请求异常 - address: {}, path: {}, error: {}", 
                        baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("[HttpClientUtil] 所有API地址的GET请求都失败 - path: {}", path);
        throw lastException != null ? lastException : new IOException("所有API地址请求失败");
    }
    
    /**
     * 执行POST请求并返回Result<T>
     * @param path API路径
     * @param requestBody 请求体对象（会被序列化为JSON）
     * @param responseType 响应数据类型
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> post(String path, Object requestBody, Class<T> responseType) throws IOException {
        String jsonBody = apiUtil.getGson().toJson(requestBody);
        return executeRequestWithRetry(path, null, jsonBody, "POST", responseType, null);
    }
    
    /**
     * 执行POST请求并返回Result<T>（使用TypeToken指定复杂类型）
     * @param path API路径
     * @param requestBody 请求体对象
     * @param responseTypeToken 响应数据类型Token
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> post(String path, Object requestBody, TypeToken<T> responseTypeToken) throws IOException {
        String jsonBody = apiUtil.getGson().toJson(requestBody);
        return executeRequestWithRetry(path, null, jsonBody, "POST", null, responseTypeToken);
    }
    
    /**
     * 执行POST请求并返回boolean（用于返回Result<Void>的情况）
     * @param path API路径
     * @param requestBody 请求体对象
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean postForBoolean(String path, Object requestBody) throws IOException {
        Result<Void> result = post(path, requestBody, Void.class);
        return Result.isSuccess(result);
    }
    
    /**
     * 执行PUT请求并返回Result<T>
     * @param path API路径
     * @param requestBody 请求体对象
     * @param responseType 响应数据类型
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> put(String path, Object requestBody, Class<T> responseType) throws IOException {
        String jsonBody = apiUtil.getGson().toJson(requestBody);
        return executeRequestWithRetry(path, null, jsonBody, "PUT", responseType, null);
    }
    
    /**
     * 执行PUT请求并返回boolean（用于返回Result<Void>的情况）
     * @param path API路径
     * @param requestBody 请求体对象
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean putForBoolean(String path, Object requestBody) throws IOException {
        Result<Void> result = put(path, requestBody, Void.class);
        return Result.isSuccess(result);
    }
    
    /**
     * 执行DELETE请求并返回Result<T>
     * @param path API路径
     * @param responseType 响应数据类型
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> delete(String path, Class<T> responseType) throws IOException {
        return delete(path, responseType, null);
    }
    
    /**
     * 执行DELETE请求并返回Result<T>（带查询参数）
     * @param path API路径
     * @param responseType 响应数据类型
     * @param queryParams 查询参数映射
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> delete(String path, Class<T> responseType, Map<String, String> queryParams) throws IOException {
        return executeRequestWithRetry(path, queryParams, null, "DELETE", responseType, null);
    }
    
    /**
     * 执行DELETE请求并返回boolean（用于返回Result<Void>的情况）
     * @param path API路径
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean deleteForBoolean(String path) throws IOException {
        return deleteForBoolean(path, null);
    }
    
    /**
     * 执行DELETE请求并返回boolean（带查询参数）
     * @param path API路径
     * @param queryParams 查询参数映射
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean deleteForBoolean(String path, Map<String, String> queryParams) throws IOException {
        Result<Void> result = delete(path, Void.class, queryParams);
        return Result.isSuccess(result);
    }
    
    /**
     * 执行自定义请求并返回Result<T>
     * @param requestBuilder 请求构建器函数
     * @param path API路径
     * @param responseType 响应数据类型
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> executeCustomRequest(Function<String, Request.Builder> requestBuilder, 
                                               String path, 
                                               Class<T> responseType) throws IOException {
        IOException lastException = null;
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                Request request = requestBuilder.apply(url).build();
                Result<T> result = executeRequest(request, responseType);
                logger.debug("[HttpClientUtil] 自定义请求成功 - url: {}", url);
                return result;
            } catch (IOException e) {
                logger.warn("[HttpClientUtil] 自定义请求异常 - address: {}, path: {}, error: {}", 
                        baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("[HttpClientUtil] 所有API地址的自定义请求都失败 - path: {}", path);
        throw lastException != null ? lastException : new IOException("所有API地址请求失败");
    }
    
    /**
     * 执行请求并重试多个地址
     * @param path API路径
     * @param queryParams 查询参数（可为null）
     * @param jsonBody JSON请求体（可为null）
     * @param method HTTP方法（GET, POST, PUT, DELETE）
     * @param responseType 响应数据类型（可为null，如果使用TypeToken）
     * @param responseTypeToken 响应数据类型Token（可为null，如果使用Class）
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    private <T> Result<T> executeRequestWithRetry(String path, 
                                                    Map<String, String> queryParams,
                                                    String jsonBody,
                                                    String method,
                                                    Class<T> responseType,
                                                    TypeToken<T> responseTypeToken) throws IOException {
        IOException lastException = null;
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                Request.Builder requestBuilder;
                
                // 构建URL和查询参数
                if (queryParams != null && !queryParams.isEmpty()) {
                    HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
                    queryParams.forEach(urlBuilder::addQueryParameter);
                    url = urlBuilder.build().toString();
                }
                
                // 根据方法构建请求
                switch (method.toUpperCase()) {
                    case "GET":
                        requestBuilder = new Request.Builder().url(url).get();
                        break;
                    case "POST":
                        RequestBody body = jsonBody != null ? 
                                RequestBody.create(jsonBody, JSON_MEDIA_TYPE) : null;
                        requestBuilder = new Request.Builder().url(url).post(body);
                        break;
                    case "PUT":
                        RequestBody putBody = jsonBody != null ? 
                                RequestBody.create(jsonBody, JSON_MEDIA_TYPE) : null;
                        requestBuilder = new Request.Builder().url(url).put(putBody);
                        break;
                    case "DELETE":
                        requestBuilder = new Request.Builder().url(url).delete();
                        break;
                    default:
                        throw new IllegalArgumentException("不支持的HTTP方法: " + method);
                }
                String auth = SessionManager.getInstance().getAuthorizationHeader();
                if (auth != null && !auth.isEmpty()) {
                    requestBuilder.addHeader("Authorization", auth);
                }
                Request request = requestBuilder.build();
                
                // 根据类型执行请求
                if (responseTypeToken != null) {
                    Result<T> result = executeRequest(request, responseTypeToken);
                    logger.debug("[HttpClientUtil] {}请求成功 - url: {}", method, url);
                    return result;
                } else {
                    Result<T> result = executeRequest(request, responseType);
                    logger.debug("[HttpClientUtil] {}请求成功 - url: {}", method, url);
                    return result;
                }
            } catch (IOException e) {
                logger.warn("[HttpClientUtil] {}请求异常 - address: {}, path: {}, error: {}", 
                        method, baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("[HttpClientUtil] 所有API地址的{}请求都失败 - path: {}", method, path);
        throw lastException != null ? lastException : new IOException("所有API地址请求失败");
    }
    
    /**
     * 执行请求并解析Result响应
     * @param request HTTP请求
     * @param responseType 响应数据类型
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    private <T> Result<T> executeRequest(Request request, Class<T> responseType) throws IOException {
        Type resultType = TypeToken.getParameterized(Result.class, responseType).getType();
        return executeRequestInternal(request, resultType);
    }
    
    /**
     * 执行请求并解析Result响应（使用TypeToken）
     * @param request HTTP请求
     * @param responseTypeToken 响应数据类型Token
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    private <T> Result<T> executeRequest(Request request, TypeToken<T> responseTypeToken) throws IOException {
        Type resultType = TypeToken.getParameterized(Result.class, responseTypeToken.getType()).getType();
        return executeRequestInternal(request, resultType);
    }
    
    /**
     * 执行请求并解析PageResult响应
     * @param request HTTP请求
     * @param itemType 分页项类型
     * @return PageResult包装的分页数据
     * @throws IOException 网络异常
     */
    private <T> PageResult<T> executePageRequest(Request request, Class<T> itemType) throws IOException {
        Type resultType = TypeToken.getParameterized(PageResult.class, itemType).getType();
        return executeRequestInternal(request, resultType);
    }
    
    /**
     * 内部方法：执行HTTP请求并解析响应
     * @param request HTTP请求
     * @param resultType 结果类型
     * @return 解析后的响应对象
     * @throws IOException 网络异常
     */
    @SuppressWarnings("unchecked")
    private <T> T executeRequestInternal(Request request, Type resultType) throws IOException {
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                NotificationToast.showError("请求失败: HTTP " + response.code() + " - " + errorBody);
                throw new IOException("请求失败: HTTP " + response.code() + " - " + errorBody);
            }
            String responseBody = response.body().string();
            return (T) apiUtil.getGson().fromJson(responseBody, resultType);
        }
    }
    
    /**
     * 从Result中提取数据，如果失败则抛出异常
     * @param result Result对象
     * @param errorMessage 错误消息前缀
     * @return 提取的数据
     * @throws IOException 如果Result不成功
     */
    public <T> T extractData(Result<T> result, String errorMessage) throws IOException {
        if (Result.isSuccess(result)) {
            T data = result.getData();
            if (data == null) {
                throw new IOException(errorMessage + ": 数据为空");
            }
            return data;
        } else {
            throw new IOException(errorMessage + ": " + result.getMsg());
        }
    }
    
    /**
     * 从Result中提取数据，允许数据为空
     * @param result Result对象
     * @param errorMessage 错误消息前缀
     * @return 提取的数据（可能为null）
     * @throws IOException 如果Result不成功
     */
    public <T> T extractDataOrNull(Result<T> result, String errorMessage) throws IOException {
        if (Result.isSuccess(result)) {
            return result.getData();
        } else {
            throw new IOException(errorMessage + ": " + result.getMsg());
        }
    }
    
    /**
     * 构建查询参数映射（从查询对象中提取非空字段）
     * @param query 查询对象
     * @param fieldExtractors 字段提取器映射（字段名 -> 值提取函数）
     * @return 查询参数映射
     */
    public static <T> Map<String, String> buildQueryParams(T query, Map<String, Function<T, Object>> fieldExtractors) {
        Map<String, String> params = new java.util.HashMap<>();
        if (query != null && fieldExtractors != null) {
            fieldExtractors.forEach((key, extractor) -> {
                Object value = extractor.apply(query);
                if (value != null) {
                    if (value instanceof String && !((String) value).isEmpty()) {
                        params.put(key, (String) value);
                    } else if (!(value instanceof String)) {
                        params.put(key, String.valueOf(value));
                    }
                }
            });
        }
        return params;
    }
}

