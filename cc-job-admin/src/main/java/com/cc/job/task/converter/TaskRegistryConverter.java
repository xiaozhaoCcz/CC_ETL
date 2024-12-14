package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.entity.TaskRegistry;
import com.cc.job.task.model.form.TaskRegistryForm;

/**
 * 执行器对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Mapper(componentModel = "spring")
public interface TaskRegistryConverter{

    TaskRegistryForm toForm(TaskRegistry entity);

    TaskRegistry toEntity(TaskRegistryForm formData);
}