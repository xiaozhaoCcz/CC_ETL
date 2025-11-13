package com.cc.job.gui.view;

import com.cc.job.gui.history.CanvasAction;
import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import javafx.animation.PauseTransition;
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
import java.util.List;
import java.util.Map;
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
    private List<NodeConnection> connections = new ArrayList<>();
    private UndoRedoManager undoRedoManager;
    private boolean historyEnabled = true;

    // 临时连线相关
    private ProcessNode startNode;
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

    public interface LogCallback {
        void log(String message);
    }
    
    public NodeCanvas() {
        // 设置初始尺寸
        setPrefSize(2000, 1500);
        setStyle("-fx-background-color: #F3F4F6;");
        
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
                log("📍 定位连接: " + connection.getSourceNode().getJobHandlerName()
                        + " → " + connection.getTargetNode().getJobHandlerName());
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
        logger.debug(message);
        if (logCallback != null) {
            logCallback.log(message);
        }
    }

    private void beginAutoShiftSession(ProcessNode node) {
        currentDraggingNode = node;
        autoShiftOriginalPositions.clear();
        autoShiftApplied = 0.0;
        autoShiftAppliedX = 0.0;
    }

    private void endAutoShiftSession() {
        currentDraggingNode = null;
        autoShiftOriginalPositions.clear();
        autoShiftApplied = 0.0;
        autoShiftAppliedX = 0.0;
    }

    private Point2D adjustNodePositionOnDrag(ProcessNode node, double proposedX, double proposedY) {
        double adjustedX = Math.max(0, proposedX);
        double adjustedY = Math.max(0, proposedY);

        if (currentDraggingNode == node) {
            if (adjustedY < TOP_DRAG_MARGIN) {
                double requiredShift = TOP_DRAG_MARGIN - adjustedY;
                double incrementalShift = requiredShift - autoShiftApplied;
                if (incrementalShift > 0) {
                    shiftOtherNodesVertically(node, incrementalShift);
                    autoShiftApplied += incrementalShift;
                }
                adjustedY = TOP_DRAG_MARGIN;
            } else if (autoShiftApplied > 0) {
                double release = Math.min(autoShiftApplied, adjustedY - TOP_DRAG_MARGIN);
                if (release > 0) {
                    shiftOtherNodesVertically(node, -release);
                    autoShiftApplied -= release;
                }
            }

            if (adjustedX < LEFT_DRAG_MARGIN) {
                double requiredShiftX = LEFT_DRAG_MARGIN - adjustedX;
                double incrementalShiftX = requiredShiftX - autoShiftAppliedX;
                if (incrementalShiftX > 0) {
                    shiftOtherNodesHorizontally(node, incrementalShiftX);
                    autoShiftAppliedX += incrementalShiftX;
                }
                adjustedX = LEFT_DRAG_MARGIN;
            } else if (autoShiftAppliedX > 0) {
                double releaseX = Math.min(autoShiftAppliedX, adjustedX - LEFT_DRAG_MARGIN);
                if (releaseX > 0) {
                    shiftOtherNodesHorizontally(node, -releaseX);
                    autoShiftAppliedX -= releaseX;
                }
            }
        }

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

        double contentWidth = getBoundsInLocal().getWidth();
        double contentHeight = getBoundsInLocal().getHeight();
        double viewportWidth = viewportInCanvas.getWidth();
        double viewportHeight = viewportInCanvas.getHeight();

        double contentMaxX = Math.max(contentWidth - viewportWidth, 0);
        double contentMaxY = Math.max(contentHeight - viewportHeight, 0);

        double viewportMinX = viewportInCanvas.getMinX();
        double viewportMaxX = viewportInCanvas.getMaxX();
        double viewportMinY = viewportInCanvas.getMinY();
        double viewportMaxY = viewportInCanvas.getMaxY();

        double newViewportX = viewportMinX;
        double newViewportY = viewportMinY;

        if (nodeBounds.getMinY() < viewportMinY + AUTO_SCROLL_MARGIN) {
            newViewportY = Math.max(nodeBounds.getMinY() - AUTO_SCROLL_MARGIN, 0);
        } else if (nodeBounds.getMaxY() > viewportMaxY - AUTO_SCROLL_MARGIN) {
            newViewportY = Math.min(nodeBounds.getMaxY() + AUTO_SCROLL_MARGIN - viewportHeight, contentMaxY);
        }

        if (nodeBounds.getMinX() < viewportMinX + AUTO_SCROLL_MARGIN) {
            newViewportX = Math.max(nodeBounds.getMinX() - AUTO_SCROLL_MARGIN, 0);
        } else if (nodeBounds.getMaxX() > viewportMaxX - AUTO_SCROLL_MARGIN) {
            newViewportX = Math.min(nodeBounds.getMaxX() + AUTO_SCROLL_MARGIN - viewportWidth, contentMaxX);
        }

        if (contentMaxY > 0 && Math.abs(newViewportY - viewportMinY) > 1e-3) {
            hostingScrollPane.setVvalue(clamp01(newViewportY / contentMaxY));
        } else if (contentMaxY <= 0) {
            hostingScrollPane.setVvalue(0);
        }

        if (contentMaxX > 0 && Math.abs(newViewportX - viewportMinX) > 1e-3) {
            hostingScrollPane.setHvalue(clamp01(newViewportX / contentMaxX));
        } else if (contentMaxX <= 0) {
            hostingScrollPane.setHvalue(0);
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

        // 拖拽结束后记录历史并结束批量移动
        node.setOnDragFinished((oldX, oldY, newX, newY) -> {
            // 如果正在批量移动，先结束批量移动并更新所有选中节点的原始位置
            if (isMovingSelection && dragStartNode == node) {
                endSelectionMove();
            }
            
            // 然后记录历史
            if (undoRedoManager != null && historyEnabled) {
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
            if (conn.getSourceNode() == node || conn.getTargetNode() == node) {
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
            this.getChildren().add(0, connection);
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
            log("🗑️ 准备删除连接: " + connection.getSourceNode().getJobHandlerName()
                    + " → " + connection.getTargetNode().getJobHandlerName());
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
        ProcessNode source = connection.getSourceNode();
        ProcessNode target = connection.getTargetNode();
        String sourcePos = getConnectorPosition(source, connection.getSourceConnector());
        String targetPos = getConnectorPosition(target, connection.getTargetConnector());
        log(prefix + ": " + source.getJobHandlerName() + "[" + sourcePos + "] → " +
            target.getJobHandlerName() + "[" + targetPos + "]");
    }
    
    /**
     * 为连接点设置事件处理器
     */
    private void setupConnectorHandler(ProcessNode node, Circle connector) {
        // 按下连接点开始连线
        connector.setOnMousePressed(e -> {
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
                        addConnection(startNode, connector, targetNode, targetConnector, true);
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
        
        logger.debug("   → 检测位置: sceneX={}, sceneY={} -> canvasX={}, canvasY={}", sceneX, sceneY, x, y);
        
        for (ProcessNode node : nodes) {
            double nodeX = node.getLayoutX();
            double nodeY = node.getLayoutY();
            double nodeWidth = node.getPrefWidth();
            double nodeHeight = node.getPrefHeight();
            
            logger.debug("   → 检查节点: {} bounds=[{},{}, {}x{}]", node.getJobHandlerName(), nodeX, nodeY, nodeWidth, nodeHeight);
            
            // 扩大检测范围（包括连接点突出部分）
            if (x >= nodeX - 15 && x <= nodeX + nodeWidth + 15 &&
                y >= nodeY - 15 && y <= nodeY + nodeHeight + 15) {
                logger.debug("   ✅ 找到目标节点: {}", node.getJobHandlerName());
                return node;
            }
        }
        
        logger.debug("   ❌ 未找到目标节点");
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
        if (undoRedoManager != null) {
            undoRedoManager.clear();
        }

        runWithoutHistory(() -> {
            Map<Long, double[]> previousPositionsByJobId = snapshotNodePositionsByJobId();
            Map<String, double[]> previousPositionsByNodeId = snapshotNodePositionsByNodeId();

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
                        logger.debug("✅ 节点 {} (nodeId: {}) 已设置jobId: {}", text, nodeData.getId(), nodeData.getJobId());
                        log("✅ 节点已设置jobId: " + text + " -> jobId: " + nodeData.getJobId());
                    } else {
                        logger.warn("⚠️ 节点 {} (nodeId: {}) 的jobId为空！", text, nodeData.getId());
                        log("⚠️ 警告: 节点 " + text + " 的jobId为空！");
                    }

                    // ⭐ 恢复节点运行状态（triggerStatus）
                    Integer triggerStatus = nodeData.getTriggerStatus();
                    Long nodeJobId = nodeData.getJobId();
                    if (triggerStatus == null && nodeJobId != null) {
                        triggerStatus = com.cc.job.gui.util.NodeStatusSyncManager.getInstance()
                                .getCachedStatus(nodeJobId);
                        if (triggerStatus != null) {
                            logger.debug("ℹ️ 使用缓存的节点运行状态: {} -> {}", text, triggerStatus);
                        }
                    }
                    if (triggerStatus != null) {
                        node.updateStatusByCode(triggerStatus);
                        if (nodeJobId != null) {
                            com.cc.job.gui.util.NodeStatusSyncManager.getInstance()
                                    .rememberStatus(nodeJobId, triggerStatus);
                        }
                        logger.debug("✅ 恢复节点运行状态: {} -> {}", text, triggerStatus);
                        log("✅ 恢复节点运行状态: " + text + " -> " + triggerStatus);
                    }

                    // 设置位置
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
                            // 如果没有位置信息，使用默认布局
                            int index = nodeDataList.indexOf(nodeData);
                            node.setLayoutX(100 + (index % 3) * 250);
                            node.setLayoutY(100 + (index / 3) * 200);
                        }
                    }

                    // 设置节点类型显示
                    String mappedType = mapNodeType(nodeData.getType(), nodeData.getProperties());
                    node.setType(mappedType);

                    // 添加节点到画布
                    addNode(node, false);
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
                            NodeConnection edge = addConnection(sourceNode, sourceConnector, targetNode, targetConnector, false);
                            if (edge != null) {
                                edge.setEdgeId(edgeData.getId());
                            }
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
        logger.debug("📊 所有边的运行状态已更新: {}", running ? "运行中" : "停止");
    }
    
    /**
     * 根据jobId更新节点状态
     * @param jobId 任务ID
     * @param statusCode 状态码：0=失败, 1=成功, 2=运行中
     */
    public void updateNodeStatusByJobId(Long jobId, Integer statusCode) {
        if (jobId == null || statusCode == null) {
            logger.warn("⚠️ 参数无效: jobId={}, statusCode={}", jobId, statusCode);
            return;
        }
        
        logger.debug("🔍 开始查找节点: jobId={}, statusCode={}", jobId, statusCode);
        logger.debug("📋 画布中共有 {} 个节点", nodes.size());
        
        boolean found = false;
        for (ProcessNode node : nodes) {
            Long nodeJobId = node.getJobId();
            logger.debug("   → 检查节点: {}, jobId={}", node.getJobHandlerName(), nodeJobId);
            
            if (nodeJobId != null && nodeJobId.equals(jobId)) {
                logger.debug("✅ 找到匹配的节点: {} (jobId={})", node.getJobHandlerName(), jobId);
                logger.debug("   当前状态: {}", node.getStatus());
                logger.debug("   即将更新为状态码: {}", statusCode);
                
                node.updateStatusByCode(statusCode);
                
                logger.debug("✅ 节点状态已更新: jobId={}, statusCode={}", jobId, statusCode);
                logger.debug("   更新后状态: {}", node.getStatus());
                
                // ⭐ 添加到批量更新队列（不立即调用后端）
                com.cc.job.gui.util.NodeStatusSyncManager.getInstance().addPendingUpdate(jobId, statusCode);
                
                found = true;
                break; // 找到节点后更新并退出
            }
        }
        
        if (!found) {
            logger.warn("⚠️ 未找到jobId={}的节点", jobId);
            logger.debug("📋 画布中的节点jobId列表:");
            for (ProcessNode node : nodes) {
                logger.debug("   - {}: jobId={}", node.getJobHandlerName(), node.getJobId());
            }
        }
    }
    
    /**
     * 同步所有待更新的节点状态到数据库
     * 在页面切换、任务完成等时机调用
     */
    public void syncPendingNodeStatus() {
        com.cc.job.gui.util.NodeStatusSyncManager.getInstance().syncNow();
    }

    public void syncPendingNodeStatusBlocking() {
        com.cc.job.gui.util.NodeStatusSyncManager.getInstance().syncNowBlocking();
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
            ProcessNode sourceNode = connection.getSourceNode();
            ProcessNode targetNode = connection.getTargetNode();
            
            // 如果源节点和目标节点都被选中，则边也被选中
            if (selectedNodes.contains(sourceNode) && selectedNodes.contains(targetNode)) {
                selectedConnections.add(connection);
                connection.setSelected(true);
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
    
}

