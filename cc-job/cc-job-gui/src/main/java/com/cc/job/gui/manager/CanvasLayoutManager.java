package com.cc.job.gui.manager;

import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import javafx.geometry.Point2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * 画布布局管理器 - 负责智能布局算法
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class CanvasLayoutManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CanvasLayoutManager.class);
    
    // 布局参数
    private static final double NODE_WIDTH = 180;
    private static final double NODE_HEIGHT = 80;
    private static final double HORIZONTAL_SPACING = 250; // 水平间距
    private static final double VERTICAL_SPACING = 150; // 垂直间距
    private static final double START_X = 100; // 起始X坐标
    private static final double START_Y = 100; // 起始Y坐标
    
    // 力导向布局参数
    private static final double SPRING_LENGTH = 200; // 弹簧理想长度
    private static final double SPRING_STRENGTH = 0.1; // 弹簧强度
    private static final double REPULSION_STRENGTH = 10000; // 排斥力强度
    private static final int ITERATIONS = 300; // 迭代次数
    
    private final Consumer<String> loggerCallback;
    
    public CanvasLayoutManager(Consumer<String> loggerCallback) {
        this.loggerCallback = loggerCallback;
    }
    
    /**
     * 布局算法类型枚举
     */
    public enum LayoutAlgorithm {
        GRID,           // 网格布局（原有简单布局）
        HIERARCHICAL,  // 层次化布局
        FORCE_DIRECTED, // 力导向布局
        TREE           // 树形布局
    }
    
    /**
     * 执行布局算法
     *
     * @param nodes 节点列表
     * @param connections 连接列表
     * @param algorithm 布局算法类型
     */
    public void layout(List<ProcessNode> nodes, List<NodeConnection> connections, LayoutAlgorithm algorithm) {
        if (nodes == null || nodes.isEmpty()) {
            log("⚠ 画布中没有节点");
            return;
        }
        
        switch (algorithm) {
            case GRID:
                gridLayout(nodes);
                break;
            case HIERARCHICAL:
                hierarchicalLayout(nodes, connections);
                break;
            case FORCE_DIRECTED:
                forceDirectedLayout(nodes, connections);
                break;
            case TREE:
                treeLayout(nodes, connections);
                break;
            default:
                gridLayout(nodes);
                break;
        }
        
        log("✓ " + algorithm.name() + " 布局完成");
    }
    
    /**
     * 网格布局（原有简单布局）
     */
    private void gridLayout(List<ProcessNode> nodes) {
        int cols = (int) Math.ceil(Math.sqrt(nodes.size()));
        int spacing = 200;
        int startX = 100;
        int startY = 100;
        
        int col = 0;
        int row = 0;
        for (ProcessNode node : nodes) {
            node.setLayoutX(startX + col * spacing);
            node.setLayoutY(startY + row * spacing);
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }
    
    /**
     * 层次化布局 - 基于节点依赖关系自动排列，上游节点在上方，下游节点在下方
     */
    private void hierarchicalLayout(List<ProcessNode> nodes, List<NodeConnection> connections) {
        // 构建依赖关系图
        Map<ProcessNode, Set<ProcessNode>> graph = buildGraph(nodes, connections);
        Map<ProcessNode, Integer> levels = calculateLevels(nodes, graph);
        
        // 按层级分组节点
        Map<Integer, List<ProcessNode>> levelGroups = new HashMap<>();
        for (ProcessNode node : nodes) {
            int level = levels.getOrDefault(node, 0);
            levelGroups.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
        }
        
        // 计算每层的最大节点数
        int maxNodesPerLevel = levelGroups.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(1);
        
        // 布局节点
        double currentY = START_Y;
        for (int level = 0; level <= levelGroups.keySet().stream().mapToInt(Integer::intValue).max().orElse(0); level++) {
            List<ProcessNode> levelNodes = levelGroups.getOrDefault(level, new ArrayList<>());
            if (levelNodes.isEmpty()) continue;
            
            // 计算该层节点的总宽度
            double levelWidth = levelNodes.size() * HORIZONTAL_SPACING;
            double startX = START_X + (maxNodesPerLevel * HORIZONTAL_SPACING - levelWidth) / 2;
            
            // 水平排列该层的节点
            for (int i = 0; i < levelNodes.size(); i++) {
                ProcessNode node = levelNodes.get(i);
                node.setLayoutX(startX + i * HORIZONTAL_SPACING);
                node.setLayoutY(currentY);
            }
            
            currentY += VERTICAL_SPACING;
        }
    }
    
    /**
     * 力导向布局 - 模拟物理力场，自动优化节点位置，减少连线交叉
     */
    private void forceDirectedLayout(List<ProcessNode> nodes, List<NodeConnection> connections) {
        // 初始化节点位置（随机分布）
        Random random = new Random(42); // 固定种子以便结果可重现
        for (ProcessNode node : nodes) {
            node.setLayoutX(START_X + random.nextDouble() * 500);
            node.setLayoutY(START_Y + random.nextDouble() * 500);
        }
        
        // 构建连接关系
        Map<ProcessNode, Set<ProcessNode>> graph = buildGraph(nodes, connections);
        
        // 迭代优化位置
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Map<ProcessNode, Point2D> forces = new HashMap<>();
            
            // 初始化力向量
            for (ProcessNode node : nodes) {
                forces.put(node, new Point2D(0, 0));
            }
            
            // 计算弹簧力（连接的节点之间）
            for (NodeConnection conn : connections) {
                ProcessNode source = conn.getSourceNode();
                ProcessNode target = conn.getTargetNode();
                
                if (source != null && target != null && nodes.contains(source) && nodes.contains(target)) {
                    Point2D sourcePos = new Point2D(source.getLayoutX(), source.getLayoutY());
                    Point2D targetPos = new Point2D(target.getLayoutX(), target.getLayoutY());
                    Point2D delta = targetPos.subtract(sourcePos);
                    double distance = delta.magnitude();
                    
                    if (distance > 0) {
                        // 弹簧力：F = k * (distance - idealLength)
                        double force = SPRING_STRENGTH * (distance - SPRING_LENGTH);
                        Point2D forceVector = delta.normalize().multiply(force);
                        
                        forces.put(source, forces.get(source).add(forceVector));
                        forces.put(target, forces.get(target).subtract(forceVector));
                    }
                }
            }
            
            // 计算排斥力（所有节点之间）
            for (int i = 0; i < nodes.size(); i++) {
                ProcessNode node1 = nodes.get(i);
                Point2D pos1 = new Point2D(node1.getLayoutX(), node1.getLayoutY());
                
                for (int j = i + 1; j < nodes.size(); j++) {
                    ProcessNode node2 = nodes.get(j);
                    Point2D pos2 = new Point2D(node2.getLayoutX(), node2.getLayoutY());
                    Point2D delta = pos2.subtract(pos1);
                    double distance = delta.magnitude();
                    
                    if (distance > 0) {
                        // 排斥力：F = k / distance^2
                        double force = REPULSION_STRENGTH / (distance * distance);
                        Point2D forceVector = delta.normalize().multiply(force);
                        
                        forces.put(node1, forces.get(node1).subtract(forceVector));
                        forces.put(node2, forces.get(node2).add(forceVector));
                    }
                }
            }
            
            // 应用力并更新位置（带阻尼）
            double damping = 0.1;
            for (ProcessNode node : nodes) {
                Point2D force = forces.get(node);
                double newX = node.getLayoutX() + force.getX() * damping;
                double newY = node.getLayoutY() + force.getY() * damping;
                
                // 确保节点不超出画布边界
                newX = Math.max(START_X, Math.min(newX, START_X + 2000));
                newY = Math.max(START_Y, Math.min(newY, START_Y + 1500));
                
                node.setLayoutX(newX);
                node.setLayoutY(newY);
            }
        }
    }
    
    /**
     * 树形布局 - 对于树状结构，自动生成清晰的树形排列
     */
    private void treeLayout(List<ProcessNode> nodes, List<NodeConnection> connections) {
        // 构建依赖关系图
        Map<ProcessNode, Set<ProcessNode>> graph = buildGraph(nodes, connections);
        
        // 找到根节点（没有入边的节点）
        Set<ProcessNode> hasIncoming = new HashSet<>();
        for (NodeConnection conn : connections) {
            if (conn.getTargetNode() != null) {
                hasIncoming.add(conn.getTargetNode());
            }
        }
        
        List<ProcessNode> roots = new ArrayList<>();
        for (ProcessNode node : nodes) {
            if (!hasIncoming.contains(node)) {
                roots.add(node);
            }
        }
        
        // 如果没有根节点，选择第一个节点作为根
        if (roots.isEmpty() && !nodes.isEmpty()) {
            roots.add(nodes.get(0));
        }
        
        // 递归布局树
        Map<ProcessNode, Integer> subtreeSizes = calculateSubtreeSizes(nodes, graph);
        double currentX = START_X;
        
        for (ProcessNode root : roots) {
            currentX = layoutTree(root, graph, subtreeSizes, currentX, START_Y, 0);
            currentX += HORIZONTAL_SPACING; // 多个根节点之间的间距
        }
    }
    
    /**
     * 递归布局树节点
     */
    private double layoutTree(ProcessNode node, Map<ProcessNode, Set<ProcessNode>> graph,
                              Map<ProcessNode, Integer> subtreeSizes, double x, double y, int depth) {
        Set<ProcessNode> children = graph.getOrDefault(node, new HashSet<>());
        
        if (children.isEmpty()) {
            // 叶子节点
            node.setLayoutX(x);
            node.setLayoutY(y);
            return x + HORIZONTAL_SPACING;
        }
        
        // 计算子树的总宽度
        double subtreeWidth = 0;
        for (ProcessNode child : children) {
            int size = subtreeSizes.getOrDefault(child, 1);
            subtreeWidth += size * HORIZONTAL_SPACING;
        }
        
        // 父节点居中
        double nodeX = x + subtreeWidth / 2 - HORIZONTAL_SPACING / 2;
        node.setLayoutX(nodeX);
        node.setLayoutY(y);
        
        // 布局子节点
        double childX = x;
        double childY = y + VERTICAL_SPACING;
        
        for (ProcessNode child : children) {
            int size = subtreeSizes.getOrDefault(child, 1);
            childX = layoutTree(child, graph, subtreeSizes, childX, childY, depth + 1);
        }
        
        return x + subtreeWidth;
    }
    
    /**
     * 构建节点依赖关系图
     */
    private Map<ProcessNode, Set<ProcessNode>> buildGraph(List<ProcessNode> nodes, List<NodeConnection> connections) {
        Map<ProcessNode, Set<ProcessNode>> graph = new HashMap<>();
        
        // 初始化所有节点
        for (ProcessNode node : nodes) {
            graph.put(node, new HashSet<>());
        }
        
        // 添加边
        for (NodeConnection conn : connections) {
            ProcessNode source = conn.getSourceNode();
            ProcessNode target = conn.getTargetNode();
            
            if (source != null && target != null && nodes.contains(source) && nodes.contains(target)) {
                graph.get(source).add(target);
            }
        }
        
        return graph;
    }
    
    /**
     * 计算节点的层级（拓扑排序）
     */
    private Map<ProcessNode, Integer> calculateLevels(List<ProcessNode> nodes, Map<ProcessNode, Set<ProcessNode>> graph) {
        Map<ProcessNode, Integer> levels = new HashMap<>();
        Map<ProcessNode, Integer> inDegree = new HashMap<>();
        
        // 初始化入度
        for (ProcessNode node : nodes) {
            inDegree.put(node, 0);
        }
        
        // 计算入度
        for (Set<ProcessNode> targets : graph.values()) {
            for (ProcessNode target : targets) {
                inDegree.put(target, inDegree.getOrDefault(target, 0) + 1);
            }
        }
        
        // 拓扑排序
        Queue<ProcessNode> queue = new LinkedList<>();
        for (ProcessNode node : nodes) {
            if (inDegree.get(node) == 0) {
                queue.offer(node);
                levels.put(node, 0);
            }
        }
        
        while (!queue.isEmpty()) {
            ProcessNode current = queue.poll();
            int currentLevel = levels.get(current);
            
            for (ProcessNode neighbor : graph.getOrDefault(current, new HashSet<>())) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                    levels.put(neighbor, currentLevel + 1);
                }
            }
        }
        
        // 处理孤立节点
        for (ProcessNode node : nodes) {
            if (!levels.containsKey(node)) {
                levels.put(node, 0);
            }
        }
        
        return levels;
    }
    
    /**
     * 计算子树大小（用于树形布局）
     */
    private Map<ProcessNode, Integer> calculateSubtreeSizes(List<ProcessNode> nodes, Map<ProcessNode, Set<ProcessNode>> graph) {
        Map<ProcessNode, Integer> sizes = new HashMap<>();
        
        // 递归计算
        for (ProcessNode node : nodes) {
            calculateSubtreeSize(node, graph, sizes);
        }
        
        return sizes;
    }
    
    /**
     * 递归计算子树大小
     */
    private int calculateSubtreeSize(ProcessNode node, Map<ProcessNode, Set<ProcessNode>> graph, Map<ProcessNode, Integer> sizes) {
        if (sizes.containsKey(node)) {
            return sizes.get(node);
        }
        
        Set<ProcessNode> children = graph.getOrDefault(node, new HashSet<>());
        if (children.isEmpty()) {
            sizes.put(node, 1);
            return 1;
        }
        
        int size = 1;
        for (ProcessNode child : children) {
            size += calculateSubtreeSize(child, graph, sizes);
        }
        
        sizes.put(node, size);
        return size;
    }
    
    private void log(String message) {
        if (loggerCallback != null) {
            loggerCallback.accept(message);
        }
        logger.info(message);
    }
}
