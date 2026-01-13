package com.cc.job.gui.model;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 条件节点容器（if条件节点）：支持条件表达式，可以包含其他节点
 */
public class ConditionNode extends StackPane {

    private final String nodeId;
    private String conditionName;
    
    // 条件表达式相关
    private String conditionExpression; // 条件表达式
    private ExpressionType expressionType = ExpressionType.SIMPLE; // 表达式类型：简单或脚本
    
    // 条件节点类型
    private ConditionType conditionType = ConditionType.IF; // 条件节点类型：IF、WHILE、FOREACH等
    
    public enum ExpressionType {
        SIMPLE,  // 简单表达式，如：变量 > 100
        SCRIPT   // 脚本表达式，支持复杂逻辑
    }
    
    public enum ConditionType {
        IF,      // if条件节点
        WHILE,   // while循环节点
        FOREACH  // foreach循环节点
    }
    
    /**
     * 获取条件节点类型对应的图标
     */
    public static Feather getIconForType(ConditionType type) {
        switch (type) {
            case IF:
                return Feather.CHECK_CIRCLE;
            case WHILE:
                return Feather.REPEAT;
            case FOREACH:
                return Feather.LIST;
            default:
                return Feather.CODE;
        }
    }
    
    private final Rectangle frame;
    private final Label titleLabel;
    private final VBox header;
    private final Label toggleBtn;
    private final Label zoomLabel; // 缩放比例显示
    private final Pane contentLayer;
    // 连接点层
    private final Pane connectorPane = new Pane();
    private final Circle topConnector = new Circle(5, Color.web("#E0E7FF"));
    private final Circle bottomConnector = new Circle(5, Color.web("#E0E7FF"));
    private final Circle leftConnector = new Circle(5, Color.web("#E0E7FF"));
    private final Circle rightConnector = new Circle(5, Color.web("#E0E7FF"));
    // 四个角的调整大小控制点
    private final Circle topLeftResizeHandle = new Circle(6, Color.web("#F59E0B"));
    private final Circle topRightResizeHandle = new Circle(6, Color.web("#F59E0B"));
    private final Circle bottomLeftResizeHandle = new Circle(6, Color.web("#F59E0B"));
    private final Circle bottomRightResizeHandle = new Circle(6, Color.web("#F59E0B"));
    private boolean expanded = true;
    private ContextMenu contextMenu;
    private Runnable onExpand; // 扩展回调
    private Runnable onDelete; // 删除回调
    private Runnable onEditCondition; // 编辑条件回调
    private Runnable onSizeChanged; // 大小改变回调
    private Runnable onDragFinished; // 拖拽结束回调

    private final List<ProcessNode> innerNodes = new ArrayList<>();
    private final List<ProcessNode> managedCanvasNodes = new ArrayList<>();
    private final List<ConditionNode> managedConditionNodes = new ArrayList<>(); // 管理的条件节点
    private final List<NodeConnection> managedConnections = new ArrayList<>();
    private final Map<ProcessNode, double[]> baseRelativePos = new HashMap<>();
    private final Map<ProcessNode, Consumer<ProcessNode>> originalPositionCallbacks = new HashMap<>();
    private boolean isContainerDragging = false;
    private boolean isResizing = false;
    private double originX = 0;
    private double originY = 0;
    private double zoom = 1.0;
    
    // ⭐ 新增：父容器引用（用于嵌套条件节点向上通知）
    private ConditionNode parentContainer = null;

