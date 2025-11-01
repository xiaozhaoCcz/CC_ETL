package com.example.nodefx.view;

import com.example.nodefx.model.JobComposeData;
import com.example.nodefx.model.NodeConnection;
import com.example.nodefx.model.ProcessNode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 画布，用于管理节点和连接线
 */
public class NodeCanvas extends Pane {
    
    private List<ProcessNode> nodes = new ArrayList<>();
    private List<NodeConnection> connections = new ArrayList<>();
    
    // 临时连线相关
    private ProcessNode startNode;
    private Circle startConnector;
    private Line tempLine;
    
    // 日志回调
    private LogCallback logCallback;
    private Runnable onNodeMoved; // 节点移动回调

    public interface LogCallback {
        void log(String message);
    }
    
    public NodeCanvas() {
        // 设置初始尺寸
        setPrefSize(2000, 1500);
        setStyle("-fx-background-color: #F3F4F6;");
    }
    
    public void setLogCallback(LogCallback callback) {
        this.logCallback = callback;
    }
    
    public void setOnNodeMoved(Runnable callback) {
        this.onNodeMoved = callback;
    }
    
    public void setOnLog(LogCallback callback) {
        this.logCallback = callback;
    }
    
    public List<ProcessNode> getNodes() {
        return nodes;
    }
    
    public List<NodeConnection> getConnections() {
        return connections;
    }
    
    private void log(String message) {
        System.out.println(message);
        if (logCallback != null) {
            logCallback.log(message);
        }
    }
    
    /**
     * 添加节点
     */
    public void addNode(ProcessNode node) {
        nodes.add(node);
        this.getChildren().add(node);
        
        // 为节点的连接点设置事件处理器
        setupConnectorHandler(node, node.getTopConnector());
        setupConnectorHandler(node, node.getBottomConnector());
        setupConnectorHandler(node, node.getLeftConnector());
        setupConnectorHandler(node, node.getRightConnector());
        
        // 设置删除回调
        node.setOnDelete(() -> removeNode(node));
        
        // 设置拖动回调 - 实时更新小地图
        node.setOnDragged(() -> {
            if (onNodeMoved != null) {
                onNodeMoved.run();
            }
        });
        
        // 监听节点位置变化，动态调整画布大小
        node.layoutXProperty().addListener((obs, oldVal, newVal) -> updateCanvasSize());
        node.layoutYProperty().addListener((obs, oldVal, newVal) -> updateCanvasSize());
        
        log("✓ 添加节点: " + node.getJobHandlerName());
        updateCanvasSize();
    }
    
    /**
     * 移除节点
     */
    public void removeNode(ProcessNode node) {
        // 移除相关的连接
        List<NodeConnection> toRemove = new ArrayList<>();
        for (NodeConnection conn : connections) {
            if (conn.getSourceNode() == node || conn.getTargetNode() == node) {
                toRemove.add(conn);
            }
        }
        toRemove.forEach(this::removeConnection);
        
        nodes.remove(node);
        this.getChildren().remove(node);
        
        log("✓ 删除节点: " + node.getJobHandlerName());
    }
    
    /**
     * 添加连接线（指定具体的连接点）
     */
    public void addConnection(ProcessNode source, Circle sourceConnector, 
                             ProcessNode target, Circle targetConnector) {
        NodeConnection connection = new NodeConnection(source, sourceConnector, target, targetConnector);
        connections.add(connection);
        
        // 将连接线添加到最底层（索引0），这样节点会显示在连接线之上
        // 由于连接点在节点外部，箭头和连接点仍然清晰可见
        this.getChildren().add(0, connection);
        
        String sourcePos = getConnectorPosition(source, sourceConnector);
        String targetPos = getConnectorPosition(target, targetConnector);
        
        log("✓ 添加连接: " + source.getJobHandlerName() + "[" + sourcePos + "] → " + 
            target.getJobHandlerName() + "[" + targetPos + "]");
    }
    
    /**
     * 添加连接线（自动选择连接点 - 兼容旧方法）
     */
    public void addConnection(ProcessNode source, ProcessNode target) {
        // 默认使用右侧连接到左侧
        addConnection(source, source.getRightConnector(), target, target.getLeftConnector());
    }
    
