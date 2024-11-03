package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * task_infoMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper
public interface TaskInfoMapper extends BaseMapper<TaskInfo> {

    /**
     * 获取task_info分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return
     */
    Page<TaskInfoVO> getTaskInfoPage(Page<TaskInfoVO> page, TaskInfoQuery queryParams);

}
