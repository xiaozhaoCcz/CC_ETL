package com.cc.job.admin.core.executor;

import com.cc.job.admin.common.constant.JobConstants;
import com.cc.job.admin.common.exception.ExecutorException;
import com.cc.job.admin.common.exception.TriggerException;
import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.task.enums.ExecutorRouteStrategyEnum;
import com.cc.job.admin.task.trigger.XxlJobTrigger;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.util.IpUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

/**
 * 任务执行处理器
 * 
 * 负责任务的触发和执行逻辑
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Component
@RequiredArgsConstructor
public class JobExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionHandler.class);

    @Value("${server.port}")
    private int port;

    /**
     * 触发任务执行
     *
     * @param xxlJobContext 任务上下文
     * @param jobInfo       任务信息
     * @param randomId      随机ID
     */
    public void triggerJob(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId) {
        log.debug("[JobExecution] 开始触发任务 - jobId: {}, executorHandler: {}, randomId: {}",
                jobInfo.getId(), jobInfo.getExecutorHandler(), randomId);

        JobGroup group = XxlJobAdminConfig.getAdminConfig().getJobGroupMapper().selectById(jobInfo.getJobGroup());
        String ip = IpUtil.getIp();
        String adminAddress = String.format(JobConstants.AddressFormat.ADMIN_ADDRESS, ip, port);

        List<String> registryList = group.getRegistryList();
        if (registryList == null || registryList.isEmpty()) {
            log.error("[JobExecution] 执行器组未注册可用实例 - jobId: {}", jobInfo.getId());
            throw new ExecutorException(null, "执行器未注册，无法触发任务");
        }

        ExecutorRouteStrategyEnum routeStrategyEnum = ExecutorRouteStrategyEnum.match(
                jobInfo.getExecutorRouteStrategy(), ExecutorRouteStrategyEnum.FIRST);

        // 分片广播模式
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == routeStrategyEnum) {
            triggerBroadcast(jobInfo, randomId, xxlJobContext, adminAddress, registryList);
            return;
        }

        // 单个执行器触发
        TriggerParam triggerParam = createTriggerParam(jobInfo, randomId, xxlJobContext, adminAddress, 0, 1);
        ReturnT<String> routeResult = routeStrategyEnum.getRouter().route(triggerParam, registryList);
        
        if (routeResult == null || routeResult.getCode() != ReturnT.SUCCESS_CODE || routeResult.getContent() == null) {
            String reason = routeResult != null ? routeResult.getMsg() : "未获取到可用执行器";
            log.error("[JobExecution] 触发路由失败 - jobId: {}, route: {}, 原因: {}", 
                    jobInfo.getId(), routeStrategyEnum, reason);
            throw new TriggerException(jobInfo.getId(), "触发路由失败: " + reason);
        }

        doTrigger(jobInfo, randomId, xxlJobContext, triggerParam, routeResult.getContent());
    }

    /**
     * 分片广播触发
     */
    private void triggerBroadcast(JobInfo jobInfo, String randomId, XxlJobContext xxlJobContext,
                                  String adminAddress, List<String> registryList) {
        log.info("[JobExecution] 分片广播触发 - jobId: {}, 分片数: {}", jobInfo.getId(), registryList.size());
        
        for (int i = 0; i < registryList.size(); i++) {
            TriggerParam broadcastParam = createTriggerParam(
                    jobInfo, randomId, xxlJobContext, adminAddress, i, registryList.size());
            doTrigger(jobInfo, randomId, xxlJobContext, broadcastParam, registryList.get(i));
        }
    }

    /**
     * 创建触发参数
     */
    private TriggerParam createTriggerParam(JobInfo jobInfo, String randomId, XxlJobContext xxlJobContext,
                                           String adminAddress, int broadcastIndex, int broadcastTotal) {
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId().intValue());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(randomId);
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(-1);
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        
        if (jobInfo.getGlueUpdatetime() != null) {
            triggerParam.setGlueUpdatetime(
                    jobInfo.getGlueUpdatetime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        
        triggerParam.setBroadcastIndex(broadcastIndex);
        triggerParam.setBroadcastTotal(broadcastTotal);
        triggerParam.setReqBody(jobInfo.getReqBody());
        triggerParam.setReqHeader(jobInfo.getReqHeader());
        triggerParam.setReqType(jobInfo.getReqType());
        triggerParam.setReqUrl(jobInfo.getReqUrl());
        triggerParam.setXxlJobContext(xxlJobContext);
        triggerParam.setAddress(adminAddress);
        
        return triggerParam;
    }

    /**
     * 执行触发
     */
    private void doTrigger(JobInfo jobInfo, String randomId, XxlJobContext xxlJobContext,
                          TriggerParam triggerParam, String address) {
        log.debug("[JobExecution] 发送任务到执行器 - jobId: {}, address: {}", jobInfo.getId(), address);

        ReturnT<String> returnT = XxlJobTrigger.runExecutor(triggerParam, address);
        
        if (returnT.getCode() != ReturnT.SUCCESS_CODE) {
            log.error("[JobExecution] 任务触发失败 - jobId: {}, error: {}", jobInfo.getId(), returnT.getMsg());
            XxlJobHelper.log(xxlJobContext,
                    "========================================= 任务触发失败 =========================================");
            XxlJobHelper.log(xxlJobContext, "任务ID: {}, 错误信息: {}", jobInfo.getId(), returnT.getMsg());
            
            // 如果不是忽略策略，抛出异常
            if (!JobConstants.ExecutionResult.DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                throw new TriggerException(jobInfo.getId(), returnT.getMsg());
            }
            return;
        }

        log.debug("[JobExecution] 任务触发成功 - jobId: {}", jobInfo.getId());
        XxlJobHelper.log(xxlJobContext, "任务触发成功 - 任务ID: {}", jobInfo.getId());
    }

    /**
     * 暂停任务执行
     *
     * @param jobInfo 任务信息
     */
    public void pauseJobIfNeeded(JobInfo jobInfo) {
        boolean isPaused = jobInfo.getIsPause() == JobConstants.PauseStatus.PAUSED;
        
        if (!isPaused) {
            return;
        }

        log.info("[JobExecution] 任务处于暂停状态，等待恢复 - jobId: {}, jobDesc: {}",
                jobInfo.getId(), jobInfo.getJobDesc());

        long timeout = jobInfo.getExecutorTimeout() > 0 
                ? jobInfo.getExecutorTimeout() * 1000 
                : JobConstants.DEFAULT_PAUSE_TIMEOUT;
        
        long startTime = System.currentTimeMillis();
        
        while (isPaused) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeout) {
                log.warn("[JobExecution] 任务暂停等待超时 - jobId: {}, 等待时长: {}ms", 
                        jobInfo.getId(), elapsed);
                break;
            }
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("[JobExecution] 任务暂停等待被中断 - jobId: {}", jobInfo.getId(), e);
                Thread.currentThread().interrupt();
                throw new TriggerException(jobInfo.getId(), "任务暂停等待被中断", e);
            }
            
            // 重新检查暂停状态（需要从数据库重新获取）
            // 这里暂时保持原逻辑，实际应该注入 Service 重新查询
            isPaused = jobInfo.getIsPause() == JobConstants.PauseStatus.PAUSED;
        }
        
        if (!isPaused) {
            log.info("[JobExecution] 任务恢复执行 - jobId: {}, jobDesc: {}", 
                    jobInfo.getId(), jobInfo.getJobDesc());
        }
    }
}

