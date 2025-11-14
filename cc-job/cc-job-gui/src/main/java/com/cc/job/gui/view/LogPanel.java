package com.cc.job.gui.view;

import com.cc.job.gui.util.StyleUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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
    private Label countLabel;
    private Button clearBtn;
    private Button exportBtn;
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
        VBox logList;
        int logCount = 0;
        int filteredCount = 0;
        String status = "就绪";
        String statusColor = "#10B981";
        private final List<LogEntry> entries = new ArrayList<>();
        
        LogTabData() {
            logList = new VBox();
            logList.setSpacing(0);
            logList.setPadding(new Insets(0));
            logList.setFillWidth(true);
            logList.setStyle("-fx-background-color: transparent;");
            
            scrollPane = new ScrollPane(logList);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle(
                "-fx-background-color: transparent; " +
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
                            "-fx-background-color: rgba(226, 232, 240, 0.55); " +
                            "-fx-background-radius: 4;"
                        );
                    }
                    
                    javafx.scene.Node thumb = scrollPane.lookup(".scroll-bar:vertical .thumb");
                    if (thumb != null) {
                        thumb.setStyle(
                            "-fx-background-color: rgba(148, 163, 184, 0.7); " +
                            "-fx-background-radius: 4;"
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

            logList.getChildren().clear();

            // 【过滤】只显示任务组运行日志（isExecutionLog == true）
            List<LogEntry> executionLogs = new ArrayList<>();
            for (LogEntry entry : entries) {
                if (entry.isExecutionLog) {
                    executionLogs.add(entry);
                }
            }

            if (executionLogs.isEmpty()) {
                filteredCount = 0;
                if (hasKeyword) {
                    logList.getChildren().add(buildEmptyState("暂无日志可供搜索", "调整关键字或过滤条件后重试。"));
                    Platform.runLater(() -> scrollPane.setVvalue(0));
                } else {
                    logList.getChildren().add(buildEmptyState("日志暂未产生", "启动任务组或执行操作后，这里会实时展示运行信息。"));
                    scrollToBottom();
                }
                return;
            }

            int matches = 0;
            int rowIndex = 0;
            for (LogEntry entry : executionLogs) {
                if (!hasKeyword || entry.matches(normalized)) {
                    logList.getChildren().add(buildLogRow(entry, rowIndex++));
                    matches++;
                }
            }

            if (hasKeyword) {
                filteredCount = matches;
                if (matches == 0) {
                    logList.getChildren().add(buildEmptyState("未找到匹配的日志记录", "尝试缩短搜索词或更改日志级别筛选条件。"));
                }
                Platform.runLater(() -> scrollPane.setVvalue(0));
            } else {
                filteredCount = executionLogs.size();
                scrollToBottom();
            }
        }
        
        private Node buildLogRow(LogEntry entry, int rowIndex) {
            boolean isRaw = entry.raw;
            // 移除背景颜色设置
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 20, 8, 20));
            row.setMaxWidth(Double.MAX_VALUE);
            row.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent transparent rgba(226,232,240,0.75) transparent; " +
                "-fx-border-width: 0 0 1 0;"
            );

            Label timestampLabel = new Label(entry.timestamp != null ? entry.timestamp : "--:--:--");
            timestampLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
            timestampLabel.setTextFill(Color.web(StyleUtil.GRAY_500));
            timestampLabel.setMinWidth(140);
            timestampLabel.setPrefWidth(140);
            timestampLabel.setMaxWidth(140);

            String levelName = isRaw ? "输出" : resolveLevelName(entry.level);
            String swatchColor = entry.levelColor != null ? entry.levelColor : StyleUtil.GRAY_500;
            if (isRaw) {
                swatchColor = StyleUtil.GRAY_500;
            }

            Label levelLabel = new Label(levelName);
            levelLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
            levelLabel.setTextFill(Color.web("#FFFFFF"));
            levelLabel.setAlignment(Pos.CENTER);
            levelLabel.setMinWidth(64);
            levelLabel.setPrefWidth(64);
            levelLabel.setMaxWidth(64);
            levelLabel.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 3 0;",
                swatchColor
            ));

            Label messageLabel = new Label(entry.message != null ? entry.message : "");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(Double.MAX_VALUE);
            
            // 设置消息颜色
            // "输出"类型（raw=true）：默认黑色，错误显示红色，警告显示黄色
            // 其他类型：使用 entry.messageColor 或默认颜色
            String finalMessageColor;
            if (isRaw) {
                // "输出"类型：如果 messageColor 已设置（错误或警告），使用它；否则使用黑色
                if (entry.messageColor != null && 
                    (entry.messageColor.equals("#DC2626") || entry.messageColor.equals("#D97706"))) {
                    // 错误（红色）或警告（黄色）已设置，使用它
                    finalMessageColor = entry.messageColor;
                } else {
                    // 默认黑色
                    finalMessageColor = "#000000";
                }
            } else {
                // 非"输出"类型：使用 entry.messageColor 或默认颜色
                finalMessageColor = entry.messageColor != null ? entry.messageColor : StyleUtil.GRAY_700;
            }
            
            messageLabel.setTextFill(Color.web(finalMessageColor));
            messageLabel.setFont(isRaw
                ? Font.font("Consolas", FontWeight.NORMAL, 12)
                : Font.font("System", FontWeight.NORMAL, 13));
            HBox.setHgrow(messageLabel, Priority.ALWAYS);

            row.getChildren().addAll(timestampLabel, levelLabel, messageLabel);
            return row;
        }

        private Node buildEmptyState(String title, String description) {
            VBox container = new VBox(6);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(24, 28, 24, 28));
            container.setMaxWidth(Double.MAX_VALUE);
            String radius = StyleUtil.RADIUS_LG;
            container.setStyle(String.format(
                "-fx-background-color: #FFFFFF; " +
                "-fx-border-color: rgba(226,232,240,0.9); " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: %1$s; " +
                "-fx-background-radius: %1$s;",
                radius
            ));

            Label titleLabel = new Label(title);
            titleLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
            titleLabel.setTextFill(Color.web(StyleUtil.GRAY_700));

            Label descLabel = new Label(description);
            descLabel.setWrapText(true);
            descLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
            descLabel.setTextFill(Color.web(StyleUtil.GRAY_500));

            container.getChildren().addAll(titleLabel, descLabel);

            StackPane wrapper = new StackPane(container);
            wrapper.setPadding(new Insets(20, 20, 20, 20));
            return wrapper;
        }
        
        private void scrollToBottom() {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }

        private String resolveLevelName(String level) {
            if (level == null) {
                return "日志";
            }
            return switch (level) {
                case "INFO" -> "信息";
                case "WARN" -> "警告";
                case "ERROR" -> "错误";
                case "SUCCESS" -> "成功";
                case "DEBUG" -> "调试";
                default -> level;
            };
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
        final boolean isExecutionLog; // 是否是任务组运行日志
        
        LogEntry(String timestamp,
                 String icon,
                 String level,
                 String levelColor,
                 String message,
                 String messageColor,
                 boolean raw,
                 boolean isExecutionLog) {
            this.timestamp = timestamp;
            this.icon = icon;
            this.level = level;
            this.levelColor = levelColor;
            this.message = message;
            this.messageColor = messageColor;
            this.raw = raw;
            this.isExecutionLog = isExecutionLog;
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
        private Label nameLabel;
        
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
        
        public LogTab(Long taskGroupId, String taskGroupName) {
            this.taskGroupId = taskGroupId;
            this.taskGroupName = taskGroupName;
            initializeUI();
        }
        
        private void initializeUI() {
            // 进一步减小 padding，使标签页更窄
            setPadding(new Insets(6, 8, 6, 8));
            setPrefHeight(38);
            setMinHeight(38);
            setMaxHeight(38);
            setStyle(NORMAL_STYLE);
            
            HBox content = new HBox(6);
            content.setAlignment(Pos.CENTER_LEFT);
            
            // 任务组名称 - 进一步减小宽度
            nameLabel = new Label(taskGroupName);
            nameLabel.setStyle(
                "-fx-font-size: 12px; " +
                "-fx-text-fill: " + StyleUtil.GRAY_600 + "; " +
                "-fx-font-weight: 600;"
            );
            nameLabel.setPrefWidth(100);
            nameLabel.setMaxWidth(100);
            nameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            Tooltip.install(nameLabel, new Tooltip(taskGroupName));
            
            // 关闭按钮 - 使用更现代的样式
            Label closeIcon = new Label("×");
            closeIcon.setStyle(
                "-fx-text-fill: " + StyleUtil.GRAY_400 + "; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 400; " +
                "-fx-padding: 0; " +
                "-fx-cursor: hand;"
            );
            
            StackPane closeBtn = new StackPane(closeIcon);
            closeBtn.setPrefSize(20, 20);
            closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
            
            closeBtn.setOnMouseEntered(e -> {
                closeBtn.setStyle(
                    "-fx-background-color: rgba(239, 68, 68, 0.12); " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;"
                );
                closeIcon.setStyle(
                    "-fx-text-fill: " + StyleUtil.ERROR + "; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: 400; " +
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
            if (active) {
                setStyle(ACTIVE_STYLE);
                nameLabel.setStyle(
                    "-fx-font-size: 12.5px; " +
                    "-fx-text-fill: " + StyleUtil.PRIMARY_DARK + "; " +
                    "-fx-font-weight: 700;"
                );
            } else {
                setStyle(NORMAL_STYLE);
                nameLabel.setStyle(
                    "-fx-font-size: 12.5px; " +
                    "-fx-text-fill: " + StyleUtil.GRAY_600 + "; " +
                    "-fx-font-weight: 600;"
                );
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
        // 确保 LogPanel 本身没有任何 padding 和 margin
        setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 0; " +
            "-fx-background-insets: 0;"
        );
        if (!getStyleClass().contains("log-panel")) {
            getStyleClass().add("log-panel");
        }
        setPrefHeight(280);
        setMinHeight(280);
        setSpacing(0);
        setPadding(new Insets(0));
        
        // 标签页导航栏
        HBox tabBar = createTabBar();
        
        // 标题栏
        HBox titleBar = createTitleBar();
        
        // 日志显示区域容器
        logContainer = new StackPane();
        logContainer.setPadding(new Insets(12, 12, 12, 12));
        logContainer.setStyle("-fx-background-color: transparent;");
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
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 0; " +
            "-fx-background-insets: 0;"
        );
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPrefHeight(36);
        tabBar.setMinHeight(36);
        tabBar.setMaxHeight(36);
        
        tabContainer = new HBox(0);
        tabContainer.setAlignment(Pos.CENTER_LEFT);
        tabContainer.setPadding(new Insets(0));
        HBox.setMargin(tabContainer, new Insets(0)); // 确保没有 margin
        
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
            // 确保 ScrollPane 的内容区域没有 insets
            javafx.scene.Node viewport = scrollPane.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-insets: 0; -fx-padding: 0;");
            }
        });
        
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        HBox.setMargin(scrollPane, new Insets(0)); // 确保 ScrollPane 没有 margin
        
        tabBar.getChildren().add(scrollPane);
        
        return tabBar;
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(12);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle(
            "-fx-border-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 14 20;"
        );
        titleBar.setPrefHeight(56);
        titleBar.setMinHeight(56);
        titleBar.setMaxHeight(56);
        
        // 标题 - 减小字体大小
        Label titleLabel = new Label("日志监控");
        titleLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        titleLabel.setTextFill(Color.web(StyleUtil.GRAY_900));
        
        // 搜索框
        searchField = new TextField();
        searchField.setPromptText("搜索日志...");
        searchField.setPrefWidth(200);
        searchField.setStyle(
            "-fx-background-color: rgba(255,255,255,0.95); " +
            "-fx-text-fill: " + StyleUtil.GRAY_700 + "; " +
            "-fx-font-size: 12px; " +
            "-fx-border-color: rgba(148,163,184,0.55); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 6 12 6 32; " +
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
        countLabel.setTextFill(Color.web(StyleUtil.GRAY_500));
        countLabel.setStyle("-fx-font-weight: 600;");
        
        // 分隔线
        Region separator2 = createInlineSeparator();
        
        // 滚动到顶部按钮
        Button scrollToTopBtn = createToolButton("↑", () -> {
            LogTabData tabData = getCurrentTabData();
            if (tabData != null && tabData.scrollPane != null) {
                Platform.runLater(() -> tabData.scrollPane.setVvalue(0));
            }
        });
        scrollToTopBtn.setTooltip(new Tooltip("滚动到顶部"));
        
        // 清空按钮 - 扁平化设计，靠右排列
        clearBtn = createToolButton("清空", this::clearLogs);
        
        // 导出按钮 - 扁平化设计，靠右排列
        exportBtn = createToolButton("导出", this::exportLogs);
        
        titleBar.getChildren().addAll(
            titleLabel,
            searchField,
            spacer,
            countLabel,
            separator2,
            scrollToTopBtn,
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
        statusBar.setPrefHeight(36);
        statusBar.setMinHeight(36);
        statusBar.setMaxHeight(36);
        
        Label infoLabel = new Label("提示: 启动任务后将显示实时日志信息");
        infoLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        infoLabel.setTextFill(Color.web(StyleUtil.GRAY_500));
        infoLabel.setStyle("-fx-font-weight: 400;");
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label("最后更新: 从未");
        timeLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        timeLabel.setTextFill(Color.web(StyleUtil.GRAY_400));
        timeLabel.setStyle("-fx-font-weight: 500;");
        
        statusBar.getChildren().addAll(infoLabel, spacer, timeLabel);
        
        return statusBar;
    }
    
    private Button createToolButton(String text, Runnable action) {
        Button btn = new Button(text);
        String normal = 
            "-fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.96), rgba(241,245,249,0.96)); " +
            "-fx-text-fill: " + StyleUtil.GRAY_600 + "; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: rgba(148,163,184,0.6); " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.99), rgba(226,232,240,0.99)); " +
            "-fx-text-fill: " + StyleUtil.GRAY_800 + "; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: " + StyleUtil.PRIMARY_LIGHT + "; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.18), 10, 0, 0, 2); " +
            "-fx-cursor: hand;";
        StyleUtil.applyButtonHover(btn, normal, hover);
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
     * 解析HTML实体，将 &lt; &gt; &amp; 等转换为实际字符
     * @param text 原始文本
     * @return 解析后的文本
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) {
            return null;
        }
        // 先解析HTML实体，再处理换行标签
        // 注意：需要先解析 &amp; 避免将 &amp;lt; 误解析
        String result = text;
        // 先处理 &amp;，避免后续替换时出现问题
        result = result.replace("&amp;", "&");
        // 然后解析其他HTML实体
        result = result.replace("&lt;", "<")
                       .replace("&gt;", ">")
                       .replace("&quot;", "\"")
                       .replace("&apos;", "'")
                       .replace("&nbsp;", " ");
        // 最后处理换行标签（包括已解析的 <br> 和未解析的 &lt;br&gt;）
        result = result.replace("&lt;br&gt;", "\n")
                       .replace("&lt;br/&gt;", "\n")
                       .replace("&lt;br /&gt;", "\n")
                       .replace("<br>", "\n")
                       .replace("<br/>", "\n")
                       .replace("<br />", "\n");
        return result;
    }
    
    /**
     * 直接追加文本（不添加时间戳和格式）
     * 用于显示原始日志内容（针对特定任务组）
     * "输出"类型的日志只有黑色字体，只有当错误信息时才显示红色，警告信息显示黄色
     */
    public void appendText(Long taskGroupId, String text) {
        Platform.runLater(() -> {
            LogTabData tabData = getTabData(taskGroupId);
            if (tabData == null || text == null) {
                return;
            }
            
            // 解析HTML实体（如 &lt;br&gt; 转换为换行符）
            String decodedText = decodeHtmlEntities(text);
            
            String lowerText = decodedText.toLowerCase();
            
            // 先检测成功信息（优先级最高，避免误判）
            // 成功的关键词：执行结果:成功、任务执行成功、返回结果: SUCCESS、SUCCESS等
            boolean isSuccess = lowerText.contains("执行结果:成功") ||
                                lowerText.contains("任务执行成功") ||
                                lowerText.contains("返回结果: success") ||
                                lowerText.contains("返回结果:success") ||
                                (lowerText.contains("成功") && 
                                 (lowerText.contains("执行结果") || lowerText.contains("任务执行")) &&
                                 !lowerText.contains("失败"));
            
            // 检测错误信息（需要更精确的匹配，避免误判）
            // 只有在不是成功的情况下才检测错误
            boolean isError = false;
            if (!isSuccess) {
                // 精确匹配错误关键词，避免误判
                // 例如："失败重试次数"不应该被识别为错误
                isError = lowerText.contains("执行结果:失败") ||
                          lowerText.contains("任务执行失败") ||
                          lowerText.contains("任务触发失败") ||
                          (lowerText.contains("错误") && 
                           !lowerText.contains("成功") && 
                           !lowerText.contains("失败重试")) ||
                          (lowerText.contains("error") && 
                           !lowerText.contains("success") && 
                           !lowerText.contains("fail retry")) ||
                          (lowerText.contains("失败") && 
                           !lowerText.contains("成功") && 
                           !lowerText.contains("失败重试") &&
                           !lowerText.contains("executorfailretrycount")) ||
                          (lowerText.contains("fail") && 
                           !lowerText.contains("success") && 
                           !lowerText.contains("fail retry") &&
                           !lowerText.contains("executorfailretrycount")) ||
                          (lowerText.contains("exception") && !lowerText.contains("success")) ||
                          (lowerText.contains("异常") && !lowerText.contains("成功"));
            }
            
            // 检测警告信息
            boolean isWarn = lowerText.contains("警告") ||
                             lowerText.contains("warn") ||
                             lowerText.contains("⚠");
            
            // "输出"类型的日志默认黑色，只有错误显示红色，警告显示黄色
            String messageColor;
            String levelColor;
            if (isError) {
                // 错误信息：红色
                messageColor = "#DC2626";
                levelColor = "#DC2626";
            } else if (isWarn) {
                // 警告信息：黄色
                messageColor = "#D97706";
                levelColor = "#D97706";
            } else {
                // 默认：黑色（包括成功信息）
                messageColor = "#000000";
                levelColor = StyleUtil.GRAY_500;
            }
            
            // 标记为任务组运行日志（raw = true 表示"输出"类型）
            // 使用解析后的文本（HTML实体已转换）
            LogEntry entry = new LogEntry(null, null, "TEXT", levelColor, decodedText, messageColor, true, true);
            tabData.addEntry(entry);
            
            tabData.status = "运行中";
            tabData.statusColor = "#10B981";
            
            // 检测特定日志，触发通知提示框
            checkAndShowNotification(decodedText, lowerText, isError, isWarn, isSuccess);
            
            if (isCurrentTab(taskGroupId)) {
                tabData.render(currentSearchKeyword);
                updateStatusBar(tabData);
            }
        });
    }
    
    /**
     * 检测特定日志并显示通知提示框
     * 只显示警告和错误信息，不显示成功信息
     * @param originalText 原始日志文本
     * @param lowerText 小写日志文本
     * @param isError 是否是错误
     * @param isWarn 是否是警告
     * @param isSuccess 是否是成功（不显示）
     */
    private void checkAndShowNotification(String originalText, String lowerText, boolean isError, boolean isWarn, boolean isSuccess) {
        // 检测需要显示提示框的关键词（只显示警告和错误）
        boolean shouldShowNotification = false;
        com.cc.job.gui.util.NotificationToast.NotificationType notificationType = null;
        String notificationMessage = null;
        
        // 检测"任务正在运行中"等关键词（警告类型）
        if (lowerText.contains("任务正在运行中") || 
            lowerText.contains("任务正在执行") ||
            lowerText.contains("任务开始执行")) {
            shouldShowNotification = true;
            notificationType = com.cc.job.gui.util.NotificationToast.NotificationType.WARNING;
            notificationMessage = "任务正在运行中，请等待执行完成";
        } 
        // 检测错误信息
        else if (isError && (lowerText.contains("任务执行失败") || 
                                lowerText.contains("任务触发失败") ||
                                lowerText.contains("执行结果:失败"))) {
            shouldShowNotification = true;
            notificationType = com.cc.job.gui.util.NotificationToast.NotificationType.ERROR;
            notificationMessage = "任务执行失败，请查看日志详情";
        } 
        // 检测警告信息
        else if (isWarn && lowerText.contains("警告")) {
            shouldShowNotification = true;
            notificationType = com.cc.job.gui.util.NotificationToast.NotificationType.WARNING;
            // 提取警告信息（最多120个字符）
            if (originalText.length() > 120) {
                notificationMessage = originalText.substring(0, 117) + "...";
            } else {
                notificationMessage = originalText;
            }
        }
        
        // 显示通知提示框（只显示警告和错误）
        if (shouldShowNotification && notificationType != null && notificationMessage != null) {
            com.cc.job.gui.util.NotificationToast.show(notificationMessage, notificationType);
        }
    }
    
    /**
     * 直接追加文本（不添加时间戳和格式）
     * 用于显示原始日志内容（添加到当前标签页）
     */
    public void appendText(String text) {
        appendText(null, text);
    }
    
    /**
     * 判断日志消息是否是任务组运行相关的日志
     * 
     * @param message 日志消息
     * @return true 如果是任务运行相关日志，false 否则
     */
    private boolean isExecutionRelatedLog(String message) {
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // 任务运行相关的关键词
        return lowerMessage.contains("开始执行任务组") ||
               lowerMessage.contains("执行任务组") ||
               (lowerMessage.contains("任务组") && (lowerMessage.contains("执行") || lowerMessage.contains("运行"))) ||
               lowerMessage.contains("任务执行") ||
               lowerMessage.contains("任务运行") ||
               lowerMessage.contains("任务完成") ||
               lowerMessage.contains("任务失败") ||
               lowerMessage.contains("任务已提交") ||
               lowerMessage.contains("任务已停止") ||
               lowerMessage.contains("执行批次id") ||
               lowerMessage.contains("sse") ||
               lowerMessage.contains("收到sse消息") ||
               lowerMessage.contains("更新节点状态") ||
               lowerMessage.contains("任务运行完成") ||
               lowerMessage.contains("日志加载完成") ||
               (lowerMessage.contains("═══════") && (lowerMessage.contains("执行") || lowerMessage.contains("完成")));
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
            
            // 判断是否是任务组运行相关的日志
            // 任务运行相关的日志包括：开始执行、执行完成、执行失败、SSE消息、任务状态更新等
            boolean isExecutionLog = isExecutionRelatedLog(message);
            
            LogEntry entry = new LogEntry(timestamp, icon, level, levelColor, message, messageColor, false, isExecutionLog);
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
