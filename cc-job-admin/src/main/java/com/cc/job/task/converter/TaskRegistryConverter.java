package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.cc.job.xo.model.entity.JobRegistry;
import com.cc.job.xo.model.form.JobRegistryForm;

/**
 * 执行器对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Mapper(componentModel = "spring")
public interface TaskRegistryConverter{

    JobRegistryForm toForm(JobRegistry entity);

    JobRegistry toEntity(JobRegistryForm formData);
}