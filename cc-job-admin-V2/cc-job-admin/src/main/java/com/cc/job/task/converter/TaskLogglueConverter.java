package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.entity.TaskLogglue;
import com.cc.job.task.model.form.TaskLogglueForm;

/**
 * task_logglue对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Mapper(componentModel = "spring")
public interface TaskLogglueConverter{

    TaskLogglueForm toForm(TaskLogglue entity);

    TaskLogglue toEntity(TaskLogglueForm formData);
}