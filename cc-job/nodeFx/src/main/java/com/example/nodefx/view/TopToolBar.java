package com.example.nodefx.view;

import com.example.nodefx.model.RunningJobGroup;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * 顶部工具栏组件
 */
public class TopToolBar extends VBox {
    
    // 回调接口
    public interface ToolBarCallback {
        void onNew();
        void onOpen();
        void onSave();
        void onUndo();
        void onRedo();
        void onZoomIn();
        void onZoomOut();
        void onZoomFit();
        void onRun();
        void onStop(Long jobId);
        void onClear();
        void onSettings();
    }
    
    private ToolBarCallback callback;
    private Label zoomLabel;
    private double currentZoom = 1.0;
    
    // 运行按钮和下拉菜单
    private Button runButton;
    private Button retryButton;
    private MenuButton runningTasksMenu;
    private HBox runGroup;
    
    // 当前任务组ID（用于判断是否正在运行）
    private Long currentTaskGroupId;
    
    // 运行中的任务组列表
    private Map<Long, RunningJobGroup> runningJobs = new java.util.HashMap<>();
    
    public TopToolBar() {
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");
        setPadding(new Insets(0));
        
        // 主菜单栏
        HBox menuBar = createMenuBar();
        
        // 工具栏
        HBox toolBar = createToolBar();
        
        getChildren().addAll(menuBar, toolBar);
    }
    
    private HBox createMenuBar() {
        HBox menuBar = new HBox(0);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setPadding(new Insets(8, 10, 8, 10));
        menuBar.setStyle("-fx-background-color: #F9FAFB;");
        
        // 文件菜单
        Label fileMenu = createMenuLabel("📄 文件");
        Label taskMenu = createMenuLabel("📋 任务组");
        
        menuBar.getChildren().addAll(fileMenu, taskMenu);
        
        return menuBar;
    }
    
