package com.cc.job.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 钉钉机器人告警配置
 *
 * @author cc-job-team
 */
@ConfigurationProperties(prefix = "cc-job.alarm.dingtalk")
public class AlarmDingTalkProperties {

    private boolean enabled = false;
    /**
     * 钉钉群机器人 Webhook 地址
     */
    private String webhookUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
