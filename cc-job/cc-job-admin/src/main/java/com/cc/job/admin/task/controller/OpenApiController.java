package com.cc.job.admin.task.controller;

import com.cc.job.xo.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放 API 列表与文档入口
 */
@Tag(name = "开放API")
@RestController
@RequestMapping("/api/v1/open")
public class OpenApiController {

    @Value("${server.port:8989}")
    private String serverPort;

    @Operation(summary = "获取开放 API 列表与示例")
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("docUrl", "http://localhost:" + serverPort + "/xxl-job-admin/doc.html");
        root.put("openApiDoc", "项目 doc/开放API.md");
        root.put("endpoints", List.of(
                map("POST", "/api/v1/jobInfos/trigger", "触发任务", "Body: {\"id\": jobId, \"executorParam\": \"\"}"),
                map("GET", "/api/v1/dashboard/stats", "任务报表统计", "Query: filterTimeStart, filterTimeEnd, jobId"),
                map("GET", "/api/v1/dashboard/trend", "按日趋势", "Query: filterTimeStart, filterTimeEnd, jobId"),
                map("GET", "/api/v1/dashboard/health", "健康度与失败TopN", "Query: filterTimeStart, filterTimeEnd, jobId, topN, recentLogLimit"),
                map("GET", "/api/v1/dashboard/execution-stats", "执行时长与SLA", "Query: filterTimeStart, filterTimeEnd, jobId, timeoutThresholdSeconds, slowLogLimit"),
                map("GET", "/api/v1/dashboard/export", "导出报表Excel", "Query: filterTimeStart, filterTimeEnd, jobId"),
                map("GET", "/api/v1/jobInfos/getJobStatus/{id}", "任务运行状态", "Path: id"),
                map("GET", "/api/v1/jobLogs/page", "日志分页", "Query: pageNum, pageSize, jobId")
        ));
        return Result.success(root);
    }

    private static Map<String, String> map(String method, String path, String summary, String example) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("method", method);
        m.put("path", path);
        m.put("summary", summary);
        m.put("example", example);
        return m;
    }
}
