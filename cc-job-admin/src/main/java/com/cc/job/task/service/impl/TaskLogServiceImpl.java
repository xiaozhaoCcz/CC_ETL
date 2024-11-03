package com.cc.job.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskLogMapper;
import com.cc.job.task.service.TaskLogService;
import com.cc.job.task.model.entity.TaskLog;
import com.cc.job.task.model.form.TaskLogForm;
import com.cc.job.task.model.query.TaskLogQuery;
import com.cc.job.task.model.vo.TaskLogVO;
import com.cc.job.task.converter.TaskLogConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * task_log服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Service
@RequiredArgsConstructor
public class TaskLogServiceImpl extends ServiceImpl<TaskLogMapper, TaskLog> implements TaskLogService {

    private final TaskLogConverter taskLogConverter;

    /**
    * 获取task_log分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskLogVO>} task_log分页列表
    */
    @Override
    public IPage<TaskLogVO> getTaskLogPage(TaskLogQuery queryParams) {
        Page<TaskLogVO> pageVO = this.baseMapper.getTaskLogPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取task_log表单数据
     *
     * @param id task_logID
     * @return
     */
    @Override
    public TaskLogForm getTaskLogFormData(Long id) {
        TaskLog entity = this.getById(id);
        return taskLogConverter.toForm(entity);
    }
    
    /**
     * 新增task_log
     *
     * @param formData task_log表单对象
     * @return
     */
    @Override
    public boolean saveTaskLog(TaskLogForm formData) {
        TaskLog entity = taskLogConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新task_log
     *
     * @param id   task_logID
     * @param formData task_log表单对象
     * @return
     */
    @Override
    public boolean updateTaskLog(Long id,TaskLogForm formData) {
        TaskLog entity = taskLogConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除task_log
     *
     * @param ids task_logID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskLogs(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_log数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
