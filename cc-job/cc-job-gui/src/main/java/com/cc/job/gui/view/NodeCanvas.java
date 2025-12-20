package com.cc.job.gui.view;

import com.cc.job.gui.history.CanvasAction;
import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.manager.*;
import com.cc.job.gui.model.GroupContainer;
import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.NodeStatusSyncManager;
import javafx.animation.AnimationTimer;
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
    
    // 画布自动扩展相关
    private static final double EDGE_THRESHOLD = 100.0; // 距离边缘的阈值（像素）
    private static final double EXPAND_SIZE = 500.0;    // 每次扩展的大小
    
    // 窗口自动滚动相关
    private static final double VIEWPORT_EDGE_THRESHOLD = 50.0; // 距离可视窗口边缘的阈值（像素）
    private static final double SCROLL_SPEED = 0.02;            // 滚动速度系数
    
    // 防止重复扩展
    private boolean isExpanding = false; // 标记是否正在执行扩展操作
    private boolean isDragging = false;  // 标记是否有节点正在被拖拽
    
    // 拖拽优化：节流控制
    private long lastExpandCheckTime = 0;
    private long lastScrollCheckTime = 0;
    private static final long EXPAND_THROTTLE_MS = 50;   // 扩展检查节流间隔（50ms）
    private static final long SCROLL_THROTTLE_MS = 16;   // 滚动检查节流间隔（约16ms，约60fps）
    
    // 拖拽过程中的平滑滚动
    private AnimationTimer dragScrollTimer;
    private ProcessNode currentDragNode;
    private volatile boolean scrollAnimationRunning = false;
    
    public interface LogCallback {
        void log(String message);
    }
    
    public NodeCanvas() {
        setPrefSize(2000, 1000);
        setStyle("-fx-background-color: #F3F4F6;");
        
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
        
        // 检查新添加的节点是否需要扩展画布
        checkAndExpandCanvas(node);
        
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
            // 优化：使用节流检查画布扩展和滚动
            throttledCheckAndExpandCanvas(node);
            throttledCheckAndScrollViewport(node);
            handleNodeDrag(node);
        });
        
        node.setOnDragStarted(() -> {
            isDragging = true; // 标记开始拖拽
            currentDragNode = node; // 记录当前拖拽节点
            startDragScrollAnimation(); // 启动平滑滚动动画
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
            isDragging = false; // 标记拖拽结束
            stopDragScrollAnimation(); // 停止滚动动画
            currentDragNode = null;
            
            // 拖拽结束后，检查是否需要左侧或上侧扩展
            checkAndExpandCanvas(node);
            
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
    
    /**
     * 启动拖拽时的平滑滚动动画
     */
    private void startDragScrollAnimation() {
        if (dragScrollTimer != null || scrollAnimationRunning) return;
        
        scrollAnimationRunning = true;
        dragScrollTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentDragNode != null && isDragging) {
                    performSmoothScroll(currentDragNode);
                }
            }
        };
        dragScrollTimer.start();
    }
    
    /**
     * 停止拖拽滚动动画
     */
    private void stopDragScrollAnimation() {
        if (dragScrollTimer != null) {
            dragScrollTimer.stop();
            dragScrollTimer = null;
        }
        scrollAnimationRunning = false;
    }
    
    /**
     * 节流版的画布扩展检查
     */
    private void throttledCheckAndExpandCanvas(ProcessNode node) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExpandCheckTime >= EXPAND_THROTTLE_MS) {
            lastExpandCheckTime = currentTime;
            checkAndExpandCanvas(node);
        }
    }
    
    /**
     * 节流版的视窗滚动检查
     */
    private void throttledCheckAndScrollViewport(ProcessNode node) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScrollCheckTime >= SCROLL_THROTTLE_MS) {
            lastScrollCheckTime = currentTime;
            // 滚动由 AnimationTimer 处理，这里只更新节点引用
            currentDragNode = node;
        }
    }
    
    private void handleNodeDrag(ProcessNode node) {
        // 优化：画布扩展检查已在 onDragged 中通过节流处理
        
        if (selectionManager.isMovingSelection() && selectionManager.getDragStartNode() == node && 
            selectionManager.getSelectedNodes().size() > 1) {
            
            double currentX = node.getLayoutX();
            double currentY = node.getLayoutY();
            double[] dragStartOriginalPos = selectionManager.getSelectionOriginalPositions().get(node);
            
            if (dragStartOriginalPos != null) {
                double deltaX = currentX - dragStartOriginalPos[0];
                double deltaY = currentY - dragStartOriginalPos[1];
                
                // 优化：批量更新节点位置，减少重复的画布扩展检查
                double maxX = currentX;
                double maxY = currentY;
                
                for (ProcessNode selectedNode : selectionManager.getSelectedNodes()) {
                    if (selectedNode != node) {
                        double[] originalPos = selectionManager.getSelectionOriginalPositions().get(selectedNode);
                        if (originalPos != null) {
                            double newX = Math.max(0, originalPos[0] + deltaX);
                            double newY = Math.max(0, originalPos[1] + deltaY);
                            selectedNode.setLayoutX(newX);
                            selectedNode.setLayoutY(newY);
                            
                            // 记录最大坐标，用于统一检查画布扩展
                            maxX = Math.max(maxX, newX + selectedNode.getWidth());
                            maxY = Math.max(maxY, newY + selectedNode.getHeight());
                        }
                    }
                }
                
                // 优化：只对最远端的节点进行画布扩展检查
                checkCanvasBoundsForMultiDrag(maxX, maxY);
                
                selectionManager.updateSelectionBoundingBox();
            }
        }
    }
    
    /**
     * 多节点拖拽时的画布边界检查（优化版）
     */
    private void checkCanvasBoundsForMultiDrag(double maxX, double maxY) {
        if (isExpanding) return;
        
        double currentWidth = getPrefWidth();
        double currentHeight = getPrefHeight();
        
        boolean needsExpansion = false;
        double newWidth = currentWidth;
        double newHeight = currentHeight;
        
        // 只检查右侧和底部边缘（拖拽时的常见场景）
        if (maxX > currentWidth - EDGE_THRESHOLD) {
            newWidth = maxX + EXPAND_SIZE;
            needsExpansion = true;
        }
        
        if (maxY > currentHeight - EDGE_THRESHOLD) {
            newHeight = maxY + EXPAND_SIZE;
            needsExpansion = true;
        }
        
        if (needsExpansion) {
            isExpanding = true;
            setPrefSize(newWidth, newHeight);
            setMinSize(newWidth, newHeight);
            isExpanding = false;
        }
    }
    
    /**
     * 执行平滑滚动（由 AnimationTimer 调用）
     */
    private void performSmoothScroll(ProcessNode node) {
        if (node == null || hostingScrollPane == null) return;
        
        double nodeX = node.getLayoutX();
        double nodeY = node.getLayoutY();
        
        double nodeWidth = node.getWidth() > 0 ? node.getWidth() : node.getPrefWidth();
        double nodeHeight = node.getHeight() > 0 ? node.getHeight() : node.getPrefHeight();
        
        if (nodeWidth <= 0 || nodeHeight <= 0) return;
        
        // 节点中心点
        double nodeCenterX = nodeX + nodeWidth / 2;
        double nodeCenterY = nodeY + nodeHeight / 2;
        
        double viewportWidth = hostingScrollPane.getViewportBounds().getWidth();
        double viewportHeight = hostingScrollPane.getViewportBounds().getHeight();
        
        double currentHValue = hostingScrollPane.getHvalue();
        double currentVValue = hostingScrollPane.getVvalue();
        
        double canvasWidth = getPrefWidth();
        double canvasHeight = getPrefHeight();
        
        // 计算可视区域在画布上的位置
        double scrollableWidth = canvasWidth - viewportWidth;
        double scrollableHeight = canvasHeight - viewportHeight;
        
        if (scrollableWidth <= 0 && scrollableHeight <= 0) return;
        
        double viewportLeft = scrollableWidth > 0 ? currentHValue * scrollableWidth : 0;
        double viewportRight = viewportLeft + viewportWidth;
        double viewportTop = scrollableHeight > 0 ? currentVValue * scrollableHeight : 0;
        double viewportBottom = viewportTop + viewportHeight;
        
        double newHValue = currentHValue;
        double newVValue = currentVValue;
        boolean needsScroll = false;
        
        // 优化的滚动速度计算 - 使用更平滑的插值
        double smoothScrollSpeed = 0.008; // 更小的基础滚动速度，更平滑
        
        // 检查左边缘
        if (nodeCenterX < viewportLeft + VIEWPORT_EDGE_THRESHOLD && scrollableWidth > 0) {
            double distance = (viewportLeft + VIEWPORT_EDGE_THRESHOLD) - nodeCenterX;
            double speedMultiplier = Math.min(distance / VIEWPORT_EDGE_THRESHOLD, 2.0);
            newHValue = Math.max(0, currentHValue - smoothScrollSpeed * speedMultiplier);
            needsScroll = true;
        }
        // 检查右边缘
        else if (nodeCenterX > viewportRight - VIEWPORT_EDGE_THRESHOLD && scrollableWidth > 0) {
            double distance = nodeCenterX - (viewportRight - VIEWPORT_EDGE_THRESHOLD);
            double speedMultiplier = Math.min(distance / VIEWPORT_EDGE_THRESHOLD, 2.0);
            newHValue = Math.min(1, currentHValue + smoothScrollSpeed * speedMultiplier);
            needsScroll = true;
        }
        
        // 检查上边缘
        if (nodeCenterY < viewportTop + VIEWPORT_EDGE_THRESHOLD && scrollableHeight > 0) {
            double distance = (viewportTop + VIEWPORT_EDGE_THRESHOLD) - nodeCenterY;
            double speedMultiplier = Math.min(distance / VIEWPORT_EDGE_THRESHOLD, 2.0);
            newVValue = Math.max(0, currentVValue - smoothScrollSpeed * speedMultiplier);
            needsScroll = true;
        }
        // 检查下边缘
        else if (nodeCenterY > viewportBottom - VIEWPORT_EDGE_THRESHOLD && scrollableHeight > 0) {
            double distance = nodeCenterY - (viewportBottom - VIEWPORT_EDGE_THRESHOLD);
            double speedMultiplier = Math.min(distance / VIEWPORT_EDGE_THRESHOLD, 2.0);
            newVValue = Math.min(1, currentVValue + smoothScrollSpeed * speedMultiplier);
            needsScroll = true;
        }
        
        // 执行平滑滚动
        if (needsScroll) {
            hostingScrollPane.setHvalue(newHValue);
            hostingScrollPane.setVvalue(newVValue);
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
    
    /**
     * 检查节点是否靠近画布边缘，如果是则自动扩展画布
     * 支持四个方向的扩展：左、右、上、下
     */
    private void checkAndExpandCanvas(ProcessNode node) {
        if (node == null) return;
        
        // 如果正在执行扩展操作，跳过本次检查，防止重复扩展
        if (isExpanding) return;
        
        double nodeX = node.getLayoutX();
        double nodeY = node.getLayoutY();
        
        // 获取节点实际尺寸，如果为0则使用预设尺寸
        double nodeWidth = node.getWidth() > 0 ? node.getWidth() : node.getPrefWidth();
        double nodeHeight = node.getHeight() > 0 ? node.getHeight() : node.getPrefHeight();
        
        // 防止出现异常值
        if (nodeWidth <= 0 || nodeHeight <= 0) {
            return;
        }
        
        // 节点的四个边缘
        double nodeRight = nodeX + nodeWidth;
        double nodeBottom = nodeY + nodeHeight;
        
        // 当前画布尺寸
        double currentWidth = getPrefWidth();
        double currentHeight = getPrefHeight();
        
        boolean needsExpansion = false;
        double newWidth = currentWidth;
        double newHeight = currentHeight;
        double offsetX = 0; // 向右偏移量（左侧扩展时使用）
        double offsetY = 0; // 向下偏移量（上侧扩展时使用）
        
        // 检查左边缘 - 当节点靠近左边缘时扩展（拖拽时禁用，避免坐标系统冲突）
        if (nodeX < EDGE_THRESHOLD && !isDragging) {
            offsetX = EXPAND_SIZE;
            newWidth = currentWidth + EXPAND_SIZE;
            needsExpansion = true;
        }
        
        // 检查右边缘 - 当节点靠近右边缘时扩展
        if (nodeRight > currentWidth - EDGE_THRESHOLD) {
            newWidth = Math.max(newWidth, nodeRight + EXPAND_SIZE + offsetX);
            needsExpansion = true;
        }
        
        // 检查上边缘 - 当节点靠近上边缘时扩展（拖拽时禁用，避免坐标系统冲突）
        if (nodeY < EDGE_THRESHOLD && !isDragging) {
            offsetY = EXPAND_SIZE;
            newHeight = currentHeight + EXPAND_SIZE;
            needsExpansion = true;
        }
        
        // 检查底边缘 - 当节点靠近底边缘时扩展
        if (nodeBottom > currentHeight - EDGE_THRESHOLD) {
            newHeight = Math.max(newHeight, nodeBottom + EXPAND_SIZE + offsetY);
            needsExpansion = true;
        }
        
        // 如果需要扩展，则执行扩展
        if (needsExpansion && !isExpanding) {
            // 设置扩展标志，防止重复扩展
            isExpanding = true;
            
            // 如果是左侧或上侧扩展，需要调整所有节点和容器的位置
            if (offsetX > 0 || offsetY > 0) {
                adjustAllNodesPosition(offsetX, offsetY);
            }
            
            setPrefSize(newWidth, newHeight);
            setMinSize(newWidth, newHeight);
            
            // 调整 ScrollPane 的滚动位置，保持视图稳定
            if (hostingScrollPane != null && (offsetX > 0 || offsetY > 0)) {
                adjustScrollPaneAfterExpansion(offsetX, offsetY, currentWidth, currentHeight, newWidth, newHeight);
            }
            
            String direction = "";
            if (offsetX > 0) direction += "左";
            if (offsetY > 0) direction += "上";
            if (nodeRight > currentWidth - EDGE_THRESHOLD) direction += "右";
            if (nodeBottom > currentHeight - EDGE_THRESHOLD) direction += "下";
            
            log(String.format("🔄 画布已向%s扩展至: %.0f x %.0f", direction, newWidth, newHeight));
            
            // 重置扩展标志
            isExpanding = false;
        }
    }
    
    /**
     * 调整所有节点和容器的位置（用于左侧或上侧扩展时）
     */
    private void adjustAllNodesPosition(double offsetX, double offsetY) {
        // 调整所有节点的位置
        for (ProcessNode processNode : nodes) {
            if (processNode != null) {
                processNode.setLayoutX(processNode.getLayoutX() + offsetX);
                processNode.setLayoutY(processNode.getLayoutY() + offsetY);
                // 同时调整拖拽起始坐标，避免拖拽时节点被"拉回"原位置
                processNode.adjustDragStart(offsetX, offsetY);
            }
        }
        
        // 调整所有组容器的位置
        for (GroupContainer container : groupContainers) {
            if (container != null) {
                container.setLayoutX(container.getLayoutX() + offsetX);
                container.setLayoutY(container.getLayoutY() + offsetY);
            }
        }
        
        // 调整多选时的原始位置记录
        if (selectionManager != null && selectionManager.getSelectionOriginalPositions() != null) {
            for (Map.Entry<ProcessNode, double[]> entry : selectionManager.getSelectionOriginalPositions().entrySet()) {
                double[] pos = entry.getValue();
                if (pos != null && pos.length >= 2) {
                    pos[0] += offsetX;
                    pos[1] += offsetY;
                }
            }
        }
        
        // 连接线会自动跟随节点位置更新
    }
    
    /**
     * 调整 ScrollPane 的滚动位置，保持视图稳定
     */
    private void adjustScrollPaneAfterExpansion(double offsetX, double offsetY, 
                                                double oldWidth, double oldHeight,
                                                double newWidth, double newHeight) {
        if (hostingScrollPane == null) return;
        
        double viewportWidth = hostingScrollPane.getViewportBounds().getWidth();
        double viewportHeight = hostingScrollPane.getViewportBounds().getHeight();
        
        // 如果画布小于视口，不需要调整滚动
        if (oldWidth <= viewportWidth && oldHeight <= viewportHeight) {
            return;
        }
        
        // 计算当前视图在画布上的实际位置
        double currentHValue = hostingScrollPane.getHvalue();
        double currentVValue = hostingScrollPane.getVvalue();
        
        // 计算偏移后应该的滚动位置，保持视图内容不变
        if (offsetX > 0 && newWidth > viewportWidth) {
            double oldScrollableWidth = oldWidth - viewportWidth;
            double newScrollableWidth = newWidth - viewportWidth;
            double oldViewportLeft = currentHValue * oldScrollableWidth;
            double newViewportLeft = oldViewportLeft + offsetX;
            double newHValue = newScrollableWidth > 0 ? newViewportLeft / newScrollableWidth : 0;
            hostingScrollPane.setHvalue(Math.max(0, Math.min(1, newHValue)));
        }
        
        if (offsetY > 0 && newHeight > viewportHeight) {
            double oldScrollableHeight = oldHeight - viewportHeight;
            double newScrollableHeight = newHeight - viewportHeight;
            double oldViewportTop = currentVValue * oldScrollableHeight;
            double newViewportTop = oldViewportTop + offsetY;
            double newVValue = newScrollableHeight > 0 ? newViewportTop / newScrollableHeight : 0;
            hostingScrollPane.setVvalue(Math.max(0, Math.min(1, newVValue)));
        }
    }
    
    /**
     * 检查节点是否靠近可视窗口边缘，如果是则自动滚动窗口
     * 注意：此方法已被 performSmoothScroll 替代，保留以便兼容
     * @deprecated 使用 performSmoothScroll 获取更平滑的滚动体验
     */
    @SuppressWarnings("unused")
    private void checkAndScrollViewport(ProcessNode node) {
        // 实际滚动由 AnimationTimer (performSmoothScroll) 处理
        // 此方法保留以便向后兼容
        if (node == null || hostingScrollPane == null) return;
        
        // 滚动逻辑已移至 performSmoothScroll
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


