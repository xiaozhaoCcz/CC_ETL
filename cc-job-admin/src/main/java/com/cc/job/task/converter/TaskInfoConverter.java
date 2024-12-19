package com.cc.job.task.converter;

import com.cc.job.xo.model.vo.JobInfoVO;
import org.mapstruct.Mapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.form.JobInfoForm;

/**
 * task_info对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper(componentModel = "spring")
public interface TaskInfoConverter{

    JobInfoForm toForm(JobInfo entity);

    JobInfo toEntity(JobInfoForm formData);

    JobInfoVO toVo(JobInfo taskInfo);
}