    public ConditionNode(String nodeId, String conditionName, ConditionType conditionType) {
        this.nodeId = nodeId;
        this.conditionName = conditionName != null ? conditionName : "条件节点";
        this.conditionType = conditionType != null ? conditionType : ConditionType.IF;

        setPickOnBounds(false);
        setMouseTransparent(false);

        // 条件节点使用不同的颜色主题（橙色系）
        frame = new Rectangle(320, 200);
        frame.setArcWidth(12);
        frame.setArcHeight(12);
        frame.setFill(Color.web("#FFF7ED", 0.85)); // 浅橙色背景
        frame.setStroke(Color.web("#F59E0B")); // 橙色边框
        frame.setStrokeWidth(2);
        frame.setMouseTransparent(false);
        frame.setPickOnBounds(true);
        // ⭐ 在frame上添加单击事件，因为frame会拦截点击事件
        frame.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                // 单击时触发编辑条件
                if (onEditCondition != null) {
                    onEditCondition.run();
                    e.consume();
                }
            }
        });
        // 阴影效果
        javafx.scene.effect.DropShadow ds = new javafx.scene.effect.DropShadow();
        ds.setRadius(8);
        ds.setOffsetX(0);
        ds.setOffsetY(2);
        ds.setColor(Color.web("#FED7AA", 0.55)); // 橙色阴影
        frame.setEffect(ds);

        header = new VBox();
        header.setPadding(new Insets(6, 10, 6, 10));
        header.setStyle("-fx-background-color: rgba(245,158,11,0.15); -fx-background-radius: 10 10 0 0;"); // 橙色背景

        // 顶部栏：条件图标 + 标题 + 缩放 + 展开/收起按钮
        HBox headerBar = new HBox(8);
        headerBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // 条件图标（根据类型选择）
        FontIcon conditionIcon = new FontIcon(getIconForType(this.conditionType));
        conditionIcon.setIconSize(16);
        conditionIcon.setIconColor(Color.web("#F59E0B"));
        
        titleLabel = new Label(conditionName);
        titleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #92400E;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleBtn = new Label("-");
        toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #92400E; -fx-background-color: rgba(245,158,11,0.15); -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;");
        toggleBtn.setOnMouseClicked(e -> {
            toggle();
            e.consume();
        });
        toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #F59E0B; -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));
        toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #92400E; -fx-background-color: rgba(245,158,11,0.15); -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));

        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #F59E0B; -fx-background-color: rgba(245,158,11,0.15); -fx-padding: 2 6 2 6; -fx-background-radius: 4;");

        headerBar.getChildren().addAll(conditionIcon, titleLabel, spacer, zoomLabel, toggleBtn);
        header.getChildren().add(headerBar);

        contentLayer = new Pane();
        contentLayer.setPickOnBounds(false);
        contentLayer.setStyle("-fx-background-color: transparent;");

        // 连接点层
        connectorPane.setPickOnBounds(false);
        connectorPane.setMouseTransparent(false);
        for (Circle c : new Circle[]{topConnector, bottomConnector, leftConnector, rightConnector}) {
            c.setRadius(5);
            c.setFill(Color.web("#F59E0B")); // 橙色连接点
            c.setStroke(Color.WHITE);
            c.setStrokeWidth(2);
            c.setVisible(false);
            c.setMouseTransparent(false);
            c.setCursor(Cursor.CROSSHAIR);
            c.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 0.5);");
            c.setOnMouseEntered(ev -> {
                c.setFill(Color.web("#FBBF24")); // 浅橙色
                c.setCursor(Cursor.CROSSHAIR);
            });
            c.setOnMouseExited(ev -> {
                c.setFill(Color.web("#F59E0B"));
            });
        }

        VBox container = new VBox();
        container.getChildren().addAll(header, contentLayer);
        container.setPickOnBounds(false);
        container.setMouseTransparent(false);

        setupResizeHandles();

        getChildren().addAll(frame, container, connectorPane);
        getChildren().addAll(topLeftResizeHandle, topRightResizeHandle, bottomLeftResizeHandle, bottomRightResizeHandle);

        topLeftResizeHandle.toFront();
        topRightResizeHandle.toFront();
        bottomLeftResizeHandle.toFront();
        bottomRightResizeHandle.toFront();

        enableDrag();

        // ⭐ 添加单击事件：单击条件节点时打开编辑对话框（作为备用，frame会优先拦截）
