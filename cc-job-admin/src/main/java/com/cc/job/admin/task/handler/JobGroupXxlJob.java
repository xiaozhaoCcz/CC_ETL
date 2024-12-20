package com.cc.job.admin.task.handler;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.task.enums.TriggerTypeEnum;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.redis.StreamConsumer;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.thread.JobTriggerPoolHelper;
import com.cc.job.admin.task.websocket.WebSocketServer;
import com.cc.job.admin.task.websocket.model.Message;
import com.cc.tasktool.callback.ICallback;
import com.cc.tasktool.executor.Async;
import com.cc.tasktool.worker.WorkResult;
import com.cc.tasktool.wrapper.WorkerWrapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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

    final RedisTemplate redisTemplate;

    final JobInfoService taskInfoService;

    final JobNodeService taskNodeService;

    final JobEdgeService taskEdgeService;

    final WebSocketServer webSocketServer;

    final JobInfoMapper taskInfoMapper;

    static final ConcurrentHashMap<String, WorkerWrapper<Long, String>> stopMap = new ConcurrentHashMap<>();

    public static void removeWorkWrapper(Long parentId,String randomId) {
        stopMap.remove(setExecuteJobId(parentId,randomId));
    }

    public static WorkerWrapper<Long, String> getWorkWrapper(Long parentId,String randomId) {
        return stopMap.get(setExecuteJobId(parentId,randomId));
    }

    static final Map<String, Map<Long, Set<Long>>> taskIdMap = new ConcurrentHashMap<>();

    @XxlJob("runJobGroupXxlJob")
    public void runTaskRankXxlJob() {
        String executeParam = XxlJobHelper.getJobParam();
        validateExecuteParam(executeParam);

        String[] split = executeParam.split(":");
        long jobId = Long.parseLong(split[0]);
        String randomId = split[1];

        JobInfo taskInfo = getTaskInfoById(jobId);
        List<JobNode> nodes = getTaskNodesByJobId(jobId);
        List<JobEdge> edges = getTaskEdgesByJobId(jobId);

        setTaskIdMap(nodes, jobId, randomId);
        buildGraph(jobId, nodes, edges);
        Map<Long, List<JobNode>> nextMap = buildNextNode(nodes, edges);

        List<WorkerWrapper<Long, String>> workerWrappers = buildWorkerWrappers(nodes, nextMap, randomId);
        List<Long> startNodes = getStartNodes(nodes);
        List<WorkerWrapper<Long, String>> startWrappers = getStartWrappers(workerWrappers, startNodes);

        WorkerWrapper<Long, String> startWork = createStartWorkWrapper(jobId, startWrappers);
        stopMap.put(setExecuteJobId(jobId, randomId), startWork);
        redisTemplate.opsForValue().set(setExecuteJobId(jobId, randomId), "");

        try {
            Async.beginWork(taskInfo.getExecutorTimeout(), startWork);
        } catch (ExecutionException | InterruptedException e) {
            handleExecutionException(jobId, e);
        }

        completeJobExecution(jobId, randomId);
    }

    private void validateExecuteParam(String executeParam) {
        if (StringUtils.isBlank(executeParam)) {
            throw new BusinessException("executeParam is null");
        }
    }

    private JobInfo getTaskInfoById(long jobId) {
        return Optional.ofNullable(taskInfoService.getById(jobId))
                .orElseThrow(() -> new BusinessException("TaskInfo not found for jobId: " + jobId));
    }

    private List<JobNode> getTaskNodesByJobId(long jobId) {
        return taskNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, jobId));
    }

    private List<JobEdge> getTaskEdgesByJobId(long jobId) {
        return taskEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getTaskParentId, jobId));
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
        taskIdMap.remove(setExecuteJobId(jobId, randomId));
        stopMap.remove(setExecuteJobId(jobId, randomId));
        taskInfoMapper.stopTaskSet(jobId);
    }

    private static WorkerWrapper<Long, String> createStartWorkWrapper(long jobId, List<WorkerWrapper<Long, String>> startWrappers) {
        return new WorkerWrapper<Long, String>()
                .id(String.valueOf(jobId))
                .param(jobId)
                .worker((id, allWrappers) -> {
                    logger.info("{} start ... ", id);
                    XxlJobHelper.log("task:{} start",id);
                    return "";
                }).next(startWrappers.toArray(new WorkerWrapper[0]));
    }

    private void sendCompletionMessage(long jobId,String randomId) {
        Message message = new Message();
        message.setTaskId(jobId);
        message.setParentTaskId(jobId);
        message.setStatus(1);
        message.setRandomId(randomId);
        webSocketServer.sendInfo(message);
    }

    private List<WorkerWrapper<Long, String>> buildWorkerWrappers(List<JobNode> nodes, Map<Long, List<JobNode>> nextMap, String randomId) {
        List<WorkerWrapper<Long, String>> result = new ArrayList<>();
        List<Long> taskIds = nodes.stream().map(JobNode::getTaskId).toList();
        List<JobInfo> taskInfos = taskInfoService.listByIds(taskIds);
        final Map<Long, JobInfo> taskInfoMap = taskInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));
        for (JobNode node : nodes) {
            final JobInfo taskInfo = taskInfoMap.get(node.getTaskId());
            WorkerWrapper<Long, String> worker = new WorkerWrapper<Long, String>()
                    .id(String.valueOf(node.getId()))
                    .param(node.getTaskId())
                    .timeout(taskInfo.getExecutorTimeout())
                    .worker((taskId, allWrappers) -> executeTask(taskId, node, taskInfo,randomId))
                    .callback(new TaskCallback(node,randomId));
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

    private String executeTask(Long taskId, JobNode node, JobInfo taskInfo, String randomId) {
        JobTriggerPoolHelper.trigger(taskId.intValue(), TriggerTypeEnum.MANUAL, -1, null, randomId, "");
        String result;
        int count = 0;
        Label:
        while (true) {
            List<Pair<String, Boolean>> callbackRes = StreamConsumer.getCallbackRes();
            for (Pair<String, Boolean> pair : new ArrayList<>(callbackRes)) {
                if (pair.getKey().equals(setExecuteJobId(taskId,randomId))) {
                    handleTaskCompletion(pair, node, taskInfo, count,randomId);
                    result ="end task:"+taskId;
                    break Label;
                }
            }
        }
        return result;
    }

    private void handleTaskCompletion(Pair<String, Boolean> pair, JobNode node, JobInfo taskInfo, int count, String randomId) {
        String key = pair.getKey();
        Long taskId = Long.valueOf(key.split(":")[0]);
        boolean success = pair.getValue();
        logger.info("end task:{}, status:{}", taskId, success ? "success" : "fail");
        XxlJobHelper.log("task:{}, status:{}", taskId, success ? "success" : "fail");
        Message message = new Message();
        message.setTaskId(taskId);
        message.setParentTaskId(node.getTaskParentId());
        message.setRandomId(randomId);
        Long pValue = findParent(taskId, node.getTaskParentId(),randomId);

        if (success) {
            message.setStatus(1);
            webSocketServer.sendInfo(message);
            StreamConsumer.removeCallbackRes(setExecuteJobId(taskId,randomId));
            updateTaskIdMap(node, pValue,randomId);
        } else {
            StreamConsumer.removeCallbackRes(setExecuteJobId(taskId,randomId));
            if (++count <= taskInfo.getExecutorFailRetryCount()) {
                logger.info("Retrying task: {}, attempt: {}", taskId, count);
                XxlJobHelper.log("Retrying task: {}, attempt: {}", taskId, count);
            } else {
                message.setStatus(0);
                webSocketServer.sendInfo(message);
                if (taskIdMap.get(setExecuteJobId(node.getTaskParentId(),randomId)) != null && pValue != null) {
                    sendParentFailureMessage(node, pValue,randomId);
                }
                if (!"DO_NOTHING".equalsIgnoreCase(taskInfo.getExecutorBlockStrategy())) {
                    throw new RuntimeException();
                }
            }
        }
    }

    private void updateTaskIdMap(JobNode node, Long pValue, String randomId) {
        String executeJobId = setExecuteJobId(node.getTaskParentId(), randomId);
        if (!taskIdMap.get(executeJobId).isEmpty()) {
            Set<Long> ids = taskIdMap.get(executeJobId).get(pValue);
            ids.remove(node.getTaskId());
            taskIdMap.computeIfAbsent(executeJobId, k -> new HashMap<>())
                    .computeIfAbsent(pValue, k -> new HashSet<>())
                    .addAll(ids);
            if (ids.isEmpty()) {
                sendParentSuccessMessage(node, pValue,randomId);
            }
        }
    }

    private void sendParentSuccessMessage(JobNode node, Long pValue, String randomId) {
        Message pMessage = new Message();
        pMessage.setParentTaskId(node.getTaskParentId());
        pMessage.setTaskId(pValue);
        pMessage.setStatus(1);
        pMessage.setRandomId(randomId);
        webSocketServer.sendInfo(pMessage);
    }

    private void sendParentFailureMessage(JobNode node, Long pValue, String randomId) {
        Message pMessage = new Message();
        pMessage.setParentTaskId(node.getTaskParentId());
        pMessage.setTaskId(pValue);
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
                JobInfo taskInfo = taskInfoService.getById(currentNode.getTaskId());
                if (taskInfo.getJobType() != 2 && Objects.equals(currentNode.getTaskParentId(), jobId)) {
                    resNodeList.add(currentNode);
                    continue;
                }

                if (taskInfo.getJobType() == 2 && Objects.equals(currentNode.getTaskParentId(), jobId)) {
                    stop = true;
                    List<JobEdge> collectEdges = taskEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getTaskParentId, taskInfo.getId()));
                    for (JobEdge edge : collectEdges) {
                        edge.setTaskParentId(jobId);
                        edgeList.add(edge);
                    }

                    //得到当前节点的所有开始节点
                    List<Long> fromIds = edgeList.stream().filter(v -> v.getEndNodeId().equals(currentNode.getId())).map(JobEdge::getFromNodeId).toList();
                    // List<TaskNode> fromNodes = taskNodeService.listByIds(fromIds);
                    //得到当前节点的所有孩子节点
                    List<JobNode> childrenNode = taskNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, taskInfo.getId()));
                    // 得到孩子节点的开始节点
                    List<JobNode> startNodes = childrenNode.stream().filter(v -> v.getNodeInDegree() == 0).toList();
                    //            for (TaskNode fromNode : fromNodes) {
                    //                fromNode.setNodeOutDegree(fromNode.getNodeOutDegree() - 1 + startNodes.size());
                    //            }
                    //            for (TaskNode startNode : startNodes) {
                    //                startNode.setNodeInDegree(startNode.getNodeInDegree() + fromNodes.size());
                    //            }
                    if (!fromIds.isEmpty()) {
                        for (JobNode taskNode : startNodes) {
                            for (Long fromId : fromIds) {
                                JobEdge taskEdge = new JobEdge();
                                taskEdge.setFromNodeId(fromId);
                                taskEdge.setEndNodeId(taskNode.getId());
                                taskEdge.setTaskParentId(jobId);
                                edgeList.add(taskEdge);
                            }
                        }
                    }

                    List<Long> endIds = edgeList.stream().filter(v -> v.getFromNodeId().equals(currentNode.getId())).map(JobEdge::getEndNodeId).toList();

                    List<JobNode> childEndNodes = childrenNode.stream().filter(v -> v.getNodeOutDegree() == 0).toList();

