package com.cc.job.admin.core.trigger;

import com.cc.job.admin.common.constant.JobConstants;
import com.cc.job.admin.common.exception.JobNotFoundException;
import com.cc.job.admin.common.exception.TriggerException;
import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.task.enums.TriggerTypeEnum;
import com.cc.job.admin.task.thread.JobTriggerPoolHelper;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLog;
import com.xxl.job.core.util.IpUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务触发服务
 * 
 * 负责任务触发的业务逻辑
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Service
@RequiredArgsConstructor
public class JobTriggerService {

    private static final Logger log = LoggerFactory.getLogger(JobTriggerService.class);

    @Value("${server.port}")
    private int port;

    /**
     * 触发任务执行一次
     *
     * @param triggerDto 触发参数
     * @return 触发结果消息
     */
    public String triggerJob(JobInfoTriggerDto triggerDto) {
        log.info("[JobTrigger] 手动触发任务 - jobId: {}, executorParam: {}", 
                triggerDto.getId(), triggerDto.getExecutorParam());

        JobInfo jobInfo = XxlJobAdminConfig.getAdminConfig()
                .getJobInfoMapper()
                .selectById(triggerDto.getId());

        if (jobInfo == null) {
            log.warn("[JobTrigger] 任务不存在 - jobId: {}", triggerDto.getId());
            throw new JobNotFoundException(triggerDto.getId());
        }

        // 验证任务状态
        if (jobInfo.getTriggerStatus() == JobConstants.JobStatus.STOPPED) {
            log.warn("[JobTrigger] 任务已停止，无法触发 - jobId: {}", triggerDto.getId());
            throw new TriggerException(triggerDto.getId(), "任务已停止，请先启动任务");
        }

        // 解析执行参数
        String executorParam = parseExecutorParam(triggerDto.getExecutorParam(), jobInfo.getExecutorParam());

        // 获取 admin 地址
        String ip = IpUtil.getIp();
        String adminAddress = String.format(JobConstants.AddressFormat.ADMIN_ADDRESS, ip, port);

        // 触发任务
        JobTriggerPoolHelper.trigger(
                jobInfo.getId().intValue(),
                TriggerTypeEnum.MANUAL,
                -1,
                null,
                executorParam,
                triggerDto.getAddressList(),
                1,
                adminAddress
        );

        log.info("[JobTrigger] 任务触发成功 - jobId: {}", triggerDto.getId());
        return "任务触发成功";
    }

    /**
     * 解析执行参数
     *
     * @param triggerParam 触发时传入的参数
     * @param defaultParam 任务默认参数
     * @return 最终执行参数
     */
    private String parseExecutorParam(String triggerParam, String defaultParam) {
        if (triggerParam != null && !triggerParam.trim().isEmpty()) {
            return triggerParam.trim();
        }
        return defaultParam != null ? defaultParam : "";
    }

    /**
     * 创建任务日志
     *
     * @param jobInfo          任务信息
     * @param triggerType      触发类型
     * @param executorParam    执行参数
     * @param executorSharding 分片参数
     * @return 任务日志
     */
    public JobLog createJobLog(JobInfo jobInfo, TriggerTypeEnum triggerType, 
                               String executorParam, String executorSharding) {
        JobLog jobLog = new JobLog();
        jobLog.setJobId(jobInfo.getId());
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setExecutorAddress(null);
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(executorParam);
        jobLog.setExecutorShardingParam(executorSharding);
        jobLog.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        jobLog.setTriggerTime(LocalDateTime.now());
        jobLog.setTriggerCode(0);
        jobLog.setTriggerMsg(null);
        jobLog.setHandleCode(0);
        jobLog.setHandleMsg(null);
        jobLog.setHandleTime(null);
        jobLog.setAlarmStatus(0);

        log.debug("[JobTrigger] 创建任务日志 - jobId: {}, triggerType: {}", 
                jobInfo.getId(), triggerType);

        return jobLog;
    }

    /**
     * 停止任务
     *
     * @param jobId 任务ID
     * @return 是否成功
     */
    public boolean stopJob(Long jobId) {
        log.info("[JobTrigger] 停止任务 - jobId: {}", jobId);

        JobInfo jobInfo = XxlJobAdminConfig.getAdminConfig()
                .getJobInfoMapper()
                .selectById(jobId);

        if (jobInfo == null) {
            throw new JobNotFoundException(jobId);
        }

        jobInfo.setTriggerStatus(JobConstants.JobStatus.STOPPED);
        jobInfo.setTriggerLastTime(0L);
        jobInfo.setTriggerNextTime(0L);
        jobInfo.setUpdateTime(LocalDateTime.now());

        int result = XxlJobAdminConfig.getAdminConfig()
                .getJobInfoMapper()
                .updateById(jobInfo);

        if (result > 0) {
            log.info("[JobTrigger] 任务停止成功 - jobId: {}", jobId);
            return true;
        }

        log.warn("[JobTrigger] 任务停止失败 - jobId: {}", jobId);
        return false;
    }

    /**
     * 启动任务
     *
     * @param jobId 任务ID
     * @return 是否成功
     */
    public boolean startJob(Long jobId) {
        log.info("[JobTrigger] 启动任务 - jobId: {}", jobId);

        JobInfo jobInfo = XxlJobAdminConfig.getAdminConfig()
                .getJobInfoMapper()
                .selectById(jobId);

        if (jobInfo == null) {
            throw new JobNotFoundException(jobId);
        }

        // 验证调度配置
        validateScheduleConfig(jobInfo);

        jobInfo.setTriggerStatus(JobConstants.JobStatus.RUNNING);
        jobInfo.setTriggerLastTime(0L);
        jobInfo.setTriggerNextTime(System.currentTimeMillis() + 5000); // 5秒后首次执行
        jobInfo.setUpdateTime(LocalDateTime.now());

        int result = XxlJobAdminConfig.getAdminConfig()
                .getJobInfoMapper()
                .updateById(jobInfo);

        if (result > 0) {
            log.info("[JobTrigger] 任务启动成功 - jobId: {}", jobId);
            return true;
        }

        log.warn("[JobTrigger] 任务启动失败 - jobId: {}", jobId);
        return false;
    }

    /**
     * 验证调度配置
     */
    private void validateScheduleConfig(JobInfo jobInfo) {
        if (jobInfo.getScheduleType() == null) {
            throw new TriggerException(jobInfo.getId(), "调度类型不能为空");
        }

        if ("CRON".equals(jobInfo.getScheduleType())) {
            if (jobInfo.getScheduleConf() == null || jobInfo.getScheduleConf().trim().isEmpty()) {
                throw new TriggerException(jobInfo.getId(), "CRON表达式不能为空");
            }
        }

        if ("FIX_RATE".equals(jobInfo.getScheduleType())) {
            try {
                int fixRate = Integer.parseInt(jobInfo.getScheduleConf());
                if (fixRate < 1) {
                    throw new TriggerException(jobInfo.getId(), "固定速率必须大于0");
                }
            } catch (NumberFormatException e) {
                throw new TriggerException(jobInfo.getId(), "固定速率格式错误", e);
            }
        }
    }
}

