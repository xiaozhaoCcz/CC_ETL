package com.cc.job.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.system.model.entity.Log;
import com.cc.job.system.model.query.LogPageQuery;
import com.cc.job.system.model.vo.LogPageVO;
import com.cc.job.system.model.vo.VisitStatsVO;
import com.cc.job.system.model.vo.VisitTrendVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * 系统日志 服务接口
 *
 * @author Ray
 * @since 2.10.0
 */
public interface LogService extends IService<Log> {

    /**
     * 获取日志分页列表
     *
     * @param queryParams 查询参数
     * @return
     */
    Page<LogPageVO> getLogPage(LogPageQuery queryParams);


    /**
     * 获取访问趋势
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return
     */
    VisitTrendVO getVisitTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 获取访问统计
     *
     * @return
     */
    List<VisitStatsVO> getVisitStats();

}
