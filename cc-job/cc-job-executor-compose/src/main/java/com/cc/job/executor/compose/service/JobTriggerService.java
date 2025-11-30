package com.cc.job.executor.compose.service;

import cn.hutool.core.lang.Pair;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.engine.constant.JobConstant;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务触发服务
 * 
 * <p>负责触发实际的任务执行，调用执行器的 run 方法
 * 
 * @author xiaozhao
 */
@Service
public class JobTriggerService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobTriggerService.class);
    
    private final AdminApiClient adminApiClient;
    
    @Value("${server.port:8500}")
    private int port;
    
    /**
     * 执行器客户端缓存
     */
    private static final ConcurrentMap<String, ExecutorBiz> EXECUTOR_BIZ_CACHE = new ConcurrentHashMap<>();
    
    public JobTriggerService(AdminApiClient adminApiClient) {
        this.adminApiClient = adminApiClient;
    }
    
    /**
     * 触发任务执行
     * 
     * @param xxlJobContext 任务上下文
     * @param jobInfo 任务信息
     * @param randomId 批次ID
     * @return 是否触发成功
     */
    public boolean triggerJob(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId) {
        logger.debug("[JobTrigger] 开始触发任务 - jobId: {}, handler: {}, randomId: {}",
                jobInfo.getId(), jobInfo.getExecutorHandler(), randomId);
        
        try {
            // 1. 获取执行器组信息
            JobGroup group = adminApiClient.getJobGroup(jobInfo.getJobGroup());
            if (group == null) {
                logger.error("[JobTrigger] 执行器组不存在 - jobGroupId: {}", jobInfo.getJobGroup());
                return false;
            }
            
            // 调试日志：验证 JobGroup 对象属性
            logger.info("[JobTrigger] 获取执行器组成功 - jobGroupId: {}, appName: {}, title: {}, addressType: {}, addressList: {}", 
                    jobInfo.getJobGroup(), group.getAppName(), group.getTitle(), 
                    group.getAddressType(), group.getAddressList());
            
            // 2. 获取执行器地址列表
            List<String> registryList = group.getRegistryList();
            if (registryList == null || registryList.isEmpty()) {
                logger.error("[JobTrigger] 当前执行器组未注册可用实例 - jobId: {}", jobInfo.getId());
                XxlJobHelper.log(xxlJobContext, "执行器未注册，无法触发任务");
                return false;
            }
            
            // 3. 构建 Compose 执行器地址（用于子任务回调）
            String ip = IpUtil.getIp();
            String composeAddress = "http://" + ip + ":" + port + "/";
            
            logger.info("[JobTrigger] Compose 执行器地址: {}", composeAddress);
            
            // 4. 获取路由策略
            String routeStrategy = jobInfo.getExecutorRouteStrategy();
            if (routeStrategy == null || routeStrategy.isEmpty()) {
                routeStrategy = "FIRST";
            }
            
            // 5. 根据路由策略触发任务
            if ("SHARDING_BROADCAST".equals(routeStrategy)) {
                // 分片广播：向所有执行器发送任务
                return triggerShardingBroadcast(xxlJobContext, jobInfo, randomId, composeAddress, registryList);
            } else {
                // 普通路由：选择一个执行器
                return triggerNormal(xxlJobContext, jobInfo, randomId, composeAddress, registryList, routeStrategy);
            }
            
        } catch (Exception e) {
            logger.error("[JobTrigger] 触发任务失败 - jobId: {}", jobInfo.getId(), e);
            XxlJobHelper.log(xxlJobContext, "触发任务失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 分片广播触发
     */
    private boolean triggerShardingBroadcast(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId,
                                             String composeAddress, List<String> registryList) {
        logger.info("[JobTrigger] 分片广播触发 - jobId: {}, 执行器数量: {}", jobInfo.getId(), registryList.size());
        
        boolean allSuccess = true;
        for (int i = 0; i < registryList.size(); i++) {
            TriggerParam triggerParam = createTriggerParam(jobInfo, randomId, xxlJobContext, composeAddress, i, registryList.size());
            boolean success = doTrigger(xxlJobContext, jobInfo, randomId, triggerParam, registryList.get(i));
            if (!success) {
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }
    
    /**
     * 普通路由触发
     */
    private boolean triggerNormal(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId,
                                  String composeAddress, List<String> registryList, String routeStrategy) {
        logger.debug("[JobTrigger] 普通路由触发 - jobId: {}, 路由策略: {}", jobInfo.getId(), routeStrategy);
        
        // 构建触发参数
        TriggerParam triggerParam = createTriggerParam(jobInfo, randomId, xxlJobContext, composeAddress, 0, 1);
        
        // 路由选择执行器地址
        String address = selectAddress(triggerParam, registryList, routeStrategy);
        if (address == null) {
            logger.error("[JobTrigger] 路由失败，未找到可用执行器 - jobId: {}, 策略: {}", jobInfo.getId(), routeStrategy);
            XxlJobHelper.log(xxlJobContext, "路由失败，未找到可用执行器");
            return false;
        }
        
        // 触发任务
        return doTrigger(xxlJobContext, jobInfo, randomId, triggerParam, address);
    }
    
    /**
     * 创建触发参数
     */
    private TriggerParam createTriggerParam(JobInfo jobInfo, String randomId, XxlJobContext xxlJobContext,
                                           String composeAddress, int broadcastIndex, int broadcastTotal) {
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId().intValue());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(randomId);
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(-1);  // -1 表示这是任务组子任务
        triggerParam.setRandomId(randomId);  // 设置 randomId，用于回调时标识任务组
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        
        if (jobInfo.getGlueUpdatetime() != null) {
            triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        
        triggerParam.setBroadcastIndex(broadcastIndex);
        triggerParam.setBroadcastTotal(broadcastTotal);
        
        // 设置 HTTP 任务参数
        triggerParam.setReqBody(jobInfo.getReqBody());
        triggerParam.setReqHeader(jobInfo.getReqHeader());
        triggerParam.setReqType(jobInfo.getReqType());
        triggerParam.setReqUrl(jobInfo.getReqUrl());
        
        triggerParam.setXxlJobContext(xxlJobContext);
        // 关键：将 address 设置为 compose 执行器地址，这样子任务完成后会回调到 compose 执行器
        triggerParam.setAddress(composeAddress);
        
        logger.debug("[JobTrigger] 创建触发参数 - jobId: {}, randomId: {}, 回调地址: {}", 
                jobInfo.getId(), randomId, composeAddress);
        
        return triggerParam;
    }
    
    /**
     * 执行触发
     */
    private boolean doTrigger(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId,
                             TriggerParam triggerParam, String address) {
        logger.debug("[JobTrigger] 发送任务到执行器 - jobId: {}, 地址: {}", jobInfo.getId(), address);
        
        try {
            // 获取或创建执行器客户端
            ExecutorBiz executorBiz = getExecutorBiz(address);
            
            // 调用执行器的 run 方法
            ReturnT<String> returnT = executorBiz.run(triggerParam);
            
            if (returnT.getCode() != ReturnT.SUCCESS_CODE) {
                logger.error("[JobTrigger] 任务触发失败 - jobId: {}, 错误: {}", jobInfo.getId(), returnT.getMsg());
                XxlJobHelper.log(xxlJobContext, "========= 任务触发失败 =========");
                XxlJobHelper.log(xxlJobContext, "任务ID: {}, 错误信息: {}", jobInfo.getId(), returnT.getMsg());
                
                // 判断是否需要抛出异常
                if (!JobConstant.DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                    return false;
                }
            } else {
                logger.info("[JobTrigger] 任务触发成功 - jobId: {}", jobInfo.getId());
                XxlJobHelper.log(xxlJobContext, "任务触发成功 - 任务ID: {}", jobInfo.getId());
            }
            
            return returnT.getCode() == ReturnT.SUCCESS_CODE;
            
        } catch (Exception e) {
            logger.error("[JobTrigger] 任务触发异常 - jobId: {}, 地址: {}", jobInfo.getId(), address, e);
            XxlJobHelper.log(xxlJobContext, "任务触发异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 路由选择执行器地址
     */
    private String selectAddress(TriggerParam triggerParam, List<String> registryList, String routeStrategy) {
        if (registryList.isEmpty()) {
            return null;
        }
        
        // 简化实现：根据路由策略选择地址
        switch (routeStrategy.toUpperCase()) {
            case "FIRST":
                // 第一个
                return registryList.get(0);
                
            case "LAST":
                // 最后一个
                return registryList.get(registryList.size() - 1);
                
            case "ROUND":
                // 轮询（简化实现：使用任务ID取模）
                int index = Math.abs(triggerParam.getJobId() % registryList.size());
                return registryList.get(index);
                
            case "RANDOM":
                // 随机
                int randomIndex = (int) (Math.random() * registryList.size());
                return registryList.get(randomIndex);
                
            default:
                // 默认使用第一个
                logger.warn("[JobTrigger] 未知路由策略: {}, 使用 FIRST", routeStrategy);
                return registryList.get(0);
        }
    }
    
    /**
     * 获取或创建执行器客户端
     */
    private ExecutorBiz getExecutorBiz(String address) throws Exception {
        return EXECUTOR_BIZ_CACHE.computeIfAbsent(address, addr -> {
            try {
                // 创建执行器客户端
                // 注意：这里需要使用 XXL-Job 的 ExecutorBizClient
                return new com.xxl.job.core.biz.client.ExecutorBizClient(addr, null);
            } catch (Exception e) {
                logger.error("[JobTrigger] 创建执行器客户端失败 - 地址: {}", addr, e);
                throw new RuntimeException("创建执行器客户端失败", e);
            }
        });
    }
}
