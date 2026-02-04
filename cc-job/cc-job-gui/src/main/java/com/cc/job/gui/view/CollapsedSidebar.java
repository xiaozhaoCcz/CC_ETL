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
        /* Preserve FeatherIcons font: setStyle() replaces entire inline style; without font, icon inherits .root and shows garbled (e.g. 韦). */
        icon.setStyle("-fx-font-family: 'FeatherIcons';-fx-font-size: 16px;-fx-icon-color: #374151;");

        Tooltip tooltip = new Tooltip(tooltipText);
        button.setTooltip(tooltip);
        
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: #E5E7EB; " +
                "-fx-padding: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-border-color: #D1D5DB; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-cursor: hand;"
            );
            icon.setStyle("-fx-font-family: 'FeatherIcons';-fx-font-size: 16px;-fx-icon-color: #111827;");
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-padding: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-border-color: #E5E7EB; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-cursor: hand;"
            );
            icon.setStyle("-fx-font-family: 'FeatherIcons';-fx-font-size: 16px;-fx-icon-color: #374151;");
        });
        
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
    
    public void setOnLogPanelRestore(Runnable callback) {
        this.onLogPanelRestore = callback;
    }
    
    public void showLogPanelButton(boolean show) {
        logPanelButton.setVisible(show);
        logPanelButton.setManaged(show);
    }
}

