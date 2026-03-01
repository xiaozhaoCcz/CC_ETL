package com.cc.job.xo.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 仪表盘执行时长/SLA 统计
 *
 * @author cc-job-team
 */
@Schema(description = "执行时长与SLA统计")
public class DashboardExecutionStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "平均执行耗时（毫秒）")
    private Long avgDurationMs;
    @Schema(description = "P99 执行耗时（毫秒）")
    private Long p99DurationMs;
    @Schema(description = "超时次数（超过给定阈值的执行数）")
    private Long timeoutCount;
    @Schema(description = "参与统计的日志总数（成功且有时长）")
    private Long totalCount;
    @Schema(description = "超时或最慢的若干条日志简要信息")
    private List<SlowLogItem> slowLogs;

    @Schema(description = "单条慢/超时日志")
    public static class SlowLogItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long logId;
        private Long jobId;
        private String jobDesc;
        private Long durationMs;

        public Long getLogId() { return logId; }
        public void setLogId(Long logId) { this.logId = logId; }
        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getJobDesc() { return jobDesc; }
        public void setJobDesc(String jobDesc) { this.jobDesc = jobDesc; }
        public Long getDurationMs() { return durationMs; }
        public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    }

    public Long getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(Long avgDurationMs) { this.avgDurationMs = avgDurationMs; }
    public Long getP99DurationMs() { return p99DurationMs; }
    public void setP99DurationMs(Long p99DurationMs) { this.p99DurationMs = p99DurationMs; }
    public Long getTimeoutCount() { return timeoutCount; }
    public void setTimeoutCount(Long timeoutCount) { this.timeoutCount = timeoutCount; }
    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public List<SlowLogItem> getSlowLogs() { return slowLogs; }
    public void setSlowLogs(List<SlowLogItem> slowLogs) { this.slowLogs = slowLogs; }
}
