package com.cc.job.test;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.CcJobApplication;
import com.cc.job.task.mapper.TaskEdgeMapper;
import com.cc.job.task.mapper.TaskInfoMapper;
import com.cc.job.task.mapper.TaskNodeMapper;
import com.cc.job.task.model.entity.TaskEdge;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.entity.TaskNode;
import com.cc.job.task.model.vo.TaskEdgeVo;
import com.cc.job.task.model.vo.TaskNodeVo;
import com.cc.job.test.entity.Edge;
import com.cc.job.test.entity.Node;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.thread.JobThread;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SpringBootTest(classes = CcJobApplication.class)
public class CcJobApplicationTest {


    @Resource
    TaskInfoMapper taskInfoMapper;

    @Resource
    TaskNodeMapper taskNodeMapper;

    @Resource
    TaskEdgeMapper taskEdgeMapper;

    // 实现任务串并行调度
    @Test
    public void test1(){
        List<Node> nodes = new ArrayList<Node>();
        List<Edge> edges = new ArrayList<Edge>();

        Node node1 = new Node(1, "Node 1", 0, 0);
        Node node2 = new Node(2, "Node 2", 0, 0);
        Node node3 = new Node(3, "Node 3", 0, 0);
        Node node4 = new Node(4, "Node 4", 0, 0);
        Node node5 = new Node(5, "Node 5", 0, 0);
        Node node6 = new Node(6, "Node 6", 0, 0);
        Node node7 = new Node(7, "Node 7", 0, 0);
        Node node8 = new Node(8, "Node 8", 0, 0);
        Node node9 = new Node(9, "Node 9", 0, 0);
        Node node10 = new Node(10, "Node 10", 0, 0);

        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);
        nodes.add(node5);
        nodes.add(node6);
        nodes.add(node7);
        nodes.add(node8);
        nodes.add(node9);
        nodes.add(node10);

        Edge edge1 = new Edge(1, 2);
        Edge edge2 = new Edge(1, 3);
        Edge edge3 = new Edge(1, 4);
        Edge edge4 = new Edge(1, 5);
        Edge edge5 = new Edge(1, 6);
        Edge edge6 = new Edge(2, 7);
        Edge edge7 = new Edge(3, 7);
        Edge edge8 = new Edge(4, 8);
        Edge edge9 = new Edge(5, 8);
        Edge edge10 = new Edge(6, 9);
        Edge edge11 = new Edge(7, 10);
        Edge edge12 = new Edge(8, 10);
        Edge edge13 = new Edge(9, 10);
        edges.add(edge1);
        edges.add(edge2);
        edges.add(edge3);
        edges.add(edge4);
        edges.add(edge5);
        edges.add(edge6);
        edges.add(edge7);
        edges.add(edge8);
        edges.add(edge9);
        edges.add(edge10);
        edges.add(edge11);
        edges.add(edge12);
        edges.add(edge13);


        // 计算节点的出度和入度
        for (Node node : nodes) {
            long inCount = edges.stream().filter(v -> Objects.equals(v.getEndId(), node.getId())).count();
            long outCount = edges.stream().filter(v -> Objects.equals(v.getStartId(), node.getId())).count();
            node.setInCount((int) inCount);
            node.setOutCount((int) outCount);
        }

        List<Node> startNodes = nodes.stream().filter(v -> v.getInCount() == 0).toList();

        List<Integer> visited = new CopyOnWriteArrayList<>();
        Queue<Integer> queue = new ConcurrentLinkedQueue<>();

        Map<Integer, List<Integer>> nodeMap = new HashMap<>();

        Map<Integer, CompletableFuture<Node>> futureMap = new HashMap<>();

        startNodes.forEach(item -> {
            queue.add(item.getId());
            visited.add(item.getId());
        });

//        Map<Integer, Integer> distances = new ConcurrentHashMap<>();
//        distances.put(1, 0);
//        Queue<Integer> queue = new ConcurrentLinkedQueue<>();
//        queue.add(1);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();

