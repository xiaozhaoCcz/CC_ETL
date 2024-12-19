package com.cc.job.task.converter;

import com.cc.job.xo.model.vo.JobLogVO;
import org.mapstruct.Mapper;
import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.xo.model.form.JobLogForm;

/**
 * task_log对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Mapper(componentModel = "spring")
public interface TaskLogConverter{

    JobLogForm toForm(JobLog entity);

    JobLog toEntity(JobLogForm formData);

    JobLogVO toVo(JobLog taskLog);
}