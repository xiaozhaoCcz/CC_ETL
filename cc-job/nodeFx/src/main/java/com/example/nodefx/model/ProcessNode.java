package com.example.nodefx.model;

import javafx.geometry.Insets;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * 流程节点类，支持拖拽、右键菜单等功能
 */
public class ProcessNode extends StackPane {
    
    private String nodeId;
    private Long jobId;  // 任务ID，用于后端保存
    private String jobHandlerName;
    private double dragStartX;
    private double dragStartY;
    
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
    
    // 编辑回调
    private Runnable onEdit;
    
    // 节点状态
    private boolean enabled = true;
    private String currentColor = "#8B5CF6"; // 默认紫色
    private String type = "Bean"; // 节点类型：Bean, API, SQL等
    private NodeStatus status = NodeStatus.IDLE; // 节点运行状态
    
    // UI元素引用
    private javafx.scene.shape.Rectangle background;
    private Label typeLabel;    // 类型标签引用
    private Label handlerLabel; // 处理器名称标签引用
    
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
        
        // 类型标签
        typeLabel = new Label(type);
        typeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #9CA3AF;");
        
        // 任务处理器名称
        handlerLabel = new Label(jobHandlerName);
        handlerLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        handlerLabel.setMaxWidth(NODE_WIDTH - 20);
        
        contentBox.getChildren().addAll(typeLabel, handlerLabel);
        
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
        
