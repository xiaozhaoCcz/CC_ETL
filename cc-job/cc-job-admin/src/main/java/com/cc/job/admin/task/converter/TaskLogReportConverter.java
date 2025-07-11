package com.cc.job.admin.task.converter;

import org.mapstruct.Mapper;
import com.cc.job.xo.model.entity.JobLogReport;
import com.cc.job.xo.model.form.JobLogReportForm;

/**
 * task_log_report对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Mapper(componentModel = "spring")
public interface TaskLogReportConverter{

    JobLogReportForm toForm(JobLogReport entity);

    JobLogReport toEntity(JobLogReportForm formData);
}