package com.cc.job.task.service;

import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.form.TaskInfoForm;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * task_info服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
public interface TaskInfoService extends IService<TaskInfo> {

    /**
     *task_info分页列表
     *
     * @return
     */
    IPage<TaskInfoVO> getTaskInfoPage(TaskInfoQuery queryParams);

    /**
     * 获取task_info表单数据
     *
     * @param id task_infoID
     * @return
     */
     TaskInfoForm getTaskInfoFormData(Long id);

    /**
     * 新增task_info
     *
     * @param formData task_info表单对象
     * @return
     */
    boolean saveTaskInfo(TaskInfoForm formData);

    /**
     * 修改task_info
     *
     * @param id   task_infoID
     * @param formData task_info表单对象
     * @return
     */
    boolean updateTaskInfo(Long id, TaskInfoForm formData);

    /**
     * 删除task_info
     *
     * @param ids task_infoID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskInfos(String ids);

    boolean triggerJob(TaskInfoTriggerDto taskInfoTriggerDto);
}
