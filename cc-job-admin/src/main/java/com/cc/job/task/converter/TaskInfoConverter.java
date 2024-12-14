package com.cc.job.task.converter;

import com.cc.job.task.model.vo.TaskInfoVO;
import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.form.TaskInfoForm;

/**
 * task_info对象转换器
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Mapper(componentModel = "spring")
public interface TaskInfoConverter{

    TaskInfoForm toForm(TaskInfo entity);

    TaskInfo toEntity(TaskInfoForm formData);

    TaskInfoVO toVo(TaskInfo taskInfo);
}