package com.cc.job.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.common.exception.BusinessException;
import com.cc.job.common.result.ResultCode;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.vo.TaskGroupVO;
import com.cc.job.task.scheduler.XxlJobScheduler;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.utils.DateUtils;
import com.cc.job.task.utils.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
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

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

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

    private final TaskInfoService taskInfoService;

    /**
    * 获取task_log分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskLogVO>} task_log分页列表
    */
    @Override
    public IPage<TaskLogVO> getTaskLogPage(TaskLogQuery queryParams) {
        IPage<TaskLogVO> pageVO = new Page<>();
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();

        if(queryParams.getJobId()!=null){
            wrapper.eq(TaskLog::getJobId,queryParams.getJobId());
        }
        baseWrapper(queryParams, wrapper);
        wrapper.orderByDesc(TaskLog::getTriggerTime);

        Page<TaskLog> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<TaskLog> taskLogList = page.getRecords();
        List<Long> taskIds = taskLogList.stream().map(TaskLog::getJobId).toList();

        List<TaskInfo> taskInfos = taskInfoService.listByIds(taskIds);
        Map<Long, TaskInfo> taskInfoMap = taskInfos.stream().collect(Collectors.toMap(TaskInfo::getId, t -> t));

        List<TaskLogVO> voList  = new ArrayList<>();

        for (TaskLog taskLog : taskLogList) {
            TaskLogVO taskLogVO = BeanUtil.copyProperties(taskLog, TaskLogVO.class);
            TaskInfo taskInfo = taskInfoMap.get(taskLog.getJobId());
            taskLogVO.setJobType(taskInfo.getJobType());
            voList.add(taskLogVO);
        }
        pageVO.setRecords(voList);
        pageVO.setTotal(page.getTotal());
        return pageVO;
    }

    
    /**
     * 删除task_log
     *
     * @param ids task_logID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskLogs(TaskLogQuery queryParams) {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        // 逻辑删除
        baseWrapper(queryParams, wrapper);
        return  this.remove(wrapper);
    }

    @Override
    public ReturnT<LogResult> getLogDetailCat(Long logId, int fromLineNum) {
        try {
            // valid
            TaskLog jobLog = this.getById(logId);	// todo, need to improve performance
            if (jobLog == null) {
                return new ReturnT<LogResult>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_logid_unvalid"));
            }

            // log cat
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
            ReturnT<LogResult> logResult = executorBiz.log(new LogParam(jobLog.getTriggerTime().toInstant(ZoneOffset.of("+8")).toEpochMilli(), logId, fromLineNum));

            // is end
            if (logResult.getContent()!=null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

            // fix xss
            if (logResult.getContent()!=null && StringUtils.hasText(logResult.getContent().getLogContent())) {
                String newLogContent = logResult.getContent().getLogContent();
                newLogContent = HtmlUtils.htmlEscape(newLogContent, "UTF-8");
                logResult.getContent().setLogContent(newLogContent);
            }
            return logResult;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ReturnT<LogResult>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    private void baseWrapper(TaskLogQuery queryParams, LambdaQueryWrapper<TaskLog> wrapper) {
        if(queryParams.getJobGroup()!=null){
            wrapper.eq(TaskLog::getJobGroup, queryParams.getJobGroup());
        }

        if(queryParams.getLogStatus()!=null){
            if(queryParams.getLogStatus()==1){
                wrapper.eq(TaskLog::getHandleCode, 200);
            }else if(queryParams.getLogStatus()==2){
                wrapper.and(
                        e -> e.notIn(TaskLog::getTriggerCode, 0, 200).or()
                                .notIn(TaskLog::getHandleCode,0, 200));
            }else if(queryParams.getLogStatus()==3){
                wrapper.eq(TaskLog::getTriggerCode,200).eq(TaskLog::getHandleCode,0);
            }
        }

        if(queryParams.getFilterTime()!=null&&queryParams.getFilterTime().length>0){
            wrapper.between(TaskLog::getTriggerTime, DateUtils.formatDate(queryParams.getFilterTime()[0]),  DateUtils.formatDate(queryParams.getFilterTime()[1]));
        }
    }

}
