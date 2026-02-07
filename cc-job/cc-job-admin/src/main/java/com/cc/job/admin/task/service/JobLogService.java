package com.cc.job.admin.task.service;

import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;

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
}
