package com.cc.job.admin.task.handler;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.enums.ExecutorRouteStrategyEnum;
import com.cc.job.admin.task.netty.NettyClient;
import com.cc.job.admin.task.trigger.XxlJobTrigger;
import com.cc.job.admin.task.utils.I18nUtil;
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
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author xiaozhao
 */
@Component
@AllArgsConstructor
public class JobGroupXxlJob {

    final JobInfoService jobInfoService;

    final JobNodeService jobNodeService;

    final JobEdgeService jobEdgeService;

    final WebSocketServer webSocketServer;

    final JobInfoMapper jobInfoMapper;

    static final ConcurrentHashMap<String, WorkerWrapper<Long, String>> STOP_MAP = new ConcurrentHashMap<>();

    static final List<Pair<String, Boolean>> JOB_LIST = Collections.synchronizedList(new ArrayList<>());

    public static void removeJobMap(String jobId) {
        if (jobId != null) {
            JOB_LIST.removeIf(pair -> jobId.equals(pair.getKey()));
        }
    }

    public static void addJobMap(String jobId, Boolean isRunning) {
        JOB_LIST.add(Pair.of(jobId, isRunning));
    }

    public static void removeWorkWrapper(Long parentId, String randomId) {
        STOP_MAP.remove(setExecuteJobId(parentId, randomId));
    }

    public static WorkerWrapper<Long, String> getWorkWrapper(Long parentId, String randomId) {
        return STOP_MAP.get(setExecuteJobId(parentId, randomId));
    }


    static final Map<String, Map<Long, Set<Long>>> TASK_ID_MAP = new ConcurrentHashMap<>();

    //需要重新获取XxlJobContext解决线程问题，不然会出现日志文件错误添加的问题
    private static final InheritableThreadLocal<XxlJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<XxlJobContext>();


    @XxlJob("runJobGroupXxlJob")
    public void JobGroupXxlJob() {
        long jobId = XxlJobHelper.getJobId();
        String executeParam = XxlJobHelper.getJobParam();
        validateExecuteParam(executeParam);
        String randomId = "";
        try {
            randomId = String.valueOf(jobId).equalsIgnoreCase(executeParam) ? UUID.randomUUID().toString() : executeParam;
            JobInfo jobInfo = getJobInfoById(jobId);
            List<JobNode> nodes = getJobNodesByJobId(jobId);
            List<JobEdge> edges = getJobEdgesByJobId(jobId);

            Map<Long, List<Long>> statusMap = new HashMap<>();
            getJobStatusMap(jobId, statusMap);
            buildGraph(jobId, nodes, edges);
            Map<Long, List<JobNode>> nextMap = buildNextNode(nodes, edges);

            int avgTime = getAvgTime(nodes, jobInfo);
            CONTEXT_HOLDER.set(XxlJobContext.getXxlJobContext());
            List<WorkerWrapper<Long, String>> workerWrappers = buildWorkerWrappers(nodes, nextMap, randomId, avgTime, statusMap);
            List<Long> startNodes = getStartNodes(nodes);
            List<WorkerWrapper<Long, String>> startWrappers = getStartWrappers(workerWrappers, startNodes);
            WorkerWrapper<Long, String> startWork = createStartWorkWrapper(jobId, startWrappers);
            STOP_MAP.put(setExecuteJobId(jobId, randomId), startWork);
            Async.beginWork(jobInfo.getExecutorTimeout(), startWork);
        } catch (ExecutionException | InterruptedException e) {
            handleExecutionException(jobId, e);
        } finally {
            completeJob(jobId, randomId);
        }
    }

    private int getAvgTime(List<JobNode> nodes, JobInfo jobInfo) {
        List<JobInfo> jobInfos = getJobInfos(nodes);
        Integer executorTimeout = jobInfo.getExecutorTimeout();
        int size = nodes.size();
        for (JobInfo info : jobInfos) {
            if (info.getExecutorTimeout() > 0) {
                executorTimeout -= info.getExecutorTimeout();
                size--;
            }
            if (executorTimeout < 0) {
                throw new BusinessException("子任务运行时长超过任务组");
            }
        }
        if (size == 0) {
            size = 1;
        }
        return executorTimeout / size;
    }

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

