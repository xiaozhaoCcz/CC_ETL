package com.cc.job.gui.view;

import com.cc.job.gui.history.CanvasAction;
import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.manager.*;
import com.cc.job.gui.model.GroupContainer;
import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.NodeStatusSyncManager;
import javafx.animation.PauseTransition;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.*;

/**
 * 画布 - 用于管理节点和连接线（简化版）
 */
public class NodeCanvas extends Pane {
    
    private List<ProcessNode> nodes = new ArrayList<>();
    private List<GroupContainer> groupContainers = new ArrayList<>();
    private List<NodeConnection> connections = new ArrayList<>();
    
    // 管理器
    private CanvasNodeManager nodeManager;
    private CanvasConnectionManager connectionManager;
    private CanvasSelectionManager selectionManager;
    private CanvasDataLoader dataLoader;
    
    private UndoRedoManager undoRedoManager;
    private boolean historyEnabled = true;
    
    // 回调
    private Runnable onRequestAddNode;
    private Runnable onRequestRunTaskGroup;
    private Runnable onRequestClearCanvas;
    private java.util.function.Consumer<GroupContainer> onDeleteGroupContainer;
    private LogCallback logCallback;
    private Runnable onNodeMoved;
    private java.util.function.Consumer<Boolean> onSelectionModeChanged;
    
    // 临时连线相关
    private Object startOwner;
    private Line tempLine;
    
    private ScrollPane hostingScrollPane;
    
    public interface LogCallback {
        void log(String message);
    }
    
    public NodeCanvas() {
        setPrefSize(2000, 1000);
        setStyle("-fx-background-color: gray;");
        
        initializeManagers();
        setupCanvasContextMenu();
        setupSelectionHandlers();
    }
    
    private void initializeManagers() {
        nodeManager = new CanvasNodeManager(this, nodes, this::notifyNodeStructureChanged, this::log);
        connectionManager = new CanvasConnectionManager(this, connections, groupContainers, this::notifyNodeStructureChanged, this::log);
        selectionManager = new CanvasSelectionManager(this, nodes, connections, this::log, this::notifyNodeStructureChanged);
        dataLoader = new CanvasDataLoader(this::log);
    }
    
    // ==================== Setters ====================
    
    public void setLogCallback(LogCallback callback) {
        this.logCallback = callback;
    }
    
    public void setOnLog(LogCallback callback) {
        this.logCallback = callback;
    }
    
    public void setOnNodeMoved(Runnable callback) {
        this.onNodeMoved = callback;
    }
    
    public void setOnSelectionModeChanged(java.util.function.Consumer<Boolean> callback) {
        this.onSelectionModeChanged = callback;
    }
    
    public void setScrollPane(ScrollPane scrollPane) {
        this.hostingScrollPane = scrollPane;
    }
    
    public void setOnRequestAddNode(Runnable runnable) {
        this.onRequestAddNode = runnable;
    }
    
    public void setOnRequestRunTaskGroup(Runnable runnable) {
        this.onRequestRunTaskGroup = runnable;
    }
    
    public void setOnRequestClearCanvas(Runnable runnable) {
        this.onRequestClearCanvas = runnable;
    }
    
    public void setOnDeleteGroupContainer(java.util.function.Consumer<GroupContainer> callback) {
        this.onDeleteGroupContainer = callback;
    }
    
    public void setUndoRedoManager(UndoRedoManager undoRedoManager) {
        this.undoRedoManager = undoRedoManager;
    }
    
    // ==================== Getters ====================
    
    public List<ProcessNode> getNodes() {
        return nodes;
    }
    
    public List<NodeConnection> getConnections() {
        return connections;
    }
    
    public List<GroupContainer> getGroupContainers() {
        return new ArrayList<>(groupContainers);
    }
    
    public ProcessNode getNodeByJobId(Long jobId) {
        return nodeManager.getNodeByJobId(jobId);
    }
    
    public Set<ProcessNode> getSelectedNodes() {
        return selectionManager.getSelectedNodes();
    }
    
