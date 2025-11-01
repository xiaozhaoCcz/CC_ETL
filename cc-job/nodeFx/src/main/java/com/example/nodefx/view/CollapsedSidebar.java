package com.example.nodefx.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/**
 * 折叠后的侧边栏 - 显示图标竖线
 */
public class CollapsedSidebar extends VBox {
    
    private Button treeViewButton;
    private Button miniMapButton;
    
    private Runnable onTreeViewRestore;
    private Runnable onMiniMapRestore;
    
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
        setMinWidth(50);
        setMaxWidth(50);
        setPrefWidth(50);
        setSpacing(10);
        setPadding(new Insets(10, 5, 10, 5));
        setAlignment(Pos.TOP_CENTER);
        
        // 任务组图标按钮
        treeViewButton = createIconButton("📁", "显示任务组");
        treeViewButton.setOnAction(e -> {
            if (onTreeViewRestore != null) {
                onTreeViewRestore.run();
            }
        });
        
        // 小地图图标按钮
        miniMapButton = createIconButton("🗺️", "显示小地图");
        miniMapButton.setOnAction(e -> {
            if (onMiniMapRestore != null) {
                onMiniMapRestore.run();
            }
        });
        
        getChildren().addAll(treeViewButton, miniMapButton);
    }
    
    private Button createIconButton(String icon, String tooltipText) {
        Button button = new Button(icon);
        button.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-font-size: 18; " +
            "-fx-padding: 8; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 5; " +
            "-fx-cursor: hand;"
        );
        button.setPrefSize(40, 40);
        button.setMaxSize(40, 40);
        button.setMinSize(40, 40);
        
        Tooltip tooltip = new Tooltip(tooltipText);
        button.setTooltip(tooltip);
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: #EEF2FF; " +
            "-fx-font-size: 18; " +
            "-fx-padding: 8; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #8B5CF6; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 5; " +
            "-fx-cursor: hand;"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-font-size: 18; " +
            "-fx-padding: 8; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 5; " +
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
        treeViewButton.setVisible(show);
        treeViewButton.setManaged(show);
    }
    
    public void showMiniMapButton(boolean show) {
        miniMapButton.setVisible(show);
        miniMapButton.setManaged(show);
    }
}