//            if(!endIds.isEmpty()){
//                List<TaskNode> endNodes = taskNodeService.listByIds(endIds);
//                for (TaskNode endNode : endNodes) {
//                    endNode.setNodeInDegree(endNode.getNodeInDegree() - 1 + childEndNodes.size());
//                }
//            }

//            for (TaskNode childEndNode : childEndNodes) {
//                childEndNode.setNodeOutDegree(childEndNode.getNodeOutDegree() + endIds.size());
//            }

                    if (!childEndNodes.isEmpty()) {
                        for (JobNode endNode : childEndNodes) {
                            for (Long endId : endIds) {
                                JobEdge edge = new JobEdge();
                                edge.setFromNodeId(endNode.getId());
                                edge.setEndNodeId(endId);
                                edge.setTaskParentId(jobId);
                                edgeList.add(edge);
                            }
                        }
                    }

                    for (JobNode taskNode : childrenNode) {
                        taskNode.setTaskParentId(jobId);
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

    private void setTaskIdMap(List<JobNode> nodes, Long jobId, String randomId) {
        List<Long> ids = nodes.stream().map(JobNode::getTaskId).toList();
        List<JobInfo> taskInfos = taskInfoService.listByIds(ids);
        Map<Long, JobInfo> taskInfoMap = taskInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));

        for (JobNode node : nodes) {
            JobInfo taskInfo = taskInfoMap.get(node.getTaskId());
            ArrayList<Long> idList = new ArrayList<>();
            getTaskInfoIds(taskInfo.getId(), idList);
            taskIdMap.computeIfAbsent(setExecuteJobId(jobId,randomId), k -> new HashMap<>())
                    .computeIfAbsent(taskInfo.getId(), k -> new HashSet<>())
                    .addAll(idList);
        }
    }

    private static String setExecuteJobId(Long jobId,String randomId) {
        return jobId+":"+randomId;
    }

    private Long findParent(Long taskId, Long parentTaskId,String randomId) {
        Map<Long, Set<Long>> map = taskIdMap.get(setExecuteJobId(parentTaskId,randomId));
        Long valueToFind = null;
        //从map中拿到key
        for (Map.Entry<Long, Set<Long>> entry : map.entrySet()) {
            Set<Long> values = entry.getValue();
            if (values.contains(taskId)) {
                valueToFind = entry.getKey();
                break; // 如果只需要找到第一个匹配的键，找到后就可以退出循环
            }
        }
        return valueToFind;
    }

    public void getTaskInfoIds(Long jobId, List<Long> ids) {
        JobInfo taskInfo = taskInfoService.getById(jobId);
        if (taskInfo.getJobType() != 2) {
            ids.add(taskInfo.getId());
        } else {
            List<JobInfo> taskInfos = taskInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
            if (!taskInfos.isEmpty()) {
                taskInfos.stream().map(JobInfo::getId).forEach(childTaskId -> getTaskInfoIds(childTaskId, ids));
            }
        }
    }

    private class TaskCallback implements ICallback<Long, String> {
        private final JobNode node;

        private final String randomId;
        public TaskCallback(JobNode node, String randomId) {
            this.node = node;
            this.randomId = randomId;
        }

        @Override
        public void begin(Long taskId) {
            logger.info(" {} start ...", taskId);
            XxlJobHelper.log(" {} start ...", taskId);
            Message message = new Message();
            message.setTaskId(taskId);
            message.setStatus(2);
            message.setRandomId(randomId);
            message.setParentTaskId(node.getTaskParentId());
            webSocketServer.sendInfo(message);
            Long pValue = findParent(taskId, node.getTaskParentId(),randomId);
            if (taskIdMap.get(setExecuteJobId(node.getTaskParentId(),randomId)) != null && pValue != null) {
                Message pMessage = new Message();
                pMessage.setParentTaskId(node.getTaskParentId());
                pMessage.setTaskId(pValue);
                pMessage.setStatus(2);
                pMessage.setRandomId(randomId);
                webSocketServer.sendInfo(pMessage);
            }
        }

        @Override
        public void result(boolean success, Long param, WorkResult<String> workResult) {
            logger.info("job:{}, status:{},result:{}", param, success, workResult.getResult());
            XxlJobHelper.log("job:{}, status:{},result:{}", param, success, workResult.getResult());
        }
    }

    // 停止所有任务
    public static void stopJobGroup(){
        Set<String> keySet = taskIdMap.keySet();
        for (String key : keySet) {
            String[] split = key.split(":");
            Long jobId = Long.parseLong(split[0]);
            XxlJobAdminConfig.getAdminConfig().getTaskInfoMapper().stopTaskSet(jobId);
        }
    }

