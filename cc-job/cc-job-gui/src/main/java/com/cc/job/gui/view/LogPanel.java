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
        VBox logList;
        int logCount = 0;
        int filteredCount = 0;
        String status = "就绪";
        String statusColor = "#10B981";
        private final List<LogEntry> entries = new ArrayList<>();
        
        LogTabData() {
            logList = new VBox();
            logList.setSpacing(0);
            logList.setPadding(new Insets(12, 0, 12, 0));
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

            if (entries.isEmpty()) {
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
            for (LogEntry entry : entries) {
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
                filteredCount = entries.size();
                scrollToBottom();
            }
        }
        
        private Node buildLogRow(LogEntry entry, int rowIndex) {
            boolean isRaw = entry.raw;
            String background = rowIndex % 2 == 0 ? "rgba(248,250,252,0.9)" : "#FFFFFF";

            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 20, 8, 20));
            row.setMaxWidth(Double.MAX_VALUE);
            row.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: transparent transparent rgba(226,232,240,0.75) transparent; " +
                "-fx-border-width: 0 0 1 0;",
                background
            ));

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
            messageLabel.setTextFill(Color.web(
                entry.messageColor != null ? entry.messageColor : StyleUtil.GRAY_700
            ));
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
        private static final String BASE_STYLE =
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0; " +
            "-fx-border-width: 0; " +
            "-fx-cursor: hand; " +
            "-fx-effect: null;";
        private static final String NORMAL_STYLE = BASE_STYLE +
            "-fx-background-color: #FFFFFF;";
        private static final String HOVER_STYLE = BASE_STYLE +
            "-fx-background-color: rgba(241,245,249,0.95);";
        private static final String ACTIVE_STYLE = BASE_STYLE +
            "-fx-background-color: rgba(243,244,246,0.9);";
        
        public LogTab(Long taskGroupId, String taskGroupName) {
            this.taskGroupId = taskGroupId;
            this.taskGroupName = taskGroupName;
            initializeUI();
        }
        
        private void initializeUI() {
            setPadding(new Insets(6, 16, 6, 16));
            setPrefHeight(38);
            setMinHeight(38);
            setMaxHeight(38);
            setStyle(NORMAL_STYLE);
            
            HBox content = new HBox(10);
            content.setAlignment(Pos.CENTER_LEFT);
            
            // 任务组名称
            nameLabel = new Label(taskGroupName);
            nameLabel.setStyle(
                "-fx-font-size: 12.5px; " +
                "-fx-text-fill: " + StyleUtil.GRAY_600 + "; " +
                "-fx-font-weight: 600;"
            );
            nameLabel.setPrefWidth(150);
            nameLabel.setMaxWidth(150);
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
        String radius = StyleUtil.RADIUS_LG;
        setStyle(String.format(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: rgba(148,163,184,0.35); " +
            "-fx-border-width: 1 0 0 0; " +
            "-fx-background-radius: 0 0 %1$s %1$s; " +
            "-fx-border-radius: 0 0 %1$s %1$s;",
            radius
        ));
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
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 0;"
        );
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPrefHeight(36);
        tabBar.setMinHeight(36);
        tabBar.setMaxHeight(36);
        
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
        titleBar.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 14 20;"
        );
        titleBar.setPrefHeight(56);
        titleBar.setMinHeight(56);
        titleBar.setMaxHeight(56);
        
        // 标题 - 扁平化设计
        Label titleLabel = new Label("日志监控");
        titleLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 15));
        titleLabel.setTextFill(Color.web(StyleUtil.GRAY_900));
        
        // 状态指示器
        HBox statusIndicator = new HBox(6);
        statusIndicator.setAlignment(Pos.CENTER_LEFT);
        statusIndicator.setPadding(new Insets(0, 0, 0, 0));
        
        javafx.scene.shape.Circle indicator = new javafx.scene.shape.Circle(5);
        indicator.setFill(Color.web(StyleUtil.SUCCESS));
        indicator.setEffect(new javafx.scene.effect.Glow(0.8));
        
        statusLabel = new Label("就绪");
        statusLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
        statusLabel.setTextFill(Color.web(StyleUtil.SUCCESS_DARK));
        statusLabel.setStyle("-fx-font-weight: 600;");
        
        statusIndicator.getChildren().addAll(indicator, statusLabel);
        
        // 分隔线
        Region separator1 = createInlineSeparator();
        
        // 过滤器
        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("全部", "信息", "警告", "错误", "调试");
        filterCombo.setValue("全部");
        filterCombo.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92); " +
            "-fx-text-fill: " + StyleUtil.GRAY_600 + "; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-pref-width: 96; " +
            "-fx-pref-height: 30; " +
            "-fx-border-color: rgba(148,163,184,0.55); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 2 10 2 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.05), 4, 0, 0, 1);"
        );
        
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
            "-fx-border-color: rgba(148,163,184,0.3) transparent transparent transparent; " +
            "-fx-border-width: 1 0 0 0;"
        );
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
