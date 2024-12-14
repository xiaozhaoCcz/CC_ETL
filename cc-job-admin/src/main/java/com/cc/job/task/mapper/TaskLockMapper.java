package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskLock;
import org.apache.ibatis.annotations.Mapper;

/**
 * task_lockMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper
public interface TaskLockMapper extends BaseMapper<TaskLock> {

}
