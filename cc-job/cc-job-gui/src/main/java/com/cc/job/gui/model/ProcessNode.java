package com.cc.job.gui.model;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.util.function.Consumer;
import com.cc.job.gui.util.StyleUtil;
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
    
    // 拖拽优化：记录节点内部偏移
    private double mouseOffsetInNodeX;
    private double mouseOffsetInNodeY;
    
    // 拖拽增量计算：上一帧的 scene 与 layout，用于避免视口滚动导致节点方向错误
    private double lastSceneX;
    private double lastSceneY;
    private double lastLayoutX;
    private double lastLayoutY;
    
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
    
    // 编辑标签回调
    private Runnable onEditTags;
    
    // 编辑备注回调
    private Runnable onEditRemark;
    
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
    private Runnable onShowDependencies; // 查看依赖回调
    
    // 禁用/启用节点回调
    private DisableNodeCallback onDisable;
    
    // 节点状态设置回调
    private NodeStateChangeCallback onStateChange;
    
    // 移出容器回调
    private Runnable onRemoveFromContainer;
    private java.util.function.Supplier<Boolean> isInContainerChecker; // 检查节点是否在容器内
    
    // 颜色变更回调
    private Runnable onColorChanged; // 颜色变更时的回调,用于保存到数据库
    
    // 样式设置回调
    private Runnable onChangeStyle; // 样式设置回调

    // 保存为节点模板回调
    private Runnable onSaveAsTemplate;
    // 审批设置回调
    private Runnable onApprovalSetting;
    
    /** 拖拽调整大小结束回调，用于撤销/重做入栈；参数为 (旧宽, 旧高, 新宽, 新高) */
    private Consumer<ResizeRecord> onResizeFinished;
    
    /** 节点调整大小记录，用于撤销/重做 */
    public static final class ResizeRecord {
        private final double oldWidth;
        private final double oldHeight;
        private final double newWidth;
        private final double newHeight;
        public ResizeRecord(double oldWidth, double oldHeight, double newWidth, double newHeight) {
            this.oldWidth = oldWidth;
            this.oldHeight = oldHeight;
            this.newWidth = newWidth;
            this.newHeight = newHeight;
        }
        public double getOldWidth() { return oldWidth; }
        public double getOldHeight() { return oldHeight; }
        public double getNewWidth() { return newWidth; }
        public double getNewHeight() { return newHeight; }
    }
    
    public interface DisableNodeCallback {
        void onDisableNode(Long jobId, boolean isDisabled);
    }
    
    public interface NodeStateChangeCallback {
        void onNodeStateChange(ProcessNode node, GraphNodeState oldState, GraphNodeState newState);
    }
    
    /**
     * 边框样式枚举
     */
    public enum BorderStyle {
        SOLID,   // 实线
        DASHED,  // 虚线
        DOTTED   // 点线
    }
    
    // 节点状态
    private boolean enabled = true;
    private String currentColor = "#2563EB"; // 默认企业蓝
    private String type = "Bean"; // 节点类型：Bean, API, SQL等
    private NodeStatus status = NodeStatus.IDLE; // 节点运行状态
    private GraphNodeState graphState = GraphNodeState.NORMAL; // 图节点状态（开始/终止/阻塞）
    
    // 节点样式属性
    private double nodeWidth = NODE_WIDTH; // 节点宽度
    private double nodeHeight = NODE_HEIGHT; // 节点高度
    private BorderStyle borderStyle = BorderStyle.SOLID; // 边框样式
    private double borderWidth = 2.0; // 边框粗细
    
    // 拖拽调整大小相关
    private Circle resizeHandle; // 调整大小的控制点
    private boolean isResizing = false; // 是否正在调整大小
    private double resizeStartX; // 调整大小开始时的鼠标X坐标
    private double resizeStartY; // 调整大小开始时的鼠标Y坐标
    private double resizeStartWidth; // 调整大小开始时的节点宽度
    private double resizeStartHeight; // 调整大小开始时的节点高度
    
    // UI元素引用
    private javafx.scene.shape.Rectangle background;
    private Label typeLabel;    // 类型标签引用
    private Label handlerLabel; // 处理器名称标签引用
    private FontIcon typeIcon;  // 节点类型图标
    private FontIcon stateIcon; // 状态图标（开始/停止/阻塞/暂停）
    private Pane stateIconContainer; // 状态图标容器（左上角）
    private javafx.scene.shape.Circle iconBackground; // 状态图标背景圆圈
    private Timeline locateAnimation;
    
    // 标签相关
    private HBox tagsContainer; // 标签容器（显示在节点底部）
    private boolean tagsVisible = false; // 标签是否可见
    private java.util.List<String> tags = new java.util.ArrayList<>(); // 节点标签列表
    
    // 备注相关
    private String remark = ""; // 节点备注
    private javafx.scene.control.Tooltip remarkTooltip; // 备注提示框
    private FontIcon remarkIcon; // 备注图标（显示在节点右下角）

    // 审批相关：是否需要审批、审批人用户ID列表
    private boolean requireApproval = false;
    private java.util.List<Long> approverUserIds = new java.util.ArrayList<>();
    
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
        // 设置节点大小（使用动态大小）
        this.setPrefSize(nodeWidth, nodeHeight);
        this.setMinSize(80, 40); // 最小尺寸限制
        this.setMaxSize(800, 400); // 最大尺寸限制
        
        // 根据节点类型设置边框颜色
        updateBorderColorByType();
        
        // 创建背景
        background = new javafx.scene.shape.Rectangle(nodeWidth, nodeHeight);
        background.setFill(Color.WHITE);
        background.setStroke(Color.web(currentColor));
        background.setStrokeWidth(borderWidth);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setMouseTransparent(true); // 背景不拦截鼠标事件
        
        // 应用边框样式
        applyBorderStyle();
        
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
        typeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");
        
        typeContainer.getChildren().add(typeLabel);
        
        // 任务处理器名称
        handlerLabel = new Label(jobHandlerName);
        handlerLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");
        handlerLabel.setMaxWidth(nodeWidth - 20);
        
        contentBox.getChildren().addAll(typeContainer, handlerLabel);
        
        // 创建独立的连接点容器
        // 容器与节点大小相同，连接点将定位在边缘外部
        connectorPane = new Pane();
        connectorPane.setPrefSize(nodeWidth, nodeHeight);
        connectorPane.setMaxSize(nodeWidth, nodeHeight);
        connectorPane.setMinSize(nodeWidth, nodeHeight);
        connectorPane.setMouseTransparent(false); // 连接点容器接收鼠标事件
        // 允许子元素超出容器边界显示
        connectorPane.setClip(null);
        
        // 添加阴影效果
        this.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // 创建状态图标容器（左上角），使用 Pane 进行绝对定位
        stateIconContainer = new Pane();
        stateIconContainer.setPrefSize(28, 28);
        stateIconContainer.setLayoutX(6);
        stateIconContainer.setLayoutY(6);
        stateIconContainer.setMouseTransparent(true); // 不拦截鼠标事件
        
        // 创建状态图标背景（圆形，白色背景，带阴影效果使其更明显）
        // 背景圆圈中心在容器的 (14, 14) 位置
        iconBackground = new Circle(14, 14, 12);
        iconBackground.setFill(Color.web("#FFFFFF"));
        iconBackground.setOpacity(0.95);
        iconBackground.setStroke(Color.web("#D1D5DB"));
        iconBackground.setStrokeWidth(1.5);
        // 添加阴影效果，使图标更明显
        iconBackground.setEffect(new DropShadow(3, Color.web("#000000", 0.4)));
        
        // 创建状态图标，定位在背景圆圈中心
        // FontIcon 的基准点在左上角，所以需要计算位置使其在圆圈中心
        stateIcon = new FontIcon();
        stateIcon.setIconSize(18); // 增大图标尺寸使其更明显
        // 图标中心应该在 (14, 14)，FontIcon 的基准点在左上角，所以需要减去图标尺寸的一半
        stateIcon.setLayoutX(5);
        stateIcon.setLayoutY(21);
        
        stateIconContainer.getChildren().addAll(iconBackground, stateIcon);
        
        // 创建标签容器（显示在节点底部）
        tagsContainer = new HBox(4);
        tagsContainer.setAlignment(Pos.CENTER);
        tagsContainer.setLayoutX(0);
        tagsContainer.setLayoutY(nodeHeight - 20);
        tagsContainer.setPrefWidth(nodeWidth);
        tagsContainer.setMouseTransparent(true);
        tagsContainer.setVisible(false);
        updateTagsDisplay();
        
        // 创建备注图标容器（右下角）
        Pane remarkIconContainer = new Pane();
        remarkIconContainer.setPrefSize(18, 18);
        remarkIconContainer.setMouseTransparent(true);
        
        remarkIcon = new FontIcon(org.kordamp.ikonli.feather.Feather.FILE_TEXT);
        remarkIcon.setIconSize(12);
        remarkIcon.setIconColor(Color.web("#2563EB"));
        remarkIcon.setLayoutX(nodeWidth-20);
        remarkIcon.setLayoutY(nodeHeight-10);
        remarkIconContainer.setVisible(false);
        remarkIconContainer.getChildren().add(remarkIcon);
        
        // 创建调整大小的控制点（右下角）
        resizeHandle = new Circle(6);
        resizeHandle.setFill(Color.web("#3B82F6"));
        resizeHandle.setStroke(Color.WHITE);
        resizeHandle.setStrokeWidth(1.5);
        resizeHandle.setLayoutX(nodeWidth - 6);
        resizeHandle.setLayoutY(nodeHeight - 6);
        resizeHandle.setCursor(Cursor.SE_RESIZE);
        resizeHandle.setVisible(false); // 默认隐藏，只在鼠标移动到中心区域时显示
        
        // 设置调整大小的鼠标事件
        setupResizeHandlers();
        
        // 创建备注提示框
        remarkTooltip = new Tooltip();
        remarkTooltip.setWrapText(true);
        remarkTooltip.setMaxWidth(300);
        
        // 设置鼠标悬停时显示备注
        this.setOnMouseEntered(e -> {
            if (remark != null && !remark.trim().isEmpty()) {
                remarkTooltip.setText(remark);
                remarkTooltip.show(this, e.getScreenX(), e.getScreenY() + 10);
            }
        });
        this.setOnMouseExited(e -> {
            remarkTooltip.hide();
        });
        
        // 按顺序添加：背景 -> 内容 -> 连接点容器 -> 状态图标容器 -> 标签容器 -> 备注图标容器 -> 调整大小控制点
        this.getChildren().addAll(background, contentBox, connectorPane, stateIconContainer, tagsContainer, remarkIconContainer, resizeHandle);
        
        // 最后创建连接点并添加到独立容器中
        createConnectors();
        
        // 初始化状态图标
        updateStateIcon();
    }
    
    /**
     * 判断鼠标位置是否在节点中心区域附近
     * @param mouseX 鼠标在节点内的X坐标（相对于节点）
     * @param mouseY 鼠标在节点内的Y坐标（相对于节点）
     * @return 是否在中心区域
     */
    private boolean isMouseInCenterArea(double mouseX, double mouseY) {
        // 定义中心区域为节点中心50%的区域
        double centerX = nodeWidth / 2;
        double centerY = nodeHeight / 2;
        double centerAreaWidth = nodeWidth * 0.5;
        double centerAreaHeight = nodeHeight * 0.5;
        
        double leftBound = centerX - centerAreaWidth / 2;
        double rightBound = centerX + centerAreaWidth / 2;
        double topBound = centerY - centerAreaHeight / 2;
        double bottomBound = centerY + centerAreaHeight / 2;
        
        return mouseX >= leftBound && mouseX <= rightBound &&
               mouseY >= topBound && mouseY <= bottomBound;
    }
    
    /**
     * 设置调整大小的鼠标事件处理器
     */
    private void setupResizeHandlers() {
        resizeHandle.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                isResizing = true;
                resizeStartX = e.getSceneX();
                resizeStartY = e.getSceneY();
                resizeStartWidth = nodeWidth;
                resizeStartHeight = nodeHeight;
                // 开始调整大小时，确保控制点可见
                if (resizeHandle != null) {
                    resizeHandle.setVisible(true);
                }
                e.consume();
            }
        });
        
        resizeHandle.setOnMouseDragged(e -> {
            if (isResizing) {
                double deltaX = e.getSceneX() - resizeStartX;
                double deltaY = e.getSceneY() - resizeStartY;
                
                double newWidth = Math.max(80, Math.min(800, resizeStartWidth + deltaX));
                double newHeight = Math.max(40, Math.min(400, resizeStartHeight + deltaY));
                
                setNodeSize(newWidth, newHeight);
                // 调整大小过程中，保持控制点可见
                if (resizeHandle != null) {
                    resizeHandle.setVisible(true);
                }
                e.consume();
            }
        });
        
        resizeHandle.setOnMouseReleased(e -> {
            if (isResizing) {
                double oldW = resizeStartWidth;
                double oldH = resizeStartHeight;
                isResizing = false;
                // 尺寸实际变化时通知撤销/重做
                if (onResizeFinished != null && (oldW != nodeWidth || oldH != nodeHeight)) {
                    onResizeFinished.accept(new ResizeRecord(oldW, oldH, nodeWidth, nodeHeight));
                }
                // 触发样式变更回调，用于保存到数据库
                if (onColorChanged != null) {
                    onColorChanged.run();
                }
                // 调整大小结束后，根据鼠标位置决定是否隐藏控制点
                // 注意：此时鼠标可能已经不在节点内，所以不在这里处理显示/隐藏
                // 由鼠标移动事件处理器来处理
                e.consume();
            }
        });
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
        topConnector.setLayoutX(nodeWidth / 2);
        topConnector.setLayoutY(0);
        
        // 底部：水平居中，圆心在节点底边
        bottomConnector.setLayoutX(nodeWidth / 2);
        bottomConnector.setLayoutY(nodeHeight);
        
        // 左侧：圆心在节点左边，垂直居中
        leftConnector.setLayoutX(0);
        leftConnector.setLayoutY(nodeHeight / 2);
        
        // 右侧：圆心在节点右边，垂直居中
        rightConnector.setLayoutX(nodeWidth);
        rightConnector.setLayoutY(nodeHeight / 2);
        
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
            return "#93C5FD"; // 默认浅蓝
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
            return "#93C5FD"; // 默认浅蓝
        }
    }
    
    private void setupDragHandlers() {
        // 鼠标进入节点
        this.setOnMouseEntered(e -> {
            // 只有当鼠标不在按下状态时才改变光标和显示连接点
            if (!e.isPrimaryButtonDown()) {
                this.setCursor(Cursor.MOVE);
                showConnectors(true);
                
                // 检查鼠标是否在中心区域，如果是则显示调整大小控制点
                double mouseX = e.getX();
                double mouseY = e.getY();
                if (isMouseInCenterArea(mouseX, mouseY) && resizeHandle != null) {
                    resizeHandle.setVisible(true);
                }
            }
            e.consume();
        });
        
        // 鼠标在节点内移动
        this.setOnMouseMoved(e -> {
            // 只有当鼠标不在按下状态时才更新控制点显示
            if (!e.isPrimaryButtonDown() && !isResizing) {
                double mouseX = e.getX();
                double mouseY = e.getY();
                if (resizeHandle != null) {
                    // 根据鼠标位置动态显示/隐藏控制点
                    resizeHandle.setVisible(isMouseInCenterArea(mouseX, mouseY));
                }
            }
            e.consume();
        });
        
        // 鼠标离开节点
        this.setOnMouseExited(e -> {
            // 只有当鼠标不在按下状态时才隐藏连接点
            if (!e.isPrimaryButtonDown()) {
                this.setCursor(Cursor.DEFAULT);
                showConnectors(false);
                
                // 隐藏调整大小控制点
                if (resizeHandle != null && !isResizing) {
                    resizeHandle.setVisible(false);
                }
            }
            e.consume();
        });
        
        // 开始拖拽（只处理节点本身，不处理连接点）
        this.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown() && !isConnectorClick(e.getTarget())) {
                initialLayoutX = this.getLayoutX();
                initialLayoutY = this.getLayoutY();
                
                // 优化：记录鼠标在节点内部的偏移位置（使用节点局部坐标）
                mouseOffsetInNodeX = e.getX();
                mouseOffsetInNodeY = e.getY();
                
                // 增量拖拽：记录首帧的 scene 与 layout，用于按位移增量更新位置，避免视口滚动导致方向错误
                lastSceneX = e.getSceneX();
                lastSceneY = e.getSceneY();
                lastLayoutX = this.getLayoutX();
                lastLayoutY = this.getLayoutY();
                
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
        
        // 拖拽中：使用鼠标位移增量更新位置，避免画布缩小+视口自动滚动时节点方向错误
        this.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown() && !isConnectorClick(e.getTarget())) {
                javafx.scene.Node parent = this.getParent();
                if (parent != null) {
                    Point2D currentInParent = parent.sceneToLocal(e.getSceneX(), e.getSceneY());
                    Point2D lastInParent = parent.sceneToLocal(lastSceneX, lastSceneY);
                    double deltaX = currentInParent.getX() - lastInParent.getX();
                    double deltaY = currentInParent.getY() - lastInParent.getY();
                    
                    double newX = lastLayoutX + deltaX;
                    double newY = lastLayoutY + deltaY;
                    
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
                    
                    // 更新本帧为下一帧的“上一帧”
                    lastSceneX = e.getSceneX();
                    lastSceneY = e.getSceneY();
                    lastLayoutX = this.getLayoutX();
                    lastLayoutY = this.getLayoutY();
                    
                    if (onPositionChanged != null) {
                        onPositionChanged.accept(this);
                    }
                    
                    if (onDragged != null) {
                        onDragged.run();
                    }
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
        
        // 查看依赖
        MenuItem dependenciesItem = new MenuItem("查看依赖");
        dependenciesItem.setOnAction(e -> {
            if (onShowDependencies != null) {
                onShowDependencies.run();
            }
        });
        
        // 编辑标签
        MenuItem editTagsItem = new MenuItem("编辑标签");
        editTagsItem.setOnAction(e -> {
            if (onEditTags != null) {
                onEditTags.run();
            }
        });
        
        // 编辑备注
        MenuItem editRemarkItem = new MenuItem("编辑备注");
        editRemarkItem.setOnAction(e -> {
            if (onEditRemark != null) {
                onEditRemark.run();
            }
        });

        // 审批设置
        MenuItem approvalSettingItem = new MenuItem("审批设置");
        approvalSettingItem.setOnAction(e -> {
            if (onApprovalSetting != null) {
                onApprovalSetting.run();
            }
        });

        // 保存为节点模板
        MenuItem saveAsTemplateItem = new MenuItem("保存为节点模板");
        saveAsTemplateItem.setOnAction(e -> {
            if (onSaveAsTemplate != null) {
                onSaveAsTemplate.run();
            }
        });
        
        // 分隔符
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
        // 设置样式
        MenuItem styleItem = new MenuItem("设置样式");
        styleItem.setOnAction(e -> {
            if (onChangeStyle != null) {
                onChangeStyle.run();
            }
        });
        
        // 禁用/启用节点
        MenuItem toggleItem = new MenuItem("禁用节点");
        toggleItem.setOnAction(e -> {
            toggleNodeEnabled();
            toggleItem.setText(isEnabled() ? "禁用节点" : "启用节点");
        });
        
        // 分隔符
        SeparatorMenuItem separatorState = new SeparatorMenuItem();
        
        // 节点状态菜单
        Menu stateMenu = new Menu("节点状态");
        
        // 设置为开始节点
        MenuItem startNodeItem = new MenuItem("开始节点");
        startNodeItem.setOnAction(e -> {
            setGraphState(GraphNodeState.START);
        });
        
        // 设置为终止节点
        MenuItem stopNodeItem = new MenuItem("终止节点");
        stopNodeItem.setOnAction(e -> {
            setGraphState(GraphNodeState.STOP);
        });
        
        // 设置为阻塞节点
        MenuItem blockedNodeItem = new MenuItem("阻塞节点");
        blockedNodeItem.setOnAction(e -> {
            setGraphState(GraphNodeState.BLOCKED);
        });
        
        // 取消特殊状态（恢复为普通节点）
        MenuItem normalNodeItem = new MenuItem("取消特殊状态");
        normalNodeItem.setOnAction(e -> {
            setGraphState(GraphNodeState.NORMAL);
        });
        
        stateMenu.getItems().addAll(startNodeItem, stopNodeItem, blockedNodeItem, normalNodeItem);

        // 分隔符
        SeparatorMenuItem separator2 = new SeparatorMenuItem();
        
        // 移出容器（动态添加，如果节点在容器内才显示）
        MenuItem removeFromContainerItem = new MenuItem("移出容器");
        removeFromContainerItem.setOnAction(e -> {
            if (onRemoveFromContainer != null) {
                onRemoveFromContainer.run();
            }
        });
        
        // 删除节点
        MenuItem deleteItem = new MenuItem("删除节点");
        deleteItem.setStyle("-fx-text-fill: #EF4444;"); // 红色文字
        deleteItem.setOnAction(e -> {
            if (onDelete != null) {
                onDelete.run();
            }
        });

        //删除禁用节点的功能
        contextMenu.getItems().addAll(
            editItem,
            copyItem,
            detailsItem,
            dependenciesItem,
            editTagsItem,
            editRemarkItem,
            approvalSettingItem,
            saveAsTemplateItem,
            separator1,
            styleItem,
            //toggleItem,
            separatorState,
            stateMenu,
            separator2,
            deleteItem
        );
        
        // 右键显示菜单
        this.setOnContextMenuRequested(e -> {
            // 动态更新"移出容器"菜单项的可见性
            boolean isInContainer = isInContainerChecker != null && isInContainerChecker.get();
            if (isInContainer) {
                // 如果节点在容器内，且菜单项还未添加，则添加
                if (!contextMenu.getItems().contains(removeFromContainerItem)) {
                    // 在separator2之前插入
                    int separatorIndex = contextMenu.getItems().indexOf(separator2);
                    if (separatorIndex >= 0) {
                        contextMenu.getItems().add(separatorIndex, removeFromContainerItem);
                    } else {
                        contextMenu.getItems().add(contextMenu.getItems().size() - 1, removeFromContainerItem);
                    }
                }
                removeFromContainerItem.setVisible(true);
            } else {
                // 如果节点不在容器内，隐藏菜单项
                removeFromContainerItem.setVisible(false);
            }
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
    
    public void setOnEditTags(Runnable onEditTags) {
        this.onEditTags = onEditTags;
    }
    
    public void setOnEditRemark(Runnable onEditRemark) {
        this.onEditRemark = onEditRemark;
    }
    
    public void setOnShowDetails(Runnable onShowDetails) {
        this.onShowDetails = onShowDetails;
    }
    
    public void setOnShowDependencies(Runnable onShowDependencies) {
        this.onShowDependencies = onShowDependencies;
    }
    
    public void setOnDisable(DisableNodeCallback callback) {
        this.onDisable = callback;
    }
    
    public void setOnStateChange(NodeStateChangeCallback callback) {
        this.onStateChange = callback;
    }
    
    public void setOnRemoveFromContainer(Runnable onRemoveFromContainer) {
        this.onRemoveFromContainer = onRemoveFromContainer;
    }

    public void setOnSaveAsTemplate(Runnable onSaveAsTemplate) {
        this.onSaveAsTemplate = onSaveAsTemplate;
    }

    public void setOnApprovalSetting(Runnable onApprovalSetting) {
        this.onApprovalSetting = onApprovalSetting;
    }
    
    public void setIsInContainerChecker(java.util.function.Supplier<Boolean> isInContainerChecker) {
        this.isInContainerChecker = isInContainerChecker;
    }
    
    /**
     * 获取图节点状态
     */
    public GraphNodeState getGraphState() {
        return graphState;
    }
    
    /**
     * 设置图节点状态（开始/终止/阻塞）
     */
    public void setGraphState(GraphNodeState newState) {
        if (this.graphState == newState) {
            return;
        }
        
        GraphNodeState oldState = this.graphState;
        this.graphState = newState;
        
        // 保存状态到全局管理器（不保存到数据库）
        if (jobId != null) {
            com.cc.job.gui.util.NodeGraphStateManager.getInstance().setNodeState(jobId, newState);
        }
        
        // 更新节点样式
        updateGraphStateStyle();
        
        // 通知外部（NodeCanvas）状态变化，以便更新相关节点
        if (onStateChange != null) {
            onStateChange.onNodeStateChange(this, oldState, newState);
        }
    }
    
    /**
     * 内部方法：设置图节点状态但不触发回调（用于避免循环调用）
     * 仅在 NodeCanvas 的状态传播逻辑中使用
     * @param newState 新的图节点状态
     */
    public void setGraphStateInternal(GraphNodeState newState) {
        if (this.graphState == newState) {
            return;
        }
        
        this.graphState = newState;
        
        // 保存状态到全局管理器（不保存到数据库）
        if (jobId != null) {
            com.cc.job.gui.util.NodeGraphStateManager.getInstance().setNodeState(jobId, newState);
        }
        
        // 只更新样式，不触发回调
        updateGraphStateStyle();
    }
    
    /**
     * 更新状态图标（左上角）
     */
    private void updateStateIcon() {
        if (stateIcon == null || iconBackground == null) {
            return;
        }
        
        // 如果节点被禁用，显示暂停图标
        if (!enabled) {
            stateIcon.setIconCode(Feather.PAUSE);
            stateIcon.setIconColor(Color.web("#FFFFFF")); // 白色图标
            stateIcon.setVisible(true);
            // 图标背景圆圈：灰色背景，灰色边框
            iconBackground.setFill(Color.web("#6B7280")); // 灰色背景
            iconBackground.setStroke(Color.web("#4B5563")); // 深灰色边框
            iconBackground.setVisible(true);
            stateIconContainer.setVisible(true);
            return;
        }
        
        // 根据图节点状态显示不同图标
        switch (graphState) {
            case START:
                stateIcon.setIconCode(Feather.PLAY);
                stateIcon.setIconColor(Color.web("#FFFFFF")); // 白色图标
                stateIcon.setVisible(true);
                // 图标背景圆圈：绿色背景，深绿色边框
                iconBackground.setFill(Color.web("#10B981")); // 绿色背景
                iconBackground.setStroke(Color.web("#059669")); // 深绿色边框
                iconBackground.setVisible(true);
                stateIconContainer.setVisible(true);
                break;
            case STOP:
                stateIcon.setIconCode(Feather.SQUARE);
                stateIcon.setIconColor(Color.web("#FFFFFF")); // 白色图标
                stateIcon.setVisible(true);
                // 图标背景圆圈：红色背景，深红色边框
                iconBackground.setFill(Color.web("#EF4444")); // 红色背景
                iconBackground.setStroke(Color.web("#DC2626")); // 深红色边框
                iconBackground.setVisible(true);
                stateIconContainer.setVisible(true);
                break;
            case BLOCKED:
                stateIcon.setIconCode(Feather.LOCK);
                stateIcon.setIconColor(Color.web("#FFFFFF")); // 白色图标
                stateIcon.setVisible(true);
                // 图标背景圆圈：灰色背景，深灰色边框
                iconBackground.setFill(Color.web("#6B7280")); // 灰色背景
                iconBackground.setStroke(Color.web("#4B5563")); // 深灰色边框
                iconBackground.setVisible(true);
                stateIconContainer.setVisible(true);
                break;
            case NORMAL:
            default:
                // 普通节点不显示状态图标和背景圆圈
                stateIcon.setVisible(false);
                iconBackground.setVisible(false);
                stateIconContainer.setVisible(false);
                break;
        }
    }
    
    /**
     * 根据图节点状态更新样式
     */
    private void updateGraphStateStyle() {
        if (background == null) {
            return;
        }
        
        // 根据图节点状态设置样式
        switch (graphState) {
            case START:
                // 开始节点：不改变背景颜色，只改变边框颜色和图标
                // 如果节点有运行状态，保持运行状态的背景颜色
                if (status != NodeStatus.IDLE) {
                    updateBackgroundColorByStatus(status);
                } else {
                    // 如果节点是空闲状态，设置背景颜色为白色（清除之前的阻塞状态背景颜色）
                    background.setFill(Color.WHITE);
                }
                // 只改变边框颜色为绿色，表示开始节点
                background.setStroke(Color.web("#10B981")); // 绿色边框
                background.setStrokeWidth(3); // 加粗边框
                // 特殊状态使用实线边框
                background.getStrokeDashArray().clear();
                // 更新连接点颜色为绿色
                if (topConnector != null) {
                    topConnector.setFill(Color.web("#10B981"));
                    bottomConnector.setFill(Color.web("#10B981"));
                    leftConnector.setFill(Color.web("#10B981"));
                    rightConnector.setFill(Color.web("#10B981"));
                }
                break;
            case STOP:
                // 终止节点：不改变背景颜色，只改变边框颜色和图标
                // 如果节点有运行状态，保持运行状态的背景颜色
                if (status != NodeStatus.IDLE) {
                    updateBackgroundColorByStatus(status);
                } else {
                    // 如果节点是空闲状态，设置背景颜色为白色（清除之前的阻塞状态背景颜色）
                    background.setFill(Color.WHITE);
                }
                // 只改变边框颜色为红色，表示终止节点
                background.setStroke(Color.web("#EF4444")); // 红色边框
                background.setStrokeWidth(3); // 加粗边框
                // 特殊状态使用实线边框
                background.getStrokeDashArray().clear();
                // 更新连接点颜色为红色
                if (topConnector != null) {
                    topConnector.setFill(Color.web("#EF4444"));
                    bottomConnector.setFill(Color.web("#EF4444"));
                    leftConnector.setFill(Color.web("#EF4444"));
                    rightConnector.setFill(Color.web("#EF4444"));
                }
                break;
            case BLOCKED:
                // 阻塞节点：深灰色背景，灰色边框，使其与画布背景明显区分
                background.setFill(Color.web("#D1D5DB")); // 更深的灰色背景，与画布背景 #F3F4F6 区分明显
                background.setStroke(Color.web("#9CA3AF")); // 灰色边框，增强视觉效果
                background.setStrokeWidth(2.5); // 稍微加粗边框
                // 特殊状态使用实线边框
                background.getStrokeDashArray().clear();
                // 更新连接点颜色为灰色，表示阻塞状态
                if (topConnector != null) {
                    topConnector.setFill(Color.web("#9CA3AF"));
                    bottomConnector.setFill(Color.web("#9CA3AF"));
                    leftConnector.setFill(Color.web("#9CA3AF"));
                    rightConnector.setFill(Color.web("#9CA3AF"));
                }
                break;
            case NORMAL:
            default:
                // 普通节点：根据运行状态或类型设置颜色
                if (status != NodeStatus.IDLE) {
                    // 如果节点正在运行，使用运行状态的颜色
                    // 注意：updateStatus 内部会检查 graphState，如果是特殊状态不会更新
                    updateStatus(status);
                } else {
                    // 否则使用类型颜色
                    background.setStroke(Color.web(currentColor));
                    background.setFill(Color.WHITE);
                    background.setStrokeWidth(borderWidth);
                    // 应用边框样式
                    applyBorderStyle();
                    // 更新连接点颜色
                    if (topConnector != null) {
                        topConnector.setFill(Color.web(currentColor));
                        bottomConnector.setFill(Color.web(currentColor));
                        leftConnector.setFill(Color.web(currentColor));
                        rightConnector.setFill(Color.web(currentColor));
                    }
                }
                break;
        }
        
        // 更新状态图标
        updateStateIcon();
    }
    
    /**
     * 调整拖拽起始点，用于在画布扩展时保持拖拽位置的正确性
     * @param deltaX X方向的偏移量
     * @param deltaY Y方向的偏移量
     */
    public void adjustDragStart(double deltaX, double deltaY) {
        dragStartX += deltaX;
        dragStartY += deltaY;
        lastLayoutX += deltaX;
        lastLayoutY += deltaY;
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
        
        // 如果节点处于运行状态，需要重新应用样式以确保边框颜色正确
        if (graphState == GraphNodeState.NORMAL && status != NodeStatus.IDLE) {
            updateStatus(status);
        }
    }
    
    /**
     * 根据节点类型更新边框颜色
     */
    private void updateBorderColorByType() {
        if (type == null) {
            currentColor = "#2563EB"; // 默认企业蓝
            return;
        }
        
        currentColor = switch (type) {
            case "Bean" -> "#2563EB";      // 蓝色
            case "API" -> "#FF6B35";      // 橙色
            case "SQL" -> "#3B82F6";      // 蓝色
            case "Java" -> "#E74C3C";     // 红色
            case "Shell" -> "#4A5568";    // 深灰色
            case "Python" -> "#3776AB";   // 蓝色
            case "PHP" -> "#777BB4";     // 紫色
            case "Node" -> "#339933";     // 绿色
            case "PS" -> "#0078D4";       // 蓝色
            case "DataX" -> "#0D9488";   // teal 数据同步
            default -> "#2563EB";          // 默认蓝色
        };
        
        // 只有在普通状态下才更新边框颜色，特殊状态（开始/终止/阻塞）保持其样式
        if (graphState == GraphNodeState.NORMAL && background != null) {
            background.setStroke(Color.web(currentColor));
        }
        
        // 只有在普通状态下才更新连接点颜色
        if (graphState == GraphNodeState.NORMAL && topConnector != null) {
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
            case "DataX" -> createNodeIcon(Feather.REPEAT, "#0D9488", 18);
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
            // 动画结束后，根据当前图节点状态恢复样式
            if (graphState == GraphNodeState.NORMAL) {
                background.setStroke(Color.web(currentColor));
                background.setStrokeWidth(2);
            } else {
                // 如果是特殊状态，重新应用特殊状态样式
                updateGraphStateStyle();
            }
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
     * 设置颜色变更回调
     * @param callback 颜色变更时的回调函数,用于保存颜色到数据库
     */
    public void setOnColorChanged(Runnable callback) {
        this.onColorChanged = callback;
    }
    
    /**
     * 更改节点颜色（用于手动更改，如右键菜单）
     */
    private void changeNodeColor(String color) {
        this.currentColor = color;
        
        // 只有在普通状态下才更新颜色，特殊状态（开始/终止/阻塞）保持其样式
        if (graphState != GraphNodeState.NORMAL) {
            return;
        }
        
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
        
        // 触发颜色变更回调,用于保存到数据库
        if (onColorChanged != null) {
            onColorChanged.run();
        }
    }
    
    /**
     * 公共方法: 设置节点颜色(供外部调用,如从数据库加载时)
     * @param color 颜色值(如 "#2563EB")
     */
    public void setNodeColor(String color) {
        if (color != null && !color.isEmpty()) {
            changeNodeColor(color);
        }
    }
    
    /**
     * 设置节点大小
     * @param width 宽度
     * @param height 高度
     */
    public void setNodeSize(double width, double height) {
        // 限制范围
        width = Math.max(80, Math.min(800, width));
        height = Math.max(40, Math.min(400, height));
        
        this.nodeWidth = width;
        this.nodeHeight = height;
        
        // 更新节点大小
        this.setPrefSize(width, height);
        
        // 更新背景矩形大小
        if (background != null) {
            background.setWidth(width);
            background.setHeight(height);
        }
        
        // 更新连接点容器大小
        if (connectorPane != null) {
            connectorPane.setPrefSize(width, height);
            connectorPane.setMaxSize(width, height);
            connectorPane.setMinSize(width, height);
        }
        
        // 更新连接点位置
        if (topConnector != null) {
            topConnector.setLayoutX(width / 2);
            topConnector.setLayoutY(0);
            bottomConnector.setLayoutX(width / 2);
            bottomConnector.setLayoutY(height);
            leftConnector.setLayoutX(0);
            leftConnector.setLayoutY(height / 2);
            rightConnector.setLayoutX(width);
            rightConnector.setLayoutY(height / 2);
        }
        
        // 更新标签容器位置和大小
        if (tagsContainer != null) {
            tagsContainer.setLayoutY(height - 20);
            tagsContainer.setPrefWidth(width);
        }
        
        // 更新备注图标位置
        if (remarkIcon != null) {
            remarkIcon.setLayoutX(width - 20);
            remarkIcon.setLayoutY(height - 10);
        }
        
        // 更新调整大小控制点位置
        if (resizeHandle != null) {
            resizeHandle.setLayoutX(width - 6);
            resizeHandle.setLayoutY(height - 6);
        }
        
        // 更新处理器名称标签最大宽度
        if (handlerLabel != null) {
            handlerLabel.setMaxWidth(width - 20);
        }
    }
    
    /**
     * 获取节点宽度
     */
    public double getNodeWidth() {
        return nodeWidth;
    }
    
    /**
     * 获取节点高度
     */
    public double getNodeHeight() {
        return nodeHeight;
    }
    
    /**
     * 设置边框样式
     * @param style 边框样式
     */
    public void setBorderStyle(BorderStyle style) {
        this.borderStyle = style;
        applyBorderStyle();
        
        // 触发样式变更回调
        if (onColorChanged != null) {
            onColorChanged.run();
        }
    }
    
    /**
     * 获取边框样式
     */
    public BorderStyle getBorderStyle() {
        return borderStyle;
    }
    
    /**
     * 设置边框粗细
     * @param width 边框粗细
     */
    public void setBorderWidth(double width) {
        this.borderWidth = Math.max(1, Math.min(10, width));
        if (background != null) {
            background.setStrokeWidth(this.borderWidth);
        }
        
        // 触发样式变更回调
        if (onColorChanged != null) {
            onColorChanged.run();
        }
    }
    
    /**
     * 获取边框粗细
     */
    public double getBorderWidth() {
        return borderWidth;
    }
    
    /**
     * 应用边框样式
     */
    private void applyBorderStyle() {
        if (background == null) return;
        
        background.getStrokeDashArray().clear();
        switch (borderStyle) {
            case SOLID:
                // 实线：不设置虚线数组
                break;
            case DASHED:
                // 虚线
                background.getStrokeDashArray().addAll(10.0, 5.0);
                break;
            case DOTTED:
                // 点线
                background.getStrokeDashArray().addAll(3.0, 3.0);
                break;
        }
    }
    
    /**
     * 设置样式变更回调
     * @param callback 回调函数
     */
    public void setOnChangeStyle(Runnable callback) {
        this.onChangeStyle = callback;
    }
    
    /**
     * 设置拖拽调整大小结束回调，用于撤销/重做入栈。
     */
    public void setOnResizeFinished(Consumer<ResizeRecord> callback) {
        this.onResizeFinished = callback;
    }
    
    /**
     * 根据状态更新背景填充颜色
     * @param newStatus 新的状态
     */
    private void updateBackgroundColorByStatus(NodeStatus newStatus) {
        // 如果节点是特殊状态（开始/终止/阻塞），不更新背景颜色
        if (graphState != GraphNodeState.NORMAL) {
            return;
        }
        
        switch (newStatus) {
            case RUNNING:
                // 运行中：加深的黄色背景，边框保持节点类型颜色，更容易观察运行状态
                background.setFill(Color.web("#FCD34D")); // 更深的黄色，保持与边框的对比度
                break;
            case SUCCESS:
                // 成功：加深的绿色背景，边框保持节点类型颜色，更容易观察成功状态
                background.setFill(Color.web("#86EFAC")); // 更深的绿色，保持与边框的对比度
                break;
            case FAILED:
                // 失败：加深的红色背景，边框保持节点类型颜色，更容易观察失败状态
                background.setFill(Color.web("#FCA5A5")); // 更深的红色，保持与边框的对比度
                break;
            case IDLE:
            default:
                // 空闲：白色背景，边框使用节点类型颜色
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
        
        // 保存状态到全局管理器（不保存到数据库）
        if (jobId != null) {
            com.cc.job.gui.util.NodeGraphStateManager.getInstance().setNodeEnabled(jobId, newEnabledState);
        }
        
        if (enabled) {
            // 启用状态：根据当前图节点状态恢复样式
            if (graphState == GraphNodeState.NORMAL) {
                changeNodeColor(currentColor);
            } else {
                // 如果是特殊状态，重新应用特殊状态样式
                updateGraphStateStyle();
            }
            this.setOpacity(1.0);
        } else {
            // 禁用状态：不改变背景颜色，只改变透明度和图标
            // 保持原有的背景颜色和边框颜色
            this.setOpacity(0.6); // 降低透明度表示禁用
        }
        
        // 更新状态图标（显示/隐藏暂停图标）
        updateStateIcon();
        
        // 注意：不再调用后端API保存禁用状态，因为状态只保存在内存中
        // 如果需要调用后端API，可以保留以下代码，但状态不会持久化到数据库
        // if (onDisable != null && jobId != null) {
        //     onDisable.onDisableNode(jobId, !enabled);
        // }
    }
    
    /**
     * 恢复节点的启用/禁用状态（用于从全局管理器恢复状态）
     * @param targetEnabledState 目标启用状态
     */
    public void restoreEnabledState(boolean targetEnabledState) {
        enabled = targetEnabledState;
        
        // 保存状态到全局管理器（确保状态同步）
        if (jobId != null) {
            com.cc.job.gui.util.NodeGraphStateManager.getInstance().setNodeEnabled(jobId, targetEnabledState);
        }
        
        if (enabled) {
            // 启用状态：根据当前图节点状态恢复样式
            if (graphState == GraphNodeState.NORMAL) {
                changeNodeColor(currentColor);
            } else {
                // 如果是特殊状态，重新应用特殊状态样式
                updateGraphStateStyle();
            }
            this.setOpacity(1.0);
        } else {
            // 禁用状态：不改变背景颜色，只改变透明度和图标
            // 保持原有的背景颜色和边框颜色
            this.setOpacity(0.6); // 降低透明度表示禁用
        }
        
        // 更新状态图标（显示/隐藏暂停图标）
        updateStateIcon();
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
     * 图节点状态枚举（用于开始节点、终止节点、阻塞节点）
     */
    public enum GraphNodeState {
        NORMAL,    // 普通节点（默认）
        START,     // 开始节点（绿色边框，绿色背景）
        STOP,      // 终止节点（红色边框，红色背景）
        BLOCKED    // 阻塞节点（灰色边框，灰色背景）
    }
    
    /**
     * 更新节点状态（根据运行状态改变颜色）
     * @param newStatus 新的状态
     */
    public void updateStatus(NodeStatus newStatus) {
        NodeStatus oldStatus = this.status;
        this.status = newStatus;
        
        // 如果节点不是特殊状态（开始/终止/阻塞），才更新运行状态的颜色
        if (graphState == GraphNodeState.NORMAL) {
            // 优化：运行状态只改变背景颜色，边框保持节点类型颜色，便于区分节点类型
            // 更新背景填充颜色（直接根据状态设置，不依赖其他字段）
            updateBackgroundColorByStatus(newStatus);
            
            // 边框颜色保持节点类型颜色，不随运行状态改变
            // 这样可以在运行状态下也能区分节点类型（Bean、Java、API等）
            if (background != null) {
                background.setStroke(Color.web(currentColor));
                background.setStrokeWidth(borderWidth);
                // 重新应用边框样式
                applyBorderStyle();
            }
            
            // 连接点颜色也保持节点类型颜色
            if (topConnector != null) {
                topConnector.setFill(Color.web(currentColor));
                bottomConnector.setFill(Color.web(currentColor));
                leftConnector.setFill(Color.web(currentColor));
                rightConnector.setFill(Color.web(currentColor));
            }
        } else if (graphState == GraphNodeState.START || graphState == GraphNodeState.STOP) {
            // 开始节点或终止节点：当状态为IDLE时，背景颜色应该变成白色
            // 但保持边框颜色和连接点颜色不变（保持开始/终止节点的特征）
            if (background != null) {
                if (newStatus == NodeStatus.IDLE) {
                    background.setFill(Color.WHITE);
                } else {
                    // 如果节点有运行状态，根据运行状态设置背景颜色
                    switch (newStatus) {
                        case RUNNING:
                            background.setFill(Color.web("#FCD34D")); // 黄色
                            break;
                        case SUCCESS:
                            background.setFill(Color.web("#86EFAC")); // 绿色
                            break;
                        case FAILED:
                            background.setFill(Color.web("#FCA5A5")); // 红色
                            break;
                        default:
                            background.setFill(Color.WHITE);
                            break;
                    }
                }
            }
        }
        // 阻塞节点保持阻塞状态的样式不变
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
            case -1:
                // 未运行状态（任务组启动时会将所有子节点重置为此状态）
                updateStatus(NodeStatus.IDLE);
                break;
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
    
    // ==================== 标签相关方法 ====================
    
    /**
     * 设置节点标签
     */
    public void setTags(java.util.List<String> newTags) {
        if (newTags == null) {
            tags.clear();
        } else {
            tags = new java.util.ArrayList<>(newTags);
        }
        updateTagsDisplay();
    }
    
    /**
     * 获取节点标签
     */
    public java.util.List<String> getTags() {
        return new java.util.ArrayList<>(tags);
    }
    
    /**
     * 添加标签
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag)) {
            tags.add(tag);
            updateTagsDisplay();
        }
    }
    
    /**
     * 移除标签
     */
    public void removeTag(String tag) {
        tags.remove(tag);
        updateTagsDisplay();
    }
    
    /**
     * 更新标签显示
     */
    private void updateTagsDisplay() {
        if (tagsContainer == null) return;
        
        tagsContainer.getChildren().clear();
        
        if (tags.isEmpty()) {
            tagsContainer.setVisible(false);
            return;
        }
        
        // 只显示前3个标签，避免节点过于拥挤
        int maxTags = 3;
        for (int i = 0; i < Math.min(tags.size(), maxTags); i++) {
            String tag = tags.get(i);
            Label tagLabel = new Label(tag);
            tagLabel.setStyle(
                "-fx-font-size: 10; " +
                "-fx-font-weight: 500; " +
                "-fx-text-fill: #2563EB; " +
                "-fx-background-color: #EEF2FF; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 2 6 2 6;"
            );
            tagsContainer.getChildren().add(tagLabel);
        }
        
        // 如果有更多标签，显示省略号
        if (tags.size() > maxTags) {
            Label moreLabel = new Label("...");
            moreLabel.setStyle(
                "-fx-font-size: 10; " +
                "-fx-text-fill: #9CA3AF;"
            );
            tagsContainer.getChildren().add(moreLabel);
        }
        
        tagsContainer.setVisible(tagsVisible && !tags.isEmpty());
    }
    
    /**
     * 设置标签可见性
     */
    public void setTagsVisible(boolean visible) {
        tagsVisible = visible;
        if (tagsContainer != null) {
            tagsContainer.setVisible(visible && !tags.isEmpty());
        }
    }
    
    /**
     * 获取标签可见性
     */
    public boolean isTagsVisible() {
        return tagsVisible;
    }
    
    // ==================== 备注相关方法 ====================
    
    /**
     * 设置节点备注
     */
    public void setRemark(String newRemark) {
        remark = newRemark != null ? newRemark : "";
        updateRemarkDisplay();
    }
    
    /**
     * 获取节点备注
     */
    public String getRemark() {
        return remark;
    }

    // ==================== 审批相关 ====================

    public boolean isRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public java.util.List<Long> getApproverUserIds() {
        return new java.util.ArrayList<>(approverUserIds);
    }

    public void setApproverUserIds(java.util.List<Long> approverUserIds) {
        this.approverUserIds = approverUserIds != null ? new java.util.ArrayList<>(approverUserIds) : new java.util.ArrayList<>();
    }
    
    /**
     * 更新备注显示
     */
    private void updateRemarkDisplay() {
        if (remarkIcon != null) {
            // 如果有备注，显示备注图标
            Pane remarkContainer = (Pane) remarkIcon.getParent();
            if (remarkContainer != null) {
                remarkContainer.setVisible(remark != null && !remark.trim().isEmpty());
            }
        }
    }
}
