package com.cc.job.admin.task.alarm;

import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.config.properties.AlarmRuleProperties;
import com.cc.job.xo.model.entity.JobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 告警规则校验：连续失败次数、仅工作时间等，不满足则跳过告警
 */
@Component
public class AlarmRuleChecker {

    private static final Logger logger = LoggerFactory.getLogger(AlarmRuleChecker.class);
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AlarmRuleProperties properties;

    public AlarmRuleChecker(AlarmRuleProperties properties) {
        this.properties = properties;
    }

    /**
     * 是否应因规则跳过告警（true = 跳过，不告警）
     */
    public boolean shouldSkipByRules(Long jobId, JobLog currentLog) {
        if (properties.getConsecutiveFailures() > 0) {
            int consecutive = countConsecutiveFailures(jobId, currentLog.getId());
            if (consecutive < properties.getConsecutiveFailures()) {
                logger.debug("[AlarmRule] jobId={} consecutive failures={} < threshold {}, skip alarm", jobId, consecutive, properties.getConsecutiveFailures());
                return true;
            }
        }
        if (isWorkHoursOnlyEnabled() && !isWithinWorkHours()) {
            logger.debug("[AlarmRule] outside work hours, skip alarm");
            return true;
        }
        return false;
    }

    private boolean isWorkHoursOnlyEnabled() {
        return properties.getWorkHoursStart() != null && !properties.getWorkHoursStart().trim().isEmpty()
                && properties.getWorkHoursEnd() != null && !properties.getWorkHoursEnd().trim().isEmpty();
    }

    private boolean isWithinWorkHours() {
        try {
            LocalTime start = LocalTime.parse(properties.getWorkHoursStart().trim(), HHMM);
            LocalTime end = LocalTime.parse(properties.getWorkHoursEnd().trim(), HHMM);
            LocalTime now = LocalTime.now();
            boolean inRange = !start.isAfter(now) && !end.isBefore(now);
            if (!inRange) return false;
        } catch (Exception e) {
            return false;
        }
        if (properties.getWorkDays() == null || properties.getWorkDays().trim().isEmpty()) return true;
        String workDays = properties.getWorkDays().trim();
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        if (workDays.contains("-")) {
            String[] parts = workDays.split("-");
            if (parts.length == 2) {
                int from = Integer.parseInt(parts[0].trim());
                int to = Integer.parseInt(parts[1].trim());
                return dayOfWeek >= from && dayOfWeek <= to;
            }
        }
        return workDays.contains(String.valueOf(dayOfWeek));
    }

    private int countConsecutiveFailures(Long jobId, long fromLogId) {
        List<JobLog> recent = XxlJobAdminConfig.getAdminConfig().getJobLogMapper().listRecentByJobIdFromLogId(jobId, fromLogId, 50);
        int count = 0;
        for (JobLog log : recent) {
            if (isFail(log)) count++;
            else break;
        }
        return count;
    }

    private static boolean isFail(JobLog log) {
        if (log.getHandleCode() != null && log.getHandleCode() == 200) return false;
        if (log.getTriggerCode() != null && log.getTriggerCode() != 200 && log.getTriggerCode() != 0) return true;
        return log.getHandleCode() != null && log.getHandleCode() != 0 && log.getHandleCode() != 200;
    }
}
