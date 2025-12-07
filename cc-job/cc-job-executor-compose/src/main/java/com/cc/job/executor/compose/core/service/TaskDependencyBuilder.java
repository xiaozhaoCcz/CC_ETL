package com.cc.job.executor.compose.core.service;

import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务依赖关系构建器
 * 
 * <p>负责构建 WorkerWrapper 之间的依赖关系
 *
 * @author cc-job-team
 */
@Component
public class TaskDependencyBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskDependencyBuilder.class);
    
    /**
     * 构建 WorkerWrapper 之间的依赖关系
     * 
     * @param workerWrappers Worker包装器列表
     * @param nodes 节点列表
     * @param edges 边列表
     */
    public void buildDependencies(List<WorkerWrapper<Long, String>> workerWrappers, 
                                 List<JobNode> nodes, List<JobEdge> edges) {
        logger.info("[TaskDependency] 开始构建依赖关系 - 节点数: {}, 边数: {}", 
                nodes.size(), edges.size());
        
        // 构建 nextMap：节点ID -> 后续节点ID列表
        Map<Long, List<Long>> nextMap = buildNextNodeMap(nodes, edges);
        
        // 为每个 WorkerWrapper 设置后续任务
        setNextWrappers(workerWrappers, nextMap);
        
        // 验证依赖关系
        logDependencies(workerWrappers);
        
        logger.info("[TaskDependency] 依赖关系构建完成");
    }
    
    /**
     * 构建后续节点映射
     */
    private Map<Long, List<Long>> buildNextNodeMap(List<JobNode> nodes, List<JobEdge> edges) {
        Map<Long, List<Long>> nextMap = new HashMap<>();
        
        for (JobNode node : nodes) {
            List<Long> nextNodeIds = edges.stream()
                    .filter(e -> e.getFromNodeId().equals(node.getId()))
                    .map(JobEdge::getEndNodeId)
                    .toList();
            nextMap.put(node.getId(), nextNodeIds);
            
            if (!nextNodeIds.isEmpty()) {
                logger.debug("[TaskDependency] 节点 {} 的后续节点: {}", node.getId(), nextNodeIds);
            }
        }
        
        return nextMap;
    }
    
    /**
     * 设置 WorkerWrapper 的后续任务
     */
    private void setNextWrappers(List<WorkerWrapper<Long, String>> workerWrappers, 
                                Map<Long, List<Long>> nextMap) {
        for (WorkerWrapper<Long, String> wrapper : workerWrappers) {
            Long nodeId = Long.valueOf(wrapper.getId());
            List<Long> nextNodeIds = nextMap.get(nodeId);
            
            if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                List<WorkerWrapper<Long, String>> nextWorkers = workerWrappers.stream()
                        .filter(w -> nextNodeIds.contains(Long.valueOf(w.getId())))
                        .toList();
                
                if (!nextWorkers.isEmpty()) {
                    wrapper.next(nextWorkers.toArray(new WorkerWrapper[0]));
                    logger.debug("[TaskDependency] 节点 {} 设置了 {} 个后续节点", 
                            nodeId, nextWorkers.size());
                }
            }
        }
    }
    
    /**
     * 记录依赖关系（用于调试）
     */
    private void logDependencies(List<WorkerWrapper<Long, String>> workerWrappers) {
        for (WorkerWrapper<Long, String> wrapper : workerWrappers) {
            int dependCount = wrapper.getDependWrappers() != null 
                    ? wrapper.getDependWrappers().size() : 0;
            int nextCount = wrapper.getNextWrappers() != null 
                    ? wrapper.getNextWrappers().size() : 0;
            logger.debug("[TaskDependency] Worker {}: 依赖数={}, 后续数={}", 
                    wrapper.getId(), dependCount, nextCount);
        }
    }
}

