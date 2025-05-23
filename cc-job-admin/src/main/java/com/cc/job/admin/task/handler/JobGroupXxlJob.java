package com.cc.job.admin.task.handler;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.trigger.XxlJobTrigger;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.entity.*;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.websocket.WebSocketServer;
import com.cc.job.admin.task.websocket.model.Message;
import com.cc.tasktool.callback.ICallback;
import com.cc.tasktool.callback.IWorker;
import com.cc.tasktool.executor.Async;
import com.cc.tasktool.worker.WorkResult;
import com.cc.tasktool.wrapper.WorkerWrapper;
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

import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.cc.job.admin.task.handler.JobConstant.*;

/**
 * 任务组核心代码
 * TODO 可以考虑优化线程
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

    // 存储第一个WorkerWrapper，后续暂停任务需要
    static final ConcurrentHashMap<String, WorkerWrapper<Long, String>> STOP_MAP = new ConcurrentHashMap<>();

    // 所有的子任务集合
    static final List<Pair<String, Boolean>> JOB_LIST = Collections.synchronizedList(new ArrayList<>());

    public static void removeJobData(String jobId) {
        if (jobId != null) {
            JOB_LIST.removeIf(pair -> jobId.equals(pair.getKey()));
        }
    }

    public static void addJobData(String jobId, Boolean isRunning) {
        JOB_LIST.add(Pair.of(jobId, isRunning));
    }

    public static WorkerWrapper<Long, String> getWorkWrapper(Long parentId, String randomId) {
        return STOP_MAP.get(setExecuteJobId(parentId, randomId));
    }

    // 需要重新获取XxlJobContext解决线程问题，不然会出现日志文件错误添加的问题
    private static final InheritableThreadLocal<XxlJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    @XxlJob("runJobGroupXxlJob")
    public void jobGroupXxlJob() {
        long jobId = XxlJobHelper.getJobId();
        String executeParam = XxlJobHelper.getJobParam();
        // 验证执行参数
        validateExecuteParam(executeParam);
        String randomId = "";
        ExecutorService executorService = null;
        try {
            // 如果jobId和执行参数一致，则需要匹配一个uuid，如果不匹配则代表是platform页面执行，会携带一个随机id
            randomId = String.valueOf(jobId).equalsIgnoreCase(executeParam) ? UUID.randomUUID().toString() : executeParam;
            JobInfo jobInfo = getJobInfoById(jobId);
//            List<JobNode> nodes = getJobNodesByJobId(jobId);
//            List<JobEdge> edges = getJobEdgesByJobId(jobId);

            Map<Long, List<Long>> statusMap = new HashMap<>();
            // 设置初始状态，页面颜色提示
            getJobStatusMap(jobId, statusMap);
            // 将多节点任务或任务组构图
            List<JobNode> nodes = new ArrayList<>();
            List<JobEdge> edges = new ArrayList<>();
            getAllNodesAndEdges(jobId, nodes, edges);
            //构图
            buildGraph(jobId, nodes, edges);
            // 找到当前节点的next节点
            Map<Long, List<JobNode>> nextMap = buildNextNode(nodes, edges);

            // 设置任务的平均执行时间
            //int avgTime = getAvgTime(nodes, jobInfo);
            CONTEXT_HOLDER.set(XxlJobContext.getXxlJobContext());
            // 构造WorkerWrapper，实现任务的串并行执行
            List<WorkerWrapper<Long, String>> workerWrappers = buildWorkerWrappers(nodes, nextMap, randomId, statusMap);
            // 预测任务的运行时间
            getRuntime(workerWrappers, nodes, jobInfo.getExecutorTimeout(), jobId, randomId);
            //构造一个开始节点
            List<Long> startNodes = getStartNodes(nodes);
            List<WorkerWrapper<Long, String>> startWrappers = getStartWrappers(workerWrappers, startNodes);
            WorkerWrapper<Long, String> startWork = createStartWorkWrapper(jobId, startWrappers);
            STOP_MAP.put(setExecuteJobId(jobId, randomId), startWork);
            executorService = Executors.newFixedThreadPool(nodes.size() + 1);
            Async.beginWork(jobInfo.getExecutorTimeout(), executorService, startWork);
        } catch (ExecutionException | InterruptedException e) {
            handleExecutionException(jobId, e);
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
            completeJob(jobId, randomId);
        }
    }

    private void getRuntime(List<WorkerWrapper<Long, String>> workerWrappers, List<JobNode> nodes, long timeout, Long jobId, String randomId) {
        List<JobInfo> jobInfos = getJobInfos(nodes);
        Map<Long, JobInfo> jobInfoMap = new HashMap<>();
        final Map<Long, JobInfo> jobInfoDbMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));
        for (JobNode jobNode : nodes) {
            jobInfoMap.put(jobNode.getId(), jobInfoDbMap.get(jobNode.getJobId()));
        }
        String[][] nextRunTime = jobGroupUtils.getNextRunTime(workerWrappers, jobInfoMap, timeout, getStartNodes(nodes), jobId);
        Message message = new Message();
        message.setJobId(jobId);
        message.setParentJobId(jobId);
        message.setStatus(9);
        message.setRandomId(randomId);
        message.setResult(JSONUtil.toJsonStr(nextRunTime));
        webSocketServer.sendInfo(message);
    }


//    private int getAvgTime(List<JobNode> nodes, JobInfo jobInfo) {
//        List<JobInfo> jobInfos = getJobInfos(nodes);
//        Integer executorTimeout = jobInfo.getExecutorTimeout();
//        int size = nodes.size();
//        for (JobInfo info : jobInfos) {
//            if (info.getExecutorTimeout() > 0) {
//                executorTimeout -= info.getExecutorTimeout();
//                size--;
//            }
//            if (executorTimeout < 0) {
//                throw new BusinessException("子任务运行时长超过任务组");
//            }
//        }
//        if (size == 0) {
//            size = 1;
//        }
//        return executorTimeout / size;
//    }

    private List<JobInfo> getJobInfos(List<JobNode> nodes) {
        List<Long> jobIds = nodes.stream().map(JobNode::getJobId).toList();
        return jobInfoService.listByIds(jobIds);
    }

    private void validateExecuteParam(String executeParam) {
        if (StringUtils.isBlank(executeParam)) {
            throw new BusinessException("executeParam is null");
        }
    }

    private JobInfo getJobInfoById(long jobId) {
        return Optional.ofNullable(jobInfoService.getById(jobId))
                .orElseThrow(() -> new BusinessException("TaskInfo not found for jobId: " + jobId));
    }

    private List<Long> getStartNodes(List<JobNode> nodes) {
        return nodes.stream().filter(v -> v.getNodeInDegree().equals(0L)).map(JobNode::getId).toList();
    }

    private List<WorkerWrapper<Long, String>> getStartWrappers(List<WorkerWrapper<Long, String>> workerWrappers,
                                                               List<Long> startNodes) {
        return workerWrappers.stream().filter(v -> startNodes.contains(Long.valueOf(v.getId()))).toList();
    }

    private void handleExecutionException(long jobId, Exception e) {
        XxlJobHelper.log("{}任务运行异常,message:{}", jobId, e.getMessage());
        logger.error(e.getMessage());
        throw new BusinessException(e.getMessage());
    }

    private void completeJob(long jobId, String randomId) {
        XxlJobHelper.log("{}任务运行完成", jobId);
        sendCompletionMessage(jobId, randomId);
        jobInfoMapper.stopJobCompose(jobId);
        STOP_MAP.remove(setExecuteJobId(jobId, randomId));
    }

    private static WorkerWrapper<Long, String> createStartWorkWrapper(long jobId,
                                                                      List<WorkerWrapper<Long, String>> startWrappers) {
        XxlJobContext xxlJobContext = CONTEXT_HOLDER.get();
        return new WorkerWrapper<Long, String>()
                .id(String.valueOf(jobId))
                .param(jobId)
                .worker((id, allWrappers) -> {
                    logger.info(">>>>>>>>>>>>>>>>>任务组：{}开始运行>>>>>>>>>>>>>>>> ", id);
                    XxlJobHelper.log(xxlJobContext, ">>>>>>>>>>>>>>>>>任务组：{}开始运行>>>>>>>>>>>>>>>>", id);
                    return "";
                }).next(startWrappers.toArray(new WorkerWrapper[0]));
    }

    private void sendCompletionMessage(long jobId, String randomId) {
        Message message = new Message();
        message.setJobId(jobId);
        message.setParentJobId(jobId);
        message.setStatus(5);
        message.setRandomId(randomId);
        webSocketServer.sendInfo(message);
    }

    /**
     * 构造WorkerWrapper，详情代码请看async tool
     *
     * @param nodes     当前节点
     * @param nextMap   节点的next节点
     * @param randomId  随机id，前端页面运行传递
     * @param statusMap 状态map
     * @return
     */
    private List<WorkerWrapper<Long, String>> buildWorkerWrappers(List<JobNode> nodes, Map<Long, List<JobNode>> nextMap,
                                                                  String randomId, Map<Long, List<Long>> statusMap) {
        List<WorkerWrapper<Long, String>> result = new ArrayList<>();
        List<JobInfo> jobInfos = getJobInfos(nodes);
        final Map<Long, JobInfo> jobInfoMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));
        XxlJobContext xxlJobContext = CONTEXT_HOLDER.get();
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
                            //当前wrapper
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
                                logger.error(e.getMessage());
                            }
                            return FAIL_COMPLETE;
                        }
                    })
                    .callback(new JobCallback(xxlJobContext, node, randomId, statusMap));
            result.add(worker);
        }

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
        return result;
    }

    /**
     * 执行任务的核心代码
     *
     * @param xxlJobContext jobContext，主要是将子任务日志写到主任务中
     * @param node          当前节点
     * @param jobInfo       当前节点对应的jobInfo
     * @param randomId      随机id，随机id非常重要，保证多页面运行时节点状态运行正常
     * @param statusMap     状态map
     * @param count         失败重试次数
     * @return 运行结果
     */
    private String executeJob(XxlJobContext xxlJobContext, JobNode node, JobInfo jobInfo, String randomId,
                              Map<Long, List<Long>> statusMap, int count) {
        //暂停任务执行
        pauseJob(jobInfo);

        //触发任务
        triggerJob(xxlJobContext, jobInfo, randomId);

        // 核心代码，创建一个线程用来监听任务是否运行完成
        return listenerJob(node, jobInfo, randomId, statusMap, count);
    }

    /**
     * 监听任务是否运行完成
     */
    private String listenerJob(JobNode node, JobInfo jobInfo, String randomId, Map<Long, List<Long>> statusMap, int count) {
        String result;
        Thread thread = null;
        JobThreadListener jobThreadListener = null;
        try {
            jobThreadListener = new JobThreadListener(jobInfo, node, randomId, statusMap, count);
            FutureTask<String> futureTask = new FutureTask<>(jobThreadListener);
            thread = new Thread(futureTask);
            thread.start();
            result = jobInfo.getExecutorTimeout() > 0 ? futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.MILLISECONDS) : futureTask.get();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            jobThreadListener.toStop();
            thread.interrupt();
        }
        return result;
    }

    private void pauseJob(JobInfo jobInfo) {
        //重新获取jobInfo
        boolean isPause = jobInfo.getIsPause() == 1;
        //暂停任务，默认暂停任务5分钟
        long timeout = jobInfo.getExecutorTimeout() > 0 ? jobInfo.getExecutorTimeout() : 5 * 60 * 1000;
        long startTime = System.currentTimeMillis();
        while (isPause) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeout) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            JobInfo jobInfoModel = jobInfoService.getById(jobInfo.getId());
            isPause = jobInfoModel.getIsPause() == 1;
        }
    }

    private void triggerJob(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId) {
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
        ReturnT<String> returnT = XxlJobTrigger.runExecutor(triggerParam, address);
        if(returnT.getCode()!=ReturnT.SUCCESS_CODE){
            XxlJobHelper.log(xxlJobContext,returnT.getMsg());
            throw new BusinessException(returnT.getMsg());
        }
    }

    private String handleJobCompletion(Pair<String, Boolean> pair, JobInfo jobInfo, JobNode node, String randomId,
                                       Map<Long, List<Long>> statusMap, int count) {
        String key = pair.getKey();
        Long jobId = Long.valueOf(key.split(":")[0]);
        boolean success = pair.getValue();
        // 每次运行完需要重集合中删除节点
        removeJobData(setExecuteJobId(jobId, randomId));
        String res = "";
        if (success) {
            setNodeStatus(statusMap, jobId, 1, randomId, node.getJobParentId());
        } else {
            if (count < jobInfo.getExecutorFailRetryCount()) {
                logger.info(">>>>>>>>>>>>>>>>>任务组：{}，任务：{}，第{}次重试>>>>>>>>>>>>>>>>", jobId, node.getJobId(), count);
                return FAIL_RETRY;
            }
            setNodeStatus(statusMap, jobId, 0, randomId, node.getJobParentId());
        }
        return res;
    }

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

    private void getAllNodesAndEdges(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        List<JobNode> jobNodes = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobId));
        List<JobEdge> jobEdges = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobId));

        nodes.addAll(jobNodes);
        edges.addAll(jobEdges);

        List<JobInfo> childJobInfos = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
        for (JobInfo childJobInfo : childJobInfos) {
            if (childJobInfo.getJobType() == 2) {
                getAllNodesAndEdges(childJobInfo.getId(), nodes, edges);
            }
        }
    }

    private void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        List<JobNode> nodeList = nodes.stream().filter(v -> v.getJobParentId().equals(jobId)).toList();
        getNodeList(jobId, nodes, edges, nodeList);

        nodes.forEach(node -> {
            node.setNodeInDegree(edges.stream().filter(v -> v.getEndNodeId().equals(node.getId())).count());
            node.setNodeOutDegree(edges.stream().filter(v -> v.getFromNodeId().equals(node.getId())).count());
            node.setJobParentId(jobId);
        });
        edges.forEach(edge -> edge.setJobParentId(jobId));
    }

    private void getNodeList(Long jobId, List<JobNode> nodes, List<JobEdge> edges, List<JobNode> nodeList) {
        for (JobNode node : nodeList) {
            List<Long> preNodeIds = edges.stream().filter(v -> v.getEndNodeId().equals(node.getId())).map(JobEdge::getFromNodeId).toList();
            List<Long> nextNodeIds = edges.stream().filter(v -> v.getFromNodeId().equals(node.getId())).map(JobEdge::getEndNodeId).toList();
            concatNode(jobId, node, preNodeIds, nextNodeIds, nodes, edges);
        }
    }

    /**
     * 将多维图像降唯
     *
     * @param jobId
     * @param currentNode
     * @param preNodeIds
     * @param nextNodeIds
     * @param nodes
     * @param edges
     */
    private void concatNode(Long jobId, JobNode currentNode, List<Long> preNodeIds, List<Long> nextNodeIds, List<JobNode> nodes, List<JobEdge> edges) {
        JobInfo jobInfo = jobInfoService.getById(currentNode.getJobId());
        if (jobInfo.getJobType() == 2) {
            edges.removeIf(v -> preNodeIds.contains(v.getFromNodeId()) && v.getEndNodeId().equals(currentNode.getId()));
            edges.removeIf(v -> nextNodeIds.contains(v.getEndNodeId()) && v.getFromNodeId().equals(currentNode.getId()));

            //得到开始节点
            //得到当前节点的所有孩子节点
            List<JobNode> childrenNodes = nodes.stream().filter(v -> v.getJobParentId().equals(jobInfo.getId())).toList();
            // 得到孩子节点的开始节点
            List<JobNode> startNodes = childrenNodes.stream().filter(v -> v.getNodeInDegree() == 0).toList();
            List<JobNode> endNodes = childrenNodes.stream().filter(v -> v.getNodeOutDegree() == 0).toList();

            for (JobNode startNode : startNodes) {
                for (Long preNodeId : preNodeIds) {
                    JobEdge edge = new JobEdge();
                    edge.setFromNodeId(preNodeId);
                    edge.setEndNodeId(startNode.getId());
                    edge.setJobParentId(jobId);
                    edges.add(edge);
                }
            }

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

            nodes.removeIf(v -> v.getId().equals(currentNode.getId()));
        }
    }

    public static String setExecuteJobId(Long jobId, String randomId) {
        return jobId + ":" + randomId;
    }

    /**
     * 更新任务运行状态
     */
    private void getJobStatusMap(Long jobId, Map<Long, List<Long>> statusMap) {
        JobInfo jobInfo = jobInfoService.getById(jobId);
        if (jobInfo.getJobType() == 2) {
            List<JobInfo> jobInfos = jobInfoService
                    .list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
            for (JobInfo info : jobInfos) {
                getJobStatusMap(info.getId(), statusMap);
            }
            statusMap.put(jobId, jobInfos.stream().map(JobInfo::getId).toList());
        }
    }

    private void setNodeStatus(Map<Long, List<Long>> statusMap, Long jobId, Integer status, String randomId,
                               Long parentId) {
        for (Map.Entry<Long, List<Long>> entry : statusMap.entrySet()) {
            if (entry.getValue().contains(jobId)) {
                Message message = new Message();
                message.setJobId(jobId);
                message.setStatus(status);
                message.setRandomId(randomId);
                message.setParentJobId(parentId);
                webSocketServer.sendInfo(message);

                if (status == 0) {
                    JobInfo jobInfo = jobInfoService.getById(jobId);
                    // 忽略任务失败
                    if (!DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                        statusMap.remove(entry.getKey());
                        setNodeStatus(statusMap, entry.getKey(), status, randomId, parentId);
                        throw new RuntimeException("任务运行失败");
                    } else {
                        executeSuccessOrFailJob(statusMap, jobId, 1, randomId, parentId, entry);
                    }
                } else if (status == 1) {
                    executeSuccessOrFailJob(statusMap, jobId, status, randomId, parentId, entry);
                } else {
                    setNodeStatus(statusMap, entry.getKey(), status, randomId, parentId);
                }
                return;
            }
        }
    }

    private void executeSuccessOrFailJob(Map<Long, List<Long>> statusMap, Long jobId, Integer status, String randomId,
                                         Long parentId, Map.Entry<Long, List<Long>> entry) {
        List<Long> value = new ArrayList<>(entry.getValue());
        value.remove(jobId);
        statusMap.put(entry.getKey(), value);
        if (entry.getValue().isEmpty()) {
            statusMap.remove(entry.getKey());
            setNodeStatus(statusMap, entry.getKey(), status, randomId, parentId);
        }
    }

    /**
     * 任务运行监听器
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
                // 从任务集合中遍历
                for (Pair<String, Boolean> pair : new ArrayList<>(JOB_LIST)) {
                    if (pair.getKey().equals(setExecuteJobId(jobInfo.getId(), randomId))) {
                        try {
                            return handleJobCompletion(pair, jobInfo, node, randomId, statusMap, count);
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return "";
        }
    }

    /**
     * 任务回滚触发器
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
            logger.info(">>>>>>>>>>>>>>>>>>>>>任务：{}开始运行>>>>>>>>>>>>>>>>>>>>>", jobId);
            XxlJobHelper.log(xxlJobContext, ">>>>>>>>>>>>>>>>>>>>>任务：{}开始运行>>>>>>>>>>>>>>>>>>>>>", jobId);
            setNodeStatus(statusMap, jobId, 2, randomId, node.getJobParentId());
            this.runtime = System.currentTimeMillis();
        }

        @Override
        public void result(boolean success, Long param, WorkResult<String> workResult) {
            XxlJobHelper.log(xxlJobContext, ">>>>>>>>>>>>>>>>>>>>>任务运行完成:{}, 任务运行状态:{},运行结果:{}", param, success,
                    workResult.getResult());
            runtime = System.currentTimeMillis() - runtime;
            //更新数据库
            if (success) {
                Long jobId = this.node.getJobId();
                JobInfo jobInfo = jobInfoMapper.selectById(jobId);
                jobInfo.setRunTime(runtime);
                jobInfoMapper.updateById(jobInfo);
            }
        }
    }

    // 停止所有任务
    public static void stopJobGroup() {
        STOP_MAP.forEach((k, v) -> {
            String[] split = k.split(":");
            Long jobId = Long.parseLong(split[0]);
            XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().stopJobCompose(jobId);
            logger.info(">>>>>>>>>停止任务{}", k);
        });
    }

    private static final Logger logger = LoggerFactory.getLogger(JobGroupXxlJob.class);
}
