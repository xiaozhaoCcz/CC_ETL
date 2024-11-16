package com.cc.job.task.jobhandler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskEdge;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.entity.TaskNode;
import com.cc.job.task.service.TaskEdgeService;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.service.TaskNodeService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.thread.TriggerCallbackThread;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class TaskRankXxlJob {


    final TaskInfoService taskInfoService;

    final TaskNodeService taskNodeService;

    final TaskEdgeService taskEdgeService;

    @XxlJob("runTaskRankXxlJob")
    public void runTaskRankXxlJob(){
        String jobId = XxlJobHelper.getJobParam();
        System.out.println(">>>>>>>runTaskRankXxlJob"+jobId);
        List<TaskNode> nodes = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId,jobId));
        List<TaskEdge> edges = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId,jobId));


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

        final List<Long> taskIds = nodes.stream().map(TaskNode::getTaskId).toList();
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
                runT(currentNode,taskIds);
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


    private void runT(TaskNode node,List<Long> taskIds) {
        TaskInfo taskInfo = taskInfoService.getById(node.getTaskId());
        System.out.println("start node " + node.getTaskId()+">>>>>>>>>>> task:"+taskInfo.getJobDesc());

        TaskInfoTriggerDto taskInfoTriggerDto = new TaskInfoTriggerDto();
        taskInfoTriggerDto.setId(node.getTaskId());
        taskInfoService.triggerJob(taskInfoTriggerDto);

        try {
            Thread futureThread = null;
            FutureTask<Boolean> futureTask = new FutureTask<Boolean>(() -> {
                Label:
                while (true){
                    Vector<ReturnT<Long>> vector = TriggerCallbackThread.vector;
                    List<ReturnT<Long>> list = new ArrayList<>(vector);
                    for (ReturnT<Long> res : list) {
                        if(res.getContent().equals(node.getTaskId())){
                            System.out.println(res+"end node" + node.getTaskId()+">>>>>>>>>>> task:"+taskInfo.getJobDesc());
                            if(res.getCode()==ReturnT.SUCCESS_CODE){
                                TriggerCallbackThread.vector.remove(res);
                                break Label;
                            }else{
                                if("DO_NOTHING".equalsIgnoreCase(taskInfo.getExecutorBlockStrategy())){
                                    TriggerCallbackThread.vector.remove(res);
                                    break Label;
                                }else{
                                    // 立即停止当前任务
                                    throw new RuntimeException(res.getMsg());
                                }
                            }
                        }
                    }
                }
                return true;
            });
            futureThread = new Thread(futureTask);
            futureThread.start();

            Boolean tempResult = futureTask.get();
        }catch (Exception e){
             // 节点清空
             Vector<ReturnT<Long>> vector = TriggerCallbackThread.vector;
             vector.removeIf(res -> taskIds.contains(res.getContent()));
             throw new RuntimeException(e.getMessage());
        }
    }

    private List<Long> getNeighbors(Long node, List<TaskEdge> edges) {
        return  edges.stream().filter(v -> v.getFromNodeId().equals(node)).map(TaskEdge::getEndNodeId).collect(Collectors.toList());
    }

}
