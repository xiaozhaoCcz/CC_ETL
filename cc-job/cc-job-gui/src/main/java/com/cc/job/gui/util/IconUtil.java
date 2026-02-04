package com.cc.job.gui.util;

import atlantafx.base.theme.Styles;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * 图标工具类 - 使用 Ikonli Dashicons 图标并与 Atlantafx 主题配色保持一致。
 */
public final class IconUtil {

    private static final String SECONDARY_COLOR = "#64748B";
    private static final String ACCENT_COLOR = "#6366F1";
    private static final String SUCCESS_COLOR = "#10B981";
    private static final String WARNING_COLOR = "#F59E0B";
    private static final String DANGER_COLOR = "#EF4444";
    private static final String MUTED_COLOR = "#6B7280";
    private static final String WHITE_COLOR = "#FFFFFF";

    private IconUtil() {
        // utility class
    }

    /**
     * 创建 Feather 线性图标。
     */
    private static FontIcon createFeatherIcon(Feather feather, String color, int size) {
        FontIcon icon = new FontIcon(feather);
        icon.getStyleClass().addAll(Styles.FONT_ICON, Styles.TEXT_MUTED);
        icon.setIconSize(size);
        if (color != null) {
            icon.setIconColor(Color.web(color));
        }
        return icon;
    }

    // ============ 工具栏图标 ============

    public static FontIcon fileIcon() {
        return createFeatherIcon(Feather.FILE_TEXT, SECONDARY_COLOR, 16);
    }

    public static FontIcon folderIcon() {
        return createFeatherIcon(Feather.FOLDER, SECONDARY_COLOR, 16);
    }

    public static FontIcon saveIcon() {
        return createFeatherIcon(Feather.DOWNLOAD, ACCENT_COLOR, 16);
    }

    public static FontIcon undoIcon() {
        return createFeatherIcon(Feather.CORNER_UP_LEFT, SECONDARY_COLOR, 16);
    }

    public static FontIcon redoIcon() {
        return createFeatherIcon(Feather.CORNER_UP_RIGHT, SECONDARY_COLOR, 16);
    }

    public static FontIcon zoomInIcon() {
        return createFeatherIcon(Feather.ZOOM_IN, SECONDARY_COLOR, 16);
    }

    public static FontIcon zoomOutIcon() {
        return createFeatherIcon(Feather.ZOOM_OUT, SECONDARY_COLOR, 16);
    }

    public static FontIcon expandIcon() {
        return createFeatherIcon(Feather.MAXIMIZE_2, SECONDARY_COLOR, 16);
    }

    public static FontIcon historyIcon() {
        return createFeatherIcon(Feather.CLOCK, SECONDARY_COLOR, 16);
    }

    public static FontIcon selectIcon() {
        return createFeatherIcon(Feather.CROP, SECONDARY_COLOR, 16);
    }

    public static FontIcon layoutHorizontalIcon() {
        return createFeatherIcon(Feather.ALIGN_LEFT, SECONDARY_COLOR, 16);
    }

    public static FontIcon layoutVerticalIcon() {
        return createFeatherIcon(Feather.ALIGN_CENTER, SECONDARY_COLOR, 16);
    }
    
    public static FontIcon alignLeftIcon() {
        return createFeatherIcon(Feather.ALIGN_LEFT, SECONDARY_COLOR, 16);
    }
    
    public static FontIcon alignRightIcon() {
        return createFeatherIcon(Feather.ALIGN_RIGHT, SECONDARY_COLOR, 16);
    }
    
    public static FontIcon alignCenterIcon() {
        return createFeatherIcon(Feather.ALIGN_CENTER, SECONDARY_COLOR, 16);
    }
    
    public static FontIcon gridIcon() {
        return createFeatherIcon(Feather.GRID, SECONDARY_COLOR, 16);
    }

    public static FontIcon playIcon() {
        return createFeatherIcon(Feather.PLAY, WHITE_COLOR, 18);
    }

    public static FontIcon stopIcon() {
        return createFeatherIcon(Feather.PAUSE, WHITE_COLOR, 18);
    }

    public static FontIcon plusIcon() {
        return createFeatherIcon(Feather.PLUS_CIRCLE, SECONDARY_COLOR, 18);
    }

