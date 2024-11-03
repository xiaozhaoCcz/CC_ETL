package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.entity.TaskGroup;
import com.cc.job.task.model.form.TaskGroupForm;

/**
 * task_group对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper(componentModel = "spring")
public interface TaskGroupConverter{

    TaskGroupForm toForm(TaskGroup entity);

    TaskGroup toEntity(TaskGroupForm formData);
}