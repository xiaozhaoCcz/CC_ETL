package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.cc.job.xo.model.entity.JobLogglue;
import com.cc.job.xo.model.form.JobLogglueForm;

/**
 * task_logglue对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Mapper(componentModel = "spring")
public interface TaskLogglueConverter{

    JobLogglueForm toForm(JobLogglue entity);

    JobLogglue toEntity(JobLogglueForm formData);
}