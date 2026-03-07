package com.cc.job.admin.task.alarm.impl;

import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.config.properties.AlarmDingTalkProperties;
import com.cc.job.admin.task.alarm.JobAlarm;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 钉钉机器人告警：任务失败时向钉钉群发送文本消息。
 * <p>配置 cc-job.alarm.dingtalk.enabled=true 且 webhook-url 后生效。
 *
 * @author cc-job-team
 */
@Component
public class DingTalkJobAlarm implements JobAlarm {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkJobAlarm.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmDingTalkProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public DingTalkJobAlarm(AlarmDingTalkProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean doAlarm(JobInfo info, JobLog jobLog) {
        if (!properties.isEnabled() || properties.getWebhookUrl() == null || properties.getWebhookUrl().trim().isEmpty()) {
            return true;
        }
        JobGroup group = null;
        if (info != null && info.getJobGroup() != null) {
            group = XxlJobAdminConfig.getAdminConfig().getJobGroupMapper().selectById(info.getJobGroup());
        }
        String groupTitle = group != null ? group.getTitle() : "";
        String triggerTime = jobLog.getTriggerTime() != null ? jobLog.getTriggerTime().format(FMT) : "";
        String content = String.format(
                "【Cc-ETL 任务失败】\n任务组：%s\n任务ID：%s\n任务描述：%s\n日志ID：%s\n触发时间：%s\n调度信息：%s\n执行信息：%s",
                groupTitle,
                info != null ? info.getId() : "",
                info != null ? info.getJobDesc() : "",
                jobLog.getId(),
                triggerTime,
                jobLog.getTriggerMsg() != null ? jobLog.getTriggerMsg().replace("\n", " ") : "",
                jobLog.getHandleMsg() != null ? jobLog.getHandleMsg().replace("\n", " ") : ""
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", Map.of("content", content));
        body.put("at", Map.of("atMobiles", java.util.List.of(), "isAtAll", false));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getWebhookUrl(), entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("[DingTalkAlarm] webhook returned {}", response.getStatusCode());
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("[DingTalkAlarm] send failed, jobLogId={}", jobLog.getId(), e);
            return false;
        }
    }
}
