package com.cc.job.task.service;

import com.cc.job.task.model.entity.TaskGroup;
import com.cc.job.task.model.form.TaskGroupForm;
import com.cc.job.task.model.query.TaskGroupQuery;
import com.cc.job.task.model.vo.TaskGroupVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * task_group服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
public interface TaskGroupService extends IService<TaskGroup> {

    /**
     *task_group分页列表
     *
     * @return
     */
    IPage<TaskGroupVO> getTaskGroupPage(TaskGroupQuery queryParams);

    /**
     * 获取task_group表单数据
     *
     * @param id task_groupID
     * @return
     */
     TaskGroupForm getTaskGroupFormData(Long id);

    /**
     * 新增task_group
     *
     * @param formData task_group表单对象
     * @return
     */
    boolean saveTaskGroup(TaskGroupForm formData);

    /**
     * 修改task_group
     *
     * @param id   task_groupID
     * @param formData task_group表单对象
     * @return
     */
    boolean updateTaskGroup(Long id, TaskGroupForm formData);

    /**
     * 删除task_group
     *
     * @param ids task_groupID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskGroups(String ids);

    List<String> findAddressList(Long id);
}
