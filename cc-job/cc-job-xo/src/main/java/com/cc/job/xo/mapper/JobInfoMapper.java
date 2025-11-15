package com.cc.job.xo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.xo.model.entity.JobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    int stopJobCompose(@Param("id") Long id);

    int pauseJob(@Param("id")Long id, @Param("isPause")Integer isPause);

    /**
     * 使用行锁查询任务信息（防止并发执行）
     * 
     * @param id 任务ID
     * @return 任务信息
     */
    @Select("SELECT * FROM job_info WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    JobInfo selectByIdForUpdate(@Param("id") Long id);
}
