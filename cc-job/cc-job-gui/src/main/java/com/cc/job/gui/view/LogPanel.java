package com.cc.job.gui.view;

import com.cc.job.gui.manager.LogContentManager;
import com.cc.job.gui.manager.LogTabManager;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.util.StyleUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.apache.commons.text.StringEscapeUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 专业的日志监控面板 - 支持多任务组标签页（简化版）
 */
public class LogPanel extends VBox {
    
    private HBox tabContainer;
    private StackPane logContainer;
    private Label countLabel;
    private TextField searchField;
    private String currentSearchKeyword = "";
    
    private final LogTabManager tabManager;
    private final ConcurrentLinkedQueue<LogUpdateTask> pendingLogUpdates = new ConcurrentLinkedQueue<>();
    
    private Runnable onDetach;
    private Runnable onClose;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private javafx.animation.Timeline searchDebounceTimeline;
    private javafx.animation.Timeline batchUpdateTimeline;
    
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
        logContainer.setPadding(new Insets(12));
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
        tabBar.setStyle("-fx-background-color: #F1F5F9; -fx-border-width: 0; -fx-padding: 0;");
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPrefHeight(36);
        tabBar.setMinHeight(36);
        tabBar.setMaxHeight(36);
        
        HBox titleContainer = new HBox(8);
        titleContainer.setAlignment(Pos.CENTER_LEFT);
        titleContainer.setPadding(new Insets(0, 8, 0, 20));
        
        Label monitorLabel = new Label("监控");
        monitorLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        monitorLabel.setTextFill(Color.web(StyleUtil.GRAY_900));
        
        Button detachBtn = new Button("", IconUtil.windowIcon());
        StyleUtil.applyIconButtonHover(detachBtn);
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnAction(e -> { if (onDetach != null) onDetach.run(); });
        
        Button closeBtn = new Button("", IconUtil.closeIcon());
        StyleUtil.applyIconButtonHover(closeBtn);
        closeBtn.setTooltip(new Tooltip("关闭面板"));
        closeBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });
        
        titleContainer.getChildren().addAll(monitorLabel, detachBtn, closeBtn);
        
        tabContainer = new HBox(0);
        tabContainer.setAlignment(Pos.CENTER_LEFT);
        
        ScrollPane scrollPane = new ScrollPane(tabContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 0;");
        scrollPane.setPannable(true);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        tabBar.getChildren().addAll(titleContainer, scrollPane);
        return tabBar;
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(12);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-border-width: 0; -fx-padding: 14 20;");
        titleBar.setPrefHeight(56);
        
        searchField = new TextField();
        searchField.setPromptText("搜索日志...");
        searchField.setPrefWidth(200);
        searchField.setStyle(
            "-fx-background-color: #F1F5F9; " +
            "-fx-text-fill: " + StyleUtil.GRAY_700 + "; " +
            "-fx-font-size: 12px; " +
            "-fx-border-color: rgba(148,163,184,0.55); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 6 12 6 32;"
        );
        
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
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        countLabel = new Label("0 条日志");
        countLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        countLabel.setTextFill(Color.web(StyleUtil.GRAY_500));
        
        Region separator = createSeparator();
        
        Button scrollToTopBtn = createButton("↑", () -> {
            LogContentManager data = tabManager.getCurrentTabData();
            if (data != null) data.scrollToTop();
        });
        scrollToTopBtn.setTooltip(new Tooltip("滚动到顶部"));
        
        Button clearBtn = createButton("清空", this::clearLogs);
        Button exportBtn = createButton("导出", this::exportLogs);
        
        titleBar.getChildren().addAll(searchField, spacer, countLabel, separator, scrollToTopBtn, clearBtn, exportBtn);
        return titleBar;
    }
    
    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setMinWidth(1);
        separator.setMaxWidth(1);
        separator.setPrefHeight(24);
        separator.setStyle("-fx-background-color: #E2E8F0;");
        return separator;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(10, 16, 10, 16));
        statusBar.setPrefHeight(36);
        
        Label infoLabel = new Label("提示: 启动任务后将显示实时日志信息");
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        infoLabel.setTextFill(Color.web(StyleUtil.GRAY_500));
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label("最后更新: 从未");
        timeLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        timeLabel.setTextFill(Color.web(StyleUtil.GRAY_400));
        
        statusBar.getChildren().addAll(infoLabel, spacer, timeLabel);
        return statusBar;
    }
    
    private Button createButton(String text, Runnable action) {
        Button btn = new Button(text);
        String normal = 
            "-fx-background-color: linear-gradient(to bottom, #F1F5F9, rgba(241,245,249,0.98)); " +
            "-fx-text-fill: " + StyleUtil.GRAY_600 + "; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: rgba(148,163,184,0.6); " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand;";
        String hover = normal.replace("#F1F5F9", "rgba(226,232,240,0.99)").replace("rgba(148,163,184,0.6)", StyleUtil.PRIMARY_LIGHT);
        StyleUtil.applyButtonHover(btn, normal, hover);
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
        LogContentManager tabData = tabManager.getCurrentTabData();
        if (tabData != null) {
            tabData.render(currentSearchKeyword);
            updateStatusBar(tabData);
        }
    }
    
    private void updateStatusBar(LogContentManager tabData) {
        if (tabData != null) {
            if (currentSearchKeyword != null && !currentSearchKeyword.isBlank()) {
                countLabel.setText("匹配 " + tabData.filteredCount + " / 共 " + tabData.logCount + " 条");
            } else {
                countLabel.setText(tabData.logCount + " 条日志");
            }
        }
    }
    
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
            
            // 设置标签的点击和关闭回调
            LogTabManager.LogTab tab = tabManager.getLogTab(taskGroupId);
            if (tab != null) {
                tab.setOnClick(() -> switchToTaskGroup(taskGroupId));
                tab.setOnClose(() -> tabManager.removeTaskGroup(taskGroupId, tabContainer));
            }
        });
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
            case "DEBUG" -> new String[]{"[?]", "#7C3AED", "#6D28D9"};
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
        Platform.runLater(() -> {
            LogContentManager tabData = tabManager.getCurrentTabData();
            if (tabData != null) {
                tabData.clearEntries();
                tabData.status = "就绪";
                tabData.statusColor = "#10B981";
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
                info("日志已清空");
            }
        });
    }
    
    private void exportLogs() {
        warn("导出功能开发中...");
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
