package com.cc.job.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 告警规则配置：连续失败次数、仅工作时间等
 */
@ConfigurationProperties(prefix = "cc-job.alarm.rules")
public class AlarmRuleProperties {

    /** 连续失败达到此次数才告警，0 表示不启用此规则 */
    private int consecutiveFailures = 0;
    /** 仅工作时间告警，格式 HH:mm 如 09:00，空表示不限制 */
    private String workHoursStart = "";
    /** 仅工作时间告警结束，格式 HH:mm 如 18:00 */
    private String workHoursEnd = "";
    /** 工作日在周几，1-7 表示周一到周日，如 1-5 表示工作日，空表示不限制 */
    private String workDays = "";

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    public String getWorkHoursStart() { return workHoursStart; }
    public void setWorkHoursStart(String workHoursStart) { this.workHoursStart = workHoursStart; }
    public String getWorkHoursEnd() { return workHoursEnd; }
    public void setWorkHoursEnd(String workHoursEnd) { this.workHoursEnd = workHoursEnd; }
    public String getWorkDays() { return workDays; }
    public void setWorkDays(String workDays) { this.workDays = workDays; }
}
