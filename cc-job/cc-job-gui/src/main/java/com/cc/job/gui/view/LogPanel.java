package com.cc.job.gui.view;

import com.cc.job.gui.manager.LogContentManager;
import com.cc.job.gui.manager.LogTabManager;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.util.StyleUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.apache.commons.text.StringEscapeUtils;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 专业的日志监控面板 - 支持多任务组标签页（简化版）
 */
public class LogPanel extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(LogPanel.class);
    
    private HBox tabContainer;
    private StackPane logContainer;
    private Label countLabel;
    private TextField searchField;
    private String currentSearchKeyword = "";
    
    private final LogTabManager tabManager;
    private final ConcurrentLinkedQueue<LogUpdateTask> pendingLogUpdates = new ConcurrentLinkedQueue<>();
    
    private Runnable onDetach;
    private Runnable onClose;
    private Button detachBtn;
    private Button closeBtn;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private javafx.animation.Timeline searchDebounceTimeline;
    private javafx.animation.Timeline batchUpdateTimeline;
    
    // 新增UI控件
    private ComboBox<String> levelFilterCombo;
    private CheckBox regexCheckBox;
    private CheckBox caseSensitiveCheckBox;
    private ToggleButton autoScrollButton;
    private Label errorCountLabel;
    private Label warnCountLabel;
    private Label infoCountLabel;
    private Label lastUpdateLabel;
    private javafx.animation.Timeline updateTimeTimeline;
    
    // 搜索历史
    private final List<String> searchHistory = new ArrayList<>();
    private static final int MAX_SEARCH_HISTORY = 10;
    
    public LogPanel() {
        tabManager = new LogTabManager();
        initializeUI();
        startBatchUpdateTimer();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 0;");
        getStyleClass().add("log-panel");
        setPrefHeight(280);
        setMinHeight(280);
        setSpacing(0);
        setPadding(new Insets(0));
        
        HBox tabBar = createTabBar();
        HBox titleBar = createTitleBar();
        
        logContainer = new StackPane();
        logContainer.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(logContainer, Priority.ALWAYS);
        
        LogContentManager defaultTab = tabManager.getTabData(null);
        logContainer.getChildren().add(defaultTab.getScrollPane());
        
        HBox statusBar = createStatusBar();
        
        getChildren().addAll(tabBar, titleBar, logContainer, statusBar);
        tabManager.switchToTaskGroup(null);
    }
    
    private HBox createTabBar() {
        HBox tabBar = new HBox(0);
        tabBar.getStyleClass().add("log-panel-tab-bar");
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPrefHeight(36);
        tabBar.setMinHeight(36);
        tabBar.setMaxHeight(36);
        
        HBox titleContainer = new HBox(8);
        titleContainer.setAlignment(Pos.CENTER_LEFT);
        titleContainer.setPadding(new Insets(0, 8, 0, 20));
        
        Label monitorLabel = new Label("监控");
        monitorLabel.getStyleClass().add("log-panel-tab-bar-title");
        
        detachBtn = new Button("", IconUtil.windowIcon());
        StyleUtil.applyIconButtonHover(detachBtn);
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnAction(e -> { if (onDetach != null) {
            onDetach.run();
        }
        });
        
        closeBtn = new Button("", IconUtil.closeIcon());
        StyleUtil.applyIconButtonHover(closeBtn);
        closeBtn.setTooltip(new Tooltip("关闭面板"));
        closeBtn.setOnAction(e -> { if (onClose != null) {
            onClose.run();
        }
        });
        
        titleContainer.getChildren().addAll(monitorLabel, detachBtn, closeBtn);
        
        tabContainer = new HBox(0);
        tabContainer.setAlignment(Pos.CENTER_LEFT);
        
        ScrollPane scrollPane = new ScrollPane(tabContainer);
        scrollPane.getStyleClass().add("log-panel-tab-bar-scroll");
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        tabBar.getChildren().addAll(titleContainer, scrollPane);
        return tabBar;
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(8);
        titleBar.getStyleClass().add("log-panel-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPrefHeight(56);
        
        // 搜索框
        searchField = new TextField();
        searchField.setPromptText("搜索日志...");
        searchField.setPrefWidth(200);
        searchField.getStyleClass().add("log-panel-search-field");
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (searchDebounceTimeline != null) {
                searchDebounceTimeline.stop();
            }
            searchDebounceTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(300),
                    e -> applySearchFilter(newVal)
                )
            );
            searchDebounceTimeline.play();
        });
        
        // 级别过滤下拉菜单
        levelFilterCombo = new ComboBox<>();
        levelFilterCombo.getItems().addAll("全部", "INFO", "WARN", "ERROR", "DEBUG", "SUCCESS");
        levelFilterCombo.setValue("全部");
        levelFilterCombo.setPrefWidth(100);
        levelFilterCombo.getStyleClass().add("log-panel-level-combo");
        levelFilterCombo.setOnAction(e -> applyFilters());
        
        // 正则表达式复选框
        regexCheckBox = new CheckBox("正则");
        regexCheckBox.getStyleClass().add("log-panel-filter-checkbox");
        regexCheckBox.setTooltip(new Tooltip("启用正则表达式搜索"));
        regexCheckBox.setOnAction(e -> applyFilters());
        
        // 大小写敏感复选框
        caseSensitiveCheckBox = new CheckBox("大小写");
        caseSensitiveCheckBox.getStyleClass().add("log-panel-filter-checkbox");
        caseSensitiveCheckBox.setTooltip(new Tooltip("大小写敏感搜索"));
        caseSensitiveCheckBox.setOnAction(e -> applyFilters());
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        countLabel = new Label("0 条日志");
        countLabel.getStyleClass().add("log-panel-count-label");
        
        Region separator = createSeparator();
        
        // 自动滚动开关
        autoScrollButton = new ToggleButton("自动滚动");
        autoScrollButton.setSelected(true);
        autoScrollButton.getStyleClass().add("log-panel-auto-scroll-btn");
        autoScrollButton.setTooltip(new Tooltip("自动滚动到底部"));

        autoScrollButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            LogContentManager data = tabManager.getCurrentTabData();
            if (data != null) {
                data.autoScrollToBottom = newVal;
            }
            // 根据选中状态更新按钮文本
            if (newVal) {
                autoScrollButton.setText("自动滚动");
            } else {
                autoScrollButton.setText("关闭滚动");
            }
        });
        
        Button scrollToTopBtn = createButton("↑", () -> {
            LogContentManager data = tabManager.getCurrentTabData();
            if (data != null) data.scrollToTop();
        });
        scrollToTopBtn.setTooltip(new Tooltip("滚动到顶部"));
        
        Button clearBtn = createButton("清空", this::clearLogs);
        Button exportBtn = createButton("导出", this::exportLogs);
        
        titleBar.getChildren().addAll(
            searchField, levelFilterCombo,
            regexCheckBox, caseSensitiveCheckBox, spacer, countLabel, separator,
            autoScrollButton, scrollToTopBtn, clearBtn, exportBtn
        );
        return titleBar;
    }
    
    private Region createSeparator() {
        Region separator = new Region();
        separator.getStyleClass().add("log-panel-separator");
        separator.setPrefWidth(1);
        separator.setMinWidth(1);
        separator.setMaxWidth(1);
        separator.setPrefHeight(24);
        return separator;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(12);
        statusBar.getStyleClass().add("log-panel-status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(10, 16, 10, 16));
        statusBar.setPrefHeight(36);
        
        Label infoLabel = new Label("提示: 启动任务后将显示实时日志信息");
        infoLabel.getStyleClass().add("log-panel-status-hint");
        
        Region separator1 = createSeparator();
        
        // 日志统计标签
        errorCountLabel = new Label("错误: 0");
        errorCountLabel.getStyleClass().add("log-panel-stat-error");
        errorCountLabel.setOnMouseClicked(e -> filterByLevel("ERROR"));
        errorCountLabel.setTooltip(new Tooltip("点击过滤错误日志"));
        
        warnCountLabel = new Label("警告: 0");
        warnCountLabel.getStyleClass().add("log-panel-stat-warn");
        warnCountLabel.setOnMouseClicked(e -> filterByLevel("WARN"));
        warnCountLabel.setTooltip(new Tooltip("点击过滤警告日志"));
        
        infoCountLabel = new Label("信息: 0");
        infoCountLabel.getStyleClass().add("log-panel-stat-info");
        infoCountLabel.setOnMouseClicked(e -> filterByLevel("INFO"));
        infoCountLabel.setTooltip(new Tooltip("点击过滤信息日志"));
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        lastUpdateLabel = new Label("最后更新: 从未");
        lastUpdateLabel.getStyleClass().add("log-panel-last-update");
        
        statusBar.getChildren().addAll(
            infoLabel, separator1, errorCountLabel, warnCountLabel, infoCountLabel,
            spacer, lastUpdateLabel
        );
        
        // 启动更新时间标签的定时器
        startUpdateTimeTimer();
        
        return statusBar;
    }
    
    private void startUpdateTimeTimer() {
        updateTimeTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                e -> updateLastUpdateTime()
            )
        );
        updateTimeTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        updateTimeTimeline.play();
    }
    
    private void updateLastUpdateTime() {
        LogContentManager data = tabManager.getCurrentTabData();
        if (data != null && data.getEntries() != null && !data.getEntries().isEmpty()) {
            // 获取最后一条日志的时间
            List<LogContentManager.LogEntry> entries = data.getEntries();
            if (!entries.isEmpty()) {
                LogContentManager.LogEntry lastEntry = entries.get(entries.size() - 1);
                if (lastEntry.timestamp != null) {
                    try {
                        LocalDateTime lastTime = LocalDateTime.parse(
                            lastEntry.timestamp,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                        );
                        LocalDateTime now = LocalDateTime.now();
                        long seconds = java.time.Duration.between(lastTime, now).getSeconds();
                        String timeStr;
                        if (seconds < 60) {
                            timeStr = seconds + "秒前";
                        } else if (seconds < 3600) {
                            timeStr = (seconds / 60) + "分钟前";
                        } else {
                            timeStr = lastTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        }
                        lastUpdateLabel.setText("最后更新: " + timeStr);
                    } catch (Exception e) {
                        lastUpdateLabel.setText("最后更新: " + lastEntry.timestamp);
                    }
                }
            }
        }
    }
    
    private void filterByLevel(String level) {
        if (levelFilterCombo != null) {
            levelFilterCombo.setValue(level);
            applyFilters();
        }
    }
    
    private Button createButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("log-panel-toolbar-btn");
        btn.setOnAction(e -> action.run());
        return btn;
    }
    
    private void startBatchUpdateTimer() {
        batchUpdateTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(100),
                e -> processBatchLogUpdates()
            )
        );
        batchUpdateTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        batchUpdateTimeline.play();
    }
    
    private void processBatchLogUpdates() {
        if (pendingLogUpdates.isEmpty()) return;
        
        Map<Long, List<LogContentManager.LogEntry>> updatesByGroup = new HashMap<>();
        while (!pendingLogUpdates.isEmpty()) {
            LogUpdateTask task = pendingLogUpdates.poll();
            if (task != null) {
                updatesByGroup.computeIfAbsent(task.taskGroupId, k -> new ArrayList<>()).add(task.entry);
            }
        }
        
        Platform.runLater(() -> {
            updatesByGroup.forEach((taskGroupId, newEntries) -> {
                LogContentManager tabData = tabManager.getTabData(taskGroupId);
                if (tabData != null) {
                    for (LogContentManager.LogEntry entry : newEntries) {
                        tabData.addEntry(entry);
                    }
                    
                    if (tabManager.isCurrentTab(taskGroupId)) {
                        tabData.renderIncremental(currentSearchKeyword);
                        updateStatusBar(tabData);
                    }
                }
            });
        });
    }
    
    private void applySearchFilter(String keyword) {
        currentSearchKeyword = keyword == null ? "" : keyword.trim();
        if (!currentSearchKeyword.isEmpty() && !searchHistory.contains(currentSearchKeyword)) {
            searchHistory.add(0, currentSearchKeyword);
            if (searchHistory.size() > MAX_SEARCH_HISTORY) {
                searchHistory.remove(searchHistory.size() - 1);
            }
        }
        applyFilters();
    }
    
    private void applyFilters() {
        LogContentManager tabData = tabManager.getCurrentTabData();
        if (tabData == null) return;
        
        // 显示加载指示器
        countLabel.setText("搜索中...");
        
        // 获取级别过滤
        Set<String> enabledLevels = new HashSet<>();
        String selectedLevel = levelFilterCombo != null ? levelFilterCombo.getValue() : "全部";
        if ("全部".equals(selectedLevel)) {
            enabledLevels.add("ALL");
        } else {
            enabledLevels.add(selectedLevel);
        }
        tabData.setEnabledLevels(enabledLevels);
        
        // 获取搜索选项
        boolean useRegex = regexCheckBox != null && regexCheckBox.isSelected();
        boolean caseSensitive = caseSensitiveCheckBox != null && caseSensitiveCheckBox.isSelected();
        tabData.setRegexEnabled(useRegex);
        tabData.setCaseSensitive(caseSensitive);
        
        // 应用过滤并渲染（异步，已在render方法中实现）
        tabData.render(currentSearchKeyword, enabledLevels, useRegex, caseSensitive);
        
        // 延迟更新状态栏（等待渲染完成）
        Platform.runLater(() -> {
            try {
                Thread.sleep(100); // 给渲染线程一些时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            updateStatusBar(tabData);
        });
    }
    
    private void updateStatusBar(LogContentManager tabData) {
        if (tabData != null) {
            if (currentSearchKeyword != null && !currentSearchKeyword.isBlank()) {
                countLabel.setText("匹配 " + tabData.filteredCount + " / 共 " + tabData.logCount + " 条");
            } else {
                countLabel.setText(tabData.logCount + " 条日志");
            }
            
            // 更新统计信息
            LogContentManager.LogStatistics stats = tabData.getLogStatistics();
            if (errorCountLabel != null) {
                errorCountLabel.setText("错误: " + stats.errorCount);
            }
            if (warnCountLabel != null) {
                warnCountLabel.setText("警告: " + stats.warnCount);
            }
            if (infoCountLabel != null) {
                infoCountLabel.setText("信息: " + stats.infoCount);
            }
        }
    }
    
    /**
     * 添加或切换到任务组标签页（保留历史日志）
     * 用于用户在不同任务组之间切换时调用
     */
    public void addOrSwitchToTaskGroup(Long taskGroupId, String taskGroupName) {
        if (taskGroupId == null) return;
        
        Platform.runLater(() -> {
            tabManager.addOrSwitchToTaskGroup(taskGroupId, taskGroupName, tabContainer);
            
            LogContentManager tabData = tabManager.getTabData(taskGroupId);
            if (tabData != null) {
                logContainer.getChildren().clear();
                logContainer.getChildren().add(tabData.getScrollPane());
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
            }
            
            // 设置标签的点击和关闭回调，并确保所有标签都有右键菜单
            LogTabManager.LogTab tab = tabManager.getLogTab(taskGroupId);
            if (tab != null) {
                tab.setOnClick(() -> switchToTaskGroup(taskGroupId));
                tab.setOnClose(() -> handleTabClose(taskGroupId));
            }
            ensureAllTabsHaveContextMenu();
        });
    }
    
    /**
     * 重新启动任务组（清空日志并切换到该标签页）
     * 用于任务组重新执行时调用，会清空该任务组的历史日志
     */
    public void restartTaskGroup(Long taskGroupId, String taskGroupName) {
        if (taskGroupId == null) return;
        
        Platform.runLater(() -> {
            // 调用tabManager的restartTaskGroup方法，会清空日志并切换到该标签页
            tabManager.restartTaskGroup(taskGroupId, taskGroupName, tabContainer);
            
            LogContentManager tabData = tabManager.getTabData(taskGroupId);
            if (tabData != null) {
                logContainer.getChildren().clear();
                logContainer.getChildren().add(tabData.getScrollPane());
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
            }
            
            // 设置标签的点击和关闭回调，并确保所有标签都有右键菜单
            LogTabManager.LogTab tab = tabManager.getLogTab(taskGroupId);
            if (tab != null) {
                tab.setOnClick(() -> switchToTaskGroup(taskGroupId));
                tab.setOnClose(() -> handleTabClose(taskGroupId));
            }
            ensureAllTabsHaveContextMenu();
        });
    }
    
    /**
     * 处理标签关闭：移除任务组标签后，根据当前选中的标签刷新下方日志展示区域。
     */
    private void handleTabClose(Long taskGroupId) {
        tabManager.removeTaskGroup(taskGroupId, tabContainer);
        switchToTaskGroup(tabManager.getCurrentTaskGroupId());
    }
    
    /**
     * 按界面从左到右顺序返回日志标签的任务组 ID 列表
     */
    private List<Long> getOrderedLogTabIds() {
        List<Long> ordered = new ArrayList<>();
        for (Node node : tabContainer.getChildren()) {
            if (node instanceof LogTabManager.LogTab) {
                ordered.add(((LogTabManager.LogTab) node).getTaskGroupId());
            }
        }
        return ordered;
    }
    
    /**
     * 关闭除指定任务组外的所有日志标签
     */
    private void closeOtherTabs(Long keepTaskGroupId) {
        List<Long> toRemove = new ArrayList<>();
        for (Long id : getOrderedLogTabIds()) {
            if (!id.equals(keepTaskGroupId)) {
                toRemove.add(id);
            }
        }
        for (Long id : toRemove) {
            handleTabClose(id);
        }
        if (tabManager.getLogTab(keepTaskGroupId) != null) {
            switchToTaskGroup(keepTaskGroupId);
        }
    }
    
    /**
     * 关闭指定任务组左侧的所有日志标签
     */
    private void closeTabsToLeft(Long ofTaskGroupId) {
        List<Long> ordered = getOrderedLogTabIds();
        int idx = ordered.indexOf(ofTaskGroupId);
        if (idx <= 0) return;
        List<Long> toRemove = new ArrayList<>(ordered.subList(0, idx));
        for (Long id : toRemove) {
            handleTabClose(id);
        }
        if (tabManager.getLogTab(ofTaskGroupId) != null) {
            switchToTaskGroup(ofTaskGroupId);
        }
    }
    
    /**
     * 关闭指定任务组右侧的所有日志标签
     */
    private void closeTabsToRight(Long ofTaskGroupId) {
        List<Long> ordered = getOrderedLogTabIds();
        int idx = ordered.indexOf(ofTaskGroupId);
        if (idx < 0 || idx >= ordered.size() - 1) return;
        List<Long> toRemove = new ArrayList<>(ordered.subList(idx + 1, ordered.size()));
        for (Long id : toRemove) {
            handleTabClose(id);
        }
        if (tabManager.getLogTab(ofTaskGroupId) != null) {
            switchToTaskGroup(ofTaskGroupId);
        }
    }
    
    /**
     * 为日志标签设置右键菜单（关闭当前页 / 关闭其他页 / 关闭左侧页 / 关闭右侧页）
     */
    private void setupTabContextMenu(LogTabManager.LogTab tab) {
        ContextMenu menu = new ContextMenu();
        MenuItem closeCurrent = new MenuItem("关闭当前页");
        MenuItem closeOthers = new MenuItem("关闭其他页");
        MenuItem closeLeft = new MenuItem("关闭左侧页");
        MenuItem closeRight = new MenuItem("关闭右侧页");
        
        closeCurrent.setOnAction(e -> handleTabClose(tab.getTaskGroupId()));
        closeOthers.setOnAction(e -> closeOtherTabs(tab.getTaskGroupId()));
        closeLeft.setOnAction(e -> closeTabsToLeft(tab.getTaskGroupId()));
        closeRight.setOnAction(e -> closeTabsToRight(tab.getTaskGroupId()));
        
        menu.setOnShowing(e -> {
            List<Long> ordered = getOrderedLogTabIds();
            int index = ordered.indexOf(tab.getTaskGroupId());
            int size = ordered.size();
            closeOthers.setDisable(size <= 1);
            closeLeft.setDisable(index <= 0);
            closeRight.setDisable(index < 0 || index >= size - 1);
        });
        
        menu.getItems().addAll(closeCurrent, closeOthers, closeLeft, closeRight);
        tab.setOnContextMenuRequested(ev -> menu.show(tab, ev.getScreenX(), ev.getScreenY()));
    }
    
    /**
     * 确保所有日志标签都有右键菜单（在添加或切换标签后调用）
     */
    private void ensureAllTabsHaveContextMenu() {
        for (Node node : tabContainer.getChildren()) {
            if (node instanceof LogTabManager.LogTab) {
                setupTabContextMenu((LogTabManager.LogTab) node);
            }
        }
    }
    
    private void switchToTaskGroup(Long taskGroupId) {
        Platform.runLater(() -> {
            tabManager.switchToTaskGroup(taskGroupId);
            
            LogContentManager tabData = tabManager.getTabData(taskGroupId);
            if (tabData != null) {
                logContainer.getChildren().clear();
                logContainer.getChildren().add(tabData.getScrollPane());
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
            }
        });
    }
    
    public void info(Long taskGroupId, String message) {
        appendLog(taskGroupId, "INFO", message);
    }
    
    public void info(String message) {
        appendLog(null, "INFO", message);
    }
    
    public void warn(String message) {
        appendLog(null, "WARN", message);
    }
    
    public void warn(Long taskGroupId, String message) {
        appendLog(taskGroupId, "WARN", message);
    }
    
    public void error(String message) {
        appendLog(null, "ERROR", message);
    }
    
    public void error(Long taskGroupId, String message) {
        appendLog(taskGroupId, "ERROR", message);
    }
    
    public void debug(String message) {
        appendLog(null, "DEBUG", message);
    }
    
    public void success(String message) {
        appendLog(null, "SUCCESS", message);
    }
    
    public void success(Long taskGroupId, String message) {
        appendLog(taskGroupId, "SUCCESS", message);
    }
    
    public void appendText(Long taskGroupId, String text) {
        if (text == null) return;
        
        Platform.runLater(() -> {
            String decodedText = decodeHtmlEntities(text).trim();
            if (decodedText.isEmpty()) return;
            
            String lowerText = decodedText.toLowerCase();
            
            boolean isSuccess = lowerText.contains("执行结果:成功") || lowerText.contains("任务执行成功");
            boolean isError = !isSuccess && (lowerText.contains("执行结果:失败") || 
                              lowerText.contains("错误") || lowerText.contains("error") || 
                              lowerText.contains("失败") || lowerText.contains("exception"));
            boolean isWarn = lowerText.contains("警告") || lowerText.contains("warn") || lowerText.contains("⚠");
            
            String messageColor = isError ? "#DC2626" : (isWarn ? "#D97706" : "#000000");
            String levelColor = isError ? "#DC2626" : (isWarn ? "#D97706" : StyleUtil.GRAY_500);
            
            LogContentManager.LogEntry entry = new LogContentManager.LogEntry(
                null, null, "TEXT", levelColor, decodedText, messageColor, true, true
            );
            
            pendingLogUpdates.offer(new LogUpdateTask(taskGroupId, entry));
            checkAndShowNotification(decodedText, lowerText, isError, isWarn, isSuccess);
        });
    }
    
    public void appendText(String text) {
        appendText(null, text);
    }
    
    private void appendLog(Long taskGroupId, String level, String message) {
        Platform.runLater(() -> {
            LogContentManager tabData = tabManager.getTabData(taskGroupId);
            if (tabData == null) return;
            
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            String[] styles = getLogStyle(level);
            String decodedMessage = decodeHtmlEntities(message).trim();
            
            if (decodedMessage.isEmpty()) return;
            
            boolean isExecutionLog = isExecutionRelatedLog(decodedMessage);
            
            LogContentManager.LogEntry entry = new LogContentManager.LogEntry(
                timestamp, styles[0], level, styles[1], decodedMessage, styles[2], false, isExecutionLog
            );
            
            pendingLogUpdates.offer(new LogUpdateTask(taskGroupId, entry));
        });
    }
    
    private String[] getLogStyle(String level) {
        return switch (level) {
            case "INFO" -> new String[]{"[i]", "#2563EB", "#1E40AF"};
            case "WARN" -> new String[]{"[!]", "#D97706", "#B45309"};
            case "ERROR" -> new String[]{"[×]", "#DC2626", "#B91C1C"};
            case "DEBUG" -> new String[]{"[?]", "#2563EB", "#1D4ED8"};
            case "SUCCESS" -> new String[]{"[✓]", "#059669", "#047857"};
            default -> new String[]{"[-]", "#6B7280", "#374151"};
        };
    }
    
    private String decodeHtmlEntities(String text) {
        if (text == null) return "";
        try {
            String decoded = StringEscapeUtils.unescapeHtml4(text);
            return decoded.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n");
        } catch (Exception e) {
            return text;
        }
    }
    
    private boolean isExecutionRelatedLog(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("开始执行") || lower.contains("任务组") || lower.contains("任务执行") ||
               lower.contains("任务完成") || lower.contains("执行批次") || lower.contains("sse") ||
               lower.contains("更新节点状态") || lower.contains("═══════");
    }
    
    private void checkAndShowNotification(String text, String lowerText, boolean isError, boolean isWarn, boolean isSuccess) {
        boolean shouldShow = false;
        NotificationToast.NotificationType type = null;
        String msg = null;
        
        if (isError && (lowerText.contains("任务执行失败") || lowerText.contains("执行结果:失败"))) {
            shouldShow = true;
            type = NotificationToast.NotificationType.ERROR;
            msg = "任务执行失败，请查看日志详情";
        } else if (isWarn && lowerText.contains("警告")) {
            shouldShow = true;
            type = NotificationToast.NotificationType.WARNING;
            msg = text.length() > 120 ? text.substring(0, 117) + "..." : text;
        }
        
        if (shouldShow && type != null && msg != null) {
            NotificationToast.show(msg, type);
        }
    }
    
    private void clearLogs() {
        LogContentManager tabData = tabManager.getCurrentTabData();
        if (tabData == null) return;
        
        // 显示确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认清空");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要清空当前标签页的日志吗？此操作不可撤销。");
        
        // 添加"清空所有标签"选项
        ButtonType clearAllButton = new ButtonType("清空所有标签");
        ButtonType clearCurrentButton = new ButtonType("清空当前标签");
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(clearAllButton, clearCurrentButton, cancelButton);
        
        confirmAlert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == clearAllButton) {
                // 清空所有标签
                Platform.runLater(() -> {
                    for (LogContentManager data : tabManager.tabDataMap.values()) {
                        if (data != null) {
                            data.clearEntries();
                            data.status = "就绪";
                            data.statusColor = "#10B981";
                        }
                    }
                    tabData.render(currentSearchKeyword);
                    updateStatusBar(tabData);
                    info("所有标签页的日志已清空");
                });
            } else if (buttonType == clearCurrentButton) {
                // 清空当前标签
                Platform.runLater(() -> {
                    tabData.clearEntries();
                    tabData.status = "就绪";
                    tabData.statusColor = "#10B981";
                    tabData.render(currentSearchKeyword);
                    updateStatusBar(tabData);
                    info("日志已清空");
                });
            }
        });
    }
    
    private void exportLogs() {
        LogContentManager tabData = tabManager.getCurrentTabData();
        if (tabData == null || tabData.getEntries().isEmpty()) {
            warn("没有可导出的日志");
            return;
        }
        
        // 创建导出对话框
        Dialog<ExportOptions> dialog = new Dialog<>();
        dialog.setTitle("导出日志");
        dialog.setHeaderText("选择导出选项");
        
        // 创建选项
        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("TXT", "CSV", "JSON");
        formatCombo.setValue("TXT");
        
        RadioButton exportAll = new RadioButton("导出全部日志");
        RadioButton exportFiltered = new RadioButton("导出当前过滤结果");
        exportFiltered.setSelected(true);
        ToggleGroup exportGroup = new ToggleGroup();
        exportAll.setToggleGroup(exportGroup);
        exportFiltered.setToggleGroup(exportGroup);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
            new Label("导出格式:"),
            formatCombo,
            new Label("导出范围:"),
            exportAll,
            exportFiltered
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return new ExportOptions(
                    formatCombo.getValue(),
                    exportAll.isSelected()
                );
            }
            return null;
        });
        
        String themeCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (themeCss != null && !themeCss.isEmpty()) {
            dialog.getDialogPane().getStylesheets().add(themeCss);
        }
        Optional<ExportOptions> result = dialog.showAndWait();
        result.ifPresent(options -> {
            // 显示文件选择对话框
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("导出日志");
            
            String extension = options.format.toLowerCase();
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter(
                    options.format + "文件 (*." + extension + ")",
                    "*." + extension
                )
            );
            
            java.io.File file = fileChooser.showSaveDialog(
                searchField.getScene().getWindow()
            );
            
            if (file != null) {
                exportLogsToFile(file, options, tabData);
            }
        });
    }
    
    private void exportLogsToFile(java.io.File file, ExportOptions options, LogContentManager tabData) {
        // 显示进度对话框
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("导出中...");
        progressDialog.setHeaderText("正在导出日志，请稍候...");
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setProgress(-1); // 不确定进度
        
        Label statusLabel = new Label("准备导出...");
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(progressBar, statusLabel);
        progressDialog.getDialogPane().setContent(content);
        progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        String progressCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (progressCss != null && !progressCss.isEmpty()) {
            progressDialog.getDialogPane().getStylesheets().add(progressCss);
        }
        
        // 在后台线程中执行导出
        new Thread(() -> {
            try {
                Platform.runLater(() -> statusLabel.setText("正在收集日志数据..."));
                
                List<LogContentManager.LogEntry> entriesToExport;
                if (options.exportAll) {
                    entriesToExport = new ArrayList<>(tabData.getEntries());
                } else {
                    // 获取过滤后的日志
                    entriesToExport = new ArrayList<>();
                    Set<String> enabledLevels = tabData.getEnabledLevels();
                    
                    for (LogContentManager.LogEntry entry : tabData.getEntries()) {
                        // 应用相同的过滤条件
                        if (enabledLevels != null && !enabledLevels.isEmpty() && !enabledLevels.contains("ALL")) {
                            String entryLevel = entry.level != null ? entry.level : "TEXT";
                            if (!enabledLevels.contains(entryLevel)) {
                                continue;
                            }
                        }
                        
                        if (currentSearchKeyword == null || currentSearchKeyword.isEmpty() || 
                            entry.matches(currentSearchKeyword.toLowerCase())) {
                            entriesToExport.add(entry);
                        }
                    }
                }
                
                Platform.runLater(() -> statusLabel.setText("正在写入文件..."));
                
                // 根据格式导出
                try (java.io.FileWriter writer = new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                    switch (options.format) {
                        case "TXT":
                            exportAsTxt(writer, entriesToExport);
                            break;
                        case "CSV":
                            exportAsCsv(writer, entriesToExport);
                            break;
                        case "JSON":
                            exportAsJson(writer, entriesToExport);
                            break;
                    }
                }
                
                Platform.runLater(() -> {
                    progressDialog.close();
                    NotificationToast.showSuccess("日志已成功导出到: " + file.getAbsolutePath());
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressDialog.close();
                    error("导出失败: " + e.getMessage());
                    NotificationToast.showError("导出失败: " + e.getMessage());
                });
            }
        }, "log-export").start();
        
        progressDialog.show();
    }
    
    private void exportAsTxt(java.io.FileWriter writer, List<LogContentManager.LogEntry> entries) throws Exception {
        for (LogContentManager.LogEntry entry : entries) {
            if (entry.raw) {
                writer.write(entry.message);
            } else {
                writer.write("[" + (entry.timestamp != null ? entry.timestamp : "") + "] ");
                writer.write((entry.icon != null ? entry.icon : "") + " ");
                writer.write(String.format("%-7s", entry.level != null ? entry.level : ""));
                writer.write(" │ " + entry.message);
            }
            writer.write("\n");
        }
    }
    
    private void exportAsCsv(java.io.FileWriter writer, List<LogContentManager.LogEntry> entries) throws Exception {
        writer.write("时间戳,级别,消息\n");
        for (LogContentManager.LogEntry entry : entries) {
            writer.write("\"" + (entry.timestamp != null ? entry.timestamp : "") + "\",");
            writer.write("\"" + (entry.level != null ? entry.level : "TEXT") + "\",");
            writer.write("\"" + entry.message.replace("\"", "\"\"") + "\"\n");
        }
    }
    
    private void exportAsJson(java.io.FileWriter writer, List<LogContentManager.LogEntry> entries) throws Exception {
        writer.write("[\n");
        for (int i = 0; i < entries.size(); i++) {
            LogContentManager.LogEntry entry = entries.get(i);
            writer.write("  {\n");
            writer.write("    \"timestamp\": \"" + (entry.timestamp != null ? entry.timestamp : "") + "\",\n");
            writer.write("    \"level\": \"" + (entry.level != null ? entry.level : "TEXT") + "\",\n");
            writer.write("    \"message\": \"" + entry.message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\",\n");
            writer.write("    \"raw\": " + entry.raw + "\n");
            writer.write("  }");
            if (i < entries.size() - 1) {
                writer.write(",");
            }
            writer.write("\n");
        }
        writer.write("]\n");
    }
    
    private static class ExportOptions {
        final String format;
        final boolean exportAll;
        
        ExportOptions(String format, boolean exportAll) {
            this.format = format;
            this.exportAll = exportAll;
        }
    }
    
    public void updateStatus(String status, String colorHex) {
        Platform.runLater(() -> {
            LogContentManager tabData = tabManager.getCurrentTabData();
            if (tabData != null) {
                tabData.status = status;
                tabData.statusColor = colorHex;
                updateStatusBar(tabData);
            }
        });
    }
    
    public String getLogContent() {
        LogContentManager tabData = tabManager.getCurrentTabData();
        if (tabData != null) {
            StringBuilder sb = new StringBuilder();
            for (LogContentManager.LogEntry entry : tabData.getEntries()) {
                if (entry.raw) {
                    sb.append(entry.message);
                    if (!entry.message.endsWith("\n")) {
                        sb.append("\n");
                    }
                } else {
                    sb.append("[").append(entry.timestamp).append("] ")
                      .append(entry.icon).append(" ")
                      .append(String.format("%-7s", entry.level))
                      .append(" │ ").append(entry.message).append("\n");
                }
            }
            return sb.toString();
        }
        return "";
    }
    
    public void setOnDetach(Runnable callback) {
        this.onDetach = callback;
    }
    
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
    
    /**
     * 设置弹出和关闭按钮的可见性（用于面板弹出为独立窗口时隐藏按钮）
     */
    public void setDetachButtonsVisible(boolean visible) {
        if (detachBtn != null) {
            detachBtn.setVisible(visible);
            detachBtn.setManaged(visible);
        }
        if (closeBtn != null) {
            closeBtn.setVisible(visible);
            closeBtn.setManaged(visible);
        }
    }
    
    /**
     * 获取日志内容容器（用于弹出窗口显示）
     */
    public StackPane getLogContainer() {
        return logContainer;
    }
    
    /**
     * 获取标签栏容器（用于弹出窗口显示）
     */
    public HBox getTabContainer() {
        return tabContainer;
    }
    
    // 保存子节点的引用，用于恢复
    private HBox savedTabBar;
    private HBox savedTitleBar;
    private HBox savedStatusBar;
    
    /**
     * 弹出时调用：将所有UI元素移动到弹出窗口的容器中
     * @param targetContainer 弹出窗口的容器
     */
    public void detachContent(VBox targetContainer) {
        
        // 保存子节点引用（用于恢复）
        // LogPanel 的子节点顺序：tabBar(0), titleBar(1), logContainer(2), statusBar(3)
        if (this.getChildren().size() >= 4) {
            savedTabBar = (HBox) this.getChildren().get(0);
            savedTitleBar = (HBox) this.getChildren().get(1);
            savedStatusBar = (HBox) this.getChildren().get(3);
        }
        
        // 将所有子节点移动到目标容器
        List<Node> children = new ArrayList<>(this.getChildren());
        this.getChildren().clear();
        
        for (Node child : children) {
            targetContainer.getChildren().add(child);
            // logContainer 需要占用剩余空间
            if (child == logContainer) {
                VBox.setVgrow(child, Priority.ALWAYS);
            }
        }

    }
    
    /**
     * 关闭弹出窗口时调用：将所有UI元素移回主面板
     * @param sourceContainer 弹出窗口的容器
     */
    public void restoreContentFromDetach(VBox sourceContainer) {
        
        // 将所有子节点从源容器移回
        if (sourceContainer != null) {
            List<Node> children = new ArrayList<>(sourceContainer.getChildren());
            sourceContainer.getChildren().clear();
            
            // 清除 VBox 约束
            for (Node child : children) {
                VBox.clearConstraints(child);
            }
            
            // 添加回 LogPanel
            this.getChildren().addAll(children);
            
            // 重新设置 logContainer 的布局约束
            VBox.setVgrow(logContainer, Priority.ALWAYS);

        }
        
        // 确保所有子节点可见
        for (Node child : this.getChildren()) {
            child.setVisible(true);
            child.setManaged(true);
        }
        
        // 刷新显示
        Platform.runLater(() -> {
            LogContentManager tabData = tabManager.getCurrentTabData();
            if (tabData != null) {
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
            }
            this.requestLayout();
        });

    }
    
    /**
     * 日志更新任务
     */
    private static class LogUpdateTask {
        final Long taskGroupId;
        final LogContentManager.LogEntry entry;
        
        LogUpdateTask(Long taskGroupId, LogContentManager.LogEntry entry) {
            this.taskGroupId = taskGroupId;
            this.entry = entry;
        }
    }
}
