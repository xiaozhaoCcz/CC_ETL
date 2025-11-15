package com.cc.job.gui.view;

import com.cc.job.gui.util.IconUtil;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.commons.text.StringEscapeUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.flowless.VirtualizedScrollPane;

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
    
    // 搜索防抖：延迟300ms执行搜索
    private javafx.animation.Timeline searchDebounceTimeline;
    private static final long SEARCH_DEBOUNCE_MS = 300;
    
    // 批量更新日志：累积日志后批量渲染
    private final java.util.concurrent.ConcurrentLinkedQueue<LogUpdateTask> pendingLogUpdates = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private javafx.animation.Timeline batchUpdateTimeline;
    private static final long BATCH_UPDATE_INTERVAL_MS = 100; // 每100ms批量更新一次
    
    @SuppressWarnings("unused")
    private Runnable onDetach;  // 弹出回调
    private Runnable onClose;   // 关闭回调
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * 日志更新任务（用于批量更新）
     */
    private static class LogUpdateTask {
        final Long taskGroupId;
        final LogEntry entry;
        
        LogUpdateTask(Long taskGroupId, LogEntry entry) {
            this.taskGroupId = taskGroupId;
            this.entry = entry;
        }
    }
    
    /**
     * 日志标签页数据
     */
    private static class LogTabData {
        VirtualizedScrollPane<CodeArea> scrollPane;
        CodeArea codeArea; // 使用 RichTextFX CodeArea 显示日志
        int logCount = 0;
        int filteredCount = 0;
        String status = "就绪";
        String statusColor = "#10B981";
        private final List<LogEntry> entries = new ArrayList<>();
        
        // 记录已追加的日志数量，用于增量追加
        private int appendedCount = 0;
        private String lastSearchKeyword = "";
        
        // 是否自动滚动到底部（用户手动滚动时设为false）
        private boolean autoScrollToBottom = true;

        // 标记是否正在执行自动滚动（避免误判为用户滚动）
        private boolean isAutoScrolling = false;

        // 记录用户最后一次手动滚动的时间戳
        private long lastUserScrollTime = 0;
        
        LogTabData() {
            // 创建 CodeArea 用于显示日志
            codeArea = new CodeArea();
            codeArea.setEditable(false); // 只读
            codeArea.setWrapText(true); // 自动换行
            
            // 设置样式
            codeArea.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-padding: 12 20; " +
                "-fx-border-width: 0; " +
                "-fx-text-fill: #000000; " +
                "-fx-font-size: 13px; " +
                "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                "-fx-line-spacing: 0; " +
                "-fx-effect: null;"
            );
            
            // CodeArea 内置支持复制和全选功能（Ctrl+C, Ctrl+A）
            // 添加右键菜单增强用户体验
            ContextMenu contextMenu = new ContextMenu();
            MenuItem copyItem = new MenuItem("复制");
            copyItem.setOnAction(e -> {
                String selectedText = codeArea.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(selectedText);
                    clipboard.setContent(content);
                } else {
                    // 如果没有选中文本，复制全部内容
                    String allText = codeArea.getText();
                    if (allText != null && !allText.isEmpty()) {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent content = new ClipboardContent();
                        content.putString(allText);
                        clipboard.setContent(content);
                    }
                }
            });
            MenuItem selectAllItem = new MenuItem("全选");
            selectAllItem.setOnAction(e -> codeArea.selectAll());
            contextMenu.getItems().addAll(copyItem, selectAllItem);
            codeArea.setContextMenu(contextMenu);
            
            // 检测是否在底部的辅助方法
            Runnable checkScrollPosition = () -> {
                Platform.runLater(() -> {
                    try {
                        // 使用 VirtualizedScrollPane 来检测滚动位置
                        if (scrollPane != null) {
                            // 获取垂直滚动条
                            javafx.scene.control.ScrollBar vScrollBar = null;
                            for (javafx.scene.Node node : scrollPane.lookupAll(".scroll-bar")) {
                                if (node instanceof javafx.scene.control.ScrollBar) {
                                    javafx.scene.control.ScrollBar sb = (javafx.scene.control.ScrollBar) node;
                                    if (sb.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                                        vScrollBar = sb;
                                        break;
                                    }
                                }
                            }

                            if (vScrollBar != null) {
                                // 获取滚动条的最大值和当前值
                                double max = vScrollBar.getMax();
                                double value = vScrollBar.getValue();
                                double visibleAmount = vScrollBar.getVisibleAmount();

                                // 如果滚动条已经滚动到底部（即当前值 >= 最大值 - 可见区域大小），认为在底部
                                // 这样可以确保只有真正滚动到底部时才启用自动滚动
                                if (value >= max - visibleAmount - 1.0) { // 允许1.0的容差
                                    // 在底部，启用自动滚动
                                    autoScrollToBottom = true;
                                } else {
                                    // 不在底部，禁用自动滚动，并记录用户滚动时间
                                    autoScrollToBottom = false;
                                    lastUserScrollTime = System.currentTimeMillis();
                                }
                            } else {
                                // 如果找不到滚动条，使用备用方法：检查文本长度
                                int totalLength = codeArea.getLength();
                                if (totalLength > 0) {
                                    // 检查光标是否在最后10%的位置
                                    // 这是一个近似方法，当找不到滚动条时使用
                                    int currentPos = codeArea.getCaretPosition();
                                    int last10Percent = (int) (totalLength * 0.9);
                                    if (currentPos >= last10Percent) {
                                        autoScrollToBottom = true;
                                    } else {
                                        autoScrollToBottom = false;
                                        lastUserScrollTime = System.currentTimeMillis();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 如果检测失败，使用备用方法
                        int totalLength = codeArea.getLength();
                        if (totalLength > 0) {
                            int currentPos = codeArea.getCaretPosition();
                            int last10Percent = (int) (totalLength * 0.9);
                            if (currentPos >= last10Percent) {
                                autoScrollToBottom = true;
                            } else {
                                autoScrollToBottom = false;
                                lastUserScrollTime = System.currentTimeMillis();
                            }
                        }
                    }
                });
            };
            
            // 监听鼠标滚轮事件
            codeArea.setOnScroll(event -> {
                // 用户手动滚动，检查是否在底部
                checkScrollPosition.run();
            });
            
            // 监听鼠标拖动滚动条事件（通过监听光标位置变化）
            codeArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
                // 如果正在自动滚动，不检查位置
                if (isAutoScrolling) {
                    return;
                }

                // 只有当光标位置变化明显时才认为是用户滚动
                // 避免因自动滚动导致的光标微小变化
                if (Math.abs(newPos.intValue() - oldPos.intValue()) > 10) {
                    // 延迟检查，避免在快速连续变化时频繁触发
                    javafx.animation.Timeline delayCheck = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(
                            javafx.util.Duration.millis(100), // 减少延迟时间
                            e -> {
                                // 再次检查是否还在自动滚动
                                if (!isAutoScrolling) {
                                    checkScrollPosition.run();
                                }
                            }
                        )
                    );
                    delayCheck.play();
                }
            });
            
            // 监听键盘滚动事件（PageUp/PageDown/方向键等）
            codeArea.setOnKeyPressed(event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.PAGE_UP || code == KeyCode.PAGE_DOWN ||
                    code == KeyCode.UP || code == KeyCode.DOWN ||
                    code == KeyCode.HOME || code == KeyCode.END) {
                    // 用户使用键盘滚动，检查是否在底部
                    checkScrollPosition.run();
                }
            });
            
            // 监听文本变化，自动滚动到底部
            codeArea.textProperty().addListener((obs, oldText, newText) -> {
                if (autoScrollToBottom && newText != null && !newText.equals(oldText)) {
                    Platform.runLater(() -> {
                        // 标记正在自动滚动
                        isAutoScrolling = true;
                        // 延迟滚动，确保布局已完成
                        javafx.animation.Timeline scrollTimeline = new javafx.animation.Timeline(
                            new javafx.animation.KeyFrame(
                                javafx.util.Duration.millis(50),
                                e -> {
                                    // 滚动到底部
                                    codeArea.moveTo(codeArea.getLength());
                                    codeArea.requestFollowCaret();
                                    // 滚动完成后，延迟重置标志
                                    Platform.runLater(() -> {
                                        javafx.animation.Timeline resetFlag = new javafx.animation.Timeline(
                                            new javafx.animation.KeyFrame(
                                                javafx.util.Duration.millis(100),
                                                e2 -> {
                                                    isAutoScrolling = false;
                                                    // 自动滚动后，不要自动重新启用autoScrollToBottom
                                                    // 让用户的手动滚动行为保持有效
                                                    // 只有当用户再次滚动到底部时，才会重新启用
                                                }
                                            )
                                        );
                                        resetFlag.play();
                                    });
                                }
                            )
                        );
                        scrollTimeline.play();
                    });
                    }
                });
            
            // 使用 VirtualizedScrollPane 包装 CodeArea，提供高性能滚动
            scrollPane = new VirtualizedScrollPane<>(codeArea);
            scrollPane.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-width: 0;"
            );
            
            // 自定义滚动条样式
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
            // 在添加新日志前，清理消息中的空白字符，确保日志之间没有空格
            if (entry != null && entry.message != null) {
                String message = entry.message;
                // 移除前导空白（包括空格、制表符、换行符）
                message = message.replaceAll("^[\\s\\n\\r\\t]+", "");
                // 移除尾随空白（包括空格、制表符、换行符）
                message = message.replaceAll("[\\s\\n\\r\\t]+$", "");
                // 将中间多个连续空白字符压缩为单个空格，但保留单个换行符
                message = message.replaceAll("[\\r\\n]+", "\n"); // 统一换行符
                message = message.replaceAll("[ \\t]+", " "); // 压缩空格和制表符
                
                // 如果清理后为空，不添加
                if (message.isEmpty()) {
                    return;
                }
                
                // 创建新的日志条目，使用清理后的消息
                entry = new LogEntry(
                    entry.timestamp,
                    entry.icon,
                    entry.level,
                    entry.levelColor,
                    message,
                    entry.messageColor,
                    entry.raw,
                    entry.isExecutionLog
                );
            }
            
            entries.add(entry);
            logCount = entries.size();
        }
        
        void clearEntries() {
            entries.clear();
            logCount = 0;
            appendedCount = 0;
            lastSearchKeyword = "";
            codeArea.clear();
            autoScrollToBottom = true;
        }
        
        /**
         * 增量渲染：只追加新增的日志，避免全量重建
         * 确保日志之间没有空格，只有换行符
         */
        void renderIncremental(String keyword) {
            String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
            boolean keywordChanged = !normalized.equals(lastSearchKeyword);
            
            // 如果搜索关键词改变，需要重新渲染所有日志
            if (keywordChanged) {
                render(normalized);
                return;
            }
            
            // 获取需要渲染的日志（只显示任务组运行日志）
            List<LogEntry> executionLogs = new ArrayList<>();
            for (LogEntry entry : entries) {
                if (entry.isExecutionLog) {
                    executionLogs.add(entry);
                }
            }
            
            // 只追加新增的日志
            boolean hasKeyword = !normalized.isEmpty();
            StringBuilder newContent = new StringBuilder();
            
            for (int i = appendedCount; i < executionLogs.size(); i++) {
                LogEntry entry = executionLogs.get(i);
                if (!hasKeyword || entry.matches(normalized)) {
                    String message = formatLogMessage(entry);
                    if (message != null && !message.isEmpty()) {
                        // 如果已有内容，添加换行符（不添加空格）
                        if (newContent.length() > 0) {
                            newContent.append("\n");
                        }
                        newContent.append(message);
                    }
                }
            }
            
            // 追加新内容到 CodeArea
            if (newContent.length() > 0) {
                String currentText = codeArea.getText();
                // 移除新内容开头的所有空白字符
                String newText = newContent.toString();
                newText = newText.replaceAll("^[\\s\\n\\r\\t]+", "");
                
                // 如果当前文本不为空且不以换行符结尾，添加换行符连接新日志（不添加空格）
                if (!currentText.isEmpty() && !newText.isEmpty()) {
                    // 检查当前文本末尾是否有换行符
                    if (!currentText.endsWith("\n")) {
                        codeArea.appendText("\n" + newText);
                    } else {
                        codeArea.appendText(newText);
                    }
                } else if (!newText.isEmpty()) {
                    codeArea.appendText(newText);
                }
                appendedCount = executionLogs.size();
                
                    // 更新计数
                Platform.runLater(() -> {
                    if (!hasKeyword) {
                        filteredCount = executionLogs.size();
                    } else {
                        // 搜索模式下，需要重新计算匹配数
                        filteredCount = 0;
                        for (LogEntry entry : executionLogs) {
                            if (entry.matches(normalized)) {
                                filteredCount++;
                            }
                        }
                    }
                    
                    // 注意：不再在这里直接执行自动滚动
                    // 自动滚动将由文本变化监听器处理，这样可以尊重用户的滚动行为
                });
            }
        }
        
        /**
         * 格式化日志消息（移除前导和尾随空白，确保日志之间没有空格）
         */
        private String formatLogMessage(LogEntry entry) {
            if (entry == null || entry.message == null) {
                return "";
            }
            String message = entry.message;
            // 移除前导空白（包括空格、制表符、换行符）
            message = message.replaceAll("^[\\s\\n\\r\\t]+", "");
            // 移除尾随空白（包括空格、制表符、换行符）
            message = message.replaceAll("[\\s\\n\\r\\t]+$", "");
            // 将中间多个连续空白字符（包括换行符）压缩为单个空格
            // 但保留单个换行符，用于多行日志
            message = message.replaceAll("[\\r\\n]+", "\n"); // 统一换行符
            message = message.replaceAll("[ \\t]+", " "); // 压缩空格和制表符
            return message;
        }
        
        /**
         * 完整渲染（用于搜索或清空时）
         */
        void render(String keyword) {
            String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
            boolean hasKeyword = !normalized.isEmpty();
            lastSearchKeyword = normalized;

            // 【过滤】只显示任务组运行日志（isExecutionLog == true）
            List<LogEntry> executionLogs = new ArrayList<>();
            for (LogEntry entry : entries) {
                if (entry.isExecutionLog) {
                    executionLogs.add(entry);
                }
            }

            if (executionLogs.isEmpty()) {
                filteredCount = 0;
                appendedCount = 0;
                if (hasKeyword) {
                    codeArea.replaceText("暂无日志可供搜索\n调整关键字或过滤条件后重试。");
                } else {
                    codeArea.replaceText("日志暂未产生\n启动任务组或执行操作后，这里会实时展示运行信息。");
                }
                autoScrollToBottom = true;
                Platform.runLater(() -> {
                    if (hasKeyword) {
                        codeArea.moveTo(0);
                        codeArea.showParagraphAtTop(0);
                    } else {
                        codeArea.moveTo(codeArea.getLength());
                        codeArea.requestFollowCaret();
                    }
                });
                return;
            }

            // 构建所有日志内容，确保日志之间没有空格，只有换行符
            StringBuilder content = new StringBuilder();
            int matches = 0;
            
            for (LogEntry entry : executionLogs) {
                if (!hasKeyword || entry.matches(normalized)) {
                    String message = formatLogMessage(entry);
                    if (message != null && !message.isEmpty()) {
                        // 如果已有内容，添加换行符（不添加空格）
                        if (content.length() > 0) {
                            content.append("\n");
                        }
                        content.append(message);
                        matches++;
                    }
                }
            }

            // 设置内容到 CodeArea
            // 移除末尾的所有空白字符（包括空格、换行符等）
            String finalContent = content.toString();
            finalContent = finalContent.replaceAll("[\\s\\n\\r\\t]+$", "");
            
            // 设置文本内容
            codeArea.replaceText(finalContent);
            appendedCount = executionLogs.size();
            // 注意：不再在这里直接设置autoScrollToBottom
            // 自动滚动将由文本变化监听器处理，这样可以尊重用户的滚动行为

            // 等待布局完成后，根据模式滚动到合适位置
            Platform.runLater(() -> {
                javafx.animation.Timeline layoutTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(100),
                        e -> {
                            if (hasKeyword) {
                                // 搜索模式下，滚动到顶部
                                isAutoScrolling = true; // 标记为自动滚动，避免误判
                                codeArea.moveTo(0);
                                codeArea.showParagraphAtTop(0);
                                Platform.runLater(() -> {
                                    isAutoScrolling = false;
                                });
                            }
                            // 正常模式下，不在这里自动滚动到底部
                            // 让文本变化监听器根据autoScrollToBottom标志来决定是否滚动
                        }
                    )
                );
                layoutTimeline.play();
            });

            if (hasKeyword) {
                filteredCount = matches;
                if (matches == 0) {
                    codeArea.replaceText("未找到匹配的日志记录\n尝试缩短搜索词或更改日志级别筛选条件。");
                }
            } else {
                filteredCount = executionLogs.size();
            }
        }
        
        private Node buildLogRow(LogEntry entry, int rowIndex) {
            boolean isRaw = entry.raw;
            // 简化布局：只保留日志内容框，移除时间戳和状态标识
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(0, 20, 0, 20)); // 上下 padding 设为 0，消除行间距
            row.setMaxWidth(Double.MAX_VALUE);
            // 确保行高度根据内容自适应，不产生多余空白
            row.setPrefHeight(Region.USE_COMPUTED_SIZE);
            row.setMinHeight(Region.USE_COMPUTED_SIZE);
            row.setMaxHeight(Region.USE_PREF_SIZE);
            row.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-width: 0; " +
                "-fx-spacing: 0;"
            );

            // 只保留日志消息内容，移除时间戳和状态标识
            // 清理日志消息：移除前导和尾随空白，移除末尾的换行符，避免产生空白行
            String messageText = entry.message != null ? entry.message : "";
            // 移除前导空白
            messageText = messageText.replaceAll("^\\s+", "");
            // 移除尾随空白和换行符（但保留中间的换行符）
            messageText = messageText.replaceAll("\\s+$", "");
            // 移除末尾的换行符，避免产生空白行
            messageText = messageText.replaceAll("\\n+$", "");
            // 如果消息为空，不显示
            if (messageText.isEmpty()) {
                return null;
            }
            TextArea messageTextArea = new TextArea(messageText);
            messageTextArea.setEditable(false);
            messageTextArea.setWrapText(true);
            messageTextArea.setMaxWidth(Double.MAX_VALUE);
            
            // 精确计算 TextArea 高度，消除所有空白
            // 不使用 setPrefRowCount，直接计算精确高度
            if (messageText.isEmpty()) {
                // 空文本：设置最小高度，但尽量小
                messageTextArea.setPrefHeight(1);
                messageTextArea.setMinHeight(1);
                messageTextArea.setMaxHeight(1);
            } else {
                // 计算实际行数（考虑换行符）
                int actualLines = messageText.split("\n", -1).length;
                // 估算换行后的行数（每行约80字符）
                int wrappedLines = actualLines;
                if (messageText.length() > 80) {
                    wrappedLines = Math.max(actualLines, (int) Math.ceil(messageText.length() / 80.0));
                }
                // 每行高度约 20px（包括行间距），但我们已经移除了行间距，所以更精确
                // TextArea 的默认行高约为 20px，但我们设置了 -fx-line-spacing: 0
                // 实际行高约为 18-19px
                double lineHeight = 18.5; // 精确的行高
                double calculatedHeight = Math.max(1, wrappedLines * lineHeight);
                
                // 设置精确高度，避免多余空白
                messageTextArea.setPrefHeight(calculatedHeight);
                messageTextArea.setMinHeight(calculatedHeight);
                messageTextArea.setMaxHeight(calculatedHeight);
            }

            // 设置样式，使其看起来像 Label（无边框、透明背景、无滚动条）
            // 确保背景完全透明，与项目背景 #F1F5F9 一致
            // 通过 CSS 隐藏滚动条（只有外层 ScrollPane 才有滚动条）
            // 移除所有 padding、margin 和行间距，完全消除空白
            messageTextArea.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-padding: 0; " +
                "-fx-border-width: 0; " +
                "-fx-focus-color: rgba(99,102,241,0.5); " +
                "-fx-faint-focus-color: transparent; " +
                "-fx-control-inner-background: transparent; " +
                "-fx-text-box-border: transparent; " +
                "-fx-effect: null; " +
                "-fx-scroll-bar-policy: never; " +
                "-fx-line-spacing: 0; " +
                "-fx-font-size: 13px;"
            );
            
            // 添加样式类，用于 CSS 选择器
            messageTextArea.getStyleClass().add("log-message-textarea");
            
            // 使用 Platform.runLater 确保样式在渲染后生效
            Platform.runLater(() -> {
                // 查找 TextArea 的内部 content 区域并设置为透明，移除所有 padding
                javafx.scene.Node content = messageTextArea.lookup(".content");
                if (content != null) {
                    content.setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-effect: null; " +
                        "-fx-padding: 0; " +
                        "-fx-background-insets: 0;"
                    );
                }
                // 查找 viewport 并设置为透明，移除所有 padding
                javafx.scene.Node viewport = messageTextArea.lookup(".viewport");
                if (viewport != null) {
                    viewport.setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-effect: null; " +
                        "-fx-padding: 0; " +
                        "-fx-background-insets: 0;"
                    );
                }
                // 强制隐藏 TextArea 的滚动条（通过设置样式和可见性）
                javafx.scene.Node vScrollBar = messageTextArea.lookup(".scroll-bar:vertical");
                if (vScrollBar != null) {
                    vScrollBar.setVisible(false);
                    vScrollBar.setManaged(false);
                    vScrollBar.setStyle("-fx-opacity: 0; -fx-pref-width: 0; -fx-pref-height: 0;");
                }
                javafx.scene.Node hScrollBar = messageTextArea.lookup(".scroll-bar:horizontal");
                if (hScrollBar != null) {
                    hScrollBar.setVisible(false);
                    hScrollBar.setManaged(false);
                    hScrollBar.setStyle("-fx-opacity: 0; -fx-pref-width: 0; -fx-pref-height: 0;");
                }
                
                // 重新计算并设置精确高度，确保没有多余空白
                String text = messageTextArea.getText();
                if (text != null && !text.isEmpty()) {
                    int actualLines = text.split("\n", -1).length;
                    int wrappedLines = actualLines;
                    if (text.length() > 80) {
                        wrappedLines = Math.max(actualLines, (int) Math.ceil(text.length() / 80.0));
                    }
                    double lineHeight = 18.5;
                    double calculatedHeight = Math.max(1, wrappedLines * lineHeight);
                    messageTextArea.setPrefHeight(calculatedHeight);
                    messageTextArea.setMinHeight(calculatedHeight);
                    messageTextArea.setMaxHeight(calculatedHeight);
                }
            });
            
            // 设置消息颜色 - 统一使用纯黑色，除非是错误或警告
            // "输出"类型（raw=true）：默认纯黑色，错误显示红色，警告显示黄色
            // 其他类型：统一使用纯黑色，除非是错误或警告
            String finalMessageColor;
            if (isRaw) {
                // "输出"类型：如果 messageColor 已设置（错误或警告），使用它；否则使用纯黑色
                if (entry.messageColor != null && 
                    (entry.messageColor.equals("#DC2626") || entry.messageColor.equals("#D97706"))) {
                    // 错误（红色）或警告（黄色）已设置，使用它
                    finalMessageColor = entry.messageColor;
                } else {
                    // 默认纯黑色
                    finalMessageColor = "#000000";
                }
            } else {
                // 非"输出"类型：如果是错误或警告，使用对应颜色；否则统一使用纯黑色
                if (entry.messageColor != null && 
                    (entry.messageColor.equals("#DC2626") || entry.messageColor.equals("#D97706") ||
                     entry.messageColor.equals("#B91C1C") || entry.messageColor.equals("#B45309"))) {
                    // 错误或警告颜色，使用它
                    finalMessageColor = entry.messageColor;
                } else {
                    // 统一使用纯黑色
                    finalMessageColor = "#000000";
            }
            }
            
            // 设置文本颜色，确保没有阴影效果
            messageTextArea.setStyle(messageTextArea.getStyle() + 
                String.format(" -fx-text-fill: %s; -fx-effect: null;", finalMessageColor));
            
            // 设置字体
            Font messageFont = isRaw
                ? Font.font("Consolas", FontWeight.NORMAL, 12)
                : Font.font("System", FontWeight.NORMAL, 13);
            messageTextArea.setFont(messageFont);
            
            // TextArea 默认支持 Ctrl+C 复制，但为了更好的用户体验，我们显式处理
            // 这样可以确保复制功能在所有平台上都能正常工作
            messageTextArea.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.C && event.isControlDown()) {
                    String selectedText = messageTextArea.getSelectedText();
                    if (selectedText != null && !selectedText.isEmpty()) {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent content = new ClipboardContent();
                        content.putString(selectedText);
                        clipboard.setContent(content);
                        event.consume();
                    }
                }
            });
            
            // 添加鼠标右键菜单：复制
            ContextMenu contextMenu = new ContextMenu();
            MenuItem copyItem = new MenuItem("复制");
            copyItem.setOnAction(e -> {
                String selectedText = messageTextArea.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(selectedText);
                    clipboard.setContent(content);
                }
            });
            contextMenu.getItems().add(copyItem);
            messageTextArea.setContextMenu(contextMenu);
            
            HBox.setHgrow(messageTextArea, Priority.ALWAYS);

            // 只添加日志内容框，移除时间戳和状态标识
            row.getChildren().add(messageTextArea);
            return row;
        }

        private Node buildEmptyState(String title, String description) {
            VBox container = new VBox(6);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(24, 28, 24, 28));
            container.setMaxWidth(Double.MAX_VALUE);
            String radius = StyleUtil.RADIUS_LG;
            // 使用项目背景颜色 #F1F5F9，与主界面保持一致
            container.setStyle(String.format(
                "-fx-background-color: #F1F5F9; " +
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
        // 使用项目背景颜色 #F1F5F9，与主界面保持一致
        private static final String NORMAL_STYLE = BASE_STYLE +
            "-fx-background-color: #F1F5F9; " +
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
        
        // 启动批量更新定时器
        startBatchUpdateTimer();
    }
    
    /**
     * 启动批量更新定时器：定期批量处理日志更新，减少UI刷新频率
     */
    private void startBatchUpdateTimer() {
        batchUpdateTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(BATCH_UPDATE_INTERVAL_MS),
                e -> processBatchLogUpdates()
            )
        );
        batchUpdateTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        batchUpdateTimeline.play();
    }
    
    /**
     * 批量处理日志更新
     */
    private void processBatchLogUpdates() {
        if (pendingLogUpdates.isEmpty()) {
            return;
        }
        
        // 批量处理所有待更新的日志
        Map<Long, java.util.List<LogEntry>> updatesByGroup = new HashMap<>();
        while (!pendingLogUpdates.isEmpty()) {
            LogUpdateTask task = pendingLogUpdates.poll();
            if (task != null) {
                updatesByGroup.computeIfAbsent(task.taskGroupId, k -> new ArrayList<>())
                    .add(task.entry);
            }
        }
        
        // 在UI线程中批量更新
        Platform.runLater(() -> {
            updatesByGroup.forEach((taskGroupId, newEntries) -> {
                LogTabData tabData = getTabData(taskGroupId);
                if (tabData != null) {
                    for (LogEntry entry : newEntries) {
                        tabData.addEntry(entry);
                    }
                    
                    // 如果当前显示的是这个标签页，使用增量渲染
                    if (isCurrentTab(taskGroupId)) {
                        tabData.renderIncremental(currentSearchKeyword);
                        updateStatusBar(tabData);
                    }
                }
            });
        });
    }
    
    /**
     * 创建标签页导航栏
     */
    private HBox createTabBar() {
        HBox tabBar = new HBox(0);
        // 使用项目背景颜色 #F1F5F9，与主界面保持一致
        tabBar.setStyle(
            "-fx-background-color: #F1F5F9; " +
            "-fx-border-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 0; " +
            "-fx-background-insets: 0;"
        );
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPrefHeight(36);
        tabBar.setMinHeight(36);
        tabBar.setMaxHeight(36);
        
        // 创建标题栏容器（包含"监控"标签和操作按钮）
        HBox titleContainer = new HBox(8);
        titleContainer.setAlignment(Pos.CENTER_LEFT);
        titleContainer.setPadding(new Insets(0, 8, 0, 20));
        
        // 创建"监控"标签（固定标签，不可关闭）
        Label monitorLabel = new Label("监控");
        monitorLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        monitorLabel.setTextFill(Color.web(StyleUtil.GRAY_900));
        monitorLabel.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0;"
        );
        monitorLabel.setAlignment(Pos.CENTER_LEFT);
        
        // 弹出按钮
        Button detachBtn = new Button("", IconUtil.windowIcon());
        StyleUtil.applyIconButtonHover(detachBtn);
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnAction(e -> {
            if (onDetach != null) {
                onDetach.run();
            }
        });
        
        // 关闭按钮
        Button closeBtn = new Button("", IconUtil.closeIcon());
        StyleUtil.applyIconButtonHover(closeBtn);
        closeBtn.setTooltip(new Tooltip("关闭面板"));
        closeBtn.setOnAction(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        
        titleContainer.getChildren().addAll(monitorLabel, detachBtn, closeBtn);
        
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
        
        tabBar.getChildren().addAll(titleContainer, scrollPane);
        
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
        
        // 搜索框
        searchField = new TextField();
        searchField.setPromptText("搜索日志...");
        searchField.setPrefWidth(200);
        // 搜索框使用项目背景颜色，与主界面保持一致
        searchField.setStyle(
            "-fx-background-color: #F1F5F9; " +
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
        // 搜索框防抖：延迟执行搜索，避免频繁渲染
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // 取消之前的定时器
            if (searchDebounceTimeline != null) {
                searchDebounceTimeline.stop();
            }
            
            // 创建新的防抖定时器
            searchDebounceTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(SEARCH_DEBOUNCE_MS),
                    e -> applySearchFilter(newVal)
                )
            );
            searchDebounceTimeline.play();
        });
        
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
            if (tabData != null && tabData.codeArea != null) {
                Platform.runLater(() -> {
                    // 移动光标到文本开头
                    tabData.codeArea.moveTo(0);
                    // 请求视图跟随光标，滚动到顶部
                    tabData.codeArea.requestFollowCaret();
                    // 或者使用 showParagraphAtTop 方法确保滚动到顶部
                    tabData.codeArea.showParagraphAtTop(0);
                    tabData.autoScrollToBottom = false; // 用户手动滚动，暂时禁用自动滚动
                });
            }
        });
        scrollToTopBtn.setTooltip(new Tooltip("滚动到顶部"));
        
        // 清空按钮 - 扁平化设计，靠右排列
        clearBtn = createToolButton("清空", this::clearLogs);
        
        // 导出按钮 - 扁平化设计，靠右排列
        exportBtn = createToolButton("导出", this::exportLogs);
        
        titleBar.getChildren().addAll(
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
        // 使用项目背景颜色 #F1F5F9，与主界面保持一致
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
        String hover =
            "-fx-background-color: linear-gradient(to bottom, rgba(241,245,249,0.99), rgba(226,232,240,0.99)); " +
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
     * 解析HTML实体，将 &lt; &gt; &amp; &#39; &#x27; 等转换为实际字符
     * 使用 Apache Commons Text 库进行完善的 HTML 实体解码
     * 支持所有标准 HTML 实体（命名实体和数字实体）
     * 
     * @param text 原始文本
     * @return 解析后的文本
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) {
            return null;
        }
        
        try {
            // 使用 Apache Commons Text 库解码 HTML 实体
            // StringEscapeUtils.unescapeHtml4() 支持：
            // - 命名实体：&amp;, &lt;, &gt;, &quot;, &apos;, &nbsp; 等
            // - 数字实体：&#39; (十进制), &#x27; (十六进制) 等
            // - 所有标准 HTML 4 实体
            String decoded = StringEscapeUtils.unescapeHtml4(text);
            
            // 额外处理换行标签（Commons Text 可能不会处理这些）
            decoded = decoded.replace("<br>", "\n")
                       .replace("<br/>", "\n")
                           .replace("<br />", "\n")
                           .replace("&lt;br&gt;", "\n")
                           .replace("&lt;br/&gt;", "\n")
                           .replace("&lt;br /&gt;", "\n");
            
            return decoded;
        } catch (Exception e) {
            // 如果解码失败，返回原始文本（避免影响日志显示）
            // 记录警告但不抛出异常
            System.err.println("HTML entity decoding failed: " + e.getMessage());
            return text;
        }
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
            
            // 清理文本：移除前导和尾随空白，确保日志之间没有空格
            decodedText = decodedText.replaceAll("^[\\s\\n\\r\\t]+", ""); // 移除前导空白（包括空格、换行符等）
            decodedText = decodedText.replaceAll("[\\s\\n\\r\\t]+$", ""); // 移除尾随空白（包括空格、换行符等）
            // 将中间多个连续空白字符压缩为单个空格，但保留单个换行符
            decodedText = decodedText.replaceAll("[\\r\\n]+", "\n"); // 统一换行符
            decodedText = decodedText.replaceAll("[ \\t]+", " "); // 压缩空格和制表符
            
            // 如果清理后为空，不添加日志
            if (decodedText.isEmpty()) {
                return;
            }
            
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
            
            // 添加到批量更新队列，而不是立即渲染
            pendingLogUpdates.offer(new LogUpdateTask(taskGroupId, entry));
            
            // 更新状态（立即更新，不等待批量处理）
            if (tabData != null) {
            tabData.status = "运行中";
            tabData.statusColor = "#10B981";
            }
            
            // 检测特定日志，触发通知提示框
            checkAndShowNotification(decodedText, lowerText, isError, isWarn, isSuccess);
            
            // 状态栏更新会在批量处理时进行
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

        // 检测错误信息
        if (isError && (lowerText.contains("任务执行失败") ||
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
            
            // 解析HTML实体（包括数字实体如 &#39;）
            String decodedMessage = decodeHtmlEntities(message);
            
            // 清理消息：移除前导和尾随空白，确保日志之间没有空格
            decodedMessage = decodedMessage.replaceAll("^[\\s\\n\\r\\t]+", ""); // 移除前导空白（包括空格、换行符等）
            decodedMessage = decodedMessage.replaceAll("[\\s\\n\\r\\t]+$", ""); // 移除尾随空白（包括空格、换行符等）
            // 将中间多个连续空白字符压缩为单个空格，但保留单个换行符
            decodedMessage = decodedMessage.replaceAll("[\\r\\n]+", "\n"); // 统一换行符
            decodedMessage = decodedMessage.replaceAll("[ \\t]+", " "); // 压缩空格和制表符
            
            // 如果清理后为空，不添加日志
            if (decodedMessage.isEmpty()) {
                return;
            }
            
            // 判断是否是任务组运行相关的日志
            // 任务运行相关的日志包括：开始执行、执行完成、执行失败、SSE消息、任务状态更新等
            boolean isExecutionLog = isExecutionRelatedLog(decodedMessage);
            
            LogEntry entry = new LogEntry(timestamp, icon, level, levelColor, decodedMessage, messageColor, false, isExecutionLog);
            
            // 添加到批量更新队列
            pendingLogUpdates.offer(new LogUpdateTask(taskGroupId, entry));
            
            // 更新状态（立即更新）
            if (tabData != null) {
            tabData.status = "运行中";
            tabData.statusColor = "#10B981";
            }
            
            // 状态栏更新会在批量处理时进行
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
    
    /**
     * 设置关闭回调
     */
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
}
