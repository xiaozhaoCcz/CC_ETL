package com.cc.job.admin.task.service;

import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLogglue;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

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
    IPage<JobInfoVO> getJobInfoPage(JobInfoQuery queryParams);

    /**
     * 获取task_info表单数据
     *
     * @param id task_infoID
     * @return
     */
     JobInfoForm getJobInfoForm(Long id);

    /**
     * 新增task_info
     *
     * @param formData task_info表单对象
     * @return
     */
    boolean saveJobInfo(JobInfoForm formData);

    /**
     * 修改task_info
     *
     * @param id   task_infoID
     * @param formData task_info表单对象
     * @return
     */
    boolean updateJobInfo(Long id, JobInfoForm formData);

    /**
     * 删除task_info
     *
     * @param ids task_infoID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteJobInfos(String ids);

    void delNodes(Long jobId);

    String triggerJob(JobInfoTriggerDto JobInfoTriggerDto);

    boolean startJob(Long id);

    boolean stopJob(Long id);

    List<String> nextTriggerTime(String scheduleType, String scheduleConf);

    boolean saveJobCompose(JobInfoForm formData);

    JobInfo baseSaveJobInfo(JobInfoForm formData);

    boolean updateJobCompose(Long id, JobInfoForm formData);

    boolean stopJobCompose(Long id,String randomId);

    boolean saveGlueSource(JobGlueForm formData);

    JobInfo baseUpdateJobInfo(Long id, JobInfoForm formData);

    List<JobLogglue> getGlueList(Long id);

    List<Long> initData();

    boolean pauseJob(Long id, Integer isPause);
}
