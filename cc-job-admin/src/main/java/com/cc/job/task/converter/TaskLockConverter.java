package com.cc.job.task.converter;

import org.mapstruct.Mapper;
import com.cc.job.task.model.entity.JobLock;
import com.cc.job.task.model.form.JobLockForm;

/**
 * task_lock对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper(componentModel = "spring")
public interface TaskLockConverter{

    JobLockForm toForm(JobLock entity);

    JobLock toEntity(JobLockForm formData);
}