        while (!queue.isEmpty()) {
            List<Integer> currentLevelNodes = new ArrayList<>();
            while (!queue.isEmpty()) {
                Integer poll = queue.poll();
                List<Integer> collect = edges.stream().filter(v -> v.getEndId().equals(poll)).map(Edge::getStartId).toList();
                nodeMap.put(poll, collect);
                currentLevelNodes.add(poll);
            }

            for (int node : currentLevelNodes) {
                futures.add(executor.submit(() -> {
                    for (int neighbor : getNeighbors(node,edges)) {
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

        System.out.println(visited);
        System.out.println(nodeMap);

        nodeMap.forEach((k, v) -> {
            Node currentNode = nodes.stream().filter(item -> item.getId().equals(k)).findFirst().orElse(null);
            CompletableFuture<Node> future = CompletableFuture.supplyAsync(() -> {
                for ( int id : v) {
                    //阻塞等待
                    try {
                        futureMap.get(id).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }

                //执行任务
                runT(k);

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

    // 使用线程的方式模拟
    private void runT(Integer k) {
        System.out.println("start node" + k);
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //发送消息
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("发送消息成功"+k);
            return "";
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        System.out.println("end node" + k);
    }

    private List<Integer> getNeighbors(Integer node, List<Edge> edges) {
        return  edges.stream().filter(v -> v.getStartId().equals(node)).map(Edge::getEndId).collect(Collectors.toList());
    }


    static  boolean stop = true;
    @Test
    public void test2(){

        long jobId = 0L;

        List<TaskNode> nodeList = taskNodeMapper.selectList(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, jobId));
        List<TaskEdge> edgeList = taskEdgeMapper.selectList(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, jobId));


        Set<TaskNode> resNodeList = new HashSet<>();
        while (stop){
            stop = false;
            for (TaskNode taskNode : nodeList) {
                buildNode(taskNode,edgeList,resNodeList,jobId);
            }
            nodeList.clear();
            nodeList.addAll(resNodeList);
            resNodeList.clear();
        }

        System.out.println(nodeList);
        System.out.println(edgeList);

    }

    private void buildNode(TaskNode currentNode,List<TaskEdge> edgeList,Set<TaskNode> resNodeList,Long jobId) {
        // 获取当前任务
        TaskInfo taskInfo = taskInfoMapper.selectById(currentNode.getTaskId());
        if(taskInfo.getJobType()!=2&& Objects.equals(currentNode.getTaskParentId(), jobId)){
            resNodeList.add(currentNode);
            return;
        }

        if(taskInfo.getJobType()==2&&Objects.equals(currentNode.getTaskParentId(), jobId)){
            stop = true;

            List<TaskEdge> collectEdges = edgeList.stream().filter(v -> v.getTaskParentId().equals(currentNode.getId())).toList();
            for (TaskEdge edge : collectEdges) {
                edge.setTaskParentId(jobId);
                edgeList.add(edge);
            }

            //得到当前节点的所有开始节点
            List<Long> fromIds = edgeList.stream().filter(v -> v.getEndNodeId().equals(currentNode.getId())).map(TaskEdge::getFromNodeId).toList();

            List<TaskNode> fromNodes = taskNodeMapper.selectBatchIds(fromIds);

            //得到当前节点的所有孩子节点
            List<TaskNode> childrenNode = taskNodeMapper.selectList(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));

            // 得到孩子节点的开始节点
            List<TaskNode> startNodes = childrenNode.stream().filter(v -> v.getNodeInDegree() == 0).toList();

            for (TaskNode fromNode : fromNodes) {
                fromNode.setNodeOutDegree(fromNode.getNodeOutDegree()-1+startNodes.size());
            }

            for (TaskNode startNode : startNodes) {
                startNode.setNodeInDegree(startNode.getNodeInDegree()+fromNodes.size());
            }

            if(!fromIds.isEmpty()){
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

            List<TaskNode> endNodes = taskNodeMapper.selectBatchIds(endIds);

            List<TaskNode> childEndNodes = childrenNode.stream().filter(v -> v.getNodeOutDegree() == 0).toList();

            for (TaskNode endNode : endNodes) {
                endNode.setNodeInDegree(endNode.getNodeInDegree()-1+childEndNodes.size());
            }

            for (TaskNode childEndNode : childEndNodes) {
                childEndNode.setNodeOutDegree(childEndNode.getNodeOutDegree()+endNodes.size());
            }

            if(!childEndNodes.isEmpty()){
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

            edgeList.removeIf(v->(fromIds.contains(v.getFromNodeId())&&v.getEndNodeId().equals(currentNode.getId()))||(v.getFromNodeId().equals(currentNode.getId())&&endIds.contains(v.getEndNodeId())));
        }
    }


    private void addNode(List<TaskNode> nodeList,List<TaskEdge> edgeList,TaskInfo parentTask){
        Map<String,Long> nodeMap = new HashMap<>();

        for (TaskNode taskNode : nodeList) {
             TaskInfo taskInfo = taskInfoMapper.selectById(taskNode.getId());

            TaskInfo copyTaskInfo = BeanUtil.copyProperties(taskInfo, TaskInfo.class);
            copyTaskInfo.setParentId(parentTask.getId());
            taskInfoMapper.insert(copyTaskInfo);

            if(taskInfo.getJobType()==2){
                List<TaskNode> childNodes = taskNodeMapper.selectList(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));
                List<TaskEdge> childEdges = taskEdgeMapper.selectList(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskInfo.getId()));

                addNode(childNodes,childEdges,copyTaskInfo);
            }

            TaskNode copyTaskNode = BeanUtil.copyProperties(taskNode, TaskNode.class);
            copyTaskNode.setTaskId(copyTaskInfo.getId());
            copyTaskNode.setTaskParentId(parentTask.getId());
            taskNodeMapper.insert(copyTaskNode);

            nodeMap.put(String.valueOf(taskNode.getId()),copyTaskNode.getId());
        }

        for (TaskEdge taskEdge : edgeList) {

            TaskEdge copyEdge = BeanUtil.copyProperties(taskEdge, TaskEdge.class);
            copyEdge.setTaskParentId(parentTask.getId());
            copyEdge.setFromNodeId(nodeMap.get(String.valueOf(taskEdge.getFromNodeId())));
            copyEdge.setEndNodeId(nodeMap.get(String.valueOf(taskEdge.getEndNodeId())));
            taskEdgeMapper.insert(copyEdge);
        }
    }


    public void delNodes(Long jobId){
        TaskInfo taskInfo = taskInfoMapper.selectById(jobId);
        if(taskInfo.getJobType()==2){
            List<TaskInfo> taskInfos = taskInfoMapper.selectList(new LambdaQueryWrapper<TaskInfo>().eq(TaskInfo::getParentId, jobId));
            if(taskInfos.isEmpty()){
                return;
            }
            List<Long> childTaskIds = taskInfos.stream().map(TaskInfo::getId).toList();

           taskNodeMapper.delete(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));
            taskEdgeMapper.delete(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskInfo.getId()));

            for (Long childTaskId : childTaskIds) {
                delNodes(childTaskId);
            }
        }

        taskInfoMapper.deleteById(jobId);
    }

    public void getTaskInfoIds(Long jobId,List<Long> ids){
        TaskInfo taskInfo = taskInfoMapper.selectById(jobId);
        if(taskInfo.getJobType()!=2){
            ids.add(taskInfo.getId());
        }else{
            List<TaskInfo> taskInfos = taskInfoMapper.selectList(new LambdaQueryWrapper<TaskInfo>().eq(TaskInfo::getParentId, jobId));
            if(taskInfos.isEmpty()){
                return;
            }
            List<Long> childTaskIds = taskInfos.stream().map(TaskInfo::getId).toList();
            for (Long childTaskId : childTaskIds) {
                getTaskInfoIds(childTaskId,ids);
            }
        }
    }

    @Test
    public void test3(){


    }


    public void getChildNodeAndEdge(Long taskId,String nodeId,List<TaskNodeVo> taskNodeVos,List<TaskEdgeVo> taskEdgeVos){
        List<TaskNode> taskNodes = taskNodeMapper.selectList(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskId));
        List<TaskEdge> taskEdges = taskEdgeMapper.selectList(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskId));

        for (TaskNode taskNode : taskNodes) {
            TaskNodeVo taskNodeVo = BeanUtil.copyProperties(taskNode,TaskNodeVo.class);
            taskNodeVo.setNodePatentId(nodeId);
            taskNodeVos.add(taskNodeVo);
            getChildNodeAndEdge(taskNode.getTaskId(),String.valueOf(taskNode.getId()),taskNodeVos,taskEdgeVos);
        }

        List<TaskEdgeVo> copyTaskEdges = BeanUtil.copyToList(taskEdges, TaskEdgeVo.class);
        taskEdgeVos.addAll(copyTaskEdges);
    }
}




