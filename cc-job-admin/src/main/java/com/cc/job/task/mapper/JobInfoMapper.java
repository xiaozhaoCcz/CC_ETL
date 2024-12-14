package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.JobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * task_infoMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper
public interface JobInfoMapper extends BaseMapper<JobInfo> {

    List<JobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize );

    int stopTaskSet(@Param("id") Long id);
}
