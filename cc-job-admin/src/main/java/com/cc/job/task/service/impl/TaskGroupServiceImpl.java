package com.cc.job.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskGroupMapper;
import com.cc.job.task.service.TaskGroupService;
import com.cc.job.task.model.entity.TaskGroup;
import com.cc.job.task.model.form.TaskGroupForm;
import com.cc.job.task.model.query.TaskGroupQuery;
import com.cc.job.task.model.vo.TaskGroupVO;
import com.cc.job.task.converter.TaskGroupConverter;

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
public class TaskGroupServiceImpl extends ServiceImpl<TaskGroupMapper, TaskGroup> implements TaskGroupService {

    private final TaskGroupConverter taskGroupConverter;

    /**
    * 获取task_group分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskGroupVO>} task_group分页列表
    */
    @Override
    public IPage<TaskGroupVO> getTaskGroupPage(TaskGroupQuery queryParams) {
        Page<TaskGroupVO> pageVO = this.baseMapper.getTaskGroupPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取task_group表单数据
     *
     * @param id task_groupID
     * @return
     */
    @Override
    public TaskGroupForm getTaskGroupFormData(Long id) {
        TaskGroup entity = this.getById(id);
        return taskGroupConverter.toForm(entity);
    }
    
    /**
     * 新增task_group
     *
     * @param formData task_group表单对象
     * @return
     */
    @Override
    public boolean saveTaskGroup(TaskGroupForm formData) {
        TaskGroup entity = taskGroupConverter.toEntity(formData);
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
    public boolean updateTaskGroup(Long id,TaskGroupForm formData) {
        TaskGroup entity = taskGroupConverter.toEntity(formData);
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

}
