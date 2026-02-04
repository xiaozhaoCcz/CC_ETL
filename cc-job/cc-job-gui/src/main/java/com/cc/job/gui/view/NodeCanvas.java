package com.cc.job.gui.view;

import com.cc.job.gui.history.CanvasAction;
import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.manager.*;
import com.cc.job.gui.model.ConditionNode;
import com.cc.job.gui.model.GroupContainer;
import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.NodeGraphStateManager;
import com.cc.job.gui.util.NodeStatusSyncManager;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Node;
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
    private List<ConditionNode> conditionNodes = new ArrayList<>();
    private List<NodeConnection> connections = new ArrayList<>();
    
    // 管理器
    private CanvasNodeManager nodeManager;
    private CanvasConnectionManager connectionManager;
    private CanvasSelectionManager selectionManager;
    private CanvasDataLoader dataLoader;
    private CanvasLayoutManager layoutManager;
    
    private UndoRedoManager undoRedoManager;
    private boolean historyEnabled = true;
    private boolean autoSaveEnabled = true; // 控制自动保存标记是否启用
    
    // 回调
    private Runnable onRequestAddNode;
    private Runnable onRequestAddConditionNode; // 创建条件节点回调
    private Runnable onRequestRunTaskGroup;
    private Runnable onRequestClearCanvas;
    private java.util.function.Consumer<GroupContainer> onDeleteGroupContainer;
    private java.util.function.Consumer<ConditionNode> onDeleteConditionNode;
    private java.util.function.Consumer<ConditionNode> onEditConditionNode; // 编辑条件节点回调
    private LogCallback logCallback;
    private Runnable onNodeMoved;
    private java.util.function.Consumer<Boolean> onSelectionModeChanged;
    private java.util.function.Consumer<Boolean> onSnapToGridChanged;
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
    
    // 拖拽选择状态管理
    private Map<ProcessNode, Boolean> nodeSelectedBeforeDrag = new HashMap<>(); // 记录拖拽前节点是否被选中
    
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
    private boolean gridVisible = false; // 网格是否可见（独立于主题）
    private boolean nodeLabelsVisible = false; // 节点标签是否可见
    private boolean edgeLabelsVisible = false; // 连线标签是否可见
    private boolean rulerVisible = true; // 标尺是否可见（默认显示）
    private CanvasRuler ruler; // 标尺组件
    private Label coordinateLabel; // 坐标显示标签
    
    // 智能对齐相关
    private boolean smartAlignmentEnabled = true; // 智能对齐是否启用
    private boolean snapToGridEnabled = false; // 网格吸附是否启用
    private double gridSnapSize = 20.0; // 网格吸附大小
    private List<Line> alignmentGuideLines = new ArrayList<>(); // 对齐参考线
    private static final double ALIGNMENT_THRESHOLD = 5.0; // 对齐阈值（像素）
    
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
        setupCoordinateDisplay();
        
        // 初始化标尺（默认显示）
        initializeRuler();
    }
    
    private void initializeManagers() {
        nodeManager = new CanvasNodeManager(this, nodes, this::notifyNodeStructureChanged, this::log);
        connectionManager = new CanvasConnectionManager(this, connections, groupContainers, conditionNodes, this::notifyNodeStructureChanged, this::log);
        connectionManager.setOnRequestRemoveConnection(conn -> removeConnection(conn, true));
        selectionManager = new CanvasSelectionManager(this, nodes, connections, this::log, this::notifyNodeStructureChanged);
        dataLoader = new CanvasDataLoader(this::log);
        layoutManager = new CanvasLayoutManager(this::log);
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
    
    public void setOnSnapToGridChanged(java.util.function.Consumer<Boolean> callback) {
        this.onSnapToGridChanged = callback;
    }
    
    public void setScrollPane(ScrollPane scrollPane) {
        this.hostingScrollPane = scrollPane;
    }
    
    /**
     * 设置主窗口Stage（用于打开对话框）
     */
    public void setOwnerStage(javafx.stage.Stage ownerStage) {
        if (connectionManager != null) {
            connectionManager.setOwnerStage(ownerStage);
        }
    }
    
    public void setOnRequestAddNode(Runnable runnable) {
        this.onRequestAddNode = runnable;
    }
    
    public void setOnRequestAddConditionNode(Runnable runnable) {
        this.onRequestAddConditionNode = runnable;
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
    
    public void setOnDeleteConditionNode(java.util.function.Consumer<ConditionNode> callback) {
        this.onDeleteConditionNode = callback;
    }
    
    public void setOnEditConditionNode(java.util.function.Consumer<ConditionNode> callback) {
        this.onEditConditionNode = callback;
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
    
    public List<ConditionNode> getConditionNodes() {
        return new ArrayList<>(conditionNodes);
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
            
            // 更新坐标显示（节点移动时）
            if (coordinateLabel != null) {
                double nodeX = node.getLayoutX();
                double nodeY = node.getLayoutY();
                coordinateLabel.setText(String.format("坐标: (%.0f, %.0f)", nodeX, nodeY));
                coordinateLabel.setLayoutX(nodeX + 10);
                coordinateLabel.setLayoutY(nodeY - 30);
                coordinateLabel.setVisible(true);
            }
        });
        
        node.setOnDragStarted(() -> {
            isDragging = true; // 标记开始拖拽
            currentDragNode = node; // 记录当前拖拽节点
            // 记录拖拽前节点是否被选中
            boolean wasSelected = selectionManager.getSelectedNodes().contains(node);
            nodeSelectedBeforeDrag.put(node, wasSelected);
            
            // 开始拖拽时显示节点坐标
            if (coordinateLabel != null) {
                double nodeX = node.getLayoutX();
                double nodeY = node.getLayoutY();
                coordinateLabel.setText(String.format("坐标: (%.0f, %.0f)", nodeX, nodeY));
                coordinateLabel.setLayoutX(nodeX + 10);
                coordinateLabel.setLayoutY(nodeY - 30);
                coordinateLabel.setVisible(true);
            }
            
            startDragScrollAnimation(); // 启动平滑滚动动画
            if (!selectionManager.getSelectedNodes().isEmpty() && 
                selectionManager.getSelectedNodes().contains(node) && 
                selectionManager.getSelectedNodes().size() > 1) {
                selectionManager.setMovingSelection(true);
                selectionManager.setDragStartNode(node);
            }
        });
        
        node.setOnClicked(() -> {
            // 只有在没有发生拖拽的情况下才选中节点
            // 如果节点正在被拖拽或刚刚完成拖拽，不选中节点
            if (!selectionManager.isMovingSelection() && !isDragging) {
                selectionManager.selectNode(node);
            }
        });
        
        node.setOnDragFinished((oldX, oldY, newX, newY) -> {
            isDragging = false; // 标记拖拽结束
            currentDragNode = null; // 清除当前拖拽节点
            stopDragScrollAnimation(); // 停止滚动动画
            // 隐藏对齐参考线
            hideAlignmentGuides();
            
            // 拖拽结束后，如果鼠标不在画布上，隐藏坐标标签
            // 否则继续显示鼠标坐标
            
            // 检查拖拽前节点是否被选中
            boolean wasSelectedBeforeDrag = nodeSelectedBeforeDrag.getOrDefault(node, false);
            nodeSelectedBeforeDrag.remove(node); // 清除记录
            
            currentDragNode = null;
            
            // 拖拽结束后，检查是否需要左侧或上侧扩展
            checkAndExpandCanvas(node);
            
            // ⭐ 修复：如果节点已经在条件节点容器中，不要调用checkNodeInConditionContainer
            // 因为节点在contentLayer中时，坐标是相对坐标，checkNodeInConditionContainer可能会误判
            ConditionNode existingContainer = findConditionNodeContaining(node, conditionNodes);
            if (existingContainer == null) {
                // 只有节点不在任何容器中时，才检查是否进入容器
                checkNodeInConditionContainer(node);
            }
            
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
            
            // 只有在拖拽前节点就已经被选中的情况下，才更新选择框
            // 如果节点在拖拽前没有被选中，拖拽后也不应该显示选择框
            if (wasSelectedBeforeDrag) {
                // 确保节点仍在选中列表中（可能被其他操作清除）
                if (selectionManager.getSelectedNodes().contains(node)) {
                    selectionManager.updateSelectionBoundingBox();
                }
            } else {
                // 如果节点在拖拽前没有被选中，拖拽后应该清除选择状态
                // 这样可以避免移动节点后意外显示红色框
                if (selectionManager.getSelectedNodes().contains(node)) {
                    // 如果节点被选中了（可能是拖拽过程中触发的），清除选择
                    selectionManager.clearSelection();
                }
            }
            
            notifyNodeStructureChanged();
        });
        
        // 设置连接点处理器
        setupConnectorHandler(node, node.getTopConnector());
        setupConnectorHandler(node, node.getBottomConnector());
        setupConnectorHandler(node, node.getLeftConnector());
        setupConnectorHandler(node, node.getRightConnector());
        
        // 设置移出容器回调
        node.setOnRemoveFromContainer(() -> removeNodeFromContainer(node));
        node.setIsInContainerChecker(() -> isNodeInAnyContainer(node));
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
        
        // 智能对齐：显示对齐参考线
        if (smartAlignmentEnabled && !selectionManager.isMovingSelection()) {
            showAlignmentGuides(node);
        }
        
        // 网格吸附
        if (snapToGridEnabled) {
            snapNodeToGrid(node);
        }
        
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
    
    /**
     * 粘贴完成后由 NodeOperationManager 调用，将本次粘贴作为一步撤销入栈（方案 B）。
     * @param newNodes 本次粘贴添加的节点（非 null）
     * @param newConnections 本次粘贴添加的连接（非 null）
     */
    public void notifyPasteCompleted(List<ProcessNode> newNodes, List<NodeConnection> newConnections) {
        if (newNodes == null || newNodes.isEmpty()) return;
        pushAction(new PasteAction(newNodes, newConnections != null ? newConnections : Collections.emptyList()));
    }
    
    /**
     * 批量删除节点（剪切/Delete/批量编辑删除），作为一步撤销入栈。
     * 内部先收集节点位置及关联连接，再以 recordHistory=false 逐个 removeNode，最后 push 一个 BatchRemoveAction。
     */
    public void removeNodesAsBatch(Set<ProcessNode> nodesToRemove) {
        if (nodesToRemove == null || nodesToRemove.isEmpty()) return;
        List<ProcessNode> list = new ArrayList<>(nodesToRemove);
        double[] xPos = new double[list.size()];
        double[] yPos = new double[list.size()];
        List<NodeConnection> allConnections = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ProcessNode node = list.get(i);
            xPos[i] = node.getLayoutX();
            yPos[i] = node.getLayoutY();
        }
        for (ProcessNode node : list) {
            for (NodeConnection conn : new ArrayList<>(connections)) {
                if (conn.getSourceOwner() == node || conn.getTargetOwner() == node) {
                    allConnections.add(conn);
                }
            }
        }
        for (ProcessNode node : list) {
            removeNode(node, false);
        }
        pushAction(new BatchRemoveAction(list, xPos, yPos, allConnections));
        markAsUnsaved();
    }
    
    public boolean removeConnectionByEdgeId(String edgeId) {
        NodeConnection connection = connectionManager.getConnectionByEdgeId(edgeId);
        if (connection != null) {
            removeConnection(connection, true);
            return true;
        }
        return false;
    }
    
    public void setAllConnectionsRunning(boolean running) {
        connectionManager.setAllConnectionsRunning(running);
    }
    
    /**
     * 检测并高亮循环依赖
     * @return 是否存在循环依赖
     */
    public boolean detectAndHighlightCycles() {
        // 清除之前的循环标记
        clearCycleHighlight();
        
        // 执行循环检测
        CycleDetectionManager.CycleDetectionResult result = 
            CycleDetectionManager.detectCycles(connections);
        
        // 标记所有参与循环的连接线
        for (NodeConnection conn : result.getCycleConnections()) {
            conn.setInCycle(true);
        }
        
        return result.hasCycle();
    }
    
    /**
     * 获取循环依赖检测结果（包含详细信息）
     * @return 检测结果
     */
    public CycleDetectionManager.CycleDetectionResult getCycleDetectionResult() {
        return CycleDetectionManager.detectCycles(connections);
    }
    
    /**
     * 清除所有连接线的循环依赖标记
     */
    public void clearCycleHighlight() {
        for (NodeConnection conn : connections) {
            conn.setInCycle(false);
        }
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
    
    public void selectAllNodes() {
        selectionManager.selectNodes(new HashSet<>(nodes));
    }
    
    public void invertSelection() {
        Set<ProcessNode> currentSelected = new HashSet<>(selectionManager.getSelectedNodes());
        Set<ProcessNode> allNodes = new HashSet<>(nodes);
        Set<ProcessNode> inverted = new HashSet<>();
        for (ProcessNode node : allNodes) {
            if (!currentSelected.contains(node)) {
                inverted.add(node);
            }
        }
        selectionManager.selectNodes(inverted);
    }
    
    public void selectByType(String type) {
        Set<ProcessNode> selected = new HashSet<>();
        if ("TASK".equals(type)) {
            // 选择所有任务节点（ProcessNode类型）
            // 注意：条件节点(ConditionNode)不在nodes列表中，所以直接选择所有nodes即可
            selected.addAll(nodes);
        } else if ("CONDITION".equals(type)) {
            // 选择所有条件节点 - 条件节点不是ProcessNode类型，需要特殊处理
            // ConditionNode和ProcessNode是不同的类型，无法直接添加到selected集合
            // 暂时跳过，因为条件节点选择需要单独实现
            log("条件节点选择功能开发中...");
            return;
        } else if ("EDGE".equals(type)) {
            // 选择所有连线（这个需要特殊处理）
            // 暂时只选择节点，连线的选择需要单独实现
            log("连线选择功能开发中...");
            return;
        }
        selectionManager.selectNodes(selected);
    }
    
    public void selectUpstreamNodes() {
        Set<ProcessNode> selected = selectionManager.getSelectedNodes();
        if (selected.isEmpty()) {
            log("⚠ 请先选中一个节点");
            return;
        }
        Set<ProcessNode> upstream = new HashSet<>();
        for (ProcessNode node : selected) {
            findUpstreamNodes(node, upstream);
        }
        selectionManager.selectNodes(upstream);
    }
    
    public void selectDownstreamNodes() {
        Set<ProcessNode> selected = selectionManager.getSelectedNodes();
        if (selected.isEmpty()) {
            log("⚠ 请先选中一个节点");
            return;
        }
        Set<ProcessNode> downstream = new HashSet<>();
        for (ProcessNode node : selected) {
            findDownstreamNodes(node, downstream);
        }
        selectionManager.selectNodes(downstream);
    }
    
    private void findUpstreamNodes(ProcessNode node, Set<ProcessNode> result) {
        for (NodeConnection conn : connections) {
            if (conn.getTargetNode() == node) {
                ProcessNode source = conn.getSourceNode();
                if (source != null && !result.contains(source)) {
                    result.add(source);
                    findUpstreamNodes(source, result);
                }
            }
        }
    }
    
    private void findDownstreamNodes(ProcessNode node, Set<ProcessNode> result) {
        for (NodeConnection conn : connections) {
            if (conn.getSourceNode() == node) {
                ProcessNode target = conn.getTargetNode();
                if (target != null && !result.contains(target)) {
                    result.add(target);
                    findDownstreamNodes(target, result);
                }
            }
        }
    }
    
    public void toggleGrid() {
        gridVisible = !gridVisible;
        updateGridVisibility();
        if (gridVisible) {
            log("✓ 网格已显示");
        } else {
            log("✓ 网格已隐藏");
        }
    }
    
    /**
     * 初始化标尺（默认显示）
     */
    private void initializeRuler() {
        ruler = new CanvasRuler();
        ruler.updateCanvasSize(getPrefWidth(), getPrefHeight());
        this.getChildren().add(0, ruler);
        ruler.toBack();
        // 确保标尺不拦截鼠标事件
        ruler.setMouseTransparent(true);
        
        // 监听画布大小变化
        widthProperty().addListener((obs, oldVal, newVal) -> {
            if (ruler != null && rulerVisible) {
                ruler.updateCanvasSize(newVal.doubleValue(), getPrefHeight());
            }
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            if (ruler != null && rulerVisible) {
                ruler.updateCanvasSize(getPrefWidth(), newVal.doubleValue());
            }
        });
        
        ruler.setVisible(true);
        ruler.setManaged(true);
    }
    
    /**
     * 切换标尺显示
     */
    public void toggleRuler() {
        rulerVisible = !rulerVisible;
        if (ruler == null) {
            ruler = new CanvasRuler();
            ruler.updateCanvasSize(getPrefWidth(), getPrefHeight());
            // 将标尺放在最底层
            if (!this.getChildren().contains(ruler)) {
                this.getChildren().add(0, ruler);
            }
            ruler.toBack(); // 确保在最底层
            
            // 监听画布大小变化
            widthProperty().addListener((obs, oldVal, newVal) -> {
                if (ruler != null && rulerVisible) {
                    ruler.updateCanvasSize(newVal.doubleValue(), getPrefHeight());
                }
            });
            heightProperty().addListener((obs, oldVal, newVal) -> {
                if (ruler != null && rulerVisible) {
                    ruler.updateCanvasSize(getPrefWidth(), newVal.doubleValue());
                }
            });
        }
        
        // 确保ruler在画布中
        if (!this.getChildren().contains(ruler)) {
            this.getChildren().add(0, ruler);
            ruler.toBack();
        }
        
        // 确保标尺不拦截鼠标事件
        ruler.setMouseTransparent(true);
        
        ruler.setVisible(rulerVisible);
        ruler.setManaged(rulerVisible); // 控制是否占用布局空间
        
        if (rulerVisible) {
            // 更新标尺大小
            ruler.updateCanvasSize(getPrefWidth(), getPrefHeight());
            log("✓ 标尺已显示");
        } else {
            log("✓ 标尺已隐藏");
        }
    }
    
    /**
     * 设置坐标显示
     */
    private void setupCoordinateDisplay() {
        coordinateLabel = new Label();
        coordinateLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-text-fill: #6B7280; " +
            "-fx-background-color: rgba(255, 255, 255, 0.9); " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 4 8 4 8;"
        );
        coordinateLabel.setVisible(false);
        coordinateLabel.setMouseTransparent(true);
        this.getChildren().add(coordinateLabel);
        
        // 鼠标移动时显示坐标（仅在未拖拽节点时）
        this.setOnMouseMoved(e -> {
            // 如果正在拖拽节点，不更新鼠标坐标（让节点坐标显示）
            if (isDragging && currentDragNode != null) {
                return;
            }
            
            double x = e.getX();
            double y = e.getY();
            coordinateLabel.setText(String.format("坐标: (%.0f, %.0f)", x, y));
            
            // 计算坐标标签位置（相对于画布）
            if (this.getScene() != null) {
                coordinateLabel.setLayoutX(e.getSceneX() - this.getScene().getX() + 10);
                coordinateLabel.setLayoutY(e.getSceneY() - this.getScene().getY() - 30);
            } else {
                // 如果scene还未初始化，使用相对坐标
                coordinateLabel.setLayoutX(e.getX() + 10);
                coordinateLabel.setLayoutY(e.getY() - 30);
            }
            coordinateLabel.setVisible(true);
        });
        
        this.setOnMouseExited(e -> {
            coordinateLabel.setVisible(false);
        });
    }
    
    /**
     * 更新网格可见性
     */
    private void updateGridVisibility() {
        if (gridVisible) {
            // 如果网格Canvas不存在，创建它
            if (gridBackgroundCanvas == null) {
                createGridBackground();
            } else {
                // 如果已存在，确保它可见
                if (!this.getChildren().contains(gridBackgroundCanvas)) {
                    this.getChildren().add(0, gridBackgroundCanvas);
                }
                gridBackgroundCanvas.setVisible(true);
            }
        } else {
            // 隐藏网格（但不删除，以便快速切换）
            if (gridBackgroundCanvas != null) {
                gridBackgroundCanvas.setVisible(false);
            }
        }
    }
    
    public void toggleNodeLabels() {
        nodeLabelsVisible = !nodeLabelsVisible;
        updateAllNodeLabelsVisibility();
        if (nodeLabelsVisible) {
            log("✓ 节点标签已显示");
        } else {
            log("✓ 节点标签已隐藏");
        }
    }
    
    /**
     * 更新所有节点的标签可见性
     */
    private void updateAllNodeLabelsVisibility() {
        for (ProcessNode node : nodes) {
            node.setTagsVisible(nodeLabelsVisible);
        }
    }
    
    /**
     * 设置节点标签（从properties中读取）
     */
    public void setNodeTagsFromProperties(ProcessNode node, Map<String, Object> properties) {
        if (properties == null || node == null) return;
        
        Object tagsObj = properties.get("tags");
        if (tagsObj != null) {
            java.util.List<String> tags = new java.util.ArrayList<>();
            if (tagsObj instanceof java.util.List) {
                for (Object tag : (java.util.List<?>) tagsObj) {
                    if (tag != null) {
                        tags.add(tag.toString());
                    }
                }
            } else if (tagsObj instanceof String) {
                // 尝试解析JSON数组字符串
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.util.List<?> tagList = gson.fromJson((String) tagsObj, java.util.List.class);
                    for (Object tag : tagList) {
                        if (tag != null) {
                            tags.add(tag.toString());
                        }
                    }
                } catch (Exception e) {
                    // 解析失败，忽略
                }
            }
            node.setTags(tags);
            node.setTagsVisible(nodeLabelsVisible);
        }
    }
    
    public void toggleEdgeLabels() {
        edgeLabelsVisible = !edgeLabelsVisible;
        updateAllEdgeLabelsVisibility();
        if (edgeLabelsVisible) {
            log("✓ 连线标签已显示");
        } else {
            log("✓ 连线标签已隐藏");
        }
    }
    
    /**
     * 更新所有连线的标签可见性
     */
    private void updateAllEdgeLabelsVisibility() {
        for (NodeConnection conn : connections) {
            conn.setLabelVisible(edgeLabelsVisible);
        }
    }
    
    /**
     * 为连线设置标签（从edge数据中读取）
     */
    public void setEdgeLabelFromData(NodeConnection conn, String labelText) {
        if (conn != null && labelText != null && !labelText.trim().isEmpty()) {
            conn.setLabelText(labelText);
            conn.setLabelVisible(edgeLabelsVisible);
        }
    }
    
    /**
     * 自动布局（使用默认的网格布局）
     */
    public void autoLayout() {
        autoLayout(CanvasLayoutManager.LayoutAlgorithm.GRID);
    }
    
    /**
     * 自动布局（指定布局算法），作为一步撤销入栈
     * @param algorithm 布局算法类型
     */
    public void autoLayout(CanvasLayoutManager.LayoutAlgorithm algorithm) {
        if (nodes.isEmpty()) {
            log("⚠ 画布中没有节点");
            return;
        }
        
        if (layoutManager == null) {
            layoutManager = new CanvasLayoutManager(this::log);
        }
        
        Map<ProcessNode, double[]> oldPositions = snapshotNodePositions(nodes);
        layoutManager.layout(nodes, connections, algorithm);
        Map<ProcessNode, double[]> newPositions = snapshotNodePositions(nodes);
        pushAction(new LayoutAction(oldPositions, newPositions));
        markAsUnsaved();
    }
    
    /** 对指定节点列表做位置快照，用于 LayoutAction/BatchMoveAction */
    private Map<ProcessNode, double[]> snapshotNodePositions(Collection<ProcessNode> nodeList) {
        Map<ProcessNode, double[]> map = new HashMap<>();
        if (nodeList != null) {
            for (ProcessNode node : nodeList) {
                if (node != null) {
                    map.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
                }
            }
        }
        return map;
    }
    
    /**
     * 获取布局管理器
     */
    public CanvasLayoutManager getLayoutManager() {
        return layoutManager;
    }
    
    /**
     * 按名称查找节点（精确匹配）
     */
    public ProcessNode findNodeByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (ProcessNode node : nodes) {
            // 使用节点的jobHandlerName来匹配
            String nodeName = node.getJobHandlerName();
            if (name.equals(nodeName)) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * 模糊搜索节点（按名称）
     */
    public List<ProcessNode> findNodesByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        List<ProcessNode> results = new ArrayList<>();
        for (ProcessNode node : nodes) {
            String nodeName = node.getJobHandlerName();
            if (nodeName != null && nodeName.toLowerCase().contains(lowerKeyword)) {
                results.add(node);
            }
        }
        return results;
    }
    
    /**
     * 获取搜索管理器
     */
    public NodeSearchManager getSearchManager() {
        if (searchManager == null) {
            searchManager = new NodeSearchManager(this::log);
        }
        return searchManager;
    }
    
    private NodeSearchManager searchManager; // 搜索管理器
    
    private int currentNodeIndex = -1;
    
    public void navigateToPreviousNode() {
        if (nodes.isEmpty()) {
            return;
        }
        if (currentNodeIndex <= 0) {
            currentNodeIndex = nodes.size() - 1;
        } else {
            currentNodeIndex--;
        }
        ProcessNode node = nodes.get(currentNodeIndex);
        locateNode(node);
        selectNode(node);
    }
    
    public void navigateToNextNode() {
        if (nodes.isEmpty()) {
            return;
        }
        if (currentNodeIndex >= nodes.size() - 1) {
            currentNodeIndex = 0;
        } else {
            currentNodeIndex++;
        }
        ProcessNode node = nodes.get(currentNodeIndex);
        locateNode(node);
        selectNode(node);
    }
    
    public void locateRunningNode() {
        for (ProcessNode node : nodes) {
            if (node.getStatus() == ProcessNode.NodeStatus.RUNNING) {
                locateNode(node);
                selectNode(node);
                return;
            }
        }
        log("⚠ 未找到运行中的节点");
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
        
        MenuItem addConditionNodeItem = new MenuItem("创建条件节点");
        addConditionNodeItem.setOnAction(e -> { if (onRequestAddConditionNode != null) onRequestAddConditionNode.run(); });
        
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
                //addConditionNodeItem,
                new SeparatorMenuItem(),
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
     * 计算画布上所有内容（节点、组容器、条件节点）的包围盒，用于首次进入时扩展画布并居中。
     * 无内容时返回 null。
     */
    public Bounds getContentBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        boolean hasAny = false;
        for (ProcessNode node : nodes) {
            double w = node.getWidth() > 0 ? node.getWidth() : node.getPrefWidth();
            double h = node.getHeight() > 0 ? node.getHeight() : node.getPrefHeight();
            if (w <= 0 || h <= 0) continue;
            double x = node.getLayoutX();
            double y = node.getLayoutY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
            hasAny = true;
        }
        for (GroupContainer container : groupContainers) {
            double w = container.getFrame().getWidth();
            double h = container.getFrame().getHeight();
            if (w <= 0 || h <= 0) continue;
            double x = container.getLayoutX();
            double y = container.getLayoutY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
            hasAny = true;
        }
        for (ConditionNode conditionNode : conditionNodes) {
            double w = conditionNode.getFrame().getWidth();
            double h = conditionNode.getFrame().getHeight();
            if (w <= 0 || h <= 0) continue;
            double x = conditionNode.getLayoutX();
            double y = conditionNode.getLayoutY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
            hasAny = true;
        }
        if (!hasAny) return null;
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
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
            
            // ⭐ 修复：先创建所有普通节点，但不添加到画布（只创建节点对象和nodeMap）
            Map<String, ProcessNode> nodeMap = new HashMap<>();
            if (composeData.getNodes() != null) {
                for (JobComposeData.NodeData nodeData : composeData.getNodes()) {
                    // 跳过条件节点和任务组节点
                    if (nodeData.getType() != null && 
                        ("ConditionNode".equals(nodeData.getType()) || "condition-node".equalsIgnoreCase(nodeData.getType()))) {
                        continue;
                    }
                    if (nodeData.getType() != null && 
                        ("CustomGroup".equals(nodeData.getType()) || "custom-group".equalsIgnoreCase(nodeData.getType()))) {
                        continue;
                    }
                    
                    ProcessNode node = dataLoader.createNodeFromData(nodeData);
                    if (node != null) {
                        setupNodeCallbacks(node);
                        
                        // 从全局管理器恢复节点状态（开始/终止/阻塞）
                        if (node.getJobId() != null) {
                            NodeGraphStateManager stateManager = NodeGraphStateManager.getInstance();
                            
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
                        
                        nodeMap.put(nodeData.getId(), node);
                    }
                }
            }
            
            // ⭐ 修复：先加载所有条件节点（不绑定子节点），建立条件节点Map
            Map<String, ConditionNode> conditionNodeMap = new HashMap<>();
            if (composeData.getNodes() != null) {
                for (JobComposeData.NodeData nodeData : composeData.getNodes()) {
                    if (nodeData.getType() != null && 
                        ("ConditionNode".equals(nodeData.getType()) || "condition-node".equalsIgnoreCase(nodeData.getType()))) {
                        ConditionNode conditionNode = createConditionNodeFromData(nodeData);
                        if (conditionNode != null) {
                            conditionNodeMap.put(nodeData.getId(), conditionNode);
                            addConditionNode(conditionNode);
                        }
                    }
                }
            }
            
            // ⭐ 修复：现在绑定所有条件节点的子节点（此时所有条件节点都已加载）
            for (JobComposeData.NodeData nodeData : composeData.getNodes()) {
                if (nodeData.getType() != null && 
                    ("ConditionNode".equals(nodeData.getType()) || "condition-node".equalsIgnoreCase(nodeData.getType()))) {
                    ConditionNode conditionNode = conditionNodeMap.get(nodeData.getId());
                    if (conditionNode != null) {
                        bindConditionNodeChildren(conditionNode, nodeData, nodeMap, conditionNodeMap);
                    }
                }
            }
            
            // ⭐ 修复：只将不在任何条件节点内的普通节点添加到画布
            Set<String> nodesInContainers = new HashSet<>();
            for (ConditionNode conditionNode : conditionNodes) {
                for (ProcessNode managedNode : conditionNode.getManagedCanvasNodes()) {
                    if (managedNode.getNodeId() != null) {
                        nodesInContainers.add(managedNode.getNodeId());
                    }
                }
            }
            
            for (ProcessNode node : nodeMap.values()) {
                if (node.getNodeId() != null && !nodesInContainers.contains(node.getNodeId())) {
                    nodeManager.addNode(node);
                }
            }
            
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
                    
                    // ⭐ 修复：如果节点不在nodeMap中，可能是条件节点
                    if (sourceNode == null) {
                        for (ConditionNode cn : conditionNodes) {
                            if (cn.getNodeId() != null && cn.getNodeId().equals(edgeData.getSourceNodeId())) {
                                sourceNode = null; // 条件节点作为连接源，需要特殊处理
                                break;
                            }
                        }
                    }
                    if (targetNode == null) {
                        for (ConditionNode cn : conditionNodes) {
                            if (cn.getNodeId() != null && cn.getNodeId().equals(edgeData.getTargetNodeId())) {
                                targetNode = null; // 条件节点作为连接目标，需要特殊处理
                                break;
                            }
                        }
                    }
                    
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
                                
                                // ⭐ 修复：从properties恢复连线样式、颜色和标签信息
                                Map<String, Object> properties = edgeData.getProperties();
                                if (properties != null && !properties.isEmpty()) {
                                    // 恢复样式（默认 SOLID）
                                    if (properties.containsKey("edgeStyle")) {
                                        try {
                                            String styleName = properties.get("edgeStyle").toString();
                                            NodeConnection.EdgeStyle style = NodeConnection.EdgeStyle.valueOf(styleName);
                                            edge.setEdgeStyle(style);
                                        } catch (Exception e) {
                                            log("⚠ 无法解析连线样式: " + properties.get("edgeStyle") + "，使用默认样式 SOLID");
                                            edge.setEdgeStyle(NodeConnection.EdgeStyle.SOLID);
                                        }
                                    } else {
                                        // 如果样式缺失，使用默认样式
                                        edge.setEdgeStyle(NodeConnection.EdgeStyle.SOLID);
                                    }
                                    
                                    // 恢复颜色（默认 #374151）
                                    if (properties.containsKey("edgeColor")) {
                                        String edgeColor = properties.get("edgeColor").toString();
                                        if (edgeColor != null && !edgeColor.isEmpty()) {
                                            edge.setEdgeColor(edgeColor);
                                        } else {
                                            edge.setEdgeColor("#374151");
                                        }
                                    } else {
                                        // 如果颜色缺失，使用默认颜色
                                        edge.setEdgeColor("#374151");
                                    }
                                    
                                    // 恢复标签（默认空字符串）
                                    if (properties.containsKey("labelText")) {
                                        String labelText = properties.get("labelText").toString();
                                        if (labelText != null) {
                                            edge.setLabelText(labelText);
                                        } else {
                                            edge.setLabelText("");
                                        }
                                    } else {
                                        // 如果标签缺失，使用默认值（空字符串）
                                        edge.setLabelText("");
                                    }
                                } else {
                                    // 如果 properties 为空，使用所有默认值
                                    edge.setEdgeStyle(NodeConnection.EdgeStyle.SOLID);
                                    edge.setEdgeColor("#374151");
                                    edge.setLabelText("");
                                }
                                
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
    
    /**
     * ⭐ 新增：从NodeData创建ConditionNode（不绑定子节点）
     */
    private ConditionNode createConditionNodeFromData(JobComposeData.NodeData nodeData) {
        try {
            String nodeId = nodeData.getId();
            // ⭐ 修复：条件节点的jobId应该为null，移除对jobId的依赖
            // Long conditionId = nodeData.getJobId(); // 不再需要
            
            // ⭐ 修复：优化jobName获取逻辑，优先从conditionExpression获取
            String conditionName = "条件节点";
            if (nodeData.getProperties() != null) {
                Object conditionExpr = nodeData.getProperties().get("conditionExpression");
                if (conditionExpr != null && !conditionExpr.toString().trim().isEmpty()) {
                    conditionName = conditionExpr.toString();
                } else if (nodeData.getJobName() != null && !nodeData.getJobName().trim().isEmpty()) {
                    conditionName = nodeData.getJobName();
                }
            } else if (nodeData.getJobName() != null && !nodeData.getJobName().trim().isEmpty()) {
                conditionName = nodeData.getJobName();
            }
            
            // 从properties读取conditionType
            ConditionNode.ConditionType conditionType = ConditionNode.ConditionType.IF;
            if (nodeData.getProperties() != null) {
                Object conditionTypeObj = nodeData.getProperties().get("conditionType");
                if (conditionTypeObj != null) {
                    try {
                        conditionType = ConditionNode.ConditionType.valueOf(conditionTypeObj.toString().toUpperCase());
                    } catch (Exception e) {
                        // 如果解析失败，使用默认值IF
                        conditionType = ConditionNode.ConditionType.IF;
                    }
                }
            }
            
            ConditionNode conditionNode = new ConditionNode(nodeId,  conditionName, conditionType);
            
            // 设置位置
            if (nodeData.getX() != null && nodeData.getY() != null) {
                conditionNode.setLayoutX(nodeData.getX());
                conditionNode.setLayoutY(nodeData.getY());
            }
            
            // 设置条件表达式
            if (nodeData.getProperties() != null) {
                Object conditionExpr = nodeData.getProperties().get("conditionExpression");
                Object exprType = nodeData.getProperties().get("expressionType");
                if (conditionExpr != null) {
                    conditionNode.setConditionExpression(conditionExpr.toString());
                }
                if (exprType != null) {
                    try {
                        conditionNode.setExpressionType(ConditionNode.ExpressionType.valueOf(exprType.toString().toUpperCase()));
                    } catch (Exception e) {
                        conditionNode.setExpressionType(ConditionNode.ExpressionType.SIMPLE);
                    }
                }
                
                // ⭐ 修复：从properties读取并设置容器大小
                Object widthObj = nodeData.getProperties().get("width");
                Object heightObj = nodeData.getProperties().get("height");
                if (widthObj != null && heightObj != null) {
                    try {
                        double width = widthObj instanceof Number ? ((Number) widthObj).doubleValue() : Double.parseDouble(widthObj.toString());
                        double height = heightObj instanceof Number ? ((Number) heightObj).doubleValue() : Double.parseDouble(heightObj.toString());
                        conditionNode.setSize(width, height);
                    } catch (Exception e) {
                        // 如果解析失败，使用默认大小
                        conditionNode.setSize(320, 200);
                    }
                }
            }
            
            // ⭐ 修复：设置大小改变回调，标记需要保存
            conditionNode.setOnSizeChanged(() -> markAsUnsaved());
            
            log("✓ 创建条件节点: " + conditionName);
            return conditionNode;
        } catch (Exception e) {
            log("✗ 创建条件节点失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * ⭐ 新增：绑定条件节点的子节点（包括普通节点和嵌套的条件节点）
     */
    private void bindConditionNodeChildren(ConditionNode conditionNode, JobComposeData.NodeData nodeData, 
                                            Map<String, ProcessNode> nodeMap, Map<String, ConditionNode> conditionNodeMap) {
        if (nodeData.getProperties() == null) return;
        
        Object childrenObj = nodeData.getProperties().get("children");
        
        // ⭐ 修复：处理children字段可能是String（JSON字符串）或List的情况
        List<?> childrenList = null;
        if (childrenObj instanceof List) {
            childrenList = (List<?>) childrenObj;
        } else if (childrenObj instanceof String) {
            // 如果是JSON字符串，尝试解析
            try {
                com.google.gson.Gson gson = com.cc.job.gui.util.ApiUtil.getInstance().getGson();
                childrenList = gson.fromJson((String) childrenObj, new com.google.gson.reflect.TypeToken<List<Object>>(){}.getType());
            } catch (Exception e) {
                return;
            }
        } else if (childrenObj != null) {
            return;
        } else {
            return;
        }
        
        if (childrenList == null || childrenList.isEmpty()) return;
        
        List<String> childIds = new ArrayList<>();
        for (Object childId : childrenList) {
            if (childId != null) {
                childIds.add(childId.toString());
            }
        }
        
        // 分离普通节点和条件节点
        List<ProcessNode> childNodes = new ArrayList<>();
        List<ConditionNode> childConditionNodes = new ArrayList<>();
        
        for (String childId : childIds) {
            // 先尝试从nodeMap中查找（普通节点）
            ProcessNode childNode = nodeMap.get(childId);
            if (childNode != null) {
                childNodes.add(childNode);
            } else {
                // 如果不是普通节点，可能是嵌套的条件节点
                // 从conditionNodeMap中查找
                ConditionNode childConditionNode = conditionNodeMap.get(childId);
                if (childConditionNode != null) {
                    childConditionNodes.add(childConditionNode);
                }
            }
        }
        
        // ⭐ 修复：绑定普通节点（绑定后，这些节点会从画布的nodes列表中移除）
        if (!childNodes.isEmpty()) {
            // 从画布的nodes列表中移除这些节点（因为它们现在由条件节点管理）
            for (ProcessNode childNode : childNodes) {
                if (nodes.contains(childNode)) {
                    nodes.remove(childNode);
                    this.getChildren().remove(childNode);
                }
            }
            
            // ⭐ 修复：绑定节点到条件节点（这会添加节点到contentLayer）
            conditionNode.bindCanvasNodes(childNodes);
        }
        
        // ⭐ 修复：绑定嵌套的条件节点
        if (!childConditionNodes.isEmpty()) {
            conditionNode.bindConditionNodes(childConditionNodes);
        }
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
        // 保存需要保留的基础UI元素
        CanvasRuler savedRuler = ruler;
        Label savedCoordinateLabel = coordinateLabel;
        Canvas savedDotsBackground = dotsBackgroundCanvas;
        Canvas savedGridBackground = gridBackgroundCanvas;
        
        groupContainers.forEach(this.getChildren()::remove);
        groupContainers.clear();
        
        conditionNodes.forEach(this.getChildren()::remove);
        conditionNodes.clear();
        
        this.getChildren().clear();
        
        nodes.clear();
        connections.clear();
        
        // 重新添加需要保留的基础UI元素
        if (savedRuler != null && rulerVisible) {
            this.getChildren().add(0, savedRuler);
            savedRuler.toBack();
        }
        if (savedCoordinateLabel != null) {
            this.getChildren().add(savedCoordinateLabel);
        }
        if (savedDotsBackground != null) {
            this.getChildren().add(0, savedDotsBackground);
            savedDotsBackground.toBack();
        }
        if (savedGridBackground != null) {
            this.getChildren().add(0, savedGridBackground);
            savedGridBackground.toBack();
        }
        
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
        conditionNodes.forEach(this.getChildren()::remove);
        conditionNodes.clear();
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
    
    // ==================== 条件节点容器 ====================
    
    public void addConditionNode(ConditionNode conditionNode) {
        if (conditionNode != null && !conditionNodes.contains(conditionNode)) {
            conditionNodes.add(conditionNode);
            if (!this.getChildren().contains(conditionNode)) {
                this.getChildren().add(conditionNode);
            }
            
            // 设置条件节点的回调函数
            setupConditionNodeCallbacks(conditionNode);
            
            // ⭐ 新增：设置大小改变回调，标记需要保存
            conditionNode.setOnSizeChanged(() -> markAsUnsaved());
            
            log("✓ 添加条件节点: " + conditionNode.getConditionName());
            notifyNodeStructureChanged();
        }
    }
    
    /**
     * 设置条件节点的回调函数
     */
    private void setupConditionNodeCallbacks(ConditionNode conditionNode) {
        // 设置编辑条件回调
        conditionNode.setOnEditCondition(() -> {
            if (onEditConditionNode != null) {
                onEditConditionNode.accept(conditionNode);
            }
        });
        
        // 设置删除回调
        conditionNode.setOnDelete(() -> {
            if (onDeleteConditionNode != null) {
                onDeleteConditionNode.accept(conditionNode);
            } else {
                // 如果没有外部回调，直接删除
                removeConditionNode(conditionNode);
            }
        });
        
        // ⭐ 新增：设置拖拽结束回调，检测条件节点是否进入其他容器
        conditionNode.setOnDragFinished(() -> {
            checkConditionNodeInConditionContainer(conditionNode);
            markAsUnsaved();
        });
        
        // ⭐ 新增：为条件节点设置连接点处理器，使其可以连接边
        setupConditionNodeConnectorHandlers(conditionNode);
    }
    
    /**
     * ⭐ 新增：为条件节点设置连接点处理器
     */
    private void setupConditionNodeConnectorHandlers(ConditionNode conditionNode) {
        // 为条件节点的四个连接点设置处理器
        setupConditionNodeConnectorHandler(conditionNode, conditionNode.getTopConnector());
        setupConditionNodeConnectorHandler(conditionNode, conditionNode.getBottomConnector());
        setupConditionNodeConnectorHandler(conditionNode, conditionNode.getLeftConnector());
        setupConditionNodeConnectorHandler(conditionNode, conditionNode.getRightConnector());
    }
    
    /**
     * ⭐ 新增：为条件节点的单个连接点设置处理器
     */
    private void setupConditionNodeConnectorHandler(ConditionNode conditionNode, Circle connector) {
        connector.setOnMousePressed(e -> {
            startOwner = conditionNode;
            startTempLineForConditionNode(conditionNode, connector);
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
            finishConnectionForConditionNode(connector, e.getSceneX(), e.getSceneY());
            e.consume();
        });
    }
    
    /**
     * ⭐ 新增：为条件节点启动临时连线
     */
    private void startTempLineForConditionNode(ConditionNode conditionNode, Circle connector) {
        tempLine = new Line();
        tempLine.setStroke(Color.web("#8B5CF6"));
        tempLine.setStrokeWidth(2);
        tempLine.getStrokeDashArray().addAll(5.0, 5.0);
        
        Point2D center = new Point2D(connector.getLayoutX() + connector.getRadius(), 
                                     connector.getLayoutY() + connector.getRadius());
        Point2D nodeLocal = conditionNode.getConnectorPane().localToParent(center);
        Point2D canvasLocal = conditionNode.localToParent(nodeLocal);
        
        tempLine.setStartX(canvasLocal.getX());
        tempLine.setStartY(canvasLocal.getY());
        tempLine.setEndX(canvasLocal.getX());
        tempLine.setEndY(canvasLocal.getY());
        
        this.getChildren().add(tempLine);
    }
    
    /**
     * ⭐ 新增：完成条件节点的连接
     */
    private void finishConnectionForConditionNode(Circle connector, double sceneX, double sceneY) {
        if (tempLine == null || startOwner == null) {
            return;
        }
        
        Point2D localPoint = sceneToLocal(sceneX, sceneY);
        Node targetNode = null;
        Circle targetConnector = null;
        
        // 查找目标节点和连接点
        for (Node child : this.getChildren()) {
            if (child instanceof ProcessNode processNode && child != startOwner) {
                Circle nearest = findNearestConnectorForNode(processNode, localPoint.getX(), localPoint.getY());
                if (nearest != null) {
                    targetNode = processNode;
                    targetConnector = nearest;
                    break;
                }
            } else if (child instanceof ConditionNode conditionNode && child != startOwner) {
                Circle nearest = findNearestConnectorForConditionNode(conditionNode, localPoint.getX(), localPoint.getY());
                if (nearest != null) {
                    targetNode = conditionNode;
                    targetConnector = nearest;
                    break;
                }
            } else if (child instanceof GroupContainer groupContainer && child != startOwner) {
                Circle nearest = findNearestConnectorForGroupContainer(groupContainer, localPoint.getX(), localPoint.getY());
                if (nearest != null) {
                    targetNode = groupContainer;
                    targetConnector = nearest;
                    break;
                }
            }
        }
        
        // 移除临时连线
        this.getChildren().remove(tempLine);
        tempLine = null;
        
        // 如果找到目标，创建连接
        if (targetNode != null && targetConnector != null && startOwner instanceof ConditionNode) {
            ConditionNode sourceConditionNode = (ConditionNode) startOwner;
            String sourceAnchor = getConnectorAnchor(sourceConditionNode, connector);
            String targetAnchor = getConnectorAnchorForNode(targetNode, targetConnector);
            
            // 获取源连接点和目标连接点
            Circle sourceConnectorCircle = getConnectorByAnchorForConditionNode(sourceConditionNode, sourceAnchor);
            Circle targetConnectorCircle = getConnectorByAnchorForNode(targetNode, targetAnchor);
            
            if (sourceConnectorCircle != null && targetConnectorCircle != null) {
                // ⭐ 修复：使用通用addConnection方法，支持ConditionNode、ProcessNode和GroupContainer
                NodeConnection connection = connectionManager.addConnection(
                    sourceConditionNode, sourceConditionNode.getConnectorPane(), sourceConnectorCircle,
                    targetNode, getConnectorParentForNode(targetNode), targetConnectorCircle
                );
                connections.add(connection);
                
                markAsUnsaved();
                notifyNodeStructureChanged();
            }
        }
        
        startOwner = null;
    }
    
    /**
     * ⭐ 新增：查找条件节点上距离指定位置最近的连接点
     */
    private Circle findNearestConnectorForConditionNode(ConditionNode conditionNode, double x, double y) {
        Circle[] connectors = {
            conditionNode.getTopConnector(),
            conditionNode.getBottomConnector(),
            conditionNode.getLeftConnector(),
            conditionNode.getRightConnector()
        };
        
        Circle nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Circle connector : connectors) {
            Point2D connectorCenter = new Point2D(
                connector.getLayoutX() + connector.getRadius(),
                connector.getLayoutY() + connector.getRadius()
            );
            Point2D nodeLocal = conditionNode.getConnectorPane().localToParent(connectorCenter);
            Point2D canvasLocal = conditionNode.localToParent(nodeLocal);
            
            double dx = canvasLocal.getX() - x;
            double dy = canvasLocal.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < minDistance && distance < 30) { // 30像素范围内
                minDistance = distance;
                nearest = connector;
            }
        }
        
        return nearest;
    }
    
    /**
     * ⭐ 新增：获取条件节点连接点的锚点名称
     */
    private String getConnectorAnchor(ConditionNode conditionNode, Circle connector) {
        if (connector == conditionNode.getTopConnector()) return "top";
        if (connector == conditionNode.getBottomConnector()) return "bottom";
        if (connector == conditionNode.getLeftConnector()) return "left";
        if (connector == conditionNode.getRightConnector()) return "right";
        return "right";
    }
    
    /**
     * ⭐ 新增：根据锚点获取条件节点的连接点
     */
    private Circle getConnectorByAnchorForConditionNode(ConditionNode conditionNode, String anchor) {
        if (anchor != null && !anchor.isEmpty()) {
            return switch (anchor.toLowerCase()) {
                case "top" -> conditionNode.getTopConnector();
                case "bottom" -> conditionNode.getBottomConnector();
                case "left" -> conditionNode.getLeftConnector();
                case "right" -> conditionNode.getRightConnector();
                default -> conditionNode.getRightConnector();
            };
        }
        return conditionNode.getRightConnector();
    }
    
    /**
     * ⭐ 新增：根据锚点获取节点的连接点（通用方法）
     */
    private Circle getConnectorByAnchorForNode(Node node, String anchor) {
        if (node instanceof ProcessNode processNode) {
            return getConnectorByAnchor(processNode, anchor, false);
        } else if (node instanceof ConditionNode conditionNode) {
            return getConnectorByAnchorForConditionNode(conditionNode, anchor);
        } else if (node instanceof GroupContainer groupContainer) {
            return getConnectorByAnchorForGroupContainer(groupContainer, anchor);
        }
        return null;
    }
    
    /**
     * ⭐ 新增：根据锚点获取任务组的连接点
     */
    private Circle getConnectorByAnchorForGroupContainer(GroupContainer groupContainer, String anchor) {
        if (anchor != null && !anchor.isEmpty()) {
            return switch (anchor.toLowerCase()) {
                case "top" -> groupContainer.getTopConnector();
                case "bottom" -> groupContainer.getBottomConnector();
                case "left" -> groupContainer.getLeftConnector();
                case "right" -> groupContainer.getRightConnector();
                default -> groupContainer.getRightConnector();
            };
        }
        return groupContainer.getRightConnector();
    }
    
    /**
     * ⭐ 新增：获取节点的连接点父层
     */
    private Pane getConnectorParentForNode(Node node) {
        if (node instanceof ProcessNode processNode) {
            return processNode.getConnectorPane();
        } else if (node instanceof ConditionNode conditionNode) {
            return conditionNode.getConnectorPane();
        } else if (node instanceof GroupContainer groupContainer) {
            return groupContainer.getConnectorPane();
        }
        return null;
    }
    
    /**
     * ⭐ 新增：获取节点连接点的锚点名称（通用方法）
     */
    private String getConnectorAnchorForNode(Node node, Circle connector) {
        if (node instanceof ProcessNode processNode) {
            if (connector == processNode.getTopConnector()) return "top";
            if (connector == processNode.getBottomConnector()) return "bottom";
            if (connector == processNode.getLeftConnector()) return "left";
            if (connector == processNode.getRightConnector()) return "right";
        } else if (node instanceof ConditionNode conditionNode) {
            return getConnectorAnchor(conditionNode, connector);
        } else if (node instanceof GroupContainer groupContainer) {
            if (connector == groupContainer.getTopConnector()) return "top";
            if (connector == groupContainer.getBottomConnector()) return "bottom";
            if (connector == groupContainer.getLeftConnector()) return "left";
            if (connector == groupContainer.getRightConnector()) return "right";
        }
        return "right";
    }
    
    /**
     * ⭐ 新增：查找ProcessNode上距离指定位置最近的连接点
     */
    private Circle findNearestConnectorForNode(ProcessNode processNode, double x, double y) {
        return nodeManager.findNearestConnector(processNode, x, y);
    }
    
    /**
     * ⭐ 新增：查找GroupContainer上距离指定位置最近的连接点
     */
    private Circle findNearestConnectorForGroupContainer(GroupContainer groupContainer, double x, double y) {
        Circle[] connectors = {
            groupContainer.getTopConnector(),
            groupContainer.getBottomConnector(),
            groupContainer.getLeftConnector(),
            groupContainer.getRightConnector()
        };
        
        Circle nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Circle connector : connectors) {
            Point2D connectorCenter = new Point2D(
                connector.getLayoutX() + connector.getRadius(),
                connector.getLayoutY() + connector.getRadius()
            );
            Point2D nodeLocal = groupContainer.getConnectorPane().localToParent(connectorCenter);
            Point2D canvasLocal = groupContainer.localToParent(nodeLocal);
            
            double dx = canvasLocal.getX() - x;
            double dy = canvasLocal.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < minDistance && distance < 30) { // 30像素范围内
                minDistance = distance;
                nearest = connector;
            }
        }
        
        return nearest;
    }
    
    public void removeConditionNode(ConditionNode conditionNode) {
        if (conditionNode == null) return;
        
        List<NodeConnection> attachedConnections = new ArrayList<>();
        for (NodeConnection conn : new ArrayList<>(connections)) {
            if (conn.getSourceOwner() == conditionNode || conn.getTargetOwner() == conditionNode) {
                attachedConnections.add(conn);
                connectionManager.removeConnection(conn);
            }
        }
        
        // 移除管理的节点
        for (ProcessNode managedNode : conditionNode.getManagedCanvasNodes()) {
            removeNode(managedNode, false);
        }
        
        // 移除管理的条件节点
        for (ConditionNode managedConditionNode : conditionNode.getManagedConditionNodes()) {
            removeConditionNode(managedConditionNode);
        }
        
        conditionNodes.remove(conditionNode);
        this.getChildren().remove(conditionNode);
        
        log("✓ 删除条件节点: " + conditionNode.getConditionName());
        notifyNodeStructureChanged();
    }
    
    /**
     * 检测节点是否进入条件节点容器，如果是则自动加入容器
     * 支持递归检查嵌套的条件节点
     */
    private void checkNodeInConditionContainer(ProcessNode node) {
        if (node == null) return;
        
        // ⭐ 修复：递归检查所有条件节点（包括嵌套的）
        ConditionNode foundContainer = findConditionNodeContaining(node, conditionNodes);
        if (foundContainer != null) {
            // ⭐ 修复：节点已经在容器的managedCanvasNodes中，说明它已经被正确管理
            // 不需要检查位置，因为节点在contentLayer中时，坐标是相对坐标，位置检查会出错
            // 节点在contentLayer中会自动跟随条件节点移动，所以只要在managedCanvasNodes中就认为在容器内
            return;
        }
        
        // ⭐ 修复：检查节点是否进入任何条件节点容器（包括嵌套的）
        // 但是，如果节点已经在contentLayer中（父节点是Pane且父节点的父节点是条件节点），
        // 说明节点已经在某个容器中，不应该再次检查
        javafx.scene.Node parent = node.getParent();
        boolean nodeInContentLayer = false;
        if (parent != null) {
            javafx.scene.Node grandParent = parent.getParent();
            if (grandParent instanceof ConditionNode) {
                nodeInContentLayer = true;
            }
        }
        
        // 只有节点不在contentLayer中时，才检查是否进入容器
        if (!nodeInContentLayer) {
            ConditionNode targetContainer = findConditionNodeForNode(node, conditionNodes);
            if (targetContainer != null && targetContainer.isExpanded()) {
                // 节点进入容器，添加到容器中
                addNodeToConditionContainer(node, targetContainer);
            }
        }
    }
    
    /**
     * 递归查找包含指定节点的条件节点
     */
    private ConditionNode findConditionNodeContaining(ProcessNode node, List<ConditionNode> conditionNodes) {
        for (ConditionNode conditionNode : conditionNodes) {
            // 检查当前条件节点是否包含该节点
            if (conditionNode.getManagedCanvasNodes().contains(node)) {
                return conditionNode;
            }
            // 递归检查嵌套的条件节点
            ConditionNode nested = findConditionNodeContaining(node, conditionNode.getManagedConditionNodes());
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
    
    /**
     * 递归查找节点应该加入的条件节点容器 - ⭐ 优化：返回最内层容器
     */
    private ConditionNode findConditionNodeForNode(ProcessNode node, List<ConditionNode> conditionNodes) {
        // ⭐ 修复：如果节点已经在某个容器的managedCanvasNodes中，不应该再查找
        // 因为节点在contentLayer中时，坐标是相对坐标，不能用于位置检查
        for (ConditionNode conditionNode : conditionNodes) {
            if (conditionNode.getManagedCanvasNodes().contains(node)) {
                return null; // 节点已经在容器中，不需要再查找
            }
            // 递归检查嵌套的条件节点
            ConditionNode nested = findConditionNodeForNode(node, conditionNode.getManagedConditionNodes());
            if (nested != null) {
                return null; // 节点已经在嵌套容器中，不需要再查找
            }
        }
        
        // ⭐ 修复：优先检查嵌套的条件节点（最内层），如果找到就直接返回
        for (ConditionNode conditionNode : conditionNodes) {
            if (conditionNode.isExpanded()) {
                // 先递归检查嵌套的条件节点
                ConditionNode nested = findConditionNodeForNode(node, conditionNode.getManagedConditionNodes());
                if (nested != null) {
                    return nested; // 返回最内层的容器
                }
                // 如果嵌套容器中没有找到，再检查当前容器
                if (isNodeInContainer(node, conditionNode)) {
                    return conditionNode;
                }
            }
        }
        return null;
    }
    
    /**
     * 判断节点是否在条件节点容器内
     * ⭐ 修复：如果节点已经在容器的managedCanvasNodes中，直接返回true
     * 因为节点在contentLayer中时，坐标是相对坐标，不能直接用绝对坐标比较
     */
    private boolean isNodeInContainer(ProcessNode node, ConditionNode conditionNode) {
        if (node == null || conditionNode == null || !conditionNode.isExpanded()) {
            return false;
        }
        
        // ⭐ 修复：如果节点已经在容器的managedCanvasNodes中，说明它已经被正确管理
        // 节点在contentLayer中时，坐标是相对坐标，不需要检查位置
        if (conditionNode.getManagedCanvasNodes().contains(node)) {
            return true;
        }
        
        // ⭐ 修复：如果节点不在managedCanvasNodes中，使用绝对坐标检查（节点可能在画布上）
        // 但是，如果节点的父节点是contentLayer，说明节点已经在容器中，不应该再检查位置
        javafx.scene.Node parent = node.getParent();
        if (parent != null) {
            // 检查父节点是否是contentLayer（通过检查父节点的父节点是否是条件节点）
            javafx.scene.Node grandParent = parent.getParent();
            if (grandParent != null && grandParent == conditionNode) {
                // 节点在contentLayer中，说明它已经在容器中，直接返回true
                return true;
            }
        }
        
        // 如果节点不在contentLayer中，使用绝对坐标检查（节点可能在画布上）
        double nodeX = node.getLayoutX();
        double nodeY = node.getLayoutY();
        
        double nodeWidth = node.getPrefWidth();
        double nodeHeight = node.getPrefHeight();
        
        double containerX = conditionNode.getLayoutX();
        double containerY = conditionNode.getLayoutY();
        double containerWidth = conditionNode.getFrame().getWidth();
        double containerHeight = conditionNode.getFrame().getHeight();
        
        // 计算节点中心点
        double nodeCenterX = nodeX + nodeWidth / 2;
        double nodeCenterY = nodeY + nodeHeight / 2;
        
        // 检查节点中心点是否在容器内（考虑header高度）
        double headerHeight = 36; // 估算header高度
        double contentY = containerY + headerHeight;
        double contentHeight = containerHeight - headerHeight;
        
        return nodeCenterX >= containerX && 
               nodeCenterX <= containerX + containerWidth &&
               nodeCenterY >= contentY && 
               nodeCenterY <= contentY + contentHeight;
    }
    
    /**
     * ⭐ 新增：判断条件节点是否在指定容器内（重载方法）
     */
    private boolean isConditionNodeInContainer(ConditionNode conditionNode, ConditionNode container) {
        if (conditionNode == null || container == null || !container.isExpanded()) {
            return false;
        }
        
        // ⭐ 避免循环嵌套：不能将条件节点拖入自身或其子容器中
        if (conditionNode == container) {
            return false;
        }
        
        // 检查是否是其子容器
        if (isDescendantOf(conditionNode, container)) {
            return false;
        }
        
        double nodeX = conditionNode.getLayoutX();
        double nodeY = conditionNode.getLayoutY();
        double nodeWidth = conditionNode.getFrame().getWidth();
        double nodeHeight = conditionNode.getFrame().getHeight();
        
        double containerX = container.getLayoutX();
        double containerY = container.getLayoutY();
        double containerWidth = container.getFrame().getWidth();
        double containerHeight = container.getFrame().getHeight();
        
        // 计算条件节点中心点
        double nodeCenterX = nodeX + nodeWidth / 2;
        double nodeCenterY = nodeY + nodeHeight / 2;
        
        // 检查节点中心点是否在容器内（考虑header高度）
        double headerHeight = 36; // 估算header高度
        double contentY = containerY + headerHeight;
        double contentHeight = containerHeight - headerHeight;
        
        return nodeCenterX >= containerX && 
               nodeCenterX <= containerX + containerWidth &&
               nodeCenterY >= contentY && 
               nodeCenterY <= contentY + contentHeight;
    }
    
    /**
     * ⭐ 新增：检查conditionNode是否是container的子孙节点（避免循环嵌套）
     */
    private boolean isDescendantOf(ConditionNode conditionNode, ConditionNode container) {
        if (conditionNode == null || container == null) {
            return false;
        }
        
        // 递归检查container的所有嵌套条件节点
        for (ConditionNode nested : container.getManagedConditionNodes()) {
            if (nested == conditionNode) {
                return true;
            }
            // 递归检查嵌套的条件节点
            if (isDescendantOf(conditionNode, nested)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 将节点添加到条件节点容器
     */
    private void addNodeToConditionContainer(ProcessNode node, ConditionNode conditionNode) {
        if (node == null || conditionNode == null) return;
        
        // 检查节点是否已经在容器中
        if (conditionNode.getManagedCanvasNodes().contains(node)) {
            return;
        }
        
        // 将节点添加到容器的管理列表
        List<ProcessNode> managedNodes = new ArrayList<>(conditionNode.getManagedCanvasNodes());
        managedNodes.add(node);
        conditionNode.bindCanvasNodes(managedNodes);
        
        log("✓ 节点 " + node.getJobHandlerName() + " 已加入条件节点: " + conditionNode.getConditionName());
        markAsUnsaved();
        notifyNodeStructureChanged();
    }
    
    /**
     * 从条件节点容器中移除节点
     */
    private void removeNodeFromConditionContainer(ProcessNode node, ConditionNode conditionNode) {
        if (node == null || conditionNode == null) return;
        
        List<ProcessNode> managedNodes = new ArrayList<>(conditionNode.getManagedCanvasNodes());
        managedNodes.remove(node);
        conditionNode.bindCanvasNodes(managedNodes);
        
        log("✓ 节点 " + node.getJobHandlerName() + " 已从条件节点移除: " + conditionNode.getConditionName());
        markAsUnsaved();
        notifyNodeStructureChanged();
    }
    
    /**
     * ⭐ 新增：将条件节点添加到条件节点容器
     */
    private void addConditionNodeToConditionContainer(ConditionNode conditionNode, ConditionNode container) {
        if (conditionNode == null || container == null) return;
        
        // 检查条件节点是否已经在容器中
        if (container.getManagedConditionNodes().contains(conditionNode)) {
            return;
        }
        
        // ⭐ 避免循环嵌套：不能将条件节点拖入自身或其子容器中
        if (conditionNode == container) {
            return;
        }
        
        if (isDescendantOf(conditionNode, container)) {
            return;
        }
        
        // 如果条件节点已经在其他容器中，先从原容器中移除
        ConditionNode oldContainer = findConditionNodeContainingConditionNode(conditionNode, conditionNodes);
        if (oldContainer != null && oldContainer != container) {
            removeConditionNodeFromConditionContainer(conditionNode, oldContainer);
        }
        
        // 将条件节点添加到容器的管理列表
        List<ConditionNode> managedConditionNodes = new ArrayList<>(container.getManagedConditionNodes());
        managedConditionNodes.add(conditionNode);
        container.bindConditionNodes(managedConditionNodes);
        
        log("✓ 条件节点 " + conditionNode.getConditionName() + " 已加入条件节点: " + container.getConditionName());
        markAsUnsaved();
        notifyNodeStructureChanged();
    }
    
    /**
     * ⭐ 新增：从条件节点容器中移除条件节点
     */
    private void removeConditionNodeFromConditionContainer(ConditionNode conditionNode, ConditionNode container) {
        if (conditionNode == null || container == null) return;
        
        List<ConditionNode> managedConditionNodes = new ArrayList<>(container.getManagedConditionNodes());
        managedConditionNodes.remove(conditionNode);
        container.bindConditionNodes(managedConditionNodes);
        
        log("✓ 条件节点 " + conditionNode.getConditionName() + " 已从条件节点移除: " + container.getConditionName());
        markAsUnsaved();
        notifyNodeStructureChanged();
    }
    
    /**
     * ⭐ 新增：检测条件节点是否进入条件节点容器，如果是则自动加入容器
     * 支持递归检查嵌套的条件节点
     */
    private void checkConditionNodeInConditionContainer(ConditionNode conditionNode) {
        if (conditionNode == null) return;
        
        // 递归检查所有条件节点（包括嵌套的），查找包含该条件节点的容器
        ConditionNode foundContainer = findConditionNodeContainingConditionNode(conditionNode, conditionNodes);
        if (foundContainer != null) {
            // 条件节点已经在某个容器中，检查是否还在容器内
            if (!isConditionNodeInContainer(conditionNode, foundContainer)) {
                // 条件节点移出了容器，从容器中移除
                removeConditionNodeFromConditionContainer(conditionNode, foundContainer);
            } else {
                // 仍在容器内，不需要处理
                return;
            }
        }
        
        // 检查条件节点是否进入任何条件节点容器（包括嵌套的）
        ConditionNode targetContainer = findConditionNodeForConditionNode(conditionNode, conditionNodes);
        if (targetContainer != null && targetContainer.isExpanded()) {
            // 条件节点进入容器，添加到容器中
            addConditionNodeToConditionContainer(conditionNode, targetContainer);
        }
    }
    
    /**
     * ⭐ 新增：递归查找包含指定条件节点的条件节点容器
     */
    private ConditionNode findConditionNodeContainingConditionNode(ConditionNode conditionNode, List<ConditionNode> conditionNodes) {
        for (ConditionNode container : conditionNodes) {
            // 检查当前条件节点是否包含该条件节点
            if (container.getManagedConditionNodes().contains(conditionNode)) {
                return container;
            }
            // 递归检查嵌套的条件节点
            ConditionNode nested = findConditionNodeContainingConditionNode(conditionNode, container.getManagedConditionNodes());
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
    
    /**
     * ⭐ 新增：递归查找条件节点应该加入的条件节点容器 - 返回最内层容器
     */
    private ConditionNode findConditionNodeForConditionNode(ConditionNode conditionNode, List<ConditionNode> conditionNodes) {
        // 优先检查嵌套的条件节点（最内层），如果找到就直接返回
        for (ConditionNode container : conditionNodes) {
            if (container.isExpanded() && container != conditionNode) {
                // 先递归检查嵌套的条件节点
                ConditionNode nested = findConditionNodeForConditionNode(conditionNode, container.getManagedConditionNodes());
                if (nested != null) {
                    return nested; // 返回最内层的容器
                }
                // 如果嵌套容器中没有找到，再检查当前容器
                if (isConditionNodeInContainer(conditionNode, container)) {
                    return container;
                }
            }
        }
        return null;
    }
    
    /**
     * ⭐ 新增：递归查找所有包含指定节点的条件节点容器（从内到外）
     */
    private List<ConditionNode> findAllContainersContaining(ProcessNode node, List<ConditionNode> conditionNodes) {
        List<ConditionNode> containers = new ArrayList<>();
        for (ConditionNode conditionNode : conditionNodes) {
            // 先递归检查嵌套的条件节点（最内层）
            List<ConditionNode> nestedContainers = findAllContainersContaining(node, conditionNode.getManagedConditionNodes());
            if (!nestedContainers.isEmpty()) {
                // 如果找到嵌套容器，先添加嵌套容器（最内层）
                containers.addAll(nestedContainers);
            }
            // 检查当前条件节点是否包含该节点
            if (conditionNode.getManagedCanvasNodes().contains(node)) {
                containers.add(conditionNode);
            }
        }
        return containers;
    }
    
    /**
     * 从容器中移除节点（公共方法）- ⭐ 修复：支持递归移出到最外层
     */
    public void removeNodeFromContainer(ProcessNode node) {
        if (node == null) return;
        
        // ⭐ 修复：递归查找所有包含该节点的条件节点容器（从内到外）
        List<ConditionNode> allContainers = findAllContainersContaining(node, conditionNodes);
        if (!allContainers.isEmpty()) {
            // 从最内层到最外层依次移出
            for (ConditionNode container : allContainers) {
                removeNodeFromConditionContainer(node, container);
            }
            return;
        }
        
        // 检查是否在任务组容器中
        for (GroupContainer container : groupContainers) {
            if (container.getManagedCanvasNodes().contains(node)) {
                List<ProcessNode> managedNodes = new ArrayList<>(container.getManagedCanvasNodes());
                managedNodes.remove(node);
                container.bindCanvasNodes(managedNodes);
                log("✓ 节点 " + node.getJobHandlerName() + " 已从任务组移除");
                markAsUnsaved();
                notifyNodeStructureChanged();
                return;
            }
        }
    }
    
    /**
     * 检查节点是否在容器内
     */
    public boolean isNodeInAnyContainer(ProcessNode node) {
        if (node == null) return false;
        
        // 检查条件节点容器
        ConditionNode conditionContainer = findConditionNodeContaining(node, conditionNodes);
        if (conditionContainer != null) {
            return true;
        }
        
        // 检查任务组容器
        for (GroupContainer container : groupContainers) {
            if (container.getManagedCanvasNodes().contains(node)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * ⭐ 新增：检查条件节点是否在容器内
     */
    public boolean isConditionNodeInAnyContainer(ConditionNode conditionNode) {
        if (conditionNode == null) return false;
        
        // 检查条件节点容器
        ConditionNode conditionContainer = findConditionNodeContainingConditionNode(conditionNode, conditionNodes);
        if (conditionContainer != null) {
            return true;
        }
        
        return false;
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
        
        // ⭐ 修复：如果节点在条件节点容器中，不需要检查画布扩展
        // 因为节点在contentLayer中时，坐标是相对坐标，不能用于画布扩展检查
        ConditionNode container = findConditionNodeContaining(node, conditionNodes);
        if (container != null) {
            // 节点在容器中，不需要检查画布扩展
            return;
        }
        
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
    
    /** 一次粘贴产生的节点与连接，撤销时整体移除，重做时整体恢复 */
    private class PasteAction implements CanvasAction {
        private final List<ProcessNode> nodes;
        private final List<NodeConnection> connections;
        
        PasteAction(List<ProcessNode> nodes, List<NodeConnection> connections) {
            this.nodes = new ArrayList<>(nodes != null ? nodes : Collections.emptyList());
            this.connections = new ArrayList<>(connections != null ? connections : Collections.emptyList());
        }
        
        @Override
        public void undo() {
            for (ProcessNode node : nodes) {
                removeNode(node, false);
            }
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            for (ProcessNode node : nodes) {
                addNode(node, false);
            }
            for (NodeConnection conn : connections) {
                connectionManager.addConnectionInternal(conn);
            }
            notifyNodeStructureChanged();
        }
    }
    
    /** 批量删除（剪切/Delete/批量编辑删除），撤销时整体恢复，重做时整体移除 */
    private class BatchRemoveAction implements CanvasAction {
        private final List<ProcessNode> nodes;
        private final double[] xPositions;
        private final double[] yPositions;
        private final List<NodeConnection> attachedConnections;
        
        BatchRemoveAction(List<ProcessNode> nodes, double[] xPositions, double[] yPositions, List<NodeConnection> attachedConnections) {
            this.nodes = new ArrayList<>(nodes != null ? nodes : Collections.emptyList());
            this.xPositions = xPositions != null ? xPositions.clone() : new double[0];
            this.yPositions = yPositions != null ? yPositions.clone() : new double[0];
            this.attachedConnections = new ArrayList<>(attachedConnections != null ? attachedConnections : Collections.emptyList());
        }
        
        @Override
        public void undo() {
            for (int i = 0; i < nodes.size(); i++) {
                ProcessNode node = nodes.get(i);
                addNode(node, false);
                if (i < xPositions.length && i < yPositions.length) {
                    node.setLayoutX(xPositions[i]);
                    node.setLayoutY(yPositions[i]);
                }
            }
            for (NodeConnection conn : attachedConnections) {
                connectionManager.addConnectionInternal(conn);
            }
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            for (ProcessNode node : nodes) {
                removeNode(node, false);
            }
            notifyNodeStructureChanged();
        }
    }
    
    /** 布局/批量位置变更：保存变更前后位置，撤销恢复旧位置，重做恢复新位置 */
    private class LayoutAction implements CanvasAction {
        private final Map<ProcessNode, double[]> oldPositions;
        private final Map<ProcessNode, double[]> newPositions;
        
        LayoutAction(Map<ProcessNode, double[]> oldPositions, Map<ProcessNode, double[]> newPositions) {
            this.oldPositions = new HashMap<>(oldPositions != null ? oldPositions : Collections.emptyMap());
            this.newPositions = new HashMap<>(newPositions != null ? newPositions : Collections.emptyMap());
        }
        
        @Override
        public void undo() {
            for (Map.Entry<ProcessNode, double[]> e : oldPositions.entrySet()) {
                ProcessNode node = e.getKey();
                double[] xy = e.getValue();
                if (node != null && xy != null && xy.length >= 2) {
                    node.setLayoutX(xy[0]);
                    node.setLayoutY(xy[1]);
                }
            }
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            for (Map.Entry<ProcessNode, double[]> e : newPositions.entrySet()) {
                ProcessNode node = e.getKey();
                double[] xy = e.getValue();
                if (node != null && xy != null && xy.length >= 2) {
                    node.setLayoutX(xy[0]);
                    node.setLayoutY(xy[1]);
                }
            }
            notifyNodeStructureChanged();
        }
    }
    
    /** 等距分布/对齐到中心等批量移动，与 LayoutAction 相同结构 */
    private class BatchMoveAction implements CanvasAction {
        private final Map<ProcessNode, double[]> oldPositions;
        private final Map<ProcessNode, double[]> newPositions;
        
        BatchMoveAction(Map<ProcessNode, double[]> oldPositions, Map<ProcessNode, double[]> newPositions) {
            this.oldPositions = new HashMap<>(oldPositions != null ? oldPositions : Collections.emptyMap());
            this.newPositions = new HashMap<>(newPositions != null ? newPositions : Collections.emptyMap());
        }
        
        @Override
        public void undo() {
            for (Map.Entry<ProcessNode, double[]> e : oldPositions.entrySet()) {
                ProcessNode node = e.getKey();
                double[] xy = e.getValue();
                if (node != null && xy != null && xy.length >= 2) {
                    node.setLayoutX(xy[0]);
                    node.setLayoutY(xy[1]);
                }
            }
            notifyNodeStructureChanged();
        }
        
        @Override
        public void redo() {
            for (Map.Entry<ProcessNode, double[]> e : newPositions.entrySet()) {
                ProcessNode node = e.getKey();
                double[] xy = e.getValue();
                if (node != null && xy != null && xy.length >= 2) {
                    node.setLayoutX(xy[0]);
                    node.setLayoutY(xy[1]);
                }
            }
            notifyNodeStructureChanged();
        }
    }
    
    // ==================== 智能对齐相关方法 ====================
    
    /**
     * 显示对齐参考线
     */
    private void showAlignmentGuides(ProcessNode draggedNode) {
        hideAlignmentGuides(); // 先清除旧的参考线
        
        if (draggedNode == null) return;
        
        double nodeX = draggedNode.getLayoutX();
        double nodeY = draggedNode.getLayoutY();
        double nodeWidth = draggedNode.getPrefWidth();
        double nodeHeight = draggedNode.getPrefHeight();
        
        double nodeCenterX = nodeX + nodeWidth / 2;
        double nodeCenterY = nodeY + nodeHeight / 2;
        double nodeLeft = nodeX;
        double nodeRight = nodeX + nodeWidth;
        double nodeTop = nodeY;
        double nodeBottom = nodeY + nodeHeight;
        
        // 检查与其他节点的对齐
        for (ProcessNode otherNode : nodes) {
            if (otherNode == draggedNode) continue;
            
            double otherX = otherNode.getLayoutX();
            double otherY = otherNode.getLayoutY();
            double otherWidth = otherNode.getPrefWidth();
            double otherHeight = otherNode.getPrefHeight();
            
            double otherCenterX = otherX + otherWidth / 2;
            double otherCenterY = otherY + otherHeight / 2;
            double otherLeft = otherX;
            double otherRight = otherX + otherWidth;
            double otherTop = otherY;
            double otherBottom = otherY + otherHeight;
            
            // 检查水平对齐（中心、顶部、底部）
            if (Math.abs(nodeCenterY - otherCenterY) < ALIGNMENT_THRESHOLD) {
                createHorizontalGuideLine(otherCenterY);
                draggedNode.setLayoutY(otherCenterY - nodeHeight / 2);
            } else if (Math.abs(nodeTop - otherTop) < ALIGNMENT_THRESHOLD) {
                createHorizontalGuideLine(otherTop);
                draggedNode.setLayoutY(otherTop);
            } else if (Math.abs(nodeBottom - otherBottom) < ALIGNMENT_THRESHOLD) {
                createHorizontalGuideLine(otherBottom);
                draggedNode.setLayoutY(otherBottom - nodeHeight);
            }
            
            // 检查垂直对齐（中心、左侧、右侧）
            if (Math.abs(nodeCenterX - otherCenterX) < ALIGNMENT_THRESHOLD) {
                createVerticalGuideLine(otherCenterX);
                draggedNode.setLayoutX(otherCenterX - nodeWidth / 2);
            } else if (Math.abs(nodeLeft - otherLeft) < ALIGNMENT_THRESHOLD) {
                createVerticalGuideLine(otherLeft);
                draggedNode.setLayoutX(otherLeft);
            } else if (Math.abs(nodeRight - otherRight) < ALIGNMENT_THRESHOLD) {
                createVerticalGuideLine(otherRight);
                draggedNode.setLayoutX(otherRight - nodeWidth);
            }
        }
    }
    
    /**
     * 创建水平对齐参考线
     */
    private void createHorizontalGuideLine(double y) {
        Line guideLine = new Line(0, y, getPrefWidth(), y);
        guideLine.setStroke(Color.web("#3B82F6"));
        guideLine.setStrokeWidth(1);
        guideLine.getStrokeDashArray().addAll(5.0, 5.0);
        guideLine.setMouseTransparent(true);
        guideLine.toBack();
        alignmentGuideLines.add(guideLine);
        this.getChildren().add(guideLine);
    }
    
    /**
     * 创建垂直对齐参考线
     */
    private void createVerticalGuideLine(double x) {
        Line guideLine = new Line(x, 0, x, getPrefHeight());
        guideLine.setStroke(Color.web("#3B82F6"));
        guideLine.setStrokeWidth(1);
        guideLine.getStrokeDashArray().addAll(5.0, 5.0);
        guideLine.setMouseTransparent(true);
        guideLine.toBack();
        alignmentGuideLines.add(guideLine);
        this.getChildren().add(guideLine);
    }
    
    /**
     * 隐藏对齐参考线
     */
    private void hideAlignmentGuides() {
        for (Line guideLine : alignmentGuideLines) {
            this.getChildren().remove(guideLine);
        }
        alignmentGuideLines.clear();
    }
    
    /**
     * 网格吸附：将节点对齐到网格点
     */
    private void snapNodeToGrid(ProcessNode node) {
        if (node == null) return;
        
        double x = node.getLayoutX();
        double y = node.getLayoutY();
        
        // 对齐到最近的网格点
        double snappedX = Math.round(x / gridSnapSize) * gridSnapSize;
        double snappedY = Math.round(y / gridSnapSize) * gridSnapSize;
        
        node.setLayoutX(Math.max(0, snappedX));
        node.setLayoutY(Math.max(0, snappedY));
    }
    
    /**
     * 等距分布（水平），作为一步撤销入栈
     */
    public void distributeNodesHorizontally() {
        Set<ProcessNode> selectedNodes = selectionManager.getSelectedNodes();
        if (selectedNodes.size() < 3) {
            log("⚠ 需要至少选中 3 个节点才能等距分布");
            return;
        }
        
        Map<ProcessNode, double[]> oldPositions = snapshotNodePositions(selectedNodes);
        List<ProcessNode> sortedNodes = new ArrayList<>(selectedNodes);
        sortedNodes.sort((a, b) -> Double.compare(a.getLayoutX(), b.getLayoutX()));
        
        double minX = sortedNodes.get(0).getLayoutX();
        double maxX = sortedNodes.get(sortedNodes.size() - 1).getLayoutX();
        double spacing = (maxX - minX) / (sortedNodes.size() - 1);
        
        for (int i = 0; i < sortedNodes.size(); i++) {
            sortedNodes.get(i).setLayoutX(minX + i * spacing);
        }
        
        Map<ProcessNode, double[]> newPositions = snapshotNodePositions(selectedNodes);
        pushAction(new BatchMoveAction(oldPositions, newPositions));
        markAsUnsaved();
        log("✓ 水平等距分布完成: " + sortedNodes.size() + " 个节点");
    }
    
    /**
     * 等距分布（垂直），作为一步撤销入栈
     */
    public void distributeNodesVertically() {
        Set<ProcessNode> selectedNodes = selectionManager.getSelectedNodes();
        if (selectedNodes.size() < 3) {
            log("⚠ 需要至少选中 3 个节点才能等距分布");
            return;
        }
        
        Map<ProcessNode, double[]> oldPositions = snapshotNodePositions(selectedNodes);
        List<ProcessNode> sortedNodes = new ArrayList<>(selectedNodes);
        sortedNodes.sort((a, b) -> Double.compare(a.getLayoutY(), b.getLayoutY()));
        
        double minY = sortedNodes.get(0).getLayoutY();
        double maxY = sortedNodes.get(sortedNodes.size() - 1).getLayoutY();
        double spacing = (maxY - minY) / (sortedNodes.size() - 1);
        
        for (int i = 0; i < sortedNodes.size(); i++) {
            sortedNodes.get(i).setLayoutY(minY + i * spacing);
        }
        
        Map<ProcessNode, double[]> newPositions = snapshotNodePositions(selectedNodes);
        pushAction(new BatchMoveAction(oldPositions, newPositions));
        markAsUnsaved();
        log("✓ 垂直等距分布完成: " + sortedNodes.size() + " 个节点");
    }
    
    /**
     * 对齐到画布中心，作为一步撤销入栈
     */
    public void alignToCanvasCenter() {
        Set<ProcessNode> selectedNodes = selectionManager.getSelectedNodes();
        if (selectedNodes.isEmpty()) {
            log("⚠ 请先选中节点");
            return;
        }
        
        Map<ProcessNode, double[]> oldPositions = snapshotNodePositions(selectedNodes);
        double canvasCenterX = getPrefWidth() / 2;
        double canvasCenterY = getPrefHeight() / 2;
        
        // 计算选中节点的中心
        double nodesCenterX = 0;
        double nodesCenterY = 0;
        for (ProcessNode node : selectedNodes) {
            nodesCenterX += node.getLayoutX() + node.getPrefWidth() / 2;
            nodesCenterY += node.getLayoutY() + node.getPrefHeight() / 2;
        }
        nodesCenterX /= selectedNodes.size();
        nodesCenterY /= selectedNodes.size();
        
        // 计算偏移量
        double deltaX = canvasCenterX - nodesCenterX;
        double deltaY = canvasCenterY - nodesCenterY;
        
        // 移动所有选中节点
        for (ProcessNode node : selectedNodes) {
            node.setLayoutX(Math.max(0, node.getLayoutX() + deltaX));
            node.setLayoutY(Math.max(0, node.getLayoutY() + deltaY));
        }
        
        Map<ProcessNode, double[]> newPositions = snapshotNodePositions(selectedNodes);
        pushAction(new BatchMoveAction(oldPositions, newPositions));
        markAsUnsaved();
        log("✓ 已对齐到画布中心: " + selectedNodes.size() + " 个节点");
    }
    
    /**
     * 启用/禁用智能对齐
     */
    public void setSmartAlignmentEnabled(boolean enabled) {
        smartAlignmentEnabled = enabled;
        if (!enabled) {
            hideAlignmentGuides();
        }
    }
    
    /**
     * 启用/禁用网格吸附
     */
    public void setSnapToGridEnabled(boolean enabled) {
        snapToGridEnabled = enabled;
        if (onSnapToGridChanged != null) onSnapToGridChanged.accept(enabled);
    }
    
    /**
     * 获取网格吸附是否启用
     */
    public boolean isSnapToGridEnabled() {
        return snapToGridEnabled;
    }
    
    /**
     * 设置网格吸附大小
     */
    public void setGridSnapSize(double size) {
        gridSnapSize = Math.max(5, size);
    }
}


