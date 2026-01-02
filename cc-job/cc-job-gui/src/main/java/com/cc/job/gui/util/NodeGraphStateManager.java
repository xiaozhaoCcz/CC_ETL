package com.cc.job.gui.util;

import com.cc.job.gui.model.ProcessNode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点图状态管理器（全局单例）
 * 用于存储节点的开始/终止/阻塞状态，不保存到数据库
 * 状态在程序运行期间一直保持，直到程序退出
 */
public class NodeGraphStateManager {
    
    private static final NodeGraphStateManager instance = new NodeGraphStateManager();
    
    // 存储节点状态：key = jobId, value = GraphNodeState
    private final Map<Long, ProcessNode.GraphNodeState> nodeStates = new ConcurrentHashMap<>();
    
    private NodeGraphStateManager() {
        // 私有构造函数，单例模式
    }
    
    /**
     * 获取单例实例
     */
    public static NodeGraphStateManager getInstance() {
        return instance;
    }
    
    /**
     * 设置节点状态
     * @param jobId 节点jobId
     * @param state 节点状态
     */
    public void setNodeState(Long jobId, ProcessNode.GraphNodeState state) {
        if (jobId == null) {
            return;
        }
        
        if (state == ProcessNode.GraphNodeState.NORMAL) {
            // 如果是普通状态，从集合中移除（节省内存）
            nodeStates.remove(jobId);
        } else {
            // 否则保存状态
            nodeStates.put(jobId, state);
        }
    }
    
    /**
     * 获取节点状态
     * @param jobId 节点jobId
     * @return 节点状态，如果不存在则返回 NORMAL
     */
    public ProcessNode.GraphNodeState getNodeState(Long jobId) {
        if (jobId == null) {
            return ProcessNode.GraphNodeState.NORMAL;
        }
        return nodeStates.getOrDefault(jobId, ProcessNode.GraphNodeState.NORMAL);
    }
    
    /**
     * 移除节点状态（当节点被删除时调用）
     * @param jobId 节点jobId
     */
    public void removeNodeState(Long jobId) {
        if (jobId != null) {
            nodeStates.remove(jobId);
        }
    }
    
    /**
     * 清除所有节点状态（用于测试或重置）
     */
    public void clearAllStates() {
        nodeStates.clear();
    }
    
    /**
     * 获取所有节点状态（用于调试）
     * @return 节点状态映射的副本
     */
    public Map<Long, ProcessNode.GraphNodeState> getAllStates() {
        return new ConcurrentHashMap<>(nodeStates);
    }
    
    /**
     * 清除指定任务组的所有节点状态
     * @param taskGroupId 任务组ID（可选，如果为null则不清除）
     */
    public void clearStatesForTaskGroup(Long taskGroupId) {
        // 注意：这里我们无法直接知道哪些节点属于哪个任务组
        // 如果需要按任务组清除，需要在其他地方维护任务组和节点的关系
        // 目前先保留所有状态，因为节点状态是全局的
    }
}

