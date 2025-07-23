package com.cc.job.admin.task.handler;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.executor.Async;
import com.cc.job.admin.task.executor.callback.ICallback;
import com.cc.job.admin.task.executor.callback.IWorker;
import com.cc.job.admin.task.executor.worker.WorkResult;
import com.cc.job.admin.task.executor.wrapper.WorkerWrapper;
import com.cc.job.admin.task.trigger.XxlJobTrigger;
import com.cc.job.admin.task.websocket.WebSocketServer;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.entity.*;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.websocket.model.Message;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.util.IpUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.cc.job.admin.task.handler.JobConstant.*;

/**
 * 任务组核心处理器
 * 
 * 主要功能：
 * 1. 处理任务组的拓扑排序执行
 * 2. 管理任务间的依赖关系
 * 3. 监控任务执行状态
 * 4. 处理任务失败重试
 * 5. 支持任务暂停和恢复
 * 
 * @author xiaozhao
 */
@Component
@RequiredArgsConstructor
public class JobGroupXxlJob {

    @Value("${server.port}")
    private int port;

    final JobInfoService jobInfoService;

    final JobNodeService jobNodeService;

    final JobEdgeService jobEdgeService;

    final WebSocketServer webSocketServer;

    final JobInfoMapper jobInfoMapper;

    final JobGroupUtils jobGroupUtils;

    /**
     * 存储WorkerWrapper，用于后续暂停任务操作
     * Key: jobId:randomId, Value: WorkerWrapper列表
     */
    static final ConcurrentHashMap<String, List<WorkerWrapper<Long, String>>> STOP_MAP = new ConcurrentHashMap<>();

    /**
     * 存储任务执行状态映射
     * Key: jobId:randomId, Value: 是否成功执行
     */
    static final Map<String, Boolean> JOB_MAP = new ConcurrentHashMap<>();

    /**
     * 移除任务数据
     * 
     * @param jobId 任务ID
     */
    public static void removeJobData(String jobId) {
        if (jobId != null) {
            JOB_MAP.remove(jobId);
            logger.debug("[JobGroup] 移除任务数据 - jobId: {}, 当前任务总数: {}", jobId, JOB_MAP.size());
        }
    }

    /**
     * 添加任务数据
     * 
     * @param jobId     任务ID
     * @param isRunning 是否正在运行
     */
    public static void addJobData(String jobId, Boolean isRunning) {
        JOB_MAP.put(jobId, isRunning);
        logger.debug("[JobGroup] 添加任务数据 - jobId: {}, 运行状态: {}, 当前总数: {}",
                jobId, isRunning ? "运行中" : "已完成", JOB_MAP.size());
    }

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
            logger.info("[JobGroup] 任务信息获取成功 - 任务名称: {}, 任务类型: {}, 执行器超时时间: {}ms",
                    jobInfo.getJobDesc(), jobInfo.getJobType(), jobInfo.getExecutorTimeout());

            Map<Long, List<Long>> statusMap = new HashMap<>();
            // 设置初始状态，页面颜色提示
            getJobStatusMap(jobId, statusMap);
            logger.debug("[JobGroup] 初始化状态映射 - jobId: {}, 状态映射大小: {}", jobId, statusMap.size());