    /**
     * 移除连接线
     */
    public void removeConnection(NodeConnection connection) {
        connections.remove(connection);
        this.getChildren().remove(connection);
        
        log("✓ 删除连接: " + 
            connection.getSourceNode().getJobHandlerName() + " → " + 
            connection.getTargetNode().getJobHandlerName());
    }
    
    /**
     * 为连接点设置事件处理器
     */
    private void setupConnectorHandler(ProcessNode node, Circle connector) {
        // 按下连接点开始连线
        connector.setOnMousePressed(e -> {
            startConnector = connector;
            startNode = node;
            
            // 创建临时连线
            tempLine = new Line();
            tempLine.setStroke(Color.web("#8B5CF6"));
            tempLine.setStrokeWidth(2);
            tempLine.getStrokeDashArray().addAll(5.0, 5.0);
            
            // 设置起点 - 正确转换坐标
            // 连接点的中心位置相对于connectorPane
            javafx.geometry.Point2D connectorCenter = new javafx.geometry.Point2D(
                connector.getLayoutX() + connector.getRadius(),
                connector.getLayoutY() + connector.getRadius()
            );
            // 将connectorPane的坐标转换为节点的坐标
            javafx.geometry.Point2D nodeLocal = node.getConnectorPane().localToParent(connectorCenter);
            // 将节点的坐标转换为Canvas的坐标
            javafx.geometry.Point2D canvasLocal = node.localToParent(nodeLocal);
            
            tempLine.setStartX(canvasLocal.getX());
            tempLine.setStartY(canvasLocal.getY());
            tempLine.setEndX(canvasLocal.getX());
            tempLine.setEndY(canvasLocal.getY());
            
            this.getChildren().add(tempLine);
            
            log("开始连线: " + node.getJobHandlerName() + "[" + getConnectorPosition(node, connector) + "]");
            e.consume();
        });
        
        // 拖动时更新临时连线
        connector.setOnMouseDragged(e -> {
            if (tempLine != null) {
                // 将场景坐标转换为NodeCanvas的局部坐标
                javafx.geometry.Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                tempLine.setEndX(localPoint.getX());
                tempLine.setEndY(localPoint.getY());
            }
            e.consume();
        });
        
        // 释放鼠标，检查是否连接到另一个节点
        connector.setOnMouseReleased(e -> {
            if (tempLine != null) {
                log("📍 释放鼠标，检查目标节点...");
                
                // 检查鼠标释放位置是否在某个节点上
                ProcessNode targetNode = findNodeAtPosition(e.getSceneX(), e.getSceneY());
                
                if (targetNode != null && targetNode != startNode) {
                    // 找到最近的目标连接点
                    Circle targetConnector = findNearestConnector(targetNode, e.getSceneX(), e.getSceneY());
                    
                    // 检查是否已经存在相同的连接
                    boolean exists = connections.stream().anyMatch(conn ->
                        (conn.getSourceNode() == startNode && conn.getTargetNode() == targetNode &&
                         conn.getSourceConnector() == connector && conn.getTargetConnector() == targetConnector) ||
                        (conn.getSourceNode() == targetNode && conn.getTargetNode() == startNode &&
                         conn.getSourceConnector() == targetConnector && conn.getTargetConnector() == connector)
                    );
                    
                    if (!exists) {
                        // 创建连接，指定具体的连接点
                        addConnection(startNode, connector, targetNode, targetConnector);
                    } else {
                        log("⚠️ 连接已存在");
                    }
                } else {
                    if (targetNode == startNode) {
                        log("⚠️ 不能连接到自己");
                    } else {
                        log("❌ 取消连线（未找到目标节点）");
                    }
                }
                
                // 清除临时连线
                cancelTempLine();
            }
            e.consume();
        });
    }
    
    /**
     * 取消临时连线
     */
    private void cancelTempLine() {
        if (tempLine != null) {
            this.getChildren().remove(tempLine);
            tempLine = null;
            startConnector = null;
            startNode = null;
        }
    }
    
