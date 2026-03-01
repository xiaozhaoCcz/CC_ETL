package com.cc.job.admin.task.controller.dto;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 仪表盘导出 - 按日趋势行
 */
public class DashboardExportTrendRow {

    @ExcelProperty("日期")
    private String date;
    @ExcelProperty("成功")
    private Long successCount;
    @ExcelProperty("失败")
    private Long failCount;
    @ExcelProperty("运行中")
    private Long runningCount;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    public Long getFailCount() { return failCount; }
    public void setFailCount(Long failCount) { this.failCount = failCount; }
    public Long getRunningCount() { return runningCount; }
    public void setRunningCount(Long runningCount) { this.runningCount = runningCount; }
}
