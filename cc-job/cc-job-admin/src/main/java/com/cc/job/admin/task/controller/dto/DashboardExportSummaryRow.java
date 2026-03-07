package com.cc.job.admin.task.controller.dto;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 仪表盘导出 - 汇总行
 */
public class DashboardExportSummaryRow {

    @ExcelProperty("指标")
    private String name;
    @ExcelProperty("数值")
    private Long value;

    public DashboardExportSummaryRow() {}

    public DashboardExportSummaryRow(String name, Long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
}