    public boolean isSelectionMode() {
        return selectionManager.isSelectionMode();
    }
    
    // ==================== 节点操作 ====================
    
    public void addNode(ProcessNode node, boolean recordHistory) {
        if (node == null) return;
        
        nodeManager.addNode(node);
        setupNodeCallbacks(node);
        
        if (recordHistory) {
            pushAction(new AddNodeAction(node));
        }
    }
    
    public void removeNode(ProcessNode node, boolean recordHistory) {
        if (node == null) return;
        
        double oldX = node.getLayoutX();
        double oldY = node.getLayoutY();
        
        List<NodeConnection> attachedConnections = nodeManager.removeNode(node, connections);
        
        for (NodeConnection conn : attachedConnections) {
            connectionManager.removeConnection(conn);
        }
        
        if (recordHistory) {
            pushAction(new RemoveNodeAction(node, oldX, oldY, attachedConnections));
        }
    }
    
    private void setupNodeCallbacks(ProcessNode node) {
        // 节点操作回调（需要由外部通过 configureNodeCallbacks 设置）
        // 这里只设置画布内部的回调
        node.setOnDelete(() -> removeNode(node, true));
        
        node.setOnDragged(() -> {
            if (onNodeMoved != null) onNodeMoved.run();
            handleNodeDrag(node);
        });
        
        node.setOnDragStarted(() -> {
            if (!selectionManager.getSelectedNodes().isEmpty() && 
                selectionManager.getSelectedNodes().contains(node) && 
                selectionManager.getSelectedNodes().size() > 1) {
                selectionManager.setMovingSelection(true);
                selectionManager.setDragStartNode(node);
            }
        });
        
        node.setOnClicked(() -> {
            if (!selectionManager.isMovingSelection()) {
                selectionManager.selectNode(node);
            }
        });
        
        node.setOnDragFinished((oldX, oldY, newX, newY) -> {
            if (selectionManager.isMovingSelection() && selectionManager.getDragStartNode() == node) {
                selectionManager.setMovingSelection(false);
                selectionManager.setDragStartNode(null);
            }
            
            double deltaX = Math.abs(newX - oldX);
            double deltaY = Math.abs(newY - oldY);
            boolean isClick = deltaX < 3 && deltaY < 3;
            
            if (undoRedoManager != null && historyEnabled && !isClick) {
                pushAction(new MoveNodeAction(node, oldX, oldY, newX, newY));
            }
            notifyNodeStructureChanged();
        });
        
        // 设置连接点处理器
        setupConnectorHandler(node, node.getTopConnector());
        setupConnectorHandler(node, node.getBottomConnector());
        setupConnectorHandler(node, node.getLeftConnector());
        setupConnectorHandler(node, node.getRightConnector());
    }
    
    private void handleNodeDrag(ProcessNode node) {
        if (selectionManager.isMovingSelection() && selectionManager.getDragStartNode() == node && 
            selectionManager.getSelectedNodes().size() > 1) {
            
            double currentX = node.getLayoutX();
            double currentY = node.getLayoutY();
            double[] dragStartOriginalPos = selectionManager.getSelectionOriginalPositions().get(node);
            
            if (dragStartOriginalPos != null) {
                double deltaX = currentX - dragStartOriginalPos[0];
                double deltaY = currentY - dragStartOriginalPos[1];
                
                for (ProcessNode selectedNode : selectionManager.getSelectedNodes()) {
                    if (selectedNode != node) {
                        double[] originalPos = selectionManager.getSelectionOriginalPositions().get(selectedNode);
                        if (originalPos != null) {
                            selectedNode.setLayoutX(Math.max(0, originalPos[0] + deltaX));
                            selectedNode.setLayoutY(Math.max(0, originalPos[1] + deltaY));
                        }
                    }
                }
                selectionManager.updateSelectionBoundingBox();
            }
        }
    }
    
    // ==================== 连接线操作 ====================
    
