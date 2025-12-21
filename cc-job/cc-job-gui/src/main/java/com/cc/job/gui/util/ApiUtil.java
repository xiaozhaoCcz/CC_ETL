package com.cc.job.gui.util;

import com.cc.job.gui.infrastructure.client.HttpClientFactory;
import com.cc.job.gui.infrastructure.config.ApplicationProperties;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    /**
     * 规范化后的地址列表（只包含协议+主机+端口）
     */
    private final List<String> normalizedBaseUrls;

    /**
     * 获取基础URL（单个，兼容旧代码）
     * @deprecated 使用 ApplicationProperties.Api.getBaseUrls() 获取多个地址
     */
    @Deprecated
    public String getBaseUrl() {
        return ApplicationProperties.Api.getBaseUrl();
    }
    
    /**
     * 获取基础URL列表（多个，以逗号分隔）
     * @return 多个URL地址，以逗号分隔
     */
    public String getBaseUrls() {
        return ApplicationProperties.Api.getBaseUrls();
    }

    public OkHttpClient getClient() {
        return clientFactory.getHttpClient();
    }

    public Gson getGson() {
        return clientFactory.getGson();
    }

    public ApiUtil() {
        this.clientFactory = HttpClientFactory.getInstance();
        this.normalizedBaseUrls = parseAndNormalizeUrls(getBaseUrls());
        
        if (!normalizedBaseUrls.isEmpty()) {
            logger.info("[ApiUtil] 初始化API地址列表: {}", normalizedBaseUrls);
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
            logger.warn("[ApiUtil] 未配置API地址，使用默认地址");
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
            logger.warn("[ApiUtil] 解析后的API地址列表为空");
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
                logger.warn("[ApiUtil] 无效的地址格式: {}", address);
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
            logger.error("[ApiUtil] 解析地址失败: {}", address, e);
            return null;
        }
    }
    
    /**
     * 执行请求并重试多个地址
     * @param path API路径（如 "/api/v1/datax/getTables/123"）
     * @param requestBuilder 请求构建器函数，接收完整URL并返回Request
     * @return Response，如果所有地址都失败则抛出异常
     * @throws IOException 网络异常
     */
    public Response executeRequestWithRetry(String path, java.util.function.Function<String, Request> requestBuilder) throws IOException {
        IOException lastException = null;
        
        // 如果没有配置多地址，使用单个地址（兼容旧代码）
        if (normalizedBaseUrls.isEmpty()) {
            String url = getBaseUrl() + path;
            Request request = requestBuilder.apply(url);
            return getClient().newCall(request).execute();
        }
        
        for (String baseUrl : normalizedBaseUrls) {
            try {
                String url = baseUrl + path;
                Request request = requestBuilder.apply(url);
                Response response = getClient().newCall(request).execute();
                
                if (response.isSuccessful()) {
                    logger.debug("[ApiUtil] 请求成功 - url: {}", url);
                    return response;
                } else {
                    logger.warn("[ApiUtil] 请求失败 - code: {}, url: {}", response.code(), url);
                    response.close();
                }
            } catch (IOException e) {
                logger.warn("[ApiUtil] 请求异常 - address: {}, path: {}, error: {}", 
                        baseUrl, path, e.getMessage());
                lastException = e;
            }
        }
        
        logger.error("[ApiUtil] 所有API地址的请求都失败 - path: {}", path);
        throw lastException != null ? lastException : new IOException("所有API地址请求失败");
    }
}



