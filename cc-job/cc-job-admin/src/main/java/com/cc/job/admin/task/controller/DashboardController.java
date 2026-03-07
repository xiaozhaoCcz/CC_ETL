package com.cc.job.admin.task.controller;

import com.alibaba.excel.EasyExcel;
import com.cc.job.admin.task.controller.dto.DashboardExportSummaryRow;
import com.cc.job.admin.task.controller.dto.DashboardExportTrendRow;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobLogService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.DashboardExecutionStatsVO;
import com.cc.job.xo.model.vo.DashboardHealthVO;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import com.cc.job.xo.model.vo.DashboardTrendItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘/任务报表统计接口
 */
@Tag(name = "仪表盘接口")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JobInfoService jobInfoService;
    private final JobLogService jobLogService;

    public DashboardController(JobInfoService jobInfoService, JobLogService jobLogService) {
        this.jobInfoService = jobInfoService;
        this.jobLogService = jobLogService;
    }

    @Operation(summary = "获取任务报表统计，支持时间范围与任务组筛选")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> getStats(
            @RequestParam(required = false) String filterTimeStart,
            @RequestParam(required = false) String filterTimeEnd,
            @RequestParam(required = false) Long jobId) {
        LocalDateTime start = parseDateTime(filterTimeStart);
        LocalDateTime end = parseDateTime(filterTimeEnd);

        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTaskGroupCount(jobInfoService.countTaskGroups());
        vo.setJobCount(jobInfoService.countJobs());
        vo.setLogSuccessCount(jobLogService.countLogSuccess(start, end, jobId));
        vo.setLogFailCount(jobLogService.countLogFail(start, end, jobId));
        vo.setLogRunningCount(jobLogService.countLogRunning(start, end, jobId));
        return Result.success(vo);
    }

    @Operation(summary = "获取任务报表按日趋势，支持时间范围与任务组筛选")
    @GetMapping("/trend")
    public Result<List<DashboardTrendItemVO>> getTrend(
            @RequestParam(required = false) String filterTimeStart,
            @RequestParam(required = false) String filterTimeEnd,
            @RequestParam(required = false) Long jobId) {
        LocalDate endDate = parseDate(filterTimeEnd);
        LocalDate startDate = parseDate(filterTimeStart);
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(6);
        }
        if (startDate.isAfter(endDate)) {
            LocalDate t = startDate;
            startDate = endDate;
            endDate = t;
        }
        // 最多 90 天
        if (startDate.isBefore(endDate.minusDays(90))) {
            startDate = endDate.minusDays(90);
        }

        List<DashboardTrendItemVO> list = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();
            DashboardTrendItemVO item = new DashboardTrendItemVO();
            item.setDate(d.format(DateTimeFormatter.ISO_LOCAL_DATE));
            item.setSuccessCount(jobLogService.countLogSuccess(dayStart, dayEnd, jobId));
            item.setFailCount(jobLogService.countLogFail(dayStart, dayEnd, jobId));
            item.setRunningCount(jobLogService.countLogRunning(dayStart, dayEnd, jobId));
            list.add(item);
        }
        return Result.success(list);
    }

    @Operation(summary = "获取执行时长与SLA统计：平均耗时、P99、超时次数及慢日志列表")
    @GetMapping("/execution-stats")
    public Result<DashboardExecutionStatsVO> getExecutionStats(
            @RequestParam(required = false) String filterTimeStart,
            @RequestParam(required = false) String filterTimeEnd,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false, defaultValue = "0") int timeoutThresholdSeconds,
            @RequestParam(required = false, defaultValue = "20") int slowLogLimit) {
        LocalDateTime start = parseDateTime(filterTimeStart);
        LocalDateTime end = parseDateTime(filterTimeEnd);
        if (end == null) {
            end = LocalDateTime.now();
        }
        if (start == null) {
            start = end.minusDays(7);
        }
        DashboardExecutionStatsVO vo = jobLogService.getExecutionStats(start, end, jobId, timeoutThresholdSeconds, slowLogLimit);
        return Result.success(vo);
    }

    @Operation(summary = "获取健康度：成功率/失败率、失败任务 Top N、最近失败日志 ID 列表")
    @GetMapping("/health")
    public Result<DashboardHealthVO> getHealth(
            @RequestParam(required = false) String filterTimeStart,
            @RequestParam(required = false) String filterTimeEnd,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false, defaultValue = "10") int topN,
            @RequestParam(required = false, defaultValue = "20") int recentLogLimit) {
        LocalDateTime start = parseDateTime(filterTimeStart);
        LocalDateTime end = parseDateTime(filterTimeEnd);
        if (end == null) end = LocalDateTime.now();
        if (start == null) start = end.minusDays(7);
        DashboardHealthVO vo = jobLogService.getHealthStats(start, end, jobId, topN, recentLogLimit);
        return Result.success(vo);
    }

    @Operation(summary = "导出任务报表为 Excel（汇总 + 按日趋势）")
    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String filterTimeStart,
            @RequestParam(required = false) String filterTimeEnd,
            @RequestParam(required = false) Long jobId) {
        LocalDateTime start = parseDateTime(filterTimeStart);
        LocalDateTime end = parseDateTime(filterTimeEnd);
        LocalDate endDate = end != null ? end.toLocalDate() : LocalDate.now();
        LocalDate startDate = start != null ? start.toLocalDate() : endDate.minusDays(6);
        if (startDate.isAfter(endDate)) {
            LocalDate t = startDate;
            startDate = endDate;
            endDate = t;
        }

        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setTaskGroupCount(jobInfoService.countTaskGroups());
        stats.setJobCount(jobInfoService.countJobs());
        stats.setLogSuccessCount(jobLogService.countLogSuccess(start, end, jobId));
        stats.setLogFailCount(jobLogService.countLogFail(start, end, jobId));
        stats.setLogRunningCount(jobLogService.countLogRunning(start, end, jobId));

        List<DashboardExportSummaryRow> summaryRows = new ArrayList<>();
        summaryRows.add(new DashboardExportSummaryRow("任务组数", stats.getTaskGroupCount()));
        summaryRows.add(new DashboardExportSummaryRow("任务数", stats.getJobCount()));
        summaryRows.add(new DashboardExportSummaryRow("执行成功", stats.getLogSuccessCount()));
        summaryRows.add(new DashboardExportSummaryRow("执行失败", stats.getLogFailCount()));
        summaryRows.add(new DashboardExportSummaryRow("运行中", stats.getLogRunningCount()));

        List<DashboardTrendItemVO> trendList = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();
            DashboardTrendItemVO item = new DashboardTrendItemVO();
            item.setDate(d.format(DateTimeFormatter.ISO_LOCAL_DATE));
            item.setSuccessCount(jobLogService.countLogSuccess(dayStart, dayEnd, jobId));
            item.setFailCount(jobLogService.countLogFail(dayStart, dayEnd, jobId));
            item.setRunningCount(jobLogService.countLogRunning(dayStart, dayEnd, jobId));
            trendList.add(item);
        }
        List<DashboardExportTrendRow> trendRows = trendList.stream().map(t -> {
            DashboardExportTrendRow row = new DashboardExportTrendRow();
            row.setDate(t.getDate());
            row.setSuccessCount(t.getSuccessCount());
            row.setFailCount(t.getFailCount());
            row.setRunningCount(t.getRunningCount());
            return row;
        }).collect(Collectors.toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var writer = EasyExcel.write(out, DashboardExportSummaryRow.class).build();
        try {
            writer.write(summaryRows, EasyExcel.writerSheet(0, "汇总").build());
            writer.write(trendRows, EasyExcel.writerSheet(1, "按日趋势").head(DashboardExportTrendRow.class).build());
        } finally {
            writer.finish();
        }
        String filename = "任务报表_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx";
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(out.toByteArray());
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), ISO_LOCAL);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
