package com.cc.job.task.service;

import com.cc.job.task.model.dto.JobInfoTriggerDto;
import com.cc.job.task.model.entity.JobInfo;
import com.cc.job.task.model.entity.JobLogglue;
import com.cc.job.task.model.form.JobGlueForm;
import com.cc.job.task.model.form.JobInfoForm;
import com.cc.job.task.model.query.JobInfoQuery;
import com.cc.job.task.model.vo.JobInfoVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * task_info服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
public interface JobInfoService extends IService<JobInfo> {

    /**
     *task_info分页列表
     *
     * @return
     */
    IPage<JobInfoVO> getTaskInfoPage(JobInfoQuery queryParams);

    /**
     * 获取task_info表单数据
     *
     * @param id task_infoID
     * @return
     */
     JobInfoForm getTaskInfoFormData(Long id);

    /**
     * 新增task_info
     *
     * @param formData task_info表单对象
     * @return
     */
    boolean saveTaskInfo(JobInfoForm formData);

    /**
     * 修改task_info
     *
     * @param id   task_infoID
     * @param formData task_info表单对象
     * @return
     */
    boolean updateTaskInfo(Long id, JobInfoForm formData);

    /**
     * 删除task_info
     *
     * @param ids task_infoID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskInfos(String ids);

    boolean triggerJob(JobInfoTriggerDto taskInfoTriggerDto);

    boolean startTask(Long id);

    boolean stopTask(Long id);

    List<String> nextTriggerTime(String scheduleType, String scheduleConf);

    boolean saveTaskSet(JobInfoForm formData);

    boolean updateTaskSet(Long id, JobInfoForm formData);

    boolean stopTaskSet(Long id,String randomId);

    boolean saveGlueSource(JobGlueForm formData);

    List<JobLogglue> getGlueList(Long id);
}
