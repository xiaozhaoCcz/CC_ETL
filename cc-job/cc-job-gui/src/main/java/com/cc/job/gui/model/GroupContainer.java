package com.cc.job.gui.model;

import com.cc.job.gui.util.StyleUtil;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 任务组容器（可折叠）：大框是任务组，内部小框是子任务节点
 */
public class GroupContainer extends StackPane {

    private final String groupName;
    private final Long groupId;
    private final String nodeId;

    private final Rectangle frame;
    private final Label titleLabel;
    private final VBox header;
    private final Label toggleBtn;
    private final Label zoomLabel; // 缩放比例显示
    private final Pane contentLayer;
    // 新增：用于承载四个连接点的层
    private final Pane connectorPane = new Pane();
    private final Circle topConnector = new Circle(5, Color.web("#E0E7FF"));
    private final Circle bottomConnector = new Circle(5, Color.web("#E0E7FF"));
    private final Circle leftConnector = new Circle(5, Color.web("#E0E7FF"));
    private final Circle rightConnector = new Circle(5, Color.web("#E0E7FF"));
    // ⭐ 新增：四个角的调整大小控制点
    private final Circle topLeftResizeHandle = new Circle(6, Color.web("#2563EB"));
    private final Circle topRightResizeHandle = new Circle(6, Color.web("#2563EB"));
    private final Circle bottomLeftResizeHandle = new Circle(6, Color.web("#2563EB"));
    private final Circle bottomRightResizeHandle = new Circle(6, Color.web("#2563EB"));
    private boolean expanded = true;
    private ContextMenu contextMenu;
    private Runnable onExpand; // 扩展回调（用于懒加载）
    private Runnable onDelete; // 删除回调

    private final List<ProcessNode> innerNodes = new ArrayList<>();
    private final List<ProcessNode> managedCanvasNodes = new ArrayList<>();
    private final List<NodeConnection> managedConnections = new ArrayList<>();
    private final Map<ProcessNode, double[]> baseRelativePos = new HashMap<>();
    // ⭐ 新增：保存子节点的原始位置变化回调，以便在容器更新后恢复
    private final Map<ProcessNode, Consumer<ProcessNode>> originalPositionCallbacks = new HashMap<>();
    // ⭐ 新增：标志，用于防止在容器移动时触发自动扩大检查
    private boolean isContainerDragging = false;
    // ⭐ 新增：标志，用于防止在调整大小时触发自动扩大检查
    private boolean isResizing = false;
    private double originX = 0; // 受管节点的参考左上角（画布坐标）
    private double originY = 0;
    private double zoom = 1.0;  // 缩放倍数（针对受管节点的相对定位）

