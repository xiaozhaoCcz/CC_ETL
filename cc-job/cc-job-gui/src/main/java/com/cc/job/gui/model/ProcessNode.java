package com.cc.job.gui.model;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.util.function.Consumer;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流程节点类，支持拖拽、右键菜单等功能
 */
public class ProcessNode extends StackPane {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessNode.class);
    
    private String nodeId;
    private Long jobId;  // 任务ID，用于后端保存
    private String jobHandlerName;
    private double dragStartX;
    private double dragStartY;
    private double initialLayoutX;
    private double initialLayoutY;
    
    // 连接点
    private Circle topConnector;
    private Circle bottomConnector;
    private Circle leftConnector;
    private Circle rightConnector;
    private Pane connectorPane; // 独立的连接点容器
    
    // 右键菜单
    private ContextMenu contextMenu;
    
    // 删除回调
    private Runnable onDelete;
    
    // 拖动回调
    private Runnable onDragged;
    private DragFinishedListener dragFinishedListener;
    private Runnable onDragStarted;
    private PositionAdjuster positionAdjuster;
    private Consumer<ProcessNode> onPositionChanged;
    private Runnable onClicked; // 节点点击回调（非拖拽）
    
    // 编辑/复制/详情回调
    private Runnable onEdit;
    private Runnable onCopy;
    private Runnable onShowDetails;
    
    // 禁用/启用节点回调
    private DisableNodeCallback onDisable;


    
    public interface DisableNodeCallback {
        void onDisableNode(Long jobId, boolean isDisabled);
    }
    
    // 节点状态
    private boolean enabled = true;
    private String currentColor = "#8B5CF6"; // 默认紫色
    private String type = "Bean"; // 节点类型：Bean, API, SQL等
    private NodeStatus status = NodeStatus.IDLE; // 节点运行状态
    
    // UI元素引用
    private javafx.scene.shape.Rectangle background;
    private Label typeLabel;    // 类型标签引用
    private Label handlerLabel; // 处理器名称标签引用
    private FontIcon typeIcon;  // 节点类型图标
    private Timeline locateAnimation;
    
    private static final double NODE_WIDTH = 180;
    private static final double NODE_HEIGHT = 80;
    
    public ProcessNode(String nodeId, String jobHandlerName) {
        this.nodeId = nodeId;
        this.jobHandlerName = jobHandlerName;
        
        initializeUI();
        setupDragHandlers();
        setupContextMenu();
    }
    
    public ProcessNode(String nodeId, String jobHandlerName, double x, double y) {
        this(nodeId, jobHandlerName);
        this.setLayoutX(x);
        this.setLayoutY(y);
    }
    
    private void initializeUI() {
        // 设置节点大小
        this.setPrefSize(NODE_WIDTH, NODE_HEIGHT);
        this.setMaxSize(NODE_WIDTH, NODE_HEIGHT);
        this.setMinSize(NODE_WIDTH, NODE_HEIGHT);
        
        // 根据节点类型设置边框颜色
        updateBorderColorByType();
        
        // 创建背景
        background = new javafx.scene.shape.Rectangle(NODE_WIDTH, NODE_HEIGHT);
        background.setFill(Color.WHITE);
        background.setStroke(Color.web(currentColor));
        background.setStrokeWidth(2);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setMouseTransparent(true); // 背景不拦截鼠标事件
        
        // 创建内容容器
        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(10));
        contentBox.setMouseTransparent(true); // 内容不拦截鼠标事件
        
        // 创建类型标签容器（包含图标和文字）
        HBox typeContainer = new HBox(6);
        typeContainer.setAlignment(Pos.CENTER);
        
        // 创建节点类型图标
        typeIcon = getNodeTypeIcon(type);
        if (typeIcon != null) {
            typeIcon.setIconSize(16);
            typeContainer.getChildren().add(typeIcon);
        }
        
        // 类型标签 - 增大字体大小
        typeLabel = new Label(type);
        typeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #374151;");
        
        typeContainer.getChildren().add(typeLabel);
        
        // 任务处理器名称
        handlerLabel = new Label(jobHandlerName);
        handlerLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        handlerLabel.setMaxWidth(NODE_WIDTH - 20);
        
        contentBox.getChildren().addAll(typeContainer, handlerLabel);
        
        // 创建独立的连接点容器
        // 容器与节点大小相同，连接点将定位在边缘外部
        connectorPane = new Pane();
        connectorPane.setPrefSize(NODE_WIDTH, NODE_HEIGHT);
        connectorPane.setMaxSize(NODE_WIDTH, NODE_HEIGHT);
        connectorPane.setMinSize(NODE_WIDTH, NODE_HEIGHT);
        connectorPane.setMouseTransparent(false); // 连接点容器接收鼠标事件
        // 允许子元素超出容器边界显示
        connectorPane.setClip(null);
        
        // 添加阴影效果
        this.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // 按顺序添加：背景 -> 内容 -> 连接点容器
        this.getChildren().addAll(background, contentBox, connectorPane);
        
        // 最后创建连接点并添加到独立容器中
        createConnectors();
    }
    
    private void createConnectors() {
        // 创建四个连接点
        topConnector = createConnector();
        bottomConnector = createConnector();
        leftConnector = createConnector();
        rightConnector = createConnector();
        
        // 设置位置（在Pane中，layoutX/layoutY指定圆心位置）
        // 圆心放在节点边界上，这样连接线会紧贴节点，没有间隙
        
        // 顶部：水平居中，圆心在节点顶边
        topConnector.setLayoutX(NODE_WIDTH / 2);
        topConnector.setLayoutY(0);
        
        // 底部：水平居中，圆心在节点底边
        bottomConnector.setLayoutX(NODE_WIDTH / 2);
        bottomConnector.setLayoutY(NODE_HEIGHT);
        
        // 左侧：圆心在节点左边，垂直居中
        leftConnector.setLayoutX(0);
        leftConnector.setLayoutY(NODE_HEIGHT / 2);
        
        // 右侧：圆心在节点右边，垂直居中
        rightConnector.setLayoutX(NODE_WIDTH);
        rightConnector.setLayoutY(NODE_HEIGHT / 2);
        
        // 添加到独立的连接点容器中
        connectorPane.getChildren().addAll(topConnector, bottomConnector, leftConnector, rightConnector);
        
        // 初始时隐藏
        showConnectors(false);
        
    }
    
    private Circle createConnector() {
        Circle connector = new Circle(5); // 减小半径，使连接点更小巧
        connector.setFill(Color.web(currentColor)); // 使用当前节点类型的颜色
        connector.setStroke(Color.WHITE);
        connector.setStrokeWidth(2);
        connector.setVisible(false); // 初始隐藏
        connector.setMouseTransparent(false); // 必须能接收鼠标事件
        
        // 添加轻微的投影效果
        connector.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 0.5);");
        
        // 鼠标悬停效果 - 只改变颜色，不改变大小
        connector.setOnMouseEntered(e -> {
            // 使用当前颜色的浅色版本
            connector.setFill(Color.web(lightenColor(currentColor)));
            connector.setCursor(Cursor.CROSSHAIR);
            e.consume();
        });
        
        connector.setOnMouseExited(e -> {
            connector.setFill(Color.web(currentColor));
            connector.setCursor(Cursor.DEFAULT);
            e.consume();
        });
        
        return connector;
    }
    
    /**
     * 将颜色变浅（用于悬停效果）
     */
    private String lightenColor(String color) {
        if (color == null || !color.startsWith("#")) {
            return "#A78BFA"; // 默认浅紫色
        }
        // 简单的颜色变浅处理，将RGB值增加
        try {
            int r = Integer.parseInt(color.substring(1, 3), 16);
            int g = Integer.parseInt(color.substring(3, 5), 16);
            int b = Integer.parseInt(color.substring(5, 7), 16);
            
            r = Math.min(255, r + 30);
            g = Math.min(255, g + 30);
            b = Math.min(255, b + 30);
            
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception e) {
            return "#A78BFA"; // 默认浅紫色
        }
    }
    
    private void setupDragHandlers() {
        // 鼠标进入节点
        this.setOnMouseEntered(e -> {
            // 只有当鼠标不在按下状态时才改变光标和显示连接点
            if (!e.isPrimaryButtonDown()) {
                this.setCursor(Cursor.MOVE);
                showConnectors(true);
            }
            e.consume();
        });
        
        // 鼠标离开节点
        this.setOnMouseExited(e -> {
            // 只有当鼠标不在按下状态时才隐藏连接点
            if (!e.isPrimaryButtonDown()) {
                this.setCursor(Cursor.DEFAULT);
                showConnectors(false);
            }
            e.consume();
        });
        
        // 开始拖拽（只处理节点本身，不处理连接点）
        this.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown() && !isConnectorClick(e.getTarget())) {
                initialLayoutX = this.getLayoutX();
                initialLayoutY = this.getLayoutY();
                dragStartX = e.getSceneX() - this.getLayoutX();
                dragStartY = e.getSceneY() - this.getLayoutY();
                this.setCursor(Cursor.CLOSED_HAND);
                this.toFront(); // 拖拽时置于顶层
                if (onDragStarted != null) {
                    onDragStarted.run();
                }
                e.consume();
            }
        });
        
        // 拖拽中
        this.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown() && !isConnectorClick(e.getTarget())) {
                double newX = e.getSceneX() - dragStartX;
                double newY = e.getSceneY() - dragStartY;

                double adjustedX = Math.max(0, newX);
                double adjustedY = Math.max(0, newY);

                if (positionAdjuster != null) {
                    Point2D adjustedPoint = positionAdjuster.adjust(this, adjustedX, adjustedY);
                    if (adjustedPoint != null) {
                        adjustedX = adjustedPoint.getX();
                        adjustedY = adjustedPoint.getY();
                    }
                }

                this.setLayoutX(adjustedX);
                this.setLayoutY(adjustedY);

                if (onPositionChanged != null) {
                    onPositionChanged.accept(this);
                }
                
                // 触发拖动回调
                if (onDragged != null) {
                    onDragged.run();
                }
                
                e.consume();
            }
        });
        
        // 结束拖拽
        this.setOnMouseReleased(e -> {
            if (!isConnectorClick(e.getTarget())) {
                this.setCursor(Cursor.MOVE);
                showConnectors(true); // 保持连接点显示
                if (dragFinishedListener != null) {
                    double newX = this.getLayoutX();
                    double newY = this.getLayoutY();
                    double deltaX = Math.abs(newX - initialLayoutX);
                    double deltaY = Math.abs(newY - initialLayoutY);
                    
                    // 检查是否是拖拽还是点击
                    if (deltaX > 3 || deltaY > 3) {
                        // 拖拽：触发拖拽结束回调
                        dragFinishedListener.onDragFinished(initialLayoutX, initialLayoutY, newX, newY);
                    } else {
                        // 点击：触发点击回调
                        if (onClicked != null) {
                            onClicked.run();
                        }
                    }
                } else {
                    // 如果没有拖拽回调，也检查是否是点击
                    double newX = this.getLayoutX();
                    double newY = this.getLayoutY();
                    double deltaX = Math.abs(newX - initialLayoutX);
                    double deltaY = Math.abs(newY - initialLayoutY);
                    if (deltaX <= 3 && deltaY <= 3 && onClicked != null) {
                        onClicked.run();
                    }
                }
                e.consume();
            }
        });
    }
    
    private void setupContextMenu() {
        contextMenu = new ContextMenu();
        
        // 编辑节点
        MenuItem editItem = new MenuItem("编辑节点");
        editItem.setOnAction(e -> {
            if (onEdit != null) {
                onEdit.run();
            } else {
            }
        });
        
        // 复制节点
        MenuItem copyItem = new MenuItem("复制节点");
        copyItem.setOnAction(e -> {
            if (onCopy != null) {
                onCopy.run();
            }
        });
        
        // 节点详情
        MenuItem detailsItem = new MenuItem("查看详情");
        detailsItem.setOnAction(e -> {
            if (onShowDetails != null) {
                onShowDetails.run();
            }
        });
        
        // 分隔符
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
        // 更改颜色
        Menu colorMenu = new Menu("更改颜色");
        
        MenuItem purpleItem = new MenuItem("紫色 (默认)");
        purpleItem.setOnAction(e -> changeNodeColor("#8B5CF6"));
        
        MenuItem blueItem = new MenuItem("蓝色");
        blueItem.setOnAction(e -> changeNodeColor("#3B82F6"));
        
        MenuItem greenItem = new MenuItem("绿色");
        greenItem.setOnAction(e -> changeNodeColor("#10B981"));
        
        MenuItem orangeItem = new MenuItem("橙色");
        orangeItem.setOnAction(e -> changeNodeColor("#F59E0B"));
        
        MenuItem redItem = new MenuItem("红色");
        redItem.setOnAction(e -> changeNodeColor("#EF4444"));
        
        colorMenu.getItems().addAll(purpleItem, blueItem, greenItem, orangeItem, redItem);
        
        // 禁用/启用节点
        MenuItem toggleItem = new MenuItem("禁用节点");
        toggleItem.setOnAction(e -> {
            toggleNodeEnabled();
            toggleItem.setText(isEnabled() ? "禁用节点" : "启用节点");
        });

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
        
        contextMenu.getItems().addAll(
            editItem,
            copyItem,
            detailsItem,
            separator1,
            colorMenu,
            toggleItem,
            separator2,
            deleteItem
        );
        
        // 右键显示菜单
        this.setOnContextMenuRequested(e -> {
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }
    
    private boolean isConnectorClick(Object target) {
        return target instanceof Circle;
    }
    
    private void showConnectors(boolean show) {
        topConnector.setVisible(show);
        bottomConnector.setVisible(show);
        leftConnector.setVisible(show);
        rightConnector.setVisible(show);
        
        // 确保连接点在最上层
        if (show) {
            topConnector.toFront();
            bottomConnector.toFront();
            leftConnector.toFront();
            rightConnector.toFront();
        }
        
        // 调试信息
    }
    
    // 强制显示连接点（用于测试）
    public void forceShowConnectors() {
        showConnectors(true);
    }
    
    // Getters
    public String getNodeId() {
        return nodeId;
    }
    
    public Long getJobId() {
        return jobId;
    }
    
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
    
    public String getJobHandlerName() {
        return jobHandlerName;
    }
    
    /**
     * 更新节点显示名称
     * @param newName 新的名称
     */
    public void updateJobHandlerName(String newName) {
        if (newName == null) {
            return;
        }
        
        this.jobHandlerName = newName;
        
        // 直接更新标签文本
        if (handlerLabel != null) {
            handlerLabel.setText(newName);
        }
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
    
    public Pane getConnectorPane() {
        return connectorPane;
    }
    
    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
    }
    
    public void setOnDragged(Runnable onDragged) {
        this.onDragged = onDragged;
    }

    public void setOnDragStarted(Runnable onDragStarted) {
        this.onDragStarted = onDragStarted;
    }
    
    public void setOnDragFinished(DragFinishedListener listener) {
        this.dragFinishedListener = listener;
    }

    public void setPositionAdjuster(PositionAdjuster positionAdjuster) {
        this.positionAdjuster = positionAdjuster;
    }
    
    public void setOnPositionChanged(Consumer<ProcessNode> onPositionChanged) {
        this.onPositionChanged = onPositionChanged;
    }
    
    public void setOnClicked(Runnable onClicked) {
        this.onClicked = onClicked;
    }

    public void setOnEdit(Runnable onEdit) {
        this.onEdit = onEdit;
    }
    
    public void setOnCopy(Runnable onCopy) {
        this.onCopy = onCopy;
    }
    
    public void setOnShowDetails(Runnable onShowDetails) {
        this.onShowDetails = onShowDetails;
    }
    
    public void setOnDisable(DisableNodeCallback callback) {
        this.onDisable = callback;
    }
    
    /**
     * 调整拖拽起始点，用于在画布扩展时保持拖拽位置的正确性
     * @param deltaX X方向的偏移量
     * @param deltaY Y方向的偏移量
     */
    public void adjustDragStart(double deltaX, double deltaY) {
        dragStartX += deltaX;
        dragStartY += deltaY;
    }

    public interface DragFinishedListener {
        void onDragFinished(double oldX, double oldY, double newX, double newY);
    }

    @FunctionalInterface
    public interface PositionAdjuster {
        /**
         * 调整节点目标位置，用于在拖拽过程中增加额外逻辑（例如顶部缓冲自动下推其他节点）
         * @param node 当前拖拽的节点
         * @param proposedX 经过基础限制后的 X 坐标
         * @param proposedY 经过基础限制后的 Y 坐标
         * @return 调整后的坐标，如果返回 null 则保留原值
         */
        Point2D adjust(ProcessNode node, double proposedX, double proposedY);
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
        if (typeLabel != null) {
            typeLabel.setText(type);
        }
        // 更新边框颜色和图标
        updateBorderColorByType();
        updateTypeIcon();
    }
    
    /**
     * 根据节点类型更新边框颜色
     */
    private void updateBorderColorByType() {
        if (type == null) {
            currentColor = "#8B5CF6"; // 默认紫色
            return;
        }
        
        currentColor = switch (type) {
            case "Bean" -> "#8B5CF6";      // 紫色
            case "API" -> "#FF6B35";      // 橙色
            case "SQL" -> "#3B82F6";      // 蓝色
            case "Java" -> "#E74C3C";     // 红色
            case "Shell" -> "#4A5568";    // 深灰色
            case "Python" -> "#3776AB";   // 蓝色
            case "PHP" -> "#777BB4";     // 紫色
            case "Node" -> "#339933";     // 绿色
            case "PS" -> "#0078D4";       // 蓝色
            default -> "#8B5CF6";          // 默认紫色
        };
        
        // 如果背景已创建，更新边框颜色
        if (background != null) {
            background.setStroke(Color.web(currentColor));
        }
        
        // 更新连接点颜色
        if (topConnector != null) {
            topConnector.setFill(Color.web(currentColor));
            bottomConnector.setFill(Color.web(currentColor));
            leftConnector.setFill(Color.web(currentColor));
            rightConnector.setFill(Color.web(currentColor));
        }
    }
    
    /**
     * 根据节点类型获取图标
     */
    private FontIcon getNodeTypeIcon(String nodeType) {
        if (nodeType == null) {
            return createNodeIcon(Feather.CPU, "#4B5563", 18);
        }
        
        return switch (nodeType) {
            case "Bean" -> createNodeIcon(Feather.DATABASE, "#4B5563", 18);
            case "API" -> createNodeIcon(Feather.GLOBE, "#EA580C", 18);
            case "SQL" -> createNodeIcon(Feather.SERVER, "#2563EB", 18);
            case "Java" -> createNodeIcon(Feather.CODE, "#DC2626", 18);
            case "Shell" -> createNodeIcon(Feather.TERMINAL, "#475569", 18);
            case "Python" -> createNodeIcon(Feather.FEATHER, "#1D4ED8", 18);
            case "PHP" -> createNodeIcon(Feather.FILE_TEXT, "#6B21A8", 18);
            case "Node" -> createNodeIcon(Feather.PACKAGE, "#15803D", 18);
            case "PS" -> createNodeIcon(Feather.ZAP, "#0369A1", 18);
            default -> createNodeIcon(Feather.CPU, "#4B5563", 18);
        };
    }
    
    /**
     * 创建节点图标
     */
    private FontIcon createNodeIcon(Feather feather, String color, int size) {
        FontIcon icon = new FontIcon(feather);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(color));
        return icon;
    }
    
    /**
     * 更新类型图标
     */
    private void updateTypeIcon() {
        if (typeIcon != null && typeLabel != null) {
            // 获取父容器
            javafx.scene.Node parent = typeIcon.getParent();
            if (parent instanceof HBox) {
                HBox container = (HBox) parent;
                container.getChildren().clear();
                
                // 创建新图标
                FontIcon newIcon = getNodeTypeIcon(type);
                if (newIcon != null) {
                    newIcon.setIconSize(16);
                    container.getChildren().addAll(newIcon, typeLabel);
                    typeIcon = newIcon;
                } else {
                    container.getChildren().add(typeLabel);
                }
            }
        }
    }
    
    /**
     * 更新节点的显示信息（名称和类型）
     * @param newName 新名称
     * @param newType 新类型
     */
    public void updateNodeInfo(String newName, String newType) {
        updateJobHandlerName(newName);
        setType(newType);
    }

    public void playLocateAnimation() {
        if (background == null) {
            return;
        }
        if (locateAnimation != null) {
            locateAnimation.stop();
        }

        Color highlightColor = Color.web("#F97316");
        Color originalColor = Color.web(currentColor);

        locateAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(background.strokeProperty(), highlightColor),
                        new KeyValue(background.strokeWidthProperty(), 3)),
                new KeyFrame(Duration.seconds(0.2),
                        new KeyValue(background.strokeProperty(), originalColor),
                        new KeyValue(background.strokeWidthProperty(), 2))
        );
        locateAnimation.setAutoReverse(true);
        locateAnimation.setCycleCount(4);
        locateAnimation.setOnFinished(e -> {
            background.setStroke(Color.web(currentColor));
            background.setStrokeWidth(2);
        });
        locateAnimation.play();
    }
 
    public double getX() {
        return getLayoutX();
    }
    
    public double getY() {
        return getLayoutY();
    }
    
    /**
     * 更改节点颜色（用于手动更改，如右键菜单）
     */
    private void changeNodeColor(String color) {
        this.currentColor = color;
        // 更新边框颜色
        background.setStroke(Color.web(color));
        // 手动更改颜色时保持白色背景（除非是状态相关的颜色）
        if (status == NodeStatus.IDLE) {
            background.setFill(Color.WHITE);
        }
        // 否则保持当前状态对应的背景颜色
        
        // 更新连接点颜色
        topConnector.setFill(Color.web(color));
        bottomConnector.setFill(Color.web(color));
        leftConnector.setFill(Color.web(color));
        rightConnector.setFill(Color.web(color));
        
    }
    
    /**
     * 根据状态更新背景填充颜色
     * @param newStatus 新的状态
     */
    private void updateBackgroundColorByStatus(NodeStatus newStatus) {
        switch (newStatus) {
            case RUNNING:
                // 运行中：黄色背景
                background.setFill(Color.web("#F59E0B"));
                break;
            case SUCCESS:
                // 成功：绿色背景
                background.setFill(Color.web("#10B981"));
                break;
            case FAILED:
                // 失败：红色背景
                background.setFill(Color.web("#EF4444"));
                break;
            case IDLE:
            default:
                // 空闲：白色背景
                background.setFill(Color.WHITE);
                break;
        }
    }
    
    /**
     * 切换节点启用/禁用状态
     */
    private void toggleNodeEnabled() {
        boolean newEnabledState = !enabled;
        
        // 先更新UI状态（乐观更新）
        enabled = newEnabledState;
        
        if (enabled) {
            // 启用状态：根据当前状态恢复颜色
            changeNodeColor(currentColor);
            this.setOpacity(1.0);
        } else {
            // 禁用状态：灰色半透明
            background.setFill(Color.web("#F3F4F6"));
            background.setStroke(Color.web("#9CA3AF"));
            background.setStrokeWidth(2);
            this.setOpacity(0.6);
        }
        
        // 调用回调，通知外部（如调用后端API）
        // 回调在后台线程中执行，如果失败，会恢复UI状态
        if (onDisable != null && jobId != null) {
            onDisable.onDisableNode(jobId, !enabled);
        }
    }
    
    /**
     * 恢复节点的启用/禁用状态（用于回调失败时恢复）
     * @param targetEnabledState 目标启用状态
     */
    public void restoreEnabledState(boolean targetEnabledState) {
        enabled = targetEnabledState;
        
        if (enabled) {
            // 启用状态：根据当前状态恢复颜色
            changeNodeColor(currentColor);
            this.setOpacity(1.0);
        } else {
            // 禁用状态：灰色半透明
            background.setFill(Color.web("#F3F4F6"));
            background.setStroke(Color.web("#9CA3AF"));
            background.setStrokeWidth(2);
            this.setOpacity(0.6);
        }
    }
    
    /**
     * 检查节点是否启用
     */
    private boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取当前颜色
     */
    public String getCurrentColor() {
        return currentColor;
    }
    
    /**
     * 获取节点启用状态
     */
    public boolean getEnabled() {
        return enabled;
    }
    
    /**
     * 节点状态枚举
     */
    public enum NodeStatus {
        IDLE,      // 空闲（默认紫色）
        RUNNING,   // 运行中（黄色）
        SUCCESS,   // 成功（绿色）
        FAILED     // 失败（红色）
    }
    
    /**
     * 更新节点状态（根据运行状态改变颜色）
     * @param newStatus 新的状态
     */
    public void updateStatus(NodeStatus newStatus) {
        NodeStatus oldStatus = this.status;
        this.status = newStatus;
        
        String statusColor;
        switch (newStatus) {
            case RUNNING:
                statusColor = "#F59E0B"; // 黄色（运行中）
                break;
            case SUCCESS:
                statusColor = "#10B981"; // 绿色（成功）
                break;
            case FAILED:
                statusColor = "#EF4444"; // 红色（失败）
                break;
            case IDLE:
            default:
                statusColor = "#8B5CF6"; // 紫色（默认/空闲）
                break;
        }
        
        // 更新边框颜色和连接点颜色
        background.setStroke(Color.web(statusColor));
        topConnector.setFill(Color.web(statusColor));
        bottomConnector.setFill(Color.web(statusColor));
        leftConnector.setFill(Color.web(statusColor));
        rightConnector.setFill(Color.web(statusColor));
        
        // 更新背景填充颜色（直接根据状态设置，不依赖其他字段）
        updateBackgroundColorByStatus(newStatus);
        
        // 更新当前颜色
        this.currentColor = statusColor;
        
    }
    
    /**
     * 获取当前节点状态
     */
    public NodeStatus getStatus() {
        return status;
    }
    
    /**
     * 根据状态码更新节点状态（用于WebSocket消息）
     * @param statusCode 状态码：0=失败, 1=成功, 2=运行中
     */
    public void updateStatusByCode(Integer statusCode) {
        if (statusCode == null) {
            return;
        }
        
        switch (statusCode) {
            case 0:
                updateStatus(NodeStatus.FAILED);
                break;
            case 1:
                updateStatus(NodeStatus.SUCCESS);
                break;
            case 2:
                updateStatus(NodeStatus.RUNNING);
                break;
            default:
                updateStatus(NodeStatus.IDLE);
                break;
        }
    }
}
