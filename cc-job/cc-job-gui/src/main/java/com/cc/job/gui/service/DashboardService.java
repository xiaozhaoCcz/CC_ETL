package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.DashboardExecutionStatsVO;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import com.cc.job.xo.model.vo.DashboardTrendItemVO;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘/任务报表服务，用于获取统计接口数据
 */
public class DashboardService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    /**
     * 获取任务报表统计（无筛选）
     */
    public DashboardStatsVO getStats() throws IOException {
        return getStats(null, null, null);
    }

    /**
     * 获取任务报表统计（支持时间范围与任务组筛选）
     *
     * @param filterTimeStart 开始时间 ISO_LOCAL_DATE_TIME，null 表示不限制
     * @param filterTimeEnd   结束时间 ISO_LOCAL_DATE_TIME，null 表示不限制
     * @param jobId           任务组ID，null 表示不限制
     * @return 统计 VO，失败时抛出 IOException
     */
    public DashboardStatsVO getStats(String filterTimeStart, String filterTimeEnd, Long jobId) throws IOException {
        Map<String, String> params = new HashMap<>();
        if (filterTimeStart != null && !filterTimeStart.isEmpty()) {
            params.put("filterTimeStart", filterTimeStart);
        }
        if (filterTimeEnd != null && !filterTimeEnd.isEmpty()) {
            params.put("filterTimeEnd", filterTimeEnd);
        }
        if (jobId != null) {
            params.put("jobId", String.valueOf(jobId));
        }
        TypeToken<DashboardStatsVO> typeToken = new TypeToken<DashboardStatsVO>() {};
        Result<DashboardStatsVO> result = httpClient.get("/api/v1/dashboard/stats", typeToken, params);
        return httpClient.extractData(result, "获取任务报表统计失败");
    }

    /**
     * 获取按日趋势数据
     */
    public List<DashboardTrendItemVO> getTrend(String filterTimeStart, String filterTimeEnd, Long jobId) throws IOException {
        Map<String, String> params = new HashMap<>();
        if (filterTimeStart != null && !filterTimeStart.isEmpty()) params.put("filterTimeStart", filterTimeStart);
        if (filterTimeEnd != null && !filterTimeEnd.isEmpty()) params.put("filterTimeEnd", filterTimeEnd);
        if (jobId != null) params.put("jobId", String.valueOf(jobId));
        TypeToken<List<DashboardTrendItemVO>> typeToken = new TypeToken<List<DashboardTrendItemVO>>() {};
        Result<List<DashboardTrendItemVO>> result = httpClient.get("/api/v1/dashboard/trend", typeToken, params);
        return httpClient.extractData(result, "获取趋势数据失败");
    }

    /**
     * 获取执行时长与 SLA 统计
     */
    public DashboardExecutionStatsVO getExecutionStats(String filterTimeStart, String filterTimeEnd, Long jobId,
                                                       int timeoutThresholdSeconds, int slowLogLimit) throws IOException {
        Map<String, String> params = new HashMap<>();
        if (filterTimeStart != null && !filterTimeStart.isEmpty()) params.put("filterTimeStart", filterTimeStart);
        if (filterTimeEnd != null && !filterTimeEnd.isEmpty()) params.put("filterTimeEnd", filterTimeEnd);
        if (jobId != null) params.put("jobId", String.valueOf(jobId));
        params.put("timeoutThresholdSeconds", String.valueOf(timeoutThresholdSeconds));
        params.put("slowLogLimit", String.valueOf(slowLogLimit));
        TypeToken<DashboardExecutionStatsVO> typeToken = new TypeToken<DashboardExecutionStatsVO>() {};
        Result<DashboardExecutionStatsVO> result = httpClient.get("/api/v1/dashboard/execution-stats", typeToken, params);
        return httpClient.extractData(result, "获取执行统计失败");
    }

    /**
     * 下载报表 Excel 到指定文件
     */
    public void downloadExport(Path targetFile, String filterTimeStart, String filterTimeEnd, Long jobId) throws IOException {
        Map<String, String> params = new HashMap<>();
        if (filterTimeStart != null && !filterTimeStart.isEmpty()) params.put("filterTimeStart", filterTimeStart);
        if (filterTimeEnd != null && !filterTimeEnd.isEmpty()) params.put("filterTimeEnd", filterTimeEnd);
        if (jobId != null) params.put("jobId", String.valueOf(jobId));
        byte[] bytes = httpClient.getRaw("/api/v1/dashboard/export", params);
        if (bytes != null && bytes.length > 0) {
            try (OutputStream os = Files.newOutputStream(targetFile)) {
                os.write(bytes);
            }
        }
    }
}
