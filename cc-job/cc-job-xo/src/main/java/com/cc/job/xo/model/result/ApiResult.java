package com.cc.job.xo.model.result;

import java.util.Map;
import java.util.Objects;

/**
 * API节点执行结果
 * 
 * <p>包含HTTP请求和响应的完整信息
 *
 * @author cc-job-team
 */
public class ApiResult {
    
    /** HTTP状态码 */
    private Integer statusCode;
    
    /** 响应头 */
    private Map<String, String> headers;
    
    /** 响应体（自动解析JSON） */
    private Object body;
    
    /** 内容类型 */
    private String contentType;
    
    /** 原始响应体（字符串） */
    private String rawBody;
    
    /** 请求URL */
    private String requestUrl;
    
    /** 请求方法（GET/POST等） */
    private String requestMethod;
    
    /** 请求头 */
    private Map<String, String> requestHeaders;
    
    /** 请求体 */
    private String requestBody;
    
    /** 响应时间（毫秒） */
    private Long responseTime;
    
    /** 请求时间戳 */
    private Long requestTime;

    public ApiResult() {
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public Long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Long requestTime) {
        this.requestTime = requestTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiResult apiResult = (ApiResult) o;
        return Objects.equals(statusCode, apiResult.statusCode) &&
                Objects.equals(requestUrl, apiResult.requestUrl) &&
                Objects.equals(requestMethod, apiResult.requestMethod) &&
                Objects.equals(responseTime, apiResult.responseTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, requestUrl, requestMethod, responseTime);
    }

    @Override
    public String toString() {
        return "ApiResult{" +
                "statusCode=" + statusCode +
                ", contentType='" + contentType + '\'' +
                ", requestUrl='" + requestUrl + '\'' +
                ", requestMethod='" + requestMethod + '\'' +
                ", responseTime=" + responseTime +
                '}';
    }
}
