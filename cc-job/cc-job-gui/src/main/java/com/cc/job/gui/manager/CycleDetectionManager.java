package com.cc.job.gui.manager;

import com.cc.job.gui.model.*;
import javafx.scene.Node;

import java.util.*;

/**
 * 循环依赖检测管理器
 * 使用深度优先搜索（DFS）算法检测有向图中的循环
 */
public class CycleDetectionManager {
    
    /**
     * 检测结果类
     */
    public static class CycleDetectionResult {
        private final boolean hasCycle;
        private final List<NodeConnection> cycleConnections; // 参与循环的连接线
        private final List<List<String>> cyclePaths; // 循环路径（节点ID列表）
        
        public CycleDetectionResult(boolean hasCycle, List<NodeConnection> cycleConnections, List<List<String>> cyclePaths) {
            this.hasCycle = hasCycle;
            this.cycleConnections = cycleConnections != null ? new ArrayList<>(cycleConnections) : new ArrayList<>();
            this.cyclePaths = cyclePaths != null ? new ArrayList<>(cyclePaths) : new ArrayList<>();
        }
        
        public boolean hasCycle() {
            return hasCycle;
        }
        
        public List<NodeConnection> getCycleConnections() {
            return cycleConnections;
        }
        
        public List<List<String>> getCyclePaths() {
            return cyclePaths;
        }
    }
    
    /**
     * 检测循环依赖
     * @param connections 连接线列表
     * @return 检测结果
     */
    public static CycleDetectionResult detectCycles(List<NodeConnection> connections) {
        if (connections == null || connections.isEmpty()) {
            return new CycleDetectionResult(false, new ArrayList<>(), new ArrayList<>());
        }
        
        // 构建邻接表
        Map<String, List<EdgeInfo>> adjacencyList = buildAdjacencyList(connections);
        
        // 用于存储所有参与循环的连接线
        Set<NodeConnection> cycleConnections = new HashSet<>();
        // 用于存储循环路径
        List<List<String>> cyclePaths = new ArrayList<>();
        
        // 对所有节点执行DFS检测
        Set<String> allNodes = new HashSet<>(adjacencyList.keySet());
        for (String nodeId : adjacencyList.keySet()) {
            for (EdgeInfo edge : adjacencyList.get(nodeId)) {
                allNodes.add(edge.targetId);
            }
        }
        
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        
        for (String nodeId : allNodes) {
            if (!visited.contains(nodeId)) {
                List<String> currentPath = new ArrayList<>();
                detectCycleDFS(nodeId, adjacencyList, visited, recStack, currentPath, 
                             cycleConnections, cyclePaths, connections);
            }
        }
        
        boolean hasCycle = !cycleConnections.isEmpty();
        return new CycleDetectionResult(hasCycle, new ArrayList<>(cycleConnections), cyclePaths);
    }
    
    /**
     * 构建邻接表
     */
    private static Map<String, List<EdgeInfo>> buildAdjacencyList(List<NodeConnection> connections) {
        Map<String, List<EdgeInfo>> adjacencyList = new HashMap<>();
        
        for (NodeConnection conn : connections) {
            String sourceId = getNodeId(conn.getSourceOwner());
            String targetId = getNodeId(conn.getTargetOwner());
            
            if (sourceId != null && targetId != null) {
                adjacencyList.computeIfAbsent(sourceId, k -> new ArrayList<>())
                            .add(new EdgeInfo(targetId, conn));
            }
        }
        
        return adjacencyList;
    }
    
