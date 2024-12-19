package com.cc.job.task.service;

import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobGroupForm;
import com.cc.job.xo.model.query.JobGroupQuery;
import com.cc.job.xo.model.vo.JobGroupVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * task_group服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
public interface JobGroupService extends IService<JobGroup> {

    /**
     *task_group分页列表
     *
     * @return
     */
    IPage<JobGroupVO> getTaskGroupPage(JobGroupQuery queryParams);

    /**
     * 获取task_group表单数据
     *
     * @param id task_groupID
     * @return
     */
     JobGroupForm getTaskGroupFormData(Long id);

    /**
     * 新增task_group
     *
     * @param formData task_group表单对象
     * @return
     */
    boolean saveTaskGroup(JobGroupForm formData);

    /**
     * 修改task_group
     *
     * @param id   task_groupID
     * @param formData task_group表单对象
     * @return
     */
    boolean updateTaskGroup(Long id, JobGroupForm formData);

    /**
     * 删除task_group
     *
     * @param ids task_groupID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskGroups(String ids);

    List<String> findAddressList(Long id);
}
