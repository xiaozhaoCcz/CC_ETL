package com.cc.job.admin.core.validation;

import com.cc.job.admin.common.constant.JobConstants;
import com.cc.job.admin.common.exception.TriggerException;
import com.cc.job.admin.core.schedule.JobScheduleService;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.form.JobInfoForm;
import com.xxl.job.core.glue.GlueTypeEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 任务验证服务
 * 
 * 负责任务相关的业务验证逻辑
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Service
@RequiredArgsConstructor
public class JobValidationService {

    private static final Logger log = LoggerFactory.getLogger(JobValidationService.class);

    private final JobScheduleService jobScheduleService;

    /**
     * 验证任务表单
     *
     * @param form 任务表单
     */
    public void validateJobForm(JobInfoForm form) {
        log.debug("[JobValidation] 开始验证任务表单 - jobDesc: {}", form.getJobDesc());

        // 基础验证
        validateBasicInfo(form);

        // 调度配置验证
        validateScheduleConfig(form);

        // 执行器配置验证
        validateExecutorConfig(form);

        // GLUE代码验证
        validateGlueConfig(form);

        log.debug("[JobValidation] 任务表单验证通过");
    }

    /**
     * 验证基础信息
     */
    private void validateBasicInfo(JobInfoForm form) {
        if (form.getJobGroup() == null || form.getJobGroup() <= 0) {
            throw new BusinessException("执行器不能为空");
        }

        if (StringUtils.isBlank(form.getJobDesc())) {
            throw new BusinessException("任务描述不能为空");
        }

        if (form.getJobDesc().length() > 200) {
            throw new BusinessException("任务描述长度不能超过200个字符");
        }

        if (form.getAuthor() != null && form.getAuthor().length() > 50) {
            throw new BusinessException("负责人长度不能超过50个字符");
        }
    }

    /**
     * 验证调度配置
     */
    private void validateScheduleConfig(JobInfoForm form) {
        if (StringUtils.isBlank(form.getScheduleType())) {
            throw new BusinessException("调度类型不能为空");
        }

        if (StringUtils.isBlank(form.getScheduleConf())) {
            throw new BusinessException("调度配置不能为空");
        }

        // CRON表达式验证
        if ("CRON".equals(form.getScheduleType())) {
            if (!jobScheduleService.validateCronExpression(form.getScheduleConf())) {
                throw new BusinessException("CRON表达式格式错误");
            }
        }

        // 固定速率验证
        if ("FIX_RATE".equals(form.getScheduleType())) {
            validateFixRate(form.getScheduleConf());
        }

        // 固定延迟验证
        if ("FIX_DELAY".equals(form.getScheduleType())) {
            validateFixDelay(form.getScheduleConf());
        }
    }

    /**
     * 验证固定速率配置
     */
    private void validateFixRate(String scheduleConf) {
        try {
            int rate = Integer.parseInt(scheduleConf);
            if (rate < 1) {
                throw new BusinessException("固定速率必须大于0秒");
            }
            if (rate > 86400) { // 最大1天
                throw new BusinessException("固定速率不能超过86400秒(1天)");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("固定速率必须是数字");
        }
    }

    /**
     * 验证固定延迟配置
     */
    private void validateFixDelay(String scheduleConf) {
        try {
            int delay = Integer.parseInt(scheduleConf);
            if (delay < 1) {
                throw new BusinessException("固定延迟必须大于0秒");
            }
            if (delay > 86400) { // 最大1天
                throw new BusinessException("固定延迟不能超过86400秒(1天)");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("固定延迟必须是数字");
        }
    }

    /**
     * 验证执行器配置
     */
    private void validateExecutorConfig(JobInfoForm form) {
        // 路由策略验证
        if (StringUtils.isBlank(form.getExecutorRouteStrategy())) {
            throw new BusinessException("路由策略不能为空");
        }

        // 阻塞处理策略验证
        if (StringUtils.isBlank(form.getExecutorBlockStrategy())) {
            throw new BusinessException("阻塞处理策略不能为空");
        }

        // 超时时间验证
        if (form.getExecutorTimeout() != null && form.getExecutorTimeout() < 0) {
            throw new BusinessException("超时时间不能为负数");
        }

        // 失败重试次数验证
        if (form.getExecutorFailRetryCount() != null) {
            if (form.getExecutorFailRetryCount() < 0) {
                throw new BusinessException("失败重试次数不能为负数");
            }
            if (form.getExecutorFailRetryCount() > 10) {
                throw new BusinessException("失败重试次数不能超过10次");
            }
        }
    }

    /**
     * 验证GLUE配置
     */
    private void validateGlueConfig(JobInfoForm form) {
        if (StringUtils.isBlank(form.getGlueType())) {
            throw new BusinessException("运行模式不能为空");
        }

        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(form.getGlueType());
        if (glueTypeEnum == null) {
            throw new BusinessException("运行模式配置错误");
        }

        // BEAN模式需要JobHandler
        if (GlueTypeEnum.BEAN == glueTypeEnum) {
            if (StringUtils.isBlank(form.getExecutorHandler())) {
                throw new BusinessException("BEAN模式下，JobHandler不能为空");
            }
            if (form.getExecutorHandler().length() > 100) {
                throw new BusinessException("JobHandler长度不能超过100个字符");
            }
        }

        // GLUE模式需要源码
        if (glueTypeEnum != GlueTypeEnum.BEAN) {
            if (StringUtils.isBlank(form.getGlueSource())) {
                throw new BusinessException("GLUE模式下，源码不能为空");
            }
        }
    }

    /**
     * 验证执行器组
     *
     * @param group 执行器组
     */
    public void validateJobGroup(JobGroup group) {
        if (group == null) {
            throw new BusinessException("执行器组不存在");
        }

        if (group.getRegistryList() == null || group.getRegistryList().isEmpty()) {
            throw new BusinessException("执行器组未注册可用实例");
        }
    }

    /**
     * 验证任务是否可以启动
     *
     * @param jobInfo 任务信息
     */
    public void validateJobCanStart(JobInfo jobInfo) {
        if (jobInfo.getTriggerStatus() == JobConstants.JobStatus.RUNNING) {
            throw new TriggerException(jobInfo.getId(), "任务已经在运行中");
        }

        validateScheduleConfig(jobInfo);
    }

    /**
     * 验证任务调度配置
     */
    private void validateScheduleConfig(JobInfo jobInfo) {
        if (StringUtils.isBlank(jobInfo.getScheduleType())) {
            throw new TriggerException(jobInfo.getId(), "调度类型不能为空");
        }

        if (StringUtils.isBlank(jobInfo.getScheduleConf())) {
            throw new TriggerException(jobInfo.getId(), "调度配置不能为空");
        }
    }

    /**
     * 验证任务是否可以停止
     *
     * @param jobInfo 任务信息
     */
    public void validateJobCanStop(JobInfo jobInfo) {
        if (jobInfo.getTriggerStatus() == JobConstants.JobStatus.STOPPED) {
            throw new TriggerException(jobInfo.getId(), "任务已经停止");
        }
    }

    /**
     * 验证任务是否可以删除
     *
     * @param jobInfo 任务信息
     */
    public void validateJobCanDelete(JobInfo jobInfo) {
        if (jobInfo.getTriggerStatus() == JobConstants.JobStatus.RUNNING) {
            throw new BusinessException("任务运行中，无法删除，请先停止任务");
        }
    }
}

