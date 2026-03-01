package com.cc.job.admin.task.alarm.impl;

import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.config.properties.AlarmWeComProperties;
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
import java.util.Map;

/**
 * 企业微信机器人告警：任务失败时向企微群发送文本消息
 */
@Component
public class WeComJobAlarm implements JobAlarm {

    private static final Logger logger = LoggerFactory.getLogger(WeComJobAlarm.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmWeComProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public WeComJobAlarm(AlarmWeComProperties properties) {
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
                groupTitle, info != null ? info.getId() : "", info != null ? info.getJobDesc() : "",
                jobLog.getId(), triggerTime,
                jobLog.getTriggerMsg() != null ? jobLog.getTriggerMsg().replace("\n", " ") : "",
                jobLog.getHandleMsg() != null ? jobLog.getHandleMsg().replace("\n", " ") : ""
        );
        Map<String, Object> body = Map.of(
                "msgtype", "text",
                "text", Map.of("content", content)
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getWebhookUrl(), new HttpEntity<>(body, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("[WeComAlarm] webhook returned {}", response.getStatusCode());
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("[WeComAlarm] send failed, jobLogId={}", jobLog.getId(), e);
            return false;
        }
    }
}
