package com.cc.job.gui.util;

/**
 * 参数输入样式工具类
 * 
 * <p>定义参数输入框的样式常量，包括标签样式和提示样式
 * 
 * @author cc-job-team
 * @since 2026-01-06
 */
public class ParameterInputStyleUtil {
    
    /**
     * 参数标签样式（用于 #{jobdesc}.{attr} 表达式）
     */
    public static final String PARAMETER_TAG_STYLE = 
            "-fx-background-color: #E0F2FE; " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-border-color: #0EA5E9; " +
            "-fx-border-radius: 4; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #0369A1; " +
            "-fx-font-weight: bold;";
    
    /**
     * 自动补全弹出窗口样式
     */
    public static final String AUTOCOMPLETE_POPUP_STYLE = 
            "-fx-background-color: white; " +
            "-fx-background-radius: 4; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2); " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-radius: 4; " +
            "-fx-border-width: 1;";
    
    /**
     * 自动补全列表项样式（普通状态）
     */
    public static final String AUTOCOMPLETE_ITEM_STYLE = 
            "-fx-background-color: transparent; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-cursor: hand;";
    
    /**
     * 自动补全列表项样式（悬停状态）
     */
    public static final String AUTOCOMPLETE_ITEM_HOVER_STYLE = 
            "-fx-background-color: #F3F4F6; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-cursor: hand;";
    
    /**
     * 自动补全列表项样式（选中状态）
     */
    public static final String AUTOCOMPLETE_ITEM_SELECTED_STYLE = 
            "-fx-background-color: #2563EB; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-cursor: hand; " +
            "-fx-text-fill: white;";
    
    /**
     * 输入框提示文本样式
     */
    public static final String INPUT_HINT_STYLE = 
            "-fx-text-fill: #9CA3AF; " +
            "-fx-font-size: 12; " +
            "-fx-font-style: italic;";
    
    private ParameterInputStyleUtil() {
        // 工具类，不允许实例化
    }
}

