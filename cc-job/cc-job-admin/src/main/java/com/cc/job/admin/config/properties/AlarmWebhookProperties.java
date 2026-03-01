package com.cc.job.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 告警 Webhook 配置属性
 * <p>用于任务失败时向配置的 URL 发送 HTTP 请求，支持自定义 Header 和 Body 模板占位符。
 *
 * @author cc-job-team
 */
@ConfigurationProperties(prefix = "cc-job.alarm.webhook")
public class AlarmWebhookProperties {

    /**
     * 是否启用 Webhook 告警
     */
    private boolean enabled = false;

    /**
     * Webhook URL，多个用逗号分隔（将依次发送）
     */
    private String url;

    /**
     * HTTP 方法：POST, PUT
     */
    private String method = "POST";

    /**
     * 连接超时（秒）
     */
    private int connectTimeoutSeconds = 10;

    /**
     * 读取超时（秒）
     */
    private int readTimeoutSeconds = 15;

    /**
     * 自定义请求头，key 为 Header 名，value 为值（支持占位符：{jobId}, {jobDesc}, {logId} 等）
     */
    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * 请求体模板。为空时使用默认 JSON。
     * 占位符：{jobId}, {jobDesc}, {logId}, {groupId}, {groupTitle}, {triggerMsg}, {handleMsg}, {triggerTime}, {handleTime}
     */
    private String bodyTemplate;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }
}
