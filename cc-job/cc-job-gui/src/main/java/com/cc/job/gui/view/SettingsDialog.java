package com.cc.job.gui.view;

import com.cc.job.gui.util.ConfigManager;
import com.cc.job.gui.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 系统设置对话框
 */
public class SettingsDialog extends Dialog<Void> {
    
    private final Stage ownerStage;
    private ConfigManager configManager;
    
    // 常规设置
    private TextField autoSaveIntervalField;
    private CheckBox restoreSessionCheckBox;
    private TextField dataxPathField;
    
    // 网络设置
    private TextField apiUrlField;
    private TextField connectionTimeoutField;
    
    // 界面设置
    private ComboBox<String> themeCombo;
    private TextField defaultZoomField;
    private CheckBox animationCheckBox;
    
    public SettingsDialog(Stage owner) {
        this.ownerStage = owner;
        this.configManager = ConfigManager.getInstance();
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("系统设置");
        setHeaderText("配置系统参数");
        
        // 创建内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        String cssUrl = ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            getDialogPane().getStylesheets().add(cssUrl);
        }
        
        // 添加按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // 加载当前设置
        loadSettings();
        
        // 保存按钮事件
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.setOnAction(e -> {
            if (saveSettings()) {
                close();
            }
        });
        
        // 设置对话框大小
        getDialogPane().setPrefWidth(600);
        getDialogPane().setPrefHeight(500);
    }
    
    private VBox createContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // 常规设置
        TitledPane generalPane = createGeneralSettingsPane();
        
        // 网络设置
        TitledPane networkPane = createNetworkSettingsPane();
        
        // 界面设置
        TitledPane uiPane = createUISettingsPane();
        
        // 使用Accordion来组织设置面板
        Accordion accordion = new Accordion();
        accordion.getPanes().addAll(generalPane, networkPane, uiPane);
        accordion.setExpandedPane(generalPane);
        
        content.getChildren().add(accordion);
        
        return content;
    }
    
    private TitledPane createGeneralSettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // 自动保存间隔
        grid.add(new Label("自动保存间隔(秒):"), 0, 0);
        autoSaveIntervalField = new TextField();
        autoSaveIntervalField.setPromptText("例如: 30");
        grid.add(autoSaveIntervalField, 1, 0);
        
        // 启动时恢复上次会话
        grid.add(new Label("启动时恢复上次会话:"), 0, 1);
        restoreSessionCheckBox = new CheckBox();
        grid.add(restoreSessionCheckBox, 1, 1);
        
        // DataX 路径
        grid.add(new Label("DataX 路径(可选):"), 0, 2);
        dataxPathField = new TextField();
        dataxPathField.setPromptText("DataX 安装目录或 datax.py 路径，如 /opt/datax 或 /opt/datax/bin/datax.py");
        grid.add(dataxPathField, 1, 2);
        
        TitledPane pane = new TitledPane("常规设置", grid);
        return pane;
    }
    
    private TitledPane createNetworkSettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // API服务器地址
        grid.add(new Label("API服务器地址:"), 0, 0);
        apiUrlField = new TextField();
        apiUrlField.setPromptText("例如: http://localhost:8989/xxl-job-admin");
        grid.add(apiUrlField, 1, 0);
        
        // 连接超时
        grid.add(new Label("连接超时(秒):"), 0, 1);
        connectionTimeoutField = new TextField();
        connectionTimeoutField.setPromptText("例如: 30");
        grid.add(connectionTimeoutField, 1, 1);
        
        TitledPane pane = new TitledPane("网络设置", grid);
        return pane;
    }
    
    private TitledPane createUISettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // 主题
        grid.add(new Label("主题:"), 0, 0);
        themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("浅色", "深色", "自动");
        themeCombo.setValue("浅色");
        grid.add(themeCombo, 1, 0);
        
        // 默认缩放
        grid.add(new Label("默认缩放(%):"), 0, 1);
        defaultZoomField = new TextField();
        defaultZoomField.setPromptText("例如: 100");
        grid.add(defaultZoomField, 1, 1);
        
        // 动画效果
        grid.add(new Label("启用动画效果:"), 0, 2);
        animationCheckBox = new CheckBox();
        animationCheckBox.setSelected(true);
        grid.add(animationCheckBox, 1, 2);
        
        TitledPane pane = new TitledPane("界面设置", grid);
        return pane;
    }
    
    private void loadSettings() {
        // 加载常规设置
        String autoSaveInterval = configManager.getProperty("auto.save.interval", "30");
        autoSaveIntervalField.setText(autoSaveInterval);
        
        String restoreSession = configManager.getProperty("restore.session", "true");
        restoreSessionCheckBox.setSelected("true".equals(restoreSession));
        
        String dataxPath = configManager.getProperty("datax.path", "");
        dataxPathField.setText(dataxPath);
        
        // 加载网络设置
        String apiUrl = configManager.getBaseUrl();
        apiUrlField.setText(apiUrl);
        
        String timeout = configManager.getProperty("connection.timeout", "30");
        connectionTimeoutField.setText(timeout);
        
        // 加载界面设置（配置存 light/dark/auto，显示 浅色/深色/自动）
        String themeKey = ThemeManager.getInstance().getThemeConfigKey();
        themeCombo.setValue(themeKeyToDisplay(themeKey));
        
        String defaultZoom = configManager.getProperty("default.zoom", "100");
        defaultZoomField.setText(defaultZoom);
        
        String animation = configManager.getProperty("animation.enabled", "true");
        animationCheckBox.setSelected("true".equals(animation));
    }
    
    private boolean saveSettings() {
        try {
            // 保存常规设置
            configManager.setProperty("auto.save.interval", autoSaveIntervalField.getText());
            configManager.setProperty("restore.session", String.valueOf(restoreSessionCheckBox.isSelected()));
            configManager.setProperty("datax.path", dataxPathField.getText().trim());
            
            // 保存网络设置
            String apiUrl = apiUrlField.getText().trim();
            if (!apiUrl.isEmpty()) {
                configManager.setBaseUrl(apiUrl);
            }
            configManager.setProperty("connection.timeout", connectionTimeoutField.getText());
            
            // 保存界面设置（显示 浅色/深色/自动 转为 light/dark/auto）
            String themeKey = displayToThemeKey(themeCombo.getValue());
            ThemeManager.getInstance().setTheme(themeKey);
            configManager.setProperty("theme", themeKey);
            configManager.setProperty("default.zoom", defaultZoomField.getText());
            configManager.setProperty("animation.enabled", String.valueOf(animationCheckBox.isSelected()));
            // 主窗口样式表与小地图刷新由 ThemeManager 的主题变更监听器统一处理
            return true;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("保存失败");
            alert.setHeaderText(null);
            alert.setContentText("保存设置时发生错误: " + e.getMessage());
            alert.showAndWait();
            return false;
        }
    }

    private static String themeKeyToDisplay(String key) {
        if (key == null) return "浅色";
        switch (key) {
            case "dark": return "深色";
            case "auto": return "自动";
            default: return "浅色";
        }
    }

    private static String displayToThemeKey(String display) {
        if (display == null) return "light";
        switch (display) {
            case "深色": return "dark";
            case "自动": return "auto";
            default: return "light";
        }
    }
}