    /**
     * 查找指定位置的节点
     */
    private ProcessNode findNodeAtPosition(double sceneX, double sceneY) {
        // 将场景坐标转换为Canvas的局部坐标
        javafx.geometry.Point2D canvasPoint = sceneToLocal(sceneX, sceneY);
        double x = canvasPoint.getX();
        double y = canvasPoint.getY();
        
        System.out.println("   → 检测位置: sceneX=" + sceneX + ", sceneY=" + sceneY + 
                          " -> canvasX=" + x + ", canvasY=" + y);
        
        for (ProcessNode node : nodes) {
            double nodeX = node.getLayoutX();
            double nodeY = node.getLayoutY();
            double nodeWidth = node.getPrefWidth();
            double nodeHeight = node.getPrefHeight();
            
            System.out.println("   → 检查节点: " + node.getJobHandlerName() + 
                              " bounds=[" + nodeX + "," + nodeY + " " + nodeWidth + "x" + nodeHeight + "]");
            
            // 扩大检测范围（包括连接点突出部分）
            if (x >= nodeX - 15 && x <= nodeX + nodeWidth + 15 &&
                y >= nodeY - 15 && y <= nodeY + nodeHeight + 15) {
                System.out.println("   ✅ 找到目标节点: " + node.getJobHandlerName());
                return node;
            }
        }
        
        System.out.println("   ❌ 未找到目标节点");
        return null;
    }
    
