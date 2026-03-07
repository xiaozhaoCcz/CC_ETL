package com.cc.job.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 企业微信机器人告警配置
 */
@ConfigurationProperties(prefix = "cc-job.alarm.wecom")
public class AlarmWeComProperties {

    private boolean enabled = false;
    /** 企业微信群机器人 Webhook 地址 */
    private String webhookUrl;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}
