package com.cc.job.admin.task.service;

import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;

import com.cc.job.xo.model.vo.DashboardExecutionStatsVO;
import com.cc.job.xo.model.vo.DashboardHealthVO;

import java.time.LocalDateTime;

/**
 * task_log服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
public interface JobLogService extends IService<JobLog> {

    /**
     *task_log分页列表
     *
     * @return
     */
    IPage<JobLogVO> getJobLogPage(JobLogQuery queryParams);


    /**
     * 删除task_log
     *
     * @param ids task_logID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteJobLogs(JobLogQuery queryParams);

    /**
     * 归档：删除早于指定天数的日志（物理删除或按库策略）
     * @param olderThanDays 保留最近 N 天，早于的删除
     * @return 删除条数
     */
    int archiveOlderThanDays(int olderThanDays);

    ReturnT<LogResult> getLogDetailCat(Long logId, int fromLineNum);
    
    /**
     * 保存节点执行状态到 job_log 表
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID（即 executorParam）
     * @param nodeStatusJson 节点状态JSON字符串
     * @return 是否保存成功
     */
    boolean saveNodeStatus(Long taskGroupId, String executionBatchId, String nodeStatusJson);

    /**
     * 统计执行成功的日志数量（handle_code = 200）
     */
    long countLogSuccess();

    /**
     * 统计执行失败的日志数量（handle_code 不为 0 且不为 200）
     */
    long countLogFail();

    /**
     * 统计运行中的日志数量（handle_code = 0）
     */
    long countLogRunning();

    /**
     * 统计执行成功的日志数量（支持时间范围与任务组筛选）
     * @param start 开始时间，null 表示不限制
     * @param end 结束时间，null 表示不限制
     * @param jobId 任务组ID，null 表示不限制
     */
    long countLogSuccess(LocalDateTime start, LocalDateTime end, Long jobId);

    /**
     * 统计执行失败的日志数量（支持时间范围与任务组筛选）
     */
    long countLogFail(LocalDateTime start, LocalDateTime end, Long jobId);

    /**
     * 统计运行中的日志数量（支持时间范围与任务组筛选）
     */
    long countLogRunning(LocalDateTime start, LocalDateTime end, Long jobId);

    /**
     * 执行时长与 SLA 统计：平均耗时、P99、超时次数及慢日志列表
     * @param start 开始时间
     * @param end 结束时间
     * @param jobId 任务ID，可选
     * @param timeoutThresholdSeconds 超时阈值（秒），超过视为超时，0 表示不计算超时
     * @param slowLogLimit 返回的慢日志条数上限
     */
    DashboardExecutionStatsVO getExecutionStats(LocalDateTime start, LocalDateTime end, Long jobId,
                                                int timeoutThresholdSeconds, int slowLogLimit);

    /**
     * 健康度统计：成功率/失败率、失败任务 Top N、最近失败日志 ID 列表
     */
    DashboardHealthVO getHealthStats(LocalDateTime start, LocalDateTime end, Long jobId, int topN, int recentLogLimit);
}
