package com.cc.job.gui.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 画布标尺组件 - 显示在画布边缘的标尺
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class CanvasRuler extends Pane {
    
    private static final Logger logger = LoggerFactory.getLogger(CanvasRuler.class);
    
    private static final double RULER_SIZE = 20; // 标尺宽度/高度
    private static final double TICK_INTERVAL = 50; // 刻度间隔（像素）
    private static final double MAJOR_TICK_LENGTH = 10; // 主刻度长度
    private static final double MINOR_TICK_LENGTH = 5; // 次刻度长度
    
    private Canvas horizontalRuler; // 水平标尺
    private Canvas verticalRuler; // 垂直标尺
    private Label coordinateLabel; // 坐标显示标签
    
    private double canvasWidth = 2000;
    private double canvasHeight = 1000;
    private double viewportX = 0; // 视口X偏移
    private double viewportY = 0; // 视口Y偏移
    
    public CanvasRuler() {
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: transparent;"); // 透明背景，不覆盖画布
        
        // 创建水平标尺
        horizontalRuler = new Canvas();
        horizontalRuler.setHeight(RULER_SIZE);
        horizontalRuler.setMouseTransparent(true);
        
        // 创建垂直标尺
        verticalRuler = new Canvas();
        verticalRuler.setWidth(RULER_SIZE);
        verticalRuler.setMouseTransparent(true);
        
        // 创建坐标显示标签
        coordinateLabel = new Label("坐标: (0, 0)");
        coordinateLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-text-fill: #6B7280; " +
            "-fx-background-color: rgba(255, 255, 255, 0.9); " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 4 8 4 8;"
        );
        coordinateLabel.setVisible(false);
        
        getChildren().addAll(horizontalRuler, verticalRuler, coordinateLabel);
        
        // 布局：水平标尺在顶部，垂直标尺在左侧
        horizontalRuler.setLayoutX(RULER_SIZE);
        horizontalRuler.setLayoutY(0);
        verticalRuler.setLayoutX(0);
        verticalRuler.setLayoutY(RULER_SIZE);
        
        // 绘制标尺
        drawRulers();
    }
    
    /**
     * 更新画布尺寸
     */
    public void updateCanvasSize(double width, double height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
        horizontalRuler.setWidth(width);
        verticalRuler.setHeight(height);
        drawRulers();
    }
    
    /**
     * 更新视口偏移（用于滚动时更新标尺显示）
     */
    public void updateViewportOffset(double x, double y) {
        this.viewportX = x;
        this.viewportY = y;
        drawRulers();
    }
    
    /**
     * 更新坐标显示
     */
    public void updateCoordinate(double x, double y) {
        if (coordinateLabel != null) {
            coordinateLabel.setText(String.format("坐标: (%.0f, %.0f)", x, y));
        }
    }
    
    /**
     * 显示/隐藏坐标标签
     */
    public void setCoordinateLabelVisible(boolean visible) {
        if (coordinateLabel != null) {
            coordinateLabel.setVisible(visible);
        }
    }
    
    /**
     * 设置坐标标签位置
     */
    public void setCoordinateLabelPosition(double x, double y) {
        if (coordinateLabel != null) {
            coordinateLabel.setLayoutX(x);
            coordinateLabel.setLayoutY(y);
        }
    }
    
    /**
     * 绘制标尺
     */
    private void drawRulers() {
        drawHorizontalRuler();
        drawVerticalRuler();
    }
    
    /**
     * 绘制水平标尺
     */
    private void drawHorizontalRuler() {
        GraphicsContext gc = horizontalRuler.getGraphicsContext2D();
        gc.clearRect(0, 0, horizontalRuler.getWidth(), horizontalRuler.getHeight());
        
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(1.0);
        gc.setFill(Color.web("#374151"));
        gc.setFont(Font.font("Arial", 10));
        
        // 绘制底部边框
        gc.strokeLine(0, RULER_SIZE - 1, horizontalRuler.getWidth(), RULER_SIZE - 1);
        
        // 绘制刻度
        double startX = -viewportX % TICK_INTERVAL;
        for (double x = startX; x < horizontalRuler.getWidth(); x += TICK_INTERVAL) {
            double actualX = x + viewportX;
            
            // 主刻度（每100像素）
            if (actualX % (TICK_INTERVAL * 2) == 0) {
                gc.strokeLine(x, RULER_SIZE - MAJOR_TICK_LENGTH, x, RULER_SIZE);
                // 显示刻度值
                String label = String.valueOf((int) actualX);
                gc.fillText(label, x + 2, RULER_SIZE - MAJOR_TICK_LENGTH - 2);
            } else {
                // 次刻度
                gc.strokeLine(x, RULER_SIZE - MINOR_TICK_LENGTH, x, RULER_SIZE);
            }
        }
    }
    
    /**
     * 绘制垂直标尺
     */
    private void drawVerticalRuler() {
        GraphicsContext gc = verticalRuler.getGraphicsContext2D();
        gc.clearRect(0, 0, verticalRuler.getWidth(), verticalRuler.getHeight());
        
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(1.0);
        gc.setFill(Color.web("#374151"));
        gc.setFont(Font.font("Arial", 10));
        
        // 绘制右侧边框
        gc.strokeLine(RULER_SIZE - 1, 0, RULER_SIZE - 1, verticalRuler.getHeight());
        
        // 绘制刻度
        double startY = -viewportY % TICK_INTERVAL;
        for (double y = startY; y < verticalRuler.getHeight(); y += TICK_INTERVAL) {
            double actualY = y + viewportY;
            
            // 主刻度（每100像素）
            if (actualY % (TICK_INTERVAL * 2) == 0) {
                gc.strokeLine(RULER_SIZE - MAJOR_TICK_LENGTH, y, RULER_SIZE, y);
                // 显示刻度值（旋转90度）
                String label = String.valueOf((int) actualY);
                gc.save();
                gc.translate(RULER_SIZE - MAJOR_TICK_LENGTH - 2, y + 5);
                gc.rotate(-90);
                gc.fillText(label, 0, 0);
                gc.restore();
            } else {
                // 次刻度
                gc.strokeLine(RULER_SIZE - MINOR_TICK_LENGTH, y, RULER_SIZE, y);
            }
        }
    }
    
    /**
     * 获取标尺大小
     */
    public static double getRulerSize() {
        return RULER_SIZE;
    }
}
