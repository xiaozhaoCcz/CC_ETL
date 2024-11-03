package com.cc.job.task.service;

import com.cc.job.task.model.entity.TaskLogglue;
import com.cc.job.task.model.form.TaskLogglueForm;
import com.cc.job.task.model.query.TaskLogglueQuery;
import com.cc.job.task.model.vo.TaskLogglueVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * task_logglue服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
public interface TaskLogglueService extends IService<TaskLogglue> {

    /**
     *task_logglue分页列表
     *
     * @return
     */
    IPage<TaskLogglueVO> getTaskLoggluePage(TaskLogglueQuery queryParams);

    /**
     * 获取task_logglue表单数据
     *
     * @param id task_logglueID
     * @return
     */
     TaskLogglueForm getTaskLogglueFormData(Long id);

    /**
     * 新增task_logglue
     *
     * @param formData task_logglue表单对象
     * @return
     */
    boolean saveTaskLogglue(TaskLogglueForm formData);

    /**
     * 修改task_logglue
     *
     * @param id   task_logglueID
     * @param formData task_logglue表单对象
     * @return
     */
    boolean updateTaskLogglue(Long id, TaskLogglueForm formData);

    /**
     * 删除task_logglue
     *
     * @param ids task_logglueID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskLogglues(String ids);

}
