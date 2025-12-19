package com.cc.job.gui.manager;

import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.NodeStatusSyncManager;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 画布节点管理器 - 负责节点的添加、删除、移动等操作
 */
public class CanvasNodeManager {
    
    private final Pane canvas;
    private final List<ProcessNode> nodes;
    private final Runnable notifyChanged;
    private final Consumer<String> logger;
    
    private static final String NODE_LISTENER_KEY = "nodeCanvasListenersAttached";
    
    public CanvasNodeManager(Pane canvas, List<ProcessNode> nodes, Runnable notifyChanged, Consumer<String> logger) {
        this.canvas = canvas;
        this.nodes = nodes;
        this.notifyChanged = notifyChanged;
        this.logger = logger;
    }
    
    /**
     * 添加节点
     */
    public void addNode(ProcessNode node) {
        if (node == null || nodes.contains(node)) return;
        
        nodes.add(node);
        if (!canvas.getChildren().contains(node)) {
            canvas.getChildren().add(node);
        }
        
        // 监听节点位置变化
        if (!Boolean.TRUE.equals(node.getProperties().get(NODE_LISTENER_KEY))) {
            node.layoutXProperty().addListener((obs, oldVal, newVal) -> notifyChanged.run());
            node.layoutYProperty().addListener((obs, oldVal, newVal) -> notifyChanged.run());
            node.getProperties().put(NODE_LISTENER_KEY, Boolean.TRUE);
        }
        
        logger.accept("✓ 添加节点: " + node.getJobHandlerName());
        notifyChanged.run();
    }
    
    /**
     * 移除节点及其相关连接
     */
    public List<NodeConnection> removeNode(ProcessNode node, List<NodeConnection> connections) {
        if (node == null) return new ArrayList<>();
        
        List<NodeConnection> attachedConnections = new ArrayList<>();
        for (NodeConnection conn : new ArrayList<>(connections)) {
            Node sourceOwner = conn.getSourceOwner();
            Node targetOwner = conn.getTargetOwner();
            
            if ((sourceOwner instanceof ProcessNode && sourceOwner == node) ||
                (targetOwner instanceof ProcessNode && targetOwner == node)) {
                attachedConnections.add(conn);
            }
        }
        
        nodes.remove(node);
        canvas.getChildren().remove(node);
        
        logger.accept("✓ 删除节点: " + node.getJobHandlerName());
        notifyChanged.run();
        
        return attachedConnections;
    }
    
    /**
     * 根据 jobId 查找节点
     */
    public ProcessNode getNodeByJobId(Long jobId) {
        if (jobId == null) return null;
        
        for (ProcessNode node : nodes) {
            if (node.getJobId() != null && node.getJobId().equals(jobId)) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * 更新节点状态
     */
    public void updateNodeStatusByJobId(Long jobId, Integer statusCode) {
        if (jobId == null || statusCode == null) return;
        
        for (ProcessNode node : nodes) {
            Long nodeJobId = node.getJobId();
            if (nodeJobId != null && nodeJobId.equals(jobId)) {
                node.updateStatusByCode(statusCode);
                NodeStatusSyncManager.getInstance().addPendingUpdate(jobId, statusCode);
                break;
            }
        }
    }
    
    /**
     * 刷新所有节点状态从缓存
     */
    public void refreshAllNodeStatusFromCache() {
        NodeStatusSyncManager statusManager = NodeStatusSyncManager.getInstance();
        
        for (ProcessNode node : nodes) {
            Long jobId = node.getJobId();
            if (jobId != null) {
                Integer cachedStatus = statusManager.getCachedStatus(jobId);
                if (cachedStatus != null) {
                    node.updateStatusByCode(cachedStatus);
                }
            }
        }
    }
    
    /**
     * 查找指定位置的节点
     */
    public ProcessNode findNodeAtPosition(double x, double y) {
        for (ProcessNode node : nodes) {
            double nodeX = node.getLayoutX();
            double nodeY = node.getLayoutY();
            double nodeWidth = node.getPrefWidth();
            double nodeHeight = node.getPrefHeight();
            
            if (x >= nodeX - 15 && x <= nodeX + nodeWidth + 15 &&
                y >= nodeY - 15 && y <= nodeY + nodeHeight + 15) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * 找到节点上距离指定位置最近的连接点
     */
    public Circle findNearestConnector(ProcessNode node, double x, double y) {
        Circle[] connectors = {
            node.getTopConnector(),
            node.getBottomConnector(),
            node.getLeftConnector(),
            node.getRightConnector()
        };
        
        Circle nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Circle connector : connectors) {
            Point2D connectorCenter = new Point2D(
                connector.getLayoutX() + connector.getRadius(),
                connector.getLayoutY() + connector.getRadius()
            );
            Point2D nodeLocal = node.getConnectorPane().localToParent(connectorCenter);
            Point2D canvasLocal = node.localToParent(nodeLocal);
            
            double dx = canvasLocal.getX() - x;
            double dy = canvasLocal.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = connector;
            }
        }
        
        return nearest;
    }
    
    /**
     * 获取连接点的位置名称
     */
    public String getConnectorPosition(ProcessNode node, Circle connector) {
        if (connector == node.getTopConnector()) return "顶部";
        if (connector == node.getBottomConnector()) return "底部";
        if (connector == node.getLeftConnector()) return "左侧";
        if (connector == node.getRightConnector()) return "右侧";
        return "未知";
    }
}

