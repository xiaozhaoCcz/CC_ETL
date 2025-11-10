package com.cc.job.gui.view;

import com.cc.job.gui.model.RunningJobGroup;
import com.cc.job.gui.util.StyleUtil;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 任务组导航栏 - 类似浏览器标签页
 */
public class TaskNavigationBar extends HBox {
    
    private HBox tabContainer;
    private Map<String, TaskTab> tabs;
    private String currentTaskGroup;
    private TaskSwitchCallback switchCallback;
    
    // 运行/停止按钮区域
    private HBox actionButtonArea;
    private Button runOrRetryButton;
    private MenuButton runningTasksMenu;
    
    // 回调接口
    public interface RunCallback {
        void onRun();
    }
    
    public interface StopCallback {
        void onStop(Long jobId);
    }
    
    private RunCallback runCallback;
    private StopCallback stopCallback;
    
    // 当前任务组ID（用于判断是否正在运行）
    private Long currentTaskGroupId;
    
    // 运行中的任务组列表
    private Map<Long, RunningJobGroup> runningJobs = new HashMap<>();
    
    public interface TaskSwitchCallback {
        void onTaskSwitch(String taskGroupName);
    }
    
    public TaskNavigationBar() {
        tabs = new HashMap<>();
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.98), rgba(243,244,246,0.96)); " +
            "-fx-border-color: transparent transparent rgba(148,163,184,0.35) transparent; " +
            "-fx-border-width: 0 0 1 0; " +
            "-fx-padding: 8 16 8 16;"
        );
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(54);
        
        // 标签容器
        tabContainer = new HBox(8);
        tabContainer.setAlignment(Pos.CENTER_LEFT);
        tabContainer.setPadding(new Insets(0, 12, 0, 0));
        
        // 滚动面板包装标签容器
        ScrollPane scrollPane = new ScrollPane(tabContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 0;"
        );
        scrollPane.setPannable(true);
        
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        // 不再显示运行/停止按钮，这些按钮已移至顶部工具栏
        getChildren().add(scrollPane);
    }
    
    /**
     * 添加任务组标签
     */
    public void addTaskGroup(String taskGroupName) {
        if (tabs.containsKey(taskGroupName)) {
            // 如果已存在，直接切换到该标签
            switchToTaskGroup(taskGroupName);
            return;
        }
        
        TaskTab tab = new TaskTab(taskGroupName);
        tab.setOnClick(() -> switchToTaskGroup(taskGroupName));
        tab.setOnClose(() -> removeTaskGroup(taskGroupName));
        
        tabs.put(taskGroupName, tab);
        tabContainer.getChildren().add(tab);
        
        // 自动切换到新添加的标签
        switchToTaskGroup(taskGroupName);
    }
    
    /**
     * 添加或选择任务组标签（别名方法）
     */
    public void addOrSelectTask(String taskGroupName) {
        addTaskGroup(taskGroupName);
    }
    
    /**
     * 切换到指定任务组
     */
    public void switchToTaskGroup(String taskGroupName) {
        if (!tabs.containsKey(taskGroupName)) {
            return;
        }
        
        // 更新所有标签的激活状态
        tabs.forEach((name, tab) -> {
            tab.setActive(name.equals(taskGroupName));
        });
        
        currentTaskGroup = taskGroupName;
        
        // 触发切换回调
        if (switchCallback != null) {
            switchCallback.onTaskSwitch(taskGroupName);
        }
        
        // 更新按钮状态
        updateButtonState();
    }
    
    /**
     * 设置当前任务组ID
     */
    public void setCurrentTaskGroupId(Long taskGroupId) {
        this.currentTaskGroupId = taskGroupId;
        updateButtonState();
    }
    
    /**
     * 设置运行回调
     */
    public void setOnRun(RunCallback callback) {
        this.runCallback = callback;
    }
    
    /**
     * 设置停止回调
     */
    public void setOnStop(StopCallback callback) {
        this.stopCallback = callback;
    }
    
    /**
     * 更新运行中的任务组列表
     */
    public void updateRunningJobs(Map<Long, RunningJobGroup> runningJobs) {
        this.runningJobs = runningJobs != null ? new HashMap<>(runningJobs) : new HashMap<>();
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
            
            // 更新运行/重试按钮
            if (isCurrentRunning) {
                runOrRetryButton.setText("🔄 重试");
                runOrRetryButton.setStyle(
                    "-fx-background-color: #F97316; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
            } else {
                runOrRetryButton.setText("▶️ 开始");
                runOrRetryButton.setStyle(
                    "-fx-background-color: #10B981; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
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
                            if (stopCallback != null) {
                                stopCallback.onStop(job.getJobId());
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
                            if (job.isRunning() && stopCallback != null) {
                                stopCallback.onStop(job.getJobId());
                            }
                        }
                    });
                    runningTasksMenu.getItems().add(stopAllItem);
                }
            }
        });
    }
    
    /**
     * 移除任务组标签
     */
    public void removeTaskGroup(String taskGroupName) {
        TaskTab tab = tabs.remove(taskGroupName);
        if (tab != null) {
            tabContainer.getChildren().remove(tab);
            
            // 如果删除的是当前标签，切换到第一个标签
            if (taskGroupName.equals(currentTaskGroup) && !tabs.isEmpty()) {
                String firstTab = tabs.keySet().iterator().next();
                switchToTaskGroup(firstTab);
            } else if (tabs.isEmpty()) {
                currentTaskGroup = null;
            }
        }
    }
    
    /**
     * 获取当前任务组
     */
    public String getCurrentTaskGroup() {
        return currentTaskGroup;
    }
    
    /**
     * 根据任务组ID获取任务组名称
     * 注意：这个方法需要配合外部存储的ID到名称的映射来使用
     * 当前实现只是简单地从标签名称中查找
     */
    public String getTaskNameById(Long jobId) {
        // 如果有任务组名称到ID的映射，可以通过反向查找
        // 这里暂时返回null，让调用方从映射中查找
        return null;
    }
    
    /**
     * 设置切换回调
     */
    public void setOnTaskSwitch(TaskSwitchCallback callback) {
        this.switchCallback = callback;
    }
    
    /**
     * 设置切换回调（重载方法，接受 Consumer）
     */
    public void setOnTaskSwitch(Consumer<String> callback) {
        this.switchCallback = callback::accept;
    }
    
    /**
     * 设置任务选择回调（别名方法）
     */
    public void setOnTaskSelected(Consumer<String> callback) {
        setOnTaskSwitch(callback);
    }
    
    /**
     * 更新指定任务组的运行状态（显示/隐藏小绿点）
     */
    public void updateTaskGroupRunningStatus(String taskGroupName, boolean isRunning) {
        Platform.runLater(() -> {
            TaskTab tab = tabs.get(taskGroupName);
            if (tab != null) {
                tab.setRunning(isRunning);
            }
        });
    }
    
    /**
     * 清除所有任务组的运行状态
     */
    public void clearAllRunningStatus() {
        Platform.runLater(() -> {
            for (TaskTab tab : tabs.values()) {
                tab.setRunning(false);
            }
        });
    }
    
    /**
     * 任务组标签
     */
    private static class TaskTab extends StackPane {
        
        private static final String BASE_STYLE =
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand; " +
            "-fx-effect: null;";
        private static final String NORMAL_STYLE = BASE_STYLE +
            "-fx-background-color: rgba(255,255,255,0.88); " +
            "-fx-border-color: rgba(148,163,184,0.45);";
        private static final String HOVER_STYLE = BASE_STYLE +
            "-fx-background-color: rgba(241,245,249,0.95); " +
            "-fx-border-color: rgba(99,102,241,0.35); " +
            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.12), 12, 0, 0, 2);";
        private static final String ACTIVE_STYLE = BASE_STYLE +
            "-fx-background-color: linear-gradient(to bottom, rgba(99,102,241,0.2), rgba(79,70,229,0.28)); " +
            "-fx-border-color: rgba(79,70,229,0.6); " +
            "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.32), 20, 0, 0, 4);";
        
        private final String taskGroupName;
        private boolean active;
        private Runnable onClickCallback;
        private Runnable onCloseCallback;
        private Label nameLabel;
        
        // 运行状态指示器
        private Circle runningIndicator;
        private FadeTransition blinkAnimation;
        private boolean isRunning = false;
        
        public TaskTab(String taskGroupName) {
            this.taskGroupName = taskGroupName;
            initializeUI();
        }
        
        private void initializeUI() {
            setPadding(new Insets(6, 14, 6, 14));
            setStyle(NORMAL_STYLE);
            
            HBox content = new HBox(10);
            content.setAlignment(Pos.CENTER_LEFT);
            
            // 运行状态指示器（小绿点）
            runningIndicator = new Circle(4.5);
            runningIndicator.setFill(Color.web(StyleUtil.SUCCESS));
            runningIndicator.setStroke(Color.TRANSPARENT);
            runningIndicator.setVisible(false);
            runningIndicator.setEffect(new javafx.scene.effect.Glow(0.6));
            
            // 设置闪烁动画
            blinkAnimation = new FadeTransition(Duration.millis(800), runningIndicator);
            blinkAnimation.setFromValue(1.0);
            blinkAnimation.setToValue(0.2);
            blinkAnimation.setCycleCount(FadeTransition.INDEFINITE);
            blinkAnimation.setAutoReverse(true);
            
            // 任务组名称
            nameLabel = new Label(taskGroupName);
            nameLabel.setStyle(
                "-fx-font-size: 12.5px; " +
                "-fx-font-weight: 600; " +
                "-fx-text-fill: " + StyleUtil.GRAY_600 + ";"
            );
            
            // 关闭按钮
            Label closeIcon = new Label("×");
            closeIcon.setStyle(
                "-fx-text-fill: " + StyleUtil.GRAY_400 + "; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 400; " +
                "-fx-padding: 0;"
            );
            StackPane closeBtn = new StackPane(closeIcon);
            closeBtn.setPrefSize(20, 20);
            closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
            closeBtn.setOnMouseEntered(e -> {
                closeBtn.setStyle("-fx-background-color: rgba(239,68,68,0.12); -fx-background-radius: 10; -fx-cursor: hand;");
                closeIcon.setStyle(
                    "-fx-text-fill: " + StyleUtil.ERROR + "; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: 500; " +
                    "-fx-padding: 0;"
                );
            });
            closeBtn.setOnMouseExited(e -> {
                closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
                closeIcon.setStyle(
                    "-fx-text-fill: " + StyleUtil.GRAY_400 + "; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: 400; " +
                    "-fx-padding: 0;"
                );
            });
            closeBtn.setOnMouseClicked(e -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
                e.consume();
            });
            
            content.getChildren().addAll(runningIndicator, nameLabel, closeBtn);
            getChildren().add(content);
            
            // 点击标签切换
            setOnMouseClicked(e -> {
                if (onClickCallback != null) {
                    onClickCallback.run();
                }
            });
            
            // 悬停效果
            setOnMouseEntered(e -> {
                if (!active) {
                    setStyle(HOVER_STYLE);
                }
            });
            
            setOnMouseExited(e -> updateStyle());
        }
        
        public void setActive(boolean active) {
            this.active = active;
            updateStyle();
        }
        
        private void updateStyle() {
            setStyle(active ? ACTIVE_STYLE : NORMAL_STYLE);
            updateLabelColor();
        }
        
        private void updateLabelColor() {
            if (active) {
                nameLabel.setStyle(
                    "-fx-font-size: 12.5px; " +
                    "-fx-font-weight: 700; " +
                    "-fx-text-fill: " + StyleUtil.PRIMARY_DARK + ";"
                );
            } else if (isRunning) {
                nameLabel.setStyle(
                    "-fx-font-size: 12.5px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-text-fill: " + StyleUtil.SUCCESS_DARK + ";"
                );
            } else {
                nameLabel.setStyle(
                    "-fx-font-size: 12.5px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-text-fill: " + StyleUtil.GRAY_600 + ";"
                );
            }
        }
        
        public void setOnClick(Runnable callback) {
            this.onClickCallback = callback;
        }
        
        public void setOnClose(Runnable callback) {
            this.onCloseCallback = callback;
        }
        
        /**
         * 设置运行状态
         */
        public void setRunning(boolean running) {
            Platform.runLater(() -> {
                this.isRunning = running;
                if (running) {
                    runningIndicator.setVisible(true);
                    if (blinkAnimation.getStatus() != FadeTransition.Status.RUNNING) {
                        blinkAnimation.play();
                    }
                } else {
                    runningIndicator.setVisible(false);
                    if (blinkAnimation.getStatus() == FadeTransition.Status.RUNNING) {
                        blinkAnimation.stop();
                        runningIndicator.setOpacity(1.0);
                    }
                }
                updateLabelColor();
            });
        }
        
        /**
         * 获取任务组名称
         */
        public String getTaskGroupName() {
            return taskGroupName;
        }
    }
}

