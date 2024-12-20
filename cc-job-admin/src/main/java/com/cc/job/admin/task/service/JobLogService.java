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
    IPage<JobLogVO> getTaskLogPage(JobLogQuery queryParams);


    /**
     * 删除task_log
     *
     * @param ids task_logID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskLogs(JobLogQuery queryParams);

    ReturnT<LogResult> getLogDetailCat(Long logId, int fromLineNum);
}
