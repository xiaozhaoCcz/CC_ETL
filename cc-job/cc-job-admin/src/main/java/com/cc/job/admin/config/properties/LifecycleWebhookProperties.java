package com.cc.job.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 生命周期 Webhook：任务开始/成功/失败时向配置的 URL POST JSON
 */
@ConfigurationProperties(prefix = "cc-job.lifecycle.webhook")
public class LifecycleWebhookProperties {

    private boolean enabled = false;
    /** 可配置多个 URL，每个事件会依次 POST */
    private List<String> url = new ArrayList<>();
    /** 要发送的事件：start, success, fail。空表示全部 */
    private List<String> events = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<String> getUrl() { return url; }
    public void setUrl(List<String> url) { this.url = url; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
}
