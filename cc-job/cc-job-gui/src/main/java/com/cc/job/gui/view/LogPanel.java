package com.cc.job.gui.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 专业的日志监控面板 - 支持多任务组标签页
 */
public class LogPanel extends VBox {
    
    // 标签页导航栏
    private HBox tabContainer;
    private Map<Long, LogTab> logTabs;
    private Long currentTaskGroupId;
    
    // 日志显示区域容器
    private StackPane logContainer;
    
    // UI组件
    private Label statusLabel;
    private Label countLabel;
    private Button clearBtn;
    private Button exportBtn;
    private ComboBox<String> filterCombo;
    private TextField searchField;
    private String currentSearchKeyword = "";
    
    // 当前活动标签页的日志数据
    private Map<Long, LogTabData> tabDataMap;
    
    @SuppressWarnings("unused")
    private Runnable onDetach;  // 弹出回调
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * 日志标签页数据
     */
    private static class LogTabData {
        ScrollPane scrollPane;
        TextFlow textFlow;
        int logCount = 0;
        int filteredCount = 0;
        String status = "就绪";
        String statusColor = "#10B981";
        private final List<LogEntry> entries = new ArrayList<>();
        
        LogTabData() {
            textFlow = new TextFlow();
            textFlow.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-padding: 12;"
            );
            textFlow.setLineSpacing(2);
            
            scrollPane = new ScrollPane(textFlow);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-border-width: 0;"
            );
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            
            Platform.runLater(() -> {
                try {
                    javafx.scene.Node scrollBar = scrollPane.lookup(".scroll-bar:vertical");
                    if (scrollBar != null) {
                        scrollBar.setStyle(
                            "-fx-background-color: transparent; " +
                            "-fx-background-insets: 0;"
                        );
                    }
                    
                    javafx.scene.Node track = scrollPane.lookup(".scroll-bar:vertical .track");
                    if (track != null) {
                        track.setStyle(
                            "-fx-background-color: #E5E7EB; " +
                            "-fx-background-radius: 0;"
                        );
                    }
                    
                    javafx.scene.Node thumb = scrollPane.lookup(".scroll-bar:vertical .thumb");
                    if (thumb != null) {
                        thumb.setStyle(
                            "-fx-background-color: #9CA3AF; " +
                            "-fx-background-radius: 2;"
                        );
                    }
                } catch (Exception ignored) {
                }
            });
            
