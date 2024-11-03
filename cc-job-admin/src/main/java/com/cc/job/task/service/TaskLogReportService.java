package com.cc.job.task.service;

import com.cc.job.task.model.entity.TaskLogReport;
import com.cc.job.task.model.form.TaskLogReportForm;
import com.cc.job.task.model.query.TaskLogReportQuery;
import com.cc.job.task.model.vo.TaskLogReportVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * task_log_report服务类
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
public interface TaskLogReportService extends IService<TaskLogReport> {

    /**
     *task_log_report分页列表
     *
     * @return
     */
    IPage<TaskLogReportVO> getTaskLogReportPage(TaskLogReportQuery queryParams);

    /**
     * 获取task_log_report表单数据
     *
     * @param id task_log_reportID
     * @return
     */
     TaskLogReportForm getTaskLogReportFormData(Long id);

    /**
     * 新增task_log_report
     *
     * @param formData task_log_report表单对象
     * @return
     */
    boolean saveTaskLogReport(TaskLogReportForm formData);

    /**
     * 修改task_log_report
     *
     * @param id   task_log_reportID
     * @param formData task_log_report表单对象
     * @return
     */
    boolean updateTaskLogReport(Long id, TaskLogReportForm formData);

    /**
     * 删除task_log_report
     *
     * @param ids task_log_reportID，多个以英文逗号(,)分割
     * @return
     */
    boolean deleteTaskLogReports(String ids);

}
