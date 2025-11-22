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
    // ⚠️ 关键修复：使用ID作为key，支持同名任务组
    private Map<Long, TaskTab> tabs;
    // 存储ID到名称的映射（用于显示）
    private Map<Long, String> taskGroupIdToNameMap = new HashMap<>();
    private Long currentTaskGroupId;
    private TaskSwitchCallback switchCallback;
    private TaskCloseCallback closeCallback;
    
    public interface TaskCloseCallback {
        void onTaskClose(Long taskGroupId, String taskGroupName);
    }
    
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
    
    
    // 运行中的任务组列表
    private Map<Long, RunningJobGroup> runningJobs = new HashMap<>();
    
    public interface TaskSwitchCallback {
        void onTaskSwitch(String taskGroupName, Long taskGroupId);
    }
    
    public TaskNavigationBar() {
        tabs = new HashMap<>();
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: transparent; " +
            "-fx-border-width: 0;"
        );
        setSpacing(0);
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(54);
        
        // 标签容器 - 无间距，标签页紧贴
        tabContainer = new HBox(0);
        tabContainer.setAlignment(Pos.CENTER_LEFT);
        tabContainer.setPadding(new Insets(0));
        
        // 滚动面板包装标签容器
        ScrollPane scrollPane = new ScrollPane(tabContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 0; " +
            "-fx-background-insets: 0;"
        );
        scrollPane.setPannable(true);
        
        // 确保内容从最左边开始，移除默认的内容边距
        Platform.runLater(() -> {
            javafx.scene.Node content = scrollPane.getContent();
            if (content != null) {
                HBox.setMargin(content, new Insets(0));
            }
        });
        
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        // 不再显示运行/停止按钮，这些按钮已移至顶部工具栏
        getChildren().add(scrollPane);
    }
    
    /**
     * 添加任务组标签
     * @param taskGroupName 任务组名称
     * @param taskGroupId 任务组ID（必须，用于唯一标识）
     */
    public void addTaskGroup(String taskGroupName, Long taskGroupId) {
        if (taskGroupId == null) {
            throw new IllegalArgumentException("任务组ID不能为null");
        }
        
        // ⚠️ 关键修复：使用ID作为key，支持同名任务组
        if (tabs.containsKey(taskGroupId)) {
            // 如果已存在该ID的标签，直接切换到该标签
            switchToTaskGroup(taskGroupId);
            return;
        }
        
        TaskTab tab = new TaskTab(taskGroupName, taskGroupId);
        tab.setOnClick(() -> switchToTaskGroup(taskGroupId));
        tab.setOnClose(() -> removeTaskGroup(taskGroupId));
        
        tabs.put(taskGroupId, tab);
        taskGroupIdToNameMap.put(taskGroupId, taskGroupName);
        tabContainer.getChildren().add(tab);
        
        // 自动切换到新添加的标签
        switchToTaskGroup(taskGroupId);
    }
    
    /**
     * 添加或选择任务组标签（别名方法）
     * @param taskGroupName 任务组名称
     * @param taskGroupId 任务组ID（必须）
     */
    public void addOrSelectTask(String taskGroupName, Long taskGroupId) {
        addTaskGroup(taskGroupName, taskGroupId);
    }
    
    /**
     * 切换到指定任务组（使用ID）
     */
    public void switchToTaskGroup(Long taskGroupId) {
        if (taskGroupId == null || !tabs.containsKey(taskGroupId)) {
            return;
        }
        
        // 更新所有标签的激活状态
        tabs.forEach((id, tab) -> {
            tab.setActive(id.equals(taskGroupId));
        });
        
        currentTaskGroupId = taskGroupId;
        
        // 获取任务组名称
        String taskGroupName = taskGroupIdToNameMap.get(taskGroupId);
        
        // 触发切换回调，传递名称和ID
        if (switchCallback != null) {
            switchCallback.onTaskSwitch(taskGroupName != null ? taskGroupName : ("任务组 " + taskGroupId), taskGroupId);
        }
        
        // 更新按钮状态
        updateButtonState();
    }
    
    /**
     * 设置当前任务组ID
     */
    public void setCurrentTaskGroupId(Long taskGroupId) {
        if (taskGroupId != null && tabs.containsKey(taskGroupId)) {
            this.currentTaskGroupId = taskGroupId;
            // 更新标签激活状态
            tabs.forEach((id, tab) -> {
                tab.setActive(id.equals(taskGroupId));
            });
        }
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
     * 注意：运行/停止按钮已移至 TopToolBar，此方法仅保留用于兼容性
     * 如果按钮不存在则直接返回，避免 NullPointerException
     */
    private void updateButtonState() {
        Platform.runLater(() -> {
            // 运行/停止按钮已移至 TopToolBar，这里不再需要更新
            // 如果按钮不存在（null），直接返回，避免 NullPointerException
            if (runOrRetryButton == null && runningTasksMenu == null) {
                return;
            }
            
            // 判断当前任务组是否正在运行
            boolean isCurrentRunning = currentTaskGroupId != null && 
                runningJobs.containsKey(currentTaskGroupId) && 
                runningJobs.get(currentTaskGroupId).isRunning();
            
            // 更新运行/重试按钮（如果存在）
            if (runOrRetryButton != null) {
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
            }
            
            // 更新运行中任务下拉菜单（如果存在）
            if (runningTasksMenu != null) {
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
            }
        });
    }
    
    /**
     * 移除任务组标签（使用ID）
     */
    public void removeTaskGroup(Long taskGroupId) {
        if (taskGroupId == null) {
            return;
        }
        
        TaskTab tab = tabs.remove(taskGroupId);
        String taskGroupName = taskGroupIdToNameMap.remove(taskGroupId);
        
        if (tab != null) {
            tabContainer.getChildren().remove(tab);
            
            // 判断是否是关闭当前标签页
            boolean isClosingCurrentTab = taskGroupId.equals(currentTaskGroupId);
            
            // 触发关闭回调，通知外部清除树形视图的选中状态
            // 注意：如果关闭的是当前标签页，会立即切换到新标签页，不需要清除选中状态
            if (closeCallback != null && !isClosingCurrentTab && taskGroupName != null) {
                closeCallback.onTaskClose(taskGroupId, taskGroupName);
            }
            
            // 如果删除的是当前标签，切换到第一个标签
            if (isClosingCurrentTab && !tabs.isEmpty()) {
                Long firstTabId = tabs.keySet().iterator().next();
                switchToTaskGroup(firstTabId);
            } else if (tabs.isEmpty()) {
                currentTaskGroupId = null;
            }
        }
    }
    
    /**
     * 清除所有任务组标签
     */
    public void clearAllTasks() {
        tabs.clear();
        tabContainer.getChildren().clear();
        taskGroupIdToNameMap.clear(); // ⚠️ 关键修复：清除ID到名称的映射
        currentTaskGroupId = null;
    }
    
    /**
     * 设置关闭回调
     */
    public void setOnTaskClose(TaskCloseCallback callback) {
        this.closeCallback = callback;
    }
    
    /**
     * 获取当前任务组名称
     */
    public String getCurrentTaskGroup() {
        return currentTaskGroupId != null ? taskGroupIdToNameMap.get(currentTaskGroupId) : null;
    }
    
    /**
     * 获取当前任务组ID
     */
    public Long getCurrentTaskGroupId() {
        return currentTaskGroupId;
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
     * 设置切换回调（重载方法，接受 Consumer<String>，兼容旧代码）
     */
    public void setOnTaskSwitch(Consumer<String> callback) {
        this.switchCallback = (name, id) -> callback.accept(name);
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
    public void updateTaskGroupRunningStatus(Long taskGroupId, boolean isRunning) {
        Platform.runLater(() -> {
            TaskTab tab = tabs.get(taskGroupId);
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
        
        // 移除圆角和边框，未选中时背景为白色，选中时为浅灰色
        // 未选中标签右边有分割线，颜色与选中标签背景一致
        private static final String BASE_STYLE =
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0; " +
            "-fx-cursor: hand; " +
            "-fx-effect: null;";
        private static final String NORMAL_STYLE = BASE_STYLE +
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: transparent transparent transparent rgba(243,244,246,0.9); " +
            "-fx-border-width: 0 0 0 1;";
        private static final String HOVER_STYLE = BASE_STYLE +
            "-fx-background-color: rgba(241,245,249,0.95); " +
            "-fx-border-color: transparent transparent transparent rgba(243,244,246,0.9); " +
            "-fx-border-width: 0 0 0 1;";
        private static final String ACTIVE_STYLE = BASE_STYLE +
            "-fx-background-color: rgba(243,244,246,0.9); " +
            "-fx-border-width: 0;";
        
        private final String taskGroupName;
        private final Long taskGroupId; // ⚠️ 关键修复：存储任务组ID
        private boolean active;
        private Runnable onClickCallback;
        private Runnable onCloseCallback;
        private Label nameLabel;
        
        // 运行状态指示器
        private Circle runningIndicator;
        private FadeTransition blinkAnimation;
        private boolean isRunning = false;
        
        public TaskTab(String taskGroupName, Long taskGroupId) {
            this.taskGroupName = taskGroupName;
            this.taskGroupId = taskGroupId;
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
        
        /**
         * 获取任务组ID
         */
        public Long getTaskGroupId() {
            return taskGroupId;
        }
    }
}

