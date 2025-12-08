package com.cc.job.gui.util;

import com.cc.job.gui.view.MiniMapView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 可分离面板工具类
 * 支持将面板弹出为独立窗口，并可以恢复到原位置
 */
public class DetachablePanel {
    
    private static final Logger logger = LoggerFactory.getLogger(DetachablePanel.class);
    
    private Region content;              // 面板内容
    private Pane originalParent;         // 原始父容器
    private int originalIndex;           // 在父容器中的原始索引
    private Stage detachedStage;         // 弹出的窗口
    private boolean isDetached = false;  // 是否已弹出
    
    private String title;                // 窗口标题
    private double defaultWidth = 800;   // 默认宽度
    private double defaultHeight = 600;  // 默认高度
    
    private Runnable onDetach;           // 弹出时的回调
    private Runnable onReattach;         // 恢复时的回调
    
    public DetachablePanel(Region content, String title) {
        this.content = content;
        this.title = title;
    }
    
    /**
     * 弹出面板为独立窗口
     */
    public void detach() {
        if (isDetached) {
            // 如果已经弹出，只是显示窗口
            if (detachedStage != null) {
                detachedStage.show();
                detachedStage.toFront();
            }
            return;
        }
        
        // 记录原始位置
        if (content.getParent() instanceof Pane) {
            originalParent = (Pane) content.getParent();
            originalIndex = originalParent.getChildren().indexOf(content);
            
            // 从原始容器中移除
            originalParent.getChildren().remove(content);
        }
        
        // 创建新窗口
        detachedStage = new Stage();
        detachedStage.setTitle(title);
        detachedStage.initStyle(StageStyle.DECORATED);
        
        // 使用StackPane包装content，确保content能够自动填充窗口
        StackPane root = new StackPane(content);
        
        // 确保content能够填充整个StackPane
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // 创建场景
        Scene scene = new Scene(root, defaultWidth, defaultHeight);
        detachedStage.setScene(scene);
        
        // 窗口关闭时自动恢复到原位置
        detachedStage.setOnCloseRequest(e -> {
            reattach();
        });
        
        isDetached = true;
        detachedStage.show();
        
        // 强制刷新内容布局（修复白板问题）
        // 使用多级runLater确保布局完全完成
        Platform.runLater(() -> {
            content.requestLayout();
            Platform.runLater(() -> {
                // 如果是MiniMapView，需要手动触发刷新
                if (content instanceof MiniMapView) {
                    Platform.runLater(() -> {
                        ((MiniMapView) content).refresh();
                    });
                }
            });
        });
        
        // 触发弹出回调
        if (onDetach != null) {
            onDetach.run();
        }
        
    }
    
    /**
     * 恢复面板到原位置
     */
    public void reattach() {
        if (!isDetached) {
            return;
        }
        
        // 从窗口中移除内容
        if (detachedStage != null) {
            detachedStage.hide();
            detachedStage.setScene(null);
        }
        
        // 恢复到原始容器
        if (originalParent != null) {
            if (originalIndex >= 0 && originalIndex <= originalParent.getChildren().size()) {
                originalParent.getChildren().add(originalIndex, content);
            } else {
                originalParent.getChildren().add(content);
            }
        }
        
        isDetached = false;
        detachedStage = null;
        
        // 触发恢复回调
        if (onReattach != null) {
            onReattach.run();
        }
        
    }
    
    /**
     * 切换弹出/恢复状态
     */
    public void toggle() {
        if (isDetached) {
            reattach();
        } else {
            detach();
        }
    }
    
    // Getters and Setters
    
    public boolean isDetached() {
        return isDetached;
    }
    
    public void setDefaultSize(double width, double height) {
        this.defaultWidth = width;
        this.defaultHeight = height;
    }
    
    public void setOnDetach(Runnable callback) {
        this.onDetach = callback;
    }
    
    public void setOnReattach(Runnable callback) {
        this.onReattach = callback;
    }
    
    public Stage getDetachedStage() {
        return detachedStage;
    }
}

