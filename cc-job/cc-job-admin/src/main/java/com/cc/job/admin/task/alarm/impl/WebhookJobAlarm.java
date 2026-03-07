package com.cc.job.admin.task.alarm.impl;

import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.config.properties.AlarmWebhookProperties;
import com.cc.job.admin.task.alarm.JobAlarm;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务失败 Webhook 告警
 * <p>当配置了 cc-job.alarm.webhook.url 且 enabled=true 时，向该 URL 发送 POST 请求，Body 为 JSON。
 *
 * @author cc-job-team
 */
@Component
public class WebhookJobAlarm implements JobAlarm {

    private static final Logger logger = LoggerFactory.getLogger(WebhookJobAlarm.class);
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AlarmWebhookProperties webhookProperties;
    private final RestTemplate restTemplate;

    public WebhookJobAlarm(AlarmWebhookProperties webhookProperties) {
        this.webhookProperties = webhookProperties;
        this.restTemplate = createRestTemplate(webhookProperties);
    }

    private static RestTemplate createRestTemplate(AlarmWebhookProperties p) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        int connectMs = (p.getConnectTimeoutSeconds() > 0 ? p.getConnectTimeoutSeconds() : 10) * 1000;
        int readMs = (p.getReadTimeoutSeconds() > 0 ? p.getReadTimeoutSeconds() : 15) * 1000;
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(readMs);
        return new RestTemplate(factory);
    }

    @Override
    public boolean doAlarm(JobInfo info, JobLog jobLog) {
        if (!webhookProperties.isEnabled() || webhookProperties.getUrl() == null || webhookProperties.getUrl().trim().isEmpty()) {
            return true;
        }

        String[] urls = Arrays.stream(webhookProperties.getUrl().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (urls.length == 0) {
            return true;
        }

        JobGroup group = null;
        if (info != null && info.getJobGroup() != null) {
            group = XxlJobAdminConfig.getAdminConfig().getJobGroupMapper().selectById(info.getJobGroup());
        }
        String groupTitle = group != null ? group.getTitle() : "";

        String triggerTimeStr = jobLog.getTriggerTime() != null ? jobLog.getTriggerTime().format(ISO_FORMAT) : "";
        String handleTimeStr = jobLog.getHandleTime() != null ? jobLog.getHandleTime().format(ISO_FORMAT) : "";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", info != null ? info.getId() : null);
        body.put("jobDesc", info != null ? info.getJobDesc() : "");
        body.put("logId", jobLog.getId());
        body.put("groupId", info != null ? info.getJobGroup() : null);
        body.put("groupTitle", groupTitle);
        body.put("triggerMsg", jobLog.getTriggerMsg() != null ? jobLog.getTriggerMsg() : "");
        body.put("handleMsg", jobLog.getHandleMsg() != null ? jobLog.getHandleMsg() : "");
        body.put("triggerCode", jobLog.getTriggerCode());
        body.put("handleCode", jobLog.getHandleCode());
        body.put("triggerTime", triggerTimeStr);
        body.put("handleTime", handleTimeStr);
        body.put("alarmType", "job_fail");

        String bodyJson = buildBody(body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (webhookProperties.getHeaders() != null) {
            for (Map.Entry<String, String> e : webhookProperties.getHeaders().entrySet()) {
                String value = e.getValue()
                        .replace("{jobId}", String.valueOf(info != null ? info.getId() : ""))
                        .replace("{jobDesc}", info != null ? info.getJobDesc() : "")
                        .replace("{logId}", String.valueOf(jobLog.getId()));
                headers.set(e.getKey(), value);
            }
        }

        HttpMethod method = "PUT".equalsIgnoreCase(webhookProperties.getMethod()) ? HttpMethod.PUT : HttpMethod.POST;
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

        boolean allSuccess = true;
        for (String url : urls) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    logger.warn("[WebhookAlarm] url={} returned {}", url, response.getStatusCode());
                    allSuccess = false;
                }
            } catch (Exception e) {
                logger.error("[WebhookAlarm] send failed, url={}, jobLogId={}", url, jobLog.getId(), e);
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    private String buildBody(Map<String, Object> body) {
        if (webhookProperties.getBodyTemplate() != null && !webhookProperties.getBodyTemplate().trim().isEmpty()) {
            String tpl = webhookProperties.getBodyTemplate();
            for (Map.Entry<String, Object> e : body.entrySet()) {
                tpl = tpl.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue().toString() : "");
            }
            return tpl;
        }
        return body.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":\"" + (e.getValue() != null ? escapeJson(String.valueOf(e.getValue())) : "") + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
