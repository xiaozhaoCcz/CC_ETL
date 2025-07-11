package com.cc.job.admin.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.admin.task.scheduler.XxlJobScheduler;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.utils.DateUtils;
import com.cc.job.admin.task.utils.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobLogMapper;
import com.cc.job.admin.task.service.JobLogService;
import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;
import com.cc.job.admin.task.converter.TaskLogConverter;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class JobLogServiceImpl extends ServiceImpl<JobLogMapper, JobLog> implements JobLogService {
    private final JobInfoService taskInfoService;

    /**
    * 获取task_log分页列表
    * @param queryParams 查询参数
    * @return {@link IPage< JobLogVO >} task_log分页列表
    */
    @Override
    public IPage<JobLogVO> getJobLogPage(JobLogQuery queryParams) {
        IPage<JobLogVO> pageVO = new Page<>();
        LambdaQueryWrapper<JobLog> wrapper = new LambdaQueryWrapper<>();

        if(queryParams.getJobId()!=null){
            wrapper.eq(JobLog::getJobId,queryParams.getJobId());
        }
        baseWrapper(queryParams, wrapper);
        wrapper.orderByDesc(JobLog::getTriggerTime);

        Page<JobLog> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<JobLog> taskLogList = page.getRecords();
        List<Long> taskIds = taskLogList.stream().map(JobLog::getJobId).toList();

        if(taskIds.isEmpty()){
            return pageVO;
        }
        List<JobInfo> taskInfos = taskInfoService.listByIds(taskIds);
        Map<Long, JobInfo> taskInfoMap = taskInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));

        List<JobLogVO> voList  = new ArrayList<>();

        for (JobLog taskLog : taskLogList) {
            JobLogVO taskLogVO = BeanUtil.copyProperties(taskLog, JobLogVO.class);
            JobInfo taskInfo = taskInfoMap.get(taskLog.getJobId());
            if(taskInfo!=null){
                taskLogVO.setJobDesc(taskInfo.getJobDesc());
                taskLogVO.setJobType(taskInfo.getJobType());
            }
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
    public boolean deleteJobLogs(JobLogQuery queryParams) {
        LambdaQueryWrapper<JobLog> wrapper = new LambdaQueryWrapper<>();
        // 逻辑删除
        baseWrapper(queryParams, wrapper);
        return this.remove(wrapper);
    }

    @Override
    public ReturnT<LogResult> getLogDetailCat(Long logId, int fromLineNum) {
        try {
            // valid
            JobLog jobLog = this.getById(logId);	// todo, need to improve performance
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

    private void baseWrapper(JobLogQuery queryParams, LambdaQueryWrapper<JobLog> wrapper) {
        if(queryParams.getJobGroup()!=null){
            wrapper.eq(JobLog::getJobGroup, queryParams.getJobGroup());
        }

        if(queryParams.getLogStatus()!=null){
            if(queryParams.getLogStatus()==1){
                wrapper.eq(JobLog::getHandleCode, 200);
            }else if(queryParams.getLogStatus()==2){
                wrapper.and(
                        e -> e.notIn(JobLog::getTriggerCode, 0, 200).or()
                                .notIn(JobLog::getHandleCode,0, 200));
            }else if(queryParams.getLogStatus()==3){
                wrapper.eq(JobLog::getTriggerCode,200).eq(JobLog::getHandleCode,0);
            }
        }

        if(queryParams.getFilterTime()!=null&&queryParams.getFilterTime().length>0){
            wrapper.between(JobLog::getTriggerTime, DateUtils.formatDate(queryParams.getFilterTime()[0]),  DateUtils.formatDate(queryParams.getFilterTime()[1]));
        }
    }

}
