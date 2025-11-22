package com.cc.job.gui.model;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

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
    private final javafx.scene.shape.Circle topConnector = new javafx.scene.shape.Circle(5, Color.web("#E0E7FF"));
    private final javafx.scene.shape.Circle bottomConnector = new javafx.scene.shape.Circle(5, Color.web("#E0E7FF"));
    private final javafx.scene.shape.Circle leftConnector = new javafx.scene.shape.Circle(5, Color.web("#E0E7FF"));
    private final javafx.scene.shape.Circle rightConnector = new javafx.scene.shape.Circle(5, Color.web("#E0E7FF"));
    private boolean expanded = true;
    private ContextMenu contextMenu;
    private Runnable onExpand; // 扩展回调（用于懒加载）
    private Runnable onDelete; // 删除回调
    
    private final List<ProcessNode> innerNodes = new ArrayList<>();
    private final List<ProcessNode> managedCanvasNodes = new ArrayList<>();
    private final List<NodeConnection> managedConnections = new ArrayList<>();
    private final java.util.Map<ProcessNode, double[]> baseRelativePos = new java.util.HashMap<>();
    private double originX = 0; // 受管节点的参考左上角（画布坐标）
    private double originY = 0;
    private double zoom = 1.0;  // 缩放倍数（针对受管节点的相对定位）
    
    public GroupContainer(String nodeId, Long groupId, String groupName) {
        this.nodeId = nodeId;
        this.groupId = groupId;
        this.groupName = groupName;
        
        setPickOnBounds(false);
        
        frame = new Rectangle(320, 200);
        frame.setArcWidth(12);
        frame.setArcHeight(12);
        frame.setFill(Color.web("#FFFFFF", 0.85));
        frame.setStroke(Color.web("#6366F1"));
        frame.setStrokeWidth(2);
        // 柔和阴影，提升层次
        javafx.scene.effect.DropShadow ds = new javafx.scene.effect.DropShadow();
        ds.setRadius(8);
        ds.setOffsetX(0);
        ds.setOffsetY(2);
        ds.setColor(Color.web("#C7D2FE", 0.55));
        frame.setEffect(ds);
        
        header = new VBox();
        header.setPadding(new Insets(6, 10, 6, 10));
        header.setStyle("-fx-background-color: rgba(99,102,241,0.10); -fx-background-radius: 10 10 0 0;");
        
        // 顶部栏：左标题 + 右上角 +/- 按钮
        javafx.scene.layout.HBox headerBar = new javafx.scene.layout.HBox();
        headerBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        titleLabel = new Label(groupName);
        titleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #3730A3;");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        toggleBtn = new Label("-");
        toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3730A3; -fx-background-color: rgba(99,102,241,0.12); -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;");
        toggleBtn.setOnMouseClicked(e -> {
            toggle();
            e.consume();
        });
        toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #6366F1; -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));
        toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3730A3; -fx-background-color: rgba(99,102,241,0.12); -fx-padding: 0 6 0 6; -fx-background-radius: 8; -fx-cursor: hand;"));
        
        // 缩放比例显示标签
        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6366F1; -fx-background-color: rgba(99,102,241,0.12); -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
        
        headerBar.getChildren().addAll(titleLabel, spacer, zoomLabel, toggleBtn);
        header.getChildren().add(headerBar);
        
        contentLayer = new Pane();
        contentLayer.setPickOnBounds(false);
        contentLayer.setStyle("-fx-background-color: transparent;");
        
        // 连接点层置顶、不可拦截事件
        connectorPane.setPickOnBounds(false);
        connectorPane.setMouseTransparent(false);
        for (javafx.scene.shape.Circle c : new javafx.scene.shape.Circle[]{topConnector, bottomConnector, leftConnector, rightConnector}) {
            c.setRadius(5); // 与普通节点一致
            c.setFill(Color.web("#8B5CF6")); // 与普通节点默认紫色一致
            c.setStroke(Color.WHITE);
            c.setStrokeWidth(2);
            c.setVisible(false); // 初始隐藏，悬停显示
            c.setMouseTransparent(false);
            c.setCursor(javafx.scene.Cursor.CROSSHAIR);
            // 轻微投影
            c.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 0.5);");
            c.setOnMouseEntered(ev -> {
                c.setFill(Color.web("#A78BFA")); // 浅紫
                c.setCursor(javafx.scene.Cursor.CROSSHAIR);
            });
            c.setOnMouseExited(ev -> {
                c.setFill(Color.web("#8B5CF6"));
            });
        }
        
        VBox container = new VBox();
        container.getChildren().addAll(header, contentLayer);
        container.setPickOnBounds(false);
        
        // 作为顶层叠加：frame < container < connectorPane（与普通节点一致的覆盖顺序）
        getChildren().addAll(frame, container, connectorPane);
        
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
        addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
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
        this.setOnMouseEntered(e -> setConnectorVisible(true));
        this.setOnMouseExited(e -> setConnectorVisible(false));
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
            if (onDelete != null) {
                onDelete.run();
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
        managedCanvasNodes.clear();
        if (nodesOnCanvas != null) {
            managedCanvasNodes.addAll(nodesOnCanvas);
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
     * 绑定管理这些节点之间的连线，折叠时隐藏、展开时显示
     */
    public void bindConnections(List<NodeConnection> connections) {
        managedConnections.clear();
        if (connections != null) {
            managedConnections.addAll(connections);
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
            updateFrameSize();
            toggleBtn.setText("-"); // 展开时显示“-”（点击收起）
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
    public javafx.scene.shape.Circle getTopConnector() { return topConnector; }
    public javafx.scene.shape.Circle getBottomConnector() { return bottomConnector; }
    public javafx.scene.shape.Circle getLeftConnector() { return leftConnector; }
    public javafx.scene.shape.Circle getRightConnector() { return rightConnector; }
}


