package com.cc.job.task.jobhandler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.common.exception.BusinessException;
import com.cc.job.task.enums.TriggerTypeEnum;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskEdge;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.entity.TaskNode;
import com.cc.job.task.service.TaskEdgeService;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.service.TaskNodeService;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.cc.job.task.websocket.WebSocketServer;
import com.cc.job.task.websocket.model.Message;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.core.thread.TriggerCallbackThread;
import jakarta.annotation.Resource;
import kotlin.Pair;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class TaskRankXxlJob {

    final TaskInfoService taskInfoService;

    final TaskNodeService taskNodeService;

    final TaskEdgeService taskEdgeService;

    final WebSocketServer webSocketServer;

    static final ConcurrentHashMap<Long, Pair<Boolean, Long>> stopMap = new ConcurrentHashMap<Long, Pair<Boolean, Long>>();

    public static void processStopMap(Long parentId, boolean flag, Long id) {
        stopMap.put(parentId, new Pair<>(flag, id));
    }

    public static void removeStopMap(Long parentId) {
        stopMap.remove(parentId);
    }

    static final Map<Long,Map<Long,Set<Long>>> taskIdMap = new ConcurrentHashMap<>();

    @XxlJob("runTaskRankXxlJob")
    public void runTaskRankXxlJob() {
        //得到任务运行的参数
        String jobParam = XxlJobHelper.getJobParam();
        if (StringUtils.isBlank(jobParam)) {
            throw new BusinessException("jobParam is null");
        }

        long jobId = Long.parseLong(jobParam);

        List<TaskNode> nodes = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, jobId));
        List<TaskEdge> edges = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, jobId));

        setTaskIdMap(nodes, jobId);

        // 构图
        buildGraph(Long.valueOf(jobId), nodes, edges);

        List<TaskNode> startNodes = nodes.stream().filter(v -> v.getNodeInDegree().equals(0L)).toList();

        List<Long> visited = new CopyOnWriteArrayList<>();
        Queue<Long> queue = new ConcurrentLinkedQueue<>();

        Map<Long, List<Long>> nodeMap = new HashMap<>();
        Map<Long, CompletableFuture<TaskNode>> futureMap = new HashMap<>();

        buildNodeMap(startNodes, queue, visited, edges, nodeMap);

        final List<Long> taskIds = nodes.stream().map(TaskNode::getTaskId).toList();

        stopMap.put(Long.valueOf(jobId), new Pair<>(false, -1L));

        executeTask(nodeMap, nodes, futureMap, taskIds);
    }

    private void executeTask(Map<Long, List<Long>> nodeMap, List<TaskNode> nodes, Map<Long, CompletableFuture<TaskNode>> futureMap, List<Long> taskIds) {
        nodeMap.forEach((k, v) -> {
            TaskNode currentNode = nodes.stream().filter(item -> item.getId().equals(k)).findFirst().orElse(null);
            CompletableFuture<TaskNode> future = CompletableFuture.supplyAsync(() -> {
                for (long d : v) {
                    //阻塞等待
                    try {
                        if(futureMap.get(d)!=null){
                            futureMap.get(d).get();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                //执行任务
                runT(currentNode, taskIds);
                return currentNode;
            });
            futureMap.put(k, future);
        });
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]));
        try {
            voidCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildNodeMap(List<TaskNode> startNodes, Queue<Long> queue, List<Long> visited, List<TaskEdge> edges, Map<Long, List<Long>> nodeMap) {
        startNodes.forEach(item -> {
            queue.add(item.getId());
            visited.add(item.getId());
        });

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();

        while (!queue.isEmpty()) {
            List<Long> currentLevelNodes = new ArrayList<>();
            while (!queue.isEmpty()) {
                Long poll = queue.poll();
                List<Long> collect = edges.stream().filter(v -> v.getEndNodeId().equals(poll)).map(TaskEdge::getFromNodeId).toList();
                nodeMap.put(poll, collect);
                currentLevelNodes.add(poll);
            }
            for (Long node : currentLevelNodes) {
                futures.add(executor.submit(() -> {
                    for (long neighbor : getNeighbors(node, edges)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            futures.clear();
        }
    }

    private void buildGraph(Long jobId, List<TaskNode> nodes, List<TaskEdge> edgeList) {
        boolean stop = true;
        Set<TaskNode> resNodeList = new HashSet<>();
        while (stop) {
            stop = false;
            for (TaskNode currentNode : nodes) {
                TaskInfo taskInfo = taskInfoService.getById(currentNode.getTaskId());
                if (taskInfo.getJobType() != 2 && Objects.equals(currentNode.getTaskParentId(), jobId)) {
                    resNodeList.add(currentNode);
                    continue;
                }

                if (taskInfo.getJobType() == 2 && Objects.equals(currentNode.getTaskParentId(), jobId)) {
                    stop = true;
                    List<TaskEdge> collectEdges = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskInfo.getId()));
                    for (TaskEdge edge : collectEdges) {
                        edge.setTaskParentId(jobId);
                        edgeList.add(edge);
                    }

                    //得到当前节点的所有开始节点
                    List<Long> fromIds = edgeList.stream().filter(v -> v.getEndNodeId().equals(currentNode.getId())).map(TaskEdge::getFromNodeId).toList();
                    // List<TaskNode> fromNodes = taskNodeService.listByIds(fromIds);
                    //得到当前节点的所有孩子节点
                    List<TaskNode> childrenNode = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));
                    // 得到孩子节点的开始节点
                    List<TaskNode> startNodes = childrenNode.stream().filter(v -> v.getNodeInDegree() == 0).toList();
                    //            for (TaskNode fromNode : fromNodes) {
                    //                fromNode.setNodeOutDegree(fromNode.getNodeOutDegree() - 1 + startNodes.size());
                    //            }
                    //            for (TaskNode startNode : startNodes) {
                    //                startNode.setNodeInDegree(startNode.getNodeInDegree() + fromNodes.size());
                    //            }
                    if (!fromIds.isEmpty()) {
                        for (TaskNode taskNode : startNodes) {
                            for (Long fromId : fromIds) {
                                TaskEdge taskEdge = new TaskEdge();
                                taskEdge.setFromNodeId(fromId);
                                taskEdge.setEndNodeId(taskNode.getId());
                                taskEdge.setTaskParentId(jobId);
                                edgeList.add(taskEdge);
                            }
                        }
                    }

                    List<Long> endIds = edgeList.stream().filter(v -> v.getFromNodeId().equals(currentNode.getId())).map(TaskEdge::getEndNodeId).toList();

                    List<TaskNode> childEndNodes = childrenNode.stream().filter(v -> v.getNodeOutDegree() == 0).toList();

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
                        for (TaskNode endNode : childEndNodes) {
                            for (Long endId : endIds) {
                                TaskEdge edge = new TaskEdge();
                                edge.setFromNodeId(endNode.getId());
                                edge.setEndNodeId(endId);
                                edge.setTaskParentId(jobId);
                                edgeList.add(edge);
                            }
                        }
                    }

                    for (TaskNode taskNode : childrenNode) {
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
        for (TaskNode node : nodes) {
            node.setNodeInDegree(edgeList.stream().filter(v -> v.getEndNodeId().equals(node.getId())).count());
            node.setNodeOutDegree(edgeList.stream().filter(v -> v.getFromNodeId().equals(node.getId())).count());
        }
    }

    private void setTaskIdMap(List<TaskNode> nodes, Long jobId) {
        List<Long> ids = nodes.stream().map(TaskNode::getTaskId).toList();
        List<TaskInfo> taskInfos = taskInfoService.listByIds(ids);
        Map<Long, TaskInfo> taskInfoMap = taskInfos.stream().collect(Collectors.toMap(TaskInfo::getId, t -> t));

        for (TaskNode node : nodes) {
            TaskInfo taskInfo = taskInfoMap.get(node.getTaskId());
            ArrayList<Long> idList = new ArrayList<>();
            getTaskInfoIds(taskInfo.getId(),idList);
            taskIdMap.computeIfAbsent(jobId, k->new HashMap<>())
                    .computeIfAbsent(taskInfo.getId(), k->new HashSet<>())
                    .addAll(idList);
        }
    }



    private void runT(TaskNode node, List<Long> taskIds) {
        TaskInfo taskInfo = taskInfoService.getById(node.getTaskId());

        Message message = new Message();
        message.setParentTaskId(node.getTaskParentId());
        message.setTaskId(node.getTaskId());

        while (Boolean.TRUE.equals(stopMap.get(node.getTaskParentId()).getFirst())) {
            XxlJobExecutor.removeJobThread(node.getTaskParentId().intValue(), "stop task" + node.getTaskParentId());
            // 节点清空
            Vector<ReturnT<Long>> vector = TriggerCallbackThread.vector;
            vector.removeIf(res -> taskIds.contains(res.getContent()));
            taskIdMap.remove(node.getTaskParentId());
            webSocketServer.onClose(node.getTaskParentId());
            throw new BusinessException(node.getTaskParentId() + " task stop");
        }

        JobTriggerPoolHelper.trigger(taskInfo.getId().intValue(), TriggerTypeEnum.MANUAL, -1, null, taskInfo.getExecutorParam(), "");

        message.setStatus(2);
        webSocketServer.sendInfo(message);

            //往父亲节点发送消息
        Long valueToFind = findParent(taskInfo.getId(), node.getTaskParentId());
        if(valueToFind!=null){
            Message pMessage = new Message();
            pMessage.setParentTaskId(node.getTaskParentId());
            pMessage.setTaskId(valueToFind);
            pMessage.setStatus(2);
            webSocketServer.sendInfo(pMessage);
        }

        Thread futureThread = null;
        FutureTask<Boolean> futureTask = new FutureTask<Boolean>(() -> {
            Label:
            while (true) {
                Vector<ReturnT<Long>> vector = TriggerCallbackThread.vector;
                List<ReturnT<Long>> list = new ArrayList<>(vector);
                for (ReturnT<Long> res : list) {
                    if (res.getContent().equals(node.getTaskId())) {
                        logger.info("{}end node{}>>>>>>>>>>> task:{}", res, node.getTaskId(), taskInfo.getJobDesc());
                        if (res.getCode() == ReturnT.SUCCESS_CODE) {
                            message.setStatus(1);
                            webSocketServer.sendInfo(message);
                            TriggerCallbackThread.vector.remove(res);
                            Long pValue = findParent(node.getTaskId(),node.getTaskParentId());

                            if(!taskIdMap.get(node.getTaskParentId()).isEmpty()){
                                Set<Long> ids = taskIdMap.get(node.getTaskParentId()).get(pValue);
                                ids.remove(node.getTaskId());
                                taskIdMap.computeIfAbsent(node.getTaskParentId(),k->new HashMap<>())
                                                .computeIfAbsent(pValue,k->new HashSet<>())
                                                        .addAll(ids);
                                if(ids.isEmpty()){
                                    Message pMessage = new Message();
                                    pMessage.setParentTaskId(node.getTaskParentId());
                                    pMessage.setTaskId(pValue);
                                    pMessage.setStatus(1);
                                    webSocketServer.sendInfo(pMessage);
                                }
                            }

                            break Label;
                        } else {
                            if ("DO_NOTHING".equalsIgnoreCase(taskInfo.getExecutorBlockStrategy())) {
                                TriggerCallbackThread.vector.remove(res);
                                break Label;
                            } else {
                                message.setStatus(0);
                                webSocketServer.sendInfo(message);
                                stopMap.put(node.getTaskParentId(), new Pair<>(true, node.getTaskId()));
                                Long pValue = findParent(node.getTaskId(),node.getTaskParentId());
                                if(taskIdMap.get(node.getTaskParentId())!=null&&pValue!=null) {
                                    Message pMessage = new Message();
                                    pMessage.setParentTaskId(node.getTaskParentId());
                                    pMessage.setTaskId(pValue);
                                    pMessage.setStatus(0);
                                    webSocketServer.sendInfo(pMessage);
                                }
                                throw new RuntimeException();
                            }
                        }

                    }
                }
            }
            return true;
        });

        futureThread = new Thread(futureTask);
        futureThread.start();

        try {
            Boolean tempResult = futureTask.get();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private List<Long> getNeighbors(Long node, List<TaskEdge> edges) {
        return edges.stream().filter(v -> v.getFromNodeId().equals(node)).map(TaskEdge::getEndNodeId).toList();
    }

    private Long findParent(Long taskId,Long parentTaskId){
        Map<Long, Set<Long>> map = taskIdMap.get(parentTaskId);
        Long valueToFind = null;
        //从map中拿到key
        for (Map.Entry<Long, Set<Long>> entry: map.entrySet()) {
            Set<Long> values = entry.getValue();
            if (values.contains(taskId)) {
                valueToFind = entry.getKey();
                break; // 如果只需要找到第一个匹配的键，找到后就可以退出循环
            }
        }
        return valueToFind;
    }

    public void getTaskInfoIds(Long jobId,List<Long> ids){
        TaskInfo taskInfo = taskInfoService.getById(jobId);
        if(taskInfo.getJobType()!=2){
            ids.add(taskInfo.getId());
        }else{
            List<TaskInfo> taskInfos = taskInfoService.list(new LambdaQueryWrapper<TaskInfo>().eq(TaskInfo::getParentId, jobId));
            if(taskInfos.isEmpty()){
                return;
            }
            List<Long> childTaskIds = taskInfos.stream().map(TaskInfo::getId).toList();
            for (Long childTaskId : childTaskIds) {
                getTaskInfoIds(childTaskId,ids);
            }
        }
    }

    private static Logger logger = LoggerFactory.getLogger(TaskRankXxlJob.class);

}
