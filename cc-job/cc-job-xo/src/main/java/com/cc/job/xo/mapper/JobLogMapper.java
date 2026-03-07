package com.cc.job.xo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.xo.model.entity.JobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * task_logMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Mapper
public interface JobLogMapper extends BaseMapper<JobLog> {


    List<Long> findLostJobIds(@Param("losedTime") Date losedTime);

    List<Long> findFailJobLogIds(@Param("pagesize") int pagesize);

    int updateAlarmStatus(@Param("logId") long logId,
                          @Param("oldAlarmStatus") int oldAlarmStatus,
                          @Param("newAlarmStatus") int newAlarmStatus);

    Map<String, Object> findLogReport(@Param("from") Date from,
                                      @Param("to") Date to);

    List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
                               @Param("jobId") int jobId,
                               @Param("clearBeforeTime") Date clearBeforeTime,
                               @Param("clearBeforeNum") int clearBeforeNum,
                               @Param("pagesize") int pagesize);

    /**
     * 按任务ID查询最近若干条日志（含当前 logId），按 id 降序，用于连续失败次数统计
     */
    List<JobLog> listRecentByJobIdFromLogId(@Param("jobId") Long jobId, @Param("fromLogId") long fromLogId, @Param("limit") int limit);

    /** 失败任务统计：job_id, fail_count，按失败次数降序 */
    List<Map<String, Object>> listFailedJobCounts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("jobId") Long jobId, @Param("limit") int limit);

    /** 最近失败日志 ID 列表，用于快捷跳转 */
    List<Long> listRecentFailLogIds(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("jobId") Long jobId, @Param("limit") int limit);
}
