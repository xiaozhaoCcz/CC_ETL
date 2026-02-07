package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 仪表盘/任务报表服务，用于获取统计接口数据
 */
public class DashboardService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    /**
     * 获取任务报表统计
     *
     * @return 统计 VO，失败时抛出 IOException
     */
    public DashboardStatsVO getStats() throws IOException {
        TypeToken<DashboardStatsVO> typeToken = new TypeToken<DashboardStatsVO>() {};
        Result<DashboardStatsVO> result = httpClient.get("/api/v1/dashboard/stats", typeToken);
        return httpClient.extractData(result, "获取任务报表统计失败");
    }
}
