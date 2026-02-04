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
    
    // 布局参数（间距为节点外的空隙，节点实际宽高用 prefWidth/prefHeight）
    private static final double MIN_NODE_WIDTH = 120;
    private static final double MIN_NODE_HEIGHT = 60;
    private static final double GAP = 40; // 节点之间的最小间隙
    private static final double START_X = 100; // 起始X坐标
    private static final double START_Y = 100; // 起始Y坐标
    
    // 力导向布局参数
    private static final double SPRING_STRENGTH = 0.08; // 弹簧强度
    private static final double REPULSION_STRENGTH = 8000; // 排斥力强度
    private static final int ITERATIONS = 350; // 迭代次数
    private static final double FORCE_DAMPING = 0.12; // 阻尼
    
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
     * 获取节点实际宽度（用于布局计算）
     */
    private static double nodeWidth(ProcessNode node) {
        double w = node.getPrefWidth();
        return w > 0 ? w : MIN_NODE_WIDTH;
    }
    
    /**
     * 获取节点实际高度（用于布局计算）
     */
    private static double nodeHeight(ProcessNode node) {
        double h = node.getPrefHeight();
        return h > 0 ? h : MIN_NODE_HEIGHT;
    }
    
    /**
     * 网格布局 - 按节点实际宽高计算列宽、行高，避免重叠
     */
    private void gridLayout(List<ProcessNode> nodes) {
        int cols = (int) Math.ceil(Math.sqrt(nodes.size()));
        if (cols < 1) cols = 1;
        // 每列宽度 = 该列节点最大宽度 + 间隙
        double[] colWidths = new double[cols];
        for (int i = 0; i < nodes.size(); i++) {
            int c = i % cols;
            colWidths[c] = Math.max(colWidths[c], nodeWidth(nodes.get(i)) + GAP);
        }
        double startX = START_X;
        double startY = START_Y;
        double[] colX = new double[cols];
        for (int c = 0; c < cols; c++) {
            colX[c] = startX;
            startX += colWidths[c];
        }
        double rowHeight = 0;
        int col = 0;
        double currentY = startY;
        for (ProcessNode node : nodes) {
            double h = nodeHeight(node);
            rowHeight = Math.max(rowHeight, h + GAP);
            node.setLayoutX(colX[col]);
            node.setLayoutY(currentY);
            col++;
            if (col >= cols) {
                col = 0;
                currentY += rowHeight;
                rowHeight = 0;
            }
        }
    }
    
    /**
     * 层次化布局 - 基于节点依赖关系自动排列，上游在上方、下游在下方；按节点实际宽高留间距
     */
    private void hierarchicalLayout(List<ProcessNode> nodes, List<NodeConnection> connections) {
        Map<ProcessNode, Set<ProcessNode>> graph = buildGraph(nodes, connections);
        Map<ProcessNode, Integer> levels = calculateLevels(nodes, graph);
        
        Map<Integer, List<ProcessNode>> levelGroups = new HashMap<>();
        for (ProcessNode node : nodes) {
            int level = levels.getOrDefault(node, 0);
            levelGroups.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
        }
        
        int maxLevel = levelGroups.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        double currentY = START_Y;
        for (int level = 0; level <= maxLevel; level++) {
            List<ProcessNode> levelNodes = levelGroups.getOrDefault(level, new ArrayList<>());
            if (levelNodes.isEmpty()) continue;
            
            double maxHeight = 0;
            for (ProcessNode node : levelNodes) {
                maxHeight = Math.max(maxHeight, nodeHeight(node));
            }
            double startX = START_X;
            for (ProcessNode node : levelNodes) {
                double w = nodeWidth(node);
                node.setLayoutX(startX);
                node.setLayoutY(currentY);
                startX += w + GAP;
            }
            currentY += maxHeight + GAP;
        }
    }
    
    /**
     * 力导向布局 - 按节点实际宽高引入最小距离，减少重叠与边交叉
     */
    private void forceDirectedLayout(List<ProcessNode> nodes, List<NodeConnection> connections) {
        Random random = new Random(42);
        double span = 400;
        for (ProcessNode node : nodes) {
            node.setLayoutX(START_X + random.nextDouble() * span);
            node.setLayoutY(START_Y + random.nextDouble() * span);
        }
        
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Map<ProcessNode, Point2D> forces = new HashMap<>();
            for (ProcessNode node : nodes) {
                forces.put(node, new Point2D(0, 0));
            }
            
            // 弹簧力：理想长度 = 两节点“半径”和 + 间隙（按中心距）
            for (NodeConnection conn : connections) {
                ProcessNode source = conn.getSourceNode();
                ProcessNode target = conn.getTargetNode();
                if (source == null || target == null || !nodes.contains(source) || !nodes.contains(target)) continue;
                double w1 = nodeWidth(source); double h1 = nodeHeight(source);
                double w2 = nodeWidth(target); double h2 = nodeHeight(target);
                double idealLen = Math.max((w1 + w2) / 2, (h1 + h2) / 2) + GAP;
                Point2D p1 = new Point2D(source.getLayoutX() + w1 / 2, source.getLayoutY() + h1 / 2);
                Point2D p2 = new Point2D(target.getLayoutX() + w2 / 2, target.getLayoutY() + h2 / 2);
                Point2D delta = p2.subtract(p1);
                double dist = delta.magnitude();
                if (dist > 1e-6) {
                    double f = SPRING_STRENGTH * (dist - idealLen);
                    Point2D vec = delta.normalize().multiply(f);
                    forces.put(source, forces.get(source).add(vec));
                    forces.put(target, forces.get(target).subtract(vec));
                }
            }
            
            // 排斥力：中心距小于最小距离时加强排斥，避免包围盒重叠
            for (int i = 0; i < nodes.size(); i++) {
                ProcessNode n1 = nodes.get(i);
                double w1 = nodeWidth(n1); double h1 = nodeHeight(n1);
                Point2D c1 = new Point2D(n1.getLayoutX() + w1 / 2, n1.getLayoutY() + h1 / 2);
                for (int j = i + 1; j < nodes.size(); j++) {
                    ProcessNode n2 = nodes.get(j);
                    double w2 = nodeWidth(n2); double h2 = nodeHeight(n2);
                    Point2D c2 = new Point2D(n2.getLayoutX() + w2 / 2, n2.getLayoutY() + h2 / 2);
                    Point2D delta = c2.subtract(c1);
                    double dist = delta.magnitude();
                    double minDist = (w1 + w2) / 2 + (h1 + h2) / 4 + GAP; // 中心最小距离，避免重叠
                    if (dist < 1e-6) {
                        delta = new Point2D(1, 0);
                        dist = 1;
                    }
                    double forceMag = dist < minDist
                            ? REPULSION_STRENGTH * (minDist - dist) / (dist + 1)
                            : REPULSION_STRENGTH / (dist * dist);
                    Point2D vec = delta.normalize().multiply(forceMag);
                    forces.put(n1, forces.get(n1).subtract(vec));
                    forces.put(n2, forces.get(n2).add(vec));
                }
            }
            
            for (ProcessNode node : nodes) {
                Point2D f = forces.get(node);
                double newX = node.getLayoutX() + f.getX() * FORCE_DAMPING;
                double newY = node.getLayoutY() + f.getY() * FORCE_DAMPING;
                newX = Math.max(START_X, Math.min(newX, START_X + 2000));
                newY = Math.max(START_Y, Math.min(newY, START_Y + 1500));
                node.setLayoutX(newX);
                node.setLayoutY(newY);
            }
        }
    }
    
    /**
     * 树形布局 - 按节点实际宽高与子树宽度分配，避免重叠
     */
    private void treeLayout(List<ProcessNode> nodes, List<NodeConnection> connections) {
        Map<ProcessNode, Set<ProcessNode>> graph = buildGraph(nodes, connections);
        Set<ProcessNode> hasIncoming = new HashSet<>();
        for (NodeConnection conn : connections) {
            if (conn.getTargetNode() != null) {
                hasIncoming.add(conn.getTargetNode());
            }
        }
        List<ProcessNode> roots = new ArrayList<>();
        for (ProcessNode node : nodes) {
            if (!hasIncoming.contains(node)) roots.add(node);
        }
        if (roots.isEmpty() && !nodes.isEmpty()) roots.add(nodes.get(0));
        
        Map<ProcessNode, Double> subtreeWidths = new HashMap<>();
        for (ProcessNode node : nodes) {
            calculateSubtreeWidth(node, graph, subtreeWidths);
        }
        double currentX = START_X;
        for (ProcessNode root : roots) {
            currentX = layoutTree(root, graph, subtreeWidths, currentX, START_Y) + GAP;
        }
    }
    
    /**
     * 递归计算子树占用宽度（像素）
     */
    private double calculateSubtreeWidth(ProcessNode node, Map<ProcessNode, Set<ProcessNode>> graph,
                                         Map<ProcessNode, Double> out) {
        if (out.containsKey(node)) return out.get(node);
        Set<ProcessNode> children = graph.getOrDefault(node, new HashSet<>());
        double width;
        if (children.isEmpty()) {
            width = nodeWidth(node) + GAP;
        } else {
            width = 0;
            for (ProcessNode child : children) {
                width += calculateSubtreeWidth(child, graph, out);
            }
            width = Math.max(width, nodeWidth(node) + GAP);
        }
        out.put(node, width);
        return width;
    }
    
    /**
     * 递归布局树节点，按子树宽度分配 x
     */
    private double layoutTree(ProcessNode node, Map<ProcessNode, Set<ProcessNode>> graph,
                              Map<ProcessNode, Double> subtreeWidths, double x, double y) {
        double w = nodeWidth(node);
        double h = nodeHeight(node);
        Set<ProcessNode> children = graph.getOrDefault(node, new HashSet<>());
        if (children.isEmpty()) {
            node.setLayoutX(x);
            node.setLayoutY(y);
            return x + w + GAP;
        }
        double totalChildWidth = 0;
        for (ProcessNode child : children) {
            totalChildWidth += subtreeWidths.getOrDefault(child, w + GAP);
        }
        double nodeX = x + totalChildWidth / 2 - w / 2;
        node.setLayoutX(nodeX);
        node.setLayoutY(y);
        double childY = y + h + GAP;
        double childX = x;
        for (ProcessNode child : children) {
            childX = layoutTree(child, graph, subtreeWidths, childX, childY);
            childX += GAP;
        }
        return childX - GAP; // 返回子树右端，父节点会再加 GAP 作为下一兄弟起点
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
    
    private void log(String message) {
        if (loggerCallback != null) {
            loggerCallback.accept(message);
        }
        logger.info(message);
    }
}
