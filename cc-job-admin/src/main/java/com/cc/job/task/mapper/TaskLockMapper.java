package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskLock;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskLockQuery;
import com.cc.job.task.model.vo.TaskLockVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * task_lockMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper
public interface TaskLockMapper extends BaseMapper<TaskLock> {

    /**
     * 获取task_lock分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return
     */
    Page<TaskLockVO> getTaskLockPage(Page<TaskLockVO> page, TaskLockQuery queryParams);

}
