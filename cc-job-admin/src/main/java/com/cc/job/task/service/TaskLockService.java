package com.cc.job.task.service;

import com.cc.job.task.model.entity.TaskLock;
import com.cc.job.task.model.form.TaskLockForm;
import com.cc.job.task.model.query.TaskLockQuery;
import com.cc.job.task.model.vo.TaskLockVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * task_lock服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
public interface TaskLockService extends IService<TaskLock> {

    /**
     *task_lock分页列表
     *
     * @return
     */
    IPage<TaskLockVO> getTaskLockPage(TaskLockQuery queryParams);

    /**
     * 获取task_lock表单数据
     *
     * @param id task_lockID
     * @return
     */
     TaskLockForm getTaskLockFormData(Long id);

    /**
     * 新增task_lock
     *
     * @param formData task_lock表单对象
     * @return
     */
    boolean saveTaskLock(TaskLockForm formData);

    /**
     * 修改task_lock
     *
     * @param id   task_lockID
     * @param formData task_lock表单对象
     * @return
     */
    boolean updateTaskLock(Long id, TaskLockForm formData);

    /**
     * 删除task_lock
     *
     * @param ids task_lockID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskLocks(String ids);

}
