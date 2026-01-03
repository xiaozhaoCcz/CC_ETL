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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
    private boolean autoSaveEnabled = true; // 控制自动保存标记是否启用
    
    // 回调
    private Runnable onRequestAddNode;
    private Runnable onRequestRunTaskGroup;
    private Runnable onRequestClearCanvas;
    private java.util.function.Consumer<GroupContainer> onDeleteGroupContainer;
    private LogCallback logCallback;
    private Runnable onNodeMoved;
    private java.util.function.Consumer<Boolean> onSelectionModeChanged;
    private Runnable onRequestSave; // 保存数据回调
    
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
    
    // 主题相关
    private String currentTheme = "default"; // "default", "grid", "dots"
    private Canvas dotsBackgroundCanvas; // 用于绘制圆点背景的Canvas
    private Canvas gridBackgroundCanvas; // 用于绘制网格背景的Canvas
    private boolean dotsBackgroundListenersAdded = false; // 标记是否已添加监听器
    private boolean gridBackgroundListenersAdded = false; // 标记是否已添加监听器
    
    // 自动保存相关
    private PauseTransition autoSaveTransition; // 自动保存延迟触发器
    private static final double AUTO_SAVE_DELAY_SECONDS = 0.5; // 保存延迟时间（秒）
    private boolean hasUnsavedChanges = false; // 标记是否有未保存的更改
    
    public interface LogCallback {
        void log(String message);
    }
    
    public NodeCanvas() {
        setPrefSize(2000, 1000);
        setStyle("-fx-background-color: #F3F4F6;");
        
        initializeManagers();
        setupCanvasContextMenu();
        setupSelectionHandlers();
        setupAutoSave();
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
    
    /**
     * 设置自动保存回调
     * 当鼠标移开画布且有未保存更改时，会自动调用此回调
     * @param callback 保存操作的回调函数
     */
    public void setOnRequestSave(Runnable callback) {
        this.onRequestSave = callback;
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
        
        // 标记有未保存的更改
        markAsUnsaved();
    }
    
    public void removeNode(ProcessNode node, boolean recordHistory) {
        if (node == null) return;
        
        double oldX = node.getLayoutX();
        double oldY = node.getLayoutY();
        
        // 从全局管理器移除节点所有状态（图状态和启用/禁用状态）
        if (node.getJobId() != null) {
            com.cc.job.gui.util.NodeGraphStateManager.getInstance().removeAllNodeStates(node.getJobId());
        }
        
        List<NodeConnection> attachedConnections = nodeManager.removeNode(node, connections);
        
        for (NodeConnection conn : attachedConnections) {
            connectionManager.removeConnection(conn);
        }
        
        if (recordHistory) {
            pushAction(new RemoveNodeAction(node, oldX, oldY, attachedConnections));
        }
        
        // 标记有未保存的更改
        markAsUnsaved();
    }
    
    private void setupNodeCallbacks(ProcessNode node) {
        // 节点操作回调（需要由外部通过 configureNodeCallbacks 设置）
        // 这里只设置画布内部的回调
        node.setOnDelete(() -> removeNode(node, true));
        
        // 设置节点状态变化回调，用于状态传播
        node.setOnStateChange((changedNode, oldState, newState) -> {
            handleNodeStateChange(changedNode, oldState, newState);
        });
        
        node.setOnDragged(() -> {
            if (onNodeMoved != null) onNodeMoved.run();
            // 优化：使用节流检查画布扩展和滚动
            throttledCheckAndExpandCanvas(node);
            throttledCheckAndScrollViewport(node);
            handleNodeDrag(node);
            
            // 如果节点被选中，更新选择框位置
            if (selectionManager.getSelectedNodes().contains(node)) {
                selectionManager.updateSelectionBoundingBox();
            }
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
            
            // 节点位置改变，标记有未保存的更改
            if (!isClick) {
                markAsUnsaved();
            }
            
            // 如果节点被选中，确保选择框位置正确
            if (selectionManager.getSelectedNodes().contains(node)) {
                selectionManager.updateSelectionBoundingBox();
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
        // 标记有未保存的更改
        markAsUnsaved();
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
        // 标记有未保存的更改
        markAsUnsaved();
    }
    
    public boolean removeConnectionByEdgeId(String edgeId) {
        boolean removed = connectionManager.removeConnectionByEdgeId(edgeId);
        if (removed) {
            // 标记有未保存的更改
            markAsUnsaved();
        }
        return removed;
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
            if (e.isPrimaryButtonDown()) {
                boolean isClickOnNodeOrEdge = isClickOnNodeOrEdge((javafx.scene.Node) e.getTarget());
                
                if (selectionManager.isSelectionMode() && !isClickOnNodeOrEdge) {
                    // 选择模式下，点击空白区域开始框选
                    Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                    selectionManager.startSelection(localPoint.getX(), localPoint.getY());
                    e.consume();
                } else if (!selectionManager.isSelectionMode() && !isClickOnNodeOrEdge) {
                    // 非选择模式下，点击空白区域清除选择
                    if (!selectionManager.getSelectedNodes().isEmpty()) {
                        selectionManager.clearSelection();
                    }
                }
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
    
    // ==================== 自动保存设置 ====================
    
    /**
     * 设置自动保存功能
     * 监听鼠标离开画布事件和焦点变化事件，延迟触发保存操作
     */
    private void setupAutoSave() {

        
        // 初始化延迟保存触发器
        autoSaveTransition = new PauseTransition(Duration.seconds(AUTO_SAVE_DELAY_SECONDS));
        autoSaveTransition.setOnFinished(e -> {
            
              // 双重检查：确保自动保存仍然启用且有未保存更改
              if (autoSaveEnabled && hasUnsavedChanges && onRequestSave != null) {
                  onRequestSave.run();
                  hasUnsavedChanges = false;
              }
        });
        
        // 监听鼠标离开画布事件
        this.setOnMouseExited(e -> {
            triggerAutoSave();
        });
        
        // 监听鼠标进入画布事件，取消待执行的保存
        this.setOnMouseEntered(e -> {
            cancelAutoSave();
        });
        
        // 监听画布焦点变化，当失去焦点时触发自动保存（用户点击其他控件时）
        this.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (wasFocused && !isNowFocused) {
                // 画布失去焦点，触发自动保存
                triggerAutoSave();
            }
        });
        
        // 监听鼠标按下事件，用于捕获点击其他区域的行为
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                    // 检查点击是否在画布外部
                    if (!this.contains(this.sceneToLocal(e.getSceneX(), e.getSceneY()))) {
                        // 点击在画布外部，触发自动保存
                        triggerAutoSave();
                    }
                });
            }
        });
    }
    
    /**
     * 触发自动保存
     */
    private void triggerAutoSave() {
        // 只在自动保存启用且有未保存更改时触发
        if (autoSaveEnabled && hasUnsavedChanges && onRequestSave != null) {
            // 延迟触发保存，避免频繁保存
            autoSaveTransition.playFromStart();
        }
    }
    
    /**
     * 取消待执行的自动保存
     */
    private void cancelAutoSave() {
        if (autoSaveTransition != null && autoSaveTransition.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            autoSaveTransition.stop();
        }
    }
    
    /**
     * 标记画布有未保存的更改
     * 此方法应该在节点位置变化、添加/删除节点、添加/删除连接等操作后调用
     */
    public void markAsUnsaved() {
        // 只在自动保存启用时才标记（避免在加载数据时误标记）
        if (autoSaveEnabled) {
            this.hasUnsavedChanges = true;
        }
    }
    
    /**
     * 清除未保存标记
     */
    public void markAsSaved() {
        this.hasUnsavedChanges = false;
    }
    
    /**
     * 禁用自动保存（用于数据加载等场景）
     */
    public void disableAutoSave() {
        this.autoSaveEnabled = false;
        // 停止所有待执行的自动保存
        cancelAutoSave();
    }
    
    /**
     * 启用自动保存
     */
    public void enableAutoSave() {
        this.autoSaveEnabled = true;
    }
    
    /**
     * 检查是否有未保存的更改
     */
    public boolean hasUnsavedChanges() {
        return this.hasUnsavedChanges;
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
        
        // 主题菜单
        Menu themeMenu = new Menu("主题");
        MenuItem defaultThemeItem = new MenuItem("默认");
        defaultThemeItem.setOnAction(e -> setTheme("default"));
        MenuItem gridThemeItem = new MenuItem("框框");
        gridThemeItem.setOnAction(e -> setTheme("grid"));
        MenuItem dotsThemeItem = new MenuItem("圆点");
        dotsThemeItem.setOnAction(e -> setTheme("dots"));
        themeMenu.getItems().addAll(defaultThemeItem, gridThemeItem, dotsThemeItem);
        
        // 分隔符
        SeparatorMenuItem separatorState = new SeparatorMenuItem();
        
        // 重置节点状态
        MenuItem resetNodeStatesItem = new MenuItem("重置节点状态");
        resetNodeStatesItem.setOnAction(e -> resetAllNodeStates());
        
        // 恢复所有节点运行状态（功能移除）
        MenuItem restoreAllNodesItem = new MenuItem("恢复节点运行");
        restoreAllNodesItem.setOnAction(e -> restoreAllNodesEnabled());
        
        menu.getItems().addAll(addNodeItem,
                clearItem,
                runGroupItem,
                themeMenu,
                separatorState,
                resetNodeStatesItem
                //restoreAllNodesItem
        );
        
        this.setOnContextMenuRequested(e -> {
            if (!isClickOnNodeOrEdge((javafx.scene.Node) e.getTarget())) {
                menu.show(this, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });
        
        // 修复：点击画布其他地方时隐藏菜单（排除右键点击，因为右键用于显示菜单）
        this.setOnMousePressed(e -> {
            if (menu.isShowing() && e.isPrimaryButtonDown() && !isClickOnNodeOrEdge((javafx.scene.Node) e.getTarget())) {
                menu.hide();
            }
        });
    }
    
    /**
     * 设置画布主题
     * @param theme 主题名称："default"（默认）、"grid"（框框）、"dots"（圆点）
     */
    private void setTheme(String theme) {
        if (theme == null || theme.equals(currentTheme)) {
            return;
        }
        
        currentTheme = theme;
        
        // 移除之前的背景Canvas（如果存在）
        if (dotsBackgroundCanvas != null) {
            this.getChildren().remove(dotsBackgroundCanvas);
            dotsBackgroundCanvas = null;
        }
        if (gridBackgroundCanvas != null) {
            this.getChildren().remove(gridBackgroundCanvas);
            gridBackgroundCanvas = null;
        }
        
        switch (theme) {
            case "default":
                // 默认：纯色背景
                setStyle("-fx-background-color: #F3F4F6;");
                log("✓ 已切换到默认主题");
                break;
            case "grid":
                // 框框：网格背景 - 使用Canvas绘制网格图案（更可靠）
                setStyle("-fx-background-color: #F3F4F6;");
                createGridBackground();
                log("✓ 已切换到框框主题");
                break;
            case "dots":
                // 圆点：圆点背景 - 使用Canvas绘制圆点图案
                setStyle("-fx-background-color: #F3F4F6;");
                createDotsBackground();
                log("✓ 已切换到圆点主题");
                break;
            default:
                setStyle("-fx-background-color: #F3F4F6;");
                log("⚠️ 未知主题，已切换到默认主题");
                break;
        }
    }
    
    /**
     * 创建圆点背景Canvas
     */
    private void createDotsBackground() {
        // 移除旧的Canvas（如果存在）
        if (dotsBackgroundCanvas != null) {
            this.getChildren().remove(dotsBackgroundCanvas);
        }
        
        // 创建新的Canvas，大小与画布相同
        dotsBackgroundCanvas = new Canvas(getPrefWidth(), getPrefHeight());
        dotsBackgroundCanvas.setMouseTransparent(true); // 不拦截鼠标事件
        dotsBackgroundCanvas.toBack(); // 放在最底层
        
        GraphicsContext gc = dotsBackgroundCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#D1D5DB"));
        
        // 绘制圆点网格，每个圆点间隔20px
        double spacing = 20.0;
        double dotRadius = 1.5;
        
        for (double x = spacing / 2; x < getPrefWidth(); x += spacing) {
            for (double y = spacing / 2; y < getPrefHeight(); y += spacing) {
                gc.fillOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
            }
        }
        
        // 监听画布大小变化，更新Canvas大小（只添加一次监听器）
        if (!dotsBackgroundListenersAdded) {
            widthProperty().addListener((obs, oldVal, newVal) -> {
                if (dotsBackgroundCanvas != null && newVal.doubleValue() > 0) {
                    dotsBackgroundCanvas.setWidth(newVal.doubleValue());
                    redrawDotsBackground();
                }
            });
            
            heightProperty().addListener((obs, oldVal, newVal) -> {
                if (dotsBackgroundCanvas != null && newVal.doubleValue() > 0) {
                    dotsBackgroundCanvas.setHeight(newVal.doubleValue());
                    redrawDotsBackground();
                }
            });
            dotsBackgroundListenersAdded = true;
        }
        
        // 将Canvas添加到画布最底层
        this.getChildren().add(0, dotsBackgroundCanvas);
    }
    
    /**
     * 重新绘制圆点背景
     */
    private void redrawDotsBackground() {
        if (dotsBackgroundCanvas == null) return;
        
        GraphicsContext gc = dotsBackgroundCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, dotsBackgroundCanvas.getWidth(), dotsBackgroundCanvas.getHeight());
        gc.setFill(Color.web("#D1D5DB"));
        
        double spacing = 20.0;
        double dotRadius = 1.5;
        
        for (double x = spacing / 2; x < dotsBackgroundCanvas.getWidth(); x += spacing) {
            for (double y = spacing / 2; y < dotsBackgroundCanvas.getHeight(); y += spacing) {
                gc.fillOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
            }
        }
    }
    
    /**
     * 创建网格背景Canvas
     */
    private void createGridBackground() {
        // 移除旧的Canvas（如果存在）
        if (gridBackgroundCanvas != null) {
            this.getChildren().remove(gridBackgroundCanvas);
        }
        
        // 创建新的Canvas，大小与画布相同
        gridBackgroundCanvas = new Canvas(getPrefWidth(), getPrefHeight());
        gridBackgroundCanvas.setMouseTransparent(true); // 不拦截鼠标事件
        gridBackgroundCanvas.toBack(); // 放在最底层
        
        GraphicsContext gc = gridBackgroundCanvas.getGraphicsContext2D();
        gc.setStroke(Color.web("#D1D5DB"));
        gc.setLineWidth(1.0);
        
        // 绘制网格线，每个网格20px
        double spacing = 20.0;
        
        // 绘制垂直线
        for (double x = 0; x < getPrefWidth(); x += spacing) {
            gc.strokeLine(x, 0, x, getPrefHeight());
        }
        
        // 绘制水平线
        for (double y = 0; y < getPrefHeight(); y += spacing) {
            gc.strokeLine(0, y, getPrefWidth(), y);
        }
        
        // 监听画布大小变化，更新Canvas大小（只添加一次监听器）
        if (!gridBackgroundListenersAdded) {
            widthProperty().addListener((obs, oldVal, newVal) -> {
                if (gridBackgroundCanvas != null && newVal.doubleValue() > 0) {
                    gridBackgroundCanvas.setWidth(newVal.doubleValue());
                    redrawGridBackground();
                }
            });
            
            heightProperty().addListener((obs, oldVal, newVal) -> {
                if (gridBackgroundCanvas != null && newVal.doubleValue() > 0) {
                    gridBackgroundCanvas.setHeight(newVal.doubleValue());
                    redrawGridBackground();
                }
            });
            gridBackgroundListenersAdded = true;
        }
        
        // 将Canvas添加到画布最底层
        this.getChildren().add(0, gridBackgroundCanvas);
    }
    
    /**
     * 重新绘制网格背景
     */
    private void redrawGridBackground() {
        if (gridBackgroundCanvas == null) return;
        
        GraphicsContext gc = gridBackgroundCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gridBackgroundCanvas.getWidth(), gridBackgroundCanvas.getHeight());
        gc.setStroke(Color.web("#D1D5DB"));
        gc.setLineWidth(1.0);
        
        double spacing = 20.0;
        
        // 绘制垂直线
        for (double x = 0; x < gridBackgroundCanvas.getWidth(); x += spacing) {
            gc.strokeLine(x, 0, x, gridBackgroundCanvas.getHeight());
        }
        
        // 绘制水平线
        for (double y = 0; y < gridBackgroundCanvas.getHeight(); y += spacing) {
            gc.strokeLine(0, y, gridBackgroundCanvas.getWidth(), y);
        }
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
                
                // 从全局管理器恢复节点状态（开始/终止/阻塞）
                if (node.getJobId() != null) {
                    com.cc.job.gui.util.NodeGraphStateManager stateManager = 
                        com.cc.job.gui.util.NodeGraphStateManager.getInstance();
                    
                    // 恢复图节点状态（开始/终止/阻塞）
                    ProcessNode.GraphNodeState savedState = stateManager.getNodeState(node.getJobId());
                    if (savedState != ProcessNode.GraphNodeState.NORMAL) {
                        // 使用内部方法设置状态，不触发回调（避免在加载时触发状态传播）
                        node.setGraphStateInternal(savedState);
                    }
                    
                    // 恢复启用/禁用状态
                    boolean savedEnabled = stateManager.getNodeEnabled(node.getJobId());
                    if (!savedEnabled) {
                        // 如果节点是禁用状态，恢复禁用状态
                        node.restoreEnabledState(false);
                    }
                }
            });
            
            // 加载连接后，需要重新应用状态传播逻辑（因为状态可能影响其他节点）
            // 延迟执行状态传播，确保所有节点和连接都已加载完成
            javafx.application.Platform.runLater(() -> {
                applyStatePropagationAfterLoad();
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
        
        // 清空后清除未保存标记
        hasUnsavedChanges = false;
        
        log("✓ 画布已清空");
    }
    
    public void clearViewOnly() {
        new ArrayList<>(connections).forEach(connectionManager::removeConnection);
        nodes.forEach(this.getChildren()::remove);
        nodes.clear();
        groupContainers.forEach(this.getChildren()::remove);
        groupContainers.clear();
        selectionManager.clearSelection();
        
        // 清空后清除未保存标记
        hasUnsavedChanges = false;
        
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
    
    /**
     * 定位节点到视图中心并播放定位动画
     * @param node 要定位的节点
     * @return 是否成功定位
     */
    public boolean locateNode(ProcessNode node) {
        if (node == null || hostingScrollPane == null) return false;
        
        // 播放定位动画
        node.playLocateAnimation();
        
        // 滚动到节点位置（居中显示）
        scrollToNode(node);
        
        return true;
    }
    
    /**
     * 定位节点到视图中心
     * @param node 要定位的节点
     */
    public void scrollToNode(ProcessNode node) {
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
        
        double canvasWidth = getPrefWidth();
        double canvasHeight = getPrefHeight();
        
        // 计算滚动值，使节点居中
        double scrollableWidth = canvasWidth - viewportWidth;
        double scrollableHeight = canvasHeight - viewportHeight;
        
        if (scrollableWidth > 0) {
            double targetHValue = (nodeCenterX - viewportWidth / 2) / scrollableWidth;
            targetHValue = Math.max(0, Math.min(1, targetHValue));
            hostingScrollPane.setHvalue(targetHValue);
        }
        
        if (scrollableHeight > 0) {
            double targetVValue = (nodeCenterY - viewportHeight / 2) / scrollableHeight;
            targetVValue = Math.max(0, Math.min(1, targetVValue));
            hostingScrollPane.setVvalue(targetVValue);
        }
    }
    
    /**
     * 定位连接线
     * @param edgeId 连接线ID
     * @return 是否成功定位
     */
    public boolean locateConnectionByEdgeId(String edgeId) {
        if (edgeId == null) return false;
        
        for (NodeConnection connection : connections) {
            if (normalizeEdgeId(edgeId).equals(normalizeEdgeId(connection.getEdgeId()))) {
                connection.toFront();
                connection.setSelected(true);
                connection.playLocateAnimation();
                
                // 定位到连接线的中心位置
                ProcessNode sourceNode = connection.getSourceNode();
                ProcessNode targetNode = connection.getTargetNode();
                if (sourceNode != null && targetNode != null && hostingScrollPane != null) {
                    double centerX = (sourceNode.getLayoutX() + targetNode.getLayoutX()) / 2;
                    double centerY = (sourceNode.getLayoutY() + targetNode.getLayoutY()) / 2;
                    
                    // 直接计算滚动位置
                    double viewportWidth = hostingScrollPane.getViewportBounds().getWidth();
                    double viewportHeight = hostingScrollPane.getViewportBounds().getHeight();
                    double canvasWidth = getPrefWidth();
                    double canvasHeight = getPrefHeight();
                    
                    double scrollableWidth = canvasWidth - viewportWidth;
                    double scrollableHeight = canvasHeight - viewportHeight;
                    
                    if (scrollableWidth > 0) {
                        double targetHValue = (centerX - viewportWidth / 2) / scrollableWidth;
                        targetHValue = Math.max(0, Math.min(1, targetHValue));
                        hostingScrollPane.setHvalue(targetHValue);
                    }
                    
                    if (scrollableHeight > 0) {
                        double targetVValue = (centerY - viewportHeight / 2) / scrollableHeight;
                        targetVValue = Math.max(0, Math.min(1, targetVValue));
                        hostingScrollPane.setVvalue(targetVValue);
                    }
                }
                
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
    
    // ==================== 节点状态管理 ====================
    
    /**
     * 重置所有节点的图状态为普通状态
     * 清除所有开始节点、终止节点和阻塞节点状态
     */
    public void resetAllNodeStates() {
        int resetCount = 0;
        
        // 第一步：先清除所有开始节点和终止节点，并恢复它们影响的阻塞节点
        List<ProcessNode> startAndStopNodes = new ArrayList<>();
        for (ProcessNode node : new ArrayList<>(nodes)) {
            ProcessNode.GraphNodeState currentState = node.getGraphState();
            if (currentState == ProcessNode.GraphNodeState.START || 
                currentState == ProcessNode.GraphNodeState.STOP) {
                startAndStopNodes.add(node);
            }
        }
        
        // 清除开始节点和终止节点，这会自动恢复它们影响的阻塞节点
        for (ProcessNode node : startAndStopNodes) {
            node.setGraphState(ProcessNode.GraphNodeState.NORMAL);
            resetCount++;
        }
        
        // 第二步：清除所有剩余的阻塞节点
        for (ProcessNode node : new ArrayList<>(nodes)) {
            if (node.getGraphState() == ProcessNode.GraphNodeState.BLOCKED) {
                node.setGraphState(ProcessNode.GraphNodeState.NORMAL);
                resetCount++;
            }
        }
        
        if (resetCount > 0) {
            log("✓ 已重置 " + resetCount + " 个节点的状态为普通状态");
            // 重置节点状态后，更新所有连接线的样式
            updateAllConnectionBlockedStates();
            markAsUnsaved();
        } else {
            log("ℹ 没有需要重置的节点状态");
        }
    }
    
    /**
     * 恢复所有节点的运行状态（启用所有节点）
     * 清除所有暂停节点状态
     */
    public void restoreAllNodesEnabled() {
        int restoredCount = 0;
        
        // 遍历所有节点，恢复为启用状态
        for (ProcessNode node : new ArrayList<>(nodes)) {
            if (!node.getEnabled()) {
                // 恢复节点为启用状态
                node.restoreEnabledState(true);
                restoredCount++;
            }
        }
        
        if (restoredCount > 0) {
            log("✓ 已恢复 " + restoredCount + " 个节点的运行状态");
            markAsUnsaved();
        } else {
            log("ℹ 没有需要恢复的暂停节点");
        }
    }
    
    /**
     * 在数据加载完成后应用状态传播逻辑
     * 遍历所有节点，对开始节点和终止节点应用状态传播
     */
    private void applyStatePropagationAfterLoad() {
        // 遍历所有节点，对开始节点和终止节点应用状态传播
        for (ProcessNode node : nodes) {
            ProcessNode.GraphNodeState state = node.getGraphState();
            if (state == ProcessNode.GraphNodeState.START || state == ProcessNode.GraphNodeState.STOP) {
                // 触发状态传播逻辑（使用内部方法，避免重复保存到全局管理器）
                handleNodeStateChange(node, ProcessNode.GraphNodeState.NORMAL, state);
            }
        }
        
        // 加载完成后，统一更新所有连接线的阻塞状态
        updateAllConnectionBlockedStates();
    }
    
    /**
     * 更新所有连接线的阻塞状态
     * 根据连接两端节点的状态，统一更新所有连接线的样式
     */
    private void updateAllConnectionBlockedStates() {
        for (NodeConnection conn : connections) {
            updateConnectionBlockedState(conn);
        }
    }
    
    /**
     * 处理节点状态变化，实现状态传播逻辑
     * 开始节点影响所有前驱节点（使其变为阻塞状态）
     * 终止节点影响所有后继节点（使其变为阻塞状态）
     */
    private void handleNodeStateChange(ProcessNode changedNode, ProcessNode.GraphNodeState oldState, ProcessNode.GraphNodeState newState) {
        
        // 先恢复旧状态的影响（重新计算所有受影响节点的阻塞状态）
        if (oldState == ProcessNode.GraphNodeState.START) {
            // 恢复所有前驱节点的阻塞状态（BFS遍历所有前驱）
            Set<ProcessNode> allPredecessors = getAllPredecessors(changedNode);
            for (ProcessNode pred : allPredecessors) {
                updateBlockedState(pred);
            }
            // 开始节点的边不变成虚线，更新所有连接线的阻塞状态
            updateAllConnectionBlockedStates();
        } else if (oldState == ProcessNode.GraphNodeState.STOP) {
            // 恢复所有后继节点的阻塞状态（BFS遍历所有后继）
            Set<ProcessNode> allSuccessors = getAllSuccessors(changedNode);
            for (ProcessNode succ : allSuccessors) {
                updateBlockedState(succ);
            }
            // 终止节点的边不变成虚线，更新所有连接线的阻塞状态
            updateAllConnectionBlockedStates();
        } else if (oldState == ProcessNode.GraphNodeState.BLOCKED) {
            // 恢复阻塞节点的连接线样式
            updateConnectionStylesForBlockedNode(changedNode, false);
        }
        
        // 应用新状态的影响
        if (newState == ProcessNode.GraphNodeState.START) {
            // 开始节点：影响所有前驱节点（BFS遍历所有前驱）
            Set<ProcessNode> allPredecessors = getAllPredecessors(changedNode);
            for (ProcessNode pred : allPredecessors) {
                // 如果前驱节点不是开始节点或终止节点，则设置为阻塞
                if (pred.getGraphState() == ProcessNode.GraphNodeState.NORMAL || 
                    pred.getGraphState() == ProcessNode.GraphNodeState.BLOCKED) {
                    pred.setGraphState(ProcessNode.GraphNodeState.BLOCKED);
                }
            }
            // 开始节点的边不变成虚线，只有阻塞节点的边才变成虚线
            // 更新所有连接线的阻塞状态（因为可能有节点变成了阻塞状态）
            updateAllConnectionBlockedStates();
        } else if (newState == ProcessNode.GraphNodeState.STOP) {
            // 终止节点：影响所有后继节点（BFS遍历所有后继）
            Set<ProcessNode> allSuccessors = getAllSuccessors(changedNode);
            for (ProcessNode succ : allSuccessors) {
                // 如果后继节点不是开始节点或终止节点，则设置为阻塞
                if (succ.getGraphState() == ProcessNode.GraphNodeState.NORMAL || 
                    succ.getGraphState() == ProcessNode.GraphNodeState.BLOCKED) {
                    succ.setGraphState(ProcessNode.GraphNodeState.BLOCKED);
                }
            }
            // 终止节点的边不变成虚线，只有阻塞节点的边才变成虚线
            // 更新所有连接线的阻塞状态（因为可能有节点变成了阻塞状态）
            updateAllConnectionBlockedStates();
        } else if (newState == ProcessNode.GraphNodeState.BLOCKED) {
            // 手动设置为阻塞节点：如果之前是开始节点或终止节点，已经恢复了它们的影响
            // 阻塞节点本身不会影响其他节点，所以不需要额外的状态传播
            // 状态传播逻辑已经在上面处理了（恢复旧状态的影响）
            // 更新连接线样式：只有阻塞节点的所有入边和出边变为虚线
            updateConnectionStylesForBlockedNode(changedNode, true);
        } else if (newState == ProcessNode.GraphNodeState.NORMAL) {
            // 恢复为普通节点：检查是否仍应保持阻塞状态
            updateBlockedState(changedNode);
            // 恢复连接线样式：与节点相关的连接线恢复为实线（如果不再被阻塞）
            updateConnectionStylesForNode(changedNode, null, false);
        }
        
        // 标记有未保存的更改
        markAsUnsaved();
    }
    
    /**
     * 更新节点相关的连接线样式
     * @param node 节点
     * @param affectedNodes 受影响的节点集合（如果为null，则更新所有相关连接线）
     * @param blocked 是否设置为阻塞（虚线）- 如果为false，则重新计算连接线的阻塞状态
     */
    private void updateConnectionStylesForNode(ProcessNode node, Set<ProcessNode> affectedNodes, boolean blocked) {
        for (NodeConnection conn : connections) {
            ProcessNode sourceNode = conn.getSourceNode();
            ProcessNode targetNode = conn.getTargetNode();
            
            if (sourceNode == null || targetNode == null) {
                continue;
            }
            
            // 检查连接是否与节点相关
            boolean isRelated = false;
            if (affectedNodes != null) {
                // 如果指定了受影响的节点集合，只更新与这些节点之间的连接
                if (sourceNode == node && affectedNodes.contains(targetNode)) {
                    isRelated = true;
                } else if (targetNode == node && affectedNodes.contains(sourceNode)) {
                    isRelated = true;
                }
            } else {
                // 如果没有指定，更新所有与节点相关的连接
                if (sourceNode == node || targetNode == node) {
                    isRelated = true;
                }
            }
            
            if (isRelated) {
                if (blocked) {
                    // 设置为阻塞（虚线）
                    conn.setBlocked(true);
                } else {
                    // 重新计算连接线的阻塞状态
                    updateConnectionBlockedState(conn);
                }
            }
        }
    }
    
    /**
     * 更新阻塞节点相关的连接线样式
     * @param blockedNode 阻塞节点
     * @param blocked 是否设置为阻塞（虚线）- 如果为false，则重新计算连接线的阻塞状态
     */
    private void updateConnectionStylesForBlockedNode(ProcessNode blockedNode, boolean blocked) {
        for (NodeConnection conn : connections) {
            ProcessNode sourceNode = conn.getSourceNode();
            ProcessNode targetNode = conn.getTargetNode();
            
            if (sourceNode == null || targetNode == null) {
                continue;
            }
            
            // 检查连接是否与阻塞节点相关
            if (sourceNode == blockedNode || targetNode == blockedNode) {
                if (blocked) {
                    // 设置为阻塞（虚线）
                    conn.setBlocked(true);
                } else {
                    // 重新计算连接线的阻塞状态
                    updateConnectionBlockedState(conn);
                }
            }
        }
    }
    
    /**
     * 更新连接线的阻塞状态（根据连接两端节点的状态）
     * 只有阻塞节点的所有入边和出边会变成虚线
     * @param conn 连接线
     */
    private void updateConnectionBlockedState(NodeConnection conn) {
        ProcessNode sourceNode = conn.getSourceNode();
        ProcessNode targetNode = conn.getTargetNode();
        
        if (sourceNode == null || targetNode == null) {
            return;
        }
        
        // 只有连接的一端或两端是阻塞节点时，才设置为虚线
        // 开始节点和终止节点的边不变成虚线
        boolean sourceBlocked = sourceNode.getGraphState() == ProcessNode.GraphNodeState.BLOCKED;
        boolean targetBlocked = targetNode.getGraphState() == ProcessNode.GraphNodeState.BLOCKED;
        
        // 如果连接的一端或两端是阻塞节点，设置为虚线
        conn.setBlocked(sourceBlocked || targetBlocked);
    }
    
    /**
     * 更新节点的阻塞状态
     * 检查节点是否应该被阻塞（是否有开始节点作为后继，或有终止节点作为前驱）
     * 使用BFS遍历检查所有间接的前驱和后继
     */
    private void updateBlockedState(ProcessNode node) {
        // 如果节点是开始节点或终止节点，不更新
        if (node.getGraphState() == ProcessNode.GraphNodeState.START || 
            node.getGraphState() == ProcessNode.GraphNodeState.STOP) {
            return;
        }
        
        // 检查是否有开始节点作为后继（BFS遍历所有后继）
        Set<ProcessNode> allSuccessors = getAllSuccessors(node);
        boolean hasStartSuccessor = false;
        for (ProcessNode succ : allSuccessors) {
            if (succ.getGraphState() == ProcessNode.GraphNodeState.START) {
                hasStartSuccessor = true;
                break;
            }
        }
        
        // 检查是否有终止节点作为前驱（BFS遍历所有前驱）
        Set<ProcessNode> allPredecessors = getAllPredecessors(node);
        boolean hasStopPredecessor = false;
        for (ProcessNode pred : allPredecessors) {
            if (pred.getGraphState() == ProcessNode.GraphNodeState.STOP) {
                hasStopPredecessor = true;
                break;
            }
        }
        
        // 如果应该被阻塞，设置为阻塞状态；否则恢复为普通状态
        if (hasStartSuccessor || hasStopPredecessor) {
            if (node.getGraphState() != ProcessNode.GraphNodeState.BLOCKED) {
                // 使用内部方法设置状态，不触发回调，避免循环调用
                node.setGraphStateInternal(ProcessNode.GraphNodeState.BLOCKED);
                // 更新连接线样式
                updateConnectionStylesForBlockedNode(node, true);
            }
        } else {
            if (node.getGraphState() == ProcessNode.GraphNodeState.BLOCKED) {
                // 使用内部方法设置状态，不触发回调，避免循环调用
                node.setGraphStateInternal(ProcessNode.GraphNodeState.NORMAL);
                // 更新连接线样式（重新计算）
                updateConnectionStylesForBlockedNode(node, false);
            }
        }
    }
    
    /**
     * 获取节点的所有前驱节点（通过连接线，BFS遍历）
     */
    private Set<ProcessNode> getAllPredecessors(ProcessNode node) {
        Set<ProcessNode> result = new HashSet<>();
        Queue<ProcessNode> queue = new LinkedList<>();
        Set<ProcessNode> visited = new HashSet<>();
        
        queue.add(node);
        visited.add(node);
        
        while (!queue.isEmpty()) {
            ProcessNode current = queue.poll();
            
            // 获取当前节点的直接前驱
            for (NodeConnection conn : connections) {
                if (conn.getTargetNode() == current && conn.getSourceNode() != null) {
                    ProcessNode pred = conn.getSourceNode();
                    if (visited.add(pred)) {
                        result.add(pred);
                        queue.add(pred);
                    }
                }
            }
        }
        
        // 移除起始节点本身
        result.remove(node);
        return result;
    }
    
    /**
     * 获取节点的所有后继节点（通过连接线，BFS遍历）
     */
    private Set<ProcessNode> getAllSuccessors(ProcessNode node) {
        Set<ProcessNode> result = new HashSet<>();
        Queue<ProcessNode> queue = new LinkedList<>();
        Set<ProcessNode> visited = new HashSet<>();
        
        queue.add(node);
        visited.add(node);
        
        while (!queue.isEmpty()) {
            ProcessNode current = queue.poll();
            
            // 获取当前节点的直接后继
            for (NodeConnection conn : connections) {
                if (conn.getSourceNode() == current && conn.getTargetNode() != null) {
                    ProcessNode succ = conn.getTargetNode();
                    if (visited.add(succ)) {
                        result.add(succ);
                        queue.add(succ);
                    }
                }
            }
        }
        
        // 移除起始节点本身
        result.remove(node);
        return result;
    }
    // ==================== 辅助方法 ====================
    
    private void notifyNodeStructureChanged() { 
        if (onNodeMoved != null) onNodeMoved.run();
        // 结构变化时标记为未保存
        markAsUnsaved();
    }
    
    private void log(String message) { 
        if (logCallback != null) {
            logCallback.log(message);
        }
    }
    
    private void pushAction(CanvasAction action) { if (undoRedoManager != null && historyEnabled && action != null) undoRedoManager.push(action); }
    
    private void runWithoutHistory(Runnable runnable) {
        boolean previousHistory = historyEnabled;
        historyEnabled = false;
        // 注意：不在这里管理 autoSaveEnabled，因为它由外部 disableAutoSave/enableAutoSave 控制
        // 避免与外部控制冲突导致自动保存失效
        try { 
            runnable.run(); 
        } finally { 
            historyEnabled = previousHistory;
        }
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