            // 获取所有节点和边信息
            List<JobNode> nodes = new ArrayList<>();
            List<JobEdge> edges = new ArrayList<>();
            getAllNodesAndEdges(jobId, nodes, edges);
            logger.info("[JobGroup] 节点和边信息获取完成 - jobId: {}, 节点数量: {}, 边数量: {}",
                    jobId, nodes.size(), edges.size());

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
        logger.debug("[JobGroup] 开始计算任务运行时间 - jobId: {}, 节点数量: {}, 超时时间: {}ms", jobId, nodes.size(), timeout);

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
        // 使用消息队列服务发送消息，提高响应速度
        webSocketServer.sendInfo(message);
        logger.debug("[JobGroup] 运行时间计算完成并发送消息 - jobId: {}, randomId: {}", jobId, randomId);
    }

    /**
     * 获取任务信息列表
     */
    private List<JobInfo> getJobInfos(List<JobNode> nodes) {
        List<Long> jobIds = nodes.stream().map(JobNode::getJobId).toList();
        return jobInfoService.listByIds(jobIds);
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
        JobInfo jobInfo = jobInfoService.getById(jobId);
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

        // 关闭WebSocket连接
        webSocketServer.onClose(setExecuteJobId(jobId, randomId));
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
        // 使用Spring WebSocket服务发送消息
        webSocketServer.sendInfo(message);
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
        for (WorkerWrapper<Long, String> workerWrapper : result) {
            String id = workerWrapper.getId();
            List<JobNode> taskNodes = nextMap.get(Long.valueOf(id));
            if (taskNodes.isEmpty()) {
                continue;
            }
            List<Long> cNodeIds = taskNodes.stream().map(JobNode::getId).toList();
            List<WorkerWrapper<Long, String>> nextWorkers = result.stream()
                    .filter(v -> cNodeIds.contains(Long.valueOf(v.getId()))).toList();
            workerWrapper.next(nextWorkers.toArray(new WorkerWrapper[0]));
        }

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
        logger.info("[JobGroup] 开始执行任务 - jobId: {}, nodeId: {}, 重试次数: {}, 任务名称: {}",
                jobInfo.getId(), node.getId(), count, jobInfo.getJobDesc());

        // 暂停任务执行
        pauseJob(jobInfo);

        // 触发任务
        triggerJob(xxlJobContext, jobInfo, randomId);

        // 核心代码，创建一个线程用来监听任务是否运行完成
        return listenerJob(node, jobInfo, randomId, statusMap, count);
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
    private String listenerJob(JobNode node, JobInfo jobInfo, String randomId, Map<Long, List<Long>> statusMap,
            int count) {
        String result;
        Thread thread = null;
        JobThreadListener jobThreadListener = null;
        try {
            logger.debug("[JobGroup] 开始监听任务执行状态 - jobId: {}, nodeId: {}, 超时时间: {}ms",
                    jobInfo.getId(), node.getId(), jobInfo.getExecutorTimeout());
            jobThreadListener = new JobThreadListener(jobInfo, node, randomId, statusMap, count);
            FutureTask<String> futureTask = new FutureTask<>(jobThreadListener);
            thread = new Thread(futureTask);
            thread.start();
            result = jobInfo.getExecutorTimeout() > 0
                    ? futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.MILLISECONDS)
                    : futureTask.get();
            logger.info("[JobGroup] 任务监听完成 - jobId: {}, nodeId: {}, 执行结果: {}",
                    jobInfo.getId(), node.getId(), result);
        } catch (Exception e) {
            logger.error("[JobGroup] 任务监听异常 - jobId: {}, nodeId: {}, 异常信息: {}",
                    jobInfo.getId(), node.getId(), e.getMessage(), e);
            throw new RuntimeException(e);
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
        long timeout = jobInfo.getExecutorTimeout() > 0 ? jobInfo.getExecutorTimeout() : 5 * 60 * 1000;
        long startTime = System.currentTimeMillis();
        while (isPause) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeout) {
                logger.warn("[JobGroup] 任务暂停等待超时 - jobId: {}, 等待时长: {}ms", jobInfo.getId(), elapsed);
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                logger.error("[JobGroup] 任务暂停等待被中断 - jobId: {}", jobInfo.getId(), e);
                throw new RuntimeException(e);
            }
            JobInfo jobInfoModel = jobInfoService.getById(jobInfo.getId());
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

        JobGroup group = XxlJobAdminConfig.getAdminConfig().getJobGroupMapper().selectById(jobInfo.getJobGroup());
        // 2、init trigger-param
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId().intValue());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(randomId);
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(-1);
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        triggerParam.setBroadcastIndex(0);
        triggerParam.setBroadcastTotal(1);
        // 设置请求信息
        triggerParam.setReqBody(jobInfo.getReqBody());
        triggerParam.setReqHeader(jobInfo.getReqHeader());
        triggerParam.setReqType(jobInfo.getReqType());
        triggerParam.setReqUrl(jobInfo.getReqUrl());
        triggerParam.setXxlJobContext(xxlJobContext);
        // 得到本地的ip和host
        String ip = IpUtil.getIp();
        String adminAddress = String.format(ADMIN_ADDRESS, ip, port);
        triggerParam.setAddress(adminAddress);

        String address = group.getRegistryList().get(0);
        logger.debug("[JobGroup] 发送任务到执行器 - jobId: {}, 执行器地址: {}", jobInfo.getId(), address);

        ReturnT<String> returnT = XxlJobTrigger.runExecutor(triggerParam, address);
        if (returnT.getCode() != ReturnT.SUCCESS_CODE) {
            logger.error("[JobGroup] 任务触发失败 - jobId: {}, 错误信息: {}", jobInfo.getId(), returnT.getMsg());
            XxlJobHelper.log(xxlJobContext,
                    "========================================= 任务触发失败 =========================================");
            XxlJobHelper.log(xxlJobContext, "任务ID: {}, 错误信息: {}", jobInfo.getId(), returnT.getMsg());
            JobGroupXxlJob.addJobData(setExecuteJobId(jobInfo.getId(), randomId), false);
        } else {
            logger.debug("[JobGroup] 任务触发成功 - jobId: {}", jobInfo.getId());
            XxlJobHelper.log(xxlJobContext, "任务触发成功 - 任务ID: {}", jobInfo.getId());
        }
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
    private String handleJobCompletion(Pair<String, Boolean> pair, JobInfo jobInfo, JobNode node, String randomId,
            Map<Long, List<Long>> statusMap, int count) {
        String key = pair.getKey();
        Long jobId = Long.valueOf(key.split(":")[0]);
        boolean success = pair.getValue();
        // 每次运行完需要从集合中删除节点
        removeJobData(setExecuteJobId(jobId, randomId));
        if (success) {
            setNodeStatus(statusMap, jobId, 1, randomId, node.getJobParentId());
            logger.info("[JobGroup] 任务执行成功 - jobId: {}, 任务名称: {}", jobId, jobInfo.getJobDesc());
        } else {
            if (count < jobInfo.getExecutorFailRetryCount()) {
                logger.warn("[JobGroup] 任务执行失败，准备重试 - jobId: {}, nodeId: {}, 当前重试次数: {}, 最大重试次数: {}",
                        jobId, node.getJobId(), count, jobInfo.getExecutorFailRetryCount());
                return FAIL_RETRY;
            }
            logger.error("[JobGroup] 任务执行失败，已达到最大重试次数 - jobId: {}, nodeId: {}, 重试次数: {}",
                    jobId, node.getJobId(), count);
            setNodeStatus(statusMap, jobId, 0, randomId, node.getJobParentId());
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
     * 获取所有节点和边信息
     * 
     * @param jobId 任务ID
     * @param nodes 节点列表（输出参数）
     * @param edges 边列表（输出参数）
     */
    private void getAllNodesAndEdges(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        logger.debug("[JobGroup] 开始获取所有节点和边 - jobId: {}", jobId);

        // 获取直接子节点和边
        List<JobNode> jobNodes = jobNodeService
                .list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobId));
        List<JobEdge> jobEdges = jobEdgeService
                .list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobId));

        nodes.addAll(jobNodes);
        edges.addAll(jobEdges);

        logger.debug("[JobGroup] 获取直接子节点和边 - jobId: {}, 节点数量: {}, 边数量: {}", jobId, jobNodes.size(), jobEdges.size());

        // 递归获取子任务组的节点和边
        List<JobInfo> childJobInfos = jobInfoService
                .list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
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

        List<JobNode> nodeList = nodes.stream().filter(v -> v.getJobParentId().equals(jobId)).toList();
        getNodeList(jobId, nodes, edges, nodeList);

        // 计算节点的入度和出度
        nodes.forEach(node -> {
            node.setNodeInDegree(edges.stream().filter(v -> v.getEndNodeId().equals(node.getId())).count());
            node.setNodeOutDegree(edges.stream().filter(v -> v.getFromNodeId().equals(node.getId())).count());
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
    private void getNodeList(Long jobId, List<JobNode> nodes, List<JobEdge> edges, List<JobNode> nodeList) {
        logger.debug("[JobGroup] 处理节点列表 - jobId: {}, 节点数量: {}", jobId, nodeList.size());

        for (JobNode node : nodeList) {
            List<Long> preNodeIds = edges.stream().filter(v -> v.getEndNodeId().equals(node.getId()))
                    .map(JobEdge::getFromNodeId).toList();
            List<Long> nextNodeIds = edges.stream().filter(v -> v.getFromNodeId().equals(node.getId()))
                    .map(JobEdge::getEndNodeId).toList();
            logger.debug("[JobGroup] 处理节点 - nodeId: {}, 前置节点数量: {}, 后置节点数量: {}",
                    node.getId(), preNodeIds.size(), nextNodeIds.size());
            concatNode(jobId, node, preNodeIds, nextNodeIds, nodes, edges);
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
            List<JobNode> nodes, List<JobEdge> edges) {
        JobInfo jobInfo = jobInfoService.getById(currentNode.getJobId());
        if (jobInfo.getJobType() == 2) {
            // 移除与当前节点相关的边
            edges.removeIf(v -> preNodeIds.contains(v.getFromNodeId()) && v.getEndNodeId().equals(currentNode.getId()));
            edges.removeIf(
                    v -> nextNodeIds.contains(v.getEndNodeId()) && v.getFromNodeId().equals(currentNode.getId()));

            // 获取子任务组的节点
            List<JobNode> childrenNodes = nodes.stream().filter(v -> v.getJobParentId().equals(jobInfo.getId()))
                    .toList();
            // 获取开始节点和结束节点
            List<JobNode> startNodes = childrenNodes.stream().filter(v -> v.getNodeInDegree() == 0).toList();
            List<JobNode> endNodes = childrenNodes.stream().filter(v -> v.getNodeOutDegree() == 0).toList();

            // 连接前置节点到开始节点
            for (JobNode startNode : startNodes) {
                for (Long preNodeId : preNodeIds) {
                    JobEdge edge = new JobEdge();
                    edge.setFromNodeId(preNodeId);
                    edge.setEndNodeId(startNode.getId());
                    edge.setJobParentId(jobId);
                    edges.add(edge);
                }
            }

            // 连接结束节点到后置节点
            for (JobNode endNode : endNodes) {
                for (Long nextNodeId : nextNodeIds) {
                    JobEdge edge = new JobEdge();
                    edge.setFromNodeId(endNode.getId());
                    edge.setEndNodeId(nextNodeId);
                    edge.setJobParentId(jobId);
                    edges.add(edge);
                }
            }

            getNodeList(jobId, nodes, edges, childrenNodes);

            // 移除当前节点
            nodes.removeIf(v -> v.getId().equals(currentNode.getId()));
        }
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
        JobInfo jobInfo = jobInfoService.getById(jobId);
        if (jobInfo.getJobType() == 2) {
            logger.debug("[JobGroup] 初始化任务组状态映射 - jobId: {}", jobId);
            List<JobInfo> jobInfos = jobInfoService
                    .list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
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
        logger.debug("[JobGroup] 设置节点状态 - jobId: {}, status: {}, randomId: {}, parentId: {}",
                jobId, status, randomId, parentId);

        for (Map.Entry<Long, List<Long>> entry : statusMap.entrySet()) {
            if (entry.getValue().contains(jobId)) {
                Message message = new Message();
                message.setJobId(jobId);
                message.setStatus(status);
                message.setRandomId(randomId);
                message.setParentJobId(parentId);
                // 使用消息队列服务发送消息，提高响应速度
                webSocketServer.sendInfo(message);

                logger.debug("[JobGroup] 发送节点状态消息 - jobId: {}, status: {}, randomId: {}", jobId, status, randomId);

                if (status == 0) {
                    JobInfo jobInfo = jobInfoService.getById(jobId);
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
                        executeSuccessOrFailJob(statusMap, jobId, 1, randomId, parentId, entry);
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

        public JobThreadListener(JobInfo jobInfo, JobNode node, String randomId, Map<Long, List<Long>> statusMap,
                int count) {
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
            while (!stop) {
                String targetKey = setExecuteJobId(jobInfo.getId(), randomId);
                if (JOB_MAP.containsKey(targetKey)) {
                    try {
                        Pair<String, Boolean> pair = new Pair<>(targetKey, JOB_MAP.get(targetKey));
                        return handleJobCompletion(pair, jobInfo, node, randomId, statusMap, count);
                    } catch (Exception e) {
                        logger.error("[JobGroup] 任务完成处理异常 - jobId: {}, nodeId: {}, 异常信息: {}",
                                jobInfo.getId(), node.getId(), e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }
            }
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
            // 更新数据库
            if (success) {
                Long jobId = this.node.getJobId();
                JobInfo jobInfo = jobInfoMapper.selectById(jobId);
                jobInfo.setRunTime(runtime);
                jobInfoMapper.updateById(jobInfo);
                XxlJobHelper.log(xxlJobContext, "任务执行成功，已更新数据库运行时间: {}ms", runtime);
            } else {
                XxlJobHelper.log(xxlJobContext, "任务执行失败，跳过数据库更新");
            }
        }
    }

    /**
     * 停止所有任务组
     * 
     * 遍历所有正在执行的任务组，停止它们的执行
     */
    public static void stopJobGroup() {
        logger.info("[JobGroup] ========== 开始停止所有任务组 ==========");
        STOP_MAP.forEach((k, v) -> {
            String[] split = k.split(":");
            Long jobId = Long.parseLong(split[0]);
            XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().stopJobCompose(jobId);
            logger.info("[JobGroup] 停止任务组 - jobId: {}, 执行ID: {}", jobId, k);
        });
        logger.info("[JobGroup] ========== 所有任务组停止完成 ==========");
    }

    private static final Logger logger = LoggerFactory.getLogger(JobGroupXxlJob.class);
}
