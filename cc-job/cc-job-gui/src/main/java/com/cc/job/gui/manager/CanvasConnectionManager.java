package com.cc.job.gui.manager;

import com.cc.job.gui.model.ConditionNode;
import com.cc.job.gui.model.GroupContainer;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 画布连接管理器 - 负责连接线的创建、删除和管理
 */
public class CanvasConnectionManager {
    
    private final Pane canvas;
    private final List<NodeConnection> connections;
    private final List<GroupContainer> groupContainers;
    private final List<ConditionNode> conditionNodes;
    private final Runnable notifyChanged;
    private final Consumer<String> logger;
    
    public CanvasConnectionManager(Pane canvas, List<NodeConnection> connections, 
                                   List<GroupContainer> groupContainers,
                                   List<ConditionNode> conditionNodes,
                                   Runnable notifyChanged, Consumer<String> logger) {
        this.canvas = canvas;
        this.connections = connections;
        this.groupContainers = groupContainers;
        this.conditionNodes = conditionNodes != null ? conditionNodes : new ArrayList<>();
        this.notifyChanged = notifyChanged;
        this.logger = logger;
    }
    
    /**
     * 添加连接线
     */
    public NodeConnection addConnection(javafx.scene.Node sourceOwner, Pane sourceConnectorParent, Circle sourceConnector,
                                        javafx.scene.Node targetOwner, Pane targetConnectorParent, Circle targetConnector) {
        NodeConnection connection = new NodeConnection(sourceOwner, sourceConnectorParent, sourceConnector,
                                                       targetOwner, targetConnectorParent, targetConnector);
        configureConnectionInteractions(connection);
        addConnectionInternal(connection);
        notifyChanged.run();
        return connection;
    }
    
    /**
     * 添加连接线（ProcessNode 到 ProcessNode）
     */
    public NodeConnection addConnection(ProcessNode source, Circle sourceConnector,
                                        ProcessNode target, Circle targetConnector) {
        NodeConnection connection = new NodeConnection(source, sourceConnector, target, targetConnector);
        configureConnectionInteractions(connection);
        addConnectionInternal(connection);
        notifyChanged.run();
        return connection;
    }
    
    public void addConnectionInternal(NodeConnection connection) {
        if (connection == null || connections.contains(connection)) return;
        
        connections.add(connection);
        if (!canvas.getChildren().contains(connection)) {
            boolean isManagedByContainer = false;
            for (GroupContainer container : groupContainers) {
                List<NodeConnection> managedConnections = container.getManagedConnections();
                if (managedConnections != null && managedConnections.contains(connection)) {
                    isManagedByContainer = true;
                    break;
                }
            }
            // 检查是否由条件节点管理
            if (!isManagedByContainer) {
                for (ConditionNode conditionNode : conditionNodes) {
                    List<NodeConnection> managedConnections = conditionNode.getManagedConnections();
                    if (managedConnections != null && managedConnections.contains(connection)) {
                        isManagedByContainer = true;
                        break;
                    }
                }
            }
            
            if (isManagedByContainer) {
                canvas.getChildren().add(connection);
                connection.toFront();
            } else {
                canvas.getChildren().add(0, connection);
            }
        }
        logConnection("✓ 添加连接", connection);
    }
    
    /**
     * 移除连接线
     */
    public void removeConnection(NodeConnection connection) {
        if (connection == null) return;
        
        connections.remove(connection);
        canvas.getChildren().remove(connection);
        logConnection("✓ 删除连接", connection);
        notifyChanged.run();
    }
    
    /**
     * 根据 edgeId 移除连接
     */
    public boolean removeConnectionByEdgeId(String edgeId) {
        if (edgeId == null) return false;
        
        for (NodeConnection connection : new ArrayList<>(connections)) {
            if (edgeIdMatches(edgeId, connection.getEdgeId())) {
                removeConnection(connection);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 设置所有连接的运行状态
     */
    public void setAllConnectionsRunning(boolean running) {
        for (NodeConnection conn : connections) {
            conn.setRunning(running);
        }
    }
    
    private void configureConnectionInteractions(NodeConnection connection) {
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除连接");
        deleteItem.setStyle("-fx-text-fill: #EF4444;");
        deleteItem.setOnAction(e -> {
            String sourceName = getOwnerName(connection.getSourceOwner());
            String targetName = getOwnerName(connection.getTargetOwner());
            logger.accept("🗑️ 准备删除连接: " + sourceName + " → " + targetName);
            removeConnection(connection);
            logger.accept("提示: 删除后需点击保存按钮以持久化任务组变更");
        });
        
        menu.getItems().add(deleteItem);
        
        connection.setOnContextMenuRequested(event -> {
            connection.toFront();
            connection.setSelected(true);
            menu.show(connection, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        
        connection.setOnMousePressed(event -> {
            if (menu.isShowing()) menu.hide();
            connection.setSelected(false);
        });
        
        menu.setOnHidden(event -> connection.setSelected(false));
    }
    
    private void logConnection(String prefix, NodeConnection connection) {
        if (connection == null) return;
        
        String sourceName = getOwnerName(connection.getSourceOwner());
        String targetName = getOwnerName(connection.getTargetOwner());
        logger.accept(prefix + ": " + sourceName + " → " + targetName);
    }
    
    private String getOwnerName(javafx.scene.Node owner) {
        if (owner instanceof ProcessNode) {
            return ((ProcessNode) owner).getJobHandlerName();
        } else if (owner instanceof GroupContainer) {
            return ((GroupContainer) owner).getGroupName();
        }
        return "未知";
    }
    
    private boolean edgeIdMatches(String requestedId, String existingId) {
        if (existingId == null) return false;
        String normalizedRequested = normalizeEdgeId(requestedId);
        String normalizedExisting = normalizeEdgeId(existingId);
        return !normalizedRequested.isEmpty() && normalizedRequested.equals(normalizedExisting);
    }
    
    private String normalizeEdgeId(String rawId) {
        if (rawId == null) return "";
        String result = rawId.trim();
        int colonIndex = result.lastIndexOf(':');
        if (colonIndex >= 0 && colonIndex < result.length() - 1) {
            result = result.substring(colonIndex + 1);
        }
        if (result.endsWith(".0")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }
}

