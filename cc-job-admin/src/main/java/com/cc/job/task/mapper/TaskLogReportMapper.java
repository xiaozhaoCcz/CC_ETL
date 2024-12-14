package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskLogReport;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskLogReportQuery;
import com.cc.job.task.model.vo.TaskLogReportVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * task_log_reportMapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Mapper
public interface TaskLogReportMapper extends BaseMapper<TaskLogReport> {



}
