package com.cc.job.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskRegistryMapper;
import com.cc.job.task.service.TaskRegistryService;
import com.cc.job.task.model.entity.TaskRegistry;
import com.cc.job.task.model.form.TaskRegistryForm;
import com.cc.job.task.model.query.TaskRegistryQuery;
import com.cc.job.task.model.vo.TaskRegistryVO;
import com.cc.job.task.converter.TaskRegistryConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 执行器服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Service
@RequiredArgsConstructor
public class TaskRegistryServiceImpl extends ServiceImpl<TaskRegistryMapper, TaskRegistry> implements TaskRegistryService {

    private final TaskRegistryConverter taskRegistryConverter;

    /**
    * 获取执行器分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskRegistryVO>} 执行器分页列表
    */
    @Override
    public IPage<TaskRegistryVO> getTaskRegistryPage(TaskRegistryQuery queryParams) {
        Page<TaskRegistryVO> pageVO = this.baseMapper.getTaskRegistryPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取执行器表单数据
     *
     * @param id 执行器ID
     * @return
     */
    @Override
    public TaskRegistryForm getTaskRegistryFormData(Long id) {
        TaskRegistry entity = this.getById(id);
        return taskRegistryConverter.toForm(entity);
    }
    
    /**
     * 新增执行器
     *
     * @param formData 执行器表单对象
     * @return
     */
    @Override
    public boolean saveTaskRegistry(TaskRegistryForm formData) {
        TaskRegistry entity = taskRegistryConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新执行器
     *
     * @param id   执行器ID
     * @param formData 执行器表单对象
     * @return
     */
    @Override
    public boolean updateTaskRegistry(Long id,TaskRegistryForm formData) {
        TaskRegistry entity = taskRegistryConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除执行器
     *
     * @param ids 执行器ID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskRegistrys(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的执行器数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
