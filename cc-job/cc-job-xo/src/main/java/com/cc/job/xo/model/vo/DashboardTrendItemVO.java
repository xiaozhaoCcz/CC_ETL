package com.cc.job.xo.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仪表盘趋势单日/单周统计项
 *
 * @author cc-job-team
 */
@Schema(description = "仪表盘趋势单日统计项")
public class DashboardTrendItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日期，格式 yyyy-MM-dd")
    private String date;
    @Schema(description = "执行成功次数")
    private Long successCount;
    @Schema(description = "执行失败次数")
    private Long failCount;
    @Schema(description = "运行中次数")
    private Long runningCount;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getFailCount() {
        return failCount;
    }

    public void setFailCount(Long failCount) {
        this.failCount = failCount;
    }

    public Long getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(Long runningCount) {
        this.runningCount = runningCount;
    }
}
