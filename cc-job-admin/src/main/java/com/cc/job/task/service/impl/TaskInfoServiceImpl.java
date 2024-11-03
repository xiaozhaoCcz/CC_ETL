package com.cc.job.task.service.impl;

import com.cc.job.task.enums.TriggerTypeEnum;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.xxl.job.core.biz.model.ReturnT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskInfoMapper;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.form.TaskInfoForm;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;
import com.cc.job.task.converter.TaskInfoConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * task_info服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Service
@RequiredArgsConstructor
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, TaskInfo> implements TaskInfoService {

    private final TaskInfoConverter taskInfoConverter;

    /**
    * 获取task_info分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskInfoVO>} task_info分页列表
    */
    @Override
    public IPage<TaskInfoVO> getTaskInfoPage(TaskInfoQuery queryParams) {
        Page<TaskInfoVO> pageVO = this.baseMapper.getTaskInfoPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取task_info表单数据
     *
     * @param id task_infoID
     * @return
     */
    @Override
    public TaskInfoForm getTaskInfoFormData(Long id) {
        TaskInfo entity = this.getById(id);
        return taskInfoConverter.toForm(entity);
    }
    
    /**
     * 新增task_info
     *
     * @param formData task_info表单对象
     * @return
     */
    @Override
    public boolean saveTaskInfo(TaskInfoForm formData) {
        TaskInfo entity = taskInfoConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新task_info
     *
     * @param id   task_infoID
     * @param formData task_info表单对象
     * @return
     */
    @Override
    public boolean updateTaskInfo(Long id,TaskInfoForm formData) {
        TaskInfo entity = taskInfoConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除task_info
     *
     * @param ids task_infoID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskInfos(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_info数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public boolean triggerJob(TaskInfoTriggerDto taskInfoTriggerDto) {

//        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(jobId);
//        if (xxlJobInfo == null) {
//            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
//        }
        // force cover job param
        if (taskInfoTriggerDto.getExecutorParam() == null) {
            taskInfoTriggerDto.setExecutorParam("");
        }

        //JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.MANUAL, -1, null, taskInfoTriggerDto.getExecutorParam(), taskInfoTriggerDto.getAddressList());
        return true;
    }

}
