package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
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
}