    // ============ 面板图标 ============

    public static FontIcon searchIcon() {
        return createFeatherIcon(Feather.SEARCH, MUTED_COLOR, 14);
    }

    public static FontIcon closeIcon() {
        return createFeatherIcon(Feather.X, MUTED_COLOR, 16);
    }

    public static FontIcon windowIcon() {
        return createFeatherIcon(Feather.EXTERNAL_LINK, MUTED_COLOR, 16);
    }

    public static FontIcon trashIcon() {
        return createFeatherIcon(Feather.TRASH_2, SECONDARY_COLOR, 16);
    }

    public static FontIcon exportIcon() {
        return createFeatherIcon(Feather.UPLOAD, SECONDARY_COLOR, 16);
    }

    public static FontIcon refreshIcon() {
        return createFeatherIcon(Feather.REFRESH_CW, SECONDARY_COLOR, 16);
    }

    // ============ 树节点图标 ============

    public static FontIcon partitionIcon() {
        return createFeatherIcon(Feather.LAYERS, ACCENT_COLOR, 16);
    }

    public static FontIcon taskGroupIcon() {
        return createFeatherIcon(Feather.FOLDER, ACCENT_COLOR, 16);
    }

    public static FontIcon taskContainerIcon() {
        return createFeatherIcon(Feather.PACKAGE, SUCCESS_COLOR, 16);
    }

    public static FontIcon relationContainerIcon() {
        return createFeatherIcon(Feather.SHARE_2, WARNING_COLOR, 16);
    }

    public static FontIcon taskNodeIcon() {
        return createFeatherIcon(Feather.CPU, "#0EA5E9", 16);
    }

    public static FontIcon relationEdgeIcon() {
        return createFeatherIcon(Feather.ARROW_RIGHT, "#EC4899", 16);
    }

    // ============ 日志级别图标 ============

    public static FontIcon infoIcon() {
        return createFeatherIcon(Feather.INFO, ACCENT_COLOR, 16);
    }

    public static FontIcon warnIcon() {
        return createFeatherIcon(Feather.ALERT_TRIANGLE, WARNING_COLOR, 16);
    }

    public static FontIcon errorIcon() {
        return createFeatherIcon(Feather.ALERT_OCTAGON, DANGER_COLOR, 16);
    }

    public static FontIcon successIcon() {
        return createFeatherIcon(Feather.CHECK_CIRCLE, SUCCESS_COLOR, 16);
    }

    public static FontIcon debugIcon() {
        return createFeatherIcon(Feather.TERMINAL, "#7C3AED", 16);
    }

    // ============ 状态图标 ============

    public static FontIcon runningIcon() {
        return createFeatherIcon(Feather.ACTIVITY, SUCCESS_COLOR, 16);
    }

    public static FontIcon chartIcon() {
        return createFeatherIcon(Feather.PIE_CHART, ACCENT_COLOR, 16);
    }

    public static FontIcon mapIcon() {
        return createFeatherIcon(Feather.MAP, ACCENT_COLOR, 16);
    }
    
    // ============ 表单配置图标 ============
    
    public static FontIcon settingsIcon() {
        return createFeatherIcon(Feather.SETTINGS, MUTED_COLOR, 18);
    }
    
    public static FontIcon clockIcon() {
        return createFeatherIcon(Feather.CLOCK, MUTED_COLOR, 18);
    }
    
    public static FontIcon wrenchIcon() {
        return createFeatherIcon(Feather.TOOL, MUTED_COLOR, 18);
    }

    // ============ 辅助方法 ============

    public static FontIcon getIconByType(Integer type) {
        if (type == null) {
            return createFeatherIcon(Feather.SQUARE, MUTED_COLOR, 16);
        }
        return switch (type) {
            case 0 -> partitionIcon();
            case 1 -> taskGroupIcon();
            case 2 -> taskContainerIcon();
            case 3 -> relationContainerIcon();
            case 4 -> taskNodeIcon();
            case 5 -> relationEdgeIcon();
            default -> createFeatherIcon(Feather.SQUARE, MUTED_COLOR, 16);
        };
    }
}