//        this.setOnMouseClicked(e -> {
//            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
//                // 单击时触发编辑条件
//                if (onEditCondition != null) {
//                    onEditCondition.run();
//                    e.consume();
//                }
//            }
//        });

        header.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                toggle();
                e.consume();
            }
        });

        zoomLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                setZoom(1.0);
                e.consume();
            }
        });

        addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() && expanded) {
                double delta = e.getDeltaY() > 0 ? 0.1 : -0.1;
                setZoom(zoom + delta);
                e.consume();
            }
        });

        setupContextMenu();

        updateFrameSize();

        layoutConnectors();
        setConnectorVisible(false);
        this.setOnMouseEntered(e -> {
            setConnectorVisible(true);
            if (expanded) {
                topLeftResizeHandle.setVisible(true);
                topRightResizeHandle.setVisible(true);
                bottomLeftResizeHandle.setVisible(true);
                bottomRightResizeHandle.setVisible(true);
            }
        });
        this.setOnMouseExited(e -> {
            setConnectorVisible(false);
            topLeftResizeHandle.setVisible(false);
            topRightResizeHandle.setVisible(false);
            bottomLeftResizeHandle.setVisible(false);
            bottomRightResizeHandle.setVisible(false);
        });
    }

    private void layoutConnectors() {
        connectorPane.prefWidthProperty().bind(frame.widthProperty());
        connectorPane.prefHeightProperty().bind(frame.heightProperty());
        connectorPane.minWidthProperty().bind(frame.widthProperty());
        connectorPane.minHeightProperty().bind(frame.heightProperty());
        connectorPane.maxWidthProperty().bind(frame.widthProperty());
        connectorPane.maxHeightProperty().bind(frame.heightProperty());

        topConnector.layoutXProperty().bind(frame.widthProperty().divide(2));
        topConnector.layoutYProperty().set(0);

        bottomConnector.layoutXProperty().bind(frame.widthProperty().divide(2));
        bottomConnector.layoutYProperty().bind(frame.heightProperty());

        leftConnector.layoutXProperty().set(0);
        leftConnector.layoutYProperty().bind(frame.heightProperty().divide(2));

        rightConnector.layoutXProperty().bind(frame.widthProperty());
        rightConnector.layoutYProperty().bind(frame.heightProperty().divide(2));

        if (!connectorPane.getChildren().contains(topConnector)) {
            connectorPane.getChildren().addAll(topConnector, bottomConnector, leftConnector, rightConnector);
        }

        updateResizeHandlesPosition();

        topLeftResizeHandle.toFront();
        topRightResizeHandle.toFront();
        bottomLeftResizeHandle.toFront();
        bottomRightResizeHandle.toFront();
    }

    private void setConnectorVisible(boolean visible) {
        topConnector.setVisible(visible);
        bottomConnector.setVisible(visible);
        leftConnector.setVisible(visible);
        rightConnector.setVisible(visible);
        topConnector.setManaged(visible);
        bottomConnector.setManaged(visible);
        leftConnector.setManaged(visible);
        rightConnector.setManaged(visible);

        topLeftResizeHandle.setVisible(visible && expanded);
        topRightResizeHandle.setVisible(visible && expanded);
        bottomLeftResizeHandle.setVisible(visible && expanded);
        bottomRightResizeHandle.setVisible(visible && expanded);
        topLeftResizeHandle.setManaged(visible && expanded);
        topRightResizeHandle.setManaged(visible && expanded);
        bottomLeftResizeHandle.setManaged(visible && expanded);
        bottomRightResizeHandle.setManaged(visible && expanded);
    }

    private void setupResizeHandles() {
        for (Circle handle : new Circle[]{
            topLeftResizeHandle, topRightResizeHandle, bottomLeftResizeHandle, bottomRightResizeHandle
        }) {
            handle.setStroke(Color.WHITE);
            handle.setStrokeWidth(2);
            handle.setCursor(Cursor.NW_RESIZE);
            handle.setVisible(false);
            handle.setManaged(false);
            handle.setPickOnBounds(true);
            handle.setMouseTransparent(false);

            handle.setOnMouseEntered(e -> {
                handle.setFill(Color.web("#D97706"));
                handle.setRadius(7);
            });
            handle.setOnMouseExited(e -> {
                handle.setFill(Color.web("#F59E0B"));
                handle.setRadius(6);
            });

            handle.setOnContextMenuRequested(e -> {
                if (contextMenu != null) {
                    MenuItem expandMenuItem = (MenuItem) contextMenu.getItems().get(0);
                    if (expandMenuItem != null) {
                        expandMenuItem.setText(expanded ? "收起" : "展开");
                    }
                    contextMenu.show(this, e.getScreenX(), e.getScreenY());
                }
                e.consume();
            });
        }

        topLeftResizeHandle.setCursor(Cursor.NW_RESIZE);
        topRightResizeHandle.setCursor(Cursor.NE_RESIZE);
        bottomLeftResizeHandle.setCursor(Cursor.SW_RESIZE);
        bottomRightResizeHandle.setCursor(Cursor.SE_RESIZE);

        updateResizeHandlesPosition();

        setupResizeHandleDrag(topLeftResizeHandle, true, true);
        setupResizeHandleDrag(topRightResizeHandle, false, true);
        setupResizeHandleDrag(bottomLeftResizeHandle, true, false);
        setupResizeHandleDrag(bottomRightResizeHandle, false, false);
    }

    private void updateResizeHandlesPosition() {
        double width = frame.getWidth();
        double height = frame.getHeight();

        topLeftResizeHandle.setLayoutX(0);
        topLeftResizeHandle.setLayoutY(0);

        topRightResizeHandle.setLayoutX(width);
        topRightResizeHandle.setLayoutY(0);

        bottomLeftResizeHandle.setLayoutX(0);
        bottomLeftResizeHandle.setLayoutY(height);

        bottomRightResizeHandle.setLayoutX(width);
        bottomRightResizeHandle.setLayoutY(height);
    }

    private void setupResizeHandleDrag(Circle handle, boolean adjustLeft, boolean adjustTop) {
        final double[] dragStart = new double[4];

        handle.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown() && expanded) {
                dragStart[0] = e.getSceneX();
                dragStart[1] = e.getSceneY();
                dragStart[2] = frame.getWidth();
                dragStart[3] = frame.getHeight();
                e.consume();
            }
        });

        handle.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown() && expanded) {
                isResizing = true;
                try {
                    double deltaX = e.getSceneX() - dragStart[0];
                    double deltaY = e.getSceneY() - dragStart[1];

                    double newX = getLayoutX();
                    double newY = getLayoutY();
                    double newWidth = dragStart[2];
                    double newHeight = dragStart[3];

                    if (adjustLeft) {
                        newX = getLayoutX() + deltaX;
                        newWidth = dragStart[2] - deltaX;
                    } else {
                        newWidth = dragStart[2] + deltaX;
                    }

                    if (adjustTop) {
                        newY = getLayoutY() + deltaY;
                        newHeight = dragStart[3] - deltaY;
                    } else {
                        newHeight = dragStart[3] + deltaY;
                    }

                    double[] minSize = calculateMinSize();
                    double minWidth = minSize[0];
                    double minHeight = minSize[1];

                    if (newWidth < minWidth) {
                        if (adjustLeft) {
                            newX = getLayoutX() + (newWidth - minWidth);
                        }
                        newWidth = minWidth;
                    }
                    if (newHeight < minHeight) {
                        if (adjustTop) {
                            newY = getLayoutY() + (newHeight - minHeight);
                        }
                        newHeight = minHeight;
                    }

                    newX = Math.max(0, newX);
                    newY = Math.max(0, newY);

                    setLayoutX(newX);
                    setLayoutY(newY);
                    frame.setWidth(newWidth);
                    frame.setHeight(newHeight);

                    layoutConnectors();
                    updateResizeHandlesPosition();
                    
                    // ⭐ 新增：如果存在父容器，通知父容器也调整大小
                    if (parentContainer != null) {
                        parentContainer.checkAndExpandContainer();
                    }
                    
                    // 触发大小改变回调
                    if (onSizeChanged != null) {
                        onSizeChanged.run();
                    }
                } finally {
                    isResizing = false;
                }
                e.consume();
            }
        });
    }

    private double[] calculateMinSize() {
        double padding = 24;
        double minWidth = 320;
        double minHeight = 160;

        if (!managedCanvasNodes.isEmpty()) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = 0, maxY = 0;
            for (ProcessNode n : managedCanvasNodes) {
                double nodeX = n.getLayoutX();
                double nodeY = n.getLayoutY();
                double nodeWidth = n.getPrefWidth();
                double nodeHeight = n.getPrefHeight();

                minX = Math.min(minX, nodeX);
                minY = Math.min(minY, nodeY);
                maxX = Math.max(maxX, nodeX + nodeWidth);
                maxY = Math.max(maxY, nodeY + nodeHeight);
            }

            double containerX = getLayoutX();
            double containerY = getLayoutY();
            double relativeMinX = minX - containerX;
            double relativeMinY = minY - containerY;
            double relativeMaxX = maxX - containerX;
            double relativeMaxY = maxY - containerY;

            minWidth = Math.max(320, relativeMaxX - relativeMinX + padding * 2);
            minHeight = Math.max(160, relativeMaxY - relativeMinY + padding * 2 + header.getHeight());
        }

        return new double[]{minWidth, minHeight};
    }

    private void setupContextMenu() {
        contextMenu = new ContextMenu();

        MenuItem expandItem = new MenuItem();
        updateExpandMenuItemText(expandItem);
        expandItem.setOnAction(e -> {
            toggle();
            updateExpandMenuItemText(expandItem);
        });

        MenuItem editConditionItem = new MenuItem("编辑条件");
        editConditionItem.setOnAction(e -> {
            if (onEditCondition != null) {
                onEditCondition.run();
            }
        });

        MenuItem zoomInItem = new MenuItem("放大 (+10%)");
        zoomInItem.setOnAction(e -> setZoom(Math.min(2.0, zoom + 0.1)));

        MenuItem zoomOutItem = new MenuItem("缩小 (-10%)");
        zoomOutItem.setOnAction(e -> setZoom(Math.max(0.5, zoom - 0.1)));

        MenuItem resetZoomItem = new MenuItem("重置缩放 (100%)");
        resetZoomItem.setOnAction(e -> setZoom(1.0));

        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        MenuItem deleteItem = new MenuItem("删除节点");
        deleteItem.setStyle("-fx-text-fill: #EF4444;");
        deleteItem.setOnAction(e -> {
            if (onDelete != null) {
                onDelete.run();
            }
        });

        contextMenu.getItems().addAll(expandItem, new SeparatorMenuItem(),
                editConditionItem, new SeparatorMenuItem(),
                zoomInItem, zoomOutItem, resetZoomItem, separator2, deleteItem);

        setOnContextMenuRequested(e -> {
            updateExpandMenuItemText(expandItem);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        frame.setOnContextMenuRequested(e -> {
            updateExpandMenuItemText(expandItem);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        header.setOnContextMenuRequested(e -> {
            updateExpandMenuItemText(expandItem);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        contentLayer.setOnContextMenuRequested(e -> {
            updateExpandMenuItemText(expandItem);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private void updateExpandMenuItemText(MenuItem item) {
        item.setText(expanded ? "收起" : "展开");
    }

    private void enableDrag() {
        final double[] delta = new double[2];
        header.setCursor(Cursor.MOVE);
        header.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                delta[0] = e.getSceneX() - getLayoutX();
                delta[1] = e.getSceneY() - getLayoutY();
                e.consume();
            }
        });
        header.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                isContainerDragging = true;
                try {
                    double nx = Math.max(0, e.getSceneX() - delta[0]);
                    double ny = Math.max(0, e.getSceneY() - delta[1]);
                    double dx = nx - getLayoutX();
                    double dy = ny - getLayoutY();
                    setLayoutX(nx);
                    setLayoutY(ny);
                    
                    // ⭐ 修复：节点在contentLayer中时，会自动跟随条件节点移动
                    // 因为contentLayer是条件节点的子节点，所以不需要手动更新节点坐标
                    // 但是需要更新originX和originY，以及嵌套的条件节点
                    
                    // 更新originX和originY（用于计算相对位置）
                    if (!managedCanvasNodes.isEmpty()) {
                        originX += dx;
                        originY += dy;
                    }
                    
                    // ⭐ 修复：移动嵌套的条件节点（它们不在contentLayer中，需要手动更新）
                    if (!managedConditionNodes.isEmpty()) {
                        for (ConditionNode cn : managedConditionNodes) {
                            cn.setLayoutX(cn.getLayoutX() + dx);
                            cn.setLayoutY(cn.getLayoutY() + dy);
                        }
                    }
                } finally {
                    isContainerDragging = false;
                }
                e.consume();
            }
        });
        
        // ⭐ 新增：添加鼠标释放事件，用于检测拖拽结束
        header.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY && isContainerDragging) {
                // 拖拽结束，触发回调
                if (onDragFinished != null) {
                    onDragFinished.run();
                }
                isContainerDragging = false;
            }
        });
    }

    public void addInnerNode(ProcessNode node) {
        innerNodes.add(node);
        contentLayer.getChildren().add(node);
        updateFrameSize();
    }

    public void addInnerNodes(List<ProcessNode> nodes) {
        for (ProcessNode n : nodes) {
            addInnerNode(n);
        }
    }

    public List<ProcessNode> getInnerNodes() {
        return new ArrayList<>(innerNodes);
    }

    public List<ProcessNode> getManagedCanvasNodes() {
        return new ArrayList<>(managedCanvasNodes);
    }

    public List<ConditionNode> getManagedConditionNodes() {
        return new ArrayList<>(managedConditionNodes);
    }

    public void bindCanvasNodes(List<ProcessNode> nodesOnCanvas) {
        // ⭐ 修复：先移除旧的节点从contentLayer
        for (ProcessNode oldNode : managedCanvasNodes) {
            if (oldNode != null) {
                Consumer<ProcessNode> originalCallback = originalPositionCallbacks.get(oldNode);
                if (originalCallback != null) {
                    oldNode.setOnPositionChanged(originalCallback);
                } else {
                    oldNode.setOnPositionChanged(null);
                }
                // 从contentLayer中移除
                contentLayer.getChildren().remove(oldNode);
            }
        }
        originalPositionCallbacks.clear();

        managedCanvasNodes.clear();
        if (nodesOnCanvas != null) {
            managedCanvasNodes.addAll(nodesOnCanvas);
        }

        // ⭐ 修复：将新节点添加到contentLayer，并正确转换坐标
        // 节点在contentLayer中时，坐标应该是相对于contentLayer的（即相对于条件节点内容区域的）
        for (ProcessNode node : managedCanvasNodes) {
            if (node != null) {
                // ⭐ 修复：如果节点还没有添加到contentLayer，需要转换坐标
                // 节点当前的坐标是绝对坐标（相对于画布的），需要转换为相对于contentLayer的坐标
                boolean wasInContentLayer = contentLayer.getChildren().contains(node);
                
                if (!wasInContentLayer) {
                    // 获取节点的绝对坐标（相对于画布的）
                    double absoluteX = node.getLayoutX();
                    double absoluteY = node.getLayoutY();
                    
                    // 获取条件节点的绝对坐标
                    double containerX = this.getLayoutX();
                    double containerY = this.getLayoutY();
                    
                    // 获取header的高度（contentLayer在header下方）
                    double headerHeight = header.getHeight();
                    
                    // 计算相对于contentLayer的坐标
                    // contentLayer位于条件节点的(0, headerHeight)位置
                    double relativeX = absoluteX - containerX;
                    double relativeY = absoluteY - containerY - headerHeight;
                    
                    // 将节点添加到contentLayer
                    contentLayer.getChildren().add(node);
                    
                    // 设置相对位置（相对于contentLayer）
                    node.setLayoutX(relativeX);
                    node.setLayoutY(relativeY);
                    
                    // 确保节点可见
                    node.setVisible(true);
                    node.setManaged(true);
                } else {
                    // 节点已经在contentLayer中，确保它被包含
                    if (!contentLayer.getChildren().contains(node)) {
                        contentLayer.getChildren().add(node);
                    }
                    // 确保节点可见
                    node.setVisible(true);
                    node.setManaged(true);
                }
                
                // 设置位置改变回调
                node.setOnPositionChanged(n -> {
                    // ⭐ 修复：限制节点在条件节点内的移动范围
                    // 节点在contentLayer中，坐标是相对坐标，应该限制在条件节点范围内
                    double containerWidth = frame.getWidth();
                    double containerHeight = frame.getHeight();
                    double headerHeight = header.getHeight();
                    double padding = 24;
                    
                    // 限制节点位置在条件节点范围内（考虑padding）
                    double maxX = containerWidth - padding - n.getPrefWidth();
                    double maxY = containerHeight - headerHeight - padding - n.getPrefHeight();
                    
                    double nodeX = n.getLayoutX();
                    double nodeY = n.getLayoutY();
                    boolean positionAdjusted = false;
                    
                    // 如果节点超出范围，调整位置
                    if (nodeX < padding) {
                        n.setLayoutX(padding);
                        positionAdjusted = true;
                    } else if (nodeX > maxX) {
                        n.setLayoutX(maxX);
                        positionAdjusted = true;
                    }
                    
                    if (nodeY < padding) {
                        n.setLayoutY(padding);
                        positionAdjusted = true;
                    } else if (nodeY > maxY) {
                        n.setLayoutY(maxY);
                        positionAdjusted = true;
                    }
                    
                    // ⭐ 修复：只有在位置被调整时才延迟调用checkAndExpandContainer
                    // 如果节点位置没有超出范围，不需要检查扩展
                    if (positionAdjusted) {
                        // 延迟调用checkAndExpandContainer，避免在节点移动过程中频繁更新
                        javafx.application.Platform.runLater(() -> {
                            // 检查是否需要扩展容器（只扩展右下角）
                            checkAndExpandContainer();
                        });
                    } else {
                        // 节点位置正常，只检查是否需要扩展容器（不限制位置时）
                        javafx.application.Platform.runLater(() -> {
                            checkAndExpandContainer();
                        });
                    }
                });
            }
        }

        if (!managedCanvasNodes.isEmpty()) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            for (ProcessNode n : managedCanvasNodes) {
                // ⭐ 修复：节点在contentLayer中，坐标已经是相对坐标
                minX = Math.min(minX, n.getLayoutX());
                minY = Math.min(minY, n.getLayoutY());
            }
            // ⭐ 修复：如果minX或minY为负数，说明节点在条件节点左上角之外
            // 应该将originX和originY设置为0，而不是负数，避免后续计算错误
            originX = Math.max(0, minX);
            originY = Math.max(0, minY);
            baseRelativePos.clear();
            for (ProcessNode n : managedCanvasNodes) {
                baseRelativePos.put(n, new double[]{n.getLayoutX() - originX, n.getLayoutY() - originY});
            }
            updateFrameFromManagedNodes();
        } else {
            updateFrameFromManagedNodes();
        }
    }

    public void bindConditionNodes(List<ConditionNode> conditionNodes) {
        // ⭐ 修复：先清除旧嵌套条件节点的父容器引用
        for (ConditionNode oldCn : managedConditionNodes) {
            if (oldCn != null) {
                oldCn.parentContainer = null;
                oldCn.setOnSizeChanged(null);
            }
        }
        
        managedConditionNodes.clear();
        if (conditionNodes != null) {
            managedConditionNodes.addAll(conditionNodes);
            
            // ⭐ 新增：为每个嵌套的条件节点设置父容器引用和大小改变回调
            for (ConditionNode cn : managedConditionNodes) {
                if (cn != null) {
                    cn.parentContainer = this;
                    // 当嵌套容器大小改变时，触发父容器的checkAndExpandContainer
                    cn.setOnSizeChanged(() -> {
                        if (!isContainerDragging && !isResizing && expanded) {
                            checkAndExpandContainer();
                        }
                    });
                }
            }
        }
        updateFrameSize();
    }
    
    /**
     * ⭐ 新增：获取父容器引用（用于测试和调试）
     */
    public ConditionNode getParentContainer() {
        return parentContainer;
    }
    
    /**
     * ⭐ 新增：设置父容器引用（用于外部设置）
     */
    public void setParentContainer(ConditionNode parent) {
        this.parentContainer = parent;
    }

    private void checkAndExpandContainer() {
        // ⭐ 修复：如果容器为空（既没有普通节点也没有嵌套条件节点），则不调整
        if (isContainerDragging || isResizing || (!expanded) || 
            (managedCanvasNodes.isEmpty() && managedConditionNodes.isEmpty())) {
            return;
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        boolean hasNodes = false;
        
        // 计算普通节点的边界（节点在contentLayer中，坐标是相对坐标）
        for (ProcessNode n : managedCanvasNodes) {
            if (n == null) continue;
            hasNodes = true;
            double nodeX = n.getLayoutX();
            double nodeY = n.getLayoutY();
            double nodeWidth = n.getPrefWidth();
            double nodeHeight = n.getPrefHeight();

            minX = Math.min(minX, nodeX);
            minY = Math.min(minY, nodeY);
            maxX = Math.max(maxX, nodeX + nodeWidth);
            maxY = Math.max(maxY, nodeY + nodeHeight);
        }
        
        // ⭐ 修复：计算嵌套条件节点的边界
        // 嵌套的条件节点不在contentLayer中，它们的坐标是绝对坐标（相对于画布的）
        // 需要转换为相对于contentLayer的坐标
        for (ConditionNode cn : managedConditionNodes) {
            if (cn != null && cn.isExpanded()) {
                hasNodes = true;
                // 嵌套条件节点的绝对坐标
                double cnAbsoluteX = cn.getLayoutX();
                double cnAbsoluteY = cn.getLayoutY();
                double cnWidth = cn.getFrame().getWidth();
                double cnHeight = cn.getFrame().getHeight();
                
                // 转换为相对于contentLayer的坐标
                double containerX = getLayoutX();
                double containerY = getLayoutY();
                double headerHeight = header.getHeight();
                double cnRelativeX = cnAbsoluteX - containerX;
                double cnRelativeY = cnAbsoluteY - containerY - headerHeight;

                minX = Math.min(minX, cnRelativeX);
                minY = Math.min(minY, cnRelativeY);
                maxX = Math.max(maxX, cnRelativeX + cnWidth);
                maxY = Math.max(maxY, cnRelativeY + cnHeight);
            }
        }

        // ⭐ 修复：如果没有节点，不调整
        if (!hasNodes || minX == Double.MAX_VALUE || maxX == Double.MIN_VALUE) {
            return;
        }

        // ⭐ 修复：minX和minY是相对于contentLayer的坐标
        // 如果为负数，说明节点在条件节点左上角之外，应该限制节点移动而不是扩大条件节点
        // 这里只处理节点超出右下角的情况，不处理左上角的情况
        double containerWidth = frame.getWidth();
        double containerHeight = frame.getHeight();

        double padding = 24;
        // 只计算右下角的扩展需求
        double requiredMaxX = maxX + padding;
        double requiredMaxY = maxY + padding;

        boolean needUpdate = false;
        double newWidth = containerWidth;
        double newHeight = containerHeight;

        // ⭐ 修复：只扩展右下角，不移动条件节点位置
        // 如果节点在左上角之外（minX < 0 或 minY < 0），应该限制节点移动，而不是扩大条件节点
        if (requiredMaxX > containerWidth) {
            newWidth = Math.max(newWidth, requiredMaxX);
            needUpdate = true;
        }

        if (requiredMaxY > containerHeight - header.getHeight()) {
            newHeight = Math.max(newHeight, requiredMaxY + header.getHeight());
            needUpdate = true;
        }

        if (needUpdate) {
            // ⭐ 修复：限制条件节点的最大尺寸，避免异常扩大
            double maxWidth = 2000; // 最大宽度
            double maxHeight = 2000; // 最大高度
            newWidth = Math.max(320, Math.min(newWidth, maxWidth));
            newHeight = Math.max(160, Math.min(newHeight, maxHeight));

            // ⭐ 修复：不移动条件节点位置，只调整大小
            frame.setWidth(newWidth);
            frame.setHeight(newHeight);

            layoutConnectors();
            updateResizeHandlesPosition();

            // ⭐ 修复：不应该在checkAndExpandContainer中更新originX、originY和baseRelativePos
            // 这些值应该只在bindCanvasNodes时设置一次，避免移动一个节点时影响其他节点的位置
            // originX = Math.max(0, minX); // 移除：不应该在这里更新
            // originY = Math.max(0, minY); // 移除：不应该在这里更新
            // baseRelativePos.clear(); // 移除：不应该在这里更新
            // for (ProcessNode n : managedCanvasNodes) {
            //     baseRelativePos.put(n, new double[]{n.getLayoutX() - originX, n.getLayoutY() - originY});
            // }
            
            // ⭐ 新增：如果存在父容器，通知父容器也调整大小
            if (parentContainer != null) {
                parentContainer.checkAndExpandContainer();
            }
            
            // ⭐ 新增：触发大小改变回调
            if (onSizeChanged != null) {
                onSizeChanged.run();
            }
        }
    }

    public void bindConnections(List<NodeConnection> connections) {
        managedConnections.clear();
        if (connections != null) {
            managedConnections.addAll(connections);
            ensureConnectionsOnTop();
        }
    }

    public List<NodeConnection> getManagedConnections() {
        return new ArrayList<>(managedConnections);
    }

    private void ensureConnectionsOnTop() {
        for (NodeConnection conn : managedConnections) {
            if (conn != null && conn.getParent() != null) {
                conn.toFront();
            }
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void expand() {
        if (!expanded) {
            expanded = true;
            contentLayer.setVisible(true);
            contentLayer.setManaged(true);
            // ⭐ 修复：确保所有管理的节点都可见，并且已添加到contentLayer
            for (ProcessNode n : managedCanvasNodes) {
                if (n != null) {
                    // 确保节点在contentLayer中
                    if (!contentLayer.getChildren().contains(n)) {
                        contentLayer.getChildren().add(n);
                    }
                    n.setVisible(true);
                    n.setManaged(true);
                }
            }
            for (ConditionNode cn : managedConditionNodes) {
                if (cn != null) {
                    // 确保嵌套的条件节点也在contentLayer中
                    if (!contentLayer.getChildren().contains(cn)) {
                        contentLayer.getChildren().add(cn);
                    }
                    cn.setVisible(true);
                    cn.setManaged(true);
                }
            }
            for (NodeConnection c : managedConnections) {
                c.setVisible(true);
                c.setManaged(true);
            }
            ensureConnectionsOnTop();
            updateFrameSize();
            updateResizeHandlesPosition();
            toggleBtn.setText("-");
            if (onExpand != null) {
                onExpand.run();
            }
        }
    }

    public void collapse() {
        if (expanded) {
            expanded = false;
            contentLayer.setVisible(false);
            contentLayer.setManaged(false);
            for (ProcessNode n : managedCanvasNodes) {
                n.setVisible(false);
                n.setManaged(false);
            }
            for (ConditionNode cn : managedConditionNodes) {
                cn.setVisible(false);
                cn.setManaged(false);
            }
            for (NodeConnection c : managedConnections) {
                c.setVisible(false);
                c.setManaged(false);
            }
            frame.setWidth(Math.max(160, titleLabel.getText().length() * 12));
            frame.setHeight(36);
            toggleBtn.setText("+");
        }
    }

    public void toggle() {
        if (expanded) collapse(); else expand();
    }

    public void setOnExpand(Runnable onExpand) {
        this.onExpand = onExpand;
    }

    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnEditCondition(Runnable onEditCondition) {
        this.onEditCondition = onEditCondition;
    }

    public void setOnSizeChanged(Runnable onSizeChanged) {
        this.onSizeChanged = onSizeChanged;
    }
    
    public void setOnDragFinished(Runnable onDragFinished) {
        this.onDragFinished = onDragFinished;
    }
    
    public double getContainerWidth() {
        return frame.getWidth();
    }
    
    public double getContainerHeight() {
        return frame.getHeight();
    }
    
    public void setSize(double width, double height) {
        double minWidth = 320;
        double minHeight = 160;
        double actualWidth = Math.max(minWidth, width);
        double actualHeight = Math.max(minHeight, height);
        frame.setWidth(actualWidth);
        frame.setHeight(actualHeight);
        layoutConnectors();
        updateResizeHandlesPosition();
        if (onSizeChanged != null) {
            onSizeChanged.run();
        }
    }

    private void updateFrameSize() {
        if (!expanded) {
            collapse();
            return;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = 0, maxY = 0;
        for (ProcessNode n : innerNodes) {
            minX = Math.min(minX, n.getLayoutX());
            minY = Math.min(minY, n.getLayoutY());
            maxX = Math.max(maxX, n.getLayoutX() + n.getPrefWidth());
            maxY = Math.max(maxY, n.getLayoutY() + n.getPrefHeight());
        }
        if (innerNodes.isEmpty()) {
            if (!updateFrameFromManagedNodes()) {
                frame.setWidth(320);
                frame.setHeight(200);
            }
        } else {
            double padding = 24;
            frame.setWidth(Math.max(320, (maxX - minX) + padding * 2));
            frame.setHeight(Math.max(160, (maxY - minY) + padding * 2 + header.getHeight()));
        }
    }

    private boolean updateFrameFromManagedNodes() {
        if (managedCanvasNodes.isEmpty() && managedConditionNodes.isEmpty()) {
            return false;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = 0, maxY = 0;

        for (ProcessNode n : managedCanvasNodes) {
            minX = Math.min(minX, n.getLayoutX());
            minY = Math.min(minY, n.getLayoutY());
            maxX = Math.max(maxX, n.getLayoutX() + n.getPrefWidth());
            maxY = Math.max(maxY, n.getLayoutY() + n.getPrefHeight());
        }

        // ⭐ 修复：嵌套条件节点的坐标需要转换为相对于contentLayer的坐标
        for (ConditionNode cn : managedConditionNodes) {
            // 嵌套条件节点的绝对坐标
            double cnAbsoluteX = cn.getLayoutX();
            double cnAbsoluteY = cn.getLayoutY();
            double cnWidth = cn.getFrame().getWidth();
            double cnHeight = cn.getFrame().getHeight();
            
            // 转换为相对于contentLayer的坐标
            double containerX = getLayoutX();
            double containerY = getLayoutY();
            double headerHeight = header.getHeight();
            double cnRelativeX = cnAbsoluteX - containerX;
            double cnRelativeY = cnAbsoluteY - containerY - headerHeight;
            
            minX = Math.min(minX, cnRelativeX);
            minY = Math.min(minY, cnRelativeY);
            maxX = Math.max(maxX, cnRelativeX + cnWidth);
            maxY = Math.max(maxY, cnRelativeY + cnHeight);
        }

        javafx.scene.Parent parent = getParent();
        if (parent instanceof javafx.scene.layout.Pane pane) {
            for (javafx.scene.Node child : pane.getChildren()) {
                if (child instanceof ConditionNode nestedContainer && child != this) {
                    double nestedX = nestedContainer.getLayoutX();
                    double nestedY = nestedContainer.getLayoutY();
                    double nestedWidth = nestedContainer.getFrame().getWidth();
                    double nestedHeight = nestedContainer.getFrame().getHeight();

                    boolean isNested = false;
                    for (ProcessNode n : managedCanvasNodes) {
                        double nodeX = n.getLayoutX();
                        double nodeY = n.getLayoutY();
                        double nodeWidth = n.getPrefWidth();
                        double nodeHeight = n.getPrefHeight();

                        if (nestedX >= nodeX - 50 && nestedX <= nodeX + nodeWidth + 50 &&
                                nestedY >= nodeY - 50 && nestedY <= nodeY + nodeHeight + 50) {
                            isNested = true;
                            break;
                        }
                    }

                    if (isNested) {
                        minX = Math.min(minX, nestedX);
                        minY = Math.min(minY, nestedY);
                        maxX = Math.max(maxX, nestedX + nestedWidth);
                        maxY = Math.max(maxY, nestedY + nestedHeight);
                    }
                }
            }
        }

        double padding = 24;
        // ⭐ 修复：只调整frame的大小，不移动条件节点的位置
        // minX和minY是相对于contentLayer的坐标，不应该用来移动条件节点的绝对位置
        // 如果minX或minY为负数，说明节点在条件节点左上角之外，应该限制节点移动，而不是移动条件节点
        double newWidth = Math.max(320, (maxX - minX) + padding * 2);
        double newHeight = Math.max(160, (maxY - minY) + padding * 2 + header.getHeight());
        
        // ⭐ 修复：限制条件节点的最大尺寸，避免异常扩大
        double maxWidth = 2000;
        double maxHeight = 2000;
        newWidth = Math.min(newWidth, maxWidth);
        newHeight = Math.min(newHeight, maxHeight);
        
        frame.setWidth(newWidth);
        frame.setHeight(newHeight);
        
        // ⭐ 修复：不移动条件节点位置，保持原有位置
        // setLayoutX(Math.max(0, minX - padding)); // 移除：不应该移动条件节点位置
        // setLayoutY(Math.max(0, minY - (padding + header.getHeight()))); // 移除：不应该移动条件节点位置
        
        return true;
    }

    public Rectangle getFrame() {
        return frame;
    }

    public void setZoom(double value) {
        double clamped = Math.max(0.5, Math.min(2.0, value));
        if (Math.abs(clamped - this.zoom) < 1e-6) {
            return;
        }
        this.zoom = clamped;

        int zoomPercent = (int) Math.round(zoom * 100);
        zoomLabel.setText(zoomPercent + "%");

        if (!managedCanvasNodes.isEmpty() && !baseRelativePos.isEmpty()) {
            for (ProcessNode n : managedCanvasNodes) {
                double[] base = baseRelativePos.get(n);
                if (base != null) {
                    double nx = originX + base[0] * zoom;
                    double ny = originY + base[1] * zoom;
                    n.setLayoutX(nx);
                    n.setLayoutY(ny);
                }
            }
            updateFrameFromManagedNodes();
        }
    }

    public double getZoom() {
        return zoom;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
        if (titleLabel != null) {
            titleLabel.setText(conditionName);
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public ExpressionType getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(ExpressionType expressionType) {
        this.expressionType = expressionType;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionType conditionType) {
        this.conditionType = conditionType != null ? conditionType : ConditionType.IF;
        // 更新图标显示
        if (header != null && header.getChildren().size() > 0) {
            HBox headerBar = (HBox) header.getChildren().get(0);
            if (headerBar.getChildren().size() > 0 && headerBar.getChildren().get(0) instanceof FontIcon) {
                FontIcon icon = (FontIcon) headerBar.getChildren().get(0);
                icon.setIconCode(getIconForType(this.conditionType));
            }
        }
    }

    public Pane getConnectorPane() {
        return connectorPane;
    }

    public Circle getTopConnector() {
        return topConnector;
    }

    public Circle getBottomConnector() {
        return bottomConnector;
    }

    public Circle getLeftConnector() {
        return leftConnector;
    }

    public Circle getRightConnector() {
        return rightConnector;
    }
}

