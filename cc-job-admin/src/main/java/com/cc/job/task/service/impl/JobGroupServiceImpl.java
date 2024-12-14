package com.cc.job.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.JobGroupMapper;
import com.cc.job.task.service.JobGroupService;
import com.cc.job.task.model.entity.JobGroup;
import com.cc.job.task.model.form.JobGroupForm;
import com.cc.job.task.model.query.JobGroupQuery;
import com.cc.job.task.model.vo.JobGroupVO;
import com.cc.job.task.converter.TaskGroupConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * task_group服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Service
@RequiredArgsConstructor
public class JobGroupServiceImpl extends ServiceImpl<JobGroupMapper, JobGroup> implements JobGroupService {

    private final TaskGroupConverter taskGroupConverter;

    /**
    * 获取task_group分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage< JobGroupVO >} task_group分页列表
    */
    @Override
    public IPage<JobGroupVO> getTaskGroupPage(JobGroupQuery queryParams) {
        Page<JobGroupVO> pageVO = new Page<>();
        LambdaQueryWrapper<JobGroup> wrapper= new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(queryParams.getAppName())){
            wrapper.like(JobGroup::getAppName,queryParams.getAppName());
        }
        if(StringUtils.isNotBlank(queryParams.getTitle())){
            wrapper.like(JobGroup::getTitle,queryParams.getTitle());
        }
        Page<JobGroup> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<JobGroup> records = page.getRecords();
        List<JobGroupVO> voList = records.stream().map(taskGroupConverter::toVo).toList();
        pageVO.setRecords(voList);
        pageVO.setTotal(page.getTotal());
        return pageVO;
    }
    
    /**
     * 获取task_group表单数据
     *
     * @param id task_groupID
     * @return
     */
    @Override
    public JobGroupForm getTaskGroupFormData(Long id) {
        JobGroup entity = this.getById(id);
        return taskGroupConverter.toForm(entity);
    }
    
    /**
     * 新增task_group
     *
     * @param formData task_group表单对象
     * @return
     */
    @Override
    public boolean saveTaskGroup(JobGroupForm formData) {
        JobGroup entity = taskGroupConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新task_group
     *
     * @param id   task_groupID
     * @param formData task_group表单对象
     * @return
     */
    @Override
    public boolean updateTaskGroup(Long id, JobGroupForm formData) {
        JobGroup entity = taskGroupConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除task_group
     *
     * @param ids task_groupID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskGroups(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_group数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public List<String> findAddressList(Long id) {
        JobGroup taskGroup = this.getById(id);
        if (taskGroup!= null && StrUtil.isNotBlank(taskGroup.getAddressList())) {
            return Arrays.stream(taskGroup.getAddressList().split(","))
                   .map(String::trim)
                   .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

}
