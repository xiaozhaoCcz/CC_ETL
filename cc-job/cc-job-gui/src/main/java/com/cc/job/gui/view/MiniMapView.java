package com.cc.job.gui.view;

import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
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

/**
 * 小地图组件 - 显示画布缩略图和当前视图位置
 */
public class MiniMapView extends VBox {
    
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
    
    // 关闭回调
    private Runnable onClose;
    
    // 弹出回调
    private Runnable onDetach;
    
    public MiniMapView() {
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95); " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);"
        );
        setPadding(new Insets(8));
        setSpacing(5);
        setMinWidth(200);  // 最小宽度200px
        setMinHeight(200); // 最小高度
        setPrefHeight(MINIMAP_HEIGHT + 40);  // 默认高度
        // 移除maxHeight限制，让它能在弹出窗口时自动扩展
        
        // 标题栏
        HBox titleBar = createTitleBar();
        
        // 画布容器 - 使用 Pane 支持绝对定位！
        Pane canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #F3F4F6; -fx-border-color: #D1D5DB; -fx-border-width: 1;");
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
                System.out.println("📐 Canvas宽度调整为: " + w);
                updateMiniMap();  // 重绘小地图
            }
        });
        
        // 监听容器高度变化,动态调整 canvas 高度
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            double h = newVal.doubleValue();
            if (h > 0) {
                canvas.setHeight(h);
                System.out.println("📐 Canvas高度调整为: " + h);
                updateMiniMap();  // 重绘小地图
            }
        });
        
        // 视口矩形
        viewportRect = new Rectangle();
        viewportRect.setFill(Color.TRANSPARENT);
        viewportRect.setStroke(Color.web("#EF4444"));
        viewportRect.setStrokeWidth(2);
        viewportRect.setMouseTransparent(false);
        
        canvasContainer.getChildren().addAll(canvas, viewportRect);
        
        getChildren().addAll(titleBar, canvasContainer);
        
        // 点击小地图跳转
        setupClickNavigation();
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        // 标题
        Label titleLabel = new Label("小地图");
        titleLabel.setStyle(StyleUtil.caption() + "-fx-font-weight: 600;");
        
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        // 弹出按钮
        Button detachBtn = new Button("", IconUtil.windowIcon());
        StyleUtil.applyIconButtonHover(detachBtn);
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnAction(e -> {
            if (onDetach != null) {
                onDetach.run();
            }
        });
        
        // 关闭按钮
        Button closeBtn = new Button("", IconUtil.closeIcon());
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
        
        // 监听画布内容变化
        nodeCanvas.getChildren().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) c -> {
            updateMiniMap();
        });
        
        // 监听滚动位置变化
        scrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("🔄 水平滚动: " + oldVal + " → " + newVal);
            updateViewport();
        });
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("🔄 垂直滚动: " + oldVal + " → " + newVal);
            updateViewport();
        });
        
        // 监听视口大小变化
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("🔄 视口大小变化: " + newVal);
            updateViewport();
        });
        
        // 添加定时器，定期更新视口（解决 pannable 模式下监听器不触发的问题）
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(100),
                e -> {
                    // 定期检查滚动位置
                    double currentH = scrollPane.getHvalue();
                    double currentV = scrollPane.getVvalue();
                    if (lastHValue != currentH || lastVValue != currentV) {
                        System.out.println("🔄 检测到滚动变化: H=" + currentH + ", V=" + currentV);
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
        
        // 绘制节点
        gc.setFill(Color.web("#8B5CF6"));
        gc.setStroke(Color.web("#7C3AED"));
        gc.setLineWidth(1);
        
        nodeCanvas.getNodes().forEach(node -> {
            double x = node.getLayoutX() * scale + offsetX;
            double y = node.getLayoutY() * scale + offsetY;
            double w = node.getPrefWidth() * scale;
            double h = node.getPrefHeight() * scale;
            
            // 绘制节点矩形
            gc.fillRoundRect(x, y, w, h, 3, 3);
            gc.strokeRoundRect(x, y, w, h, 3, 3);
        });
        
        // 绘制连接线
        gc.setStroke(Color.web("#6B7280"));
        gc.setLineWidth(1);
        
        nodeCanvas.getConnections().forEach(conn -> {
            double x1 = conn.getSourceNode().getLayoutX() * scale + offsetX + 
                       conn.getSourceNode().getPrefWidth() * scale / 2;
            double y1 = conn.getSourceNode().getLayoutY() * scale + offsetY + 
                       conn.getSourceNode().getPrefHeight() * scale / 2;
            double x2 = conn.getTargetNode().getLayoutX() * scale + offsetX + 
                       conn.getTargetNode().getPrefWidth() * scale / 2;
            double y2 = conn.getTargetNode().getLayoutY() * scale + offsetY + 
                       conn.getTargetNode().getPrefHeight() * scale / 2;
            
            gc.strokeLine(x1, y1, x2, y2);
        });
        
        updateViewport();
    }
    
    /**
     * 更新视口矩形位置（用于 pannable 模式）
     */
    private void updateViewportForPannable() {
        if (nodeCanvas == null || scrollPane == null) return;
        
        System.out.println("📍 更新视口矩形（Pannable模式）...");
        
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
        
        System.out.println("   平移: X=" + translateX + ", Y=" + translateY);
        System.out.println("   视口尺寸: " + viewportWidth + " x " + viewportHeight);
        
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
        
        System.out.println("   视口矩形: [" + rectX + ", " + rectY + ", " + rectW + ", " + rectH + "]");
        
        // 如果矩形大小异常，隐藏它
        if (rectW <= 0 || rectH <= 0 || rectW > MINIMAP_WIDTH || rectH > MINIMAP_HEIGHT) {
            viewportRect.setVisible(false);
            System.out.println("   ❌ 视口矩形隐藏（大小异常）");
        } else {
            viewportRect.setVisible(true);
            System.out.println("   ✅ 视口矩形显示");
        }
    }
    
    /**
     * 更新视口矩形位置（用于标准滚动条模式）
     */
    private void updateViewport() {
        if (nodeCanvas == null || scrollPane == null) return;
        
        System.out.println("📍 更新视口矩形（标准模式）...");
        
        double canvasWidth = nodeCanvas.getPrefWidth();
        double canvasHeight = nodeCanvas.getPrefHeight();
        
        System.out.println("   画布尺寸: " + canvasWidth + " x " + canvasHeight);
        
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
        
        System.out.println("   滚动值: H=" + hValue + ", V=" + vValue);
        
        double contentWidth = canvasWidth - viewportWidth;
        double contentHeight = canvasHeight - viewportHeight;
        
        System.out.println("   内容尺寸: " + contentWidth + " x " + contentHeight);
        
        double scrollX = contentWidth > 0 ? hValue * contentWidth : 0;
        double scrollY = contentHeight > 0 ? vValue * contentHeight : 0;
        
        System.out.println("   滚动位置: X=" + scrollX + ", Y=" + scrollY);
        
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
        
        System.out.println("   视口矩形: [" + rectX + ", " + rectY + ", " + rectW + ", " + rectH + "]");
        
        // 如果矩形大小异常，隐藏它
        if (rectW <= 0 || rectH <= 0 || rectW > MINIMAP_WIDTH || rectH > MINIMAP_HEIGHT) {
            viewportRect.setVisible(false);
            System.out.println("   ❌ 视口矩形隐藏（大小异常）");
        } else {
            viewportRect.setVisible(true);
            System.out.println("   ✅ 视口矩形显示");
        }
    }
    
    /**
     * 设置点击导航
     */
    private void setupClickNavigation() {
        canvas.setOnMouseClicked(e -> {
            if (nodeCanvas == null || scrollPane == null) return;
            
            // 获取点击位置
            double clickX = e.getX();
            double clickY = e.getY();
            
            // 计算对应的画布位置
            double canvasWidth = nodeCanvas.getPrefWidth();
            double canvasHeight = nodeCanvas.getPrefHeight();
            
            double scaleX = MINIMAP_WIDTH / canvasWidth;
            double scaleY = MINIMAP_HEIGHT / canvasHeight;
            double scale = Math.min(scaleX, scaleY);
            
            double offsetX = (MINIMAP_WIDTH - canvasWidth * scale) / 2;
            double offsetY = (MINIMAP_HEIGHT - canvasHeight * scale) / 2;
            
            double targetX = (clickX - offsetX) / scale;
            double targetY = (clickY - offsetY) / scale;
            
            // 计算滚动值
            Bounds viewportBounds = scrollPane.getViewportBounds();
            double viewportWidth = viewportBounds.getWidth();
            double viewportHeight = viewportBounds.getHeight();
            
            double contentWidth = canvasWidth - viewportWidth;
            double contentHeight = canvasHeight - viewportHeight;
            
            // 将目标位置居中
            double scrollX = targetX - viewportWidth / 2;
            double scrollY = targetY - viewportHeight / 2;
            
            // 限制范围
            scrollX = Math.max(0, Math.min(scrollX, contentWidth));
            scrollY = Math.max(0, Math.min(scrollY, contentHeight));
            
            // 设置滚动值
            if (contentWidth > 0) {
                scrollPane.setHvalue(scrollX / contentWidth);
            }
            if (contentHeight > 0) {
                scrollPane.setVvalue(scrollY / contentHeight);
            }
        });
    }
    
    /**
     * 手动触发更新
     */
    public void refresh() {
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
     * 获取Canvas（用于调试）
     */
    public Canvas getCanvas() {
        return canvas;
    }
}

