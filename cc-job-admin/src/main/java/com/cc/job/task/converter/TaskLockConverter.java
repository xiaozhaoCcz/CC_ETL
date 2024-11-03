package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.entity.TaskLock;
import com.cc.job.task.model.form.TaskLockForm;

/**
 * task_lock对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper(componentModel = "spring")
public interface TaskLockConverter{

    TaskLockForm toForm(TaskLock entity);

    TaskLock toEntity(TaskLockForm formData);
}