    /**
     * 使用DFS检测循环
     */
    private static void detectCycleDFS(String nodeId, Map<String, List<EdgeInfo>> adjacencyList,
                                       Set<String> visited, Set<String> recStack,
                                       List<String> currentPath,
                                       Set<NodeConnection> cycleConnections,
                                       List<List<String>> cyclePaths,
                                       List<NodeConnection> allConnections) {
        visited.add(nodeId);
        recStack.add(nodeId);
        currentPath.add(nodeId);
        
        List<EdgeInfo> neighbors = adjacencyList.get(nodeId);
        if (neighbors != null) {
            for (EdgeInfo edge : neighbors) {
                String neighborId = edge.targetId;
                
                if (!visited.contains(neighborId)) {
                    // 继续DFS
                    detectCycleDFS(neighborId, adjacencyList, visited, recStack, currentPath,
                                  cycleConnections, cyclePaths, allConnections);
                } else if (recStack.contains(neighborId)) {
                    // 发现循环：当前节点在递归栈中，说明存在回边
                    cycleConnections.add(edge.connection);
                    
                    // 找到循环路径：从neighborId到nodeId的路径
                    int startIndex = currentPath.indexOf(neighborId);
                    if (startIndex >= 0) {
                        List<String> cyclePath = new ArrayList<>();
                        for (int i = startIndex; i < currentPath.size(); i++) {
                            cyclePath.add(currentPath.get(i));
                        }
                        cyclePath.add(neighborId); // 闭合循环
                        
                        // 检查是否已存在相同的循环路径（避免重复）
                        boolean isDuplicate = false;
                        for (List<String> existingPath : cyclePaths) {
                            if (isSameCycle(existingPath, cyclePath)) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate) {
                            cyclePaths.add(cyclePath);
                        }
                        
                        // 标记循环路径上的所有连接线
                        markCycleConnectionsInPath(cyclePath, allConnections, cycleConnections);
                    }
                }
            }
        }
        
        recStack.remove(nodeId);
        if (!currentPath.isEmpty()) {
            currentPath.remove(currentPath.size() - 1);
        }
    }
    
    /**
     * 标记循环路径上的所有连接线
     */
    private static void markCycleConnectionsInPath(List<String> cyclePath, 
                                                  List<NodeConnection> allConnections,
                                                  Set<NodeConnection> cycleConnections) {
        // 遍历循环路径，标记路径上每两个相邻节点之间的连接线
        for (int i = 0; i < cyclePath.size() - 1; i++) {
            String sourceId = cyclePath.get(i);
            String targetId = cyclePath.get(i + 1);
            
            // 找到对应的连接线
            for (NodeConnection conn : allConnections) {
                String connSourceId = getNodeId(conn.getSourceOwner());
                String connTargetId = getNodeId(conn.getTargetOwner());
                
                if (sourceId != null && targetId != null &&
                    sourceId.equals(connSourceId) && targetId.equals(connTargetId)) {
                    cycleConnections.add(conn);
                }
            }
        }
    }
    
    /**
     * 检查两个循环路径是否相同（考虑循环的起点）
     */
    private static boolean isSameCycle(List<String> path1, List<String> path2) {
        if (path1.size() != path2.size()) {
            return false;
        }
        
        // 循环路径可以有不同的起点，需要旋转比较
        for (int i = 0; i < path1.size(); i++) {
            boolean match = true;
            for (int j = 0; j < path1.size(); j++) {
                int index2 = (i + j) % path2.size();
                if (!path1.get(j).equals(path2.get(index2))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取节点ID（支持ProcessNode、GroupContainer、ConditionNode）
     */
    private static String getNodeId(Node owner) {
        if (owner instanceof ProcessNode) {
            return ((ProcessNode) owner).getNodeId();
        } else if (owner instanceof GroupContainer) {
            return ((GroupContainer) owner).getNodeId();
        } else if (owner instanceof ConditionNode) {
            return ((ConditionNode) owner).getNodeId();
        }
        return null;
    }
    
    /**
     * 边信息内部类
     */
    private static class EdgeInfo {
        final String targetId;
        final NodeConnection connection;
        
        EdgeInfo(String targetId, NodeConnection connection) {
            this.targetId = targetId;
            this.connection = connection;
        }
    }
}
