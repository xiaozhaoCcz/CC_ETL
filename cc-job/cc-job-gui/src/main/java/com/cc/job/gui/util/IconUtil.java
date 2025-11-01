package com.cc.job.gui.util;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.layout.StackPane;

/**
 * 图标工具类 - 统一管理界面图标
 * 使用Unicode符号和SVG形状
 */
public class IconUtil {
    
    // 默认颜色
    private static final String SECONDARY_COLOR = "#64748B";
    private static final String WHITE_COLOR = "#FFFFFF";
    
    /**
     * 创建文字图标
     */
    private static Label createTextIcon(String text, String color, int size) {
        Label label = new Label(text);
        label.setStyle(
            "-fx-text-fill: " + color + "; " +
            "-fx-font-size: " + size + "px; " +
            "-fx-font-weight: 600;"
        );
        return label;
    }
    
    // ============ 工具栏图标 ============
    
    public static Label fileIcon() {
        return createTextIcon("📄", SECONDARY_COLOR, 16);
    }
    
    public static Label folderIcon() {
        return createTextIcon("📂", SECONDARY_COLOR, 16);
    }
    
    public static Label saveIcon() {
        return createTextIcon("💾", SECONDARY_COLOR, 16);
    }
    
    public static Label undoIcon() {
        return createTextIcon("↶", SECONDARY_COLOR, 16);
    }
    
    public static Label redoIcon() {
        return createTextIcon("↷", SECONDARY_COLOR, 16);
    }
    
    public static Label zoomInIcon() {
        return createTextIcon("🔍+", SECONDARY_COLOR, 14);
    }
    
    public static Label zoomOutIcon() {
        return createTextIcon("🔍−", SECONDARY_COLOR, 14);
    }
    
    public static Label expandIcon() {
        return createTextIcon("⤢", SECONDARY_COLOR, 16);
    }
    
    public static Label playIcon() {
        return createTextIcon("▶", WHITE_COLOR, 14);
    }
    
    public static Label stopIcon() {
        return createTextIcon("◼", WHITE_COLOR, 14);
    }
    
    public static Label plusIcon() {
        return createTextIcon("+", SECONDARY_COLOR, 18);
    }
    
    // ============ 面板图标 ============
    
    public static Label searchIcon() {
        return createTextIcon("🔍", "#9CA3AF", 14);
    }
    
    public static Label closeIcon() {
        return createTextIcon("×", "#6B7280", 18);
    }
    
    public static Label windowIcon() {
        return createTextIcon("⧉", "#6B7280", 16);
    }
    
    public static Label trashIcon() {
        return createTextIcon("🗑", SECONDARY_COLOR, 16);
    }
    
    public static Label exportIcon() {
        return createTextIcon("⬇", SECONDARY_COLOR, 16);
    }
    
    public static Label refreshIcon() {
        return createTextIcon("⟳", SECONDARY_COLOR, 16);
    }
    
    // ============ 树节点图标 ============
    
