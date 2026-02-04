package com.cc.job.gui.view;

import com.cc.job.gui.model.GroupContainer;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 小地图组件 - 显示画布缩略图和当前视图位置
 */
public class MiniMapView extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(MiniMapView.class);
    
    private Canvas canvas;
    private GraphicsContext gc;
    private Rectangle viewportRect;
    private NodeCanvas nodeCanvas;
    private ScrollPane scrollPane;
    
    // 记录上次的滚动值，用于检测变化
    private double lastHValue = 0.0;
    private double lastVValue = 0.0;
    
    private static final double MINIMAP_WIDTH = 280;  // 与导航组件同宽
    private static final double MINIMAP_HEIGHT = 280; // 与日志面板同高
    private static final double SCALE_FACTOR = 0.1; // 缩放比例
    
    // 节流机制：避免频繁更新小地图
    private javafx.animation.Timeline throttledUpdateTimeline;
    private javafx.animation.Timeline throttledViewportUpdateTimeline;
    private boolean updatePending = false;
    private boolean viewportUpdatePending = false;
    private static final long THROTTLE_DELAY_MS = 200; // 节流延迟200ms

    /** 正在拖拽视口框时跳过 updateViewport，避免覆盖用户拖拽位置 */
    private boolean isDraggingViewport = false;
    private double dragAnchorX;
    private double dragAnchorY;
    private double dragAnchorRectX;
    private double dragAnchorRectY;
    
    // 关闭回调
    private Runnable onClose;
    
    // 弹出回调
    private Runnable onDetach;
    
    // 弹出和关闭按钮
    private Button detachBtn;
    private Button closeBtn;
    
    public MiniMapView() {
        initializeUI();
    }
    
    private void initializeUI() {
        // 设置样式：SplitPane 会提供分隔线，移除顶部边框
        setStyle(
            "-fx-background-color: transparent; " +
            "-fx-padding: 12 12 0 8;"
        );
        setSpacing(12);
        setMinWidth(200);  // 最小宽度200px
        setMinHeight(200); // 最小高度
        setPrefHeight(MINIMAP_HEIGHT + 40);  // 默认高度
        // 移除maxHeight限制，让它能在弹出窗口时自动扩展
        
        // 标题栏
        HBox titleBar = createTitleBar();
        
        // 画布容器 - 使用 Pane 支持绝对定位！
        Pane canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #F3F3F3;");
        canvasContainer.setMinWidth(150);  // 最小宽度
        canvasContainer.setMinHeight(150); // 最小高度
        canvasContainer.setPrefHeight(MINIMAP_HEIGHT);  // 默认高度
        // 移除maxHeight限制，让容器能够自动扩展
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);  // 让容器能够扩展
        
        // 缩略画布 - 设置初始大小
        canvas = new Canvas(MINIMAP_WIDTH, MINIMAP_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        // 监听容器宽度变化,动态调整 canvas 大小
        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            double w = newVal.doubleValue();
            if (w > 0) {
                canvas.setWidth(w);
                scheduleThrottledUpdate();  // 使用节流更新
            }
        });
        
        // 监听容器高度变化,动态调整 canvas 高度
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            double h = newVal.doubleValue();
            if (h > 0) {
                canvas.setHeight(h);
                scheduleThrottledUpdate();  // 使用节流更新
            }
        });
        
        // 视口矩形
        viewportRect = new Rectangle();
        viewportRect.setFill(Color.TRANSPARENT);
        viewportRect.setStroke(Color.web("#2563EB"));
        viewportRect.setStrokeWidth(2);
        viewportRect.setMouseTransparent(false);
        
        canvasContainer.getChildren().addAll(canvas, viewportRect);
        
        getChildren().addAll(titleBar, canvasContainer);
        
        // 拖拽视口框移动画布（不再使用点击小地图跳转）
        setupViewportDrag();
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        // 标题
        Label titleLabel = new Label("小地图");
        titleLabel.setStyle(StyleUtil.caption() + "-fx-font-weight: 600;");
        
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        // 弹出按钮
        detachBtn = new Button("", IconUtil.windowIcon());
        StyleUtil.applyIconButtonHover(detachBtn);
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnAction(e -> {
            if (onDetach != null) {
                onDetach.run();
            }
        });
        
        // 关闭按钮
        closeBtn = new Button("", IconUtil.closeIcon());
        StyleUtil.applyIconButtonHover(closeBtn);
        closeBtn.setTooltip(new Tooltip("关闭面板"));
        closeBtn.setOnAction(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        
        titleBar.getChildren().addAll(titleLabel, detachBtn, closeBtn);
        return titleBar;
    }
    
    /**
     * 绑定到主画布和滚动面板
     */
    public void bindTo(NodeCanvas nodeCanvas, ScrollPane scrollPane) {
        this.nodeCanvas = nodeCanvas;
        this.scrollPane = scrollPane;
        
        // 监听画布内容变化（节流：避免频繁更新）
        nodeCanvas.getChildren().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) c -> {
            scheduleThrottledUpdate();
        });
        
        // 监听滚动位置变化（使用节流，避免频繁更新）
        scrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            scheduleThrottledViewportUpdate();
        });
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            scheduleThrottledViewportUpdate();
        });
        
        // 监听视口大小变化（使用节流）
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            scheduleThrottledViewportUpdate();
        });
        
        // 添加定时器，定期更新视口（解决 pannable 模式下监听器不触发的问题）
        // 优化：降低更新频率从100ms到300ms，减少CPU占用
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(300), // 从100ms增加到300ms
                e -> {
                    // 定期检查滚动位置
                    double currentH = scrollPane.getHvalue();
                    double currentV = scrollPane.getVvalue();
                    if (Math.abs(lastHValue - currentH) > 0.001 || Math.abs(lastVValue - currentV) > 0.001) {
                        lastHValue = currentH;
                        lastVValue = currentV;
                        updateViewport();
                    }
                }
            )
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
        
        // 初始更新
        updateMiniMap();
    }
    
    /**
     * 更新小地图内容
     */
    private void updateMiniMap() {
        if (nodeCanvas == null) return;
        
        // 使用成员变量 gc
        if (gc == null) {
            gc = canvas.getGraphicsContext2D();
        }
        
        // 获取Canvas的实际宽高（而不是固定值）
        double canvasW = canvas.getWidth();
        double canvasH = canvas.getHeight();
        
        // 如果Canvas还没有初始化宽高，使用默认值
        if (canvasW <= 0) canvasW = MINIMAP_WIDTH;
        if (canvasH <= 0) canvasH = MINIMAP_HEIGHT;
        
        // 清空画布
        gc.clearRect(0, 0, canvasW, canvasH);
        
        // 绘制背景
        gc.setFill(Color.web("#F3F4F6"));
        gc.fillRect(0, 0, canvasW, canvasH);
        
        // 获取画布尺寸
        double canvasWidth = nodeCanvas.getPrefWidth();
        double canvasHeight = nodeCanvas.getPrefHeight();
        
        // 计算缩放比例
        double scaleX = MINIMAP_WIDTH / canvasWidth;
        double scaleY = MINIMAP_HEIGHT / canvasHeight;
        double scale = Math.min(scaleX, scaleY);
        
        // 计算偏移，使内容居中
        double offsetX = (MINIMAP_WIDTH - canvasWidth * scale) / 2;
        double offsetY = (MINIMAP_HEIGHT - canvasHeight * scale) / 2;
        
        // 先绘制连接线（在节点下方）
        gc.setStroke(Color.web("#6B7280"));
        gc.setLineWidth(1);
        
        nodeCanvas.getConnections().forEach(conn -> {
            // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
            javafx.scene.Node sourceOwner = conn.getSourceOwner();
            javafx.scene.Node targetOwner = conn.getTargetOwner();
            
            if (sourceOwner == null || targetOwner == null) {
                return; // 跳过无效的连接
            }
            
            double x1, y1, x2, y2;
            
            // 获取源节点/容器的位置
            if (sourceOwner instanceof ProcessNode) {
                ProcessNode sourceNode = (ProcessNode) sourceOwner;
                x1 = sourceNode.getLayoutX() * scale + offsetX + sourceNode.getPrefWidth() * scale / 2;
                y1 = sourceNode.getLayoutY() * scale + offsetY + sourceNode.getPrefHeight() * scale / 2;
            } else if (sourceOwner instanceof GroupContainer) {
                GroupContainer sourceContainer = (GroupContainer) sourceOwner;
                x1 = sourceContainer.getLayoutX() * scale + offsetX + sourceContainer.getFrame().getWidth() * scale / 2;
                y1 = sourceContainer.getLayoutY() * scale + offsetY + sourceContainer.getFrame().getHeight() * scale / 2;
            } else {
                return; // 跳过未知类型的连接
            }
            
            // 获取目标节点/容器的位置
            if (targetOwner instanceof ProcessNode) {
                ProcessNode targetNode = (ProcessNode) targetOwner;
                x2 = targetNode.getLayoutX() * scale + offsetX + targetNode.getPrefWidth() * scale / 2;
                y2 = targetNode.getLayoutY() * scale + offsetY + targetNode.getPrefHeight() * scale / 2;
            } else if (targetOwner instanceof GroupContainer) {
                GroupContainer targetContainer = (GroupContainer) targetOwner;
                x2 = targetContainer.getLayoutX() * scale + offsetX + targetContainer.getFrame().getWidth() * scale / 2;
                y2 = targetContainer.getLayoutY() * scale + offsetY + targetContainer.getFrame().getHeight() * scale / 2;
            } else {
                return; // 跳过未知类型的连接
            }
            
            gc.strokeLine(x1, y1, x2, y2);
        });
        
        // 再绘制节点（覆盖在线条上面）- 使用统一的颜色，不区分节点类型
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.web("#9CA3AF")); // 统一的灰色边框
        gc.setLineWidth(1);
        
        nodeCanvas.getNodes().forEach(node -> {
            double x = node.getLayoutX() * scale + offsetX;
            double y = node.getLayoutY() * scale + offsetY;
            double w = node.getPrefWidth() * scale;
            double h = node.getPrefHeight() * scale;
            
            // 绘制节点矩形 - 白色填充，统一灰色边框
            gc.fillRoundRect(x, y, w, h, 3, 3);
            gc.strokeRoundRect(x, y, w, h, 3, 3);
        });
        
        updateViewport();
    }
    
    /**
     * 更新视口矩形位置（用于 pannable 模式）
     */
    private void updateViewportForPannable() {
        if (nodeCanvas == null || scrollPane == null) return;
        
        
        double canvasWidth = nodeCanvas.getPrefWidth();
        double canvasHeight = nodeCanvas.getPrefHeight();
        
        // 计算缩放比例
        double scaleX = MINIMAP_WIDTH / canvasWidth;
        double scaleY = MINIMAP_HEIGHT / canvasHeight;
        double scale = Math.min(scaleX, scaleY);
        
        // 计算偏移
        double offsetX = (MINIMAP_WIDTH - canvasWidth * scale) / 2;
        double offsetY = (MINIMAP_HEIGHT - canvasHeight * scale) / 2;
        
        // 获取视口信息
        Bounds viewportBounds = scrollPane.getViewportBounds();
        double viewportWidth = viewportBounds.getWidth();
        double viewportHeight = viewportBounds.getHeight();
        
        // 在 pannable 模式下，使用画布的 translate 属性计算可见区域
        double translateX = -nodeCanvas.getTranslateX(); // 注意取负值
        double translateY = -nodeCanvas.getTranslateY();
        
        // 限制在有效范围内
        translateX = Math.max(0, Math.min(translateX, canvasWidth - viewportWidth));
        translateY = Math.max(0, Math.min(translateY, canvasHeight - viewportHeight));
        
        
        // 计算视口矩形
        double rectX = translateX * scale + offsetX;
        double rectY = translateY * scale + offsetY;
        double rectW = Math.min(viewportWidth * scale, canvasWidth * scale);
        double rectH = Math.min(viewportHeight * scale, canvasHeight * scale);
        
        // 确保矩形不超出小地图边界
        rectX = Math.max(offsetX, Math.min(rectX, offsetX + canvasWidth * scale - rectW));
        rectY = Math.max(offsetY, Math.min(rectY, offsetY + canvasHeight * scale - rectH));
        
        viewportRect.setX(rectX);
        viewportRect.setY(rectY);
        viewportRect.setWidth(rectW);
        viewportRect.setHeight(rectH);
        
        
        // 如果矩形大小异常，隐藏它
        if (rectW <= 0 || rectH <= 0 || rectW > MINIMAP_WIDTH || rectH > MINIMAP_HEIGHT) {
            viewportRect.setVisible(false);
        } else {
            viewportRect.setVisible(true);
        }
    }
    
    /**
     * 更新视口矩形位置（用于标准滚动条模式）
     */
    private void updateViewport() {
        if (nodeCanvas == null || scrollPane == null) return;
        if (isDraggingViewport) return;

        double canvasWidth = nodeCanvas.getPrefWidth();
        double canvasHeight = nodeCanvas.getPrefHeight();
        
        
        // 计算缩放比例
        double scaleX = MINIMAP_WIDTH / canvasWidth;
        double scaleY = MINIMAP_HEIGHT / canvasHeight;
        double scale = Math.min(scaleX, scaleY);
        
        // 计算偏移
        double offsetX = (MINIMAP_WIDTH - canvasWidth * scale) / 2;
        double offsetY = (MINIMAP_HEIGHT - canvasHeight * scale) / 2;
        
        // 获取视口信息
        Bounds viewportBounds = scrollPane.getViewportBounds();
        double viewportWidth = viewportBounds.getWidth();
        double viewportHeight = viewportBounds.getHeight();
        
        // 计算滚动位置
        double hValue = scrollPane.getHvalue();
        double vValue = scrollPane.getVvalue();
        
        
        double contentWidth = canvasWidth - viewportWidth;
        double contentHeight = canvasHeight - viewportHeight;
        
        
        double scrollX = contentWidth > 0 ? hValue * contentWidth : 0;
        double scrollY = contentHeight > 0 ? vValue * contentHeight : 0;
        
        
        // 更新视口矩形 - 限制在小地图可见范围内
        double rectX = scrollX * scale + offsetX;
        double rectY = scrollY * scale + offsetY;
        double rectW = Math.min(viewportWidth * scale, canvasWidth * scale);
        double rectH = Math.min(viewportHeight * scale, canvasHeight * scale);
        
        // 确保矩形不超出小地图边界
        rectX = Math.max(offsetX, Math.min(rectX, offsetX + canvasWidth * scale - rectW));
        rectY = Math.max(offsetY, Math.min(rectY, offsetY + canvasHeight * scale - rectH));
        
        // 如果内容完全可见（没有滚动条），视口矩形应该覆盖整个内容
        if (contentWidth <= 0 && contentHeight <= 0) {
            rectX = offsetX;
            rectY = offsetY;
            rectW = canvasWidth * scale;
            rectH = canvasHeight * scale;
        }
        
        viewportRect.setX(rectX);
        viewportRect.setY(rectY);
        viewportRect.setWidth(rectW);
        viewportRect.setHeight(rectH);
        
        
        // 如果矩形大小异常，隐藏它
        if (rectW <= 0 || rectH <= 0 || rectW > MINIMAP_WIDTH || rectH > MINIMAP_HEIGHT) {
            viewportRect.setVisible(false);
        } else {
            viewportRect.setVisible(true);
        }
    }
    
    /**
     * 设置拖拽视口框：拖拽蓝框时移动主画布视图（替代原点击小地图跳转）
     */
    private void setupViewportDrag() {
        viewportRect.setOnMousePressed(e -> {
            if (nodeCanvas == null || scrollPane == null) return;
            isDraggingViewport = true;
            dragAnchorX = e.getX();
            dragAnchorY = e.getY();
            dragAnchorRectX = viewportRect.getX();
            dragAnchorRectY = viewportRect.getY();
        });

        viewportRect.setOnMouseDragged(e -> {
            if (nodeCanvas == null || scrollPane == null) return;
            double canvasWidth = nodeCanvas.getPrefWidth();
            double canvasHeight = nodeCanvas.getPrefHeight();
            double scaleX = MINIMAP_WIDTH / canvasWidth;
            double scaleY = MINIMAP_HEIGHT / canvasHeight;
            double scale = Math.min(scaleX, scaleY);
            double offsetX = (MINIMAP_WIDTH - canvasWidth * scale) / 2;
            double offsetY = (MINIMAP_HEIGHT - canvasHeight * scale) / 2;
            double rectW = viewportRect.getWidth();
            double rectH = viewportRect.getHeight();

            double newRectX = dragAnchorRectX + (e.getX() - dragAnchorX);
            double newRectY = dragAnchorRectY + (e.getY() - dragAnchorY);
            double minX = offsetX;
            double minY = offsetY;
            double maxX = offsetX + canvasWidth * scale - rectW;
            double maxY = offsetY + canvasHeight * scale - rectH;
            newRectX = Math.max(minX, Math.min(newRectX, maxX));
            newRectY = Math.max(minY, Math.min(newRectY, maxY));

            viewportRect.setX(newRectX);
            viewportRect.setY(newRectY);

            Bounds viewportBounds = scrollPane.getViewportBounds();
            double viewportWidth = viewportBounds.getWidth();
            double viewportHeight = viewportBounds.getHeight();
            double contentWidth = canvasWidth - viewportWidth;
            double contentHeight = canvasHeight - viewportHeight;
            double scrollX = (newRectX - offsetX) / scale;
            double scrollY = (newRectY - offsetY) / scale;
            scrollX = Math.max(0, Math.min(scrollX, contentWidth > 0 ? contentWidth : 0));
            scrollY = Math.max(0, Math.min(scrollY, contentHeight > 0 ? contentHeight : 0));
            if (contentWidth > 0) {
                scrollPane.setHvalue(scrollX / contentWidth);
            }
            if (contentHeight > 0) {
                scrollPane.setVvalue(scrollY / contentHeight);
            }
        });

        viewportRect.setOnMouseReleased(e -> {
            isDraggingViewport = false;
        });
    }
    
    /**
     * 节流更新：延迟执行更新，避免频繁重绘
     */
    private void scheduleThrottledUpdate() {
        if (updatePending) {
            return; // 如果已有待处理的更新，跳过
        }
        
        updatePending = true;
        
        // 取消之前的定时器
        if (throttledUpdateTimeline != null) {
            throttledUpdateTimeline.stop();
        }
        
        // 创建新的节流定时器
        throttledUpdateTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(THROTTLE_DELAY_MS),
                e -> {
                    updateMiniMap();
                    updatePending = false;
                }
            )
        );
        throttledUpdateTimeline.play();
    }
    
    /**
     * 节流更新视口：延迟执行视口更新，避免频繁重绘
     */
    private void scheduleThrottledViewportUpdate() {
        if (viewportUpdatePending) {
            return; // 如果已有待处理的更新，跳过
        }
        
        viewportUpdatePending = true;
        
        // 取消之前的定时器
        if (throttledViewportUpdateTimeline != null) {
            throttledViewportUpdateTimeline.stop();
        }
        
        // 创建新的节流定时器
        throttledViewportUpdateTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(THROTTLE_DELAY_MS),
                e -> {
                    updateViewport();
                    viewportUpdatePending = false;
                }
            )
        );
        throttledViewportUpdateTimeline.play();
    }
    
    /**
     * 手动触发更新（立即执行，不使用节流）
     */
    public void refresh() {
        updatePending = false;
        if (throttledUpdateTimeline != null) {
            throttledUpdateTimeline.stop();
        }
        updateMiniMap();
    }
    
    /**
     * 设置关闭回调
     */
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
    
    /**
     * 设置弹出回调
     */
    public void setOnDetach(Runnable callback) {
        this.onDetach = callback;
    }
    
    /**
     * 设置弹出和关闭按钮的可见性（用于面板弹出为独立窗口时隐藏按钮）
     */
    public void setDetachButtonsVisible(boolean visible) {
        if (detachBtn != null) {
            detachBtn.setVisible(visible);
            detachBtn.setManaged(visible);
        }
        if (closeBtn != null) {
            closeBtn.setVisible(visible);
            closeBtn.setManaged(visible);
        }
    }
    
    /**
     * 获取Canvas（用于调试）
     */
    public Canvas getCanvas() {
        return canvas;
    }
}

