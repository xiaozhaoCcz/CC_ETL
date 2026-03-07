package com.cc.job.admin.task.lifecycle;

import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.config.properties.LifecycleWebhookProperties;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务生命周期 Webhook 发送：start / success / fail 时 POST JSON 到配置的 URL
 */
@Component
public class LifecycleWebhookSender {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleWebhookSender.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final LifecycleWebhookProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public LifecycleWebhookSender(LifecycleWebhookProperties properties) {
        this.properties = properties;
    }

    public void send(JobLog log, String event) {
        if (!properties.isEnabled() || properties.getUrl() == null || properties.getUrl().isEmpty()) return;
        List<String> events = properties.getEvents();
        if (events != null && !events.isEmpty() && !events.stream().anyMatch(e -> e.equalsIgnoreCase(event))) return;

        JobInfo info = log != null && log.getJobId() != null
                ? XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().selectById(log.getJobId()) : null;
        JobGroup group = null;
        if (info != null && info.getJobGroup() != null) {
            group = XxlJobAdminConfig.getAdminConfig().getJobGroupMapper().selectById(info.getJobGroup());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", event);
        body.put("jobId", info != null ? info.getId() : (log != null ? log.getJobId() : null));
        body.put("jobDesc", info != null ? info.getJobDesc() : "");
        body.put("logId", log != null ? log.getId() : null);
        body.put("groupTitle", group != null ? group.getTitle() : "");
        body.put("triggerTime", log != null && log.getTriggerTime() != null ? log.getTriggerTime().format(FMT) : "");
        body.put("handleTime", log != null && log.getHandleTime() != null ? log.getHandleTime().format(FMT) : "");
        body.put("handleCode", log != null ? log.getHandleCode() : null);
        body.put("handleMsg", log != null ? log.getHandleMsg() : "");
        body.put("triggerMsg", log != null ? log.getTriggerMsg() : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        for (String u : properties.getUrl()) {
            try {
                restTemplate.postForEntity(u, entity, String.class);
            } catch (Exception e) {
                logger.warn("[LifecycleWebhook] send event={} failed, url={}, logId={}", event, u, log != null ? log.getId() : null, e);
            }
        }
    }
}