    public GroupContainer(String nodeId, Long groupId, String groupName) {
        this.nodeId = nodeId;
        this.groupId = groupId;
        this.groupName = groupName;

        setPickOnBounds(false);
        // ⭐ 修复：确保容器本身可以接收鼠标事件，以便触发右键菜单
        setMouseTransparent(false);

        frame = new Rectangle(320, 200);
        frame.setArcWidth(12);
        frame.setArcHeight(12);
        frame.setFill(Color.web("#FFFFFF", 0.85));
        frame.setStroke(Color.web("#2563EB"));
        frame.setStrokeWidth(2);
        // ⭐ 修复：确保frame可以接收鼠标事件，以便触发右键菜单
        frame.setMouseTransparent(false);
        frame.setPickOnBounds(true);
        // 柔和阴影，提升层次
        javafx.scene.effect.DropShadow ds = new javafx.scene.effect.DropShadow();
        ds.setRadius(8);
        ds.setOffsetX(0);
        ds.setOffsetY(2);
        ds.setColor(Color.web("#C7D2FE", 0.55));
        frame.setEffect(ds);

        header = new VBox();
        header.setPadding(new Insets(6, 10, 6, 10));
        boolean dark = StyleUtil.isDarkTheme();
        if (dark) {
            header.setStyle("-fx-background-color: #2D2D30; -fx-background-radius: 10 10 0 0;");
        } else {
            header.setStyle("-fx-background-color: rgba(99,102,241,0.10); -fx-background-radius: 10 10 0 0;");
        }

        HBox headerBar = new HBox();
        headerBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        titleLabel = new Label(groupName);
        titleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + (dark ? "#569CD6" : "#3730A3") + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleBtn = new Label("-");
        if (dark) {
            toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #569CD6; -fx-background-color: #3C3C3C; -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;");
            toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #007ACC; -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));
            toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #569CD6; -fx-background-color: #3C3C3C; -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));
        } else {
            toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3730A3; -fx-background-color: rgba(99,102,241,0.12); -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;");
            toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #2563EB; -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));
            toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3730A3; -fx-background-color: rgba(99,102,241,0.12); -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));
        }
        toggleBtn.setOnMouseClicked(e -> {
            toggle();
            e.consume();
        });

        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + (dark ? "#569CD6" : "#2563EB") + "; -fx-background-color: " + (dark ? "#3C3C3C" : "rgba(37,99,235,0.12)") + "; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");

        headerBar.getChildren().addAll(titleLabel, spacer, zoomLabel, toggleBtn);
        header.getChildren().add(headerBar);

        contentLayer = new Pane();
        contentLayer.setPickOnBounds(false);
        contentLayer.setStyle("-fx-background-color: transparent;");

        // 连接点层置顶、不可拦截事件
        connectorPane.setPickOnBounds(false);
        connectorPane.setMouseTransparent(false);
        for (Circle c : new Circle[]{topConnector, bottomConnector, leftConnector, rightConnector}) {
            c.setRadius(5); // 与普通节点一致
            c.setFill(Color.web("#2563EB")); // 与普通节点默认蓝色一致
            c.setStroke(Color.WHITE);
            c.setStrokeWidth(2);
            c.setVisible(false); // 初始隐藏，悬停显示
            c.setMouseTransparent(false);
            c.setCursor(Cursor.CROSSHAIR);
            // 轻微投影
            c.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 0.5);");
            c.setOnMouseEntered(ev -> {
                c.setFill(Color.web("#93C5FD")); // 浅蓝
                c.setCursor(Cursor.CROSSHAIR);
            });
            c.setOnMouseExited(ev -> {
                c.setFill(Color.web("#2563EB"));
            });
        }

        VBox container = new VBox();
        container.getChildren().addAll(header, contentLayer);
        container.setPickOnBounds(false);
        // ⭐ 修复：确保container可以接收鼠标事件，以便触发右键菜单
        container.setMouseTransparent(false);

        // ⭐ 新增：初始化调整大小控制点
        setupResizeHandles();

        // 作为顶层叠加：frame < container < connectorPane < resizeHandles（调整大小控制点在最上层）
        getChildren().addAll(frame, container, connectorPane);
        getChildren().addAll(topLeftResizeHandle, topRightResizeHandle, bottomLeftResizeHandle, bottomRightResizeHandle);

        // ⭐ 修复：确保调整大小控制点始终在最上层，并且可以接收鼠标事件
        topLeftResizeHandle.toFront();
        topRightResizeHandle.toFront();
        bottomLeftResizeHandle.toFront();
        bottomRightResizeHandle.toFront();

        // 拖拽移动容器
        enableDrag();

        // 双击标题切换展开/收起
        header.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                toggle();
                e.consume();
            }
        });

        // 双击缩放标签重置缩放
        zoomLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                setZoom(1.0);
                e.consume();
            }
        });

        // Ctrl+滚轮缩放（在整个容器上）
        addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() && expanded) {
                double delta = e.getDeltaY() > 0 ? 0.1 : -0.1;
                setZoom(zoom + delta);
                e.consume();
            }
        });

        // 右键菜单
        setupContextMenu();

        updateFrameSize();

        // 初始化连接点位置绑定到外框
        layoutConnectors();
        // 默认隐藏，悬停显示（与普通节点一致）
        setConnectorVisible(false);
        this.setOnMouseEntered(e -> {
            setConnectorVisible(true);
            // ⭐ 新增：鼠标进入时显示调整大小控制点
            if (expanded) {
                topLeftResizeHandle.setVisible(true);
                topRightResizeHandle.setVisible(true);
                bottomLeftResizeHandle.setVisible(true);
                bottomRightResizeHandle.setVisible(true);
            }
        });
        this.setOnMouseExited(e -> {
            setConnectorVisible(false);
            // ⭐ 新增：鼠标离开时隐藏调整大小控制点
            topLeftResizeHandle.setVisible(false);
            topRightResizeHandle.setVisible(false);
            bottomLeftResizeHandle.setVisible(false);
            bottomRightResizeHandle.setVisible(false);
        });
    }

    private void layoutConnectors() {
        // connectorPane 尺寸与 frame 一致覆盖
        connectorPane.prefWidthProperty().bind(frame.widthProperty());
        connectorPane.prefHeightProperty().bind(frame.heightProperty());
        connectorPane.minWidthProperty().bind(frame.widthProperty());
        connectorPane.minHeightProperty().bind(frame.heightProperty());
        connectorPane.maxWidthProperty().bind(frame.widthProperty());
        connectorPane.maxHeightProperty().bind(frame.heightProperty());

        // 连接点相对 frame 四边定位（与普通节点一致：位于边缘正中）
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

        // ⭐ 新增：更新调整大小控制点位置
        updateResizeHandlesPosition();

        // ⭐ 修复：确保调整大小控制点始终在最上层
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

        // ⭐ 新增：调整大小控制点也跟随连接点的显示/隐藏
        topLeftResizeHandle.setVisible(visible && expanded);
        topRightResizeHandle.setVisible(visible && expanded);
        bottomLeftResizeHandle.setVisible(visible && expanded);
        bottomRightResizeHandle.setVisible(visible && expanded);
        topLeftResizeHandle.setManaged(visible && expanded);
        topRightResizeHandle.setManaged(visible && expanded);
        bottomLeftResizeHandle.setManaged(visible && expanded);
        bottomRightResizeHandle.setManaged(visible && expanded);
    }

    /**
     * ⭐ 新增：设置调整大小控制点
     */
    private void setupResizeHandles() {
        // 设置控制点样式
        for (Circle handle : new Circle[]{
            topLeftResizeHandle, topRightResizeHandle, bottomLeftResizeHandle, bottomRightResizeHandle
        }) {
            handle.setStroke(Color.WHITE);
            handle.setStrokeWidth(2);
            handle.setCursor(Cursor.NW_RESIZE);
            handle.setVisible(false);
            handle.setManaged(false);
            handle.setPickOnBounds(true);
            handle.setMouseTransparent(false); // ⭐ 修复：确保可以接收鼠标事件

            // 鼠标悬停效果
            handle.setOnMouseEntered(e -> {
                handle.setFill(Color.web("#1D4ED8"));
                handle.setRadius(7);
            });
            handle.setOnMouseExited(e -> {
                handle.setFill(Color.web("#2563EB"));
                handle.setRadius(6);
            });

            // ⭐ 修复：为调整大小控制点添加右键事件处理，确保右键菜单可以显示
            handle.setOnContextMenuRequested(e -> {
                // 直接显示容器的右键菜单
                if (contextMenu != null) {
                    // 更新菜单项文本
                    MenuItem expandMenuItem = (MenuItem) contextMenu.getItems().get(0);
                    if (expandMenuItem != null) {
                        expandMenuItem.setText(expanded ? "收起" : "展开");
                    }
                    // 显示菜单
                    contextMenu.show(this, e.getScreenX(), e.getScreenY());
                }
                e.consume();
            });
        }

        // 设置不同角的鼠标样式
        topLeftResizeHandle.setCursor(Cursor.NW_RESIZE);
        topRightResizeHandle.setCursor(Cursor.NE_RESIZE);
        bottomLeftResizeHandle.setCursor(Cursor.SW_RESIZE);
        bottomRightResizeHandle.setCursor(Cursor.SE_RESIZE);

        // 绑定控制点位置到frame的四个角
        updateResizeHandlesPosition();

        // 为每个控制点添加拖动事件
        setupResizeHandleDrag(topLeftResizeHandle, true, true);
        setupResizeHandleDrag(topRightResizeHandle, false, true);
        setupResizeHandleDrag(bottomLeftResizeHandle, true, false);
        setupResizeHandleDrag(bottomRightResizeHandle, false, false);
    }

    /**
     * ⭐ 新增：更新调整大小控制点的位置
     */
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

    /**
     * ⭐ 新增：为调整大小控制点设置拖动事件
     * @param handle 控制点
     * @param adjustLeft 是否调整左边界
     * @param adjustTop 是否调整上边界
     */
    private void setupResizeHandleDrag(Circle handle, boolean adjustLeft, boolean adjustTop) {
        final double[] dragStart = new double[4]; // [startX, startY, startWidth, startHeight]

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

                    // 计算新的容器位置和大小
                    double newX = getLayoutX();
                    double newY = getLayoutY();
                    double newWidth = dragStart[2];
                    double newHeight = dragStart[3];

                    // 根据拖动的角调整相应的边界
                    if (adjustLeft) {
                        // 调整左边界
                        newX = getLayoutX() + deltaX;
                        newWidth = dragStart[2] - deltaX;
                    } else {
                        // 调整右边界
                        newWidth = dragStart[2] + deltaX;
                    }

                    if (adjustTop) {
                        // 调整上边界
                        newY = getLayoutY() + deltaY;
                        newHeight = dragStart[3] - deltaY;
                    } else {
                        // 调整下边界
                        newHeight = dragStart[3] + deltaY;
                    }

                    // 计算包含所有子节点的最小尺寸
                    double[] minSize = calculateMinSize();
                    double minWidth = minSize[0];
                    double minHeight = minSize[1];

                    // 确保容器大小不小于最小尺寸
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

                    // 确保容器位置不小于0
                    newX = Math.max(0, newX);
                    newY = Math.max(0, newY);

                    // 更新容器位置和大小
                    setLayoutX(newX);
                    setLayoutY(newY);
                    frame.setWidth(newWidth);
                    frame.setHeight(newHeight);

                    // 更新连接点和调整大小控制点的位置
                    layoutConnectors();
                    updateResizeHandlesPosition();
                } finally {
                    isResizing = false;
                }
                e.consume();
            }
        });
    }

    /**
     * ⭐ 新增：计算包含所有子节点的最小尺寸
     * @return [minWidth, minHeight]
     */
    private double[] calculateMinSize() {
        double padding = 24;
        double minWidth = 320; // 默认最小宽度
        double minHeight = 160; // 默认最小高度

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

            // 计算相对于容器位置的尺寸
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

        MenuItem zoomInItem = new MenuItem("放大 (+10%)");
        zoomInItem.setOnAction(e -> setZoom(Math.min(2.0, zoom + 0.1)));

        MenuItem zoomOutItem = new MenuItem("缩小 (-10%)");
        zoomOutItem.setOnAction(e -> setZoom(Math.max(0.5, zoom - 0.1)));

        MenuItem resetZoomItem = new MenuItem("重置缩放 (100%)");
        resetZoomItem.setOnAction(e -> setZoom(1.0));

        // 分隔符
        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        // 删除节点
        MenuItem deleteItem = new MenuItem("删除节点");
        deleteItem.setStyle("-fx-text-fill: #EF4444;"); // 红色文字
        deleteItem.setOnAction(e -> {
            System.out.println("删除节点菜单项被点击，onDelete回调: " + (onDelete != null ? "已设置" : "为null"));
            if (onDelete != null) {
                System.out.println("执行onDelete回调");
                onDelete.run();
            } else {
                System.err.println("错误：onDelete回调为null，无法删除任务组节点！");
            }
        });

        contextMenu.getItems().addAll(expandItem, new SeparatorMenuItem(),
                zoomInItem, zoomOutItem, resetZoomItem, separator2, deleteItem);

        // 右键显示菜单时更新展开/收起文本
        setOnContextMenuRequested(e -> {
            updateExpandMenuItemText(expandItem);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // ⭐ 修复：为frame添加右键事件转发，确保点击frame时也能显示右键菜单
        frame.setOnContextMenuRequested(e -> {
            updateExpandMenuItemText(expandItem);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // ⭐ 修复：为header添加右键事件转发（但header已经有双击事件，需要确保右键也能工作）
        header.setOnContextMenuRequested(e -> {
            updateExpandMenuItemText(expandItem);
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // ⭐ 修复：为contentLayer添加右键事件转发
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
                // ⭐ 修复：设置标志，防止在容器移动时触发自动扩大检查
                isContainerDragging = true;
                try {
                    double nx = Math.max(0, e.getSceneX() - delta[0]);
                    double ny = Math.max(0, e.getSceneY() - delta[1]);
                    double dx = nx - getLayoutX();
                    double dy = ny - getLayoutY();
                    // 移动容器
                    setLayoutX(nx);
                    setLayoutY(ny);
                    // 连带移动受管节点
                    if (!managedCanvasNodes.isEmpty()) {
                        for (ProcessNode n : managedCanvasNodes) {
                            n.setLayoutX(n.getLayoutX() + dx);
                            n.setLayoutY(n.getLayoutY() + dy);
                        }
                        // 更新参考原点
                        originX += dx;
                        originY += dy;
                    }
                } finally {
                    isContainerDragging = false;
                }
                e.consume();
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

    /**
     * ⭐ 新增：获取管理的画布节点列表（用于嵌套任务组）
     */
    public List<ProcessNode> getManagedCanvasNodes() {
        return new ArrayList<>(managedCanvasNodes);
    }

    /**
     * 绑定管理一组已经在画布上的节点（不作为子节点添加，只负责折叠显示与测量外框）
     */
    public void bindCanvasNodes(List<ProcessNode> nodesOnCanvas) {
        // ⭐ 修复：先清除旧节点的位置变化监听器，恢复原始回调
        for (ProcessNode oldNode : managedCanvasNodes) {
            if (oldNode != null) {
                Consumer<ProcessNode> originalCallback = originalPositionCallbacks.get(oldNode);
                if (originalCallback != null) {
                    oldNode.setOnPositionChanged(originalCallback);
                } else {
                    oldNode.setOnPositionChanged(null);
                }
            }
        }
        originalPositionCallbacks.clear();

        managedCanvasNodes.clear();
        if (nodesOnCanvas != null) {
            managedCanvasNodes.addAll(nodesOnCanvas);
        }

        // ⭐ 新增：为每个子节点添加位置变化监听器，当节点移动时自动更新容器大小
        for (ProcessNode node : managedCanvasNodes) {
            if (node != null) {
                // 由于 ProcessNode 没有 getOnPositionChanged 方法，我们直接设置新的回调
                // 新的回调会检查容器边界并自动扩大容器
                node.setOnPositionChanged(n -> {
                    // 检查子节点是否超出容器边界，如果超出则自动扩大容器
                    checkAndExpandContainer();
                });
            }
        }

        // 计算参考原点与相对位置
        if (!managedCanvasNodes.isEmpty()) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            for (ProcessNode n : managedCanvasNodes) {
                minX = Math.min(minX, n.getLayoutX());
                minY = Math.min(minY, n.getLayoutY());
            }
            originX = minX;
            originY = minY;
            baseRelativePos.clear();
            for (ProcessNode n : managedCanvasNodes) {
                baseRelativePos.put(n, new double[]{n.getLayoutX() - originX, n.getLayoutY() - originY});
            }
            updateFrameFromManagedNodes();
        } else {
            updateFrameFromManagedNodes();
        }
    }

    /**
     * ⭐ 新增：检查子节点是否超出容器边界，如果超出则自动扩大容器
     */
    private void checkAndExpandContainer() {
        // ⭐ 修复：如果容器正在拖动或调整大小，不触发自动扩大检查（避免无限循环）
        if (isContainerDragging || isResizing || managedCanvasNodes.isEmpty() || !expanded) {
            return;
        }

        // 计算所有子节点的边界
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

        // 计算当前容器的边界
        double containerX = getLayoutX();
        double containerY = getLayoutY();
        double containerWidth = frame.getWidth();
        double containerHeight = frame.getHeight();

        // 计算需要的padding
        double padding = 24;
        double requiredMinX = minX - padding;
        double requiredMinY = minY - padding - header.getHeight();

        // 检查是否需要调整容器
        boolean needUpdate = false;
        double newX = containerX;
        double newY = containerY;
        double newWidth = containerWidth;
        double newHeight = containerHeight;

        // 如果子节点超出了容器的左边界，需要向左扩展
        if (requiredMinX < containerX) {
            newX = Math.max(0, requiredMinX);
            newWidth = containerWidth + (containerX - newX);
            needUpdate = true;
        }

        // 如果子节点超出了容器的上边界，需要向上扩展
        if (requiredMinY < containerY) {
            newY = Math.max(0, requiredMinY);
            newHeight = containerHeight + (containerY - newY);
            needUpdate = true;
        }

        // 如果子节点超出了容器的右边界，需要向右扩展
        if (maxX + padding > containerX + containerWidth) {
            newWidth = Math.max(newWidth, maxX + padding - newX);
            needUpdate = true;
        }

        // 如果子节点超出了容器的下边界，需要向下扩展
        if (maxY + padding > containerY + containerHeight) {
            newHeight = Math.max(newHeight, maxY + padding - newY);
            needUpdate = true;
        }

        // 如果容器需要更新，调整容器的大小和位置
        if (needUpdate) {
            // 确保最小尺寸
            newWidth = Math.max(320, newWidth);
            newHeight = Math.max(160, newHeight);

            // 更新容器位置和大小
            setLayoutX(newX);
            setLayoutY(newY);
            frame.setWidth(newWidth);
            frame.setHeight(newHeight);

            // 更新连接点位置
            layoutConnectors();
            // ⭐ 新增：更新调整大小控制点位置
            updateResizeHandlesPosition();

            // 更新参考原点
            originX = minX;
            originY = minY;
            baseRelativePos.clear();
            for (ProcessNode n : managedCanvasNodes) {
                baseRelativePos.put(n, new double[]{n.getLayoutX() - originX, n.getLayoutY() - originY});
            }
        }
    }

    /**
     * 绑定管理这些节点之间的连线，折叠时隐藏、展开时显示
     */
    public void bindConnections(List<NodeConnection> connections) {
        managedConnections.clear();
        if (connections != null) {
            managedConnections.addAll(connections);
            // ⭐ 修复：确保任务组容器内的边的Z-order高于容器本身，这样边才能显示和选择
            ensureConnectionsOnTop();
        }
    }

    /**
     * ⭐ 新增：获取管理的连接列表
     */
    public List<NodeConnection> getManagedConnections() {
        return new ArrayList<>(managedConnections);
    }

    /**
     * ⭐ 修复：确保任务组容器内的边的Z-order高于容器本身
     */
    private void ensureConnectionsOnTop() {
        for (NodeConnection conn : managedConnections) {
            if (conn != null && conn.getParent() != null) {
                // 将边移到容器前面
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
            // 还原被管理的画布节点显示
            for (ProcessNode n : managedCanvasNodes) {
                n.setVisible(true);
                n.setManaged(true);
            }
            for (NodeConnection c : managedConnections) {
                c.setVisible(true);
                c.setManaged(true);
            }
            // ⭐ 修复：展开时确保边的Z-order高于容器
            ensureConnectionsOnTop();
            updateFrameSize();
            // ⭐ 新增：展开时更新调整大小控制点位置
            updateResizeHandlesPosition();
            toggleBtn.setText("-"); // 展开时显示"-"（点击收起）
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
            // 折叠时隐藏被管理的画布节点
            for (ProcessNode n : managedCanvasNodes) {
                n.setVisible(false);
                n.setManaged(false);
            }
            for (NodeConnection c : managedConnections) {
                c.setVisible(false);
                c.setManaged(false);
            }
            // 收起后显示为标题条的高度
            frame.setWidth(Math.max(160, titleLabel.getText().length() * 12));
            frame.setHeight(36);
            toggleBtn.setText("+"); // 收起时显示“+”（点击展开）
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
        if (managedCanvasNodes.isEmpty()) {
            return false;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = 0, maxY = 0;

        for (ProcessNode n : managedCanvasNodes) {
            minX = Math.min(minX, n.getLayoutX());
            minY = Math.min(minY, n.getLayoutY());
            maxX = Math.max(maxX, n.getLayoutX() + n.getPrefWidth()); maxY = Math.max(maxY, n.getLayoutY() + n.getPrefHeight()); }

        javafx.scene.Parent parent = getParent();
        if (parent instanceof javafx.scene.layout.Pane pane) {
            for (javafx.scene.Node child : pane.getChildren()) {
                if (child instanceof GroupContainer nestedContainer && child != this) {
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
        frame.setWidth(Math.max(320, (maxX - minX) + padding * 2));
        frame.setHeight(Math.max(160, (maxY - minY) + padding * 2 + header.getHeight()));
        setLayoutX(Math.max(0, minX - padding));
        setLayoutY(Math.max(0, minY - (padding + header.getHeight())));
        return true;
    }

    /**
     * ⭐ 新增：获取框架矩形（用于嵌套任务组的边界计算）
     */
    public Rectangle getFrame() {
        return frame;
    }

    /**
     * 设置缩放倍数（0.5 ~ 2.0），对受管节点相对参考点缩放布局，实现"缩放展开"
     */
    public void setZoom(double value) {
        double clamped = Math.max(0.5, Math.min(2.0, value));
        if (Math.abs(clamped - this.zoom) < 1e-6) {
            return;
        }
        this.zoom = clamped;

        // 更新缩放比例显示
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

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getNodeId() {
        return nodeId;
    }

    // 暴露连接点与承载层，供 NodeCanvas 统一处理
    public Pane getConnectorPane() {
        return connectorPane;
    }
    public Circle getTopConnector() { return topConnector; }
    public Circle getBottomConnector() { return bottomConnector; }
    public Circle getLeftConnector() { return leftConnector; }
    public Circle getRightConnector() { return rightConnector; }
}