            render("");
        }
        
        void addEntry(LogEntry entry) {
            entries.add(entry);
            logCount = entries.size();
        }
        
        void clearEntries() {
            entries.clear();
            logCount = 0;
        }
        
        void render(String keyword) {
            String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
            boolean hasKeyword = !normalized.isEmpty();
            
            textFlow.getChildren().clear();
            
            if (!hasKeyword) {
                textFlow.getChildren().addAll(createWelcomeTexts());
            }
            
            int matches = 0;
            if (entries.isEmpty()) {
                if (hasKeyword) {
                    textFlow.getChildren().add(createStyledText("暂无日志可供搜索\n", "#9CA3AF"));
                }
            } else {
                for (LogEntry entry : entries) {
                    if (!hasKeyword || entry.matches(normalized)) {
                        textFlow.getChildren().addAll(entry.toTexts());
                        matches++;
                    }
                }
                if (hasKeyword && matches == 0) {
                    textFlow.getChildren().add(createStyledText("未找到匹配的日志记录\n", "#9CA3AF"));
                }
            }
            
            filteredCount = hasKeyword ? matches : entries.size();
            
            if (hasKeyword) {
                Platform.runLater(() -> scrollPane.setVvalue(0));
            } else {
                scrollToBottom();
            }
        }
        
        private List<Text> createWelcomeTexts() {
            List<Text> texts = new ArrayList<>();
            texts.add(createStyledText("╔════════════════════════════════════════════════════════════════╗\n", "#9CA3AF"));
            texts.add(createStyledText("║                     NodeFx 流程节点编辑器                       ║\n", "#9CA3AF"));
            texts.add(createStyledText("║                      日志监控系统 v1.0                         ║\n", "#9CA3AF"));
            texts.add(createStyledText("╚════════════════════════════════════════════════════════════════╝\n\n", "#9CA3AF"));
            texts.add(createStyledText("[i] 系统就绪，等待任务启动...\n", "#6B7280"));
            texts.add(createStyledText("[i] 所有操作日志将实时显示在此处\n\n", "#6B7280"));
            texts.add(createStyledText("─────────────────────────────────────────────────────────────────\n\n", "#9CA3AF"));
            return texts;
        }
        
        private void scrollToBottom() {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }
    }
    
    private static class LogEntry {
        final String timestamp;
        final String icon;
        final String level;
        final String levelColor;
        final String message;
        final String messageColor;
        final boolean raw;
        
        LogEntry(String timestamp,
                 String icon,
                 String level,
                 String levelColor,
                 String message,
                 String messageColor,
                 boolean raw) {
            this.timestamp = timestamp;
            this.icon = icon;
            this.level = level;
            this.levelColor = levelColor;
            this.message = message;
            this.messageColor = messageColor;
            this.raw = raw;
        }
        
        List<Text> toTexts() {
            List<Text> nodes = new ArrayList<>();
            if (raw) {
                String content = message.endsWith("\n") ? message : message + "\n";
                nodes.add(createStyledText(content, messageColor));
            } else {
                nodes.add(createStyledText("[" + timestamp + "] ", "#9CA3AF"));
                nodes.add(createStyledText(icon + " ", levelColor));
                String levelPadded = String.format("%-7s", level);
                nodes.add(createStyledText(levelPadded + " │ ", levelColor));
                nodes.add(createStyledText(message + "\n", messageColor));
            }
            return nodes;
        }
        
        boolean matches(String keywordLower) {
            if (keywordLower == null || keywordLower.isEmpty()) {
                return true;
            }
            String baseMessage = message == null ? "" : message;
            String target = raw
                ? baseMessage
                : ((timestamp != null ? timestamp : "") + " " + (level != null ? level : "") + " " + baseMessage);
            return target != null && target.toLowerCase().contains(keywordLower);
        }
    }
    
    /**
     * 日志标签页UI组件
     */
    private static class LogTab extends StackPane {
        private Long taskGroupId;
        private String taskGroupName;
        private boolean active;
        private Runnable onClickCallback;
        private Runnable onCloseCallback;
        
        public LogTab(Long taskGroupId, String taskGroupName) {
            this.taskGroupId = taskGroupId;
            this.taskGroupName = taskGroupName;
            initializeUI();
        }
        
        private void initializeUI() {
            setPadding(new Insets(8, 14, 8, 14));
            setPrefHeight(36);
            setMinHeight(36);
            setMaxHeight(36);
            setStyle(
                "-fx-background-color: #F3F4F6; " +
                "-fx-border-color: #E5E7EB; " +
                "-fx-border-width: 0 1 0 0; " +
                "-fx-cursor: hand;"
            );
            
            HBox content = new HBox(10);
            content.setAlignment(Pos.CENTER_LEFT);
            
            // 任务组名称
            Label nameLabel = new Label(taskGroupName);
            nameLabel.setStyle(
                "-fx-font-size: 13px; " +
                "-fx-text-fill: #4B5563; " +
                "-fx-font-weight: 500;"
            );
            
            // 关闭按钮 - 使用更现代的样式
            Label closeIcon = new Label("×");
            closeIcon.setStyle(
                "-fx-text-fill: #9CA3AF; " +
                "-fx-font-size: 18px; " +
                "-fx-font-weight: 300; " +
                "-fx-padding: 0 0 2 0; " +
                "-fx-cursor: hand;"
            );
            
            StackPane closeBtn = new StackPane(closeIcon);
            closeBtn.setPrefSize(20, 20);
            closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            
            closeBtn.setOnMouseEntered(e -> {
                closeBtn.setStyle(
                    "-fx-background-color: #EF4444; " +
                    "-fx-background-radius: 3; " +
                    "-fx-cursor: hand;"
                );
                closeIcon.setStyle(
                    "-fx-text-fill: #FFFFFF; " +
                    "-fx-font-size: 18px; " +
                    "-fx-font-weight: 300; " +
                    "-fx-padding: 0 0 2 0;"
                );
            });
            
            closeBtn.setOnMouseExited(e -> {
                closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                closeIcon.setStyle(
                    "-fx-text-fill: #9CA3AF; " +
                    "-fx-font-size: 18px; " +
                    "-fx-font-weight: 300; " +
                    "-fx-padding: 0 0 2 0;"
                );
            });
            
            closeBtn.setOnMouseClicked(e -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
                e.consume();
            });
            
            content.getChildren().addAll(nameLabel, closeBtn);
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
                    setStyle(
                        "-fx-background-color: #E5E7EB; " +
                        "-fx-border-color: #D1D5DB; " +
                        "-fx-border-width: 0 1 0 0; " +
                        "-fx-cursor: hand;"
                    );
                }
            });
            
            setOnMouseExited(e -> updateStyle());
        }
        
        public void setActive(boolean active) {
            this.active = active;
            updateStyle();
        }
        
        private void updateStyle() {
            if (active) {
                setStyle(
                    "-fx-background-color: #FFFFFF; " +
                    "-fx-border-color: #6366F1; " +
                    "-fx-border-width: 0 1 0 0; " +
                    "-fx-border-insets: 0 0 0 0; " +
                    "-fx-effect: dropshadow(gaussian, rgba(99, 102, 241, 0.2), 0, 0, 0, 1); " +
                    "-fx-cursor: hand;"
                );
                // 更新标签文字颜色
                if (getChildren().size() > 0 && getChildren().get(0) instanceof HBox) {
                    HBox content = (HBox) getChildren().get(0);
                    if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof Label) {
                        ((Label) content.getChildren().get(0)).setStyle(
                            "-fx-font-size: 13px; " +
                            "-fx-text-fill: #6366F1; " +
                            "-fx-font-weight: 600;"
                        );
                    }
                }
            } else {
                setStyle(
                    "-fx-background-color: #F3F4F6; " +
                    "-fx-border-color: #E5E7EB; " +
                    "-fx-border-width: 0 1 0 0; " +
                    "-fx-effect: null; " +
                    "-fx-cursor: hand;"
                );
                // 更新标签文字颜色
                if (getChildren().size() > 0 && getChildren().get(0) instanceof HBox) {
                    HBox content = (HBox) getChildren().get(0);
                    if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof Label) {
                        ((Label) content.getChildren().get(0)).setStyle(
                            "-fx-font-size: 13px; " +
                            "-fx-text-fill: #4B5563; " +
                            "-fx-font-weight: 500;"
                        );
                    }
                }
            }
        }
        
        @SuppressWarnings("unused")
        public Long getTaskGroupId() {
            return taskGroupId;
        }
        
        @SuppressWarnings("unused")
        public String getTaskGroupName() {
            return taskGroupName;
        }
        
        public void setOnClick(Runnable callback) {
            this.onClickCallback = callback;
        }
        
        public void setOnClose(Runnable callback) {
            this.onCloseCallback = callback;
        }
    }
    
    public LogPanel() {
        logTabs = new HashMap<>();
        tabDataMap = new HashMap<>();
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1 0 0 0;"
        );
        setPrefHeight(280);
        setMinHeight(280);
        setPadding(new Insets(0));
        
        // 标签页导航栏
        HBox tabBar = createTabBar();
        
        // 标题栏
        HBox titleBar = createTitleBar();
        
        // 日志显示区域容器
        logContainer = new StackPane();
        logContainer.setStyle("-fx-background-color: #FFFFFF;");
        VBox.setVgrow(logContainer, Priority.ALWAYS);
        
        // 创建默认日志区域（系统日志，不关联任何任务组）
        LogTabData defaultTab = new LogTabData();
        tabDataMap.put(null, defaultTab);
        logContainer.getChildren().add(defaultTab.scrollPane);
        
        // 底部状态栏
        HBox statusBar = createStatusBar();
        
        getChildren().addAll(tabBar, titleBar, logContainer, statusBar);
        
        // 默认显示系统日志
        switchToTaskGroup(null);
    }
    
    /**
     * 创建标签页导航栏
     */
    private HBox createTabBar() {
        HBox tabBar = new HBox(0);
        tabBar.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 0 0 1 0; " +
            "-fx-padding: 0;"
        );
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPrefHeight(36);
        tabBar.setMinHeight(36);
        tabBar.setMaxHeight(36);
        
        tabContainer = new HBox(0);
        tabContainer.setAlignment(Pos.CENTER_LEFT);
        
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
        
        tabBar.getChildren().add(scrollPane);
        
        return tabBar;
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(12);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(12, 16, 12, 16));
        titleBar.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 0 0 1 0;"
        );
        titleBar.setPrefHeight(48);
        titleBar.setMinHeight(48);
        titleBar.setMaxHeight(48);
        
        // 标题 - 扁平化设计
        Label titleLabel = new Label("日志监控");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#111827"));
        titleLabel.setStyle("-fx-font-weight: 600;");
        
        // 状态指示器
        HBox statusIndicator = new HBox(6);
        statusIndicator.setAlignment(Pos.CENTER_LEFT);
        statusIndicator.setPadding(new Insets(0, 0, 0, 0));
        
        javafx.scene.shape.Circle indicator = new javafx.scene.shape.Circle(5);
        indicator.setFill(Color.web("#10B981"));
        indicator.setEffect(new javafx.scene.effect.Glow(0.8));
        
        statusLabel = new Label("就绪");
        statusLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        statusLabel.setTextFill(Color.web("#059669"));
        statusLabel.setStyle("-fx-font-weight: 500;");
        
        statusIndicator.getChildren().addAll(indicator, statusLabel);
        
        // 分隔线
        Region separator1 = createInlineSeparator();
        
        // 过滤器
        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("全部", "信息", "警告", "错误", "调试");
        filterCombo.setValue("全部");
        filterCombo.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-pref-width: 90; " +
            "-fx-pref-height: 28; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 0 8 0 8;"
        );
        
        // 搜索框
        searchField = new TextField();
        searchField.setPromptText("搜索日志...");
        searchField.setPrefWidth(200);
        searchField.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 4 10 4 28; " +
            "-fx-background-image: url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"%236B7280\" viewBox=\"0 0 24 24\"><path d=\"M10 2a8 8 0 105.293 14.293l4.147 4.147 1.414-1.414-4.147-4.147A8 8 0 0010 2zm0 2a6 6 0 110 12 6 6 0 010-12z\"/></svg>'); " +
            "-fx-background-repeat: no-repeat; " +
            "-fx-background-position: 8px center;"
        );
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
        
        // 空白区域
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 日志计数
        countLabel = new Label("0 条日志");
        countLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        countLabel.setTextFill(Color.web("#6B7280"));
        countLabel.setStyle("-fx-font-weight: 500;");
        
        // 分隔线
        Region separator2 = createInlineSeparator();
        
        // 清空按钮 - 扁平化设计，靠右排列
        clearBtn = createToolButton("清空", this::clearLogs);
        
        // 导出按钮 - 扁平化设计，靠右排列
        exportBtn = createToolButton("导出", this::exportLogs);
        
        titleBar.getChildren().addAll(
            titleLabel,
            statusIndicator,
            separator1,
            filterCombo,
            searchField,
            spacer,
            countLabel,
            separator2,
            clearBtn,
            exportBtn
        );
        
        return titleBar;
    }

    private Region createInlineSeparator() {
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setMinWidth(1);
        separator.setMaxWidth(1);
        separator.setPrefHeight(24);
        separator.setMinHeight(18);
        separator.setStyle("-fx-background-color: #E2E8F0;");
        return separator;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(10, 16, 10, 16));
        statusBar.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1 0 0 0;"
        );
        statusBar.setPrefHeight(36);
        statusBar.setMinHeight(36);
        statusBar.setMaxHeight(36);
        
        Label infoLabel = new Label("提示: 启动任务后将显示实时日志信息");
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        infoLabel.setTextFill(Color.web("#6B7280"));
        infoLabel.setStyle("-fx-font-weight: 400;");
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label("最后更新: 从未");
        timeLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        timeLabel.setTextFill(Color.web("#9CA3AF"));
        timeLabel.setStyle("-fx-font-weight: 400;");
        
        statusBar.getChildren().addAll(infoLabel, spacer, timeLabel);
        
        return statusBar;
    }
    
    private Button createToolButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6 14 6 14; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-text-fill: #111827; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6 14 6 14; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #9CA3AF; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 2, 0, 0, 1); " +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6 14 6 14; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-effect: null; " +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnAction(e -> action.run());
        
        return btn;
    }
    
    /**
     * 添加或切换到任务组日志标签页
     */
    public void addOrSwitchToTaskGroup(Long taskGroupId, String taskGroupName) {
        if (taskGroupId == null) {
            return;
        }
        
        Platform.runLater(() -> {
            // 如果标签页不存在，创建新的
            if (!logTabs.containsKey(taskGroupId)) {
                LogTab tab = new LogTab(taskGroupId, taskGroupName);
                tab.setOnClick(() -> switchToTaskGroup(taskGroupId));
                tab.setOnClose(() -> removeTaskGroup(taskGroupId));
                
                logTabs.put(taskGroupId, tab);
                tabContainer.getChildren().add(tab);
                
                // 创建对应的日志数据
                LogTabData tabData = new LogTabData();
                tabDataMap.put(taskGroupId, tabData);
            }
            
            // 切换到该标签页
            switchToTaskGroup(taskGroupId);
        });
    }
    
    /**
     * 切换到指定任务组的日志标签页
     */
    private void switchToTaskGroup(Long taskGroupId) {
        Platform.runLater(() -> {
            // 更新所有标签的激活状态
            logTabs.forEach((id, tab) -> {
                tab.setActive(id.equals(taskGroupId));
            });
            
            currentTaskGroupId = taskGroupId;
            
            // 切换显示的日志区域
            LogTabData tabData = tabDataMap.get(taskGroupId);
            if (tabData != null) {
                logContainer.getChildren().clear();
                logContainer.getChildren().add(tabData.scrollPane);
                tabData.render(currentSearchKeyword);
                
                // 更新状态栏
                updateStatusBar(tabData);
            }
        });
    }
    
    /**
     * 移除任务组日志标签页
     */
    private void removeTaskGroup(Long taskGroupId) {
        Platform.runLater(() -> {
            LogTab tab = logTabs.remove(taskGroupId);
            if (tab != null) {
                tabContainer.getChildren().remove(tab);
                
                // 移除对应的日志数据
                tabDataMap.remove(taskGroupId);
                
                // 如果删除的是当前标签，切换到第一个标签
                if (taskGroupId.equals(currentTaskGroupId)) {
                    if (!logTabs.isEmpty()) {
                        Long firstTabId = logTabs.keySet().iterator().next();
                        switchToTaskGroup(firstTabId);
                    } else {
                        // 切换到系统日志（null）
                        switchToTaskGroup(null);
                    }
                }
            }
        });
    }
    
    /**
     * 更新状态栏
     */
    private void updateStatusBar(LogTabData tabData) {
        if (tabData != null) {
            if (currentSearchKeyword != null && !currentSearchKeyword.isBlank()) {
                countLabel.setText("匹配 " + tabData.filteredCount + " / 共 " + tabData.logCount + " 条");
            } else {
                countLabel.setText(tabData.logCount + " 条日志");
            }
            statusLabel.setText(tabData.status);
            statusLabel.setTextFill(Color.web(tabData.statusColor));
        }
    }
    
    private void applySearchFilter(String keyword) {
        currentSearchKeyword = keyword == null ? "" : keyword.trim();
        LogTabData tabData = getCurrentTabData();
        if (tabData != null) {
            tabData.render(currentSearchKeyword);
            updateStatusBar(tabData);
        }
    }
    
    /**
     * 获取当前任务组的日志数据
     */
    private LogTabData getCurrentTabData() {
        return tabDataMap.get(currentTaskGroupId);
    }
    
    private boolean isCurrentTab(Long taskGroupId) {
        return (taskGroupId == null && currentTaskGroupId == null) ||
               (taskGroupId != null && taskGroupId.equals(currentTaskGroupId));
    }
    
    /**
     * 获取指定任务组的日志数据（如果不存在则使用当前）
     */
    private LogTabData getTabData(Long taskGroupId) {
        LogTabData tabData = tabDataMap.get(taskGroupId);
        if (tabData == null) {
            tabData = getCurrentTabData();
        }
        return tabData;
    }
    
    /**
     * 添加日志 - INFO级别（针对特定任务组）
     */
    public void info(Long taskGroupId, String message) {
        appendLog(taskGroupId, "INFO", message);
    }
    
    /**
     * 添加日志 - INFO级别（添加到当前标签页）
     */
    public void info(String message) {
        appendLog(null, "INFO", message);
    }
    
    /**
     * 添加日志 - WARN级别
     */
    public void warn(String message) {
        appendLog(null, "WARN", message);
    }
    
    /**
     * 添加日志 - WARN级别（针对特定任务组）
     */
    public void warn(Long taskGroupId, String message) {
        appendLog(taskGroupId, "WARN", message);
    }
    
    /**
     * 添加日志 - ERROR级别
     */
    public void error(String message) {
        appendLog(null, "ERROR", message);
    }
    
    /**
     * 添加日志 - ERROR级别（针对特定任务组）
     */
    public void error(Long taskGroupId, String message) {
        appendLog(taskGroupId, "ERROR", message);
    }
    
    /**
     * 添加日志 - DEBUG级别
     */
    public void debug(String message) {
        appendLog(null, "DEBUG", message);
    }
    
    /**
     * 添加日志 - SUCCESS级别
     */
    public void success(String message) {
        appendLog(null, "SUCCESS", message);
    }
    
    /**
     * 添加日志 - SUCCESS级别（针对特定任务组）
     */
    public void success(Long taskGroupId, String message) {
        appendLog(taskGroupId, "SUCCESS", message);
    }
    
    /**
     * 直接追加文本（不添加时间戳和格式）
     * 用于显示原始日志内容（针对特定任务组）
     * 智能识别错误信息并应用红色样式
     */
    public void appendText(Long taskGroupId, String text) {
        Platform.runLater(() -> {
            LogTabData tabData = getTabData(taskGroupId);
            if (tabData == null || text == null) {
                return;
            }
            
            String lowerText = text.toLowerCase();
            boolean isError = lowerText.contains("错误") ||
                              lowerText.contains("error") ||
                              lowerText.contains("失败") ||
                              lowerText.contains("fail") ||
                              lowerText.contains("exception") ||
                              lowerText.contains("异常") ||
                              lowerText.contains("执行结果:失败") ||
                              lowerText.contains("任务执行失败") ||
                              lowerText.contains("任务触发失败");
            boolean isWarn = lowerText.contains("警告") ||
                             lowerText.contains("warn") ||
                             lowerText.contains("⚠");
            boolean isSuccess = lowerText.contains("成功") ||
                                lowerText.contains("success") ||
                                lowerText.contains("执行结果:成功") ||
                                lowerText.contains("任务执行成功");
            
            String color;
            if (isError) {
                color = "#DC2626";
            } else if (isWarn) {
                color = "#D97706";
            } else if (isSuccess) {
                color = "#059669";
            } else {
                color = "#374151";
            }
            
            LogEntry entry = new LogEntry(null, null, "TEXT", color, text, color, true);
            tabData.addEntry(entry);
            
            tabData.status = "运行中";
            tabData.statusColor = "#10B981";
            
            if (isCurrentTab(taskGroupId)) {
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
            }
        });
    }
    
    /**
     * 创建带样式的文本节点
     */
    private static Text createStyledText(String content, String color) {
        Text text = new Text(content);
        text.setFill(Color.web(color));
        text.setFont(Font.font("Consolas", FontWeight.NORMAL, 13));
        return text;
    }
    
    /**
     * 直接追加文本（不添加时间戳和格式）
     * 用于显示原始日志内容（添加到当前标签页）
     */
    public void appendText(String text) {
        appendText(null, text);
    }
    
    private void appendLog(Long taskGroupId, String level, String message) {
        Platform.runLater(() -> {
            LogTabData tabData = getTabData(taskGroupId);
            if (tabData == null) {
                return;
            }
            
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            
            // 根据级别选择图标和颜色 - 扁平化设计
            String icon;
            String levelColor;
            String messageColor;
            
            switch (level) {
                case "INFO":
                    icon = "[i]";
                    levelColor = "#2563EB"; // 蓝色
                    messageColor = "#1E40AF"; // 深蓝色
                    break;
                case "WARN":
                    icon = "[!]";
                    levelColor = "#D97706"; // 黄色
                    messageColor = "#B45309"; // 深黄色
                    break;
                case "ERROR":
                    icon = "[×]";
                    levelColor = "#DC2626"; // 红色
                    messageColor = "#B91C1C"; // 深红色
                    break;
                case "DEBUG":
                    icon = "[?]";
                    levelColor = "#7C3AED"; // 紫色
                    messageColor = "#6D28D9"; // 深紫色
                    break;
                case "SUCCESS":
                    icon = "[✓]";
                    levelColor = "#059669"; // 绿色
                    messageColor = "#047857"; // 深绿色
                    break;
                default:
                    icon = "[-]";
                    levelColor = "#6B7280"; // 灰色
                    messageColor = "#374151"; // 深灰色
                    break;
            }
            
            LogEntry entry = new LogEntry(timestamp, icon, level, levelColor, message, messageColor, false);
            tabData.addEntry(entry);
            
            // 更新状态
            tabData.status = "运行中";
            tabData.statusColor = "#10B981";
            
            // 如果当前显示的是这个标签页，更新状态栏
            if (isCurrentTab(taskGroupId)) {
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
            }
        });
    }
    
    /**
     * 清空当前标签页的日志
     */
    public void clearLogs() {
        Platform.runLater(() -> {
            LogTabData tabData = getCurrentTabData();
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
    
    /**
     * 导出日志
     */
    public void exportLogs() {
        // TODO: 实现导出功能
        warn("导出功能开发中...");
    }
    
    /**
     * 更新状态
     */
    public void updateStatus(String status, String colorHex) {
        Platform.runLater(() -> {
            LogTabData tabData = getCurrentTabData();
            if (tabData != null) {
                tabData.status = status;
                tabData.statusColor = colorHex;
                updateStatusBar(tabData);
            }
        });
    }
    
    /**
     * 获取当前标签页的日志内容
     */
    public String getLogContent() {
        LogTabData tabData = getCurrentTabData();
        if (tabData != null) {
            StringBuilder sb = new StringBuilder();
            for (LogEntry entry : tabData.entries) {
                if (entry.raw) {
                    sb.append(entry.message);
                    if (!entry.message.endsWith("\n")) {
                        sb.append("\n");
                    }
                } else {
                    sb.append("[")
                      .append(entry.timestamp)
                      .append("] ")
                      .append(entry.icon)
                      .append(" ")
                      .append(String.format("%-7s", entry.level))
                      .append(" │ ")
                      .append(entry.message)
                      .append("\n");
                }
            }
            return sb.toString();
        }
        return "";
    }
    
    /**
     * 设置弹出回调
     */
    public void setOnDetach(Runnable callback) {
        this.onDetach = callback;
    }
}
