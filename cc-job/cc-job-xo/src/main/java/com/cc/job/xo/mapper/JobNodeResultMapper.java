package com.cc.job.xo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.xo.model.entity.JobNodeResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 节点执行结果Mapper接口
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
@Mapper
public interface JobNodeResultMapper extends BaseMapper<JobNodeResult> {

    /**
     * 查询最近一次全量跑的批次ID（该批次下去重 job_id 数等于 nodeCount）
     */
    @Select("SELECT execution_batch_id FROM job_node_result WHERE task_group_id = #{taskGroupId} " +
            "GROUP BY execution_batch_id HAVING COUNT(DISTINCT job_id) = #{nodeCount} " +
            "ORDER BY MAX(create_time) DESC LIMIT 1")
    String selectLatestFullRunBatchId(@Param("taskGroupId") Long taskGroupId, @Param("nodeCount") long nodeCount);
}

