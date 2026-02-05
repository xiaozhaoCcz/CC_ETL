package com.cc.job.gui.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 样式工具类 - 统一管理界面样式
 * 提供专业的现代化UI样式
 */
public class StyleUtil {
    
    // ============ 颜色定义 ============
    
    // 主色调（企业蓝）
    public static final String PRIMARY = "#2563EB";
    public static final String PRIMARY_DARK = "#1D4ED8";
    public static final String PRIMARY_LIGHT = "#3B82F6";
    
    // 成功/错误/警告
    public static final String SUCCESS = "#10B981";           // 绿色
    public static final String SUCCESS_DARK = "#059669";
    public static final String WARNING = "#F59E0B";           // 橙色
    public static final String WARNING_DARK = "#D97706";
    public static final String ERROR = "#EF4444";             // 红色
    public static final String ERROR_DARK = "#DC2626";
    
    // 灰度
    public static final String GRAY_50 = "#F9FAFB";
    public static final String GRAY_100 = "#F3F4F6";
    public static final String GRAY_200 = "#E5E7EB";
    public static final String GRAY_300 = "#D1D5DB";
    public static final String GRAY_400 = "#9CA3AF";
    public static final String GRAY_500 = "#6B7280";
    public static final String GRAY_600 = "#4B5563";
    public static final String GRAY_700 = "#374151";
    public static final String GRAY_800 = "#1F2937";
    public static final String GRAY_900 = "#111827";
    public static final String TEXT_PRIMARY = "#1F2937";
    public static final String TEXT_SECONDARY = "#6B7280";
    
    // 背景色
    public static final String BG_PRIMARY = "#FFFFFF";
    public static final String BG_SECONDARY = "#F9FAFB";
    public static final String BG_HOVER = "#F3F4F6";
    
    // ============ 阴影效果 ============
    
    public static final String SHADOW_SM = "dropshadow(gaussian, rgba(0,0,0,0.05), 2, 0, 0, 1)";
    public static final String SHADOW_MD = "dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2)";
    public static final String SHADOW_LG = "dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 4)";
    
    // ============ 圆角 ============
    
    public static final String RADIUS_SM = "4";
    public static final String RADIUS_MD = "6";
    public static final String RADIUS_LG = "8";
    public static final String BORDER = "";

    // ============ 按钮样式 ============
    
    /**
     * 主按钮样式（填充背景）
     */
    public static String primaryButton() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: %s; " +
            "-fx-cursor: hand;",
            PRIMARY, RADIUS_MD
        );
    }
    
    /**
     * 次要按钮样式（边框）
     */
    public static String secondaryButton() {
        return String.format(
            "-fx-background-color: white; " +
            "-fx-text-fill: %s; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 8 16; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-cursor: hand;",
            GRAY_700, GRAY_300, RADIUS_MD, RADIUS_MD
        );
    }
    
    /**
     * 成功按钮样式
     */
    public static String successButton() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: %s; " +
            "-fx-cursor: hand;",
            SUCCESS, RADIUS_MD
        );
    }
    
    /**
     * 错误按钮样式
     */
    public static String errorButton() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: %s; " +
            "-fx-cursor: hand;",
            ERROR, RADIUS_MD
        );
    }
    
    /**
     * 幽灵按钮样式（透明背景）
     */
    public static String ghostButton() {
        return String.format(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: %s; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: %s; " +
            "-fx-cursor: hand;",
            GRAY_600, RADIUS_MD
        );
    }
    
    /**
     * 图标按钮样式
     */
    public static String iconButton() {
        return String.format(
            "-fx-background-color: transparent; " +
            "-fx-padding: 6; " +
            "-fx-background-radius: %s; " +
            "-fx-cursor: hand;",
            RADIUS_SM
        );
    }
    
    // ============ 面板样式 ============
    
    /**
     * 卡片样式
     */
    public static String card() {
        return String.format(
            "-fx-background-color: -color-bg-elevated; " +
            "-fx-background-radius: %s;",
            RADIUS_LG
        );
    }
    
    /**
     * 侧边栏样式
     */
    public static String sidebar() {
        return "-fx-background-color: -color-bg-elevated; " +
               "-fx-border-color: -color-border-muted; " +
               "-fx-border-width: 0 1 0 0;";
    }
    
    /**
     * 工具栏样式
     */
    public static String toolbar() {
        return "-fx-background-color: -color-bg-elevated; " +
               "-fx-border-color: -color-border-muted; " +
               "-fx-border-width: 0 0 1 0; " +
               "-fx-padding: 12 16;";
    }
    
    /**
     * 标题栏样式
     */
    public static String titleBar() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-padding: 12 16; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 0 0 1 0;",
            GRAY_50, GRAY_200
        );
    }
    
    // ============ 输入框样式 ============
    
    /**
     * 搜索框样式
     */
    public static String searchField() {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 8 12; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-prompt-text-fill: %s;",
            BG_PRIMARY, GRAY_900, GRAY_300, RADIUS_MD, RADIUS_MD, GRAY_400
        );
    }
    
    // ============ 文本样式 ============
    
    /**
     * 标题样式
     */
    public static String title() {
        return String.format(
            "-fx-font-size: 16px; " +
            "-fx-font-weight: 700; " +
            "-fx-text-fill: %s;",
            GRAY_900
        );
    }
    
    /**
     * 副标题样式
     */
    public static String subtitle() {
        return String.format(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-text-fill: %s;",
            GRAY_700
        );
    }
    
    /**
     * 正文样式
     */
    public static String body() {
        return String.format(
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 400; " +
            "-fx-text-fill: %s;",
            GRAY_700
        );
    }
    
    /**
     * 说明文字样式
     */
    public static String caption() {
        return String.format(
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 400; " +
            "-fx-text-fill: %s;",
            GRAY_500
        );
    }

    /**
     * 正文样式（仅字体，不含颜色，供对话框内标签用，颜色由主题 CSS 控制）
     */
    public static String bodyFontOnly() {
        return "-fx-font-size: 13px; -fx-font-weight: 400;";
    }

    /**
     * 说明文字样式（仅字体，不含颜色）
     */
    public static String captionFontOnly() {
        return "-fx-font-size: 12px; -fx-font-weight: 400;";
    }
    
    // ============ 应用样式方法 ============
    
    /**
     * 应用按钮悬停效果
     */
    public static void applyButtonHover(Button button, String normalStyle, String hoverStyle) {
        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }
    
    /**
     * 应用主按钮悬停效果
     */
    public static void applyPrimaryButtonHover(Button button) {
        String normal = primaryButton();
        String hover = normal.replace(PRIMARY, PRIMARY_DARK);
        applyButtonHover(button, normal, hover);
    }
    
    /**
     * 应用成功按钮悬停效果
     */
    public static void applySuccessButtonHover(Button button) {
        String normal = successButton();
        String hover = normal.replace(SUCCESS, SUCCESS_DARK);
        applyButtonHover(button, normal, hover);
    }
    
    /**
     * 应用错误按钮悬停效果
     */
    public static void applyErrorButtonHover(Button button) {
        String normal = errorButton();
        String hover = normal.replace(ERROR, ERROR_DARK);
        applyButtonHover(button, normal, hover);
    }
    
    /**
     * 应用图标按钮悬停效果
     */
    public static void applyIconButtonHover(Button button) {
        String normal = iconButton();
        String hover = normal.replace("transparent", GRAY_100);
        applyButtonHover(button, normal, hover);
    }
}

