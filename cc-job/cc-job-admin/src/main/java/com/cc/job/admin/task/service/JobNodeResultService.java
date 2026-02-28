package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobNodeResult;

import java.util.List;
import java.util.Map;

/**
 * 节点执行结果服务接口
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
public interface JobNodeResultService extends IService<JobNodeResult> {

    /**
     * 保存节点执行结果
     *
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @param jobId 节点任务ID
     * @param jobName 节点任务名称
     * @param resultData 执行结果数据（JSON格式）
     * @return 是否保存成功
     */
    boolean saveNodeResult(Long taskGroupId, String executionBatchId, Long jobId, String jobName, String resultData);

    /**
     * 批量保存节点执行结果
     *
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @param results 节点结果列表，每个元素包含 jobId, jobName, resultData
     * @return 保存成功的数量
     */
    int batchSaveNodeResults(Long taskGroupId, String executionBatchId, List<Map<String, Object>> results);

    /**
     * 根据任务组ID和批次ID获取节点执行结果
     *
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @return 节点执行结果列表
     */
    List<JobNodeResult> getNodeResultsByBatch(Long taskGroupId, String executionBatchId);

    /**
     * 根据任务组ID获取最近一次执行的批次ID
     *
     * @param taskGroupId 任务组ID
     * @return 最近一次执行的批次ID，如果不存在则返回null
     */
    String getLatestBatchId(Long taskGroupId);

    /**
     * 根据任务组ID和jobId获取最近一次执行的节点结果
     *
     * @param taskGroupId 任务组ID
     * @param jobId 节点任务ID
     * @return 节点执行结果，如果不存在则返回null
     */
    JobNodeResult getLatestNodeResult(Long taskGroupId, Long jobId);

    /**
     * 获取最近一次全量跑的批次ID（该批次下节点结果数等于任务组节点总数）
     *
     * @param taskGroupId 任务组ID
     * @return 批次ID，若无全量跑批次则返回null
     */
    String getLatestFullRunBatchId(Long taskGroupId);
}

