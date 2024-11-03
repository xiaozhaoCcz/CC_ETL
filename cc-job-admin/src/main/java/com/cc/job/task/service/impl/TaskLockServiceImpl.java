package com.cc.job.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskLockMapper;
import com.cc.job.task.service.TaskLockService;
import com.cc.job.task.model.entity.TaskLock;
import com.cc.job.task.model.form.TaskLockForm;
import com.cc.job.task.model.query.TaskLockQuery;
import com.cc.job.task.model.vo.TaskLockVO;
import com.cc.job.task.converter.TaskLockConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * task_lock服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Service
@RequiredArgsConstructor
public class TaskLockServiceImpl extends ServiceImpl<TaskLockMapper, TaskLock> implements TaskLockService {

    private final TaskLockConverter taskLockConverter;

    /**
    * 获取task_lock分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskLockVO>} task_lock分页列表
    */
    @Override
    public IPage<TaskLockVO> getTaskLockPage(TaskLockQuery queryParams) {
        Page<TaskLockVO> pageVO = this.baseMapper.getTaskLockPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取task_lock表单数据
     *
     * @param id task_lockID
     * @return
     */
    @Override
    public TaskLockForm getTaskLockFormData(Long id) {
        TaskLock entity = this.getById(id);
        return taskLockConverter.toForm(entity);
    }
    
    /**
     * 新增task_lock
     *
     * @param formData task_lock表单对象
     * @return
     */
    @Override
    public boolean saveTaskLock(TaskLockForm formData) {
        TaskLock entity = taskLockConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新task_lock
     *
     * @param id   task_lockID
     * @param formData task_lock表单对象
     * @return
     */
    @Override
    public boolean updateTaskLock(Long id,TaskLockForm formData) {
        TaskLock entity = taskLockConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除task_lock
     *
     * @param ids task_lockID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskLocks(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_lock数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
