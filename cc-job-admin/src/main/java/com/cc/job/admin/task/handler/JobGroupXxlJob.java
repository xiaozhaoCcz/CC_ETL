package com.cc.job.admin.task.handler;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.trigger.XxlJobTrigger;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.task.enums.TriggerTypeEnum;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobLogMapper;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.redis.StreamConsumer;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.thread.JobTriggerPoolHelper;
import com.cc.job.admin.task.websocket.WebSocketServer;
import com.cc.job.admin.task.websocket.model.Message;
import com.cc.tasktool.callback.ICallback;
import com.cc.tasktool.callback.IWorker;
import com.cc.tasktool.executor.Async;
import com.cc.tasktool.worker.WorkResult;
import com.cc.tasktool.wrapper.WorkerWrapper;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.util.GsonTool;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author xiaozhao
 */
@Component
@AllArgsConstructor
public class JobGroupXxlJob {

    final RedisTemplate redisTemplate;

    final JobInfoService jobInfoService;

    final JobNodeService jobNodeService;

    final JobEdgeService jobEdgeService;

    final WebSocketServer webSocketServer;

    final JobInfoMapper jobInfoMapper;

    final JobLogMapper jobLogMapper;

    static final ConcurrentHashMap<String, WorkerWrapper<Long, String>> STOP_MAP = new ConcurrentHashMap<>();

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
            JobInfo jobInfo = getTaskInfoById(jobId);
            List<JobNode> nodes = getTaskNodesByJobId(jobId);
            List<JobEdge> edges = getTaskEdgesByJobId(jobId);

            setJobIdMap(nodes, jobId, randomId);
            buildGraph(jobId, nodes, edges);
            Map<Long, List<JobNode>> nextMap = buildNextNode(nodes, edges);

            int avgTime = getAvgTime(nodes, jobInfo);
            CONTEXT_HOLDER.set(XxlJobContext.getXxlJobContext());
            List<WorkerWrapper<Long, String>> workerWrappers = buildWorkerWrappers(nodes, nextMap, randomId, avgTime);
            List<Long> startNodes = getStartNodes(nodes);
            List<WorkerWrapper<Long, String>> startWrappers = getStartWrappers(workerWrappers, startNodes);

            WorkerWrapper<Long, String> startWork = createStartWorkWrapper(jobId, startWrappers);
            STOP_MAP.put(setExecuteJobId(jobId, randomId), startWork);
            redisTemplate.opsForValue().set(setExecuteJobId(jobId, randomId), "");