//    private List<Long> getNeighbors(Long node, List<TaskEdge> edges) {
//        return edges.stream().filter(v -> v.getFromNodeId().equals(node)).map(TaskEdge::getEndNodeId).toList();
//    }

//    private void executeTask(Map<Long, List<Long>> nodeMap, List<TaskNode> nodes, Map<Long, CompletableFuture<TaskNode>> futureMap, List<Long> taskIds) {
//        nodeMap.forEach((k, v) -> {
//            TaskNode currentNode = nodes.stream().filter(item -> item.getId().equals(k)).findFirst().orElse(null);
//            CompletableFuture<TaskNode> future = CompletableFuture.supplyAsync(() -> {
//                for (long d : v) {
//                    //阻塞等待
//                    try {
//                        if (futureMap.get(d) != null) {
//                            futureMap.get(d).get();
//                        }
//                    } catch (InterruptedException | ExecutionException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//                //执行任务
//                runT(currentNode, taskIds);
//                return currentNode;
//            });
//            futureMap.put(k, future);
//        });
//        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]));
//        try {
//            voidCompletableFuture.get();
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private void buildNodeMap(List<TaskNode> startNodes, Queue<Long> queue, List<Long> visited, List<TaskEdge> edges, Map<Long, List<Long>> nodeMap) {
//        startNodes.forEach(item -> {
//            queue.add(item.getId());
//            visited.add(item.getId());
//        });
//
//        ExecutorService executor = Executors.newFixedThreadPool(4);
//        List<Future<?>> futures = new ArrayList<>();
//
//        while (!queue.isEmpty()) {
//            List<Long> currentLevelNodes = new ArrayList<>();
//            while (!queue.isEmpty()) {
//                Long poll = queue.poll();
//                List<Long> collect = edges.stream().filter(v -> v.getEndNodeId().equals(poll)).map(TaskEdge::getFromNodeId).toList();
//                nodeMap.put(poll, collect);
//                currentLevelNodes.add(poll);
//            }
//            for (Long node : currentLevelNodes) {
//                futures.add(executor.submit(() -> {
//                    for (long neighbor : getNeighbors(node, edges)) {
//                        if (!visited.contains(neighbor)) {
//                            visited.add(neighbor);
//                            queue.add(neighbor);
//                        }
//                    }
//                }));
//            }
//            for (Future<?> future : futures) {
//                try {
//                    future.get();
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            futures.clear();
//        }
//    }

//    private void runT(TaskNode node, List<Long> taskIds) {
//        TaskInfo taskInfo = taskInfoService.getById(node.getTaskId());
//
//        Message message = new Message();
//        message.setParentTaskId(node.getTaskParentId());
//        message.setTaskId(node.getTaskId());
//
//        while (Boolean.TRUE.equals(stopMap.get(node.getTaskParentId()).getFirst())) {
//            XxlJobExecutor.removeJobThread(node.getTaskParentId().intValue(), "stop task" + node.getTaskParentId());
//            // 节点清空
//            List<ReturnT<String>> returnTList = JobCompleteHelper.getReturnTList();
//            returnTList.removeIf(res -> taskIds.contains(Long.valueOf(res.getContent())));
//            taskIdMap.remove(node.getTaskParentId());
//            webSocketServer.onClose(node.getTaskParentId());
//            throw new BusinessException(node.getTaskParentId() + " task stop");
//        }
//
//        JobTriggerPoolHelper.trigger(taskInfo.getId().intValue(), TriggerTypeEnum.MANUAL, -1, null, taskInfo.getExecutorParam(), "");
//
//        message.setStatus(2);
//        webSocketServer.sendInfo(message);
//
//            //往父亲节点发送消息
//        Long valueToFind = findParent(taskInfo.getId(), node.getTaskParentId());
//        if(valueToFind!=null){
//            Message pMessage = new Message();
//            pMessage.setParentTaskId(node.getTaskParentId());
//            pMessage.setTaskId(valueToFind);
//            pMessage.setStatus(2);
//            webSocketServer.sendInfo(pMessage);
//        }
//
//        Thread futureThread = null;
//        FutureTask<Boolean> futureTask = new FutureTask<Boolean>(() -> {
//            Label:
//            while (true) {
//                List<ReturnT<String>> returnTList = JobCompleteHelper.getReturnTList();
//                ArrayList<ReturnT<String>> returnTS = new ArrayList<>(returnTList);
//                for (ReturnT<String> res : returnTS) {
//                    if (res.getContent().equals(String.valueOf(node.getTaskId()))) {
//                        logger.info("{}end node{}>>>>>>>>>>> task:{}", res, node.getTaskId(), taskInfo.getJobDesc());
//                        if (res.getCode() == ReturnT.SUCCESS_CODE) {
//                            message.setStatus(1);
//                            webSocketServer.sendInfo(message);
//                            JobCompleteHelper.removeReturnT(res);
//                            Long pValue = findParent(node.getTaskId(),node.getTaskParentId());
//
//                            if(!taskIdMap.get(node.getTaskParentId()).isEmpty()){
//                                Set<Long> ids = taskIdMap.get(node.getTaskParentId()).get(pValue);
//                                ids.remove(node.getTaskId());
//                                taskIdMap.computeIfAbsent(node.getTaskParentId(),k->new HashMap<>())
//                                                .computeIfAbsent(pValue,k->new HashSet<>())
//                                                        .addAll(ids);
//                                if(ids.isEmpty()){
//                                    Message pMessage = new Message();
//                                    pMessage.setParentTaskId(node.getTaskParentId());
//                                    pMessage.setTaskId(pValue);
//                                    pMessage.setStatus(1);
//                                    webSocketServer.sendInfo(pMessage);
//                                }
//                            }
//
//                            break Label;
//                        } else {
//                            if ("DO_NOTHING".equalsIgnoreCase(taskInfo.getExecutorBlockStrategy())) {
//                                JobCompleteHelper.removeReturnT(res);
//                                break Label;
//                            } else {
//                                message.setStatus(0);
//                                webSocketServer.sendInfo(message);
//                                stopMap.put(node.getTaskParentId(), new Pair<>(true, node.getTaskId()));
//                                Long pValue = findParent(node.getTaskId(),node.getTaskParentId());
//                                if(taskIdMap.get(node.getTaskParentId())!=null&&pValue!=null) {
//                                    Message pMessage = new Message();
//                                    pMessage.setParentTaskId(node.getTaskParentId());
//                                    pMessage.setTaskId(pValue);
//                                    pMessage.setStatus(0);
//                                    webSocketServer.sendInfo(pMessage);
//                                }
//                                throw new RuntimeException();
//                            }
//                        }
//
//                    }
//                }
//            }
//            return true;
//        });
//
//        futureThread = new Thread(futureTask);
//        futureThread.start();
//
//        try {
//            Boolean tempResult = futureTask.get();
//        } catch (Exception e) {
//            throw new RuntimeException(e.getMessage());
//        }
//   }

    private static Logger logger = LoggerFactory.getLogger(JobGroupXxlJob.class);

}