    public NodeConnection addConnection(ProcessNode source, Circle sourceConnector,
                                        ProcessNode target, Circle targetConnector, boolean recordHistory) {
        NodeConnection connection = connectionManager.addConnection(source, sourceConnector, target, targetConnector);
        if (recordHistory) {
            pushAction(new AddConnectionAction(connection));
        }
        return connection;
    }
    
    public NodeConnection addConnection(ProcessNode source, ProcessNode target) {
        return addConnection(source, source.getRightConnector(), target, target.getLeftConnector(), true);
    }
    
    public void removeConnection(NodeConnection connection, boolean recordHistory) {
        connectionManager.removeConnection(connection);
        if (recordHistory) {
            pushAction(new RemoveConnectionAction(connection));
        }
    }
    
    public boolean removeConnectionByEdgeId(String edgeId) {
        return connectionManager.removeConnectionByEdgeId(edgeId);
    }
    
    public void setAllConnectionsRunning(boolean running) {
        connectionManager.setAllConnectionsRunning(running);
    }
    
    // ==================== 连接点处理 ====================
    
    private void setupConnectorHandler(ProcessNode node, Circle connector) {
        connector.setOnMousePressed(e -> {
            startOwner = node;
            startTempLine(node, connector);
            e.consume();
        });
        
        connector.setOnMouseDragged(e -> {
            if (tempLine != null) {
                Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                tempLine.setEndX(localPoint.getX());
                tempLine.setEndY(localPoint.getY());
            }
            e.consume();
        });
        
        connector.setOnMouseReleased(e -> {
            finishConnection(connector, e.getSceneX(), e.getSceneY());
            e.consume();
        });
    }
    
    private void startTempLine(ProcessNode node, Circle connector) {
        tempLine = new Line();
        tempLine.setStroke(Color.web("#8B5CF6"));
        tempLine.setStrokeWidth(2);
        tempLine.getStrokeDashArray().addAll(5.0, 5.0);
        
        Point2D center = new Point2D(connector.getLayoutX() + connector.getRadius(), 
                                     connector.getLayoutY() + connector.getRadius());
        Point2D nodeLocal = node.getConnectorPane().localToParent(center);
        Point2D canvasLocal = node.localToParent(nodeLocal);
        
        tempLine.setStartX(canvasLocal.getX());
        tempLine.setStartY(canvasLocal.getY());
        tempLine.setEndX(canvasLocal.getX());
        tempLine.setEndY(canvasLocal.getY());
        
        this.getChildren().add(tempLine);
    }
    
    private void finishConnection(Circle sourceConnector, double sceneX, double sceneY) {
        if (tempLine == null) return;
        
        log("📍 释放鼠标，检查目标节点...");
        
        Point2D local = sceneToLocal(sceneX, sceneY);
        ProcessNode targetNode = nodeManager.findNodeAtPosition(local.getX(), local.getY());
        
        if (targetNode != null && startOwner instanceof ProcessNode && targetNode != startOwner) {
            ProcessNode sourceNode = (ProcessNode) startOwner;
            log("✓ 找到目标节点: " + targetNode.getJobHandlerName());
            
            Circle targetConnector = nodeManager.findNearestConnector(targetNode, local.getX(), local.getY());
            
            boolean exists = connections.stream().anyMatch(conn ->
                conn.getSourceOwner() == startOwner && conn.getTargetOwner() == targetNode &&
                conn.getSourceConnector() == sourceConnector && conn.getTargetConnector() == targetConnector
            );
            
            if (!exists) {
                log("创建连接: " + sourceNode.getJobHandlerName() + " → " + targetNode.getJobHandlerName());
                addConnection(sourceNode, sourceConnector, targetNode, targetConnector, true);
                log("✓ 连接创建成功");
            } else {
                log("⚠️ 连接已存在");
            }
        } else {
            if (targetNode != null && targetNode == startOwner) {
                log("⚠️ 不能连接到自己");
            } else {
                log("❌ 取消连线（未找到目标节点）");
            }
        }
        
        cancelTempLine();
    }
    
    private void cancelTempLine() {
        if (tempLine != null) {
            this.getChildren().remove(tempLine);
            tempLine = null;
            startOwner = null;
        }
    }
    
