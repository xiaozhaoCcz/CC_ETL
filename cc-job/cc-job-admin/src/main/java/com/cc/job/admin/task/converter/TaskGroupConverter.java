package com.cc.job.admin.task.converter;

import com.cc.job.xo.model.vo.JobGroupVO;
import org.mapstruct.Mapper;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobGroupForm;

/**
 * task_group对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper(componentModel = "spring")
public interface TaskGroupConverter{

    JobGroupForm toForm(JobGroup entity);

    JobGroup toEntity(JobGroupForm formData);

    JobGroupVO toVo(JobGroup entity);
}