package com.cc.job.gui.service;

import com.cc.job.gui.util.ApiUtil;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.common.result.Result;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;

/**
 * HTTP客户端工具类
 * 封装通用的HTTP请求和响应处理逻辑，减少代码重复
 * 
 * @author xiaozhao
 */
public class HttpClientUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    
    private final ApiUtil apiUtil;
    
    public HttpClientUtil(ApiUtil apiUtil) {
        this.apiUtil = apiUtil;
    }
    
    /**
     * 构建完整的API URL
     * @param path API路径（如 "/api/v1/jobInfos/page"）
     * @return 完整的URL
     */
    public String buildUrl(String path) {
        return apiUtil.getBaseUrl() + path;
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
        String url = buildUrl(path);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        
        if (queryParams != null) {
            queryParams.forEach(urlBuilder::addQueryParameter);
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        
        return executeRequest(request, responseType);
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
        String url = buildUrl(path);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        
        if (queryParams != null) {
            queryParams.forEach(urlBuilder::addQueryParameter);
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        
        return executeRequest(request, responseTypeToken);
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
        String url = buildUrl(path);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        
        if (queryParams != null) {
            queryParams.forEach(urlBuilder::addQueryParameter);
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        
        return executePageRequest(request, itemType);
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
        String url = buildUrl(path);
        String jsonBody = apiUtil.getGson().toJson(requestBody);
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        return executeRequest(request, responseType);
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
        String url = buildUrl(path);
        String jsonBody = apiUtil.getGson().toJson(requestBody);
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        return executeRequest(request, responseTypeToken);
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
        String url = buildUrl(path);
        String jsonBody = apiUtil.getGson().toJson(requestBody);
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        
        return executeRequest(request, responseType);
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
        String url = buildUrl(path);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        
        if (queryParams != null) {
            queryParams.forEach(urlBuilder::addQueryParameter);
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .delete()
                .build();
        
        return executeRequest(request, responseType);
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
     * @param responseType 响应数据类型
     * @return Result包装的响应数据
     * @throws IOException 网络异常
     */
    public <T> Result<T> executeCustomRequest(Function<String, Request.Builder> requestBuilder, 
                                               String path, 
                                               Class<T> responseType) throws IOException {
        String url = buildUrl(path);
        Request request = requestBuilder.apply(url).build();
        return executeRequest(request, responseType);
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

