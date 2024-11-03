package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskLogQuery;
import com.cc.job.task.model.vo.TaskLogVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * task_logMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {

    /**
     * 获取task_log分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return
     */
    Page<TaskLogVO> getTaskLogPage(Page<TaskLogVO> page, TaskLogQuery queryParams);

}
