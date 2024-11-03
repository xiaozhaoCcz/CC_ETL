package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.entity.TaskLog;
import com.cc.job.task.model.form.TaskLogForm;

/**
 * task_log对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Mapper(componentModel = "spring")
public interface TaskLogConverter{

    TaskLogForm toForm(TaskLog entity);

    TaskLog toEntity(TaskLogForm formData);
}