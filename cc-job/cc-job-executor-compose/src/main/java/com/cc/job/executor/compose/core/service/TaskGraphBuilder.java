package com.cc.job.executor.compose.core.service;

import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 任务图构建器
 * 
 * <p>负责计算节点的入度和出度
 *
 * @author cc-job-team
 */
@Component
public class TaskGraphBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskGraphBuilder.class);
    
    /**
     * 构建任务依赖图
     * 
     * @param jobId 任务组ID
     * @param nodes 节点列表
     * @param edges 边列表
     */
    public void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        logger.debug("[TaskGraphBuilder] 开始构建任务图 - jobId: {}, 节点数: {}, 边数: {}", 
                jobId, nodes.size(), edges.size());
        
        Map<Long, List<JobEdge>> edgesByFrom = new HashMap<>();
        Map<Long, List<JobEdge>> edgesByTo = new HashMap<>();
        
        // 按起点和终点分组边
        for (JobEdge edge : edges) {
            edgesByFrom.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
            edgesByTo.computeIfAbsent(edge.getEndNodeId(), k -> new ArrayList<>()).add(edge);
        }
        
        // 计算每个节点的入度和出度
        for (JobNode node : nodes) {
            long inDegree = edgesByTo.getOrDefault(node.getId(), Collections.emptyList()).size();
            long outDegree = edgesByFrom.getOrDefault(node.getId(), Collections.emptyList()).size();
            node.setNodeInDegree(inDegree);
            node.setNodeOutDegree(outDegree);
            node.setJobParentId(jobId);
            
            logger.debug("[TaskGraphBuilder] 节点: {}, 入度: {}, 出度: {}", 
                    node.getId(), inDegree, outDegree);
        }
        
        logger.info("[TaskGraphBuilder] 任务图构建完成 - jobId: {}", jobId);
    }
    
    /**
     * 验证图是否有环
     * 
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 是否有环
     */
    public boolean hasCycle(List<JobNode> nodes, List<JobEdge> edges) {
        Map<Long, List<Long>> adjacency = new HashMap<>();
        
        for (JobEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>())
                    .add(edge.getEndNodeId());
        }
        
        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();
        
        for (JobNode node : nodes) {
            if (detectCycle(node.getId(), adjacency, visited, recursionStack)) {
                logger.warn("[TaskGraphBuilder] 检测到循环依赖");
                return true;
            }
        }
        
        return false;
    }
    
    private boolean detectCycle(Long nodeId, Map<Long, List<Long>> adjacency, 
                                Set<Long> visited, Set<Long> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }
        
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        List<Long> neighbors = adjacency.getOrDefault(nodeId, Collections.emptyList());
        for (Long neighbor : neighbors) {
            if (detectCycle(neighbor, adjacency, visited, recursionStack)) {
                return true;
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }
}