    /**
     * 任务分区图标 - 立方体
     */
    public static StackPane partitionIcon() {
        // 使用SVG Path绘制立方体
        SVGPath cube = new SVGPath();
        cube.setContent("M 2 5 L 8 2 L 14 5 L 14 11 L 8 14 L 2 11 Z M 8 2 L 8 8 M 2 5 L 8 8 M 14 5 L 8 8");
        cube.setFill(Color.TRANSPARENT);
        cube.setStroke(Color.web("#3B82F6"));
        cube.setStrokeWidth(1.5);
        
        StackPane pane = new StackPane(cube);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 任务组图标 - 文件夹
     */
    public static StackPane taskGroupIcon() {
        SVGPath folder = new SVGPath();
        folder.setContent("M 2 4 L 6 4 L 7 2 L 14 2 L 14 12 L 2 12 Z");
        folder.setFill(Color.web("#8B5CF6"));
        folder.setStroke(Color.web("#7C3AED"));
        folder.setStrokeWidth(1.2);
        
        StackPane pane = new StackPane(folder);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 任务容器图标 - 列表
     */
    public static StackPane taskContainerIcon() {
        SVGPath list = new SVGPath();
        list.setContent("M 2 3 L 14 3 M 2 7 L 14 7 M 2 11 L 14 11 M 3 3 L 3 3.5 M 3 7 L 3 7.5 M 3 11 L 3 11.5");
        list.setFill(Color.TRANSPARENT);
        list.setStroke(Color.web("#10B981"));
        list.setStrokeWidth(1.5);
        list.setStrokeLineCap(StrokeLineCap.ROUND);
        
        StackPane pane = new StackPane(list);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 关系容器图标 - 连接节点
     */
    public static StackPane relationContainerIcon() {
        SVGPath network = new SVGPath();
        network.setContent("M 3 3 L 8 8 M 13 3 L 8 8 M 8 8 L 8 13 M 3 3 A 1 1 0 1 1 3 3.01 M 13 3 A 1 1 0 1 1 13 3.01 M 8 13 A 1 1 0 1 1 8 13.01");
        network.setFill(Color.TRANSPARENT);
        network.setStroke(Color.web("#F59E0B"));
        network.setStrokeWidth(1.5);
        network.setStrokeLineCap(StrokeLineCap.ROUND);
        
        StackPane pane = new StackPane(network);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 任务节点图标 - 圆点带边框
     */
    public static StackPane taskNodeIcon() {
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(5);
        circle.setFill(Color.web("#06B6D4"));
        circle.setStroke(Color.web("#0891B2"));
        circle.setStrokeWidth(1.5);
        
        StackPane pane = new StackPane(circle);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 关系边图标 - 箭头
     */
    public static StackPane relationEdgeIcon() {
        SVGPath arrow = new SVGPath();
        arrow.setContent("M 2 8 L 12 8 M 8 4 L 12 8 L 8 12");
        arrow.setFill(Color.TRANSPARENT);
        arrow.setStroke(Color.web("#EC4899"));
        arrow.setStrokeWidth(1.5);
        arrow.setStrokeLineCap(StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(StrokeLineJoin.ROUND);
        
        StackPane pane = new StackPane(arrow);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    // ============ 日志级别图标 ============
    
    /**
     * 信息图标 - 圆形i
     */
    public static StackPane infoIcon() {
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(6);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web("#2563EB"));
        circle.setStrokeWidth(1.5);
        
        SVGPath i = new SVGPath();
        i.setContent("M 8 5 L 8 11 M 8 3 L 8 3.5");
        i.setStroke(Color.web("#2563EB"));
        i.setStrokeWidth(1.8);
        i.setStrokeLineCap(StrokeLineCap.ROUND);
        
        StackPane pane = new StackPane(circle, i);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 警告图标 - 三角形感叹号
     */
    public static StackPane warnIcon() {
        SVGPath triangle = new SVGPath();
        triangle.setContent("M 8 2 L 14 13 L 2 13 Z");
        triangle.setFill(Color.TRANSPARENT);
        triangle.setStroke(Color.web("#D97706"));
        triangle.setStrokeWidth(1.5);
        
        SVGPath exclamation = new SVGPath();
        exclamation.setContent("M 8 6 L 8 9 M 8 11 L 8 11.5");
        exclamation.setStroke(Color.web("#D97706"));
        exclamation.setStrokeWidth(1.8);
        exclamation.setStrokeLineCap(StrokeLineCap.ROUND);
        
        StackPane pane = new StackPane(triangle, exclamation);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 错误图标 - 圆形X
     */
    public static StackPane errorIcon() {
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(6);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web("#DC2626"));
        circle.setStrokeWidth(1.5);
        
        SVGPath x = new SVGPath();
        x.setContent("M 5 5 L 11 11 M 11 5 L 5 11");
        x.setStroke(Color.web("#DC2626"));
        x.setStrokeWidth(1.8);
        x.setStrokeLineCap(StrokeLineCap.ROUND);
        
        StackPane pane = new StackPane(circle, x);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 成功图标 - 圆形对勾
     */
    public static StackPane successIcon() {
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(6);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web("#059669"));
        circle.setStrokeWidth(1.5);
        
        SVGPath check = new SVGPath();
        check.setContent("M 5 8 L 7 10 L 11 6");
        check.setStroke(Color.web("#059669"));
        check.setStrokeWidth(1.8);
        check.setStrokeLineCap(StrokeLineCap.ROUND);
        check.setStrokeLineJoin(StrokeLineJoin.ROUND);
        
        StackPane pane = new StackPane(circle, check);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    /**
     * 调试图标 - 虫子
     */
    public static StackPane debugIcon() {
        SVGPath bug = new SVGPath();
        bug.setContent("M 8 3 L 8 13 M 5 5 L 11 5 M 5 8 L 11 8 M 5 11 L 11 11 M 5 3 L 3 1 M 11 3 L 13 1 M 5 6 L 3 6 M 11 6 L 13 6 M 5 10 L 3 10 M 11 10 L 13 10");
        bug.setFill(Color.TRANSPARENT);
        bug.setStroke(Color.web("#7C3AED"));
        bug.setStrokeWidth(1.3);
        bug.setStrokeLineCap(StrokeLineCap.ROUND);
        
        StackPane pane = new StackPane(bug);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
    
    // ============ 状态图标 ============
    
    public static Label runningIcon() {
        return createTextIcon("◷", "#10B981", 14);
    }
    
    public static Label chartIcon() {
        return createTextIcon("📊", "#6366F1", 16);
    }
    
    public static Label mapIcon() {
        return createTextIcon("🗺", "#6366F1", 16);
    }
    
    // ============ 辅助方法 ============
    
    /**
     * 根据节点类型获取图标
     */
    public static StackPane getIconByType(Integer type) {
        if (type == null) {
            Label label = createTextIcon("?", "#6B7280", 14);
            StackPane pane = new StackPane(label);
            pane.setMinSize(16, 16);
            pane.setMaxSize(16, 16);
            return pane;
        }
        return switch (type) {
            case 0 -> partitionIcon();
            case 1 -> taskGroupIcon();
            case 2 -> taskContainerIcon();
            case 3 -> relationContainerIcon();
            case 4 -> taskNodeIcon();
            case 5 -> relationEdgeIcon();
            default -> {
                Label label = createTextIcon("?", "#6B7280", 14);
                StackPane pane = new StackPane(label);
                pane.setMinSize(16, 16);
                pane.setMaxSize(16, 16);
                yield pane;
            }
        };
    }
}

