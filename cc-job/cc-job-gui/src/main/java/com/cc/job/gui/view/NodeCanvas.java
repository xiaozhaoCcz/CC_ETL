package com.cc.job.gui.view;

import com.cc.job.gui.history.CanvasAction;
import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.model.GroupContainer;
import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.service.JobPartService;
import com.cc.job.gui.util.NodeStatusSyncManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/**
 * 画布，用于管理节点和连接线
 */
public class NodeCanvas extends Pane {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeCanvas.class);

    private static final String NODE_LISTENER_KEY = "nodeCanvasListenersAttached";
    private static final double TOP_DRAG_MARGIN = 80.0;
    private static final double LEFT_DRAG_MARGIN = 80.0;
    private static final double AUTO_SCROLL_MARGIN = 120.0;

    private List<ProcessNode> nodes = new ArrayList<>();
    private List<GroupContainer> groupContainers = new ArrayList<>();
    private List<NodeConnection> connections = new ArrayList<>();
    private UndoRedoManager undoRedoManager;
    private boolean historyEnabled = true;

    // 页面级右键菜单回调
    private Runnable onRequestAddNode;
    private Runnable onRequestSelectTaskGroup;
    private Runnable onRequestRunTaskGroup;
    private Runnable onRequestClearCanvas;
    
    // 任务组容器删除回调
    private java.util.function.Consumer<GroupContainer> onDeleteGroupContainer;

    // 临时连线相关
    private Object startOwner; // ProcessNode 或 GroupContainer
    private Line tempLine;
    
    // 框选相关
    private boolean selectionMode = false;
    private javafx.scene.shape.Rectangle selectionRect; // 框选时的临时矩形（蓝色，拖动时显示）
    private javafx.scene.shape.Rectangle selectionBoundingBox; // 选中节点的包围框（红色，框选完成后显示）
    private double selectionStartX;
    private double selectionStartY;
    private java.util.Set<ProcessNode> selectedNodes = new java.util.HashSet<>();
    private java.util.Set<NodeConnection> selectedConnections = new java.util.HashSet<>();
    
    // 批量移动相关
    private boolean isMovingSelection = false;
    private ProcessNode dragStartNode; // 开始拖拽的节点
    private java.util.Map<ProcessNode, double[]> selectionOriginalPositions = new java.util.HashMap<>(); // 选中节点的原始位置
    
    // 日志回调
    private LogCallback logCallback;
    private Runnable onNodeMoved; // 节点移动回调
    private java.util.function.Consumer<Boolean> onSelectionModeChanged; // 框选模式改变回调

    private final Map<ProcessNode, double[]> autoShiftOriginalPositions = new HashMap<>();
    private ProcessNode currentDraggingNode;
    private double autoShiftApplied = 0.0;
    private double autoShiftAppliedX = 0.0;
    private ScrollPane hostingScrollPane;
    private double leftExpansionApplied = 0.0; // 记录左侧已扩展的宽度
    private boolean hasExpandedInThisDrag = false; // 标记本次拖拽是否已经扩展过

    public interface LogCallback {
        void log(String message);
    }
    
    public NodeCanvas() {
        // 设置初始尺寸
        setPrefSize(2000, 1000);
        //setStyle("-fx-background-color: #F3F4F6;");
        setStyle("-fx-background-color: gray;");
        
        // 初始化框选矩形（临时框选时显示）
        selectionRect = new javafx.scene.shape.Rectangle();
        selectionRect.setFill(Color.web("#2563EB", 0.1)); // 半透明蓝色填充
        selectionRect.setStroke(Color.web("#2563EB")); // 蓝色边框
        selectionRect.setStrokeWidth(2);
        selectionRect.getStrokeDashArray().addAll(5.0, 5.0);
        selectionRect.setVisible(false);
        selectionRect.setMouseTransparent(true); // 不拦截鼠标事件
        this.getChildren().add(selectionRect);
        
        // 初始化选中节点的红色包围框（框选完成后显示）
        selectionBoundingBox = new javafx.scene.shape.Rectangle();
        selectionBoundingBox.setFill(Color.TRANSPARENT); // 无填充
        selectionBoundingBox.setStroke(Color.web("#EF4444")); // 红色边框（类似图片中的红框）
        selectionBoundingBox.setStrokeWidth(2);
        selectionBoundingBox.getStrokeDashArray().addAll(8.0, 4.0); // 虚线样式
        selectionBoundingBox.setVisible(false);
        selectionBoundingBox.setMouseTransparent(true); // 不拦截鼠标事件
        this.getChildren().add(selectionBoundingBox);
        
        // 设置画布鼠标事件处理（用于框选）
        setupSelectionHandlers();
        
        // 设置页面级右键菜单（空白区域）
        setupCanvasContextMenu();
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
    
    /**
     * 设置框选模式改变回调
     * @param callback 回调函数，参数为新的框选模式状态
     */
    public void setOnSelectionModeChanged(java.util.function.Consumer<Boolean> callback) {
        this.onSelectionModeChanged = callback;
    }

    public void setScrollPane(ScrollPane scrollPane) {
        this.hostingScrollPane = scrollPane;
    }
    
    // 页面级右键菜单回调 setter
    public void setOnRequestAddNode(Runnable runnable) {
        this.onRequestAddNode = runnable;
    }
    public void setOnRequestSelectTaskGroup(Runnable runnable) {
        this.onRequestSelectTaskGroup = runnable;
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
    
    public List<ProcessNode> getNodes() {
        return nodes;
    }
    
    public List<NodeConnection> getConnections() {
        return connections;
    }

    public boolean removeConnectionByEdgeId(String edgeId) {
        if (edgeId == null) {
            return false;
        }
        for (NodeConnection connection : new ArrayList<>(connections)) {
            if (edgeIdMatches(edgeId, connection.getEdgeId())) {
                removeConnection(connection);
                return true;
            }
        }
        return false;
    }

    public boolean locateConnectionByEdgeId(String edgeId) {
        if (edgeId == null) {
            return false;
        }
        for (NodeConnection connection : connections) {
            if (edgeIdMatches(edgeId, connection.getEdgeId())) {
                connection.toFront();
                connection.setSelected(true);
                connection.playLocateAnimation();
                PauseTransition delay = new PauseTransition(Duration.seconds(1.2));
                delay.setOnFinished(e -> connection.setSelected(false));
                delay.play();
                // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
                String sourceName = getOwnerName(connection.getSourceOwner());
                String targetName = getOwnerName(connection.getTargetOwner());
                log("📍 定位连接: " + sourceName + " → " + targetName);
                return true;
            }
        }
        return false;
    }

    public void setUndoRedoManager(UndoRedoManager undoRedoManager) {
        this.undoRedoManager = undoRedoManager;
    }

    private void pushAction(CanvasAction action) {
        if (undoRedoManager != null && historyEnabled && action != null) {
            undoRedoManager.push(action);
        }
    }

    private void notifyNodeStructureChanged() {
        updateCanvasSize();
        if (onNodeMoved != null) {
            onNodeMoved.run();
        }
    }
    
    public List<GroupContainer> getGroupContainers() {
        return new ArrayList<>(groupContainers);
    }
    
    /**
     * 将任务组容器添加到集合中（当容器已经添加到UI时使用）
     * @param container 要添加到集合的任务组容器
     */
    public void addGroupContainerToCollection(GroupContainer container) {
        if (container == null) {
            return;
        }
        
        // 检查容器是否已经存在
        if (!groupContainers.contains(container)) {
            groupContainers.add(container);
        }
    }
    
    /**
     * 移除任务组容器
     */
    public void removeGroupContainer(GroupContainer container) {
        removeGroupContainer(container, true);
    }
    
    public void removeGroupContainer(GroupContainer container, boolean recordHistory) {
        if (container == null) {
            return;
        }
        
        // ⭐ 修复：移除所有与容器相关的连接线（包括连接到容器本身的连线）
        List<NodeConnection> attachedConnections = new ArrayList<>();
        for (NodeConnection conn : new ArrayList<>(connections)) {
            boolean isAttached = false;
            
            // 1. 检查连接是否连接到容器本身
            if (conn.getSourceOwner() == container || conn.getTargetOwner() == container) {
                isAttached = true;
            }
            
            // 2. 检查连接是否与容器管理的节点相关
            if (!isAttached) {
                // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
                javafx.scene.Node sourceOwner = conn.getSourceOwner();
                javafx.scene.Node targetOwner = conn.getTargetOwner();
                
                for (ProcessNode managedNode : container.getManagedCanvasNodes()) {
                    if ((sourceOwner instanceof ProcessNode && sourceOwner == managedNode) ||
                        (targetOwner instanceof ProcessNode && targetOwner == managedNode)) {
                        isAttached = true;
                        break;
                    }
                }
            }
            
            if (isAttached) {
                attachedConnections.add(conn);
                removeConnection(conn, false);
            }
        }
        
        // 移除容器管理的所有节点（这些节点已经在画布上）
        for (ProcessNode managedNode : container.getManagedCanvasNodes()) {
            removeNode(managedNode, false);
        }
        
        // 从列表中移除容器
        groupContainers.remove(container);
        this.getChildren().remove(container);
        
        log("✓ 删除任务组容器: " + container.getGroupName());
        notifyNodeStructureChanged();
        
        if (recordHistory) {
            // TODO: 如果需要支持撤销/重做，可以在这里添加历史记录
        }
    }

    private void runWithoutHistory(Runnable runnable) {
        boolean previous = historyEnabled;
        historyEnabled = false;
        try {
            runnable.run();
        } finally {
            historyEnabled = previous;
        }
    }
    
    private void log(String message) {
        if (logCallback != null) {
            logCallback.log(message);
        }
    }

    private void beginAutoShiftSession(ProcessNode node) {
        currentDraggingNode = node;
        autoShiftOriginalPositions.clear();
        autoShiftApplied = 0.0;
        autoShiftAppliedX = 0.0;
        leftExpansionApplied = 0.0; // 重置扩展记录
        hasExpandedInThisDrag = false; // 重置扩展标记
    }

    private void endAutoShiftSession() {
        currentDraggingNode = null;
        autoShiftOriginalPositions.clear();
        autoShiftApplied = 0.0;
        autoShiftAppliedX = 0.0;
        leftExpansionApplied = 0.0; // 重置扩展记录
        hasExpandedInThisDrag = false; // 重置扩展标记
    }

    private Point2D adjustNodePositionOnDrag(ProcessNode node, double proposedX, double proposedY) {
        double adjustedX = Math.max(0, proposedX);
        double adjustedY = Math.max(0, proposedY);

//        if (currentDraggingNode == node) {
//            if (adjustedY < TOP_DRAG_MARGIN) {
//                double requiredShift = TOP_DRAG_MARGIN - adjustedY;
//                double incrementalShift = requiredShift - autoShiftApplied;
//                if (incrementalShift > 0) {
//                    shiftOtherNodesVertically(node, incrementalShift);
//                    autoShiftApplied += incrementalShift;
//                }
//                adjustedY = TOP_DRAG_MARGIN;
//            } else if (autoShiftApplied > 0) {
//                double release = Math.min(autoShiftApplied, adjustedY - TOP_DRAG_MARGIN);
//                if (release > 0) {
//                    shiftOtherNodesVertically(node, -release);
//                    autoShiftApplied -= release;
//                }
//            }
//
//            if (adjustedX < LEFT_DRAG_MARGIN) {
//                double requiredShiftX = LEFT_DRAG_MARGIN - adjustedX;
//                double incrementalShiftX = requiredShiftX - autoShiftAppliedX;
//                if (incrementalShiftX > 0) {
//                    shiftOtherNodesHorizontally(node, incrementalShiftX);
//                    autoShiftAppliedX += incrementalShiftX;
//                }
//                adjustedX = LEFT_DRAG_MARGIN;
//            } else if (autoShiftAppliedX > 0) {
//                double releaseX = Math.min(autoShiftAppliedX, adjustedX - LEFT_DRAG_MARGIN);
//                if (releaseX > 0) {
//                    shiftOtherNodesHorizontally(node, -releaseX);
//                    autoShiftAppliedX -= releaseX;
//                }
//            }
//        }

        return new Point2D(adjustedX, adjustedY);
    }

    private void shiftOtherNodesVertically(ProcessNode sourceNode, double delta) {
        if (Math.abs(delta) < 1e-3) {
            return;
        }

        for (ProcessNode node : nodes) {
            if (node == sourceNode) {
                continue;
            }
            recordOriginalPosition(node);
            node.setLayoutY(node.getLayoutY() + delta);
        }

        notifyNodeStructureChanged();
    }

    private void recordOriginalPosition(ProcessNode node) {
        autoShiftOriginalPositions.computeIfAbsent(node,
                key -> new double[]{node.getLayoutX(), node.getLayoutY()});
    }

    private void shiftOtherNodesHorizontally(ProcessNode sourceNode, double delta) {
        if (Math.abs(delta) < 1e-3) {
            return;
        }

        for (ProcessNode node : nodes) {
            if (node == sourceNode) {
                continue;
            }
            recordOriginalPosition(node);
            node.setLayoutX(node.getLayoutX() + delta);
        }

        notifyNodeStructureChanged();
    }

    private void handleNodePositionChanged(ProcessNode node) {
        if (node == null || node != currentDraggingNode) {
            return;
        }
        autoScrollIfNeeded(node);
    }

    private void autoScrollIfNeeded(ProcessNode node) {
        if (hostingScrollPane == null) {
            return;
        }

        // ========== 第一部分：画布扩展逻辑（检查节点是否靠近画布边缘）==========
        // 画布扩展：检查节点是否靠近画布的绝对边缘（画布坐标）
        double nodeLayoutX = node.getLayoutX();
        double contentWidth = getBoundsInLocal().getWidth();
        
        // 检查节点是否靠近画布左边缘（画布的绝对位置）
        if (nodeLayoutX < 80 && !hasExpandedInThisDrag) {
            double incrementalExpansion = 500; // 限制每次最多扩展500

            if (incrementalExpansion > 10) {
                // 页面进行扩展
                setPrefWidth(contentWidth + incrementalExpansion);

                // 调整节点位置和拖拽起始点
                node.setLayoutX(nodeLayoutX + incrementalExpansion);
                node.adjustDragStart(-incrementalExpansion, 0);

                // 其他节点也需要平移
                for (ProcessNode processNode : nodes) {
                    if (processNode != node) {
                        processNode.setLayoutX(processNode.getLayoutX() + incrementalExpansion);
                    }
                }

                // 更新已扩展的宽度和标记
                leftExpansionApplied += incrementalExpansion;
                hasExpandedInThisDrag = true;
                
                // 扩展后，重新获取信息用于后续的窗口跟随检查
                contentWidth = getBoundsInLocal().getWidth();
            }
        }

        // ========== 第二部分：窗口跟随逻辑（检查节点是否靠近可视区域边缘）==========
        // 窗口跟随：检查节点是否靠近或超出可视区域边缘（视口坐标）
        Bounds viewportBounds = hostingScrollPane.getViewportBounds();
        if (viewportBounds == null || viewportBounds.getWidth() <= 0 || viewportBounds.getHeight() <= 0) {
            return;
        }

        Bounds viewportInScene = hostingScrollPane.localToScene(viewportBounds);
        if (viewportInScene == null) {
            return;
        }

        Bounds viewportInCanvas = sceneToLocal(viewportInScene);
        if (viewportInCanvas == null) {
            return;
        }

        Bounds nodeBounds = node.getBoundsInParent();
        double contentHeight = getBoundsInLocal().getHeight();
        double viewportWidth = viewportInCanvas.getWidth();
        double viewportHeight = viewportInCanvas.getHeight();

        // 计算可视区域和节点的边界
        double viewportMinX = viewportInCanvas.getMinX();
        double viewportMaxX = viewportInCanvas.getMaxX();
        double viewportMinY = viewportInCanvas.getMinY();
        double viewportMaxY = viewportInCanvas.getMaxY();

        double nodeMinX = nodeBounds.getMinX();
        double nodeMaxX = nodeBounds.getMaxX();
        double nodeMinY = nodeBounds.getMinY();
        double nodeMaxY = nodeBounds.getMaxY();

        // 使用容差值避免浮点数精度问题
        final double EPSILON = 1.0;
        
        // 检查节点是否完全在可视区域内
        boolean nodeFullyVisibleX = (nodeMinX + EPSILON >= viewportMinX) && (nodeMaxX - EPSILON <= viewportMaxX);
        boolean nodeFullyVisibleY = (nodeMinY + EPSILON >= viewportMinY) && (nodeMaxY - EPSILON <= viewportMaxY);
        boolean nodeFullyVisible = nodeFullyVisibleX && nodeFullyVisibleY;

        // 如果节点完全在可视区域内，不进行滚动，直接返回
        if (nodeFullyVisible) {
            return;
        }

        // 只有当节点真正超出可视区域时，才计算新的滚动位置
        double contentMaxX = Math.max(contentWidth - viewportWidth, 0);
        double contentMaxY = Math.max(contentHeight - viewportHeight, 0);

        boolean needsVerticalScroll = false;
        boolean needsHorizontalScroll = false;
        
        double newViewportX = viewportMinX;
        double newViewportY = viewportMinY;

        // 垂直方向：只有当节点真正超出可视区域时才滚动
        if (nodeMinY < viewportMinY - EPSILON) {
            // 节点超出上边缘，向上滚动使节点可见
            needsVerticalScroll = true;
            newViewportY = Math.max(nodeMinY - AUTO_SCROLL_MARGIN, 0);
        } else if (nodeMaxY > viewportMaxY + EPSILON) {
            // 节点超出下边缘，向下滚动使节点可见
            needsVerticalScroll = true;
            newViewportY = Math.min(nodeMaxY + AUTO_SCROLL_MARGIN - viewportHeight, contentMaxY);
        }

        // 水平方向：只有当节点真正超出可视区域时才滚动
        if (nodeMinX < viewportMinX - EPSILON) {
            // 节点超出左边缘，向左滚动使节点可见
            needsHorizontalScroll = true;
            newViewportX = Math.max(nodeMinX - AUTO_SCROLL_MARGIN, 0);
        } else if (nodeMaxX > viewportMaxX + EPSILON) {
            // 节点超出右边缘，向右滚动使节点可见
            needsHorizontalScroll = true;
            newViewportX = Math.min(nodeMaxX + AUTO_SCROLL_MARGIN - viewportWidth, contentMaxX);
        }

        // 应用垂直滚动（只有在真正需要且位置改变时才更新）
        if (needsVerticalScroll && contentMaxY > 0 && Math.abs(newViewportY - viewportMinY) > 1e-3) {
            hostingScrollPane.setVvalue(clamp01(newViewportY / contentMaxY));
        }

        // 应用水平滚动（只有在真正需要且位置改变时才更新）
        if (needsHorizontalScroll && contentMaxX > 0 && Math.abs(newViewportX - viewportMinX) > 1e-3) {
            hostingScrollPane.setHvalue(clamp01(newViewportX / contentMaxX));
        }
    }

    private double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }
    
    /**
     * 添加节点
     */
    public void addNode(ProcessNode node) {
        addNode(node, true);
    }

    public void addNode(ProcessNode node, boolean recordHistory) {
        if (node == null) {
            return;
        }

        if (!nodes.contains(node)) {
            nodes.add(node);
        }
        if (!this.getChildren().contains(node)) {
            this.getChildren().add(node);
        }

        // 为节点的连接点设置事件处理器
        setupConnectorHandler(node, node.getTopConnector());
        setupConnectorHandler(node, node.getBottomConnector());
        setupConnectorHandler(node, node.getLeftConnector());
        setupConnectorHandler(node, node.getRightConnector());

        // 设置删除回调
        node.setOnDelete(() -> removeNode(node, true));

        // 设置拖动回调 - 实时更新小地图和批量移动
        node.setOnDragged(() -> {
            if (onNodeMoved != null) {
                onNodeMoved.run();
            }
            
            // 如果正在批量移动，同步移动其他选中的节点
            if (isMovingSelection && dragStartNode == node && selectedNodes.size() > 1) {
                double currentX = node.getLayoutX();
                double currentY = node.getLayoutY();
                // 使用拖拽开始时的原始位置计算偏移量，避免累积误差
                double[] dragStartOriginalPos = selectionOriginalPositions.get(node);
                if (dragStartOriginalPos != null) {
                    double deltaX = currentX - dragStartOriginalPos[0];
                    double deltaY = currentY - dragStartOriginalPos[1];
                    
                    // 移动所有其他选中的节点，保持相对位置
                    for (ProcessNode selectedNode : selectedNodes) {
                        if (selectedNode != node) {
                            double[] originalPos = selectionOriginalPositions.get(selectedNode);
                            if (originalPos != null) {
                                double newX = Math.max(0, originalPos[0] + deltaX);
                                double newY = Math.max(0, originalPos[1] + deltaY);
                                selectedNode.setLayoutX(newX);
                                selectedNode.setLayoutY(newY);
                            }
                        }
                    }
                    
                    // 更新包围框位置
                    updateSelectionBoundingBox();
                }
            }
        });

        node.setOnDragStarted(() -> {
            beginAutoShiftSession(node);
            // 如果该节点被选中，且还有其他选中的节点，则开始批量移动
            if (!selectedNodes.isEmpty() && selectedNodes.contains(node) && selectedNodes.size() > 1) {
                startSelectionMove(node);
            }
        });
        node.setPositionAdjuster((processNode, proposedX, proposedY) ->
                adjustNodePositionOnDrag(processNode, proposedX, proposedY));
        node.setOnPositionChanged(this::handleNodePositionChanged);

        // 设置节点点击回调（用于选中节点）
        node.setOnClicked(() -> {
            if (!isMovingSelection) {
                // 点击节点时选中它
                selectNode(node);
            }
        });
        
        // 拖拽结束后记录历史并结束批量移动
        node.setOnDragFinished((oldX, oldY, newX, newY) -> {
            // 如果正在批量移动，先结束批量移动并更新所有选中节点的原始位置
            if (isMovingSelection && dragStartNode == node) {
                endSelectionMove();
            }
            
            // 检查是否是点击（没有移动）还是拖拽
            double deltaX = Math.abs(newX - oldX);
            double deltaY = Math.abs(newY - oldY);
            boolean isClick = deltaX < 3 && deltaY < 3; // 移动距离小于3像素认为是点击
            
            // 然后记录历史（只有实际移动了才记录）
            if (undoRedoManager != null && historyEnabled && !isClick) {
                CanvasAction action;
                if (!autoShiftOriginalPositions.isEmpty()) {
                    Map<ProcessNode, NodePositionSnapshot> shiftedNodes = new HashMap<>();
                    for (Map.Entry<ProcessNode, double[]> entry : autoShiftOriginalPositions.entrySet()) {
                        ProcessNode shiftedNode = entry.getKey();
                        double[] original = entry.getValue();
                        shiftedNodes.put(
                            shiftedNode,
                            new NodePositionSnapshot(
                                original[0],
                                original[1],
                                shiftedNode.getLayoutX(),
                                shiftedNode.getLayoutY()
                            )
                        );
                    }
                    action = new MoveNodeGroupAction(node, oldX, oldY, newX, newY, shiftedNodes);
                } else {
                    action = new MoveNodeAction(node, oldX, oldY, newX, newY);
                }
                pushAction(action);
            }
            notifyNodeStructureChanged();
            endAutoShiftSession();
        });

        // 监听节点位置变化，动态调整画布大小（仅注册一次）
        if (!Boolean.TRUE.equals(node.getProperties().get(NODE_LISTENER_KEY))) {
            node.layoutXProperty().addListener((obs, oldVal, newVal) -> {
                updateCanvasSize();
                // 如果节点被选中，更新包围框
                if (selectedNodes.contains(node)) {
                    updateSelectionBoundingBox();
                }
            });
            node.layoutYProperty().addListener((obs, oldVal, newVal) -> {
                updateCanvasSize();
                // 如果节点被选中，更新包围框
                if (selectedNodes.contains(node)) {
                    updateSelectionBoundingBox();
                }
            });
            node.getProperties().put(NODE_LISTENER_KEY, Boolean.TRUE);
        }

        log("✓ 添加节点: " + node.getJobHandlerName());
        notifyNodeStructureChanged();

        if (recordHistory) {
            pushAction(new AddNodeAction(node));
        }
    }
    
    /**
     * 移除节点
     */
    public void removeNode(ProcessNode node) {
        removeNode(node, true);
    }

    public void removeNode(ProcessNode node, boolean recordHistory) {
        if (node == null) {
            return;
        }

        double oldX = node.getLayoutX();
        double oldY = node.getLayoutY();

        List<NodeConnection> attachedConnections = new ArrayList<>();
        for (NodeConnection conn : new ArrayList<>(connections)) {
            // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
            javafx.scene.Node sourceOwner = conn.getSourceOwner();
            javafx.scene.Node targetOwner = conn.getTargetOwner();
            
            if ((sourceOwner instanceof ProcessNode && sourceOwner == node) ||
                (targetOwner instanceof ProcessNode && targetOwner == node)) {
                attachedConnections.add(conn);
                removeConnection(conn, false);
            }
        }

        nodes.remove(node);
        this.getChildren().remove(node);

        log("✓ 删除节点: " + node.getJobHandlerName());
        notifyNodeStructureChanged();

        if (recordHistory) {
            pushAction(new RemoveNodeAction(node, oldX, oldY, attachedConnections));
        }
    }
    
    /**
     * 添加连接线（指定具体的连接点）
     */
    public NodeConnection addConnection(ProcessNode source, Circle sourceConnector,
                                        ProcessNode target, Circle targetConnector) {
        return addConnection(source, sourceConnector, target, targetConnector, true);
    }

    public NodeConnection addConnection(ProcessNode source, Circle sourceConnector,
                                        ProcessNode target, Circle targetConnector, boolean recordHistory) {
        NodeConnection connection = new NodeConnection(source, sourceConnector, target, targetConnector);
        configureConnectionInteractions(connection);
        addConnectionInternal(connection);
        notifyNodeStructureChanged();
        if (recordHistory) {
            pushAction(new AddConnectionAction(connection));
        }
        return connection;
    }

    /**
     * 添加连接线（自动选择连接点 - 兼容旧方法）
     */
    public NodeConnection addConnection(ProcessNode source, ProcessNode target) {
        // 默认使用右侧连接到左侧
        return addConnection(source, source.getRightConnector(), target, target.getLeftConnector(), true);
    }
    
    /**
     * 添加连接线（支持任务组容器）
     * @param sourceOwner 源所有者（ProcessNode 或 GroupContainer）
     * @param sourceConnectorParent 源连接点父容器
     * @param sourceConnector 源连接点
     * @param targetOwner 目标所有者（ProcessNode 或 GroupContainer）
     * @param targetConnectorParent 目标连接点父容器
     * @param targetConnector 目标连接点
     * @param recordHistory 是否记录历史
     * @return 创建的连接对象
     */
    public NodeConnection addConnection(javafx.scene.Node sourceOwner, Pane sourceConnectorParent, Circle sourceConnector,
                                        javafx.scene.Node targetOwner, Pane targetConnectorParent, Circle targetConnector,
                                        boolean recordHistory) {
        // ⭐ 新增：检查连线规则 - 不在同一个任务组里面的节点不能连线
        if (!canConnectNodes(sourceOwner, targetOwner)) {
            log("❌ 连线失败：不在同一个任务组里面的节点不能连线");
            cancelTempLine();
            return null;
        }
        
        NodeConnection connection = new NodeConnection(sourceOwner, sourceConnectorParent, sourceConnector,
                                                       targetOwner, targetConnectorParent, targetConnector);
        configureConnectionInteractions(connection);
        addConnectionInternal(connection);
        notifyNodeStructureChanged();
        if (recordHistory) {
            pushAction(new AddConnectionAction(connection));
        }
        return connection;
    }
    
    /**
     * ⭐ 新增：检查两个节点是否可以连线
     * 规则：
     * 1. 不在同一个任务组里面的节点不能连线
     * 2. 任务组外面的节点只能连接到任务组容器的边
     * 3. 任务组里面的节点只能互相连线
     * @param sourceOwner 源节点或任务组容器
     * @param targetOwner 目标节点或任务组容器
     * @return 是否可以连线
     */
    private boolean canConnectNodes(javafx.scene.Node sourceOwner, javafx.scene.Node targetOwner) {
        // 如果源和目标都是任务组容器，允许连线（任务组之间可以连线）
        if (sourceOwner instanceof GroupContainer && targetOwner instanceof GroupContainer) {
            return true;
        }
        
        // 如果源和目标都是普通节点
        if (sourceOwner instanceof ProcessNode && targetOwner instanceof ProcessNode) {
            ProcessNode sourceNode = (ProcessNode) sourceOwner;
            ProcessNode targetNode = (ProcessNode) targetOwner;
            
            // 查找源节点和目标节点所属的任务组
            GroupContainer sourceGroup = findGroupForNode(sourceNode);
            GroupContainer targetGroup = findGroupForNode(targetNode);
            
            // 如果两个节点都不在任何任务组中，允许连线
            if (sourceGroup == null && targetGroup == null) {
                return true;
            }
            
            // 如果两个节点在同一个任务组中，允许连线
            if (sourceGroup != null && sourceGroup == targetGroup) {
                return true;
            }
            
            // 如果两个节点在不同的任务组中，不允许连线
            if (sourceGroup != null && targetGroup != null && sourceGroup != targetGroup) {
                return false;
            }
            
            // 如果一个节点在任务组中，另一个不在，不允许连线
            if ((sourceGroup != null && targetGroup == null) || (sourceGroup == null && targetGroup != null)) {
                return false;
            }
        }
        
        // 如果源是普通节点，目标是任务组容器，允许连线（外部节点可以连接到任务组容器）
        if (sourceOwner instanceof ProcessNode && targetOwner instanceof GroupContainer) {
            ProcessNode sourceNode = (ProcessNode) sourceOwner;
            GroupContainer targetGroup = (GroupContainer) targetOwner;
            
            // 检查源节点是否在目标任务组中
            GroupContainer sourceGroup = findGroupForNode(sourceNode);
            if (sourceGroup == targetGroup) {
                // 源节点在目标任务组中，不允许连接到任务组容器（应该连接到任务组内的节点）
                return false;
            }
            // 源节点不在目标任务组中，允许连接到任务组容器
            return true;
        }
        
        // 如果源是任务组容器，目标是普通节点，允许连线（任务组容器可以连接到外部节点）
        if (sourceOwner instanceof GroupContainer && targetOwner instanceof ProcessNode) {
            GroupContainer sourceGroup = (GroupContainer) sourceOwner;
            ProcessNode targetNode = (ProcessNode) targetOwner;
            
            // 检查目标节点是否在源任务组中
            GroupContainer targetGroup = findGroupForNode(targetNode);
            if (targetGroup == sourceGroup) {
                // 目标节点在源任务组中，不允许从任务组容器连接到内部节点（应该从内部节点连接）
                return false;
            }
            // 目标节点不在源任务组中，允许从任务组容器连接到外部节点
            return true;
        }
        
        return true;
    }
    
    /**
     * ⭐ 新增：查找节点所属的任务组容器
     * @param node 节点
     * @return 任务组容器，如果节点不在任何任务组中则返回null
     */
    private GroupContainer findGroupForNode(ProcessNode node) {
        if (node == null) {
            return null;
        }
        
        for (GroupContainer container : groupContainers) {
            List<ProcessNode> managedNodes = container.getManagedCanvasNodes();
            if (managedNodes != null && managedNodes.contains(node)) {
                return container;
            }
        }
        
        return null;
    }

    /**
     * 移除连接线
     */
    public void removeConnection(NodeConnection connection) {
        removeConnection(connection, true);
    }

    public void removeConnection(NodeConnection connection, boolean recordHistory) {
        if (connection == null) {
            return;
        }
        removeConnectionInternal(connection);
        notifyNodeStructureChanged();
        if (recordHistory) {
            pushAction(new RemoveConnectionAction(connection));
        }
    }

    private void addConnectionInternal(NodeConnection connection) {
        if (connection == null) {
            return;
        }
        if (!connections.contains(connection)) {
            connections.add(connection);
        }
        if (!this.getChildren().contains(connection)) {
            // ⭐ 修复：检查边是否属于某个任务组容器
            // 如果属于，确保边的Z-order高于容器
            boolean isManagedByContainer = false;
            for (GroupContainer container : groupContainers) {
                List<NodeConnection> managedConnections = container.getManagedConnections();
                if (managedConnections != null && managedConnections.contains(connection)) {
                    isManagedByContainer = true;
                    break;
                }
            }
            
            if (isManagedByContainer) {
                // 任务组容器内的边，添加到画布后立即移到前面，确保显示在容器上方
                this.getChildren().add(connection);
                connection.toFront();
            } else {
                // 普通边，添加到前面
                this.getChildren().add(0, connection);
            }
        }
        logConnection("✓ 添加连接", connection);
    }

    private void removeConnectionInternal(NodeConnection connection) {
        if (connection == null) {
            return;
        }
        connections.remove(connection);
        this.getChildren().remove(connection);
        logConnection("✓ 删除连接", connection);
    }

    private void configureConnectionInteractions(NodeConnection connection) {
        ContextMenu menu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("删除连接");
        deleteItem.setStyle("-fx-text-fill: #EF4444;");
        deleteItem.setOnAction(e -> {
            // ⭐ 修复：支持任务组容器的连线删除
            String sourceName = getOwnerName(connection.getSourceOwner());
            String targetName = getOwnerName(connection.getTargetOwner());
            log("🗑️ 准备删除连接: " + sourceName + " → " + targetName);
            removeConnection(connection);
            log("提示: 删除后需点击保存按钮以持久化任务组变更");
        });

        menu.getItems().addAll(deleteItem);

        connection.setOnContextMenuRequested(event -> {
            connection.toFront();
            connection.setSelected(true);
            menu.show(connection, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        connection.setOnMousePressed(event -> {
            if (menu.isShowing()) {
                menu.hide();
            }
            connection.setSelected(false);
        });

        menu.setOnHidden(event -> connection.setSelected(false));
    }

    private void logConnection(String prefix, NodeConnection connection) {
        if (connection == null) {
            return;
        }
        // ⭐ 修复：支持任务组容器的连线日志
        String sourceName = getOwnerName(connection.getSourceOwner());
        String targetName = getOwnerName(connection.getTargetOwner());
        String sourcePos = getConnectorPosition(connection.getSourceOwner(), connection.getSourceConnector());
        String targetPos = getConnectorPosition(connection.getTargetOwner(), connection.getTargetConnector());
        log(prefix + ": " + sourceName + "[" + sourcePos + "] → " + targetName + "[" + targetPos + "]");
    }
    
    /**
     * 获取所有者名称（支持 ProcessNode 和 GroupContainer）
     */
    private String getOwnerName(javafx.scene.Node owner) {
        if (owner instanceof ProcessNode) {
            return ((ProcessNode) owner).getJobHandlerName();
        } else if (owner instanceof GroupContainer) {
            return ((GroupContainer) owner).getGroupName();
        }
        return "未知";
    }
    
    /**
     * 获取连接点位置（支持 ProcessNode 和 GroupContainer）
     */
    private String getConnectorPosition(javafx.scene.Node owner, Circle connector) {
        if (owner == null || connector == null) {
            return "未知";
        }
        if (owner instanceof ProcessNode) {
            ProcessNode node = (ProcessNode) owner;
            if (connector == node.getTopConnector()) return "top";
            if (connector == node.getBottomConnector()) return "bottom";
            if (connector == node.getLeftConnector()) return "left";
            if (connector == node.getRightConnector()) return "right";
        } else if (owner instanceof GroupContainer) {
            GroupContainer group = (GroupContainer) owner;
            if (connector == group.getTopConnector()) return "top";
            if (connector == group.getBottomConnector()) return "bottom";
            if (connector == group.getLeftConnector()) return "left";
            if (connector == group.getRightConnector()) return "right";
        }
        return "未知";
    }
    
    /**
     * 为连接点设置事件处理器
     */
    private void setupConnectorHandler(ProcessNode node, Circle connector) {
        // 按下连接点开始连线
        connector.setOnMousePressed(e -> {
            startOwner = node;
            
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
                
            // 检查鼠标释放位置是否在某个节点或任务组上
            ProcessNode targetNode = findNodeAtPosition(e.getSceneX(), e.getSceneY());
            GroupContainer targetGroup = findGroupAtPosition(e.getSceneX(), e.getSceneY());
            
            if (targetNode != null && startOwner instanceof ProcessNode && targetNode != startOwner) {
                    // 找到最近的目标连接点
                    Circle targetConnector = findNearestConnector(targetNode, e.getSceneX(), e.getSceneY());
                    
                    // 检查是否已经存在相同的连接
                    // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
                    boolean exists = connections.stream().anyMatch(conn -> {
                        javafx.scene.Node connSourceOwner = conn.getSourceOwner();
                        javafx.scene.Node connTargetOwner = conn.getTargetOwner();
                        return (connSourceOwner == startOwner && connTargetOwner == targetNode &&
                                conn.getSourceConnector() == connector && conn.getTargetConnector() == targetConnector) ||
                               (connSourceOwner == targetNode && connTargetOwner == startOwner &&
                                conn.getSourceConnector() == targetConnector && conn.getTargetConnector() == connector);
                    });
                    
                    if (!exists) {
                        // ⭐ 新增：检查连线规则
                        javafx.scene.Node sourceOwner = startOwner instanceof ProcessNode ? (ProcessNode) startOwner : null;
                        if (sourceOwner == null || !canConnectNodes(sourceOwner, targetNode)) {
                            log("❌ 连线失败：不在同一个任务组里面的节点不能连线");
                            cancelTempLine();
                        } else {
                            // 创建连接，指定具体的连接点
                            NodeConnection conn = addConnection((ProcessNode) startOwner, connector, targetNode, targetConnector, true);
                            if (conn == null) {
                                // 连线失败，已经记录日志，只需要清除临时连线
                                cancelTempLine();
                            } else {
                                // ⭐ 修复：连接成功后清除临时连线
                                cancelTempLine();
                            }
                        }
                    } else {
                        log("⚠️ 连接已存在");
                        // ⭐ 修复：即使连接已存在，也要清除临时连线
                        cancelTempLine();
                    }
            } else if (targetGroup != null) {
                // 与任务组容器相连（无论起点是节点还是容器）
                Circle targetConnector = findNearestConnector(targetGroup, e.getSceneX(), e.getSceneY());
                
                // ⭐ 修复：检查是否已经存在相同的连接
                boolean exists = false;
                if (startOwner instanceof ProcessNode) {
                    exists = connections.stream().anyMatch(conn ->
                        conn.getSourceOwner() == startOwner && conn.getTargetOwner() == targetGroup &&
                        conn.getSourceConnector() == connector && conn.getTargetConnector() == targetConnector
                    );
                } else if (startOwner instanceof GroupContainer) {
                    Circle startConn = findNearestConnector((GroupContainer) startOwner, e.getSceneX(), e.getSceneY());
                    exists = connections.stream().anyMatch(conn ->
                        conn.getSourceOwner() == startOwner && conn.getTargetOwner() == targetGroup &&
                        conn.getSourceConnector() == startConn && conn.getTargetConnector() == targetConnector
                    );
                }
                
                if (!exists) {
                    // ⭐ 新增：检查连线规则
                    javafx.scene.Node sourceOwner = startOwner instanceof ProcessNode ? (ProcessNode) startOwner : 
                                                    (startOwner instanceof GroupContainer ? (GroupContainer) startOwner : null);
                    if (sourceOwner == null || !canConnectNodes(sourceOwner, targetGroup)) {
                        log("❌ 连线失败：不在同一个任务组里面的节点不能连线");
                        cancelTempLine();
                    } else {
                        if (startOwner instanceof ProcessNode) {
                            // ⭐ 修复：使用 addConnection 方法，自动配置交互功能
                            NodeConnection conn = addConnection(
                                (ProcessNode) startOwner, ((ProcessNode) startOwner).getConnectorPane(), connector,
                                targetGroup, targetGroup.getConnectorPane(), targetConnector,
                                true
                            );
                            if (conn == null) {
                                // 连线失败，已经记录日志，只需要清除临时连线
                                cancelTempLine();
                            } else {
                                // ⭐ 修复：连接成功后清除临时连线
                                cancelTempLine();
                            }
                        } else if (startOwner instanceof GroupContainer) {
                            // 任务组 -> 任务组
                            Circle startConn = findNearestConnector((GroupContainer) startOwner, e.getSceneX(), e.getSceneY());
                            // ⭐ 修复：使用 addConnection 方法，自动配置交互功能
                            NodeConnection conn = addConnection(
                                ((GroupContainer) startOwner), ((GroupContainer) startOwner).getConnectorPane(), startConn,
                                targetGroup, targetGroup.getConnectorPane(), targetConnector,
                                true
                            );
                            if (conn == null) {
                                // 连线失败，已经记录日志，只需要清除临时连线
                                cancelTempLine();
                            } else {
                                // ⭐ 修复：连接成功后清除临时连线
                                cancelTempLine();
                            }
                        }
                    }
                } else {
                    log("⚠️ 连接已存在");
                    // ⭐ 修复：即使连接已存在，也要清除临时连线
                    cancelTempLine();
                }
                } else {
                if (targetNode != null && targetNode == startOwner) {
                        log("⚠️ 不能连接到自己");
                    } else {
                        log("❌ 取消连线（未找到目标节点）");
                    }
                    // ⭐ 修复：未找到目标时也要清除临时连线
                    cancelTempLine();
                }
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
            startOwner = null;
        }
    }

    // 为任务组容器的连接点挂接同样的行为
    private void setupConnectorHandler(GroupContainer group, Circle connector) {
        connector.setOnMousePressed(e -> {
            startOwner = group;
            
            tempLine = new Line();
            tempLine.setStroke(Color.web("#8B5CF6"));
            tempLine.setStrokeWidth(2);
            tempLine.getStrokeDashArray().addAll(5.0, 5.0);
            
            javafx.geometry.Point2D connectorCenter = new javafx.geometry.Point2D(
                connector.getLayoutX(),
                connector.getLayoutY()
            );
            javafx.geometry.Point2D nodeLocal = group.getConnectorPane().localToParent(connectorCenter);
            javafx.geometry.Point2D canvasLocal = group.localToParent(nodeLocal);
            
            tempLine.setStartX(canvasLocal.getX());
            tempLine.setStartY(canvasLocal.getY());
            
            this.getChildren().add(tempLine);
            e.consume();
        });
        
        connector.setOnMouseDragged(e -> {
            if (tempLine != null) {
                javafx.geometry.Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                tempLine.setEndX(localPoint.getX());
                tempLine.setEndY(localPoint.getY());
            }
            e.consume();
        });
        
        connector.setOnMouseReleased(e -> {
            if (tempLine != null) {
                ProcessNode targetNode = findNodeAtPosition(e.getSceneX(), e.getSceneY());
                GroupContainer targetGroup = findGroupAtPosition(e.getSceneX(), e.getSceneY());
                
                if (targetNode != null) {
                    Circle targetConnector = findNearestConnector(targetNode, e.getSceneX(), e.getSceneY());
                    Circle startConn = findNearestConnector(group, e.getSceneX(), e.getSceneY());
                    
                    // ⭐ 修复：检查是否已经存在相同的连接
                    boolean exists = connections.stream().anyMatch(conn ->
                        conn.getSourceOwner() == group && conn.getTargetOwner() == targetNode &&
                        conn.getSourceConnector() == startConn && conn.getTargetConnector() == targetConnector
                    );
                    
                    if (!exists) {
                        // ⭐ 新增：检查连线规则
                        if (!canConnectNodes(group, targetNode)) {
                            log("❌ 连线失败：不在同一个任务组里面的节点不能连线");
                            cancelTempLine();
                        } else {
                            // ⭐ 修复：使用 addConnection 方法，自动配置交互功能
                            NodeConnection conn = addConnection(
                                group, group.getConnectorPane(), startConn,
                                targetNode, targetNode.getConnectorPane(), targetConnector,
                                true
                            );
                            if (conn == null) {
                                // 连线失败，已经记录日志，只需要清除临时连线
                                cancelTempLine();
                            } else {
                                // ⭐ 修复：连接成功后清除临时连线
                                cancelTempLine();
                            }
                        }
                    } else {
                        log("⚠️ 连接已存在");
                        // ⭐ 修复：即使连接已存在，也要清除临时连线
                        cancelTempLine();
                    }
                } else if (targetGroup != null && targetGroup != group) {
                    Circle targetConnector = findNearestConnector(targetGroup, e.getSceneX(), e.getSceneY());
                    Circle startConn = findNearestConnector(group, e.getSceneX(), e.getSceneY());
                    
                    // ⭐ 修复：检查是否已经存在相同的连接
                    boolean exists = connections.stream().anyMatch(conn ->
                        conn.getSourceOwner() == group && conn.getTargetOwner() == targetGroup &&
                        conn.getSourceConnector() == startConn && conn.getTargetConnector() == targetConnector
                    );
                    
                    if (!exists) {
                        // ⭐ 新增：检查连线规则
                        if (!canConnectNodes(group, targetGroup)) {
                            log("❌ 连线失败：不在同一个任务组里面的节点不能连线");
                            cancelTempLine();
                        } else {
                            // ⭐ 修复：使用 addConnection 方法，自动配置交互功能
                            NodeConnection conn = addConnection(
                                group, group.getConnectorPane(), startConn,
                                targetGroup, targetGroup.getConnectorPane(), targetConnector,
                                true
                            );
                            if (conn == null) {
                                // 连线失败，已经记录日志，只需要清除临时连线
                                cancelTempLine();
                            } else {
                                // ⭐ 修复：连接成功后清除临时连线
                                cancelTempLine();
                            }
                        }
                    } else {
                        log("⚠️ 连接已存在");
                        // ⭐ 修复：即使连接已存在，也要清除临时连线
                        cancelTempLine();
                    }
                } else {
                    log("❌ 取消连线（未找到目标节点）");
                    // ⭐ 修复：未找到目标时也要清除临时连线
                    cancelTempLine();
                }
            }
            e.consume();
        });
    }
    
    /**
     * 查找指定位置的节点
     */
    private ProcessNode findNodeAtPosition(double sceneX, double sceneY) {
        // 将场景坐标转换为Canvas的局部坐标
        javafx.geometry.Point2D canvasPoint = sceneToLocal(sceneX, sceneY);
        double x = canvasPoint.getX();
        double y = canvasPoint.getY();
        
        
        for (ProcessNode node : nodes) {
            double nodeX = node.getLayoutX();
            double nodeY = node.getLayoutY();
            double nodeWidth = node.getPrefWidth();
            double nodeHeight = node.getPrefHeight();
            
            
            // 扩大检测范围（包括连接点突出部分）
            if (x >= nodeX - 15 && x <= nodeX + nodeWidth + 15 &&
                y >= nodeY - 15 && y <= nodeY + nodeHeight + 15) {
                return node;
            }
        }
        
        return null;
    }
    
    private com.cc.job.gui.model.GroupContainer findGroupAtPosition(double sceneX, double sceneY) {
        javafx.geometry.Point2D canvasPoint = sceneToLocal(sceneX, sceneY);
        double x = canvasPoint.getX();
        double y = canvasPoint.getY();
        for (GroupContainer g : groupContainers) {
            double gx = g.getLayoutX();
            double gy = g.getLayoutY();
            double gw = g.getWidth() > 0 ? g.getWidth() : 320;
            double gh = g.getHeight() > 0 ? g.getHeight() : 160;
            if (x >= gx && x <= gx + gw && y >= gy && y <= gy + gh) {
                return g;
            }
        }
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
    
    // 重载：任务组容器
    private Circle findNearestConnector(GroupContainer group, double sceneX, double sceneY) {
        javafx.geometry.Point2D canvasPoint = sceneToLocal(sceneX, sceneY);
        double x = canvasPoint.getX();
        double y = canvasPoint.getY();
        Circle[] connectors = new Circle[] {
            group.getTopConnector(),
            group.getBottomConnector(),
            group.getLeftConnector(),
            group.getRightConnector()
        };
        Circle nearest = connectors[0];
        double minDist = Double.MAX_VALUE;
        for (Circle c : connectors) {
            javafx.geometry.Point2D nodeLocal = group.getConnectorPane().localToParent(new javafx.geometry.Point2D(c.getLayoutX(), c.getLayoutY()));
            javafx.geometry.Point2D parentLocal = group.localToParent(nodeLocal);
            double dx = parentLocal.getX() - x;
            double dy = parentLocal.getY() - y;
            double dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = c;
            }
        }
        return nearest;
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
     * 根据锚点字符串获取任务组容器对应的连接点
     * @param container 任务组容器
     * @param anchor 锚点字符串，可能为 "top", "bottom", "left", "right" 或 null
     * @param isSource 是否为源节点（true=源节点，false=目标节点）
     * @return 连接点Circle对象
     */
    private Circle getConnectorByAnchor(GroupContainer container, String anchor, boolean isSource) {
        if (anchor != null && !anchor.isEmpty()) {
            // 根据锚点字符串返回对应的连接点（不区分大小写）
            String anchorLower = anchor.toLowerCase();
            if ("top".equals(anchorLower)) {
                return container.getTopConnector();
            } else if ("bottom".equals(anchorLower)) {
                return container.getBottomConnector();
            } else if ("left".equals(anchorLower)) {
                return container.getLeftConnector();
            } else if ("right".equals(anchorLower)) {
                return container.getRightConnector();
            }
        }
        
        // 如果没有指定锚点，使用默认值
        // 源节点默认使用右侧（数据流出）
        // 目标节点默认使用左侧（数据流入）
        if (isSource) {
            return container.getRightConnector();
        } else {
            return container.getLeftConnector();
        }
    }
    
    /**
     * 清空画布
     */
    public void clear() {
        // ⭐ 修复：清空任务组容器
        for (GroupContainer container : new ArrayList<>(groupContainers)) {
            this.getChildren().remove(container);
        }
        groupContainers.clear();
        
        this.getChildren().clear();
        nodes.clear();
        connections.clear();
        log("✓ 画布已清空");
        updateCanvasSize();
    }
    
    /**
     * 仅清空页面内容（不删除辅助UI元素、不触发持久化），用于"清空页面"功能
     */
    public void clearViewOnly() {
        // 移除所有连接与节点，但保留辅助矩形
        for (NodeConnection conn : new ArrayList<>(connections)) {
            removeConnectionInternal(conn);
        }
        for (ProcessNode node : new ArrayList<>(nodes)) {
            nodes.remove(node);
            this.getChildren().remove(node);
        }
        // ⭐ 修复：清空任务组容器
        for (GroupContainer container : new ArrayList<>(groupContainers)) {
            this.getChildren().remove(container);
        }
        groupContainers.clear();
        // 隐藏选择框
        clearSelection();
        updateCanvasSize();
        log("✓ 已清空页面（仅视图，不影响数据库，保存后才生效）");
    }
    
    /**
     * 动态更新画布大小以包含所有节点
     */
    private void updateCanvasSize() {
//        if (nodes.isEmpty()) {
//            setPrefSize(2000, 1500);
//            return;
//        }
//
//        // 计算所有节点的边界
//        double maxX = 0;
//        double maxY = 0;
//
//        for (ProcessNode node : nodes) {
//            double nodeRight = node.getLayoutX() + node.getPrefWidth() + 100; // 额外空间
//            double nodeBottom = node.getLayoutY() + node.getPrefHeight() + 100;
//
//            maxX = Math.max(maxX, nodeRight);
//            maxY = Math.max(maxY, nodeBottom);
//        }
//
//        // 设置最小尺寸，确保画布至少有基本大小
//        maxX = Math.max(maxX, 2000);
//        maxY = Math.max(maxY, 1500);
//
//        setPrefSize(maxX, maxY);
    }

    public void loadFromComposeData(JobComposeData composeData) {
        if (composeData == null) {
            log("⚠ 没有数据可加载");
            return;
        }
        if (undoRedoManager != null) {
            undoRedoManager.clear();
        }

        runWithoutHistory(() -> {
            Map<Long, double[]> previousPositionsByJobId = snapshotNodePositionsByJobId();
            Map<String, double[]> previousPositionsByNodeId = snapshotNodePositionsByNodeId();

            clear();

            Map<String, ProcessNode> nodeMap = new HashMap<>();
            List<JobComposeData.NodeData> groupNodeDataList = new ArrayList<>();

            List<JobComposeData.NodeData> nodeDataList = composeData.getNodes();
            if (nodeDataList != null && !nodeDataList.isEmpty()) {
                for (JobComposeData.NodeData nodeData : nodeDataList) {
                    String nodeType = nodeData.getType();
                    String jobName = nodeData.getJobName();
                    Long jobId = nodeData.getJobId();
                    String nodeId = nodeData.getId();

                    boolean isGroupNode = false;
                    if (nodeType != null && !nodeType.trim().isEmpty() && !"null".equals(nodeType)) {
                        String normalizedType = nodeType.trim();
                        isGroupNode = normalizedType.equals("CustomGroup") ||
                                normalizedType.equalsIgnoreCase("custom-group");
                    }

                    if (!isGroupNode && nodeData.getProperties() != null) {
                        Map<String, Object> props = nodeData.getProperties();
                        Object childrenObj = props.get("children");
                        if (childrenObj != null) {
                            isGroupNode = true;
                        }
                    }

                    if (isGroupNode) {
                        groupNodeDataList.add(nodeData);
                        continue;
                    }

                    String text = nodeData.getJobName() != null ? nodeData.getJobName() : "Node";
                    ProcessNode node = new ProcessNode(nodeData.getId(), text);

                    if (nodeData.getJobId() != null) {
                        node.setJobId(nodeData.getJobId());
                        NodeStatusSyncManager.getInstance()
                                .rememberStatus(nodeData.getJobId(), nodeData.getTriggerStatus());
                    }

                    Integer triggerStatus = null;
                    Long nodeJobId = nodeData.getJobId();
                    Integer backendStatus = nodeData.getTriggerStatus();
                    Integer cachedStatus = nodeJobId != null
                            ? NodeStatusSyncManager.getInstance().getCachedStatus(nodeJobId)
                            : null;

                    if (backendStatus != null && cachedStatus != null) {
                        if (backendStatus == 2 && (cachedStatus == 0 || cachedStatus == 1)) {
                            triggerStatus = cachedStatus;
                        } else {
                            triggerStatus = backendStatus;
                        }
                    } else if (cachedStatus != null) {
                        triggerStatus = cachedStatus;
                    } else if (backendStatus != null) {
                        triggerStatus = backendStatus;
                    }

                    if (triggerStatus != null) {
                        node.updateStatusByCode(triggerStatus);
                        if (nodeJobId != null) {
                            NodeStatusSyncManager.getInstance()
                                    .rememberStatus(nodeJobId, triggerStatus);
                        }
                    }

                    if (hasValidCoordinates(nodeData.getX(), nodeData.getY())) {
                        node.setLayoutX(nodeData.getX());
                        node.setLayoutY(nodeData.getY());
                    } else {
                        double[] previous = null;
                        if (nodeData.getJobId() != null) {
                            previous = previousPositionsByJobId.get(nodeData.getJobId());
                        }
                        if (previous == null) {
                            previous = previousPositionsByNodeId.get(nodeData.getId());
                        }
                        if (previous != null) {
                            node.setLayoutX(previous[0]);
                            node.setLayoutY(previous[1]);
                        } else {
                            int index = nodeDataList.indexOf(nodeData);
                            node.setLayoutX(100 + (index % 3) * 250);
                            node.setLayoutY(100 + (index / 3) * 200);
                        }
                    }

                    String mappedType = mapNodeType(nodeData.getType(), nodeData.getProperties());
                    node.setType(mappedType);

                    addNode(node, false);
                    nodeMap.put(nodeData.getId(), node);
                }

                log("✓ 加载了 " + nodeDataList.size() + " 个节点");
            }

            Map<String, GroupContainer> containerMap = new HashMap<>();
            if (!groupNodeDataList.isEmpty()) {
                Map<String, JobComposeData.NodeData> groupNodeMap = new HashMap<>();
                for (JobComposeData.NodeData groupNodeData : groupNodeDataList) {
                    groupNodeMap.put(groupNodeData.getId(), groupNodeData);
                }

                for (JobComposeData.NodeData groupNodeData : groupNodeDataList) {
                    try {
                        createGroupContainerRecursive(groupNodeData, composeData, nodeMap, containerMap, groupNodeMap);
                    } catch (Exception e) {
                        logger.error("创建任务组容器失败: id={}, name={}, error={}", 
                            groupNodeData.getId(), groupNodeData.getJobName(), e.getMessage(), e);
                        log("✗ 创建任务组容器失败: " + groupNodeData.getJobName() + " - " + e.getMessage());
                    }
                }
            } else {
            }

            // ⭐ 修复：在创建任务组容器后，再次处理边数据，支持连接到任务组容器的边
            List<JobComposeData.EdgeData> edgeDataList = composeData.getEdges();
            if (edgeDataList != null && !edgeDataList.isEmpty()) {
                int successCount = 0;
                for (JobComposeData.EdgeData edgeData : edgeDataList) {
                    // 尝试从 nodeMap 中查找源节点
                    ProcessNode sourceNode = nodeMap.get(edgeData.getSourceNodeId());
                    // 如果找不到，尝试从 containerMap 中查找任务组容器
                    GroupContainer sourceContainer = containerMap.get(edgeData.getSourceNodeId());
                    
                    // 尝试从 nodeMap 中查找目标节点
                    ProcessNode targetNode = nodeMap.get(edgeData.getTargetNodeId());
                    // 如果找不到，尝试从 containerMap 中查找任务组容器
                    GroupContainer targetContainer = containerMap.get(edgeData.getTargetNodeId());

                    // ⭐ 修复：支持多种连接组合：节点->节点、节点->任务组、任务组->节点、任务组->任务组
                    javafx.scene.Node sourceOwner = null;
                    javafx.scene.Node targetOwner = null;
                    Circle sourceConnector = null;
                    Circle targetConnector = null;
                    Pane sourceConnectorParent = null;
                    Pane targetConnectorParent = null;
                    
                    // 确定源节点/容器
                    if (sourceNode != null) {
                        sourceOwner = sourceNode;
                        sourceConnector = getConnectorByAnchor(sourceNode, edgeData.getSourceAnchor(), true);
                        sourceConnectorParent = sourceNode.getConnectorPane();
                    } else if (sourceContainer != null) {
                        sourceOwner = sourceContainer;
                        sourceConnector = getConnectorByAnchor(sourceContainer, edgeData.getSourceAnchor(), true);
                        sourceConnectorParent = sourceContainer.getConnectorPane();
                    }
                    
                    // 确定目标节点/容器
                    if (targetNode != null) {
                        targetOwner = targetNode;
                        targetConnector = getConnectorByAnchor(targetNode, edgeData.getTargetAnchor(), false);
                        targetConnectorParent = targetNode.getConnectorPane();
                    } else if (targetContainer != null) {
                        targetOwner = targetContainer;
                        targetConnector = getConnectorByAnchor(targetContainer, edgeData.getTargetAnchor(), false);
                        targetConnectorParent = targetContainer.getConnectorPane();
                    }
                    
                    // 如果源和目标都找到了，创建连接
                    if (sourceOwner != null && targetOwner != null && 
                        sourceConnector != null && targetConnector != null &&
                        sourceConnectorParent != null && targetConnectorParent != null) {
                        NodeConnection edge = addConnection(
                            sourceOwner, sourceConnectorParent, sourceConnector,
                            targetOwner, targetConnectorParent, targetConnector,
                            false
                        );
                        if (edge != null) {
                            edge.setEdgeId(edgeData.getId());
                        }
                        successCount++;
                    } else {
                        log("⚠ 无法创建连接: 找不到节点 " + edgeData.getSourceNodeId() + " 或 " + edgeData.getTargetNodeId());
                    }
                }

                log("✓ 加载了 " + successCount + " 条连接");
            }
            
            // ⭐ 修复：在所有边创建完成后，重新刷新所有任务组容器的边绑定
            // 确保任务组容器中的边被正确管理，折叠时会隐藏
            for (GroupContainer container : containerMap.values()) {
                List<ProcessNode> managedNodes = container.getManagedCanvasNodes();
                Set<String> managedNodeIds = new HashSet<>();
                for (ProcessNode node : managedNodes) {
                    if (node.getNodeId() != null) {
                        managedNodeIds.add(node.getNodeId());
                    }
                }
                refreshGroupContainerConnections(container, managedNodeIds);
                
                // ⭐ 修复：确保任务组容器内的边的Z-order高于容器本身
                List<NodeConnection> containerConnections = container.getManagedConnections();
                if (containerConnections != null) {
                    for (NodeConnection conn : containerConnections) {
                        if (conn != null && getChildren().contains(conn)) {
                            conn.toFront();
                        }
                    }
                }
            }

            updateCanvasSize();
            log("✓ 任务组数据加载完成");
        });

        notifyNodeStructureChanged();
    }

    private boolean hasValidCoordinates(Double x, Double y) {
        if (!isCoordinateNumber(x) || !isCoordinateNumber(y)) {
            return false;
        }
        return Math.abs(x) + Math.abs(y) > 1e-3;
    }

    private boolean isCoordinateNumber(Double value) {
        if (value == null) {
            return false;
        }
        return !value.isNaN() && !value.isInfinite();
    }

    private Map<Long, double[]> snapshotNodePositionsByJobId() {
        Map<Long, double[]> map = new HashMap<>();
        for (ProcessNode node : nodes) {
            Long jobId = node.getJobId();
            if (jobId != null) {
                map.put(jobId, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        }
        return map;
    }

    private Map<String, double[]> snapshotNodePositionsByNodeId() {
        Map<String, double[]> map = new HashMap<>();
        for (ProcessNode node : nodes) {
            String nodeId = node.getNodeId();
            if (nodeId != null) {
                map.put(nodeId, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        }
        return map;
    }

    private void createGroupContainerRecursive(JobComposeData.NodeData groupNodeData,
                                               JobComposeData composeData,
                                               Map<String, ProcessNode> nodeMap,
                                               Map<String, GroupContainer> containerMap,
                                               Map<String, JobComposeData.NodeData> groupNodeMap) {
        if (containerMap.containsKey(groupNodeData.getId())) {
            return;
        }

        String groupName = groupNodeData.getJobName() != null ? groupNodeData.getJobName() : "任务组";
        Long groupJobId = groupNodeData.getJobId();
        String groupNodeId = groupNodeData.getId();

        List<String> childNodeIds = new ArrayList<>();
        Map<String, Object> properties = groupNodeData.getProperties();
        if (properties != null) {
            Object childrenObj = properties.get("children");
            
            if (childrenObj instanceof String childrenStr) {
                // ⭐ 修复：使用 Gson 正确解析 JSON 字符串
                childrenStr = childrenStr.trim();
                if (childrenStr.startsWith("[") && childrenStr.endsWith("]")) {
                    try {
                        // 尝试使用 Gson 解析 JSON 数组
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
                        List<String> parsedList = gson.fromJson(childrenStr, listType);
                        if (parsedList != null) {
                            childNodeIds.addAll(parsedList);
                        }
                    } catch (Exception e) {
                        // 如果 JSON 解析失败，回退到手动解析
                        childrenStr = childrenStr.substring(1, childrenStr.length() - 1);
                        if (!childrenStr.isBlank()) {
                            for (String part : childrenStr.split(",")) {
                                String trimmed = part.trim().replace("\"", "").replace("'", "");
                                if (!trimmed.isEmpty()) {
                                    childNodeIds.add(trimmed);
                                }
                            }
                        }
                    }
                } else {
                    // 如果不是 JSON 数组格式，尝试按逗号分割
                    if (!childrenStr.isBlank()) {
                        for (String part : childrenStr.split(",")) {
                            String trimmed = part.trim().replace("\"", "").replace("'", "");
                            if (!trimmed.isEmpty()) {
                                childNodeIds.add(trimmed);
                            }
                        }
                    }
                }
            } else if (childrenObj instanceof List<?> childrenList) {
                for (Object child : childrenList) {
                    if (child != null) {
                        childNodeIds.add(child.toString());
                    }
                }
            }
        } else {
        }

        List<ProcessNode> childNodes = new ArrayList<>();
        List<GroupContainer> childContainers = new ArrayList<>();
        List<NodeConnection> childConnections = new ArrayList<>();

        // ⭐ 修复：优先使用 childrenNodes 字段（包含子节点的完整数据）
        // 但需要按照 children ID列表的顺序处理，确保子节点位置正确
        List<JobComposeData.NodeData> childrenNodesList = groupNodeData.getChildrenNodes();
        if (childrenNodesList != null && !childrenNodesList.isEmpty()) {
            
            // ⭐ 修复：按照 children ID列表的顺序处理子节点，确保位置正确
            Map<String, JobComposeData.NodeData> childrenNodesMap = new HashMap<>();
            for (JobComposeData.NodeData childNodeData : childrenNodesList) {
                childrenNodesMap.put(childNodeData.getId(), childNodeData);
            }
            
            // 按照 children ID列表的顺序处理
            for (String childId : childNodeIds) {
                JobComposeData.NodeData childNodeData = childrenNodesMap.get(childId);
                if (childNodeData == null) {
                    continue;
                }
                String childType = childNodeData.getType();
                
                // 检查是否是任务组节点
                boolean isChildGroupNode = false;
                if (childType != null && !childType.trim().isEmpty() && !"null".equals(childType)) {
                    String normalizedType = childType.trim();
                    isChildGroupNode = normalizedType.equals("CustomGroup") ||
                            normalizedType.equalsIgnoreCase("custom-group");
                }
                
                if (!isChildGroupNode && childNodeData.getProperties() != null) {
                    Map<String, Object> childProps = childNodeData.getProperties();
                    Object childChildrenObj = childProps.get("children");
                    if (childChildrenObj != null) {
                        isChildGroupNode = true;
                    }
                }
                
                if (isChildGroupNode) {
                    // 子节点是任务组节点，递归创建
                    // 确保子任务组节点在 groupNodeMap 中
                    if (!groupNodeMap.containsKey(childId)) {
                        groupNodeMap.put(childId, childNodeData);
                    }
                    createGroupContainerRecursive(childNodeData, composeData, nodeMap, containerMap, groupNodeMap);
                    GroupContainer childContainer = containerMap.get(childId);
                    if (childContainer != null) {
                        childContainers.add(childContainer);
                    } else {
                    }
                } else {
                    // 子节点是普通节点，从 nodeMap 中查找或创建
                    ProcessNode childNode = nodeMap.get(childId);
                    if (childNode != null) {
                        childNodes.add(childNode);
                    } else {
                        // 如果找不到，尝试创建节点（这种情况不应该发生，但为了容错）
                        String text = childNodeData.getJobName() != null ? childNodeData.getJobName() : "Node";
                        ProcessNode newNode = new ProcessNode(childId, text);
                        if (childNodeData.getJobId() != null) {
                            newNode.setJobId(childNodeData.getJobId());
                            NodeStatusSyncManager.getInstance()
                                    .rememberStatus(childNodeData.getJobId(), childNodeData.getTriggerStatus());
                        }
                        
                        // 设置节点状态
                        Integer triggerStatus = childNodeData.getTriggerStatus();
                        if (triggerStatus != null) {
                            newNode.updateStatusByCode(triggerStatus);
                        }
                        
                        if (hasValidCoordinates(childNodeData.getX(), childNodeData.getY())) {
                            newNode.setLayoutX(childNodeData.getX());
                            newNode.setLayoutY(childNodeData.getY());
                        }
                        String mappedType = mapNodeType(childNodeData.getType(), childNodeData.getProperties());
                        newNode.setType(mappedType);
                        addNode(newNode, false);
                        nodeMap.put(childId, newNode);
                        childNodes.add(newNode);
                    }
                }
            }
        } else {
            // 如果没有 childrenNodes 字段，回退到使用 children ID 列表
            for (String childId : childNodeIds) {
                JobComposeData.NodeData childGroupData = groupNodeMap.get(childId);
                if (childGroupData != null) {
                    createGroupContainerRecursive(childGroupData, composeData, nodeMap, containerMap, groupNodeMap);
                    GroupContainer childContainer = containerMap.get(childId);
                    if (childContainer != null) {
                        childContainers.add(childContainer);
                    } else {
                    }
                } else {
                    ProcessNode childNode = nodeMap.get(childId);
                    if (childNode != null) {
                        childNodes.add(childNode);
                    } else {
                    }
                }
            }
        }

        // ⭐ 修复：查找子节点之间的边（包括嵌套任务组中的边）
        // 注意：这里先收集所有子节点的ID（包括嵌套任务组中的节点）
        Set<String> allChildNodeIds = new HashSet<>();
        for (ProcessNode childNode : childNodes) {
            if (childNode.getNodeId() != null) {
                allChildNodeIds.add(childNode.getNodeId());
            }
        }
        for (GroupContainer childContainer : childContainers) {
            List<ProcessNode> nestedNodes = childContainer.getManagedCanvasNodes();
            if (nestedNodes != null) {
                for (ProcessNode nestedNode : nestedNodes) {
                    if (nestedNode.getNodeId() != null) {
                        allChildNodeIds.add(nestedNode.getNodeId());
                    }
                }
            }
        }
        
        List<JobComposeData.EdgeData> allEdgeDataList = composeData.getEdges();
        if (allEdgeDataList != null) {
            for (JobComposeData.EdgeData edgeData : allEdgeDataList) {
                String sourceId = edgeData.getSourceNodeId();
                String targetId = edgeData.getTargetNodeId();
                
                // ⭐ 修复：检查边的源节点和目标节点是否都在当前任务组的子节点中
                if (allChildNodeIds.contains(sourceId) && allChildNodeIds.contains(targetId)) {
                    // 从 connections 中查找对应的连接
                    for (NodeConnection conn : connections) {
                        // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
                        javafx.scene.Node sourceOwner = conn.getSourceOwner();
                        javafx.scene.Node targetOwner = conn.getTargetOwner();
                        
                        String connSourceId = null;
                        String connTargetId = null;
                        
                        if (sourceOwner instanceof ProcessNode) {
                            connSourceId = ((ProcessNode) sourceOwner).getNodeId();
                        } else if (sourceOwner instanceof GroupContainer) {
                            connSourceId = ((GroupContainer) sourceOwner).getNodeId();
                        }
                        
                        if (targetOwner instanceof ProcessNode) {
                            connTargetId = ((ProcessNode) targetOwner).getNodeId();
                        } else if (targetOwner instanceof GroupContainer) {
                            connTargetId = ((GroupContainer) targetOwner).getNodeId();
                        }
                        
                        // 匹配边的源和目标ID
                        if (sourceId.equals(connSourceId) && targetId.equals(connTargetId)) {
                            if (!childConnections.contains(conn)) {
                                childConnections.add(conn);
                            }
                            break;
                        }
                    }
                }
            }
        }

        GroupContainer container = new GroupContainer(groupNodeData.getId(), groupJobId, groupName);
        if (hasValidCoordinates(groupNodeData.getX(), groupNodeData.getY())) {
            container.setLayoutX(groupNodeData.getX());
            container.setLayoutY(groupNodeData.getY());
        }

        List<ProcessNode> allChildNodes = new ArrayList<>(childNodes);
        for (GroupContainer childContainer : childContainers) {
            List<ProcessNode> nestedNodes = childContainer.getManagedCanvasNodes();
            if (nestedNodes != null) {
                allChildNodes.addAll(nestedNodes);
            }
        }

        container.bindCanvasNodes(allChildNodes);
        // ⭐ 修复：先绑定初始的边（可能为空，因为边可能还没有创建）
        container.bindConnections(childConnections);

        for (GroupContainer childContainer : childContainers) {
            if (!getChildren().contains(childContainer)) {
                getChildren().add(0, childContainer);
            }
        }

        container.expand();
        getChildren().add(0, container);
        groupContainers.add(container);
        containerMap.put(groupNodeId, container);
        
        // ⭐ 修复：在创建任务组容器后，重新查找并绑定子节点之间的边
        // 因为边可能是在创建任务组容器之后才创建的
        refreshGroupContainerConnections(container, allChildNodeIds);
        
        // ⭐ 修复：确保任务组容器内的边的Z-order高于容器本身
        // 在容器创建后，立即调整所有管理边的层级
        List<NodeConnection> containerConnections = container.getManagedConnections();
        if (containerConnections != null) {
            for (NodeConnection conn : containerConnections) {
                if (conn != null && getChildren().contains(conn)) {
                    conn.toFront();
                }
            }
        }

        setupConnectorHandler(container, container.getTopConnector());
        setupConnectorHandler(container, container.getBottomConnector());
        setupConnectorHandler(container, container.getLeftConnector());
        setupConnectorHandler(container, container.getRightConnector());
        
        // 设置删除回调
        container.setOnDelete(() -> {
            System.out.println("GroupContainer的onDelete回调被触发，onDeleteGroupContainer: " + (onDeleteGroupContainer != null ? "已设置" : "为null"));
            if (onDeleteGroupContainer != null) {
                System.out.println("调用onDeleteGroupContainer.accept");
                onDeleteGroupContainer.accept(container);
            } else {
                System.err.println("错误：onDeleteGroupContainer回调为null，无法删除任务组节点！");
            }
        });

        log("✓ 加载任务组节点: " + groupName +
                " (子节点数: " + childNodes.size() + ", 嵌套任务组数: " + childContainers.size() + ")");
    }
    
    /**
     * 刷新任务组容器的边绑定（在边创建后调用）
     * @param container 任务组容器
     * @param managedNodeIds 管理的节点ID集合
     */
    private void refreshGroupContainerConnections(GroupContainer container, Set<String> managedNodeIds) {
        if (container == null || managedNodeIds == null || managedNodeIds.isEmpty()) {
            return;
        }
        
        List<NodeConnection> managedConnections = new ArrayList<>();
        
        // 查找所有源节点和目标节点都在 managedNodeIds 中的边
        for (NodeConnection conn : connections) {
            javafx.scene.Node sourceOwner = conn.getSourceOwner();
            javafx.scene.Node targetOwner = conn.getTargetOwner();
            
            String sourceId = null;
            String targetId = null;
            
            if (sourceOwner instanceof ProcessNode) {
                sourceId = ((ProcessNode) sourceOwner).getNodeId();
            } else if (sourceOwner instanceof GroupContainer) {
                sourceId = ((GroupContainer) sourceOwner).getNodeId();
            }
            
            if (targetOwner instanceof ProcessNode) {
                targetId = ((ProcessNode) targetOwner).getNodeId();
            } else if (targetOwner instanceof GroupContainer) {
                targetId = ((GroupContainer) targetOwner).getNodeId();
            }
            
            // 如果边的源节点和目标节点都在管理的节点ID集合中，则添加到管理列表
            if (sourceId != null && targetId != null && 
                managedNodeIds.contains(sourceId) && managedNodeIds.contains(targetId)) {
                if (!managedConnections.contains(conn)) {
                    managedConnections.add(conn);
                }
            }
        }
        
        // 重新绑定边
        container.bindConnections(managedConnections);
        
        // ⭐ 修复：确保任务组容器内的边的Z-order高于容器本身
        // 在绑定后立即调整边的层级
        for (NodeConnection conn : managedConnections) {
            if (conn != null && getChildren().contains(conn)) {
                conn.toFront();
            }
        }
        
    }

    private String mapNodeType(String rawType, Map<String, Object> properties) {
        String candidate = rawType;
        if ((candidate == null || candidate.isBlank()) && properties != null) {
            Object glueType = properties.get("glueType");
            if (glueType instanceof String) {
                candidate = (String) glueType;
            }
        }
        if (candidate == null || candidate.isBlank()) {
            return "Bean";
        }
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "bean", "custom-bean" -> "Bean";
            case "api", "custom-api" -> "API";
            case "sql", "custom-sql" -> "SQL";
            case "java", "glue(java)", "custom-java" -> "Java";
            case "shell", "glue(shell)", "custom-shell" -> "Shell";
            case "python", "glue(python)", "custom-python" -> "Python";
            case "php", "glue(php)", "custom-php" -> "PHP";
            case "node", "nodejs", "glue(nodejs)", "custom-nodejs" -> "Node";
            case "powershell", "ps", "glue(powershell)", "custom-powershell" -> "PS";
            default -> {
                if (normalized.startsWith("glue")) {
                    String suffix = normalized.replace("glue", "").replace("(", "").replace(")", "").trim();
                    if (!suffix.isEmpty()) {
                        String upper = suffix.toUpperCase(Locale.ROOT);
                        yield switch (upper) {
                            case "JAVA" -> "Java";
                            case "SHELL" -> "Shell";
                            case "PYTHON" -> "Python";
                            case "PHP" -> "PHP";
                            case "NODEJS" -> "Node";
                            case "POWERSHELL" -> "PS";
                            default -> "Bean";
                        };
                    }
                }
                yield candidate;
            }
        };
    }
 
    /**
     * 设置所有边的运行状态（任务组运行时调用）
     * @param running 是否运行中
     */
    public void setAllConnectionsRunning(boolean running) {
        for (NodeConnection conn : connections) {
            conn.setRunning(running);
        }
    }
    
    /**
     * 根据jobId更新节点状态
     * @param jobId 任务ID
     * @param statusCode 状态码：0=失败, 1=成功, 2=运行中
     */
    public void updateNodeStatusByJobId(Long jobId, Integer statusCode) {
        if (jobId == null || statusCode == null) {
            return;
        }
        
        
        boolean found = false;
        for (ProcessNode node : nodes) {
            Long nodeJobId = node.getJobId();
            
            if (nodeJobId != null && nodeJobId.equals(jobId)) {
                
                node.updateStatusByCode(statusCode);
                
                
                // ⭐ 添加到批量更新队列（不立即调用后端）
                NodeStatusSyncManager.getInstance().addPendingUpdate(jobId, statusCode);
                
                found = true;
                break; // 找到节点后更新并退出
            }
        }
        
        if (!found) {
            for (ProcessNode node : nodes) {
            }
        }
    }
    
    /**
     * 同步所有待更新的节点状态到数据库
     * 在页面切换、任务完成等时机调用
     */
    public void syncPendingNodeStatus() {
        NodeStatusSyncManager.getInstance().syncNow();
    }

    public void syncPendingNodeStatusBlocking() {
        com.cc.job.gui.util.NodeStatusSyncManager.getInstance().syncNowBlocking();
    }
    
    /**
     * 刷新所有节点的状态（从缓存中获取最新状态）
     * 用于切换任务组时恢复节点状态
     */
    public void refreshAllNodeStatusFromCache() {
        NodeStatusSyncManager statusManager = NodeStatusSyncManager.getInstance();
        int updatedCount = 0;
        
        for (ProcessNode node : nodes) {
            Long jobId = node.getJobId();
            if (jobId != null) {
                Integer cachedStatus = statusManager.getCachedStatus(jobId);
                if (cachedStatus != null) {
                    // 如果缓存中有状态，更新节点状态
                    node.updateStatusByCode(cachedStatus);
                    updatedCount++;
                }
            }
        }
        
        if (updatedCount > 0) {
        } else {
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
        private final double oldX;
        private final double oldY;
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
                addConnectionInternal(connection);
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
        private final double oldX;
        private final double oldY;
        private final double newX;
        private final double newY;

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

    private static class NodePositionSnapshot {
        final double oldX;
        final double oldY;
        final double newX;
        final double newY;

        NodePositionSnapshot(double oldX, double oldY, double newX, double newY) {
            this.oldX = oldX;
            this.oldY = oldY;
            this.newX = newX;
            this.newY = newY;
        }
    }

    private class MoveNodeGroupAction implements CanvasAction {
        private final ProcessNode mainNode;
        private final double mainOldX;
        private final double mainOldY;
        private final double mainNewX;
        private final double mainNewY;
        private final Map<ProcessNode, NodePositionSnapshot> shiftedNodes;

        MoveNodeGroupAction(ProcessNode mainNode,
                            double mainOldX,
                            double mainOldY,
                            double mainNewX,
                            double mainNewY,
                            Map<ProcessNode, NodePositionSnapshot> shiftedNodes) {
            this.mainNode = mainNode;
            this.mainOldX = mainOldX;
            this.mainOldY = mainOldY;
            this.mainNewX = mainNewX;
            this.mainNewY = mainNewY;
            this.shiftedNodes = shiftedNodes;
        }

        @Override
        public void undo() {
            mainNode.setLayoutX(mainOldX);
            mainNode.setLayoutY(mainOldY);
            for (Map.Entry<ProcessNode, NodePositionSnapshot> entry : shiftedNodes.entrySet()) {
                ProcessNode node = entry.getKey();
                NodePositionSnapshot snapshot = entry.getValue();
                node.setLayoutX(snapshot.oldX);
                node.setLayoutY(snapshot.oldY);
            }
            notifyNodeStructureChanged();
        }

        @Override
        public void redo() {
            mainNode.setLayoutX(mainNewX);
            mainNode.setLayoutY(mainNewY);
            for (Map.Entry<ProcessNode, NodePositionSnapshot> entry : shiftedNodes.entrySet()) {
                ProcessNode node = entry.getKey();
                NodePositionSnapshot snapshot = entry.getValue();
                node.setLayoutX(snapshot.newX);
                node.setLayoutY(snapshot.newY);
            }
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
            removeConnection(connection, false);
            notifyNodeStructureChanged();
        }

        @Override
        public void redo() {
            addConnectionInternal(connection);
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
            addConnectionInternal(connection);
            notifyNodeStructureChanged();
        }

        @Override
        public void redo() {
            removeConnection(connection, false);
            notifyNodeStructureChanged();
        }
    }

    private boolean edgeIdMatches(String requestedId, String existingId) {
        if (existingId == null) {
            return false;
        }
        String normalizedRequested = normalizeEdgeId(requestedId);
        String normalizedExisting = normalizeEdgeId(existingId);
        return !normalizedRequested.isEmpty() && normalizedRequested.equals(normalizedExisting);
    }

    private String normalizeEdgeId(String rawId) {
        if (rawId == null) {
            return "";
        }
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
    
    /**
     * 检查是否处于框选模式
     */
    public boolean isSelectionMode() {
        return selectionMode;
    }
    
    /**
     * 检查点击是否在节点或边上
     */
    private boolean isClickOnNodeOrEdge(javafx.scene.Node target) {
        if (target == null) {
            return false;
        }
        
        // 检查目标本身
        if (target instanceof ProcessNode || target instanceof NodeConnection) {
            return true;
        }
        
        // 向上遍历父节点，检查是否在节点或边内
        javafx.scene.Node current = target.getParent();
        while (current != null && current != this) {
            if (current instanceof ProcessNode || current instanceof NodeConnection) {
                return true;
            }
            current = current.getParent();
        }
        
        return false;
    }
    
    /**
     * 设置框选事件处理器
     * 使用事件过滤器（EventFilter）确保在事件传播到子节点之前处理框选逻辑
     */
    private void setupSelectionHandlers() {
        // 使用事件过滤器，在事件传播到子节点之前处理
        // 鼠标按下：开始框选或清除选择
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (!e.isPrimaryButtonDown()) {
                return;
            }
            
            // 检查是否点击在节点或连接点上
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            boolean isOnNodeOrEdge = isClickOnNodeOrEdge(target);
            
            // 如果不在框选模式
            if (!selectionMode) {
                if (!isOnNodeOrEdge) {
                    // 点击空白区域时清除选择
                    clearSelection();
                }
                return; // 不拦截，让节点/边正常处理
            }
            
            // 框选模式：只有点击在空白区域时才开始框选
            // 如果点击在节点上，让节点正常处理（允许移动节点）
            if (isOnNodeOrEdge) {
                return; // 不拦截，让节点/边正常处理
            }
            
            // 点击空白区域，开始框选
            javafx.geometry.Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
            selectionStartX = localPoint.getX();
            selectionStartY = localPoint.getY();
            
            // 初始化并显示临时蓝色框选矩形
            selectionRect.setX(selectionStartX);
            selectionRect.setY(selectionStartY);
            selectionRect.setWidth(0);
            selectionRect.setHeight(0);
            selectionRect.setVisible(true);
            
            // 确保蓝色框在最上层显示
            this.getChildren().remove(selectionRect);
            this.getChildren().add(selectionRect);
            selectionRect.toFront();
            
            // 拦截事件，防止其他处理
            e.consume();
        });
        
        // 鼠标拖动：更新框选矩形（使用事件过滤器）
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (!selectionMode || !e.isPrimaryButtonDown()) {
                return;
            }
            
            // 如果临时框选矩形可见，说明正在进行框选操作
            if (selectionRect.isVisible()) {
                // 获取当前鼠标位置
                javafx.geometry.Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                double currentX = localPoint.getX();
                double currentY = localPoint.getY();
                
                // 计算矩形的位置和大小
                double rectX = Math.min(selectionStartX, currentX);
                double rectY = Math.min(selectionStartY, currentY);
                double rectWidth = Math.abs(currentX - selectionStartX);
                double rectHeight = Math.abs(currentY - selectionStartY);
                
                // 更新临时蓝色框选矩形的位置和大小
                selectionRect.setX(rectX);
                selectionRect.setY(rectY);
                selectionRect.setWidth(rectWidth);
                selectionRect.setHeight(rectHeight);
                selectionRect.setVisible(true);
                
                // 确保蓝色框在最上层
                selectionRect.toFront();
                
                // 实时更新选择：高亮选中的节点（但不显示红色包围框）
                updateSelectionDuringDrag(rectX, rectY, rectWidth, rectHeight);
                
                // 拦截拖动事件，防止节点移动
                e.consume();
            }
            // 如果蓝色框不可见，说明不是在框选，让节点正常处理拖动
        });
        
        // 鼠标释放：完成框选（使用事件过滤器）
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            if (!selectionMode) {
                return;
            }
            
            // 如果临时框选矩形可见，说明正在进行框选
            if (selectionRect.isVisible()) {
                javafx.geometry.Point2D localPoint = sceneToLocal(e.getSceneX(), e.getSceneY());
                double currentX = localPoint.getX();
                double currentY = localPoint.getY();
                
                double rectX = Math.min(selectionStartX, currentX);
                double rectY = Math.min(selectionStartY, currentY);
                double rectWidth = Math.abs(currentX - selectionStartX);
                double rectHeight = Math.abs(currentY - selectionStartY);
                
                // 隐藏临时蓝色框选矩形
                selectionRect.setVisible(false);
                
                // 只有当框选区域足够大时才完成选择（避免误触）
                if (rectWidth > 5 && rectHeight > 5) {
                    // 完成选择：更新选中节点并显示红色包围框
                    updateSelection(rectX, rectY, rectWidth, rectHeight);
                    
                    // 记录日志
                    if (!selectedNodes.isEmpty()) {
                        log("✓ 框选完成: 选中 " + selectedNodes.size() + " 个节点, " + selectedConnections.size() + " 条边");
                    } else {
                        log("⚠ 框选区域未选中任何节点");
                    }
                } else {
                    // 框选区域太小，清除选择
                    clearSelection();
                }
                
                // 拦截释放事件
                e.consume();
            }
        });
    }
    
    /**
     * 页面（空白区域）右键菜单
     */
    private void setupCanvasContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        MenuItem addNodeItem = new MenuItem("新增节点");
        addNodeItem.setOnAction(e -> {
            if (onRequestAddNode != null) {
                onRequestAddNode.run();
            } else {
                log("ℹ 新增节点回调未设置");
            }
        });
        // TODO 选择任务组功能需要优化
//        MenuItem chooseGroupItem = new MenuItem("选择任务组");
//        chooseGroupItem.setOnAction(e -> {
//            if (onRequestSelectTaskGroup != null) {
//                onRequestSelectTaskGroup.run();
//            } else {
//                log("ℹ 选择任务组回调未设置");
//            }
//        });
        
        MenuItem clearItem = new MenuItem("清空页面");
        clearItem.setOnAction(e -> {
            if (onRequestClearCanvas != null) {
                onRequestClearCanvas.run();
            } else {
                // 默认行为：仅清空视图
                clearViewOnly();
            }
        });
        
        MenuItem runGroupItem = new MenuItem("运行任务组");
        // 为“运行任务组”添加一个小三角图标
        {
            javafx.scene.text.Text playIcon = new javafx.scene.text.Text("▶");
            playIcon.setStyle("-fx-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: bold;");
            runGroupItem.setGraphic(playIcon);
        }
        runGroupItem.setOnAction(e -> {
            if (onRequestRunTaskGroup != null) {
                onRequestRunTaskGroup.run();
            } else {
                log("ℹ 运行任务组回调未设置");
            }
        });
        
        menu.getItems().addAll(addNodeItem, clearItem, runGroupItem);
        
        this.setOnContextMenuRequested(e -> {
            // 仅在空白区域展示页面级菜单
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            boolean isOnNodeOrEdge = isClickOnNodeOrEdge(target);
            if (!isOnNodeOrEdge) {
                menu.show(this, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });
        
        // 点击、拖动或滚动其他区域时自动隐藏菜单（避免与右键触发冲突）
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (menu.isShowing() && !e.isSecondaryButtonDown()) {
                menu.hide();
            }
        });
        this.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (menu.isShowing()) {
                menu.hide();
            }
        });
        this.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (menu.isShowing()) {
                menu.hide();
            }
        });
    }
    
    /**
     * 拖动过程中更新选择（只高亮节点，不显示包围框）
     */
    private void updateSelectionDuringDrag(double rectX, double rectY, double rectWidth, double rectHeight) {
        // 清除之前的高亮效果（但不清除包围框，因为拖动时不应该显示包围框）
        for (ProcessNode node : selectedNodes) {
            highlightNode(node, false);
        }
        for (NodeConnection connection : selectedConnections) {
            connection.setSelected(false);
        }
        
        selectedNodes.clear();
        selectedConnections.clear();
        selectionOriginalPositions.clear();
        
        // 检查节点是否在框选区域内
        for (ProcessNode node : nodes) {
            double nodeX = node.getLayoutX();
            double nodeY = node.getLayoutY();
            double nodeWidth = node.getPrefWidth();
            double nodeHeight = node.getPrefHeight();
            
            // 检查节点是否与框选矩形相交
            if (rectIntersects(rectX, rectY, rectWidth, rectHeight, 
                              nodeX, nodeY, nodeWidth, nodeHeight)) {
                selectedNodes.add(node);
                highlightNode(node, true);
                // 记录节点的原始位置（用于批量移动）
                selectionOriginalPositions.put(node, new double[]{nodeX, nodeY});
            }
        }
        
        // 检查边是否在框选区域内（边的源节点和目标节点都在选择区域内）
        for (NodeConnection connection : connections) {
            ProcessNode sourceNode = connection.getSourceNode();
            ProcessNode targetNode = connection.getTargetNode();
            
            // 如果源节点和目标节点都被选中，则边也被选中
            if (selectedNodes.contains(sourceNode) && selectedNodes.contains(targetNode)) {
                selectedConnections.add(connection);
                connection.setSelected(true);
            }
        }
        
        // 拖动过程中不显示包围框，只高亮节点
    }
    
    /**
     * 更新选择（根据框选矩形）- 框选完成时调用
     */
    private void updateSelection(double rectX, double rectY, double rectWidth, double rectHeight) {
        // 清除之前的选择视觉效果
        clearSelectionVisual();
        
        selectedNodes.clear();
        selectedConnections.clear();
        selectionOriginalPositions.clear();
        
        // 检查节点是否在框选区域内
        for (ProcessNode node : nodes) {
            double nodeX = node.getLayoutX();
            double nodeY = node.getLayoutY();
            double nodeWidth = node.getPrefWidth();
            double nodeHeight = node.getPrefHeight();
            
            // 检查节点是否与框选矩形相交
            if (rectIntersects(rectX, rectY, rectWidth, rectHeight, 
                              nodeX, nodeY, nodeWidth, nodeHeight)) {
                selectedNodes.add(node);
                highlightNode(node, true);
                // 记录节点的原始位置（用于批量移动）
                selectionOriginalPositions.put(node, new double[]{nodeX, nodeY});
            }
        }
        
        // 检查边是否在框选区域内（边的源节点和目标节点都在选择区域内）
        for (NodeConnection connection : connections) {
            // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
            javafx.scene.Node sourceOwner = connection.getSourceOwner();
            javafx.scene.Node targetOwner = connection.getTargetOwner();
            
            // 只处理 ProcessNode 之间的连接（任务组容器的连接暂不支持框选）
            if (sourceOwner instanceof ProcessNode && targetOwner instanceof ProcessNode) {
                ProcessNode sourceNode = (ProcessNode) sourceOwner;
                ProcessNode targetNode = (ProcessNode) targetOwner;
                
                // 如果源节点和目标节点都被选中，则边也被选中
                if (selectedNodes.contains(sourceNode) && selectedNodes.contains(targetNode)) {
                    selectedConnections.add(connection);
                    connection.setSelected(true);
                }
            }
        }
        
        // 更新选中节点的红色包围框
        updateSelectionBoundingBox();
    }
    
    /**
     * 更新选中节点的红色包围框
     */
    public void updateSelectionBoundingBox() {
        if (selectedNodes.isEmpty()) {
            selectionBoundingBox.setVisible(false);
            return;
        }
        
        // 计算所有选中节点的边界
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        
        for (ProcessNode node : selectedNodes) {
            double nodeX = node.getLayoutX();
            double nodeY = node.getLayoutY();
            double nodeWidth = node.getPrefWidth();
            double nodeHeight = node.getPrefHeight();
            
            minX = Math.min(minX, nodeX);
            minY = Math.min(minY, nodeY);
            maxX = Math.max(maxX, nodeX + nodeWidth);
            maxY = Math.max(maxY, nodeY + nodeHeight);
        }
        
        // 添加一些边距，使框更美观
        double padding = 10;
        double x = minX - padding;
        double y = minY - padding;
        double width = maxX - minX + padding * 2;
        double height = maxY - minY + padding * 2;
        
        // 更新红色包围框
        selectionBoundingBox.setX(x);
        selectionBoundingBox.setY(y);
        selectionBoundingBox.setWidth(width);
        selectionBoundingBox.setHeight(height);
        selectionBoundingBox.setVisible(true);
        
        // 确保包围框在画布的最上层（在所有节点和边之上）
        this.getChildren().remove(selectionBoundingBox);
        this.getChildren().add(selectionBoundingBox);
    }
    
    /**
     * 检查两个矩形是否相交
     */
    private boolean rectIntersects(double x1, double y1, double w1, double h1,
                                   double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
    
    /**
     * 高亮显示节点
     */
    public void highlightNode(ProcessNode node, boolean highlight) {
        if (highlight) {
            // 添加高亮效果（可以通过添加一个半透明的矩形覆盖层）
            node.setStyle("-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.5), 10, 0, 0, 0);");
        } else {
            node.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        }
    }
    
    /**
     * 清除选择视觉效果
     */
    private void clearSelectionVisual() {
        for (ProcessNode node : selectedNodes) {
            highlightNode(node, false);
        }
        for (NodeConnection connection : selectedConnections) {
            connection.setSelected(false);
        }
        // 隐藏红色包围框
        selectionBoundingBox.setVisible(false);
    }
    
    /**
     * 清除选择
     */
    public void clearSelection() {
        clearSelectionVisual();
        selectedNodes.clear();
        selectedConnections.clear();
        selectionRect.setVisible(false);
        selectionBoundingBox.setVisible(false);
        isMovingSelection = false;
        dragStartNode = null;
        selectionOriginalPositions.clear();
    }
    
    /**
     * 设置框选模式时也清除选择
     */
    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        if (!enabled) {
            // 退出框选模式时清除选择
            clearSelection();
        }
        log(selectionMode ? "✓ 框选模式已启用" : "✓ 框选模式已禁用");
        
        // 通知外部组件框选模式已改变
        if (onSelectionModeChanged != null) {
            onSelectionModeChanged.accept(enabled);
        }
    }
    
    /**
     * 获取选中的节点
     */
    public java.util.Set<ProcessNode> getSelectedNodes() {
        return new java.util.HashSet<>(selectedNodes);
    }
    
    /**
     * 获取选中的边
     */
    public java.util.Set<NodeConnection> getSelectedConnections() {
        return new java.util.HashSet<>(selectedConnections);
    }
    
    /**
     * 选中指定的节点（用于程序化选择）
     * @param node 要选中的节点
     */
    public void selectNode(ProcessNode node) {
        if (node == null || !nodes.contains(node)) {
            return;
        }
        clearSelection();
        selectedNodes.add(node);
        highlightNode(node, true);
        // 记录节点的原始位置（用于批量移动）
        selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
        updateSelectionBoundingBox();
    }
    
    /**
     * 添加节点到选中集合（不清除之前的选择）
     * @param node 要添加的节点
     */
    public void addToSelection(ProcessNode node) {
        if (node == null || !nodes.contains(node)) {
            return;
        }
        if (!selectedNodes.contains(node)) {
            selectedNodes.add(node);
            highlightNode(node, true);
            // 记录节点的原始位置（用于批量移动）
            selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
            updateSelectionBoundingBox();
        }
    }
    
    /**
     * 选中多个节点（用于程序化选择）
     * @param nodes 要选中的节点集合
     */
    public void selectNodes(java.util.Collection<ProcessNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            clearSelection();
            return;
        }
        clearSelection();
        for (ProcessNode node : nodes) {
            if (node != null && this.nodes.contains(node)) {
                selectedNodes.add(node);
                highlightNode(node, true);
                // 记录节点的原始位置（用于批量移动）
                selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        }
        updateSelectionBoundingBox();
    }
    
    /**
     * 开始选中节点的批量移动
     */
    private void startSelectionMove(ProcessNode draggedNode) {
        if (selectedNodes.size() <= 1) {
            // 只有一个节点或没有选中节点，使用正常的单节点移动
            return;
        }
        
        isMovingSelection = true;
        dragStartNode = draggedNode;
        
        // 记录所有选中节点的初始位置（如果还没有记录的话）
        for (ProcessNode node : selectedNodes) {
            if (!selectionOriginalPositions.containsKey(node)) {
                selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        }
    }
    
    /**
     * 结束选中节点的批量移动
     */
    private void endSelectionMove() {
        if (!isMovingSelection) {
            return;
        }
        
        isMovingSelection = false;
        
        // 更新选中节点的原始位置
        for (ProcessNode node : selectedNodes) {
            selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
        }
        
        dragStartNode = null;
        notifyNodeStructureChanged();
    }
    
    /**
     * 横向布局：将所有选中节点的 Y 坐标对齐到同一水平线
     */
    public void alignHorizontal() {
        if (selectedNodes.isEmpty()) {
            log("⚠ 请先选中要布局的节点");
            return;
        }
        
        if (selectedNodes.size() < 2) {
            log("⚠ 需要至少选中 2 个节点才能进行横向布局");
            return;
        }
        
        // 计算所有选中节点的平均 Y 坐标（或使用最小 Y）
        double avgY = 0.0;
        Map<ProcessNode, double[]> originalPositions = new HashMap<>();
        
        for (ProcessNode node : selectedNodes) {
            double y = node.getLayoutY();
            avgY += y;
            // 记录原始位置用于历史记录
            originalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
        }
        
        final double finalAvgY = Math.max(0, avgY / selectedNodes.size());
        
        // 对齐所有节点的 Y 坐标
        runWithoutHistory(() -> {
            for (ProcessNode node : selectedNodes) {
                node.setLayoutY(finalAvgY);
            }
        });
        
        // 记录历史
        if (undoRedoManager != null && historyEnabled) {
            Map<ProcessNode, NodePositionSnapshot> nodeSnapshots = new HashMap<>();
            for (Map.Entry<ProcessNode, double[]> entry : originalPositions.entrySet()) {
                ProcessNode node = entry.getKey();
                double[] oldPos = entry.getValue();
                nodeSnapshots.put(node, new NodePositionSnapshot(
                    oldPos[0], oldPos[1],
                    node.getLayoutX(), node.getLayoutY()
                ));
            }
            
            if (!nodeSnapshots.isEmpty()) {
                // 使用第一个节点作为主要节点（用于 MoveNodeGroupAction）
                ProcessNode mainNode = nodeSnapshots.keySet().iterator().next();
                NodePositionSnapshot mainSnapshot = nodeSnapshots.remove(mainNode);
                nodeSnapshots.put(mainNode, mainSnapshot);
                
                pushAction(new MoveNodeGroupAction(
                    mainNode,
                    mainSnapshot.oldX, mainSnapshot.oldY,
                    mainSnapshot.newX, mainSnapshot.newY,
                    nodeSnapshots
                ));
            }
        }
        
        // 更新包围框
        updateSelectionBoundingBox();
        notifyNodeStructureChanged();
        
        log("✓ 横向布局完成: " + selectedNodes.size() + " 个节点已对齐到 Y=" + String.format("%.1f", finalAvgY));
    }
    
    /**
     * 纵向布局：将所有选中节点的 X 坐标对齐到同一垂直线
     */
    public void alignVertical() {
        if (selectedNodes.isEmpty()) {
            log("⚠ 请先选中要布局的节点");
            return;
        }
        
        if (selectedNodes.size() < 2) {
            log("⚠ 需要至少选中 2 个节点才能进行纵向布局");
            return;
        }
        
        // 计算所有选中节点的平均 X 坐标（或使用最小 X）
        double avgX = 0.0;
        Map<ProcessNode, double[]> originalPositions = new HashMap<>();
        
        for (ProcessNode node : selectedNodes) {
            double x = node.getLayoutX();
            avgX += x;
            // 记录原始位置用于历史记录
            originalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
        }
        
        final double finalAvgX = Math.max(0, avgX / selectedNodes.size());
        
        // 对齐所有节点的 X 坐标
        runWithoutHistory(() -> {
            for (ProcessNode node : selectedNodes) {
                node.setLayoutX(finalAvgX);
            }
        });
        
        // 记录历史
        if (undoRedoManager != null && historyEnabled) {
            Map<ProcessNode, NodePositionSnapshot> nodeSnapshots = new HashMap<>();
            for (Map.Entry<ProcessNode, double[]> entry : originalPositions.entrySet()) {
                ProcessNode node = entry.getKey();
                double[] oldPos = entry.getValue();
                nodeSnapshots.put(node, new NodePositionSnapshot(
                    oldPos[0], oldPos[1],
                    node.getLayoutX(), node.getLayoutY()
                ));
            }
            
            if (!nodeSnapshots.isEmpty()) {
                // 使用第一个节点作为主要节点（用于 MoveNodeGroupAction）
                ProcessNode mainNode = nodeSnapshots.keySet().iterator().next();
                NodePositionSnapshot mainSnapshot = nodeSnapshots.remove(mainNode);
                nodeSnapshots.put(mainNode, mainSnapshot);
                
                pushAction(new MoveNodeGroupAction(
                    mainNode,
                    mainSnapshot.oldX, mainSnapshot.oldY,
                    mainSnapshot.newX, mainSnapshot.newY,
                    nodeSnapshots
                ));
            }
        }
        
        // 更新包围框
        updateSelectionBoundingBox();
        notifyNodeStructureChanged();
        
        log("✓ 纵向布局完成: " + selectedNodes.size() + " 个节点已对齐到 X=" + String.format("%.1f", finalAvgX));
    }
    
}

