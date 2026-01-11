package com.cc.job.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import com.cc.job.gui.util.IconUtil;

/**
 * 折叠后的侧边栏 - 显示图标竖线
 */
public class CollapsedSidebar extends VBox {
    
    private Button treeViewButton;
    private Button miniMapButton;
    private Button logPanelButton;
    
    private Runnable onTreeViewRestore;
    private Runnable onMiniMapRestore;
    private Runnable onLogPanelRestore;
    
    public CollapsedSidebar() {
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle(
            "-fx-background-color: #F3F4F6; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 0 1 0 0;"
        );
        // 设置固定宽度 - 使用min/max来确保宽度不变
        setMinWidth(40);
        setMaxWidth(40);
        setPrefWidth(40);
        setSpacing(12);
        setPadding(new Insets(12, 6, 12, 6));
        setAlignment(Pos.TOP_CENTER);
        
        // 任务组图标按钮
        treeViewButton = createIconButton(IconUtil.folderIcon(), "显示任务组");
        treeViewButton.setOnAction(e -> {
            if (onTreeViewRestore != null) {
                onTreeViewRestore.run();
            }
        });
        
        // 小地图图标按钮
        miniMapButton = createIconButton(IconUtil.mapIcon(), "显示小地图");
        miniMapButton.setOnAction(e -> {
            if (onMiniMapRestore != null) {
                onMiniMapRestore.run();
            }
        });
        
        // 日志监控图标按钮
        logPanelButton = createIconButton(IconUtil.infoIcon(), "显示监控");
        logPanelButton.setOnAction(e -> {
            if (onLogPanelRestore != null) {
                onLogPanelRestore.run();
            }
        });
        
        getChildren().addAll(treeViewButton, miniMapButton, logPanelButton);
        
        // 初始状态：所有面板都可见，所以所有恢复按钮都隐藏
        showTreeViewButton(false);
        showMiniMapButton(false);
        showLogPanelButton(false);
    }
    
    private Button createIconButton(FontIcon icon, String tooltipText) {
        Button button = new Button();
        icon.setIconSize(16);
        StackPane iconWrapper = new StackPane(icon);
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.setPrefSize(18, 18);
        iconWrapper.setMinSize(18, 18);
        iconWrapper.setMaxSize(18, 18);
        StackPane.setAlignment(icon, Pos.CENTER);
        button.setGraphic(iconWrapper);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAlignment(Pos.CENTER);
        button.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-padding: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-cursor: hand;"
        );
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        button.setMinSize(36, 36);
        
        Tooltip tooltip = new Tooltip(tooltipText);
        button.setTooltip(tooltip);
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: #EEF2FF; " +
            "-fx-padding: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-border-color: #8B5CF6; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-padding: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        return button;
    }
    
    public void setOnTreeViewRestore(Runnable callback) {
        this.onTreeViewRestore = callback;
    }
    
    public void setOnMiniMapRestore(Runnable callback) {
        this.onMiniMapRestore = callback;
    }
    
    public void showTreeViewButton(boolean show) {
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("/Users/xiaozhao/Desktop/xz/IdeaProject/Cc_ETL/.cursor/debug.log", true);
            fw.write(String.format("{\"timestamp\":%d,\"location\":\"CollapsedSidebar.java:135\",\"message\":\"showTreeViewButton called\",\"data\":{\"show\":%s},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"K\"}\n", 
                System.currentTimeMillis(), show));
            fw.close();
        } catch (java.io.IOException e) {}
        // #endregion
        treeViewButton.setVisible(show);
        treeViewButton.setManaged(show);
    }
    
    public void showMiniMapButton(boolean show) {
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("/Users/xiaozhao/Desktop/xz/IdeaProject/Cc_ETL/.cursor/debug.log", true);
            fw.write(String.format("{\"timestamp\":%d,\"location\":\"CollapsedSidebar.java:140\",\"message\":\"showMiniMapButton called\",\"data\":{\"show\":%s},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"K\"}\n", 
                System.currentTimeMillis(), show));
            fw.close();
        } catch (java.io.IOException e) {}
        // #endregion
        miniMapButton.setVisible(show);
        miniMapButton.setManaged(show);
    }
    
    public void setOnLogPanelRestore(Runnable callback) {
        this.onLogPanelRestore = callback;
    }
    
    public void showLogPanelButton(boolean show) {
        logPanelButton.setVisible(show);
        logPanelButton.setManaged(show);
    }
}