        System.out.println("✅ 连接点已创建并定位在边界: " + jobHandlerName);
    }
    
    private Circle createConnector() {
        Circle connector = new Circle(5); // 减小半径，使连接点更小巧
        connector.setFill(Color.web("#8B5CF6"));
        connector.setStroke(Color.WHITE);
        connector.setStrokeWidth(2);
        connector.setVisible(false); // 初始隐藏
        connector.setMouseTransparent(false); // 必须能接收鼠标事件
        
        // 添加轻微的投影效果
        connector.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 0.5);");
        
        // 鼠标悬停效果 - 只改变颜色，不改变大小
        connector.setOnMouseEntered(e -> {
            connector.setFill(Color.web("#A78BFA")); // 浅紫色
            connector.setCursor(Cursor.CROSSHAIR);
            e.consume();
        });
        
        connector.setOnMouseExited(e -> {
            connector.setFill(Color.web("#8B5CF6"));
            connector.setCursor(Cursor.DEFAULT);
            e.consume();
        });
        
        return connector;
    }
    
    private void setupDragHandlers() {
        // 鼠标进入节点
        this.setOnMouseEntered(e -> {
            System.out.println("✅ 鼠标进入节点: " + jobHandlerName);
            // 只有当鼠标不在按下状态时才改变光标和显示连接点
            if (!e.isPrimaryButtonDown()) {
                this.setCursor(Cursor.MOVE);
                showConnectors(true);
                System.out.println("   → 连接点应该显示");
            }
            e.consume();
        });
        
        // 鼠标离开节点
        this.setOnMouseExited(e -> {
            System.out.println("❌ 鼠标离开节点: " + jobHandlerName);
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
                dragStartX = e.getSceneX() - this.getLayoutX();
                dragStartY = e.getSceneY() - this.getLayoutY();
                this.setCursor(Cursor.CLOSED_HAND);
                this.toFront(); // 拖拽时置于顶层
                e.consume();
            }
        });
        
        // 拖拽中
        this.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown() && !isConnectorClick(e.getTarget())) {
                double newX = e.getSceneX() - dragStartX;
                double newY = e.getSceneY() - dragStartY;
                
                // 限制在画布范围内（可选）
                this.setLayoutX(Math.max(0, newX));
                this.setLayoutY(Math.max(0, newY));
                
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
                e.consume();
            }
        });
    }
    
    private void setupContextMenu() {
        contextMenu = new ContextMenu();
        
        // 📝 编辑节点
        MenuItem editItem = new MenuItem("📝 编辑节点");
        editItem.setOnAction(e -> {
            System.out.println("✏️ 点击编辑节点: " + jobHandlerName + " (nodeId: " + nodeId + ")");
            if (onEdit != null) {
                System.out.println("✓ 触发 onEdit 回调");
                onEdit.run();
            } else {
                System.err.println("✗ onEdit 回调为 null！");
            }
        });
        
        // 📋 复制节点
        MenuItem copyItem = new MenuItem("📋 复制节点");
        copyItem.setOnAction(e -> {
            System.out.println("📋 复制节点: " + jobHandlerName);
            // TODO: 实现复制功能
        });
        
        // 📄 节点详情
        MenuItem detailsItem = new MenuItem("📄 查看详情");
        detailsItem.setOnAction(e -> {
            System.out.println("📄 查看详情: " + jobHandlerName);
            // TODO: 显示节点详情对话框
        });
        
        // 分隔符
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
        // 🎨 更改颜色
        Menu colorMenu = new Menu("🎨 更改颜色");
        
        MenuItem purpleItem = new MenuItem("🟣 紫色 (默认)");
        purpleItem.setOnAction(e -> changeNodeColor("#8B5CF6"));
        
        MenuItem blueItem = new MenuItem("🔵 蓝色");
        blueItem.setOnAction(e -> changeNodeColor("#3B82F6"));
        
        MenuItem greenItem = new MenuItem("🟢 绿色");
        greenItem.setOnAction(e -> changeNodeColor("#10B981"));
        
        MenuItem orangeItem = new MenuItem("🟠 橙色");
        orangeItem.setOnAction(e -> changeNodeColor("#F59E0B"));
        
        MenuItem redItem = new MenuItem("🔴 红色");
        redItem.setOnAction(e -> changeNodeColor("#EF4444"));
        
        colorMenu.getItems().addAll(purpleItem, blueItem, greenItem, orangeItem, redItem);
        
        // ⚙️ 禁用/启用节点
        MenuItem toggleItem = new MenuItem("⚙️ 禁用节点");
        toggleItem.setOnAction(e -> {
            toggleNodeEnabled();
            toggleItem.setText(isEnabled() ? "⚙️ 禁用节点" : "✅ 启用节点");
        });
        
        // 📌 设为起始节点
        MenuItem startItem = new MenuItem("📌 设为起始节点");
        startItem.setOnAction(e -> {
            System.out.println("📌 设为起始节点: " + jobHandlerName);
            // TODO: 标记为起始节点
        });
        
        // 分隔符
        SeparatorMenuItem separator2 = new SeparatorMenuItem();
        
        // 🗑️ 删除节点
        MenuItem deleteItem = new MenuItem("🗑️ 删除节点");
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
            startItem,
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
        System.out.println("   🔵 连接点" + (show ? "显示" : "隐藏") + ": " + jobHandlerName);
        System.out.println("      顶部: visible=" + topConnector.isVisible() + ", layoutX=" + topConnector.getLayoutX() + ", layoutY=" + topConnector.getLayoutY());
        System.out.println("      底部: visible=" + bottomConnector.isVisible() + ", layoutX=" + bottomConnector.getLayoutX() + ", layoutY=" + bottomConnector.getLayoutY());
        System.out.println("      左侧: visible=" + leftConnector.isVisible() + ", layoutX=" + leftConnector.getLayoutX() + ", layoutY=" + leftConnector.getLayoutY());
        System.out.println("      右侧: visible=" + rightConnector.isVisible() + ", layoutX=" + rightConnector.getLayoutX() + ", layoutY=" + rightConnector.getLayoutY());
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
    
    public void setOnEdit(Runnable onEdit) {
        this.onEdit = onEdit;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
        if (typeLabel != null) {
            typeLabel.setText(type);
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
        
        System.out.println("🎨 更改节点颜色: " + jobHandlerName + " → " + color + " (状态: " + status + ")");
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
                System.out.println("🟡 节点背景设为黄色（运行中）: " + jobHandlerName);
                break;
            case SUCCESS:
                // 成功：绿色背景
                background.setFill(Color.web("#10B981"));
                System.out.println("🟢 节点背景设为绿色（成功）: " + jobHandlerName);
                break;
            case FAILED:
                // 失败：红色背景
                background.setFill(Color.web("#EF4444"));
                System.out.println("🔴 节点背景设为红色（失败）: " + jobHandlerName);
                break;
            case IDLE:
            default:
                // 空闲：白色背景
                background.setFill(Color.WHITE);
                System.out.println("⚪ 节点背景设为白色（空闲）: " + jobHandlerName);
                break;
        }
    }
    
    /**
     * 切换节点启用/禁用状态
     */
    private void toggleNodeEnabled() {
        enabled = !enabled;
        
        if (enabled) {
            // 启用状态：根据当前状态恢复颜色
            changeNodeColor(currentColor);
            this.setOpacity(1.0);
            System.out.println("✅ 启用节点: " + jobHandlerName);
        } else {
            // 禁用状态：灰色半透明
            background.setFill(Color.web("#F3F4F6"));
            background.setStroke(Color.web("#9CA3AF"));
            background.setStrokeWidth(2);
            this.setOpacity(0.6);
            System.out.println("⚙️ 禁用节点: " + jobHandlerName);
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
        
        System.out.println("📊 节点状态更新: " + jobHandlerName + " [" + oldStatus + " → " + newStatus + "] (边框颜色: " + statusColor + ")");
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
