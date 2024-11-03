package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskLogglue;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskLogglueQuery;
import com.cc.job.task.model.vo.TaskLogglueVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * task_logglueMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Mapper
public interface TaskLogglueMapper extends BaseMapper<TaskLogglue> {

    /**
     * 获取task_logglue分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return
     */
    Page<TaskLogglueVO> getTaskLoggluePage(Page<TaskLogglueVO> page, TaskLogglueQuery queryParams);

}