    private List<JobNode> getJobNodesByJobId(long jobId) {
        return jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobId));
    }

    private List<JobEdge> getJobEdgesByJobId(long jobId) {
        return jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobId));
    }

    private List<Long> getStartNodes(List<JobNode> nodes) {
        return nodes.stream().filter(v -> v.getNodeInDegree().equals(0L)).map(JobNode::getId).toList();
    }

    private List<WorkerWrapper<Long, String>> getStartWrappers(List<WorkerWrapper<Long, String>> workerWrappers, List<Long> startNodes) {
        return workerWrappers.stream().filter(v -> startNodes.contains(Long.valueOf(v.getId()))).toList();
    }

    private void handleExecutionException(long jobId, Exception e) {
        XxlJobHelper.log("{}任务运行异常,message:{}", jobId, e.getMessage());
        throw new BusinessException(e.getMessage());
    }

    private void completeJob(long jobId, String randomId) {
        XxlJobHelper.log("{}任务运行完成", jobId);
        sendCompletionMessage(jobId, randomId);
        jobInfoMapper.stopJobCompose(jobId);
        TASK_ID_MAP.remove(setExecuteJobId(jobId, randomId));
        removeWorkWrapper(jobId, randomId);
    }

    private static WorkerWrapper<Long, String> createStartWorkWrapper(long jobId, List<WorkerWrapper<Long, String>> startWrappers) {
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
        message.setStatus(1);
        message.setRandomId(randomId);
        webSocketServer.sendInfo(message);
    }

    private List<WorkerWrapper<Long, String>> buildWorkerWrappers(List<JobNode> nodes, Map<Long, List<JobNode>> nextMap, String randomId, int avgTime, Map<Long, List<Long>> statusMap) {
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
                            int count = allWrappers.get(String.valueOf(node.getId())).getCount();
                            return executeJob(xxlJobContext, node, jobInfo, randomId, avgTime, statusMap, count);
                        }

                        @Override
                        public String defaultValue() {
                            try {
                                setNodeStatus(statusMap, node.getJobId(), 0, randomId, node.getJobParentId());
                            } catch (Exception e) {
                                logger.error(e.getMessage());
                            }
                            return "任务运行超时异常";
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
            List<WorkerWrapper<Long, String>> nextWorkers = result.stream().filter(v -> cNodeIds.contains(Long.valueOf(v.getId()))).toList();
            workerWrapper.next(nextWorkers.toArray(new WorkerWrapper[0]));
        }
        return result;
    }

    private String executeJob(XxlJobContext xxlJobContext, JobNode node, JobInfo jobInfo, String randomId, int avgTime, Map<Long, List<Long>> statusMap, int count) {

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
        //设置请求信息
        triggerParam.setReqBody(jobInfo.getReqBody());
        triggerParam.setReqHeader(jobInfo.getReqHeader());
        triggerParam.setReqType(jobInfo.getReqType());
        triggerParam.setReqUrl(jobInfo.getReqUrl());
        triggerParam.setXxlJobContext(xxlJobContext);


        String address = group.getRegistryList().get(0);

        NettyClient nettyClient = new NettyClient();
        try {
            nettyClient.init(address + "run");
            nettyClient.send(triggerParam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String result;
        Thread thread = null;
        JobThreadListener jobThreadListener = null;
        try {
            jobThreadListener = new JobThreadListener(jobInfo, node, randomId, statusMap, count);
            FutureTask<String> futureTask = new FutureTask<>(jobThreadListener);
            thread = new Thread(futureTask);
            thread.start();
            result = jobInfo.getExecutorTimeout() > 0 ? futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.MILLISECONDS) : futureTask.get(avgTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            jobThreadListener.toStop();
            thread.interrupt();
        }

        return result;
    }

    private String handleJobCompletion(Pair<String, Boolean> pair, JobInfo jobInfo, JobNode node, String randomId, Map<Long, List<Long>> statusMap, int count) {
        String key = pair.getKey();
        Long jobId = Long.valueOf(key.split(":")[0]);
        boolean success = pair.getValue();
        removeJobMap(setExecuteJobId(jobId, randomId));
        String res = "";
        if (success) {
            setNodeStatus(statusMap, jobId, 1, randomId, node.getJobParentId());
        } else {
            if (count < jobInfo.getExecutorFailRetryCount()) {
                logger.info(">>>>>>>>>>>>>>>>>任务组：{}，任务：{}，第{}次重试>>>>>>>>>>>>>>>>", jobId, node.getJobId(), count);
                return "FAIL_RETRY";
            }
            setNodeStatus(statusMap, jobId, 0, randomId, node.getJobParentId());
            throw new RuntimeException();
        }
        return res;
    }

    private Map<Long, List<JobNode>> buildNextNode(List<JobNode> nodes, List<JobEdge> edges) {
        Map<Long, List<JobNode>> result = new HashMap<>();
        for (JobNode node : nodes) {
            List<Long> nodeIds = edges.stream().filter(v -> v.getFromNodeId().equals(node.getId())).map(JobEdge::getEndNodeId).toList();
            List<JobNode> cNodes = nodes.stream().filter(v -> nodeIds.contains(v.getId())).toList();
            result.put(node.getId(), cNodes);
        }
        return result;
    }

    private void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edgeList) {
        boolean stop = true;
        Set<JobNode> resNodeList = new HashSet<>();
        while (stop) {
            stop = false;
            for (JobNode currentNode : nodes) {
                JobInfo jobInfo = jobInfoService.getById(currentNode.getJobId());
                if (jobInfo.getJobType() != 2 && Objects.equals(currentNode.getJobParentId(), jobId)) {
                    resNodeList.add(currentNode);
                    continue;
                }

                if (jobInfo.getJobType() == 2 && Objects.equals(currentNode.getJobParentId(), jobId)) {
                    stop = true;
                    List<JobEdge> collectEdges = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobInfo.getId()));
                    for (JobEdge edge : collectEdges) {
                        edge.setJobParentId(jobId);
                        edgeList.add(edge);
                    }

                    //得到当前节点的所有开始节点
                    List<Long> fromIds = edgeList.stream().filter(v -> v.getEndNodeId().equals(currentNode.getId())).map(JobEdge::getFromNodeId).toList();
                    //得到当前节点的所有孩子节点
                    List<JobNode> childrenNode = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobInfo.getId()));
                    // 得到孩子节点的开始节点
                    List<JobNode> startNodes = childrenNode.stream().filter(v -> v.getNodeInDegree() == 0).toList();
                    if (!fromIds.isEmpty()) {
                        for (JobNode taskNode : startNodes) {
                            for (Long fromId : fromIds) {
                                JobEdge taskEdge = new JobEdge();
                                taskEdge.setFromNodeId(fromId);
                                taskEdge.setEndNodeId(taskNode.getId());
                                taskEdge.setJobParentId(jobId);
                                edgeList.add(taskEdge);
                            }
                        }
                    }

                    List<Long> endIds = edgeList.stream().filter(v -> v.getFromNodeId().equals(currentNode.getId())).map(JobEdge::getEndNodeId).toList();

                    List<JobNode> childEndNodes = childrenNode.stream().filter(v -> v.getNodeOutDegree() == 0).toList();
                    if (!childEndNodes.isEmpty()) {
                        for (JobNode endNode : childEndNodes) {
                            for (Long endId : endIds) {
                                JobEdge edge = new JobEdge();
                                edge.setFromNodeId(endNode.getId());
                                edge.setEndNodeId(endId);
                                edge.setJobParentId(jobId);
                                edgeList.add(edge);
                            }
                        }
                    }

                    for (JobNode taskNode : childrenNode) {
                        taskNode.setJobParentId(jobId);
                        resNodeList.add(taskNode);
                    }

                    edgeList.removeIf(v -> (fromIds.contains(v.getFromNodeId()) && v.getEndNodeId().equals(currentNode.getId())) || (v.getFromNodeId().equals(currentNode.getId()) && endIds.contains(v.getEndNodeId())));
                }
            }
            nodes.clear();
            nodes.addAll(resNodeList);
            resNodeList.clear();
        }

        // 计算节点的出度和入度
        for (JobNode node : nodes) {
            node.setNodeInDegree(edgeList.stream().filter(v -> v.getEndNodeId().equals(node.getId())).count());
            node.setNodeOutDegree(edgeList.stream().filter(v -> v.getFromNodeId().equals(node.getId())).count());
        }
    }


    private static String setExecuteJobId(Long jobId, String randomId) {
        return jobId + ":" + randomId;
    }

    private void getJobStatusMap(Long jobId, Map<Long, List<Long>> statusMap) {
        JobInfo jobInfo = jobInfoService.getById(jobId);
        if (jobInfo.getJobType() == 2) {
            List<JobInfo> jobInfos = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
            for (JobInfo info : jobInfos) {
                getJobStatusMap(info.getId(), statusMap);
            }
            statusMap.put(jobId, jobInfos.stream().map(JobInfo::getId).toList());

        }
    }

    private void setNodeStatus(Map<Long, List<Long>> statusMap, Long jobId, Integer status, String randomId, Long parentId) {
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
                    if (!"DO_NOTHING".equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
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

    private void executeSuccessOrFailJob(Map<Long, List<Long>> statusMap, Long jobId, Integer status, String randomId, Long parentId, Map.Entry<Long, List<Long>> entry) {
        List<Long> value = new ArrayList<>(entry.getValue());
        value.remove(jobId);
        statusMap.put(entry.getKey(), value);
        if (entry.getValue().isEmpty()) {
            statusMap.remove(entry.getKey());
            setNodeStatus(statusMap, entry.getKey(), status, randomId, parentId);
        }
    }

    private class JobThreadListener implements Callable<String> {

        private final JobInfo jobInfo;

        private final JobNode node;

        private final String randomId;


        private final Map<Long, List<Long>> statusMap;

        private volatile boolean stop = false;

        private final int count;

        public JobThreadListener(JobInfo jobInfo, JobNode node, String randomId, Map<Long, List<Long>> statusMap, int count) {
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

    private class JobCallback implements ICallback<Long, String> {
        private final JobNode node;

        private final String randomId;

        private final XxlJobContext xxlJobContext;

        private final Map<Long, List<Long>> statusMap;

        public JobCallback(XxlJobContext xxlJobContext, JobNode node, String randomId, Map<Long, List<Long>> statusMap) {
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
        }

        @Override
        public void result(boolean success, Long param, WorkResult<String> workResult) {
            XxlJobHelper.log(xxlJobContext, ">>>>>>>>>>>>>>>>>>>>>任务运行完成:{}, 任务运行状态:{},运行结果:{}", param, success, workResult.getResult());
        }
    }

    // 停止所有任务
    public static void stopJobGroup() {
        Set<String> keySet = TASK_ID_MAP.keySet();
        for (String key : keySet) {
            String[] split = key.split(":");
            Long jobId = Long.parseLong(split[0]);
            XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().stopJobCompose(jobId);
        }
    }

    private static Logger logger = LoggerFactory.getLogger(JobGroupXxlJob.class);
}
