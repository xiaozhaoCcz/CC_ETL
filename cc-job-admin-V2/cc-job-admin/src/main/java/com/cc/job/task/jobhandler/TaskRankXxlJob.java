package com.cc.job.task.jobhandler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.common.exception.BusinessException;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskEdge;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.entity.TaskNode;
import com.cc.job.task.service.TaskEdgeService;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.service.TaskNodeService;
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
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    static final ConcurrentHashMap<Long, Boolean> flagMap = new ConcurrentHashMap<>();

    @XxlJob("runTaskRankXxlJob")
    public void runTaskRankXxlJob() {
        String jobId = XxlJobHelper.getJobParam();

        if (StringUtils.isBlank(jobId)) {
            throw new BusinessException("jobId is null");
        }
        List<TaskNode> nodes = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, jobId));
        List<TaskEdge> edges = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, jobId));

        flagMap.put(Long.valueOf(jobId), true);

        Set<TaskNode> resNodeList = new HashSet<>();
        while (flagMap.get(Long.valueOf(jobId))) {
            flagMap.put(Long.valueOf(jobId), false);
            for (TaskNode taskNode : nodes) {
                buildNode(taskNode, edges, resNodeList, Long.valueOf(jobId));
            }
            nodes.clear();
            nodes.addAll(resNodeList);
            resNodeList.clear();
        }

        List<TaskNode> startNodes = nodes.stream().filter(v -> v.getNodeInDegree().equals(0L)).toList();

        List<Long> visited = new CopyOnWriteArrayList<>();
        Queue<Long> queue = new ConcurrentLinkedQueue<>();

        Map<Long, List<Long>> nodeMap = new HashMap<>();

        Map<Long, CompletableFuture<TaskNode>> futureMap = new HashMap<>();

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

        System.out.println("nodes:" + nodes);
        System.out.println("edges:" + edges);
        System.out.println("nodeMap" + nodeMap);

        final List<Long> taskIds = nodes.stream().map(TaskNode::getTaskId).toList();
        stopMap.put(Long.valueOf(jobId), new Pair<>(false, -1L));
        nodeMap.forEach((k, v) -> {
            TaskNode currentNode = nodes.stream().filter(item -> item.getId().equals(k)).findFirst().orElse(null);
            CompletableFuture<TaskNode> future = CompletableFuture.supplyAsync(() -> {
                for (long d : v) {
                    //阻塞等待
                    try {
                        futureMap.get(d).get();
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

    private void buildNode(TaskNode currentNode, List<TaskEdge> edgeList, Set<TaskNode> resNodeList, Long jobId) {
        // 获取当前任务
        TaskInfo taskInfo = taskInfoService.getById(currentNode.getTaskId());
        if (taskInfo.getJobType() != 2 && Objects.equals(currentNode.getTaskParentId(), jobId)) {
            resNodeList.add(currentNode);
            return;
        }

        if (taskInfo.getJobType() == 2 && Objects.equals(currentNode.getTaskParentId(), jobId)) {
            flagMap.put(jobId, true);
            List<TaskEdge> collectEdges = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskInfo.getId()));
            for (TaskEdge edge : collectEdges) {
                edge.setTaskParentId(jobId);
                edgeList.add(edge);
            }

            //得到当前节点的所有开始节点
            List<Long> fromIds = edgeList.stream().filter(v -> v.getEndNodeId().equals(currentNode.getId())).map(TaskEdge::getFromNodeId).toList();

            List<TaskNode> fromNodes = taskNodeService.listByIds(fromIds);

            //得到当前节点的所有孩子节点
            List<TaskNode> childrenNode = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));

            // 得到孩子节点的开始节点
            List<TaskNode> startNodes = childrenNode.stream().filter(v -> v.getNodeInDegree() == 0).toList();

            for (TaskNode fromNode : fromNodes) {
                fromNode.setNodeOutDegree(fromNode.getNodeOutDegree() - 1 + startNodes.size());
            }

            for (TaskNode startNode : startNodes) {
                startNode.setNodeInDegree(startNode.getNodeInDegree() + fromNodes.size());
            }

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

            List<TaskNode> endNodes = taskNodeService.listByIds(endIds);

            List<TaskNode> childEndNodes = childrenNode.stream().filter(v -> v.getNodeOutDegree() == 0).toList();

            for (TaskNode endNode : endNodes) {
                endNode.setNodeInDegree(endNode.getNodeInDegree() - 1 + childEndNodes.size());
            }

            for (TaskNode childEndNode : childEndNodes) {
                childEndNode.setNodeOutDegree(childEndNode.getNodeOutDegree() + endNodes.size());
            }

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


    private void runT(TaskNode node, List<Long> taskIds) {
        TaskInfo taskInfo = taskInfoService.getById(node.getTaskId());

        Message message = new Message();
        message.setParentTaskId(node.getTaskParentId());
        message.setTaskId(node.getTaskId());

        while (stopMap.get(node.getTaskParentId()).getFirst()) {
           // message.setNodeId(stopMap.get(node.getTaskParentId()).getSecond());
            message.setStatus(0);
            webSocketServer.sendInfo(message);
            taskInfoService.stopTaskSet(node.getTaskParentId());
            // 节点清空
            Vector<ReturnT<Long>> vector = TriggerCallbackThread.vector;
            vector.removeIf(res -> taskIds.contains(res.getContent()));
            throw new BusinessException(node.getTaskParentId() + " task stop");
        }

        TaskInfoTriggerDto taskInfoTriggerDto = new TaskInfoTriggerDto();
        taskInfoTriggerDto.setId(node.getTaskId());
        taskInfoService.triggerJob(taskInfoTriggerDto);

        //message.setNodeId(node.getId());
        message.setStatus(2);
        webSocketServer.sendInfo(message);


        Thread futureThread = null;
        FutureTask<Boolean> futureTask = new FutureTask<Boolean>(() -> {
            int retryCount = -1;
            Label:
            while (true) {
                Vector<ReturnT<Long>> vector = TriggerCallbackThread.vector;
                List<ReturnT<Long>> list = new ArrayList<>(vector);
                for (ReturnT<Long> res : list) {
                    if (res.getContent().equals(node.getTaskId())) {
                        System.out.println(res + "end node" + node.getTaskId() + ">>>>>>>>>>> task:" + taskInfo.getJobDesc());
                        if (res.getCode() == ReturnT.SUCCESS_CODE) {
                            message.setStatus(1);
                            webSocketServer.sendInfo(message);
                            TriggerCallbackThread.vector.remove(res);
                            break Label;
                        } else {
                            retryCount++;
                            if (retryCount <= taskInfo.getExecutorFailRetryCount()) {
                                message.setStatus(0);
                                webSocketServer.sendInfo(message);
                            }
                            if ("DO_NOTHING".equalsIgnoreCase(taskInfo.getExecutorBlockStrategy())) {
                                TriggerCallbackThread.vector.remove(res);
                                break Label;
                            } else {
                                stopMap.put(node.getTaskParentId(), new Pair<>(true, node.getTaskId()));
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

}
