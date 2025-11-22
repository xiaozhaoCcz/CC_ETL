package com.cc.job.gui.view;

import com.cc.job.gui.model.RunningJobGroup;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.SessionManager;
import com.cc.job.gui.util.StyleUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 顶部工具栏组件
 */
public class TopToolBar extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(TopToolBar.class);
    
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
        void onSelect(); // 框选功能
        void onLayoutHorizontal(); // 横向布局
        void onLayoutVertical(); // 纵向布局
    }
    
    private ToolBarCallback callback;
    private Label zoomLabel;
    private double currentZoom = 1.0;
    
    // 运行按钮和下拉菜单
    private Button runButton;
    private Button stopButton;  // 独立的停止按钮
    private Button retryButton;
    private MenuButton runningTasksMenu;
    private HBox runGroup;

    private Button undoButton;
    private Button redoButton;
    private Button selectButton; // 框选按钮
    
    // 当前任务组ID（用于判断是否正在运行）
    private Long currentTaskGroupId;
    
    // 运行中的任务组列表
    private Map<Long, RunningJobGroup> runningJobs = new java.util.HashMap<>();
    
    public TopToolBar() {
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: transparent transparent rgba(148,163,184,0.35) transparent; " +
            "-fx-border-width: 0 0 1 0;"
        );
        setPadding(new Insets(0));
        
        // 工具栏（移除菜单栏，使用更简洁的设计）
        HBox toolBar = createToolBar();
        
        getChildren().add(toolBar);
    }
    
    private HBox createMenuBar() {
        HBox menuBar = new HBox(0);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setPadding(new Insets(8, 10, 8, 10));
        menuBar.setStyle("-fx-background-color: #F9FAFB;");
        
        // 文件菜单 - 扁平化设计
        Label fileMenu = createMenuLabel("文件");
        Label taskMenu = createMenuLabel("任务组");
        
        menuBar.getChildren().addAll(fileMenu, taskMenu);
        
        return menuBar;
    }
    
    private Label createMenuLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 5 15 5 15; -fx-font-size: 13; -fx-cursor: hand; -fx-font-weight: 500;");
        label.setOnMouseEntered(e -> {
            if (!label.getStyle().contains("border-width")) {
                label.setStyle("-fx-background-color: #F3F4F6; -fx-padding: 5 15 5 15; -fx-font-size: 13; -fx-cursor: hand; -fx-font-weight: 500;");
            }
        });
        label.setOnMouseExited(e -> {
            if (!label.getStyle().contains("border-width")) {
                label.setStyle("-fx-padding: 5 15 5 15; -fx-font-size: 13; -fx-cursor: hand; -fx-font-weight: 500;");
            }
        });
        return label;
    }
    
    private HBox createToolBar() {
        HBox toolBar = new HBox(8);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(8, 12, 8, 12));
        
        // 文件操作组
        HBox fileGroup = createToolGroup(
            createIconButton(IconUtil.plusIcon(), "新建", "创建新的流程图", () -> safeCall(ToolBarCallback::onNew)),
            createIconButton(IconUtil.folderIcon(), "打开", "打开已有流程图", () -> safeCall(ToolBarCallback::onOpen)),
            createIconButton(IconUtil.saveIcon(), "保存", "保存当前流程图", () -> safeCall(ToolBarCallback::onSave))
        );
        
        Region sep1 = createSeparator();
        
        // 编辑操作组
        undoButton = createIconButton(IconUtil.undoIcon(), "撤销", "撤销上一步操作", () -> safeCall(ToolBarCallback::onUndo));
        redoButton = createIconButton(IconUtil.redoIcon(), "重做", "重做上一步操作", () -> safeCall(ToolBarCallback::onRedo));
        selectButton = createIconButton(IconUtil.selectIcon(), "框选", "框选节点和边", () -> safeCall(ToolBarCallback::onSelect));
        Button layoutHorizontalButton = createIconButton(IconUtil.layoutHorizontalIcon(), "横向布局", "横向对齐选中的节点", () -> safeCall(ToolBarCallback::onLayoutHorizontal));
        Button layoutVerticalButton = createIconButton(IconUtil.layoutVerticalIcon(), "纵向布局", "纵向对齐选中的节点", () -> safeCall(ToolBarCallback::onLayoutVertical));
        undoButton.setDisable(true);
        redoButton.setDisable(true);
        HBox editGroup = createToolGroup(undoButton, redoButton, selectButton, layoutHorizontalButton, layoutVerticalButton);
        
        Region sep2 = createSeparator();
        
        // 视图操作组
        zoomLabel = new Label("100%");
        zoomLabel.setStyle(StyleUtil.body() + "-fx-font-weight: 700; -fx-padding: 0 8 0 8;");
        
        HBox viewGroup = createToolGroup(
            createIconButton(IconUtil.zoomInIcon(), "放大", "放大画布", () -> safeCall(ToolBarCallback::onZoomIn)),
            createIconButton(IconUtil.zoomOutIcon(), "缩小", "缩小画布", () -> safeCall(ToolBarCallback::onZoomOut)),
            zoomLabel,
            createIconButton(IconUtil.expandIcon(), "适应", "适应窗口大小", () -> safeCall(ToolBarCallback::onZoomFit))
        );
        
        // 右侧空白区域
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 用户信息区域
        HBox userInfoArea = createUserInfoArea();
        
        Region sep3 = createSeparator();
        
        // 运行操作组
        runGroup = createToolGroup();
        createRunButton();
        createStopButton();  // 创建停止按钮
        createRunningTasksMenu();
        updateRunGroupButtons();
        
        toolBar.getChildren().addAll(
            fileGroup, sep1,
            editGroup, sep2,
            viewGroup,
            spacer,
            userInfoArea,
            sep3, runGroup
        );
        
        return toolBar;
    }
    
    /**
     * 创建用户信息区域
     */
    private HBox createUserInfoArea() {
        HBox userInfo = new HBox(10);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        userInfo.setPadding(new Insets(0, 12, 0, 12));
        
        // 从SessionManager获取用户信息
        SessionManager session = SessionManager.getInstance();
        String username = session.getUsername();
        String userId = session.getUserId();
        
        // 如果未登录，返回空容器
        if (username == null || username.isEmpty()) {
            return userInfo;
        }
        
        // 创建用户头像（使用首字母）
        StackPane avatar = createUserAvatar(username);
        
        // 创建用户信息文本区域
        VBox textArea = new VBox(2);
        textArea.setAlignment(Pos.CENTER_RIGHT);
        
        // 用户名
        Label nameLabel = new Label(username);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.web("#1F2937"));
        
        // 用户ID
        Label idLabel = new Label("ID: " + (userId != null ? userId : "N/A"));
        idLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        idLabel.setTextFill(Color.web("#6B7280"));
        
        textArea.getChildren().addAll(nameLabel, idLabel);
        
        // 组合头像和文本
        HBox userCard = new HBox(8);
        userCard.setAlignment(Pos.CENTER);
        userCard.setPadding(new Insets(4, 12, 4, 12));
        userCard.setStyle(
            "-fx-cursor: hand;"
        );
        userCard.getChildren().addAll(avatar, textArea);
        
        // 添加悬停效果
        userCard.setOnMouseEntered(e -> {
            userCard.setStyle(
                "-fx-background-color: #F3F4F6; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 4, 0, 0, 2);"
            );
        });
        
        userCard.setOnMouseExited(e -> {
            userCard.setStyle(
                "-fx-background-color: #F9FAFB; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });
        
        // 添加点击事件（可选：显示用户菜单）
        userCard.setOnMouseClicked(e -> {
            showUserMenu(userCard);
        });
        
        userInfo.getChildren().add(userCard);
        
        return userInfo;
    }
    
    /**
     * 创建用户头像（使用首字母圆形图标）
     */
    private StackPane createUserAvatar(String username) {
        StackPane avatar = new StackPane();
        avatar.setPrefSize(32, 32);
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        
        // 圆形背景
        Circle circle = new Circle(16);
        circle.setFill(Color.web("#6366F1"));
        circle.setStroke(Color.web("#4F46E5"));
        circle.setStrokeWidth(2);
        
        // 首字母
        String initial = username.substring(0, 1).toUpperCase();
        Label initialLabel = new Label(initial);
        initialLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        initialLabel.setTextFill(Color.WHITE);
        
        avatar.getChildren().addAll(circle, initialLabel);
        
        return avatar;
    }
    
    /**
     * 显示用户菜单
     */
    private void showUserMenu(javafx.scene.Node node) {
        ContextMenu userMenu = new ContextMenu();
        
        // 用户信息菜单项（不可点击）
        SessionManager session = SessionManager.getInstance();
        MenuItem infoItem = new MenuItem("用户: " + session.getUsername());
        infoItem.setStyle("-fx-font-weight: bold; -fx-text-fill: #1F2937;");
        infoItem.setDisable(true);
        
        MenuItem idItem = new MenuItem("ID: " + session.getUserId());
        idItem.setStyle("-fx-text-fill: #6B7280;");
        idItem.setDisable(true);
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
            // 退出登录菜单项
            MenuItem logoutItem = new MenuItem("退出登录");
            logoutItem.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            logoutItem.setOnAction(e -> {
                
                // 确认对话框
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认退出");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("确定要退出登录吗？");
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // 清除会话
                        SessionManager.getInstance().logout();
                        
                        // 关闭当前窗口
                        javafx.stage.Stage stage = (javafx.stage.Stage) getScene().getWindow();
                        stage.close();
                        
                        // 重新启动应用（显示登录界面）
                        javafx.application.Platform.runLater(() -> {
                            try {
                                new com.cc.job.gui.CcJobGuiApplication().start(new javafx.stage.Stage());
                            } catch (Exception ex) {
                                logger.error("重新启动失败: {}", ex.getMessage(), ex);
                            }
                        });
                    }
                });
            });
        
        userMenu.getItems().addAll(infoItem, idItem, separator, logoutItem);
        
        // 显示菜单
        userMenu.show(node, javafx.geometry.Side.BOTTOM, 0, 0);
    }
    
    /**
     * 创建分隔线
     */
    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setMinWidth(1);
        separator.setMaxWidth(1);
        separator.setPrefHeight(28);
        separator.setMinHeight(20);
        separator.setStyle("-fx-background-color: #E2E8F0;");
        return separator;
    }
    
    private HBox createToolGroup(javafx.scene.Node... buttons) {
        HBox group = new HBox(4);
        group.setAlignment(Pos.CENTER_LEFT);
        group.getChildren().addAll(buttons);
        return group;
    }
    
    /**
     * 创建带图标的工具按钮
     */
    private Button createIconButton(javafx.scene.Node icon, String text, String tooltip, Runnable action) {
        Button btn = new Button(text, icon);
        btn.setGraphicTextGap(6);
        StyleUtil.applyIconButtonHover(btn);
        
        if (tooltip != null) {
            Tooltip tip = new Tooltip(tooltip);
            tip.setStyle("-fx-font-size: 12px;");
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

    public void updateUndoRedoState(boolean canUndo, boolean canRedo) {
        if (undoButton != null) {
            undoButton.setDisable(!canUndo);
        }
        if (redoButton != null) {
            redoButton.setDisable(!canRedo);
        }
    }
    
    /**
     * 更新框选按钮的状态
     * @param isActive 是否处于框选模式
     */
    public void updateSelectionButtonState(boolean isActive) {
        Platform.runLater(() -> {
            if (selectButton != null) {
                if (isActive) {
                    // 框选模式激活：显示高亮效果（蓝色背景）
                    selectButton.setStyle(
                        "-fx-background-color: #2563EB; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 12 6 12; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand;"
                    );
                    selectButton.setTooltip(new Tooltip("框选模式已启用，点击可关闭"));
                    
                    // 添加悬停效果
                    selectButton.setOnMouseEntered(e -> {
                        selectButton.setStyle(
                            "-fx-background-color: #1D4ED8; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12 6 12; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 4, 0, 0, 2);"
                        );
                    });
                    selectButton.setOnMouseExited(e -> {
                        selectButton.setStyle(
                            "-fx-background-color: #2563EB; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12 6 12; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4; " +
                            "-fx-cursor: hand;"
                        );
                    });
                } else {
                    // 框选模式未激活：恢复默认样式
                    StyleUtil.applyIconButtonHover(selectButton);
                    selectButton.setTooltip(new Tooltip("框选节点和边"));
                }
            }
        });
    }
    
    /**
     * 创建开始按钮
     */
    private void createRunButton() {
        runButton = new Button("开始", IconUtil.playIcon());
        runButton.setGraphicTextGap(8);
        StyleUtil.applySuccessButtonHover(runButton);
        
        // 点击开始按钮时，运行当前任务组
        runButton.setOnAction(e -> {
            if (callback != null) {
                callback.onRun();
            }
        });
    }
    
    /**
     * 创建停止按钮（独立的停止按钮，只在满足条件时显示）
     */
    private void createStopButton() {
        stopButton = new Button("停止", IconUtil.stopIcon());
        stopButton.setGraphicTextGap(8);
        stopButton.setStyle(
            "-fx-background-color: #EF4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        StyleUtil.applyErrorButtonHover(stopButton);
        
        // 点击停止按钮时，停止当前任务组
        stopButton.setOnAction(e -> {
            if (callback != null && currentTaskGroupId != null) {
                callback.onStop(currentTaskGroupId);
            }
        });
        
        // 默认隐藏
        stopButton.setVisible(false);
        stopButton.setManaged(false);  // 不占用空间
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
        runGroup.getChildren().add(stopButton);  // 添加停止按钮
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
            
            // 获取当前登录用户ID
            String currentUserId = SessionManager.getInstance().getUserId();
            
            // 判断是否应该显示停止按钮
            boolean shouldShowStopButton = false;
            if (isCurrentRunning && currentUserId != null) {
                RunningJobGroup runningJob = runningJobs.get(currentTaskGroupId);
                String triggerUserId = runningJob.getTriggerUserId();
                // 只有当触发用户ID等于当前登录用户ID时才显示停止按钮
                shouldShowStopButton = currentUserId.equals(triggerUserId);
            }
            
            // 显示/隐藏停止按钮
            stopButton.setVisible(shouldShowStopButton);
            stopButton.setManaged(shouldShowStopButton);  // 控制是否占用空间
            
            // 开始按钮始终显示，但在任务运行时禁用
            if (isCurrentRunning) {
                runButton.setDisable(true);
                runButton.setStyle(
                    "-fx-background-color: #9CA3AF; " +
                    "-fx-text-fill: white; " +
                    "-fx-opacity: 0.6; " +
                    "-fx-cursor: default;"
                );
            } else {
                runButton.setDisable(false);
                StyleUtil.applySuccessButtonHover(runButton);
            }
            
            // 更新运行中任务下拉菜单
            if (runningJobs.isEmpty()) {
                runningTasksMenu.setVisible(false);
            } else {
                runningTasksMenu.setVisible(true);
                runningTasksMenu.setText("运行中 (" + runningJobs.size() + ")");
                runningTasksMenu.getItems().clear();
                
                // 添加每个运行中的任务组 - 扁平化设计
                for (RunningJobGroup job : runningJobs.values()) {
                    if (job.isRunning()) {
                        MenuItem menuItem = new MenuItem(
                            job.getJobName() + " (ID: " + job.getJobId() + ")"
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
                    
                    // 添加"停止所有"选项 - 扁平化设计
                    MenuItem stopAllItem = new MenuItem("停止所有");
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

