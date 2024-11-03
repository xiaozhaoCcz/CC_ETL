package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskGroup;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskGroupQuery;
import com.cc.job.task.model.vo.TaskGroupVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * task_groupMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper
public interface TaskGroupMapper extends BaseMapper<TaskGroup> {

    /**
     * 获取task_group分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return
     */
    Page<TaskGroupVO> getTaskGroupPage(Page<TaskGroupVO> page, TaskGroupQuery queryParams);

}