            Async.beginWork(jobInfo.getExecutorTimeout(), startWork);
        } catch (ExecutionException | InterruptedException e) {
            handleExecutionException(jobId, e);
        } finally {
            completeJobExecution(jobId, randomId);
        }

    }

    private int getAvgTime(List<JobNode> nodes, JobInfo jobInfo) {
        List<JobInfo> jobInfos = getJobInfos(nodes);
        Integer executorTimeout = jobInfo.getExecutorTimeout();
        int size = nodes.size();
        for (JobInfo info : jobInfos) {
            if(info.getExecutorTimeout()>0){
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

    private JobInfo getTaskInfoById(long jobId) {
        return Optional.ofNullable(jobInfoService.getById(jobId))
                .orElseThrow(() -> new BusinessException("TaskInfo not found for jobId: " + jobId));
    }

    private List<JobNode> getTaskNodesByJobId(long jobId) {
        return jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobId));
    }

    private List<JobEdge> getTaskEdgesByJobId(long jobId) {
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

    private void completeJobExecution(long jobId, String randomId) {
        logger.info("{}任务运行完成", jobId);
        XxlJobHelper.log("{}任务运行完成", jobId);
        sendCompletionMessage(jobId, randomId);
        TASK_ID_MAP.remove(setExecuteJobId(jobId, randomId));
        STOP_MAP.remove(setExecuteJobId(jobId, randomId));
        redisTemplate.delete(setExecuteJobId(jobId, randomId));
        String recordId = StreamConsumer.messageMap.get(setExecuteJobId(jobId, randomId));
        if(StringUtils.isNotBlank(recordId)){
            redisTemplate.opsForStream().delete(StreamConsumer.TASK_SET_STREAM, recordId);
        }
        jobInfoMapper.stopTaskSet(jobId);

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

    private List<WorkerWrapper<Long, String>> buildWorkerWrappers(List<JobNode> nodes, Map<Long, List<JobNode>> nextMap, String randomId, int avgTime) {
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
                    .worker(new IWorker<>() {
                        @Override
                        public String action(Long jobId, Map<String, WorkerWrapper> allWrappers) {
                            return executeTask(xxlJobContext, jobId, node, jobInfo, randomId, avgTime);
                        }

                        @Override
                        public String defaultValue() {
                            return "任务运行超时异常";
                        }
                    })
                    .callback(new TaskCallback(xxlJobContext, node, randomId));
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

    private String executeTask(XxlJobContext xxlJobContext, Long jobId, JobNode node, JobInfo jobInfo, String randomId, int avgTime) {
        Long logId = XxlJobTrigger.trigger(jobId, TriggerTypeEnum.MANUAL, -1, null, randomId, "");
        String result;
        AtomicInteger count = new AtomicInteger(0);
        Thread thread = null;
        try {
            FutureTask<String> futureTask = new FutureTask<>(() -> {
                while (true) {
                    List<Pair<String, Boolean>> callbackRes = StreamConsumer.getCallbackRes();
                    for (Pair<String, Boolean> pair : new ArrayList<>(callbackRes)) {
                        if (pair.getKey().equals(setExecuteJobId(jobId, randomId))) {
                            try {
                                int res = handleTaskCompletion(xxlJobContext, pair, node, jobInfo, count, randomId, logId);
                                if (res == 1) {
                                    return String.valueOf(jobId);
                                }
                            } catch (Exception e) {
                                throw new BusinessException(e);
                            } finally {
                                String recordId = StreamConsumer.messageMap.get(setExecuteJobId(jobId, randomId));
                                if (recordId != null) {
                                    redisTemplate.opsForStream().delete(StreamConsumer.TASK_SET_STREAM, recordId);
                                }
                            }
                        }
                    }
                }
            });
            thread = new Thread(futureTask);
            thread.start();
            result = jobInfo.getExecutorTimeout() > 0 ? futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.MILLISECONDS) : futureTask.get(avgTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new BusinessException(e);
        } finally {
            thread.interrupt();
        }
        return result;
    }

    private int handleTaskCompletion(XxlJobContext xxlJobContext, Pair<String, Boolean> pair, JobNode node, JobInfo jobInfo, AtomicInteger count, String randomId, Long jobLogId) {
        String key = pair.getKey();
        Long jobId = Long.valueOf(key.split(":")[0]);
        boolean success = pair.getValue();
        //得到任务运行的日志结果
        JobLog jobLog = jobLogMapper.selectById(jobLogId);
        XxlJobHelper.log(xxlJobContext, ">>>>>>>>>>>>>>>>任务运行日志jonId:{}, handleCode:{},handleMsg:{}", jobLogId, jobLog.getHandleCode(), jobLog.getHandleMsg());
        Message message = new Message();
        message.setJobId(jobId);
        message.setParentJobId(node.getJobParentId());
        message.setRandomId(randomId);
        Long pValue = findParent(jobId, node.getJobParentId(), randomId);

        StreamConsumer.removeCallbackRes(setExecuteJobId(jobId, randomId));
//        redisTemplate.opsForStream().delete(StreamConsumer.TASK_SET_STREAM, key);
        int res = 1;
        if (success) {
            message.setStatus(1);
            webSocketServer.sendInfo(message);
            updateTaskIdMap(node, pValue, randomId);
        } else {
            if (count.incrementAndGet() <= jobInfo.getExecutorFailRetryCount()) {
                logger.info("Retrying task: {}, attempt: {}", jobId, count.get());
                XxlJobHelper.log(xxlJobContext, ">>>>>>>>>>>>>>>>>>>重试任务: {}, 重试次数: {}>>>>>>>>>>>>>>>", jobId, count);
                res = 0;
            } else {
                message.setStatus(0);
                webSocketServer.sendInfo(message);
                if (TASK_ID_MAP.get(setExecuteJobId(node.getJobParentId(), randomId)) != null && pValue != null) {
                    sendParentFailureMessage(node, pValue, randomId);
                }
                if (!"DO_NOTHING".equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                    throw new RuntimeException();
                }
            }
        }
        return res;
    }

    private void updateTaskIdMap(JobNode node, Long pValue, String randomId) {
        String executeJobId = setExecuteJobId(node.getJobParentId(), randomId);
        if (!TASK_ID_MAP.get(executeJobId).isEmpty()) {
            Set<Long> ids = TASK_ID_MAP.get(executeJobId).get(pValue);
            ids.remove(node.getJobId());
            TASK_ID_MAP.computeIfAbsent(executeJobId, k -> new HashMap<>())
                    .computeIfAbsent(pValue, k -> new HashSet<>())
                    .addAll(ids);
            if (ids.isEmpty()) {
                sendParentSuccessMessage(node, pValue, randomId);
            }
        }
    }

    private void sendParentSuccessMessage(JobNode node, Long pValue, String randomId) {
        Message pMessage = new Message();
        pMessage.setParentJobId(node.getJobParentId());
        pMessage.setJobId(pValue);
        pMessage.setStatus(1);
        pMessage.setRandomId(randomId);
        webSocketServer.sendInfo(pMessage);
    }

    private void sendParentFailureMessage(JobNode node, Long pValue, String randomId) {
        Message pMessage = new Message();
        pMessage.setParentJobId(node.getJobParentId());
        pMessage.setJobId(pValue);
        pMessage.setStatus(0);
        pMessage.setRandomId(randomId);
        webSocketServer.sendInfo(pMessage);
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
                    // List<TaskNode> fromNodes = jobNodeService.listByIds(fromIds);
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

    private void setJobIdMap(List<JobNode> nodes, Long jobId, String randomId) {
        List<JobInfo> jobInfos = getJobInfos(nodes);
        Map<Long, JobInfo> jobInfoMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));

        for (JobNode node : nodes) {
            JobInfo jobInfo = jobInfoMap.get(node.getJobId());
            ArrayList<Long> idList = new ArrayList<>();
            getTaskInfoIds(jobInfo.getId(), idList);
            TASK_ID_MAP.computeIfAbsent(setExecuteJobId(jobId, randomId), k -> new HashMap<>())
                    .computeIfAbsent(jobInfo.getId(), k -> new HashSet<>())
                    .addAll(idList);
        }
    }

    private static String setExecuteJobId(Long jobId, String randomId) {
        return jobId + ":" + randomId;
    }

    private Long findParent(Long jobId, Long parentTaskId, String randomId) {
        Map<Long, Set<Long>> map = TASK_ID_MAP.get(setExecuteJobId(parentTaskId, randomId));
        Long valueToFind = null;
        //从map中拿到key
        for (Map.Entry<Long, Set<Long>> entry : map.entrySet()) {
            Set<Long> values = entry.getValue();
            if (values.contains(jobId)) {
                valueToFind = entry.getKey();
                break; // 如果只需要找到第一个匹配的键，找到后就可以退出循环
            }
        }
        return valueToFind;
    }

    public void getTaskInfoIds(Long jobId, List<Long> ids) {
        JobInfo jobInfo = jobInfoService.getById(jobId);
        if (jobInfo.getJobType() != 2) {
            ids.add(jobInfo.getId());
        } else {
            List<JobInfo> jobInfos = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
            if (!jobInfos.isEmpty()) {
                jobInfos.stream().map(JobInfo::getId).forEach(childTaskId -> getTaskInfoIds(childTaskId, ids));
            }
        }
    }

    private class TaskCallback implements ICallback<Long, String> {
        private final JobNode node;

        private final String randomId;

        private final XxlJobContext xxlJobContext;

        public TaskCallback(XxlJobContext xxlJobContext, JobNode node, String randomId) {
            this.node = node;
            this.randomId = randomId;
            this.xxlJobContext = xxlJobContext;
        }

        @Override
        public void begin(Long jobId) {
            logger.info(">>>>>>>>>>>>>>>>>>>>>任务：{}开始运行>>>>>>>>>>>>>>>>>>>>>", jobId);
            XxlJobHelper.log(xxlJobContext, ">>>>>>>>>>>>>>>>>>>>>任务：{}开始运行>>>>>>>>>>>>>>>>>>>>>", jobId);
            Message message = new Message();
            message.setJobId(jobId);
            message.setStatus(2);
            message.setRandomId(randomId);
            message.setParentJobId(node.getJobParentId());
            webSocketServer.sendInfo(message);
            Long pValue = findParent(jobId, node.getJobParentId(), randomId);
            if (TASK_ID_MAP.get(setExecuteJobId(node.getJobParentId(), randomId)) != null && pValue != null) {
                Message pMessage = new Message();
                pMessage.setParentJobId(node.getJobParentId());
                pMessage.setJobId(pValue);
                pMessage.setStatus(2);
                pMessage.setRandomId(randomId);
                webSocketServer.sendInfo(pMessage);
            }
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
            XxlJobAdminConfig.getAdminConfig().getJobInfoMapper().stopTaskSet(jobId);
        }
    }

    private static Logger logger = LoggerFactory.getLogger(JobGroupXxlJob.class);
}
