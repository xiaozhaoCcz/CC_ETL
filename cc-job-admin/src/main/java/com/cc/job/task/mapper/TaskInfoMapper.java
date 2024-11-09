package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;
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
public interface TaskInfoMapper extends BaseMapper<TaskInfo> {

    List<TaskInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize );

}
