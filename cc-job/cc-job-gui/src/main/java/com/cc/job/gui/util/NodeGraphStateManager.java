package com.cc.job.gui.util;

import com.cc.job.gui.model.ProcessNode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点图状态管理器（全局单例）
 * 用于存储节点的开始/终止/阻塞状态和启用/禁用状态，不保存到数据库
 * 状态在程序运行期间一直保持，直到程序退出
 */
public class NodeGraphStateManager {
    
    private static final NodeGraphStateManager instance = new NodeGraphStateManager();
    
    // 存储节点状态：key = jobId, value = GraphNodeState
    private final Map<Long, ProcessNode.GraphNodeState> nodeStates = new ConcurrentHashMap<>();
    
    // 存储节点启用/禁用状态：key = jobId, value = enabled (true=启用, false=禁用)
    // 只存储禁用的节点（enabled=false），启用的节点不存储（节省内存）
    private final Map<Long, Boolean> nodeEnabledStates = new ConcurrentHashMap<>();
    
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
    
    /**
     * 设置节点启用/禁用状态
     * @param jobId 节点jobId
     * @param enabled 是否启用（true=启用, false=禁用）
     */
    public void setNodeEnabled(Long jobId, boolean enabled) {
        if (jobId == null) {
            return;
        }
        
        if (enabled) {
            // 如果启用，从集合中移除（节省内存，默认就是启用状态）
            nodeEnabledStates.remove(jobId);
        } else {
            // 如果禁用，保存状态
            nodeEnabledStates.put(jobId, false);
        }
    }
    
    /**
     * 获取节点启用/禁用状态
     * @param jobId 节点jobId
     * @return 是否启用，如果不存在则返回 true（默认启用）
     */
    public boolean getNodeEnabled(Long jobId) {
        if (jobId == null) {
            return true;
        }
        // 如果不在集合中，说明是启用状态（默认）
        return nodeEnabledStates.getOrDefault(jobId, true);
    }
    
    /**
     * 移除节点启用/禁用状态（当节点被删除时调用）
     * @param jobId 节点jobId
     */
    public void removeNodeEnabled(Long jobId) {
        if (jobId != null) {
            nodeEnabledStates.remove(jobId);
        }
    }
    
    /**
     * 清除所有节点启用/禁用状态（用于测试或重置）
     */
    public void clearAllEnabledStates() {
        nodeEnabledStates.clear();
    }
    
    /**
     * 移除节点所有状态（当节点被删除时调用）
     * @param jobId 节点jobId
     */
    public void removeAllNodeStates(Long jobId) {
        removeNodeState(jobId);
        removeNodeEnabled(jobId);
    }
}

