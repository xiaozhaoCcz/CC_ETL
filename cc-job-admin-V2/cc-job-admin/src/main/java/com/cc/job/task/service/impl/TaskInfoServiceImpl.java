package com.cc.job.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.common.exception.BusinessException;
import com.cc.job.core.cron.CronExpression;
import com.cc.job.task.enums.*;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskGroup;
import com.cc.job.task.model.vo.TaskGroupVO;
import com.cc.job.task.service.TaskGroupService;
import com.cc.job.task.thread.JobScheduleHelper;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.cc.job.task.utils.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import jodd.util.Task;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskInfoMapper;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.form.TaskInfoForm;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;
import com.cc.job.task.converter.TaskInfoConverter;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * task_info服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Service
@RequiredArgsConstructor
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, TaskInfo> implements TaskInfoService {

    private final TaskInfoConverter taskInfoConverter;

    private final TaskGroupService taskGroupService;

    /**
    * 获取task_info分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TaskInfoVO>} task_info分页列表
    */
    @Override
    public IPage<TaskInfoVO> getTaskInfoPage(TaskInfoQuery queryParams) {
//        Page<TaskInfoVO> pageVO = this.baseMapper.getTaskInfoPage(
//                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
//                queryParams
//        );
        Page<TaskInfoVO> pageVO = new Page<>();
        LambdaQueryWrapper<TaskInfo> wrapper = new LambdaQueryWrapper<>();
        if(queryParams.getJobGroup()!=null){
            wrapper.eq(TaskInfo::getJobGroup, queryParams.getJobGroup());
        }
        if(queryParams.getTriggerStatus()!=null){
            wrapper.eq(TaskInfo::getTriggerStatus, queryParams.getTriggerStatus());
        }
        if(StringUtils.isNotBlank(queryParams.getAuthor())){
            wrapper.like(TaskInfo::getAuthor, queryParams.getAuthor());
        }
        if(StringUtils.isNotBlank(queryParams.getJobDesc())){
            wrapper.like(TaskInfo::getJobDesc, queryParams.getJobDesc());
        }
        if(StringUtils.isNotBlank(queryParams.getExecutorHandler())){
            wrapper.eq(TaskInfo::getExecutorHandler,queryParams.getExecutorHandler());
        }



        Page<TaskInfo> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<TaskInfo> records = page.getRecords();
        List<TaskInfoVO> voList = records.stream().map(taskInfoConverter::toVo).toList();
        pageVO.setRecords(voList);
        pageVO.setTotal(page.getTotal());
        return pageVO;
    }
    
    /**
     * 获取task_info表单数据
     *
     * @param id task_infoID
     * @return
     */
    @Override
    public TaskInfoForm getTaskInfoFormData(Long id) {
        TaskInfo entity = this.getById(id);
        return taskInfoConverter.toForm(entity);
    }
    
    /**
     * 新增task_info
     *
     * @param formData task_info表单对象
     * @return
     */
    @Override
    public boolean saveTaskInfo(TaskInfoForm formData) {
        TaskGroup taskGroup = taskGroupService.getById(formData.getJobGroup());
        if (taskGroup == null) {
            throw new BusinessException(I18nUtil.getString("system_please_choose")+I18nUtil.getString("jobinfo_field_jobgroup"));
        }

        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(formData.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            throw new BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (formData.getScheduleConf()==null || !CronExpression.isValidExpression(formData.getScheduleConf())) {
                throw new BusinessException("Cron"+I18nUtil.getString("system_unvalid"));
            }
        }else if(scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE){
            if (formData.getScheduleConf() == null) {
                throw new BusinessException(I18nUtil.getString("schedule_type"));
            }
            try {
                int fixSecond = Integer.parseInt(formData.getScheduleConf());
                if (fixSecond < 1) {
                    throw new BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
                }
            } catch (Exception e) {
                throw new BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
            }
        }

        // valid job
        if (GlueTypeEnum.match(formData.getGlueType()) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_gluetype")+I18nUtil.getString("system_unvalid"));
        }
        if (GlueTypeEnum.BEAN==GlueTypeEnum.match(formData.getGlueType()) && (formData.getExecutorHandler()==null || formData.getExecutorHandler().trim().length()==0) ) {
            throw new BusinessException(I18nUtil.getString("system_please_input")+"JobHandler");
        }
        // 》fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL==GlueTypeEnum.match(formData.getGlueType()) && formData.getGlueSource()!=null) {
            formData.setGlueSource(formData.getGlueSource().replaceAll("\r", ""));
        }

        if (ExecutorRouteStrategyEnum.match(formData.getExecutorRouteStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorRouteStrategy")+I18nUtil.getString("system_unvalid"));

        }
        if (MisfireStrategyEnum.match(formData.getMisfireStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("misfire_strategy")+I18nUtil.getString("system_unvalid"));

        }
        if (ExecutorBlockStrategyEnum.match(formData.getExecutorBlockStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorBlockStrategy")+I18nUtil.getString("system_unvalid"));
        }

        if (formData.getChildJobid()!=null && formData.getChildJobid().trim().length()>0) {
            String[] childJobIds = formData.getChildJobid().split(",");
            for (String childJobIdItem: childJobIds) {
                if (childJobIdItem!=null && childJobIdItem.trim().length()>0 && isNumeric(childJobIdItem)) {
                    TaskInfo childJobInfo = this.getById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo==null) {
                        throw new BusinessException( MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_not_found")), childJobIdItem));

                    }
                } else {
                    throw   new BusinessException(
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item:childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length()-1);

            formData.setChildJobid(temp);
        }
        formData.setGlueUpdatetime(LocalDateTime.now());
        TaskInfo taskInfo = taskInfoConverter.toEntity(formData);
        return   this.save(taskInfo);
    }

    private boolean isNumeric(String str){
        try {
            int result = Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 更新task_info
     *
     * @param id   task_infoID
     * @param formData task_info表单对象
     * @return
     */
    @Override
    public boolean updateTaskInfo(Long id,TaskInfoForm formData) {
        // valid trigger
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(formData.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            throw new BusinessException (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (formData.getScheduleConf()==null || !CronExpression.isValidExpression(formData.getScheduleConf())) {
                throw new  BusinessException("Cron"+I18nUtil.getString("system_unvalid") );
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE /*|| scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
            if (formData.getScheduleConf() == null) {
                throw new BusinessException (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
            }
            try {
                int fixSecond = Integer.parseInt(formData.getScheduleConf());
                if (fixSecond < 1) {
                    throw new  BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
                }
            } catch (Exception e) {
                throw new BusinessException (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
            }
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(formData.getExecutorRouteStrategy(), null) == null) {
            throw new  BusinessException(I18nUtil.getString("jobinfo_field_executorRouteStrategy")+I18nUtil.getString("system_unvalid")) ;
        }
        if (MisfireStrategyEnum.match(formData.getMisfireStrategy(), null) == null) {
            throw new  BusinessException(I18nUtil.getString("misfire_strategy")+I18nUtil.getString("system_unvalid")) ;
        }
        if (ExecutorBlockStrategyEnum.match(formData.getExecutorBlockStrategy(), null) == null) {
            throw new BusinessException (I18nUtil.getString("jobinfo_field_executorBlockStrategy")+I18nUtil.getString("system_unvalid")) ;
        }

        // 》ChildJobId valid
        if (formData.getChildJobid()!=null && formData.getChildJobid().trim().length()>0) {
            String[] childJobIds = formData.getChildJobid().split(",");
            for (String childJobIdItem: childJobIds) {
                if (childJobIdItem!=null && childJobIdItem.trim().length()>0 && isNumeric(childJobIdItem)) {
                    TaskInfo childJobInfo = this.getById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo==null) {
                        throw new BusinessException(
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    throw new BusinessException(
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item:childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length()-1);

            formData.setChildJobid(temp);
        }

        // group valid
        TaskGroup jobGroup = taskGroupService.getById(formData.getJobGroup());
        if (jobGroup == null) {
            throw new BusinessException (I18nUtil.getString("jobinfo_field_jobgroup")+I18nUtil.getString("system_unvalid")) ;
        }

        // stage job info
        TaskInfo existsJobInfo = this.getById(id);
        if (existsJobInfo == null) {
            throw new BusinessException (I18nUtil.getString("jobinfo_field_id")+I18nUtil.getString("system_not_found")) ;
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = existsJobInfo.getTriggerNextTime();
        boolean scheduleDataNotChanged = formData.getScheduleType().equals(existsJobInfo.getScheduleType()) && formData.getScheduleConf().equals(existsJobInfo.getScheduleConf());
        if (existsJobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
            try {
                existsJobInfo.setScheduleConf(formData.getScheduleConf());
                Date nextValidTime = JobScheduleHelper.generateNextValidTime(existsJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    throw new  BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new  BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
            }
        }
        existsJobInfo.setJobGroup(formData.getJobGroup());
        existsJobInfo.setJobDesc(formData.getJobDesc());
        existsJobInfo.setAuthor(formData.getAuthor());
        existsJobInfo.setAlarmEmail(formData.getAlarmEmail());
        existsJobInfo.setScheduleType(formData.getScheduleType());
        existsJobInfo.setScheduleConf(formData.getScheduleConf());
        existsJobInfo.setMisfireStrategy(formData.getMisfireStrategy());
        existsJobInfo.setExecutorRouteStrategy(formData.getExecutorRouteStrategy());
        existsJobInfo.setExecutorHandler(formData.getExecutorHandler());
        existsJobInfo.setExecutorParam(formData.getExecutorParam());
        existsJobInfo.setExecutorBlockStrategy(formData.getExecutorBlockStrategy());
        existsJobInfo.setExecutorTimeout(formData.getExecutorTimeout());
        existsJobInfo.setExecutorFailRetryCount(formData.getExecutorFailRetryCount());
        existsJobInfo.setChildJobid(formData.getChildJobid());
        existsJobInfo.setTriggerNextTime(nextTriggerTime);
        return this.updateById(existsJobInfo);
    }
    
    /**
     * 删除task_info
     *
     * @param ids task_infoID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskInfos(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_info数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public boolean triggerJob(TaskInfoTriggerDto taskInfoTriggerDto) {

//        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(jobId);
//        if (xxlJobInfo == null) {
//            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
//        }
        // force cover job param
        if (taskInfoTriggerDto.getExecutorParam() == null) {
            taskInfoTriggerDto.setExecutorParam("");
        }

        JobTriggerPoolHelper.trigger(taskInfoTriggerDto.getId().intValue(), TriggerTypeEnum.MANUAL, -1, null, taskInfoTriggerDto.getExecutorParam(), taskInfoTriggerDto.getAddressList());
        return true;
    }

    @Override
    public boolean startTask(Long id) {
        TaskInfo xxlJobInfo = this.getById(id);

        // valid
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(xxlJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
           throw new BusinessException (I18nUtil.getString("schedule_type_none_limit_start"));
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = 0;
        try {
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(xxlJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                throw new BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid"));
        }

        xxlJobInfo.setTriggerStatus(1);
        xxlJobInfo.setTriggerLastTime(0L);
        xxlJobInfo.setTriggerNextTime(nextTriggerTime);
        return  this.updateById(xxlJobInfo);
    }

    @Override
    public boolean stopTask(Long id) {
        TaskInfo xxlJobInfo = this.getById(id);

        xxlJobInfo.setTriggerStatus(0);
        xxlJobInfo.setTriggerLastTime(0L);
        xxlJobInfo.setTriggerNextTime(0L);
        return this.updateById(xxlJobInfo);
    }

    @Override
    public List<String> nextTriggerTime(String scheduleType, String scheduleConf) {
        TaskInfo paramXxlJobInfo = new TaskInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw new BusinessException(I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")+ e.getMessage());
        }
        return result;
    }

}