    /**
     * 找到节点上距离指定位置最近的连接点
     */
    private Circle findNearestConnector(ProcessNode node, double sceneX, double sceneY) {
        // 将场景坐标转换为Canvas的局部坐标
        javafx.geometry.Point2D canvasPoint = sceneToLocal(sceneX, sceneY);
        double x = canvasPoint.getX();
        double y = canvasPoint.getY();
        
        Circle[] connectors = {
            node.getTopConnector(),
            node.getBottomConnector(),
            node.getLeftConnector(),
            node.getRightConnector()
        };
        
        Circle nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Circle connector : connectors) {
            // 计算连接点在Canvas中的位置
            javafx.geometry.Point2D connectorCenter = new javafx.geometry.Point2D(
                connector.getLayoutX() + connector.getRadius(),
                connector.getLayoutY() + connector.getRadius()
            );
            javafx.geometry.Point2D nodeLocal = node.getConnectorPane().localToParent(connectorCenter);
            javafx.geometry.Point2D canvasLocal = node.localToParent(nodeLocal);
            
            // 计算距离
            double dx = canvasLocal.getX() - x;
            double dy = canvasLocal.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = connector;
            }
        }
        
        log("   → 最近的连接点: " + getConnectorPosition(node, nearest) + " (距离: " + 
            String.format("%.1f", minDistance) + "px)");
        
        return nearest;
    }
    
    /**
     * 获取连接点的位置名称
     */
    private String getConnectorPosition(ProcessNode node, Circle connector) {
        if (connector == node.getTopConnector()) return "顶部";
        if (connector == node.getBottomConnector()) return "底部";
        if (connector == node.getLeftConnector()) return "左侧";
        if (connector == node.getRightConnector()) return "右侧";
        return "未知";
    }
    
    /**
     * 清空画布
     */
    public void clear() {
        this.getChildren().clear();
        nodes.clear();
        connections.clear();
        log("✓ 画布已清空");
        updateCanvasSize();
    }
    
    /**
     * 动态更新画布大小以包含所有节点
     */
    private void updateCanvasSize() {
        if (nodes.isEmpty()) {
            setPrefSize(2000, 1500);
            return;
        }
        
        // 计算所有节点的边界
        double maxX = 0;
        double maxY = 0;
        
        for (ProcessNode node : nodes) {
            double nodeRight = node.getLayoutX() + node.getPrefWidth() + 100; // 额外空间
            double nodeBottom = node.getLayoutY() + node.getPrefHeight() + 100;
            
            maxX = Math.max(maxX, nodeRight);
            maxY = Math.max(maxY, nodeBottom);
        }
        
        // 设置最小尺寸，确保画布至少有基本大小
        maxX = Math.max(maxX, 2000);
        maxY = Math.max(maxY, 1500);
        
        setPrefSize(maxX, maxY);
    }
    
    /**
     * 从 JobComposeData 加载节点和边
     * @param composeData 任务组合数据
     */
    public void loadFromComposeData(JobComposeData composeData) {
        if (composeData == null) {
            log("⚠ 没有数据可加载");
            return;
        }
        
        // 清空现有内容
        clear();
        
        // 用于存储节点ID到节点对象的映射
        Map<String, ProcessNode> nodeMap = new HashMap<>();
        
        // 加载节点
        List<JobComposeData.NodeData> nodeDataList = composeData.getNodes();
        if (nodeDataList != null && !nodeDataList.isEmpty()) {
            for (JobComposeData.NodeData nodeData : nodeDataList) {
                // 获取节点显示文本
                String text = nodeData.getJobName() != null ? nodeData.getJobName() : "Node";
                
                // 创建节点
                ProcessNode node = new ProcessNode(nodeData.getId(), text);
                
                // 设置位置
                if (nodeData.getX() != null && nodeData.getY() != null) {
                    node.setLayoutX(nodeData.getX());
                    node.setLayoutY(nodeData.getY());
                } else {
                    // 如果没有位置信息，使用默认布局
                    int index = nodeDataList.indexOf(nodeData);
                    node.setLayoutX(100 + (index % 3) * 250);
                    node.setLayoutY(100 + (index / 3) * 200);
                }
                
                // 添加节点到画布
                addNode(node);
                nodeMap.put(nodeData.getId(), node);
            }
            
            log("✓ 加载了 " + nodeDataList.size() + " 个节点");
        }
        
        // 加载边（连接线）
        List<JobComposeData.EdgeData> edgeDataList = composeData.getEdges();
        if (edgeDataList != null && !edgeDataList.isEmpty()) {
            int successCount = 0;
            for (JobComposeData.EdgeData edgeData : edgeDataList) {
                // 查找源节点和目标节点
                ProcessNode sourceNode = nodeMap.get(edgeData.getSourceNodeId());
                ProcessNode targetNode = nodeMap.get(edgeData.getTargetNodeId());
                
                if (sourceNode != null && targetNode != null) {
                    // 根据锚点确定连接点
                    // 源节点：如果没有指定锚点，默认使用右侧（数据流出）
                    Circle sourceConnector = getConnectorByAnchor(sourceNode, edgeData.getSourceAnchor(), true);
                    // 目标节点：如果没有指定锚点，默认使用左侧（数据流入）
                    Circle targetConnector = getConnectorByAnchor(targetNode, edgeData.getTargetAnchor(), false);
                    
                    if (sourceConnector != null && targetConnector != null) {
                        addConnection(sourceNode, sourceConnector, targetNode, targetConnector);
                        successCount++;
                    }
                } else {
                    log("⚠ 无法创建连接: 找不到节点 " + edgeData.getSourceNodeId() + " 或 " + edgeData.getTargetNodeId());
                }
            }
            
            log("✓ 加载了 " + successCount + " 条连接");
        }
        
        // 更新画布大小
        updateCanvasSize();
        
        log("✓ 任务组数据加载完成");
    }
    
    /**
     * 根据锚点名称获取连接器
     * @param node 节点
     * @param anchor 锚点名称（top/bottom/left/right）
     * @param isSource 是否为源节点（true=源节点默认右侧，false=目标节点默认左侧）
     */
    private Circle getConnectorByAnchor(ProcessNode node, String anchor, boolean isSource) {
        if (anchor == null || "null".equals(anchor)) {
            // 如果没有指定锚点，使用默认值
            // 源节点默认右侧（数据流出），目标节点默认左侧（数据流入）
            return isSource ? node.getRightConnector() : node.getLeftConnector();
        }
        
        return switch (anchor.toLowerCase()) {
            case "top" -> node.getTopConnector();
            case "bottom" -> node.getBottomConnector();
            case "left" -> node.getLeftConnector();
            case "right" -> node.getRightConnector();
            default -> isSource ? node.getRightConnector() : node.getLeftConnector();
        };
    }
}
