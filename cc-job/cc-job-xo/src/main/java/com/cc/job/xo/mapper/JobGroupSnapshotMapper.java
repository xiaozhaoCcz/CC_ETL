package com.cc.job.xo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.xo.model.entity.JobGroupSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务组快照Mapper接口
 *
 * @author ccjob
 * @since 2025-01-XX
 */
@Mapper
public interface JobGroupSnapshotMapper extends BaseMapper<JobGroupSnapshot> {

    /**
     * 根据任务组ID和批次ID查询快照
     *
     * @param jobId   任务组ID
     * @param randomId 批次ID
     * @return 快照对象
     */
    JobGroupSnapshot selectByJobIdAndRandomId(@Param("jobId") Long jobId, @Param("randomId") String randomId);

    /**
     * 删除指定任务组的所有快照（软删除）
     *
     * @param jobId 任务组ID
     * @return 删除数量
     */
    int deleteByJobId(@Param("jobId") Long jobId);
}

