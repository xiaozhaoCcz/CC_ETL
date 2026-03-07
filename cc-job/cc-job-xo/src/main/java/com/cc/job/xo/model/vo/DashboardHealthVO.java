package com.cc.job.xo.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 仪表盘健康度：成功率/失败率、失败任务 Top N、最近失败日志 ID 列表（便于快捷跳转）
 */
@Schema(description = "健康度与失败统计")
public class DashboardHealthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "成功率 0-100")
    private Double successRate;
    @Schema(description = "失败率 0-100")
    private Double failRate;
    @Schema(description = "总执行次数（成功+失败，不含运行中）")
    private Long totalFinished;
    @Schema(description = "失败任务 Top N")
    private List<FailedJobItem> failedJobTopN;
    @Schema(description = "最近失败日志 ID 列表，用于快捷跳转")
    private List<Long> recentFailLogIds;

    @Schema(description = "失败任务项")
    public static class FailedJobItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long jobId;
        private String jobDesc;
        private Long failCount;

        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getJobDesc() { return jobDesc; }
        public void setJobDesc(String jobDesc) { this.jobDesc = jobDesc; }
        public Long getFailCount() { return failCount; }
        public void setFailCount(Long failCount) { this.failCount = failCount; }
    }

    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }
    public Double getFailRate() { return failRate; }
    public void setFailRate(Double failRate) { this.failRate = failRate; }
    public Long getTotalFinished() { return totalFinished; }
    public void setTotalFinished(Long totalFinished) { this.totalFinished = totalFinished; }
    public List<FailedJobItem> getFailedJobTopN() { return failedJobTopN; }
    public void setFailedJobTopN(List<FailedJobItem> failedJobTopN) { this.failedJobTopN = failedJobTopN; }
    public List<Long> getRecentFailLogIds() { return recentFailLogIds; }
    public void setRecentFailLogIds(List<Long> recentFailLogIds) { this.recentFailLogIds = recentFailLogIds; }
}
