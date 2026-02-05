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
            "-fx-effect: null; " +
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
    
    /**
     * 输入框基础样式
     */
    public static final String INPUT_FIELD_STYLE = 
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-font-size: 14; " +
            "-fx-text-fill: #111827;";
    
    /**
     * 输入框焦点状态样式
     */
    public static final String INPUT_FIELD_FOCUSED_STYLE = 
            "-fx-border-color: #3B82F6; " +
            "-fx-border-width: 2;";
    
    /**
     * 选中文本背景样式（浅蓝色）
     */
    public static final String SELECTION_BACKGROUND_STYLE = 
            "-fx-selection-bar: #BFDBFE; " +
            "-fx-selection-bar-non-focused: #E0E7FF;";
    
    /** 深色主题：输入框基础样式 */
    public static final String INPUT_FIELD_STYLE_DARK =
            "-fx-background-color: #3C3C3C; " +
            "-fx-border-color: #505050; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-font-size: 14; " +
            "-fx-text-fill: #D4D4D4;";
    
    /** 深色主题：输入框焦点状态样式 */
    public static final String INPUT_FIELD_FOCUSED_STYLE_DARK =
            "-fx-border-color: #007ACC; " +
            "-fx-border-width: 2;";
    
    /** 深色主题：选中文本背景样式 */
    public static final String SELECTION_BACKGROUND_STYLE_DARK =
            "-fx-selection-bar: #094771; " +
            "-fx-selection-bar-non-focused: #3C3C3C;";
    
    /** 深色主题：自动补全弹出窗口样式 */
    public static final String AUTOCOMPLETE_POPUP_STYLE_DARK =
            "-fx-background-color: #2D2D30; " +
            "-fx-background-radius: 4; " +
            "-fx-effect: null; " +
            "-fx-border-color: #505050; " +
            "-fx-border-radius: 4; " +
            "-fx-border-width: 1;";

    /** 深色主题：自动补全列表项样式（普通状态） */
    public static final String AUTOCOMPLETE_ITEM_STYLE_DARK =
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #D4D4D4; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-cursor: hand;";

    /** 深色主题：自动补全列表项样式（悬停状态） */
    public static final String AUTOCOMPLETE_ITEM_HOVER_STYLE_DARK =
            "-fx-background-color: #3C3C3C; " +
            "-fx-text-fill: #D4D4D4; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-cursor: hand;";

    /** 深色主题：自动补全列表项样式（选中状态） */
    public static final String AUTOCOMPLETE_ITEM_SELECTED_STYLE_DARK =
            "-fx-background-color: #094771; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-cursor: hand; " +
            "-fx-text-fill: white;";
    
    /**
     * 节点类型标签样式 - SQL
     */
    public static final String NODE_TYPE_SQL_STYLE = 
            "-fx-background-color: #DBEAFE; " +
            "-fx-text-fill: #1E40AF; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";
    
    /**
     * 节点类型标签样式 - API
     */
    public static final String NODE_TYPE_API_STYLE = 
            "-fx-background-color: #D1FAE5; " +
            "-fx-text-fill: #065F46; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";
    
    /**
     * 节点类型标签样式 - Bean
     */
    public static final String NODE_TYPE_BEAN_STYLE = 
            "-fx-background-color: #FED7AA; " +
            "-fx-text-fill: #9A3412; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";
    
    /**
     * 节点类型标签样式 - 其他类型
     */
    public static final String NODE_TYPE_OTHER_STYLE = 
            "-fx-background-color: #F3F4F6; " +
            "-fx-text-fill: #4B5563; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";

    /** 深色主题：节点类型标签 - SQL */
    public static final String NODE_TYPE_SQL_STYLE_DARK =
            "-fx-background-color: #264F78; " +
            "-fx-text-fill: #9CDCFE; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";

    /** 深色主题：节点类型标签 - API */
    public static final String NODE_TYPE_API_STYLE_DARK =
            "-fx-background-color: #1e3a2f; " +
            "-fx-text-fill: #4EC9B0; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";

    /** 深色主题：节点类型标签 - Bean */
    public static final String NODE_TYPE_BEAN_STYLE_DARK =
            "-fx-background-color: #3d2a1e; " +
            "-fx-text-fill: #CE9178; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";

    /** 深色主题：节点类型标签 - 其他 */
    public static final String NODE_TYPE_OTHER_STYLE_DARK =
            "-fx-background-color: #3C3C3C; " +
            "-fx-text-fill: #9D9D9D; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold;";
    
    private ParameterInputStyleUtil() {
        // 工具类，不允许实例化
    }
}

