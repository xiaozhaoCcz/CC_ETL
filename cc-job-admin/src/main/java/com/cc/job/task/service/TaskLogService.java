package com.cc.job.task.service;

import com.cc.job.task.model.entity.TaskLog;
import com.cc.job.task.model.form.TaskLogForm;
import com.cc.job.task.model.query.TaskLogQuery;
import com.cc.job.task.model.vo.TaskLogVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * task_log服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
public interface TaskLogService extends IService<TaskLog> {

    /**
     *task_log分页列表
     *
     * @return
     */
    IPage<TaskLogVO> getTaskLogPage(TaskLogQuery queryParams);

    /**
     * 获取task_log表单数据
     *
     * @param id task_logID
     * @return
     */
     TaskLogForm getTaskLogFormData(Long id);

    /**
     * 新增task_log
     *
     * @param formData task_log表单对象
     * @return
     */
    boolean saveTaskLog(TaskLogForm formData);

    /**
     * 修改task_log
     *
     * @param id   task_logID
     * @param formData task_log表单对象
     * @return
     */
    boolean updateTaskLog(Long id, TaskLogForm formData);

    /**
     * 删除task_log
     *
     * @param ids task_logID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskLogs(String ids);

}
