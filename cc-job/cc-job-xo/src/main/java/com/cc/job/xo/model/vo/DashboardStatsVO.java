package com.cc.job.xo.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仪表盘/任务报表统计 VO
 */
@Schema(description = "仪表盘统计")
public class DashboardStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务组数量")
    private Long taskGroupCount;
    @Schema(description = "任务数量")
    private Long jobCount;
    @Schema(description = "执行成功次数")
    private Long logSuccessCount;
    @Schema(description = "执行失败次数")
    private Long logFailCount;
    @Schema(description = "运行中次数")
    private Long logRunningCount;

    public Long getTaskGroupCount() {
        return taskGroupCount;
    }

    public void setTaskGroupCount(Long taskGroupCount) {
        this.taskGroupCount = taskGroupCount;
    }

    public Long getJobCount() {
        return jobCount;
    }

    public void setJobCount(Long jobCount) {
        this.jobCount = jobCount;
    }

    public Long getLogSuccessCount() {
        return logSuccessCount;
    }

    public void setLogSuccessCount(Long logSuccessCount) {
        this.logSuccessCount = logSuccessCount;
    }

    public Long getLogFailCount() {
        return logFailCount;
    }

    public void setLogFailCount(Long logFailCount) {
        this.logFailCount = logFailCount;
    }

    public Long getLogRunningCount() {
        return logRunningCount;
    }

    public void setLogRunningCount(Long logRunningCount) {
        this.logRunningCount = logRunningCount;
    }
}
