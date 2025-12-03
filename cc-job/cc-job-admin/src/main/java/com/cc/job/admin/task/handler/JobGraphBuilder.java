package com.cc.job.admin.task.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 任务图构建器
 * 
 * 负责构建任务依赖关系图，处理任务节点和边的关系
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobGraphBuilder {

    private final JobInfoService jobInfoService;
    private final JobNodeService jobNodeService;
    private final JobEdgeService jobEdgeService;

    /**
     * 获取所有节点和边信息（从数据库获取）
     *
     * @param jobId 任务ID
     * @param nodes 节点列表（输出参数）
     * @param edges 边列表（输出参数）
     */
    public void getAllNodesAndEdges(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        log.debug("[JobGraph] 开始获取所有节点和边 - jobId: {}", jobId);

        // 获取直接子节点和边
        List<JobNode> jobNodes = jobNodeService
                .list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobId));
        List<JobEdge> jobEdges = jobEdgeService
                .list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobId));

        nodes.addAll(jobNodes);
        edges.addAll(jobEdges);

        log.debug("[JobGraph] 获取直接子节点和边 - jobId: {}, 节点数量: {}, 边数量: {}", 
                jobId, jobNodes.size(), jobEdges.size());

        // 递归获取子任务组的节点和边
        List<JobInfo> childJobInfos = jobInfoService
                .list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
        for (JobInfo childJobInfo : childJobInfos) {
            if (childJobInfo.getJobType() == 2) {
                log.debug("[JobGraph] 递归获取子任务组节点和边 - 父任务ID: {}, 子任务ID: {}", 
                        jobId, childJobInfo.getId());
                getAllNodesAndEdges(childJobInfo.getId(), nodes, edges);
            }
        }

        log.debug("[JobGraph] 获取所有节点和边完成 - jobId: {}, 总节点数量: {}, 总边数量: {}", 
                jobId, nodes.size(), edges.size());
    }

    /**
     * 构建任务图
     *
     * @param jobId 任务ID
     * @param nodes 节点列表
     * @param edges 边列表
     */
    public void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        log.debug("[JobGraph] 开始构建任务图 - jobId: {}", jobId);

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
        processNodeList(jobId, nodes, edges, nodeList, nodesByParent, edgesByFrom, edgesByTo);

        // 计算节点的入度和出度
        nodes.forEach(node -> {
            node.setNodeInDegree((long) edgesByTo.getOrDefault(node.getId(), Collections.emptyList()).size());
            node.setNodeOutDegree((long) edgesByFrom.getOrDefault(node.getId(), Collections.emptyList()).size());
            node.setJobParentId(jobId);
        });
        edges.forEach(edge -> edge.setJobParentId(jobId));

        log.debug("[JobGraph] 任务图构建完成 - jobId: {}, 节点数量: {}, 边数量: {}", 
                jobId, nodes.size(), edges.size());
    }

    /**
     * 构建节点关系映射
     *
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 节点关系映射
     */
    public Map<Long, List<JobNode>> buildNextNodeMap(List<JobNode> nodes, List<JobEdge> edges) {
        Map<Long, List<JobNode>> result = new HashMap<>();
        for (JobNode node : nodes) {
            List<Long> nodeIds = edges.stream()
                    .filter(v -> v.getFromNodeId().equals(node.getId()))
                    .map(JobEdge::getEndNodeId)
                    .toList();
            List<JobNode> cNodes = nodes.stream()
                    .filter(v -> nodeIds.contains(v.getId()))
                    .toList();
            result.put(node.getId(), cNodes);
        }
        return result;
    }

    /**
     * 获取开始节点列表（入度为0的节点）
     */
    public List<Long> getStartNodes(List<JobNode> nodes) {
        return nodes.stream()
                .filter(v -> v.getNodeInDegree().equals(0L))
                .map(JobNode::getId)
                .toList();
    }

    /**
     * 处理节点列表
     */
    private void processNodeList(Long jobId, List<JobNode> nodes, List<JobEdge> edges, 
                                 List<JobNode> nodeList, Map<Long, List<JobNode>> nodesByParent,
                                 Map<Long, List<JobEdge>> edgesByFrom, Map<Long, List<JobEdge>> edgesByTo) {
        log.debug("[JobGraph] 处理节点列表 - jobId: {}, 节点数量: {}", jobId, nodeList.size());

        for (JobNode node : nodeList) {
            List<Long> preNodeIds = edgesByTo.getOrDefault(node.getId(), Collections.emptyList())
                    .stream().map(JobEdge::getFromNodeId).toList();
            List<Long> nextNodeIds = edgesByFrom.getOrDefault(node.getId(), Collections.emptyList())
                    .stream().map(JobEdge::getEndNodeId).toList();
            
            log.debug("[JobGraph] 处理节点 - nodeId: {}, 前置节点数量: {}, 后置节点数量: {}",
                    node.getId(), preNodeIds.size(), nextNodeIds.size());
            
            flattenNode(jobId, node, preNodeIds, nextNodeIds, nodes, edges, 
                       nodesByParent, edgesByFrom, edgesByTo);
        }
    }

    /**
     * 将多维图像降维（扁平化任务组节点）
     *
     * 处理任务组节点，将其子节点直接连接到父节点，实现图的扁平化
     */
    private void flattenNode(Long jobId, JobNode currentNode, List<Long> preNodeIds, 
                            List<Long> nextNodeIds, List<JobNode> nodes, List<JobEdge> edges,
                            Map<Long, List<JobNode>> nodesByParent, Map<Long, List<JobEdge>> edgesByFrom,
                            Map<Long, List<JobEdge>> edgesByTo) {
        JobInfo jobInfo = jobInfoService.getById(currentNode.getJobId());
        
        // 只处理任务组类型（jobType == 2）
        if (jobInfo.getJobType() != 2) {
            return;
        }

        // 移除与当前节点相关的边
        removeEdges(preNodeIds, currentNode.getId(), edges, edgesByFrom, edgesByTo, true);
        removeEdges(nextNodeIds, currentNode.getId(), edges, edgesByFrom, edgesByTo, false);

        // 获取子任务组的节点
        List<JobNode> childrenNodes = new ArrayList<>(
                nodesByParent.getOrDefault(jobInfo.getId(), Collections.emptyList()));

        // 计算子节点的入度和出度
        List<Long> childrenNodeIds = childrenNodes.stream().map(JobNode::getId).toList();
        Map<Long, Long> childrenInDegree = calculateInDegree(childrenNodeIds, edgesByTo);
        Map<Long, Long> childrenOutDegree = calculateOutDegree(childrenNodeIds, edgesByFrom);

        // 获取开始节点和结束节点
        List<JobNode> startNodes = findStartNodes(childrenNodes, childrenInDegree);
        List<JobNode> endNodes = findEndNodes(childrenNodes, childrenOutDegree);

        log.debug("[JobGraph] 处理任务组节点 - jobId: {}, 子节点数量: {}, 开始节点数量: {}, 结束节点数量: {}",
                jobInfo.getId(), childrenNodes.size(), startNodes.size(), endNodes.size());

        if (!childrenNodes.isEmpty()) {
            // 连接前置节点到开始节点
            connectNodes(preNodeIds, startNodes, jobId, edges, edgesByFrom, edgesByTo, true);
            // 连接结束节点到后置节点
            connectNodes(nextNodeIds, endNodes, jobId, edges, edgesByFrom, edgesByTo, false);
            // 递归处理子节点
            processNodeList(jobId, nodes, edges, childrenNodes, nodesByParent, edgesByFrom, edgesByTo);
        } else {
            log.warn("[JobGraph] 任务组节点没有子节点 - jobId: {}, jobName: {}",
                    jobInfo.getId(), jobInfo.getJobDesc());
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

    private Map<Long, Long> calculateInDegree(List<Long> nodeIds, Map<Long, List<JobEdge>> edgesByTo) {
        Map<Long, Long> inDegree = new HashMap<>();
        for (Long nodeId : nodeIds) {
            long count = edgesByTo.getOrDefault(nodeId, Collections.emptyList()).stream()
                    .filter(edge -> nodeIds.contains(edge.getFromNodeId()))
                    .count();
            inDegree.put(nodeId, count);
        }
        return inDegree;
    }

    private Map<Long, Long> calculateOutDegree(List<Long> nodeIds, Map<Long, List<JobEdge>> edgesByFrom) {
        Map<Long, Long> outDegree = new HashMap<>();
        for (Long nodeId : nodeIds) {
            long count = edgesByFrom.getOrDefault(nodeId, Collections.emptyList()).stream()
                    .filter(edge -> nodeIds.contains(edge.getEndNodeId()))
                    .count();
            outDegree.put(nodeId, count);
        }
        return outDegree;
    }

    private List<JobNode> findStartNodes(List<JobNode> nodes, Map<Long, Long> inDegree) {
        List<JobNode> startNodes = nodes.stream()
                .filter(v -> inDegree.getOrDefault(v.getId(), 0L) == 0)
                .toList();
        
        if (startNodes.isEmpty()) {
            log.warn("[JobGraph] 没有找到开始节点，使用所有节点作为开始节点");
            return new ArrayList<>(nodes);
        }
        return startNodes;
    }

    private List<JobNode> findEndNodes(List<JobNode> nodes, Map<Long, Long> outDegree) {
        List<JobNode> endNodes = nodes.stream()
                .filter(v -> outDegree.getOrDefault(v.getId(), 0L) == 0)
                .toList();
        
        if (endNodes.isEmpty()) {
            log.warn("[JobGraph] 没有找到结束节点，使用所有节点作为结束节点");
            return new ArrayList<>(nodes);
        }
        return endNodes;
    }

    private void connectNodes(List<Long> fromNodeIds, List<JobNode> toNodes, Long jobId,
                             List<JobEdge> edges, Map<Long, List<JobEdge>> edgesByFrom,
                             Map<Long, List<JobEdge>> edgesByTo, boolean isStartConnection) {
        for (JobNode toNode : toNodes) {
            for (Long fromNodeId : fromNodeIds) {
                JobEdge edge = new JobEdge();
                if (isStartConnection) {
                    edge.setFromNodeId(fromNodeId);
                    edge.setEndNodeId(toNode.getId());
                } else {
                    edge.setFromNodeId(toNode.getId());
                    edge.setEndNodeId(fromNodeId);
                }
                edge.setJobParentId(jobId);
                addEdge(edge, edges, edgesByFrom, edgesByTo);
                log.debug("[JobGraph] 添加边: {} -> {}", edge.getFromNodeId(), edge.getEndNodeId());
            }
        }
    }

    private void removeEdges(List<Long> relatedNodeIds, Long currentNodeId, List<JobEdge> edges,
                           Map<Long, List<JobEdge>> edgesByFrom, Map<Long, List<JobEdge>> edgesByTo,
                           boolean removeIncoming) {
        if (relatedNodeIds == null || relatedNodeIds.isEmpty()) {
            return;
        }
        Set<Long> relatedSet = new HashSet<>(relatedNodeIds);
        List<JobEdge> candidates = removeIncoming
                ? new ArrayList<>(edgesByTo.getOrDefault(currentNodeId, Collections.emptyList()))
                : new ArrayList<>(edgesByFrom.getOrDefault(currentNodeId, Collections.emptyList()));
        
        for (JobEdge edge : candidates) {
            boolean match = removeIncoming 
                    ? relatedSet.contains(edge.getFromNodeId())
                    : relatedSet.contains(edge.getEndNodeId());
            if (match) {
                removeEdge(edge, edges, edgesByFrom, edgesByTo);
            }
        }
    }

    private void removeEdge(JobEdge edge, List<JobEdge> edges, Map<Long, List<JobEdge>> edgesByFrom,
                          Map<Long, List<JobEdge>> edgesByTo) {
        edges.remove(edge);
        removeFromMap(edgesByFrom, edge.getFromNodeId(), edge);
        removeFromMap(edgesByTo, edge.getEndNodeId(), edge);
    }

    private void removeFromMap(Map<Long, List<JobEdge>> map, Long key, JobEdge edge) {
        List<JobEdge> list = map.get(key);
        if (list != null) {
            list.remove(edge);
            if (list.isEmpty()) {
                map.remove(key);
            }
        }
    }

    private void addEdge(JobEdge edge, List<JobEdge> edges, Map<Long, List<JobEdge>> edgesByFrom,
                       Map<Long, List<JobEdge>> edgesByTo) {
        List<JobEdge> fromList = edgesByFrom.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>());
        boolean exists = fromList.stream()
                .anyMatch(e -> e.getEndNodeId().equals(edge.getEndNodeId()));
        if (exists) {
            return;
        }
        fromList.add(edge);
        edgesByTo.computeIfAbsent(edge.getEndNodeId(), k -> new ArrayList<>()).add(edge);
        edges.add(edge);
    }
}

