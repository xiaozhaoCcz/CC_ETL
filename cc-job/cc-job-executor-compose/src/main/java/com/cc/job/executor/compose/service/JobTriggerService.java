package com.cc.job.executor.compose.service;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.executor.compose.core.resolver.ParameterResolver;
import com.cc.job.executor.compose.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobInfo;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.ExecutorBizClient;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Random;
import java.util.LinkedHashMap;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
    private final ParameterResolver parameterResolver;
    
    @Value("${server.port:8500}")
    private int port;
    
    /**
     * 执行器客户端缓存
     */
    private static final ConcurrentMap<String, ExecutorBiz> EXECUTOR_BIZ_CACHE = new ConcurrentHashMap<>();
    
    /**
     * LFU策略缓存: jobId -> (address -> count)
     */
    private static final ConcurrentMap<Integer, HashMap<String, Integer>> JOB_LFU_MAP = new ConcurrentHashMap<>();
    
    /**
     * LRU策略缓存: jobId -> LinkedHashMap(address -> address)
     */
    private static final ConcurrentMap<Integer, LinkedHashMap<String, String>> JOB_LRU_MAP = new ConcurrentHashMap<>();
    
    /**
     * 轮询策略计数器
     */
    private static final ConcurrentMap<Integer, Integer> ROUND_COUNT_MAP = new ConcurrentHashMap<>();
    
    /**
     * 缓存有效时间（24小时）
     */
    private static long CACHE_VALID_TIME = 0;
    
    /**
     * 一致性哈希虚拟节点数
     */
    private static final int VIRTUAL_NODE_NUM = 100;
    
    public JobTriggerService(AdminApiClient adminApiClient, ParameterResolver parameterResolver) {
        this.adminApiClient = adminApiClient;
        this.parameterResolver = parameterResolver;
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
        return triggerJob(xxlJobContext, jobInfo, randomId, null);
    }
    
    /**
     * 触发任务执行（带执行上下文）
     * 
     * @param xxlJobContext 任务上下文
     * @param jobInfo 任务信息
     * @param randomId 批次ID
     * @param context 执行上下文（用于参数解析）
     * @return 是否触发成功
     */
    public boolean triggerJob(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId, ExecutionContext context) {
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
                return triggerShardingBroadcast(xxlJobContext, jobInfo, randomId, composeAddress, registryList, context);
            } else {
                // 普通路由：选择一个执行器
                return triggerNormal(xxlJobContext, jobInfo, randomId, composeAddress, registryList, routeStrategy, context);
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
                                             String composeAddress, List<String> registryList, ExecutionContext context) {
        logger.info("[JobTrigger] 分片广播触发 - jobId: {}, 执行器数量: {}", jobInfo.getId(), registryList.size());
        
        boolean allSuccess = true;
        for (int i = 0; i < registryList.size(); i++) {
            TriggerParam triggerParam = createTriggerParam(jobInfo, randomId, xxlJobContext, composeAddress, i, registryList.size(), context);
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
                                  String composeAddress, List<String> registryList, String routeStrategy, ExecutionContext context) {
        logger.debug("[JobTrigger] 普通路由触发 - jobId: {}, 路由策略: {}", jobInfo.getId(), routeStrategy);
        
        // 构建触发参数
        TriggerParam triggerParam = createTriggerParam(jobInfo, randomId, xxlJobContext, composeAddress, 0, 1, context);
        
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
     * 创建触发参数（带执行上下文，用于参数解析）
     */
    private TriggerParam createTriggerParam(JobInfo jobInfo, String randomId, XxlJobContext xxlJobContext,
                                           String composeAddress, int broadcastIndex, int broadcastTotal, ExecutionContext context) {
        // 获取原始参数
        String executorParam = jobInfo.getExecutorParam();
        String reqUrl = jobInfo.getReqUrl();
        String reqBody = jobInfo.getReqBody();
        String reqHeader = jobInfo.getReqHeader();
        String glueSource = jobInfo.getGlueSource();
        
        // 如果提供了执行上下文，解析并替换参数中的变量
        if (context != null && context.getDataContext() != null && context.getJobNameMap() != null) {
            DataContext dataContext = context.getDataContext();
            Map<String, Long> jobNameMap = context.getJobNameMap();
            
            // 解析 executorParam
            if (executorParam != null && !executorParam.isEmpty()) {
                executorParam = parameterResolver.resolve(executorParam, dataContext, jobNameMap);
                logger.debug("[JobTrigger] 解析 executorParam - jobId: {}, 原始: {}, 解析后: {}", 
                        jobInfo.getId(), jobInfo.getExecutorParam(), executorParam);
            }
            
            // 解析 reqUrl
            if (reqUrl != null && !reqUrl.isEmpty()) {
                reqUrl = parameterResolver.resolve(reqUrl, dataContext, jobNameMap);
                logger.debug("[JobTrigger] 解析 reqUrl - jobId: {}, 原始: {}, 解析后: {}", 
                        jobInfo.getId(), jobInfo.getReqUrl(), reqUrl);
            }
            
            // 解析 reqBody
            if (reqBody != null && !reqBody.isEmpty()) {
                reqBody = parameterResolver.resolve(reqBody, dataContext, jobNameMap);
                logger.debug("[JobTrigger] 解析 reqBody - jobId: {}, 原始: {}, 解析后: {}", 
                        jobInfo.getId(), jobInfo.getReqBody(), reqBody);
            }
            
            // 解析 reqHeader
            if (reqHeader != null && !reqHeader.isEmpty()) {
                reqHeader = parameterResolver.resolve(reqHeader, dataContext, jobNameMap);
                logger.debug("[JobTrigger] 解析 reqHeader - jobId: {}, 原始: {}, 解析后: {}", 
                        jobInfo.getId(), jobInfo.getReqHeader(), reqHeader);
            }
            
            // 解析 glueSource（对于 GLUE 任务）
            // 注意：对于GLUE脚本，我们采用混合策略：
            // 1. 简单值：继续使用字符串替换
            // 2. 复杂对象：通过环境变量传递（在脚本执行时设置）
            // 3. Java GLUE：通过DataContextAccessor API访问（在执行器中注入）
            if (glueSource != null && !glueSource.isEmpty()) {
                glueSource = parameterResolver.resolve(glueSource, dataContext, jobNameMap);
                logger.debug("[JobTrigger] 解析 glueSource - jobId: {}", jobInfo.getId());
            }
        }
        
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId().intValue());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(executorParam != null ? executorParam : randomId);
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(-1);  // -1 表示这是任务组子任务
        triggerParam.setRandomId(randomId);  // 设置 randomId，用于回调时标识任务组
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(glueSource);
        
        if (jobInfo.getGlueUpdateTime() != null) {
            triggerParam.setGlueUpdateTime(jobInfo.getGlueUpdateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        
        triggerParam.setBroadcastIndex(broadcastIndex);
        triggerParam.setBroadcastTotal(broadcastTotal);
        
        // 设置 HTTP 任务参数（使用解析后的值）
        triggerParam.setReqBody(reqBody);
        triggerParam.setReqHeader(reqHeader);
        triggerParam.setReqType(jobInfo.getReqType());
        triggerParam.setReqUrl(reqUrl);
        
        triggerParam.setXxlJobContext(xxlJobContext);
        // 关键：将 address 设置为 compose 执行器地址，这样子任务完成后会回调到 compose 执行器
        triggerParam.setAddress(composeAddress);
        
        // 对于GLUE任务，序列化上下文数据以便脚本访问
        if (context != null && context.getDataContext() != null && 
            jobInfo.getGlueType() != null && isGlueType(jobInfo.getGlueType())) {
            try {
                String contextJson = serializeDataContext(context.getDataContext());
                triggerParam.setContextData(contextJson);
                logger.debug("[JobTrigger] 序列化上下文数据 - jobId: {}, 数据大小: {} 字符", 
                        jobInfo.getId(), contextJson != null ? contextJson.length() : 0);
            } catch (Exception e) {
                logger.warn("[JobTrigger] 序列化上下文数据失败 - jobId: {}", jobInfo.getId(), e);
            }
        }
        
        logger.debug("[JobTrigger] 创建触发参数 - jobId: {}, randomId: {}, 回调地址: {}", 
                jobInfo.getId(), randomId, composeAddress);
        
        return triggerParam;
    }
    
    /**
     * 判断是否是GLUE类型
     */
    private boolean isGlueType(String glueType) {
        if (glueType == null) {
            return false;
        }
        return glueType.startsWith("GLUE_") || "GLUE_GROOVY".equals(glueType);
    }
    
    /**
     * 序列化DataContext为JSON字符串
     * 
     * @param dataContext 数据上下文
     * @return JSON字符串
     */
    private String serializeDataContext(DataContext dataContext) {
        if (dataContext == null) {
            return "{}";
        }
        
        try {
            // 将DataContext转换为Map
            Map<String, Object> contextMap = new java.util.HashMap<>();
            for (String key : dataContext.keySet()) {
                contextMap.put(key, dataContext.get(key));
            }
            
            // 序列化为JSON
            return JSONUtil.toJsonStr(contextMap);
        } catch (Exception e) {
            logger.error("[JobTrigger] 序列化DataContext失败", e);
            return "{}";
        }
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
                if (!ExecutorConstants.ExecutionResult.DO_NOTHING.equalsIgnoreCase(jobInfo.getFailStrategy())) {
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
     * 完整实现XXL-JOB原生支持的所有路由策略
     */
    private String selectAddress(TriggerParam triggerParam, List<String> registryList, String routeStrategy) {
        if (registryList.isEmpty()) {
            return null;
        }
        
        // 清理过期缓存（24小时）
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            JOB_LFU_MAP.clear();
            JOB_LRU_MAP.clear();
            ROUND_COUNT_MAP.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000L * 60 * 60 * 24;
        }
        
        switch (routeStrategy.toUpperCase()) {
            case "FIRST":
                // 第一个
                return registryList.get(0);
                
            case "LAST":
                // 最后一个
                return registryList.get(registryList.size() - 1);
                
            case "ROUND":
                // 轮询
                return routeRound(triggerParam.getJobId(), registryList);
                
            case "RANDOM":
                // 随机
                return registryList.get(new Random().nextInt(registryList.size()));
                
            case "CONSISTENT_HASH":
                // 一致性哈希
                return routeConsistentHash(triggerParam.getJobId(), registryList);
                
            case "LEAST_FREQUENTLY_USED":
                // 最不经常使用 (LFU)
                return routeLFU(triggerParam.getJobId(), registryList);
                
            case "LEAST_RECENTLY_USED":
                // 最近最久未使用 (LRU)
                return routeLRU(triggerParam.getJobId(), registryList);
                
            case "FAILOVER":
                // 故障转移：选择第一个健康的执行器
                return routeFailover(triggerParam.getJobId(), registryList);
                
            case "BUSYOVER":
                // 忙碌转移：选择第一个空闲的执行器
                return routeBusyover(triggerParam.getJobId(), registryList);
                
            default:
                logger.warn("[JobTrigger] 未知路由策略: {}, 使用 FIRST", routeStrategy);
                return registryList.get(0);
        }
    }
    
    /**
     * 轮询路由
     */
    private String routeRound(int jobId, List<String> addressList) {
        Integer count = ROUND_COUNT_MAP.get(jobId);
        if (count == null || count > 1000000) {
            count = new Random().nextInt(100);
        } else {
            count++;
        }
        ROUND_COUNT_MAP.put(jobId, count);
        return addressList.get(count % addressList.size());
    }
    
    /**
     * 一致性哈希路由
     */
    private String routeConsistentHash(int jobId, List<String> addressList) {
        TreeMap<Long, String> addressRing = new TreeMap<>();
        for (String address : addressList) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                long addressHash = hash("SHARD-" + address + "-NODE-" + i);
                addressRing.put(addressHash, address);
            }
        }
        long jobHash = hash(String.valueOf(jobId));
        SortedMap<Long, String> lastRing = addressRing.tailMap(jobHash);
        if (!lastRing.isEmpty()) {
            return lastRing.get(lastRing.firstKey());
        }
        return addressRing.firstEntry().getValue();
    }
    
    /**
     * MD5散列计算hash值
     */
    private long hash(String key) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes;
        try {
            keyBytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string: " + key, e);
        }
        md5.update(keyBytes);
        byte[] digest = md5.digest();
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);
        return hashCode & 0xffffffffL;
    }
    
    /**
     * LFU路由：最不经常使用
     */
    private String routeLFU(int jobId, List<String> addressList) {
        HashMap<String, Integer> lfuItemMap = JOB_LFU_MAP.computeIfAbsent(jobId, k -> new HashMap<>());
        
        // 添加新地址
        for (String address : addressList) {
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address) > 1000000) {
                lfuItemMap.put(address, new Random().nextInt(addressList.size()));
            }
        }
        // 移除旧地址
        lfuItemMap.keySet().removeIf(key -> !addressList.contains(key));
        
        // 找到使用次数最少的地址
        String minAddress = null;
        int minCount = Integer.MAX_VALUE;
        for (java.util.Map.Entry<String, Integer> entry : lfuItemMap.entrySet()) {
            if (entry.getValue() < minCount) {
                minCount = entry.getValue();
                minAddress = entry.getKey();
            }
        }
        // 增加使用计数
        if (minAddress != null) {
            lfuItemMap.put(minAddress, minCount + 1);
        }
        return minAddress != null ? minAddress : addressList.get(0);
    }
    
    /**
     * LRU路由：最近最久未使用
     */
    private String routeLRU(int jobId, List<String> addressList) {
        LinkedHashMap<String, String> lruItem = JOB_LRU_MAP.computeIfAbsent(jobId, 
                k -> new LinkedHashMap<>(16, 0.75f, true));
        
        // 添加新地址
        for (String address : addressList) {
            if (!lruItem.containsKey(address)) {
                lruItem.put(address, address);
            }
        }
        // 移除旧地址
        lruItem.keySet().removeIf(key -> !addressList.contains(key));
        
        // 获取最早插入的地址（最久未使用）
        String eldestKey = lruItem.keySet().iterator().next();
        return lruItem.get(eldestKey);
    }
    
    /**
     * 故障转移路由：选择第一个健康的执行器
     */
    private String routeFailover(int jobId, List<String> addressList) {
        for (String address : addressList) {
            try {
                ExecutorBiz executorBiz = getExecutorBiz(address);
                ReturnT<String> beatResult = executorBiz.beat();
                if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {
                    logger.debug("[JobTrigger] 故障转移选择执行器 - jobId: {}, address: {}", jobId, address);
                    return address;
                }
            } catch (Exception e) {
                logger.warn("[JobTrigger] 执行器心跳检查失败 - address: {}, error: {}", address, e.getMessage());
            }
        }
        logger.warn("[JobTrigger] 故障转移未找到健康执行器 - jobId: {}, 使用第一个", jobId);
        return addressList.get(0);
    }
    
    /**
     * 忙碌转移路由：选择第一个空闲的执行器
     */
    private String routeBusyover(int jobId, List<String> addressList) {
        for (String address : addressList) {
            try {
                ExecutorBiz executorBiz = getExecutorBiz(address);
                ReturnT<String> idleBeatResult = executorBiz.idleBeat(new com.xxl.job.core.biz.model.IdleBeatParam(jobId));
                if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {
                    logger.debug("[JobTrigger] 忙碌转移选择空闲执行器 - jobId: {}, address: {}", jobId, address);
                    return address;
                }
            } catch (Exception e) {
                logger.warn("[JobTrigger] 执行器空闲检查失败 - address: {}, error: {}", address, e.getMessage());
            }
        }
        logger.warn("[JobTrigger] 忙碌转移未找到空闲执行器 - jobId: {}, 使用第一个", jobId);
        return addressList.get(0);
    }
    
    /**
     * 获取或创建执行器客户端
     */
    private ExecutorBiz getExecutorBiz(String address) throws Exception {
        return EXECUTOR_BIZ_CACHE.computeIfAbsent(address, addr -> {
            try {
                // 创建执行器客户端
                // 注意：这里需要使用 XXL-Job 的 ExecutorBizClient
                return new ExecutorBizClient(addr, null);
            } catch (Exception e) {
                logger.error("[JobTrigger] 创建执行器客户端失败 - 地址: {}", addr, e);
                throw new RuntimeException("创建执行器客户端失败", e);
            }
        });
    }
}
