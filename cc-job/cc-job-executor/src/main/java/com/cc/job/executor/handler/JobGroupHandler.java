package com.cc.job.executor.handler;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.executor.executor.Async;
import com.cc.job.executor.executor.callback.ICallback;
import com.cc.job.executor.executor.callback.IWorker;
import com.cc.job.executor.executor.worker.WorkResult;
import com.cc.job.executor.executor.wrapper.WorkerWrapper;
import com.cc.job.executor.model.Message;
import com.cc.job.executor.route.ExecutorRouteFirst;
import com.cc.job.executor.service.JobNodeService;
import com.cc.job.executor.service.SSEService;
import com.cc.job.executor.utils.JobGroupUtils;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.mapper.*;
import com.cc.job.xo.model.entity.*;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.group.JobGroupTool;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cc.job.executor.constants.JobConstant.*;

@Component
@RequiredArgsConstructor
public class JobGroupHandler {

    private static final Logger logger = LoggerFactory.getLogger(JobGroupHandler.class);

    private final JobNodeService jobNodeService;

    private final JobGroupUtils jobGroupUtils;

    private final JobInfoMapper jobInfoMapper;

    private final JobGroupMapper jobGroupMapper;

    private final JobEdgeMapper jobEdgeMapper;

    private final JobNodeMapper jobNodeMapper;

    private final JobGroupSnapshotMapper jobGroupSnapshotMapper;

    final SSEService sseService;


    static final ConcurrentHashMap<String, List<WorkerWrapper<Long, String>>> STOP_MAP = new ConcurrentHashMap<>();


    /**
     * 获取WorkerWrapper列表
     *
     * @param parentId 父任务ID
     * @param randomId 随机ID
     * @return WorkerWrapper列表
     */
    public static List<WorkerWrapper<Long, String>> getWorkWrapper(Long parentId, String randomId) {
        String key = setExecuteJobId(parentId, randomId);
        return STOP_MAP.get(key);
    }

    /**
     * 线程本地变量，用于解决线程问题，避免日志文件错误添加
     */
    private static final InheritableThreadLocal<XxlJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    /**
     * 任务组执行入口方法
     *
     * 执行流程：
     * 1. 验证执行参数
     * 2. 获取任务信息
     * 3. 构建任务图
     * 4. 执行拓扑排序
     * 5. 监控执行状态
     */
    @XxlJob("runJobGroupXxlJob")
    public void jobGroupXxlJob() {
        long jobId = XxlJobHelper.getJobId();
        String executeParam = XxlJobHelper.getJobParam();

        logger.info("[JobGroup] ========== 开始执行任务组 ==========");
        logger.info("[JobGroup] 任务ID: {}, 执行参数: {}", jobId, executeParam);

        // 验证执行参数
        validateExecuteParam(executeParam);
        String randomId = "";
        try {
            // 如果jobId和执行参数一致，则需要匹配一个uuid，如果不匹配则代表是platform页面执行，会携带一个随机id
            randomId = String.valueOf(jobId).equalsIgnoreCase(executeParam) ? UUID.randomUUID().toString()
                    : executeParam;
            logger.debug("[JobGroup] 生成随机ID - jobId: {}, randomId: {}", jobId, randomId);

            JobInfo jobInfo = getJobInfoById(jobId);
            logger.info("[JobGroup] 任务信息获取成功 - 任务名称: {}, 任务类型: {}, 执行器超时时间: {}秒",
                    jobInfo.getJobDesc(), jobInfo.getJobType(), jobInfo.getExecutorTimeout());

            Map<Long, List<Long>> statusMap = new HashMap<>();
            // 设置初始状态，页面颜色提示
            getJobStatusMap(jobId, statusMap);
            logger.debug("[JobGroup] 初始化状态映射 - jobId: {}, 状态映射大小: {}", jobId, statusMap.size());

            // 【快照模式】优先从快照获取节点和边信息，如果没有快照则从数据库获取
            List<JobNode> nodes = new ArrayList<>();
            List<JobEdge> edges = new ArrayList<>();
            boolean useSnapshot = loadNodesAndEdgesFromSnapshot(jobId, randomId, nodes, edges);

            if (!useSnapshot) {
                // 如果没有快照，从数据库获取（兼容旧逻辑）
                logger.info("[JobGroup] 未找到快照，从数据库获取节点和边 - jobId: {}, randomId: {}", jobId, randomId);
                getAllNodesAndEdges(jobId, nodes, edges);
            } else {
                logger.info("[JobGroup] 从快照获取节点和边 - jobId: {}, randomId: {}, 节点数量: {}, 边数量: {}",
                        jobId, randomId, nodes.size(), edges.size());
            }

            // 修复：在任务组开始执行前，重置所有节点的运行状态为 -1（未运行状态）
            // 这样可以确保每次运行任务组时，节点状态都是干净的初始状态
            int resetCount = jobNodeService.resetAllNodeStatus(jobId);
            logger.info("[JobGroup] 重置节点状态完成 - jobId: {}, 重置节点数量: {}", jobId, resetCount);

            // 构图
            buildGraph(jobId, nodes, edges);
            logger.debug("[JobGroup] 构建任务图完成 - jobId: {}", jobId);

            // 找到当前节点的next节点
            Map<Long, List<JobNode>> nextMap = buildNextNode(nodes, edges);
            logger.debug("[JobGroup] 构建节点关系映射 - jobId: {}, 关系映射大小: {}", jobId, nextMap.size());

            CONTEXT_HOLDER.set(XxlJobContext.getXxlJobContext());
            // 构造WorkerWrapper，实现任务的串并行执行
            List<WorkerWrapper<Long, String>> workerWrappers = buildWorkerWrappers(nodes, nextMap, randomId, statusMap);
            logger.info("[JobGroup] WorkerWrapper构建完成 - jobId: {}, WorkerWrapper数量: {}", jobId, workerWrappers.size());

            // 预测任务的运行时间
            getRuntime(workerWrappers, nodes, jobInfo.getExecutorTimeout(), jobId, randomId);

            STOP_MAP.put(setExecuteJobId(jobId, randomId), workerWrappers);
            logger.info("[JobGroup] 开始拓扑排序执行任务组 - jobId: {}, 线程池大小: {}", jobId, nodes.size() + 1);

            // 使用拓扑排序执行器，按层级执行任务，控制并发度
            Async.beginWork(jobInfo.getExecutorTimeout().longValue(), (List<WorkerWrapper>) (List<?>) workerWrappers);

            logger.info("[JobGroup] ========== 任务组执行完成 ==========");
        } catch (Exception e) {
            handleExecutionException(jobId, e);
        } finally {
            completeJob(jobId, randomId);
        }
    }

