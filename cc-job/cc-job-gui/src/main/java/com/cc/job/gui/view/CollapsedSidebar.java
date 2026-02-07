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
import com.cc.job.gui.util.ThemeManager;

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
        getStyleClass().add("collapsed-sidebar");
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
        
        // 主题切换时重新应用按钮样式，使图标颜色跟随主题
        ThemeManager.getInstance().addOnThemeChanged(() -> {
            refreshButtonStyle(treeViewButton, false);
            refreshButtonStyle(miniMapButton, false);
            refreshButtonStyle(logPanelButton, false);
        });
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
        button.getStyleClass().add("sidebar-icon-button");
        refreshButtonStyle(button, false);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        button.setMinSize(36, 36);

        Tooltip tooltip = new Tooltip(tooltipText);
        button.setTooltip(tooltip);
        
        button.setOnMouseEntered(e -> refreshButtonStyle(button, true));
        button.setOnMouseExited(e -> refreshButtonStyle(button, false));
        
        return button;
    }
    
    /**
     * 根据当前主题和悬停状态刷新按钮及图标样式（主题切换或悬停时调用）。
     */
    private void refreshButtonStyle(Button button, boolean hover) {
        if (button.getGraphic() instanceof StackPane wrapper && !wrapper.getChildren().isEmpty() && wrapper.getChildren().get(0) instanceof FontIcon icon) {
            boolean dark = "dark".equals(ThemeManager.getInstance().getTheme());
            applyIconButtonStyle(button, icon, dark, hover);
        }
    }
    
    private void applyIconButtonStyle(Button button, FontIcon icon, boolean dark, boolean hover) {
        if (dark) {
            button.setStyle(
                "-fx-background-color: " + (hover ? "#505050" : "#3C3C3C") + "; " +
                "-fx-padding: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-border-color: #505050; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-cursor: hand;"
            );
            icon.setStyle("-fx-font-family: 'FeatherIcons';-fx-font-size: 16px;-fx-icon-color: " + (hover ? "#D4D4D4" : "#9D9D9D") + ";");
        } else {
            button.setStyle(
                "-fx-background-color: " + (hover ? "#E5E7EB" : "#FFFFFF") + "; " +
                "-fx-padding: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-border-color: " + (hover ? "#D1D5DB" : "#E5E7EB") + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-cursor: hand;"
            );
            icon.setStyle("-fx-font-family: 'FeatherIcons';-fx-font-size: 16px;-fx-icon-color: " + (hover ? "#111827" : "#374151") + ";");
        }
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
    
    public void setOnLogPanelRestore(Runnable callback) {
        this.onLogPanelRestore = callback;
    }
    
    public void showLogPanelButton(boolean show) {
        logPanelButton.setVisible(show);
        logPanelButton.setManaged(show);
    }
}