    // ==================== 选择操作 ====================
    
    public void setSelectionMode(boolean enabled) {
        selectionManager.setSelectionMode(enabled);
        if (onSelectionModeChanged != null) onSelectionModeChanged.accept(enabled);
    }
    
    public void selectNode(ProcessNode node) { selectionManager.selectNode(node); }
    public void selectNodes(Collection<ProcessNode> nodesToSelect) { selectionManager.selectNodes(nodesToSelect); }
    public void clearSelection() { selectionManager.clearSelection(); }
    public void alignHorizontal() { selectionManager.alignHorizontal(); }
    public void alignVertical() { selectionManager.alignVertical(); }
    public Set<NodeConnection> getSelectedConnections() { return selectionManager.getSelectedConnections(); }
    public void updateSelectionBoundingBox() { selectionManager.updateSelectionBoundingBox(); }
    public void highlightNode(ProcessNode node, boolean highlight) { selectionManager.highlightNode(node, highlight); }
    
    public void addToSelection(ProcessNode node) {
        if (node != null && nodes.contains(node) && !selectionManager.getSelectedNodes().contains(node)) {
            Set<ProcessNode> current = selectionManager.getSelectedNodes();
            current.add(node);
            selectionManager.selectNodes(current);
        }
    }
    
    private void setupSelectionHandlers() {
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isPrimaryButtonDown() && selectionManager.isSelectionMode() && !isClickOnNodeOrEdge((javafx.scene.Node) e.getTarget())) {
                Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                selectionManager.startSelection(localPoint.getX(), localPoint.getY());
                e.consume();
            }
        });
        
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (selectionManager.isSelectionMode() && e.isPrimaryButtonDown() && selectionManager.getSelectionRect().isVisible()) {
                Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                selectionManager.updateSelection(localPoint.getX(), localPoint.getY());
                e.consume();
            }
        });
        
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            if (selectionManager.isSelectionMode() && selectionManager.getSelectionRect().isVisible()) {
                Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                selectionManager.finishSelection(localPoint.getX(), localPoint.getY());
                e.consume();
            }
        });
    }
    
    private boolean isClickOnNodeOrEdge(javafx.scene.Node target) {
        if (target instanceof ProcessNode || target instanceof NodeConnection) return true;
        javafx.scene.Node current = target.getParent();
        while (current != null && current != this) {
            if (current instanceof ProcessNode || current instanceof NodeConnection) return true;
            current = current.getParent();
        }
        return false;
    }
    
    // ==================== 画布右键菜单 ====================
    
    private void setupCanvasContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        MenuItem addNodeItem = new MenuItem("新增节点");
        addNodeItem.setOnAction(e -> { if (onRequestAddNode != null) onRequestAddNode.run(); });
        
        MenuItem clearItem = new MenuItem("清空页面");
        clearItem.setOnAction(e -> { if (onRequestClearCanvas != null) onRequestClearCanvas.run(); else clearViewOnly(); });
        
        MenuItem runGroupItem = new MenuItem("运行任务组");
        runGroupItem.setOnAction(e -> { if (onRequestRunTaskGroup != null) onRequestRunTaskGroup.run(); });
        
        menu.getItems().addAll(addNodeItem, clearItem, runGroupItem);
        
        this.setOnContextMenuRequested(e -> {
            if (!isClickOnNodeOrEdge((javafx.scene.Node) e.getTarget())) {
                menu.show(this, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });
    }
    
    // ==================== 数据加载 ====================
    
    public void loadFromComposeData(JobComposeData composeData) {
        if (composeData == null) {
            log("⚠ 没有数据可加载");
            return;
        }
        
        if (undoRedoManager != null) {
            undoRedoManager.clear();
        }
        
        runWithoutHistory(() -> {
            clear();
            
            Map<String, ProcessNode> nodeMap = dataLoader.loadNodes(composeData.getNodes(), node -> {
                nodeManager.addNode(node);
                setupNodeCallbacks(node);
            });
            
            // 加载连接
            if (composeData.getEdges() != null) {
                int successCount = 0;
                for (JobComposeData.EdgeData edgeData : composeData.getEdges()) {
                    ProcessNode sourceNode = nodeMap.get(edgeData.getSourceNodeId());
                    ProcessNode targetNode = nodeMap.get(edgeData.getTargetNodeId());
                    
                    if (sourceNode != null && targetNode != null) {
                        Circle sourceConnector = getConnectorByAnchor(sourceNode, edgeData.getSourceAnchor(), true);
                        Circle targetConnector = getConnectorByAnchor(targetNode, edgeData.getTargetAnchor(), false);
                        
                        if (sourceConnector != null && targetConnector != null) {
                            NodeConnection edge = connectionManager.addConnection(
                                sourceNode, sourceNode.getConnectorPane(), sourceConnector,
                                targetNode, targetNode.getConnectorPane(), targetConnector
                            );
                            if (edge != null) {
                                edge.setEdgeId(edgeData.getId());
                                successCount++;
                            }
                        }
                    }
                }
                log("✓ 加载了 " + successCount + " 条连接");
            }
            
            log("✓ 任务组数据加载完成");
        });
        
        notifyNodeStructureChanged();
    }
    
    private Circle getConnectorByAnchor(ProcessNode node, String anchor, boolean isSource) {
        if (anchor != null && !anchor.isEmpty()) {
            return switch (anchor.toLowerCase()) {
                case "top" -> node.getTopConnector();
                case "bottom" -> node.getBottomConnector();
                case "left" -> node.getLeftConnector();
                case "right" -> node.getRightConnector();
                default -> isSource ? node.getRightConnector() : node.getLeftConnector();
            };
        }
        return isSource ? node.getRightConnector() : node.getLeftConnector();
    }
    
    // ==================== 清空操作 ====================
    
    public void clear() {
        groupContainers.forEach(this.getChildren()::remove);
        groupContainers.clear();
        
        this.getChildren().clear();
        nodes.clear();
        connections.clear();
        
        // 重新添加选择矩形到画布
        if (selectionManager != null) {
            selectionManager.reattachToCanvas();
        }
        
        log("✓ 画布已清空");
    }
    
    public void clearViewOnly() {
        new ArrayList<>(connections).forEach(connectionManager::removeConnection);
        nodes.forEach(this.getChildren()::remove);
        nodes.clear();
        groupContainers.forEach(this.getChildren()::remove);
        groupContainers.clear();
        selectionManager.clearSelection();
        log("✓ 已清空页面");
    }
    
    // ==================== 状态更新 ====================
    
    public void updateNodeStatusByJobId(Long jobId, Integer statusCode) {
        nodeManager.updateNodeStatusByJobId(jobId, statusCode);
    }
    
    public void refreshAllNodeStatusFromCache() {
        nodeManager.refreshAllNodeStatusFromCache();
    }
    
    public void syncPendingNodeStatus() {
        NodeStatusSyncManager.getInstance().syncNow();
    }
    
    public void syncPendingNodeStatusBlocking() {
        NodeStatusSyncManager.getInstance().syncNowBlocking();
    }
    
    // ==================== 定位操作 ====================
    
    public boolean locateConnectionByEdgeId(String edgeId) {
        if (edgeId == null) return false;
        
        for (NodeConnection connection : connections) {
            if (normalizeEdgeId(edgeId).equals(normalizeEdgeId(connection.getEdgeId()))) {
                connection.toFront();
                connection.setSelected(true);
                connection.playLocateAnimation();
                PauseTransition delay = new PauseTransition(Duration.seconds(1.2));
                delay.setOnFinished(e -> connection.setSelected(false));
                delay.play();
                return true;
            }
        }
        return false;
    }
    
    private String normalizeEdgeId(String rawId) {
        if (rawId == null) return "";
        String result = rawId.trim();
        int colonIndex = result.lastIndexOf(':');
        if (colonIndex >= 0) result = result.substring(colonIndex + 1);
        if (result.endsWith(".0")) result = result.substring(0, result.length() - 2);
        return result;
    }
    
    // ==================== 任务组容器 ====================
    
    public void addGroupContainerToCollection(GroupContainer container) {
        if (container != null && !groupContainers.contains(container)) {
            groupContainers.add(container);
        }
    }
    
    public void removeGroupContainer(GroupContainer container) {
        if (container == null) return;
        
        List<NodeConnection> attachedConnections = new ArrayList<>();
        for (NodeConnection conn : new ArrayList<>(connections)) {
            if (conn.getSourceOwner() == container || conn.getTargetOwner() == container) {
                attachedConnections.add(conn);
                connectionManager.removeConnection(conn);
            }
        }
        
        for (ProcessNode managedNode : container.getManagedCanvasNodes()) {
            removeNode(managedNode, false);
        }
        
        groupContainers.remove(container);
        this.getChildren().remove(container);
        
        log("✓ 删除任务组容器: " + container.getGroupName());
        notifyNodeStructureChanged();
    }
    
    // ==================== 辅助方法 ====================
    
    private void notifyNodeStructureChanged() { if (onNodeMoved != null) onNodeMoved.run(); }
    private void log(String message) { if (logCallback != null) logCallback.log(message); }
    private void pushAction(CanvasAction action) { if (undoRedoManager != null && historyEnabled && action != null) undoRedoManager.push(action); }
    
    private void runWithoutHistory(Runnable runnable) {
        boolean previous = historyEnabled;
        historyEnabled = false;
        try { runnable.run(); } finally { historyEnabled = previous; }
    }
    
    // ==================== 历史记录 Actions ====================
    
    private class AddNodeAction implements CanvasAction {
        private final ProcessNode node;
        
        AddNodeAction(ProcessNode node) {
            this.node = node;
        }
        
        @Override
        public void undo() {
            removeNode(node, false);
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            addNode(node, false);
            notifyNodeStructureChanged();
        }
    }
    
    private class RemoveNodeAction implements CanvasAction {
        private final ProcessNode node;
        private final double oldX, oldY;
        private final List<NodeConnection> attachedConnections;
        
        RemoveNodeAction(ProcessNode node, double oldX, double oldY, List<NodeConnection> attachedConnections) {
            this.node = node;
            this.oldX = oldX;
            this.oldY = oldY;
            this.attachedConnections = new ArrayList<>(attachedConnections);
        }
        
        @Override
        public void undo() {
            addNode(node, false);
            node.setLayoutX(oldX);
            node.setLayoutY(oldY);
            for (NodeConnection connection : attachedConnections) {
                connectionManager.addConnectionInternal(connection);
            }
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            removeNode(node, false);
            notifyNodeStructureChanged();
        }
    }
    
    private class MoveNodeAction implements CanvasAction {
        private final ProcessNode node;
        private final double oldX, oldY, newX, newY;
        
        MoveNodeAction(ProcessNode node, double oldX, double oldY, double newX, double newY) {
            this.node = node;
            this.oldX = oldX;
            this.oldY = oldY;
            this.newX = newX;
            this.newY = newY;
        }
        
        @Override
        public void undo() {
            node.setLayoutX(oldX);
            node.setLayoutY(oldY);
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            node.setLayoutX(newX);
            node.setLayoutY(newY);
            notifyNodeStructureChanged();
        }
    }
    
    private class AddConnectionAction implements CanvasAction {
        private final NodeConnection connection;
        
        AddConnectionAction(NodeConnection connection) {
            this.connection = connection;
        }
        
        @Override
        public void undo() {
            connectionManager.removeConnection(connection);
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            connectionManager.addConnectionInternal(connection);
            notifyNodeStructureChanged();
        }
    }
    
    private class RemoveConnectionAction implements CanvasAction {
        private final NodeConnection connection;
        
        RemoveConnectionAction(NodeConnection connection) {
            this.connection = connection;
        }
        
        @Override
        public void undo() {
            connectionManager.addConnectionInternal(connection);
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            connectionManager.removeConnection(connection);
            notifyNodeStructureChanged();
        }
    }
}