    private Label createMenuLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 5 15 5 15; -fx-font-size: 13; -fx-cursor: hand;");
        label.setOnMouseEntered(e -> {
            if (!label.getStyle().contains("border-width")) {
                label.setStyle("-fx-background-color: #F3F4F6; -fx-padding: 5 15 5 15; -fx-font-size: 13; -fx-cursor: hand;");
            }
        });
        label.setOnMouseExited(e -> {
            if (!label.getStyle().contains("border-width")) {
                label.setStyle("-fx-padding: 5 15 5 15; -fx-font-size: 13; -fx-cursor: hand;");
            }
        });
        return label;
    }
    
    private HBox createToolBar() {
        HBox toolBar = new HBox(5);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(8, 10, 8, 10));
        
        // 文件操作组
        HBox fileGroup = createToolGroup(
            createToolButton("📄 新建", "创建新的流程图", () -> safeCall(ToolBarCallback::onNew)),
            createToolButton("📂 打开", "打开已有流程图", () -> safeCall(ToolBarCallback::onOpen)),
            createToolButton("💾 保存", "保存当前流程图", () -> safeCall(ToolBarCallback::onSave))
        );
        
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sep1.setPrefHeight(25);
        
        // 编辑操作组
        HBox editGroup = createToolGroup(
            createToolButton("↶ 撤销", "撤销上一步操作", () -> safeCall(ToolBarCallback::onUndo)),
            createToolButton("↷ 重做", "重做上一步操作", () -> safeCall(ToolBarCallback::onRedo))
        );
        
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sep2.setPrefHeight(25);
        
        // 视图操作组
        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12; -fx-padding: 0 5 0 5;");
        
        HBox viewGroup = createToolGroup(
            createToolButton("🔍➕ 放大", "放大画布", () -> safeCall(ToolBarCallback::onZoomIn)),
            createToolButton("🔍➖ 缩小", "缩小画布", () -> safeCall(ToolBarCallback::onZoomOut)),
            zoomLabel,
            createToolButton("⬜ 适应", "适应窗口大小", () -> safeCall(ToolBarCallback::onZoomFit))
        );
        
        // 右侧空白区域
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Separator sep3 = new Separator();
        sep3.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sep3.setPrefHeight(25);
        
        // 运行操作组 - 只显示开始/停止和运行中按钮
        runGroup = createToolGroup();
        createRetryButton();
        createRunningTasksMenu();
        updateRunGroupButtons();
        
        toolBar.getChildren().addAll(
            fileGroup, sep1,
            editGroup, sep2,
            viewGroup,
            spacer,
            sep3, runGroup
        );
        
        return toolBar;
    }
    
    private HBox createToolGroup(javafx.scene.Node... buttons) {
        HBox group = new HBox(5);
        group.setAlignment(Pos.CENTER_LEFT);
        group.getChildren().addAll(buttons);
        return group;
    }
    
    private Button createToolButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #F3F4F6; " +
            "-fx-text-fill: #1F2937; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        if (tooltip != null) {
            Tooltip tip = new Tooltip(tooltip);
            btn.setTooltip(tip);
        }
        
        btn.setOnAction(e -> action.run());
        
        return btn;
    }
    
    private Button createActionButton(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> {
            String darkerColor = color.equals("#10B981") ? "#059669" : color;
            btn.setStyle(
                "-fx-background-color: " + darkerColor + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6 16 6 16; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand;"
            );
        });
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnAction(e -> action.run());
        
        return btn;
    }
    
    private void safeCall(java.util.function.Consumer<ToolBarCallback> method) {
        if (callback != null) {
            method.accept(callback);
        }
    }
    
    public void setCallback(ToolBarCallback callback) {
        this.callback = callback;
    }
    
    public void updateZoomLevel(double zoom) {
        this.currentZoom = zoom;
        zoomLabel.setText(String.format("%.0f%%", zoom * 100));
    }
    
    public double getCurrentZoom() {
        return currentZoom;
    }
    
    /**
     * 创建开始/停止按钮
     */
    private void createRetryButton() {
        runButton = new Button("▶️ 开始");
        runButton.setStyle(
            "-fx-background-color: #10B981; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 4 12 4 12; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        
        runButton.setOnMouseEntered(e -> {
            if (runButton.getText().contains("开始")) {
                runButton.setStyle(
                    "-fx-background-color: #059669; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
            } else {
                runButton.setStyle(
                    "-fx-background-color: #DC2626; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        runButton.setOnMouseExited(e -> {
            if (runButton.getText().contains("开始")) {
                runButton.setStyle(
                    "-fx-background-color: #10B981; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
            } else {
                runButton.setStyle(
                    "-fx-background-color: #EF4444; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        // 按钮点击事件会在updateButtonState中根据状态动态设置
    }
    
    /**
     * 创建运行中任务下拉菜单
     */
    private void createRunningTasksMenu() {
        runningTasksMenu = new MenuButton("运行中任务");
        runningTasksMenu.setStyle(
            "-fx-background-color: #EF4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 4 12 4 12; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        runningTasksMenu.setVisible(false);
    }
    
    /**
     * 更新运行组按钮状态
     */
    private void updateRunGroupButtons() {
        runGroup.getChildren().clear();
        runGroup.getChildren().add(runButton);
        runGroup.getChildren().add(runningTasksMenu);
    }
    
    /**
     * 设置当前任务组ID
     */
    public void setCurrentTaskGroupId(Long taskGroupId) {
        this.currentTaskGroupId = taskGroupId;
        updateButtonState();
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonState() {
        Platform.runLater(() -> {
            // 判断当前任务组是否正在运行
            boolean isCurrentRunning = currentTaskGroupId != null && 
                runningJobs.containsKey(currentTaskGroupId) && 
                runningJobs.get(currentTaskGroupId).isRunning();
            
            // 更新运行/停止按钮
            if (isCurrentRunning) {
                runButton.setText("🛑 停止");
                runButton.setStyle(
                    "-fx-background-color: #EF4444; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
                // 点击停止按钮时，停止当前任务组
                runButton.setOnAction(e -> {
                    if (callback != null && currentTaskGroupId != null) {
                        callback.onStop(currentTaskGroupId);
                    }
                });
            } else {
                runButton.setText("▶️ 开始");
                runButton.setStyle(
                    "-fx-background-color: #10B981; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
                // 点击开始按钮时，运行当前任务组
                runButton.setOnAction(e -> {
                    if (callback != null) {
                        callback.onRun();
                    }
                });
            }
            
            // 更新运行中任务下拉菜单
            if (runningJobs.isEmpty()) {
                runningTasksMenu.setVisible(false);
            } else {
                runningTasksMenu.setVisible(true);
                runningTasksMenu.setText("🛑 运行中 (" + runningJobs.size() + ")");
                runningTasksMenu.getItems().clear();
                
                // 添加每个运行中的任务组
                for (RunningJobGroup job : runningJobs.values()) {
                    if (job.isRunning()) {
                        MenuItem menuItem = new MenuItem(
                            "🛑 " + job.getJobName() + " (ID: " + job.getJobId() + ")"
                        );
                        menuItem.setStyle(
                            "-fx-text-fill: #EF4444; " +
                            "-fx-font-weight: bold;"
                        );
                        
                        menuItem.setOnAction(e -> {
                            if (callback != null) {
                                callback.onStop(job.getJobId());
                            }
                        });
                        
                        runningTasksMenu.getItems().add(menuItem);
                    }
                }
                
                // 添加分隔线
                if (!runningTasksMenu.getItems().isEmpty()) {
                    runningTasksMenu.getItems().add(new SeparatorMenuItem());
                    
                    // 添加"停止所有"选项
                    MenuItem stopAllItem = new MenuItem("🛑 停止所有");
                    stopAllItem.setStyle(
                        "-fx-text-fill: #EF4444; " +
                        "-fx-font-weight: bold;"
                    );
                    stopAllItem.setOnAction(e -> {
                        for (RunningJobGroup job : runningJobs.values()) {
                            if (job.isRunning() && callback != null) {
                                callback.onStop(job.getJobId());
                            }
                        }
                    });
                    runningTasksMenu.getItems().add(stopAllItem);
                }
            }
        });
    }
    
    /**
     * 更新运行按钮显示，显示运行中的任务组列表
     * @param runningJobs 运行中的任务组列表
     */
    public void updateRunningJobs(Map<Long, RunningJobGroup> runningJobs) {
        this.runningJobs = runningJobs != null ? new java.util.HashMap<>(runningJobs) : new java.util.HashMap<>();
        updateButtonState();
    }
}

