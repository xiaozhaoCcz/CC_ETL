package com.cc.job.task.service;

import com.cc.job.task.model.entity.TaskRegistry;
import com.cc.job.task.model.form.TaskRegistryForm;
import com.cc.job.task.model.query.TaskRegistryQuery;
import com.cc.job.task.model.vo.TaskRegistryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 执行器服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
public interface TaskRegistryService extends IService<TaskRegistry> {

    /**
     *执行器分页列表
     *
     * @return
     */
    IPage<TaskRegistryVO> getTaskRegistryPage(TaskRegistryQuery queryParams);

    /**
     * 获取执行器表单数据
     *
     * @param id 执行器ID
     * @return
     */
     TaskRegistryForm getTaskRegistryFormData(Long id);

    /**
     * 新增执行器
     *
     * @param formData 执行器表单对象
     * @return
     */
    boolean saveTaskRegistry(TaskRegistryForm formData);

    /**
     * 修改执行器
     *
     * @param id   执行器ID
     * @param formData 执行器表单对象
     * @return
     */
    boolean updateTaskRegistry(Long id, TaskRegistryForm formData);

    /**
     * 删除执行器
     *
     * @param ids 执行器ID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskRegistrys(String ids);

}
