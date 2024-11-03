package com.cc.job.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskLogglueMapper;
import com.cc.job.task.service.TaskLogglueService;
import com.cc.job.task.model.entity.TaskLogglue;
import com.cc.job.task.model.form.TaskLogglueForm;
import com.cc.job.task.model.query.TaskLogglueQuery;
import com.cc.job.task.model.vo.TaskLogglueVO;
import com.cc.job.task.converter.TaskLogglueConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * task_logglue服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Service
@RequiredArgsConstructor
public class TaskLogglueServiceImpl extends ServiceImpl<TaskLogglueMapper, TaskLogglue> implements TaskLogglueService {

    private final TaskLogglueConverter taskLogglueConverter;

    /**
    * 获取task_logglue分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskLogglueVO>} task_logglue分页列表
    */
    @Override
    public IPage<TaskLogglueVO> getTaskLoggluePage(TaskLogglueQuery queryParams) {
        Page<TaskLogglueVO> pageVO = this.baseMapper.getTaskLoggluePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取task_logglue表单数据
     *
     * @param id task_logglueID
     * @return
     */
    @Override
    public TaskLogglueForm getTaskLogglueFormData(Long id) {
        TaskLogglue entity = this.getById(id);
        return taskLogglueConverter.toForm(entity);
    }
    
    /**
     * 新增task_logglue
     *
     * @param formData task_logglue表单对象
     * @return
     */
    @Override
    public boolean saveTaskLogglue(TaskLogglueForm formData) {
        TaskLogglue entity = taskLogglueConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新task_logglue
     *
     * @param id   task_logglueID
     * @param formData task_logglue表单对象
     * @return
     */
    @Override
    public boolean updateTaskLogglue(Long id,TaskLogglueForm formData) {
        TaskLogglue entity = taskLogglueConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除task_logglue
     *
     * @param ids task_logglueID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskLogglues(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_logglue数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
