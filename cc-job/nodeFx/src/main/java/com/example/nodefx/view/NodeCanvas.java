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
     * 根据锚点字符串获取对应的连接点
     * @param node 节点
     * @param anchor 锚点字符串，可能为 "top", "bottom", "left", "right" 或 null
     * @param isSource 是否为源节点（true=源节点，false=目标节点）
     * @return 连接点Circle对象
     */
    private Circle getConnectorByAnchor(ProcessNode node, String anchor, boolean isSource) {
        if (anchor != null && !anchor.isEmpty()) {
            // 根据锚点字符串返回对应的连接点（不区分大小写）
            String anchorLower = anchor.toLowerCase();
            if ("top".equals(anchorLower)) {
                return node.getTopConnector();
            } else if ("bottom".equals(anchorLower)) {
                return node.getBottomConnector();
            } else if ("left".equals(anchorLower)) {
                return node.getLeftConnector();
            } else if ("right".equals(anchorLower)) {
                return node.getRightConnector();
            }
        }
        
        // 如果没有指定锚点，使用默认值
        // 源节点默认使用右侧（数据流出）
        // 目标节点默认使用左侧（数据流入）
        if (isSource) {
            return node.getRightConnector();
        } else {
            return node.getLeftConnector();
        }
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
                
                // ⭐ 设置任务ID（jobId）
                if (nodeData.getJobId() != null) {
                    node.setJobId(nodeData.getJobId());
                    System.out.println("✅ 节点 " + text + " (nodeId: " + nodeData.getId() + ") 已设置jobId: " + nodeData.getJobId());
                    log("✅ 节点已设置jobId: " + text + " -> jobId: " + nodeData.getJobId());
                } else {
                    System.out.println("⚠️ 节点 " + text + " (nodeId: " + nodeData.getId() + ") 的jobId为空！");
                    log("⚠️ 警告: 节点 " + text + " 的jobId为空！");
                }
                
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
     * 设置所有边的运行状态（任务组运行时调用）
     * @param running 是否运行中
     */
    public void setAllConnectionsRunning(boolean running) {
        for (NodeConnection conn : connections) {
            conn.setRunning(running);
        }
        System.out.println("📊 所有边的运行状态已更新: " + (running ? "运行中" : "停止"));
    }
    
    /**
     * 根据jobId更新节点状态
     * @param jobId 任务ID
     * @param statusCode 状态码：0=失败, 1=成功, 2=运行中
     */
    public void updateNodeStatusByJobId(Long jobId, Integer statusCode) {
        if (jobId == null || statusCode == null) {
            System.out.println("⚠️ 参数无效: jobId=" + jobId + ", statusCode=" + statusCode);
            return;
        }
        
        System.out.println("🔍 开始查找节点: jobId=" + jobId + ", statusCode=" + statusCode);
        System.out.println("📋 画布中共有 " + nodes.size() + " 个节点");
        
        boolean found = false;
        for (ProcessNode node : nodes) {
            Long nodeJobId = node.getJobId();
            System.out.println("   → 检查节点: " + node.getJobHandlerName() + ", jobId=" + nodeJobId);
            
            if (nodeJobId != null && nodeJobId.equals(jobId)) {
                System.out.println("✅ 找到匹配的节点: " + node.getJobHandlerName() + " (jobId=" + jobId + ")");
                System.out.println("   当前状态: " + node.getStatus());
                System.out.println("   即将更新为状态码: " + statusCode);
                
                node.updateStatusByCode(statusCode);
                
                System.out.println("✅ 节点状态已更新: jobId=" + jobId + ", statusCode=" + statusCode);
                System.out.println("   更新后状态: " + node.getStatus());
                
                found = true;
                break; // 找到节点后更新并退出
            }
        }
        
        if (!found) {
            System.out.println("⚠️ 未找到jobId=" + jobId + "的节点");
            System.out.println("📋 画布中的节点jobId列表:");
            for (ProcessNode node : nodes) {
                System.out.println("   - " + node.getJobHandlerName() + ": jobId=" + node.getJobId());
            }
        }
    }
    
    /**
     * 获取指定jobId的节点
     * @param jobId 任务ID
     * @return 节点对象，如果未找到返回null
     */
    public ProcessNode getNodeByJobId(Long jobId) {
        if (jobId == null) {
            return null;
        }
        
        for (ProcessNode node : nodes) {
            if (node.getJobId() != null && node.getJobId().equals(jobId)) {
                return node;
            }
        }
        
        return null;
    }
}

