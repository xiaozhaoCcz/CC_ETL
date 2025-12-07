package com.cc.job.admin.task.trigger;

import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.task.enums.ExecutorRouteStrategyEnum;
import com.cc.job.admin.task.enums.TriggerTypeEnum;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.admin.task.scheduler.XxlJobScheduler;
import com.cc.job.admin.task.utils.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * xxl-job trigger
 * Created by xuxueli on 17/7/13.
 */
public class XxlJobTrigger {
    private static Logger logger = LoggerFactory.getLogger(XxlJobTrigger.class);

    /**
     * trigger job
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount        >=0: use this param
     *                              <0: use param from job info config
     * @param executorShardingParam
     * @param executorParam         null: use job param
     *                              not null: cover job param
     * @param addressList           null: use executor addressList
     *                              not null: cover
     */
    public static void trigger(Long jobId,
                                              TriggerTypeEnum triggerType,
                                              int failRetryCount,
                                              String executorShardingParam,
                                              String executorParam,
                                              String addressList,
                                              long logId) {

        // load data
        JobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().selectById(jobId);
        if (jobInfo == null) {
            logger.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
            return ;
        }
        if (executorParam != null) {
            jobInfo.setExecutorParam(executorParam);
        }
        int finalFailRetryCount = failRetryCount >= 0 ? failRetryCount : jobInfo.getExecutorFailRetryCount();
        JobGroup group = XxlJobAdminConfig.getAdminConfig().getJobGroupMapper().selectById(jobInfo.getJobGroup());

        // cover addressList
        if (addressList != null && addressList.trim().length() > 0) {
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
        }

        // sharding param
        int[] shardingParam = null;
        if (executorShardingParam != null) {
            String[] shardingArr = executorShardingParam.split("/");
            if (shardingArr.length == 2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
                shardingParam = new int[2];
                shardingParam[0] = Integer.valueOf(shardingArr[0]);
                shardingParam[1] = Integer.valueOf(shardingArr[1]);
            }
        }
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null)
                && group.getRegistryList() != null && !group.getRegistryList().isEmpty()
                && shardingParam == null) {
            for (int i = 0; i < group.getRegistryList().size(); i++) {
                processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryList().size(), logId);
            }
        } else {
            if (shardingParam == null) {
                shardingParam = new int[]{0, 1};
            }
            processTrigger(group, jobInfo, finalFailRetryCount, triggerType, shardingParam[0], shardingParam[1], logId);
        }
    }

    private static boolean isNumeric(String str) {
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param group               job group, registry list may be empty
     * @param jobInfo
     * @param finalFailRetryCount
     * @param triggerType
     * @param index               sharding index
     * @param total               sharding index
     */
    private static void processTrigger(JobGroup group, JobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total, long logId) {

        // param
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);  // block strategy
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);    // route strategy
        String shardingParam = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) ? String.valueOf(index).concat("/").concat(String.valueOf(total)) : null;

        // 1、save log-id
        // ⭐ 检查是否已经存在 JobLog（避免重复创建）
        // 查询最近5秒内创建的 JobLog，如果存在且未完成（executorAddress为null）则使用它
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime fiveSecondsAgo = now.minusSeconds(5);
//        JobLog jobLog = XxlJobAdminConfig.getAdminConfig().getJobLogMapper().selectOne(
//            new LambdaQueryWrapper<JobLog>()
//                .eq(JobLog::getJobId, jobInfo.getId())
//                .eq(JobLog::getJobGroup, jobInfo.getJobGroup())
//                .eq(JobLog::getTriggerCode, 0)  // 只查询未完成的记录
//                .eq(JobLog::getHandleCode, 0)   // 只查询未完成的记录
//                .isNull(JobLog::getExecutorAddress)  // executorAddress为null表示未完成
//                .ge(JobLog::getTriggerTime, fiveSecondsAgo)
//                .le(JobLog::getTriggerTime, now)
//                .orderByDesc(JobLog::getTriggerTime)
//                .last("LIMIT 1")
//        );

        JobLog jobLog;
        if (logId <= 0) {
            // logId <= 0 表示需要创建新记录
            jobLog = new JobLog();
            jobLog.setJobGroup(jobInfo.getJobGroup());
            jobLog.setJobId(jobInfo.getId());
            jobLog.setTriggerTime(LocalDateTime.now());
            jobLog.setTriggerCode(0);
            jobLog.setHandleCode(0);
            XxlJobAdminConfig.getAdminConfig().getJobLogMapper().insert(jobLog);
            logger.debug(">>>>>>>>>>> xxl-job trigger start, jobId:{}, logId:{} (new)", jobInfo.getId(), jobLog.getId());
        } else {
            // logId > 0 表示必须使用已存在的记录，不能创建新的
            jobLog = XxlJobAdminConfig.getAdminConfig().getJobLogMapper().selectById(logId);
            if (jobLog == null) {
                // ⚠️ 如果 logId 对应的记录不存在，说明数据不一致，抛出异常而不是创建新记录
                String errorMsg = String.format("JobLog not found for logId: %d, jobId: %d. This should not happen!", logId, jobInfo.getId());
                logger.error(">>>>>>>>>>> xxl-job trigger fail, {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            // 验证 jobId 和 jobGroup 是否匹配
            if (!jobLog.getJobId().equals(jobInfo.getId()) || !jobLog.getJobGroup().equals(jobInfo.getJobGroup())) {
                String errorMsg = String.format("JobLog mismatch: logId=%d, expected jobId=%d jobGroup=%d, but got jobId=%d jobGroup=%d", 
                        logId, jobInfo.getId(), jobInfo.getJobGroup(), jobLog.getJobId(), jobLog.getJobGroup());
                logger.error(">>>>>>>>>>> xxl-job trigger fail, {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            logger.debug(">>>>>>>>>>> xxl-job trigger start, jobId:{}, logId:{} (existing)", jobInfo.getId(), jobLog.getId());
        }

        // 2、init trigger-param
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId().intValue());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(jobLog.getId());
        triggerParam.setLogDateTime(jobLog.getTriggerTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        triggerParam.setGlueUpdateTime(jobInfo.getGlueUpdateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        triggerParam.setBroadcastIndex(index);
        triggerParam.setBroadcastTotal(total);
        //设置请求信息
        triggerParam.setReqBody(jobInfo.getReqBody());
        triggerParam.setReqHeader(jobInfo.getReqHeader());
        triggerParam.setReqType(jobInfo.getReqType());
        triggerParam.setReqUrl(jobInfo.getReqUrl());


        // 3、init address
        String address = null;
        ReturnT<String> routeAddressResult = null;
        
        // 获取执行器地址列表（确保获取最新信息）
        List<String> registryList = group.getRegistryList();
        if (registryList != null && !registryList.isEmpty()) {
            if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                if (index < registryList.size()) {
                    address = registryList.get(index);
                } else {
                    address = registryList.get(0);
                }
            } else {
                routeAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, registryList);
                if (routeAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
                    address = routeAddressResult.getContent();
                }
            }
        } else {
            // 执行器组没有可用的执行器
            routeAddressResult = new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobconf_trigger_address_empty"));
            logger.warn(">>>>>>>>>>> xxl-job trigger fail, executor address is empty, jobId:{}, jobGroup:{}, appName:{}", 
                    jobInfo.getId(), jobInfo.getJobGroup(), group.getAppName());
        }

        // ⭐ 对于分片广播，只在第一次调用时（index == 0）更新 executorAddress
        // 对于普通路由，始终更新 executorAddress
        // 这样可以避免分片广播时多次更新同一条记录，同时确保普通路由时能正确记录执行器地址
        boolean shouldUpdateExecutorAddress = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST != executorRouteStrategyEnum) || (index == 0);
        if (shouldUpdateExecutorAddress) {
            // 如果当前 executorAddress 为空，或者不是分片广播，则更新
            if (jobLog.getExecutorAddress() == null || ExecutorRouteStrategyEnum.SHARDING_BROADCAST != executorRouteStrategyEnum) {
                jobLog.setExecutorAddress(address);  // address 可能为 null，表示没有可用的执行器
                // 先更新执行器地址，后续会再次更新完整的触发信息
                XxlJobAdminConfig.getAdminConfig().getJobLogMapper().updateById(jobLog);
            }
        }

        // 4、trigger remote executor
        ReturnT<String> triggerResult = null;
        if (address != null) {
            triggerResult = runExecutor(triggerParam, address);
        } else {
            triggerResult = new ReturnT<String>(ReturnT.FAIL_CODE, null);
        }

        // 返回jobId,执行日志专区任务
//        if (triggerOne == 1 && jobInfo.getJobType() == 2 && "N".equalsIgnoreCase(jobInfo.getIsNode())) {
//            String key = JobGroupXxlJob.setExecuteJobId(jobInfo.getId(), jobInfo.getExecutorParam());
//            String value = String.valueOf(jobLog.getId());
//            XxlJobRemotingUtil.postBody(adminAddress + "api/jobLogId", "", 10, new Pair<>(key, value), Pair.class);
//        }

        // 5、collection trigger info
        StringBuffer triggerMsgSb = new StringBuffer();
        triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress")).append("：").append(IpUtil.getIp());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype")).append("：")
                .append((group.getAddressType() == 0) ? I18nUtil.getString("jobgroup_field_addressType_0") : I18nUtil.getString("jobgroup_field_addressType_1"));
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress")).append("：").append(group.getRegistryList());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy")).append("：").append(executorRouteStrategyEnum.getTitle());
        if (shardingParam != null) {
            triggerMsgSb.append("(" + shardingParam + ")");
        }
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy")).append("：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：").append(jobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：").append(finalFailRetryCount);

        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_run") + "<<<<<<<<<<< </span><br>")
                .append((routeAddressResult != null && routeAddressResult.getMsg() != null) ? routeAddressResult.getMsg() + "<br><br>" : "").append(triggerResult.getMsg() != null ? triggerResult.getMsg() : "");

        // executorHandler 始终设置，因为它是任务配置的一部分
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        jobLog.setExecutorShardingParam(shardingParam);
        jobLog.setExecutorFailRetryCount(finalFailRetryCount);
        //jobLog.setTriggerTime();
        jobLog.setTriggerCode(triggerResult.getCode());
        jobLog.setTriggerMsg(triggerMsgSb.toString());
        XxlJobAdminConfig.getAdminConfig().getJobLogMapper().updateById(jobLog);

        //停止任务组
        if(triggerResult.getCode()!=ReturnT.SUCCESS_CODE){
            //关闭当前任务组
            XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().stopJobCompose(jobInfo.getId());
            throw new RuntimeException(triggerResult.getMsg());
        }
        logger.debug(">>>>>>>>>>> xxl-job trigger end, jobId:{}", jobLog.getId());
    }

    /**
     * run executor
     *
     * @param triggerParam
     * @param address
     * @return
     */
    public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address) {
        ReturnT<String> runResult = null;
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> xxl-job trigger error, please check if the executor[{}] is running.", address, e);
            runResult = new ReturnT<String>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
        }

        StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMsg());

        runResult.setMsg(runResultSB.toString());
        return runResult;
    }

}
