package com.cc.job.gui.manager;

import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 依赖关系导航器 - 负责节点依赖关系分析和导航
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class DependencyNavigator {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyNavigator.class);
    
    private final Consumer<String> loggerCallback;
    
    public DependencyNavigator(Consumer<String> loggerCallback) {
        this.loggerCallback = loggerCallback;
    }
    
    /**
     * 依赖关系图
     */
    public static class DependencyGraph {
        private Map<ProcessNode, Set<ProcessNode>> graph; // 节点 -> 下游节点集合
        private Map<ProcessNode, Set<ProcessNode>> reverseGraph; // 节点 -> 上游节点集合
        
        public DependencyGraph() {
            this.graph = new HashMap<>();
            this.reverseGraph = new HashMap<>();
        }
        
        public void addEdge(ProcessNode from, ProcessNode to) {
            graph.computeIfAbsent(from, k -> new HashSet<>()).add(to);
            reverseGraph.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        }
        
        public Set<ProcessNode> getDownstream(ProcessNode node) {
            return graph.getOrDefault(node, new HashSet<>());
        }
        
        public Set<ProcessNode> getUpstream(ProcessNode node) {
            return reverseGraph.getOrDefault(node, new HashSet<>());
        }
        
        public Set<ProcessNode> getAllNodes() {
            Set<ProcessNode> allNodes = new HashSet<>(graph.keySet());
            allNodes.addAll(reverseGraph.keySet());
            return allNodes;
        }
    }
    
    /**
     * 构建依赖关系图
     *
     * @param nodes 节点列表
     * @param connections 连接列表
     * @return 依赖关系图
     */
    public DependencyGraph buildDependencyGraph(List<ProcessNode> nodes, List<NodeConnection> connections) {
        DependencyGraph graph = new DependencyGraph();
        
        // 初始化所有节点
        for (ProcessNode node : nodes) {
            graph.graph.put(node, new HashSet<>());
            graph.reverseGraph.put(node, new HashSet<>());
        }
        
        // 添加边
        for (NodeConnection conn : connections) {
            ProcessNode source = conn.getSourceNode();
            ProcessNode target = conn.getTargetNode();
            
            if (source != null && target != null && nodes.contains(source) && nodes.contains(target)) {
                graph.addEdge(source, target);
            }
        }
        
        return graph;
    }
    
    /**
     * 获取节点的所有上游节点（递归）
     *
     * @param graph 依赖关系图
     * @param node 目标节点
     * @return 所有上游节点集合
     */
    public Set<ProcessNode> getAllUpstreamNodes(DependencyGraph graph, ProcessNode node) {
        Set<ProcessNode> upstream = new HashSet<>();
        getAllUpstreamNodesRecursive(graph, node, upstream, new HashSet<>());
        return upstream;
    }
    
    private void getAllUpstreamNodesRecursive(DependencyGraph graph, ProcessNode node, 
                                             Set<ProcessNode> result, Set<ProcessNode> visited) {
        if (visited.contains(node)) {
            return; // 避免循环依赖导致的无限递归
        }
        visited.add(node);
        
        Set<ProcessNode> upstream = graph.getUpstream(node);
        for (ProcessNode upstreamNode : upstream) {
            result.add(upstreamNode);
            getAllUpstreamNodesRecursive(graph, upstreamNode, result, visited);
        }
    }
    
    /**
     * 获取节点的所有下游节点（递归）
     *
     * @param graph 依赖关系图
     * @param node 目标节点
     * @return 所有下游节点集合
     */
    public Set<ProcessNode> getAllDownstreamNodes(DependencyGraph graph, ProcessNode node) {
        Set<ProcessNode> downstream = new HashSet<>();
        getAllDownstreamNodesRecursive(graph, node, downstream, new HashSet<>());
        return downstream;
    }
    
    private void getAllDownstreamNodesRecursive(DependencyGraph graph, ProcessNode node, 
                                               Set<ProcessNode> result, Set<ProcessNode> visited) {
        if (visited.contains(node)) {
            return; // 避免循环依赖导致的无限递归
        }
        visited.add(node);
        
        Set<ProcessNode> downstream = graph.getDownstream(node);
        for (ProcessNode downstreamNode : downstream) {
            result.add(downstreamNode);
            getAllDownstreamNodesRecursive(graph, downstreamNode, result, visited);
        }
    }
    
    /**
     * 检测循环依赖
     *
     * @param graph 依赖关系图
     * @return 循环依赖路径列表（每个路径是一个节点列表）
     */
    public List<List<ProcessNode>> detectCycles(DependencyGraph graph) {
        List<List<ProcessNode>> cycles = new ArrayList<>();
        Set<ProcessNode> visited = new HashSet<>();
        Set<ProcessNode> recursionStack = new HashSet<>();
        Map<ProcessNode, List<ProcessNode>> path = new HashMap<>();
        
        for (ProcessNode node : graph.getAllNodes()) {
            if (!visited.contains(node)) {
                detectCyclesDFS(graph, node, visited, recursionStack, path, cycles);
            }
        }
        
        return cycles;
    }
    
    private void detectCyclesDFS(DependencyGraph graph, ProcessNode node, 
                                Set<ProcessNode> visited, Set<ProcessNode> recursionStack,
                                Map<ProcessNode, List<ProcessNode>> path, List<List<ProcessNode>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        
        List<ProcessNode> currentPath = path.getOrDefault(node, new ArrayList<>());
        currentPath.add(node);
        path.put(node, currentPath);
        
        Set<ProcessNode> downstream = graph.getDownstream(node);
        for (ProcessNode next : downstream) {
            if (!visited.contains(next)) {
                List<ProcessNode> nextPath = new ArrayList<>(currentPath);
                path.put(next, nextPath);
                detectCyclesDFS(graph, next, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(next)) {
                // 发现循环
                List<ProcessNode> cycle = new ArrayList<>();
                int startIndex = currentPath.indexOf(next);
                if (startIndex >= 0) {
                    cycle.addAll(currentPath.subList(startIndex, currentPath.size()));
                    cycle.add(next); // 添加回起点
                    cycles.add(cycle);
                }
            }
        }
        
        recursionStack.remove(node);
    }
    
    /**
     * 获取两个节点之间的所有路径
     *
     * @param graph 依赖关系图
     * @param from 起始节点
     * @param to 目标节点
     * @return 所有路径列表
     */
    public List<List<ProcessNode>> getAllPaths(DependencyGraph graph, ProcessNode from, ProcessNode to) {
        List<List<ProcessNode>> paths = new ArrayList<>();
        List<ProcessNode> currentPath = new ArrayList<>();
        Set<ProcessNode> visited = new HashSet<>();
        
        getAllPathsDFS(graph, from, to, currentPath, visited, paths);
        
        return paths;
    }
    
    private void getAllPathsDFS(DependencyGraph graph, ProcessNode current, ProcessNode target,
                               List<ProcessNode> currentPath, Set<ProcessNode> visited,
                               List<List<ProcessNode>> paths) {
        if (current.equals(target)) {
            // 找到一条路径
            List<ProcessNode> path = new ArrayList<>(currentPath);
            path.add(target);
            paths.add(path);
            return;
        }
        
        visited.add(current);
        currentPath.add(current);
        
        Set<ProcessNode> downstream = graph.getDownstream(current);
        for (ProcessNode next : downstream) {
            if (!visited.contains(next)) {
                getAllPathsDFS(graph, next, target, currentPath, visited, paths);
            }
        }
        
        visited.remove(current);
        currentPath.remove(currentPath.size() - 1);
    }
    
    /**
     * 构建依赖树（用于树形视图显示）
     *
     * @param graph 依赖关系图
     * @param rootNode 根节点（如果没有，选择没有上游的节点）
     * @return 依赖树节点
     */
    public DependencyTreeNode buildDependencyTree(DependencyGraph graph, ProcessNode rootNode) {
        if (rootNode == null) {
            // 如果没有指定根节点，选择没有上游的节点
            Set<ProcessNode> allNodes = graph.getAllNodes();
            for (ProcessNode node : allNodes) {
                if (graph.getUpstream(node).isEmpty()) {
                    rootNode = node;
                    break;
                }
            }
        }
        
        if (rootNode == null) {
            return null;
        }
        
        return buildDependencyTreeRecursive(graph, rootNode, new HashSet<>());
    }
    
    private DependencyTreeNode buildDependencyTreeRecursive(DependencyGraph graph, ProcessNode node, 
                                                           Set<ProcessNode> visited) {
        if (visited.contains(node)) {
            // 循环依赖，返回null
            return null;
        }
        visited.add(node);
        
        DependencyTreeNode treeNode = new DependencyTreeNode(node);
        
        Set<ProcessNode> downstream = graph.getDownstream(node);
        for (ProcessNode child : downstream) {
            DependencyTreeNode childNode = buildDependencyTreeRecursive(graph, child, new HashSet<>(visited));
            if (childNode != null) {
                treeNode.addChild(childNode);
            }
        }
        
        return treeNode;
    }
    
    /**
     * 依赖树节点
     */
    public static class DependencyTreeNode {
        private ProcessNode node;
        private List<DependencyTreeNode> children;
        
        public DependencyTreeNode(ProcessNode node) {
            this.node = node;
            this.children = new ArrayList<>();
        }
        
        public ProcessNode getNode() {
            return node;
        }
        
        public List<DependencyTreeNode> getChildren() {
            return children;
        }
        
        public void addChild(DependencyTreeNode child) {
            children.add(child);
        }
    }
    
    private void log(String message) {
        if (loggerCallback != null) {
            loggerCallback.accept(message);
        }
        logger.info(message);
    }
}
