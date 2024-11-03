package com.cc.job.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskLogReportMapper;
import com.cc.job.task.service.TaskLogReportService;
import com.cc.job.task.model.entity.TaskLogReport;
import com.cc.job.task.model.form.TaskLogReportForm;
import com.cc.job.task.model.query.TaskLogReportQuery;
import com.cc.job.task.model.vo.TaskLogReportVO;
import com.cc.job.task.converter.TaskLogReportConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * task_log_report服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Service
@RequiredArgsConstructor
public class TaskLogReportServiceImpl extends ServiceImpl<TaskLogReportMapper, TaskLogReport> implements TaskLogReportService {

    private final TaskLogReportConverter taskLogReportConverter;

    /**
    * 获取task_log_report分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskLogReportVO>} task_log_report分页列表
    */
    @Override
    public IPage<TaskLogReportVO> getTaskLogReportPage(TaskLogReportQuery queryParams) {
        Page<TaskLogReportVO> pageVO = this.baseMapper.getTaskLogReportPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取task_log_report表单数据
     *
     * @param id task_log_reportID
     * @return
     */
    @Override
    public TaskLogReportForm getTaskLogReportFormData(Long id) {
        TaskLogReport entity = this.getById(id);
        return taskLogReportConverter.toForm(entity);
    }
    
    /**
     * 新增task_log_report
     *
     * @param formData task_log_report表单对象
     * @return
     */
    @Override
    public boolean saveTaskLogReport(TaskLogReportForm formData) {
        TaskLogReport entity = taskLogReportConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新task_log_report
     *
     * @param id   task_log_reportID
     * @param formData task_log_report表单对象
     * @return
     */
    @Override
    public boolean updateTaskLogReport(Long id,TaskLogReportForm formData) {
        TaskLogReport entity = taskLogReportConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除task_log_report
     *
     * @param ids task_log_reportID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskLogReports(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_log_report数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