    private void getRuntime(List<WorkerWrapper<Long, String>> workerWrappers, List<JobNode> nodes, long timeout,
                            Long jobId, String randomId) throws IOException {
        logger.debug("[JobGroup] 开始计算任务运行时间 - jobId: {}, 节点数量: {}, 超时时间: {}秒", jobId, nodes.size(), timeout);

        List<JobInfo> jobInfos = getJobInfos(nodes);
        Map<Long, JobInfo> jobInfoMap = new HashMap<>();
        final Map<Long, JobInfo> jobInfoDbMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));
        for (JobNode jobNode : nodes) {
            jobInfoMap.put(jobNode.getId(), jobInfoDbMap.get(jobNode.getJobId()));
        }
        String[][] nextRunTime = jobGroupUtils.getNextRunTime(workerWrappers, jobInfoMap, timeout, getStartNodes(nodes),
                jobId);
        Message message = new Message();
        message.setJobId(jobId);
        message.setParentJobId(jobId);
        message.setStatus(9);
        message.setRandomId(randomId);
        message.setResult(JSONUtil.toJsonStr(nextRunTime));
        // 使用SSE服务发送消息
        sseService.sendMessage(message);
    }

    /**
     * 获取任务信息列表
     */
    private List<JobInfo> getJobInfos(List<JobNode> nodes) {
        List<Long> jobIds = nodes.stream().map(JobNode::getJobId).toList();
        return jobInfoMapper.selectBatchIds(jobIds);
    }

    /**
     * 验证执行参数
     *
     * @param executeParam 执行参数
     * @throws BusinessException 参数为空时抛出异常
     */
    private void validateExecuteParam(String executeParam) {
        if (StringUtils.isBlank(executeParam)) {
            logger.error("[JobGroup] 执行参数验证失败 - executeParam为空");
            throw new BusinessException("executeParam is null");
        }
        logger.debug("[JobGroup] 执行参数验证通过 - executeParam: {}", executeParam);
    }

    /**
     * 根据ID获取任务信息
     *
     * @param jobId 任务ID
     * @return 任务信息
     * @throws BusinessException 任务不存在时抛出异常
     */
    private JobInfo getJobInfoById(long jobId) {
        JobInfo jobInfo = jobInfoMapper.selectById(jobId);
        if (jobInfo == null) {
            logger.error("[JobGroup] 任务信息不存在 - jobId: {}", jobId);
            throw new BusinessException("TaskInfo not found for jobId: " + jobId);
        }
        logger.debug("[JobGroup] 获取任务信息成功 - jobId: {}, 任务名称: {}", jobId, jobInfo.getJobDesc());
        return jobInfo;
    }

    /**
     * 获取开始节点列表（入度为0的节点）
     */
    private List<Long> getStartNodes(List<JobNode> nodes) {
        return nodes.stream().filter(v -> v.getNodeInDegree().equals(0L)).map(JobNode::getId).toList();
    }

    /**
     * 处理执行异常
     *
     * @param jobId 任务ID
     * @param e     异常信息
     */
    private void handleExecutionException(long jobId, Exception e) {
        XxlJobHelper.log("========================================= 任务运行异常 =========================================");
        XxlJobHelper.log("任务ID: {}, 异常信息: {}", jobId, e.getMessage());
        logger.error("[JobGroup] 任务组执行异常 - jobId: {}, 异常信息: {}", jobId, e.getMessage(), e);
        throw new BusinessException(e.getMessage());
    }

    /**
     * 完成任务组执行
     *
     * @param jobId    任务ID
     * @param randomId 随机ID
     */
    private void completeJob(long jobId, String randomId) {
        logger.info("[JobGroup] ========== 任务组执行完成 ==========");
        logger.info("[JobGroup] 任务ID: {}, 随机ID: {}", jobId, randomId);
        XxlJobHelper.log("========================================= 任务运行完成 =========================================");
        XxlJobHelper.log("任务ID: {}, 随机ID: {}", jobId, randomId);

        // 发送完成消息
        sendCompletionMessage(jobId, randomId);

        // 停止任务组合
        jobInfoMapper.stopJobCompose(jobId);

        // 清理资源
        STOP_MAP.remove(setExecuteJobId(jobId, randomId));
        
        // ⭐ 清理全局任务状态 Map（清理该任务组的所有子任务状态）
        // 注意：这里不能简单地清空整个 Map，因为可能有其他任务组正在运行
        // 应该在任务完成时逐个清理，但为了安全起见，这里也清理一次
        // 实际上，每个任务完成时已经在 handleJobCompletion 中清理了

        // 关闭SSE连接
        sseService.closeConnection(jobId, randomId);

        // 【快照模式】清理快照数据
        try {
            LambdaQueryWrapper<JobGroupSnapshot> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(JobGroupSnapshot::getJobId, jobId)
                    .eq(JobGroupSnapshot::getRandomId, randomId)
                    .eq(JobGroupSnapshot::getIsDeleted, 0);

            jobGroupSnapshotMapper.delete(wrapper);
            logger.info("[Snapshot] 快照清理成功 - jobId: {}, randomId: {}", jobId, randomId);
        } catch (Exception e) {
            logger.error("[Snapshot] 快照清理失败 - jobId: {}, randomId: {}", jobId, randomId, e);
        }

        CONTEXT_HOLDER.remove();

        logger.debug("[JobGroup] 资源清理完成 - jobId: {}, 剩余任务组数量: {}", jobId, STOP_MAP.size());
    }

    /**
     * 发送任务完成消息
     */
    private void sendCompletionMessage(long jobId, String randomId) {
        Message message = new Message();
        message.setJobId(jobId);
        message.setParentJobId(jobId);
        message.setStatus(5);
        message.setRandomId(randomId);
        // 使用SSE服务发送消息
        sseService.sendMessage(message);
        logger.debug("[JobGroup] 发送任务完成消息 - jobId: {}, randomId: {}", jobId, randomId);
    }

    /**
     * 构造WorkerWrapper，实现任务的串并行执行
     *
     * @param nodes     当前节点列表
     * @param nextMap   节点的next节点映射
     * @param randomId  随机id，前端页面运行传递
     * @param statusMap 状态映射
     * @return WorkerWrapper列表
     */
    private List<WorkerWrapper<Long, String>> buildWorkerWrappers(List<JobNode> nodes, Map<Long, List<JobNode>> nextMap,
                                                                  String randomId, Map<Long, List<Long>> statusMap) {
        logger.debug("[JobGroup] 开始构建WorkerWrapper - 节点数量: {}", nodes.size());

        List<WorkerWrapper<Long, String>> result = new ArrayList<>();
        List<JobInfo> jobInfos = getJobInfos(nodes);
        final Map<Long, JobInfo> jobInfoMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));
        XxlJobContext xxlJobContext = CONTEXT_HOLDER.get();

        // 为每个节点创建WorkerWrapper
        for (JobNode node : nodes) {
            final JobInfo jobInfo = jobInfoMap.get(node.getJobId());
            WorkerWrapper<Long, String> worker = new WorkerWrapper<Long, String>()
                    .id(String.valueOf(node.getId()))
                    .param(node.getJobId())
                    .timeout(jobInfo.getExecutorTimeout())
                    .retryCount(jobInfo.getExecutorFailRetryCount())
                    .worker(new IWorker<>() {
                        @Override
                        public String action(Long jobId, Map<String, WorkerWrapper> allWrappers) {
                            // 当前wrapper
                            WorkerWrapper workerWrapper = allWrappers.get(String.valueOf(node.getId()));
                            int count = workerWrapper.getCount();
                            // 执行任务
                            return executeJob(xxlJobContext, node, jobInfo, randomId, statusMap, count);
                        }

                        @Override
                        public String defaultValue() {
                            try {
                                setNodeStatus(statusMap, node.getJobId(), 0, randomId, node.getJobParentId());
                            } catch (Exception e) {
                                logger.error("[JobGroup] 设置节点状态失败 - nodeId: {}, jobId: {}, 异常信息: {}",
                                        node.getId(), node.getJobId(), e.getMessage(), e);
                            }
                            return FAIL_COMPLETE;
                        }
                    })
                    .callback(new JobCallback(xxlJobContext, node, randomId, statusMap));
            result.add(worker);
        }

        // 设置节点间的依赖关系
        logger.info("[JobGroup] ========== 开始设置节点依赖关系 ==========");
        int dependencyCount = 0;
        for (WorkerWrapper<Long, String> workerWrapper : result) {
            String id = workerWrapper.getId();
            List<JobNode> taskNodes = nextMap.get(Long.valueOf(id));
            if (taskNodes.isEmpty()) {
                logger.debug("[JobGroup] 节点 {} 没有后续节点", id);
                continue;
            }
            List<Long> cNodeIds = taskNodes.stream().map(JobNode::getId).toList();
            List<WorkerWrapper<Long, String>> nextWorkers = result.stream()
                    .filter(v -> cNodeIds.contains(Long.valueOf(v.getId()))).toList();
            workerWrapper.next(nextWorkers.toArray(new WorkerWrapper[0]));
            dependencyCount++;
            logger.info("[JobGroup] 节点 {} 的后续节点: {}", id, 
                    nextWorkers.stream().map(WorkerWrapper::getId).toList());
        }
        logger.info("[JobGroup] ========== 节点依赖关系设置完成 ==========");
        logger.info("[JobGroup] 总节点数: {}, 有依赖关系的节点数: {}", result.size(), dependencyCount);

        logger.debug("[JobGroup] WorkerWrapper构建完成 - 总数: {}", result.size());
        return result;
    }

    /**
     * 执行任务的核心方法
     *
     * @param xxlJobContext jobContext，主要是将子任务日志写到主任务中
     * @param node          当前节点
     * @param jobInfo       当前节点对应的jobInfo
     * @param randomId      随机id，保证多页面运行时节点状态运行正常
     * @param statusMap     状态映射
     * @param count         失败重试次数
     * @return 运行结果
     */
    private String executeJob(XxlJobContext xxlJobContext, JobNode node, JobInfo jobInfo, String randomId,
                              Map<Long, List<Long>> statusMap, int count) {
        logger.info("[JobGroup] ========== 开始执行任务 ==========");
        logger.info("[JobGroup] jobId: {}, nodeId: {}, 重试次数: {}, 任务名称: {}, randomId: {}",
                jobInfo.getId(), node.getId(), count, jobInfo.getJobDesc(), randomId);

        // 暂停任务执行
        pauseJob(jobInfo);

        // ⭐ 关键：在触发任务前，先清理可能存在的旧状态（避免重试时使用旧状态）
        int jobIdInt = jobInfo.getId().intValue();
        JobGroupTool.removeJobData(jobIdInt);
        logger.debug("[JobGroup] 已清理旧状态 - jobId: {}", jobIdInt);

        // 触发任务
        triggerJob(xxlJobContext, jobInfo, randomId);

        // 核心代码，创建一个线程用来监听任务是否运行完成
        logger.info("[JobGroup] 开始监听任务完成状态 - jobId: {}", jobInfo.getId());
        String result = listenerJob(xxlJobContext, node, jobInfo, randomId, statusMap, count);
        logger.info("[JobGroup] ========== 任务执行完成 ==========");
        logger.info("[JobGroup] jobId: {}, 执行结果: {}", jobInfo.getId(), result);
        return result;
    }

    /**
     * 监听任务是否运行完成
     *
     * @param node      任务节点
     * @param jobInfo   任务信息
     * @param randomId  随机ID
     * @param statusMap 状态映射
     * @param count     重试次数
     * @return 执行结果
     */
    private String listenerJob(XxlJobContext xxlJobContext,JobNode node, JobInfo jobInfo, String randomId, Map<Long, List<Long>> statusMap,
                               int count) {
        String result=null;
        Thread thread = null;
        JobThreadListener jobThreadListener = null;
        try {
            logger.debug("[JobGroup] 开始监听任务执行状态 - jobId: {}, nodeId: {}, 超时时间: {}秒",
                    jobInfo.getId(), node.getId(), jobInfo.getExecutorTimeout());
            jobThreadListener = new JobThreadListener(xxlJobContext,jobInfo, node, randomId, statusMap, count);
            FutureTask<String> futureTask = new FutureTask<>(jobThreadListener);
            thread = new Thread(futureTask);
            thread.start();
            result = jobInfo.getExecutorTimeout() > 0
                    ? futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.SECONDS)
                    : futureTask.get();
            logger.info("[JobGroup] 任务监听完成 - jobId: {}, nodeId: {}, 执行结果: {}",
                    jobInfo.getId(), node.getId(), result);
        } catch (Exception e) {
            logger.error("[JobGroup] 任务监听异常 - jobId: {}, nodeId: {}, 异常信息: {}",
                    jobInfo.getId(), node.getId(), e.getMessage(), e);
            // 这里只有设置超时时间失败是才会报错
            setNodeStatus(statusMap, jobInfo.getId(), 0, randomId, node.getJobParentId());
            if(!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())){
                throw new RuntimeException(e);
            }
        } finally {
            if (jobThreadListener != null) {
                jobThreadListener.toStop();
            }
            if (thread != null) {
                thread.interrupt();
            }
        }
        return result;
    }

    /**
     * 暂停任务执行
     *
     * @param jobInfo 任务信息
     */
    private void pauseJob(JobInfo jobInfo) {
        // 重新获取jobInfo
        boolean isPause = jobInfo.getIsPause() == 1;
        if (isPause) {
            logger.info("[JobGroup] 任务处于暂停状态，等待恢复 - jobId: {}, 任务名称: {}",
                    jobInfo.getId(), jobInfo.getJobDesc());
        }
        // 暂停任务，默认暂停任务5分钟
        long timeout = jobInfo.getExecutorTimeout() > 0 ? jobInfo.getExecutorTimeout()*1000 : 5 * 60 * 1000;
        long startTime = System.currentTimeMillis();
        while (isPause) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeout) {
                logger.warn("[JobGroup] 任务暂停等待超时 - jobId: {}, 等待时长: {}ms", jobInfo.getId(), elapsed);
                break;
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error("[JobGroup] 任务暂停等待被中断 - jobId: {}", jobInfo.getId(), e);
                throw new RuntimeException(e);
            }
            JobInfo jobInfoModel = jobInfoMapper.selectById(jobInfo.getId());
            isPause = jobInfoModel.getIsPause() == 1;
        }
        if (!isPause) {
            logger.info("[JobGroup] 任务恢复执行 - jobId: {}, 任务名称: {}", jobInfo.getId(), jobInfo.getJobDesc());
        }
    }

    /**
     * 触发任务执行
     *
     * @param xxlJobContext 任务上下文
     * @param jobInfo       任务信息
     * @param randomId      随机ID
     */
    private void triggerJob(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId) {
        logger.debug("[JobGroup] 开始触发任务 - jobId: {}, 执行器处理器: {}, 随机ID: {}",
                jobInfo.getId(), jobInfo.getExecutorHandler(), randomId);

        JobGroup group = jobGroupMapper.selectById(jobInfo.getJobGroup());
        String ip = IpUtil.getIp();
        String adminAddress = "";

        List<String> registryList = group.getRegistryList();
        if (registryList == null || registryList.isEmpty()) {
            logger.error("[JobGroup] 当前执行器组未注册可用实例 - jobId: {}", jobInfo.getId());
            throw new BusinessException("执行器未注册，无法触发任务");
        }

//        ExecutorRouteStrategyEnum routeStrategyEnum = ExecutorRouteStrategyEnum.match(
//                jobInfo.getExecutorRouteStrategy(), ExecutorRouteStrategyEnum.FIRST);

        // TODO 暂时先不使用路由
//        if ("SHARDING_BROADCAST" == jobInfo.getExecutorRouteStrategy()) {
//            for (int i = 0; i < registryList.size(); i++) {
//                TriggerParam broadcastParam = createTriggerParam(jobInfo, randomId, xxlJobContext, adminAddress, i,
//                        registryList.size());
//                doTrigger(jobInfo, randomId, xxlJobContext, broadcastParam, registryList.get(i));
//            }
//            return;
//        }
//
          TriggerParam triggerParam = createTriggerParam(jobInfo, randomId, xxlJobContext, adminAddress, 0, 1);
//        ReturnT<String> routeResult = routeStrategyEnum.getRouter().route(triggerParam, registryList);
//        if (routeResult == null || routeResult.getCode() != ReturnT.SUCCESS_CODE
//                || routeResult.getContent() == null) {
//            String reason = routeResult != null ? routeResult.getMsg() : "未获取到可用执行器";
//            logger.error("[JobGroup] 触发路由失败 - jobId: {}, route: {}, 原因: {}", jobInfo.getId(), routeStrategyEnum,
//                    reason);
//            throw new BusinessException("触发路由失败: " + reason);
//        }
        // TODO 默认先选择第一个执行器
        ReturnT<String> routeResult = ExecutorRouteFirst.route(triggerParam, registryList);
        doTrigger(jobInfo, randomId, xxlJobContext, triggerParam, routeResult.getContent());
    }

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
            triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
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

    private void doTrigger(JobInfo jobInfo, String randomId, XxlJobContext xxlJobContext, TriggerParam triggerParam,
                           String address) {
        logger.info("[JobGroup] ========== 发送任务到执行器 ==========");
        logger.info("[JobGroup] jobId: {}, 执行器地址: {}, randomId: {}", jobInfo.getId(), address, randomId);

        ReturnT<String> returnT = XxlJobRemotingUtil.postBody(address + "run", "", 3, triggerParam, String.class);
        if (returnT.getCode() != ReturnT.SUCCESS_CODE) {
            logger.error("[JobGroup] ❌ 任务触发失败 - jobId: {}, 错误信息: {}", jobInfo.getId(), returnT.getMsg());
            XxlJobHelper.log(xxlJobContext,
                    "========================================= 任务触发失败 =========================================");
            XxlJobHelper.log(xxlJobContext, "任务ID: {}, 错误信息: {}", jobInfo.getId(), returnT.getMsg());
            // ⭐ 修复：任务触发失败时，也要更新全局 Map，让 JobThreadListener 能检测到
            int jobIdInt = jobInfo.getId().intValue();
            xxlJobContext.setJobMap(jobIdInt, returnT.getCode());
            JobGroupTool.addJobData(jobIdInt, returnT.getCode());
            logger.info("[JobGroup] 已更新任务触发失败状态到全局 Map - jobId: {}, code: {}", jobIdInt, returnT.getCode());
            if(!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())){
                throw new RuntimeException(returnT.getMsg());
            }
            return;
        }

        logger.info("[JobGroup] ✅ 任务触发成功 - jobId: {}, 等待任务执行完成...", jobInfo.getId());
        XxlJobHelper.log(xxlJobContext, "任务触发成功 - 任务ID: {}", jobInfo.getId());
    }

    /**
     * 处理任务完成
     *
     * @param pair      任务结果对
     * @param jobInfo   任务信息
     * @param node      任务节点
     * @param randomId  随机ID
     * @param statusMap 状态映射
     * @param count     重试次数
     * @return 处理结果
     */
    private String handleJobCompletion(XxlJobContext xxlJobContext,Pair<Integer, Integer> pair, JobInfo jobInfo, JobNode node, String randomId,
                                       Map<Long, List<Long>> statusMap, int count) {
        int  jobId = pair.getKey();
        int  success = pair.getValue();
        // ⭐ 修复：从全局 Map 中移除已完成的任务状态（已在 JobThreadListener 中移除，这里再次确认清理）
        // 同时从 xxlJobContext.jobMap 中移除（用于兼容性）
        JobGroupTool.removeJobData(jobId);
        xxlJobContext.getJobMap().remove(jobId);
        if (success == 200) {
            setNodeStatus(statusMap, (long)jobId, 1, randomId, node.getJobParentId());
            logger.info("[JobGroup] 任务执行成功 - jobId: {}, 任务名称: {}", jobId, jobInfo.getJobDesc());
        } else {
            if (count < jobInfo.getExecutorFailRetryCount()) {
                logger.warn("[JobGroup] 任务执行失败，准备重试 - jobId: {}, nodeId: {}, 当前重试次数: {}, 最大重试次数: {}",
                        jobId, node.getJobId(), count, jobInfo.getExecutorFailRetryCount());
                return FAIL_RETRY;
            }
            logger.error("[JobGroup] 任务执行失败，已达到最大重试次数 - jobId: {}, nodeId: {}, 重试次数: {}",
                    jobId, node.getJobId(), count);
            setNodeStatus(statusMap, (long)jobId, 0, randomId, node.getJobParentId());
        }
        return SUCCESS;
    }

    /**
     * 构建节点关系映射
     *
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 节点关系映射
     */
    private Map<Long, List<JobNode>> buildNextNode(List<JobNode> nodes, List<JobEdge> edges) {
        Map<Long, List<JobNode>> result = new HashMap<>();
        for (JobNode node : nodes) {
            List<Long> nodeIds = edges.stream().filter(v -> v.getFromNodeId().equals(node.getId()))
                    .map(JobEdge::getEndNodeId).toList();
            List<JobNode> cNodes = nodes.stream().filter(v -> nodeIds.contains(v.getId())).toList();
            result.put(node.getId(), cNodes);
        }
        return result;
    }

    /**
     * 从快照加载节点和边信息
     *
     * @param jobId    任务组ID
     * @param randomId 批次ID
     * @param nodes    节点列表（输出参数）
     * @param edges    边列表（输出参数）
     * @return 是否成功从快照加载
     */
    private boolean loadNodesAndEdgesFromSnapshot(Long jobId, String randomId, List<JobNode> nodes, List<JobEdge> edges) {
        try {
            LambdaQueryWrapper<JobGroupSnapshot> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(JobGroupSnapshot::getJobId, jobId)
                    .eq(JobGroupSnapshot::getRandomId, randomId)
                    .eq(JobGroupSnapshot::getIsDeleted, 0)
                    .orderByDesc(JobGroupSnapshot::getCreateTime)
                    .last("LIMIT 1");

            JobGroupSnapshot snapshot = jobGroupSnapshotMapper.selectOne(wrapper);

            if (snapshot == null || StringUtils.isBlank(snapshot.getNodesJson()) || StringUtils.isBlank(snapshot.getEdgesJson())) {
                logger.debug("[Snapshot] 快照不存在或数据为空 - jobId: {}, randomId: {}", jobId, randomId);
                return false;
            }

            // 解析节点JSON
            List<JobNode> snapshotNodes = JSONUtil.toList(snapshot.getNodesJson(), JobNode.class);
            nodes.addAll(snapshotNodes);

            // 解析边JSON
            List<JobEdge> snapshotEdges = JSONUtil.toList(snapshot.getEdgesJson(), JobEdge.class);
            edges.addAll(snapshotEdges);

            logger.info("[Snapshot] 从快照加载成功 - jobId: {}, randomId: {}, 节点数量: {}, 边数量: {}",
                    jobId, randomId, snapshotNodes.size(), snapshotEdges.size());
            return true;
        } catch (Exception e) {
            logger.error("[Snapshot] 从快照加载失败 - jobId: {}, randomId: {}", jobId, randomId, e);
            return false;
        }
    }

    /**
     * 获取所有节点和边信息（从数据库获取）
     *
     * @param jobId 任务ID
     * @param nodes 节点列表（输出参数）
     * @param edges 边列表（输出参数）
     */
    private void getAllNodesAndEdges(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        logger.debug("[JobGroup] 开始获取所有节点和边 - jobId: {}", jobId);

        // 获取直接子节点和边
        List<JobNode> jobNodes = jobNodeMapper
                .selectList(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobId));
        List<JobEdge> jobEdges = jobEdgeMapper
                .selectList(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobId));

        nodes.addAll(jobNodes);
        edges.addAll(jobEdges);

        logger.debug("[JobGroup] 获取直接子节点和边 - jobId: {}, 节点数量: {}, 边数量: {}", jobId, jobNodes.size(), jobEdges.size());

        // 递归获取子任务组的节点和边
        List<JobInfo> childJobInfos = jobInfoMapper
                .selectList(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
        for (JobInfo childJobInfo : childJobInfos) {
            if (childJobInfo.getJobType() == 2) {
                logger.debug("[JobGroup] 递归获取子任务组节点和边 - 父任务ID: {}, 子任务ID: {}", jobId, childJobInfo.getId());
                getAllNodesAndEdges(childJobInfo.getId(), nodes, edges);
            }
        }

        logger.debug("[JobGroup] 获取所有节点和边完成 - jobId: {}, 总节点数量: {}, 总边数量: {}", jobId, nodes.size(), edges.size());
    }

    /**
     * 构建任务图
     *
     * @param jobId 任务ID
     * @param nodes 节点列表
     * @param edges 边列表
     */
    private void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        logger.debug("[JobGroup] 开始构建任务图 - jobId: {}", jobId);

        Map<Long, List<JobNode>> nodesByParent = new HashMap<>();
        for (JobNode node : nodes) {
            nodesByParent.computeIfAbsent(node.getJobParentId(), k -> new ArrayList<>()).add(node);
        }

        Map<Long, List<JobEdge>> edgesByFrom = new HashMap<>();
        Map<Long, List<JobEdge>> edgesByTo = new HashMap<>();
        for (JobEdge edge : edges) {
            edgesByFrom.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
            edgesByTo.computeIfAbsent(edge.getEndNodeId(), k -> new ArrayList<>()).add(edge);
        }

        List<JobNode> nodeList = new ArrayList<>(nodesByParent.getOrDefault(jobId, Collections.emptyList()));
        getNodeList(jobId, nodes, edges, nodeList, nodesByParent, edgesByFrom, edgesByTo);

        // 计算节点的入度和出度
        nodes.forEach(node -> {
            node.setNodeInDegree((long) edgesByTo.getOrDefault(node.getId(), Collections.emptyList()).size());
            node.setNodeOutDegree((long) edgesByFrom.getOrDefault(node.getId(), Collections.emptyList()).size());
            node.setJobParentId(jobId);
        });
        edges.forEach(edge -> edge.setJobParentId(jobId));

        logger.debug("[JobGroup] 任务图构建完成 - jobId: {}, 节点数量: {}, 边数量: {}", jobId, nodes.size(), edges.size());
    }

    /**
     * 处理节点列表
     *
     * @param jobId    任务ID
     * @param nodes    节点列表
     * @param edges    边列表
     * @param nodeList 待处理的节点列表
     */
    private void getNodeList(Long jobId, List<JobNode> nodes, List<JobEdge> edges, List<JobNode> nodeList,
                             Map<Long, List<JobNode>> nodesByParent, Map<Long, List<JobEdge>> edgesByFrom,
                             Map<Long, List<JobEdge>> edgesByTo) {
        logger.debug("[JobGroup] 处理节点列表 - jobId: {}, 节点数量: {}", jobId, nodeList.size());

        for (JobNode node : nodeList) {
            List<Long> preNodeIds = edgesByTo.getOrDefault(node.getId(), Collections.emptyList()).stream()
                    .map(JobEdge::getFromNodeId).toList();
            List<Long> nextNodeIds = edgesByFrom.getOrDefault(node.getId(), Collections.emptyList()).stream()
                    .map(JobEdge::getEndNodeId).toList();
            logger.debug("[JobGroup] 处理节点 - nodeId: {}, 前置节点数量: {}, 后置节点数量: {}",
                    node.getId(), preNodeIds.size(), nextNodeIds.size());
            concatNode(jobId, node, preNodeIds, nextNodeIds, nodes, edges, nodesByParent, edgesByFrom, edgesByTo);
        }
    }

    /**
     * 将多维图像降维
     *
     * 处理任务组节点，将其子节点直接连接到父节点，实现图的扁平化
     *
     * @param jobId       任务ID
     * @param currentNode 当前节点
     * @param preNodeIds  前置节点ID列表
     * @param nextNodeIds 后置节点ID列表
     * @param nodes       节点列表
     * @param edges       边列表
     */
    private void concatNode(Long jobId, JobNode currentNode, List<Long> preNodeIds, List<Long> nextNodeIds,
                            List<JobNode> nodes, List<JobEdge> edges, Map<Long, List<JobNode>> nodesByParent,
                            Map<Long, List<JobEdge>> edgesByFrom, Map<Long, List<JobEdge>> edgesByTo) {
        JobInfo jobInfo = jobInfoMapper.selectById(currentNode.getJobId());
        if (jobInfo.getJobType() == 2) {
            // 移除与当前节点相关的边
            removeEdges(preNodeIds, currentNode.getId(), edges, edgesByFrom, edgesByTo, true);
            removeEdges(nextNodeIds, currentNode.getId(), edges, edgesByFrom, edgesByTo, false);

            // 获取子任务组的节点
            List<JobNode> childrenNodes = new ArrayList<>(
                    nodesByParent.getOrDefault(jobInfo.getId(), Collections.emptyList()));

            // ⭐ 修复：基于 edgesByFrom 和 edgesByTo 实时计算子节点的入度和出度，而不是使用节点对象中可能过期的值
            // 因为子节点可能属于不同的任务组，它们的入度和出度应该基于当前图中的边来计算
            List<Long> childrenNodeIds = childrenNodes.stream().map(JobNode::getId).toList();
            Map<Long, Long> childrenInDegree = new HashMap<>();
            Map<Long, Long> childrenOutDegree = new HashMap<>();

            // 计算子节点的入度和出度（只考虑子节点之间的边）
            for (Long childNodeId : childrenNodeIds) {
                // 计算入度：有多少条边指向这个子节点（来源节点也在子节点列表中）
                long inDegree = edgesByTo.getOrDefault(childNodeId, Collections.emptyList()).stream()
                        .filter(edge -> childrenNodeIds.contains(edge.getFromNodeId()))
                        .count();
                // 计算出度：有多少条边从这个子节点出发（目标节点也在子节点列表中）
                long outDegree = edgesByFrom.getOrDefault(childNodeId, Collections.emptyList()).stream()
                        .filter(edge -> childrenNodeIds.contains(edge.getEndNodeId()))
                        .count();
                childrenInDegree.put(childNodeId, inDegree);
                childrenOutDegree.put(childNodeId, outDegree);
            }

            // 获取开始节点（入度为0的子节点）和结束节点（出度为0的子节点）
            List<JobNode> startNodes = childrenNodes.stream()
                    .filter(v -> childrenInDegree.getOrDefault(v.getId(), 0L) == 0)
                    .toList();
            List<JobNode> endNodes = childrenNodes.stream()
                    .filter(v -> childrenOutDegree.getOrDefault(v.getId(), 0L) == 0)
                    .toList();

            logger.debug("[JobGroup] 处理任务组节点 - jobId: {}, 子节点数量: {}, 开始节点数量: {}, 结束节点数量: {}",
                    jobInfo.getId(), childrenNodes.size(), startNodes.size(), endNodes.size());

            // ⭐ 修复：如果子节点列表为空，记录警告并跳过处理
            if (childrenNodes.isEmpty()) {
                logger.warn("[JobGroup] 任务组节点没有子节点 - jobId: {}, jobName: {}",
                        jobInfo.getId(), jobInfo.getJobDesc());
                // 即使没有子节点，也要移除当前任务组节点，以避免阻塞执行
            } else {
                // ⭐ 修复：如果没有开始节点，记录警告并使用所有子节点作为开始节点
                if (startNodes.isEmpty()) {
                    logger.warn("[JobGroup] 任务组节点没有找到开始节点（入度为0的节点），使用所有子节点作为开始节点 - jobId: {}",
                            jobInfo.getId());
                    startNodes = new ArrayList<>(childrenNodes);
                }

                // ⭐ 修复：如果没有结束节点，记录警告并使用所有子节点作为结束节点
                if (endNodes.isEmpty()) {
                    logger.warn("[JobGroup] 任务组节点没有找到结束节点（出度为0的节点），使用所有子节点作为结束节点 - jobId: {}",
                            jobInfo.getId());
                    endNodes = new ArrayList<>(childrenNodes);
                }

                // 连接前置节点到开始节点
                for (JobNode startNode : startNodes) {
                    for (Long preNodeId : preNodeIds) {
                        JobEdge edge = new JobEdge();
                        edge.setFromNodeId(preNodeId);
                        edge.setEndNodeId(startNode.getId());
                        edge.setJobParentId(jobId);
                        addEdge(edge, edges, edgesByFrom, edgesByTo);
                        logger.debug("[JobGroup] 添加边: {} -> {} (前置节点到开始节点)", preNodeId, startNode.getId());
                    }
                }

                // 连接结束节点到后置节点
                for (JobNode endNode : endNodes) {
                    for (Long nextNodeId : nextNodeIds) {
                        JobEdge edge = new JobEdge();
                        edge.setFromNodeId(endNode.getId());
                        edge.setEndNodeId(nextNodeId);
                        edge.setJobParentId(jobId);
                        addEdge(edge, edges, edgesByFrom, edgesByTo);
                        logger.debug("[JobGroup] 添加边: {} -> {} (结束节点到后置节点)", endNode.getId(), nextNodeId);
                    }
                }

                // ⭐ 修复：递归处理子节点（可能包含嵌套的任务组节点）
                getNodeList(jobId, nodes, edges, childrenNodes, nodesByParent, edgesByFrom, edgesByTo);
            }

            // 移除当前节点
            nodes.removeIf(v -> v.getId().equals(currentNode.getId()));
            List<JobNode> parentNodes = nodesByParent.get(jobId);
            if (parentNodes != null) {
                parentNodes.removeIf(v -> v.getId().equals(currentNode.getId()));
                if (parentNodes.isEmpty()) {
                    nodesByParent.remove(jobId);
                }
            }
        }
    }

    private void removeEdges(List<Long> relatedNodeIds, Long currentNodeId, List<JobEdge> edges,
                             Map<Long, List<JobEdge>> edgesByFrom, Map<Long, List<JobEdge>> edgesByTo, boolean removeIncoming) {
        if (relatedNodeIds == null || relatedNodeIds.isEmpty()) {
            return;
        }
        Set<Long> relatedSet = new HashSet<>(relatedNodeIds);
        List<JobEdge> candidates = removeIncoming
                ? new ArrayList<>(edgesByTo.getOrDefault(currentNodeId, Collections.emptyList()))
                : new ArrayList<>(edgesByFrom.getOrDefault(currentNodeId, Collections.emptyList()));
        for (JobEdge edge : candidates) {
            boolean match = removeIncoming ? relatedSet.contains(edge.getFromNodeId())
                    : relatedSet.contains(edge.getEndNodeId());
            if (match) {
                removeEdge(edge, edges, edgesByFrom, edgesByTo);
            }
        }
    }

    private void removeEdge(JobEdge edge, List<JobEdge> edges, Map<Long, List<JobEdge>> edgesByFrom,
                            Map<Long, List<JobEdge>> edgesByTo) {
        edges.remove(edge);
        List<JobEdge> fromList = edgesByFrom.get(edge.getFromNodeId());
        if (fromList != null) {
            fromList.remove(edge);
            if (fromList.isEmpty()) {
                edgesByFrom.remove(edge.getFromNodeId());
            }
        }
        List<JobEdge> toList = edgesByTo.get(edge.getEndNodeId());
        if (toList != null) {
            toList.remove(edge);
            if (toList.isEmpty()) {
                edgesByTo.remove(edge.getEndNodeId());
            }
        }
    }

    private void addEdge(JobEdge edge, List<JobEdge> edges, Map<Long, List<JobEdge>> edgesByFrom,
                         Map<Long, List<JobEdge>> edgesByTo) {
        List<JobEdge> fromList = edgesByFrom.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>());
        boolean exists = fromList.stream().anyMatch(e -> e.getEndNodeId().equals(edge.getEndNodeId()));
        if (exists) {
            return;
        }
        fromList.add(edge);
        edgesByTo.computeIfAbsent(edge.getEndNodeId(), k -> new ArrayList<>()).add(edge);
        edges.add(edge);
    }

    /**
     * 设置执行任务ID
     *
     * @param jobId    任务ID
     * @param randomId 随机ID
     * @return 组合后的执行ID
     */
    public static String setExecuteJobId(Long jobId, String randomId) {
        return jobId + ":" + randomId;
    }

    /**
     * 更新任务运行状态映射
     *
     * @param jobId     任务ID
     * @param statusMap 状态映射
     */
    private void getJobStatusMap(Long jobId, Map<Long, List<Long>> statusMap) {
        JobInfo jobInfo = jobInfoMapper.selectById(jobId);
        if (jobInfo.getJobType() == 2) {
            logger.debug("[JobGroup] 初始化任务组状态映射 - jobId: {}", jobId);
            List<JobInfo> jobInfos = jobInfoMapper
                    .selectList(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
            for (JobInfo info : jobInfos) {
                getJobStatusMap(info.getId(), statusMap);
            }
            statusMap.put(jobId, jobInfos.stream().map(JobInfo::getId).toList());
            logger.debug("[JobGroup] 任务组状态映射初始化完成 - jobId: {}, 子任务数量: {}", jobId, jobInfos.size());
        }
    }

    /**
     * 设置节点状态
     *
     * @param statusMap 状态映射
     * @param jobId     任务ID
     * @param status    状态值
     * @param randomId  随机ID
     * @param parentId  父任务ID
     */
    private void setNodeStatus(Map<Long, List<Long>> statusMap, Long jobId, Integer status, String randomId,
                               Long parentId) {
        logger.info("[JobGroup] ========== 设置节点状态 ==========");
        logger.info("[JobGroup] jobId: {}, status: {}, randomId: {}, parentId: {}",
                jobId, status, randomId, parentId);
        logger.debug("[JobGroup] 设置节点状态 - jobId: {}, status: {}, randomId: {}, parentId: {}",
                jobId, status, randomId, parentId);

        for (Map.Entry<Long, List<Long>> entry : statusMap.entrySet()) {
            if (entry.getValue().contains(jobId)) {
                Message message = new Message();
                message.setJobId(jobId);
                message.setStatus(status);
                message.setRandomId(randomId);
                message.setParentJobId(parentId);

                String sessionKey = parentId + ":" + randomId;
                logger.info("[JobGroup] 准备发送SSE消息");
                logger.info("[JobGroup] sessionKey: {}", sessionKey);
                logger.info("[JobGroup] message: jobId={}, status={}, randomId={}, parentJobId={}",
                        message.getJobId(), message.getStatus(), message.getRandomId(), message.getParentJobId());

                // ⭐ 重要修复：对于最终状态（成功1或失败0），直接更新数据库，确保数据库状态准确
                // 即使SSE消息丢失，数据库状态也是正确的
                if (status == 1 || status == 0) {
                    try {
                        boolean updateSuccess = jobNodeService.updateNodeStatus(jobId, status);
                        if (updateSuccess) {
                            logger.info("[JobGroup] ✅ 已直接更新数据库节点状态 - jobId: {}, status: {}", jobId, status);
                        } else {
                            logger.warn("[JobGroup] ⚠️ 更新数据库节点状态失败 - jobId: {}, status: {}", jobId, status);
                        }
                    } catch (Exception e) {
                        logger.error("[JobGroup] ❌ 更新数据库节点状态异常 - jobId: {}, status: {}, 错误: {}",
                                jobId, status, e.getMessage(), e);
                        // 即使数据库更新失败，也继续发送SSE消息，让前端能够更新UI
                    }
                }

                // 使用SSE服务发送消息
                sseService.sendMessage(message);

                logger.info("[JobGroup] ✅ 已发送节点状态消息 - jobId: {}, status: {}, randomId: {}, sessionKey: {}",
                        jobId, status, randomId, sessionKey);
                logger.debug("[JobGroup] 发送节点状态消息 - jobId: {}, status: {}, randomId: {}", jobId, status, randomId);

                if (status == 0) {
                    JobInfo jobInfo = jobInfoMapper.selectById(jobId);
                    // 忽略任务失败
                    if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                        logger.warn("[JobGroup] 任务执行失败，策略为不忽略 - jobId: {}, 策略: {}",
                                jobId, jobInfo.getExecutorBlockStrategy());
                        statusMap.remove(entry.getKey());
                        setNodeStatus(statusMap, entry.getKey(), status, randomId, parentId);
                        throw new RuntimeException("任务运行失败");
                    } else {
                        logger.info("[JobGroup] 任务执行失败，策略为忽略 - jobId: {}, 策略: {}",
                                jobId, jobInfo.getExecutorBlockStrategy());
                        executeSuccessOrFailJob(statusMap, jobId, 0, randomId, parentId, entry);
                    }
                } else if (status == 1) {
                    logger.debug("[JobGroup] 任务执行成功 - jobId: {}", jobId);
                    executeSuccessOrFailJob(statusMap, jobId, status, randomId, parentId, entry);
                } else {
                    logger.debug("[JobGroup] 任务状态更新 - jobId: {}, status: {}", jobId, status);
                    setNodeStatus(statusMap, entry.getKey(), status, randomId, parentId);
                }
                return;
            }
        }
        logger.warn("[JobGroup] 未找到对应的状态映射 - jobId: {}", jobId);
    }

    /**
     * 处理任务成功或失败的情况
     *
     * @param statusMap 状态映射
     * @param jobId     任务ID
     * @param status    状态值
     * @param randomId  随机ID
     * @param parentId  父任务ID
     * @param entry     状态映射条目
     */
    private void executeSuccessOrFailJob(Map<Long, List<Long>> statusMap, Long jobId, Integer status, String randomId,
                                         Long parentId, Map.Entry<Long, List<Long>> entry) {
        List<Long> value = new ArrayList<>(entry.getValue());
        value.remove(jobId);
        statusMap.put(entry.getKey(), value);
        if (entry.getValue().isEmpty()) {
            logger.debug("[JobGroup] 任务组所有子任务完成 - parentJobId: {}, status: {}", entry.getKey(), status);
            statusMap.remove(entry.getKey());
            setNodeStatus(statusMap, entry.getKey(), status, randomId, parentId);
        } else {
            logger.debug("[JobGroup] 任务组部分子任务完成 - parentJobId: {}, 剩余子任务数量: {}", entry.getKey(), value.size());
        }
    }

    /**
     * 任务运行监听器
     *
     * 负责监听任务执行状态，等待任务完成
     */
    private class JobThreadListener implements Callable<String> {

        private final JobInfo jobInfo;

        private final JobNode node;

        private final String randomId;

        private final Map<Long, List<Long>> statusMap;

        private volatile boolean stop = false;

        private final int count;

        private XxlJobContext xxlJobContext;

        public JobThreadListener(XxlJobContext xxlJobContext,JobInfo jobInfo, JobNode node, String randomId, Map<Long, List<Long>> statusMap,
                                 int count) {
            this.xxlJobContext = xxlJobContext;
            this.jobInfo = jobInfo;
            this.node = node;
            this.randomId = randomId;
            this.statusMap = statusMap;
            this.count = count;
        }

        public void toStop() {
            this.stop = true;
        }

        @Override
        public String call() {
                int targetKey = jobInfo.getId().intValue();
            logger.info("[JobGroup] ========== JobThreadListener 开始监听任务 ==========");
            logger.info("[JobGroup] jobId: {}, targetKey: {}, randomId: {}", jobInfo.getId(), targetKey, randomId);
            int checkCount = 0;
            long startTime = System.currentTimeMillis();
            
            while (!stop) {
                checkCount++;
                
                // ⭐ 修复：使用全局 JobGroupTool.JOB_MAP 获取状态，而不是 xxlJobContext.getJobMap()
                // 原因：TriggerParam 通过 HTTP 序列化/反序列化后，XxlJobContext 是新对象实例
                // JobThread 更新的是新对象的 jobMap，而 JobThreadListener 监听的是原对象的 jobMap
                // 所以必须使用全局 Map 来共享状态
                if (JobGroupTool.JOB_MAP.containsKey(targetKey)) {
                    try {
                        Integer status = JobGroupTool.JOB_MAP.remove(targetKey); // 获取后立即移除，避免重复处理
                        long elapsed = System.currentTimeMillis() - startTime;
                        logger.info("[JobGroup] ✅✅✅ 检测到任务完成 ✅✅✅");
                        logger.info("[JobGroup] jobId: {}, status: {}, 检查次数: {}, 耗时: {}ms, JOB_MAP大小: {}", 
                                targetKey, status, checkCount, elapsed, JobGroupTool.JOB_MAP.size());
                        logger.info("[JobGroup] JOB_MAP当前内容: {}", JobGroupTool.JOB_MAP);
                        Pair<Integer, Integer> pair = new Pair<>(targetKey, status);
                        return handleJobCompletion(xxlJobContext, pair, jobInfo, node, randomId, statusMap, count);
                    } catch (Exception e) {
                        logger.error("[JobGroup] 任务完成处理异常 - jobId: {}, nodeId: {}, 异常信息: {}",
                                jobInfo.getId(), node.getId(), e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }
                
                // 每100次检查（约5秒）输出一次调试日志
                if (checkCount % 100 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    logger.info("[JobGroup] 任务监听中 - jobId: {}, 检查次数: {}, 已等待: {}ms, JOB_MAP大小: {}, JOB_MAP内容: {}", 
                            targetKey, checkCount, elapsed, JobGroupTool.JOB_MAP.size(), JobGroupTool.JOB_MAP);
                }
                
                // 检查超时（如果设置了超时时间）
                if (jobInfo.getExecutorTimeout() > 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed > jobInfo.getExecutorTimeout() * 1000L) {
                        logger.error("[JobGroup] ⚠️ 任务监听超时 - jobId: {}, 超时时间: {}秒, 已等待: {}ms", 
                                targetKey, jobInfo.getExecutorTimeout(), elapsed);
                        // 超时后设置失败状态
                        JobGroupTool.addJobData(targetKey, XxlJobContext.HANDLE_CODE_TIMEOUT);
                        return FAIL_COMPLETE;
                    }
                }
                
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.debug("[JobGroup] 任务监听线程被中断 - jobId: {}", jobInfo.getId());
                    break;
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            logger.warn("[JobGroup] ⚠️ JobThreadListener 监听结束但未检测到任务完成");
            logger.warn("[JobGroup] jobId: {}, 检查次数: {}, 已等待: {}ms, JOB_MAP大小: {}, JOB_MAP内容: {}", 
                    targetKey, checkCount, elapsed, JobGroupTool.JOB_MAP.size(), JobGroupTool.JOB_MAP);
            return SUCCESS;
        }
    }

    /**
     * 任务回滚触发器
     *
     * 负责处理任务开始和结束的回调
     */
    private class JobCallback implements ICallback<Long, String> {
        private final JobNode node;

        private final String randomId;

        private final XxlJobContext xxlJobContext;

        private final Map<Long, List<Long>> statusMap;

        private long runtime;

        public JobCallback(XxlJobContext xxlJobContext, JobNode node, String randomId,
                           Map<Long, List<Long>> statusMap) {
            this.node = node;
            this.randomId = randomId;
            this.xxlJobContext = xxlJobContext;
            this.statusMap = statusMap;
        }

        @Override
        public void begin(Long jobId) {
            logger.info("[JobGroup] ========== 任务开始执行 ==========");
            logger.info("[JobGroup] 任务ID: {}, 节点ID: {}, 父任务ID: {}", jobId, node.getId(),
                    node.getJobParentId());
            XxlJobHelper.log(xxlJobContext,
                    "========================================= 任务开始执行 =========================================");
            XxlJobHelper.log(xxlJobContext, "任务ID: {}, 节点ID: {}, 父任务ID: {}", jobId, node.getId(),
                    node.getJobParentId());
            setNodeStatus(statusMap, jobId, 2, randomId, node.getJobParentId());
            this.runtime = System.currentTimeMillis();
        }

        @Override
        public void result(boolean success, Long param, WorkResult<String> workResult) {
            long duration = System.currentTimeMillis() - runtime;
            logger.info("[JobGroup] ========== 任务执行完成 ==========");
            logger.info("[JobGroup] 任务ID: {}, 执行结果: {}, 运行时长: {}ms, 返回结果: {}",
                    param, success ? "成功" : "失败", duration, workResult.getResult());
            XxlJobHelper.log(xxlJobContext,
                    "========================================= 任务执行完成 =========================================");
            XxlJobHelper.log(xxlJobContext, "任务ID: {}, 执行结果: {}, 运行时长: {}ms", param, success ? "成功" : "失败", duration);
            XxlJobHelper.log(xxlJobContext, "返回结果: {}", workResult.getResult());
            runtime = System.currentTimeMillis() - runtime;

            Long jobId = this.node.getJobId();
            // 更新节点状态：成功=1（绿色），失败=0（红色）
            if (success) {
                setNodeStatus(statusMap, jobId, 1, randomId, node.getJobParentId());
                logger.info("[JobGroup] 任务执行成功，已更新节点状态为1（成功） - jobId: {}", jobId);
                // 更新数据库
                JobInfo jobInfo = jobInfoMapper.selectById(jobId);
                jobInfo.setRunTime(runtime);
                jobInfoMapper.updateById(jobInfo);
                XxlJobHelper.log(xxlJobContext, "任务执行成功，已更新数据库运行时间: {}ms", runtime);
            } else {
                setNodeStatus(statusMap, jobId, 0, randomId, node.getJobParentId());
                logger.error("[JobGroup] 任务执行失败，已更新节点状态为0（失败） - jobId: {}", jobId);
                XxlJobHelper.log(xxlJobContext, "任务执行失败，跳过数据库更新");
            }
        }
    }

    /**
     * 停止所有任务组
     *
     * 遍历所有正在执行的任务组，停止它们的执行
     */
//    public static void stopJobGroup() {
//        logger.info("[JobGroup] ========== 开始停止所有任务组 ==========");
//        STOP_MAP.forEach((k, v) -> {
//            String[] split = k.split(":");
//            Long jobId = Long.parseLong(split[0]);
//            XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().stopJobCompose(jobId);
//            logger.info("[JobGroup] 停止任务组 - jobId: {}, 执行ID: {}", jobId, k);
//        });
//        logger.info("[JobGroup] ========== 所有任务组停止完成 ==========");
//    }


}
