package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.entity.TaskLogReport;
import com.cc.job.task.model.form.TaskLogReportForm;

/**
 * task_log_report对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Mapper(componentModel = "spring")
public interface TaskLogReportConverter{

    TaskLogReportForm toForm(TaskLogReport entity);

    TaskLogReport toEntity(TaskLogReportForm formData);
}