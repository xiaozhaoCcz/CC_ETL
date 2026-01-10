package com.cc.job.gui.manager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 日志内容管理器 - 负责日志的存储、渲染和显示
 */
public class LogContentManager {
    
    private VirtualizedScrollPane<CodeArea> scrollPane;
    private CodeArea codeArea;
    private final List<LogEntry> entries = new ArrayList<>();
    
    public int logCount = 0;
    public int filteredCount = 0;
    public String status = "就绪";
    public String statusColor = "#10B981";
    
    private int appendedCount = 0;
    private String lastSearchKeyword = "";
    public boolean autoScrollToBottom = true;
    
    // 过滤相关字段
    private Set<String> enabledLevels = new HashSet<>(Arrays.asList("ALL", "INFO", "WARN", "ERROR", "DEBUG", "SUCCESS", "TEXT"));
    private boolean regexEnabled = false;
    private boolean caseSensitive = false;
    private boolean pauseUpdates = false;
    
    // 性能优化：最大显示日志条数
    private static final int MAX_DISPLAY_ENTRIES = 50000;
    private static final int MAX_KEEP_ENTRIES = 100000;
    
    public LogContentManager() {
        initializeCodeArea();
    }
    
    private void initializeCodeArea() {
        codeArea = new CodeArea();
        codeArea.setEditable(false);
        codeArea.setWrapText(true);
        codeArea.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background-insets: 0; " +
            "-fx-padding: 12 20; " +
            "-fx-border-width: 0; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-size: 13px; " +
            "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace;"
        );
        
        setupContextMenu();
        setupScrollListeners();
        setupTextChangeListener();
        
        scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
    }
    
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setOnAction(e -> copyText());
        MenuItem selectAllItem = new MenuItem("全选");
        selectAllItem.setOnAction(e -> codeArea.selectAll());
        contextMenu.getItems().addAll(copyItem, selectAllItem);
        codeArea.setContextMenu(contextMenu);
    }
    
    private void copyText() {
        String text = codeArea.getSelectedText();
        if (text == null || text.isEmpty()) {
            text = codeArea.getText();
        }
        if (text != null && !text.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        }
    }
    
    private void setupScrollListeners() {
        Runnable checkScrollPosition = () -> {
            Platform.runLater(() -> {
                try {
                    ScrollBar vScrollBar = findVerticalScrollBar();
                    if (vScrollBar != null) {
                        double max = vScrollBar.getMax();
                        double value = vScrollBar.getValue();
                        double visibleAmount = vScrollBar.getVisibleAmount();
                        
                        if (value >= max - visibleAmount - 1.0) {
                            autoScrollToBottom = true;
                        } else {
                            autoScrollToBottom = false;
                        }
                    }
                } catch (Exception ignored) {}
            });
        };
        
        codeArea.setOnScroll(event -> checkScrollPosition.run());
        codeArea.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.PAGE_UP || code == KeyCode.PAGE_DOWN ||
                code == KeyCode.UP || code == KeyCode.DOWN ||
                code == KeyCode.HOME || code == KeyCode.END) {
                checkScrollPosition.run();
            }
        });
    }
    
    private ScrollBar findVerticalScrollBar() {
        for (javafx.scene.Node node : scrollPane.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar) {
                ScrollBar sb = (ScrollBar) node;
                if (sb.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                    return sb;
                }
            }
        }
        return null;
    }
    
    private void setupTextChangeListener() {
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (autoScrollToBottom && newText != null && !newText.equals(oldText)) {
                Platform.runLater(() -> {
                    Timeline scrollTimeline = new Timeline(
                        new KeyFrame(
                            Duration.millis(50),
                            e -> {
                                codeArea.moveTo(codeArea.getLength());
                                codeArea.requestFollowCaret();
                            }
                        )
                    );
                    scrollTimeline.play();
                });
            }
        });
    }
    
    public void addEntry(LogEntry entry) {
        if (entry != null && entry.message != null) {
            String message = entry.message.trim();
            if (!message.isEmpty()) {
                entries.add(new LogEntry(
                    entry.timestamp, entry.icon, entry.level, entry.levelColor,
                    message, entry.messageColor, entry.raw, entry.isExecutionLog
                ));
                logCount = entries.size();
                
                // 性能优化：当日志量过大时，自动清理旧日志（保留最新的）
                if (entries.size() > MAX_KEEP_ENTRIES) {
                    int removeCount = entries.size() - MAX_KEEP_ENTRIES;
                    entries.subList(0, removeCount).clear();
                    logCount = entries.size();
                }
            }
        }
    }
    
    public void clearEntries() {
        entries.clear();
        logCount = 0;
        appendedCount = 0;
        lastSearchKeyword = "";
        codeArea.clear();
        autoScrollToBottom = true;
    }
    
    public void renderIncremental(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        boolean keywordChanged = !normalized.equals(lastSearchKeyword);
        
        if (keywordChanged) {
            render(normalized);
            return;
        }
        
        List<LogEntry> executionLogs = getExecutionLogs();
        StringBuilder newContent = new StringBuilder();
        
        for (int i = appendedCount; i < executionLogs.size(); i++) {
            LogEntry entry = executionLogs.get(i);
            if (normalized.isEmpty() || entry.matches(normalized)) {
                if (newContent.length() > 0) {
                    newContent.append("\n");
                }
                newContent.append(entry.message);
            }
        }
        
        if (newContent.length() > 0) {
            String currentText = codeArea.getText();
            String newText = newContent.toString().trim();
            
            if (!currentText.isEmpty() && !newText.isEmpty()) {
                codeArea.appendText(currentText.endsWith("\n") ? newText : "\n" + newText);
            } else if (!newText.isEmpty()) {
                codeArea.appendText(newText);
            }
            appendedCount = executionLogs.size();
            updateFilteredCount(normalized, executionLogs);
        }
    }
    
    public void render(String keyword) {
        render(keyword, enabledLevels, regexEnabled, caseSensitive);
    }
    
    public void render(String keyword, Set<String> levels, boolean useRegex, boolean caseSensitive) {
        // 异步渲染，避免UI卡顿
        new Thread(() -> {
            String normalized = keyword == null ? "" : keyword.trim();
            lastSearchKeyword = normalized;
            
            List<LogEntry> executionLogs = getExecutionLogs();
            
            if (executionLogs.isEmpty()) {
                Platform.runLater(() -> {
                    filteredCount = 0;
                    appendedCount = 0;
                    codeArea.replaceText(normalized.isEmpty() ? 
                        "日志暂未产生\n启动任务组或执行操作后，这里会实时展示运行信息。" :
                        "暂无日志可供搜索\n调整关键字或过滤条件后重试。");
                });
                return;
            }
            
            StringBuilder content = new StringBuilder();
            int matches = 0;
            Pattern pattern = null;
            
            // 编译正则表达式（如果启用）
            if (useRegex && !normalized.isEmpty()) {
                try {
                    int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                    pattern = Pattern.compile(normalized, flags);
                } catch (PatternSyntaxException e) {
                    // 正则表达式无效，回退到普通搜索
                    pattern = null;
                }
            }
            
            // 性能优化：对于大量日志，只处理最新的部分
            int startIndex = Math.max(0, executionLogs.size() - MAX_DISPLAY_ENTRIES);
            List<LogEntry> logsToProcess = executionLogs.subList(startIndex, executionLogs.size());
            
            for (LogEntry entry : logsToProcess) {
                // 级别过滤
                if (levels != null && !levels.isEmpty() && !levels.contains("ALL")) {
                    String entryLevel = entry.level != null ? entry.level : "TEXT";
                    if (!levels.contains(entryLevel)) {
                        continue;
                    }
                }
                
                // 关键词匹配
                boolean matchesKeyword = normalized.isEmpty();
                if (!matchesKeyword) {
                    if (pattern != null) {
                        matchesKeyword = pattern.matcher(entry.getSearchableText()).find();
                    } else {
                        String searchText = caseSensitive ? entry.getSearchableText() : entry.getSearchableText().toLowerCase();
                        String searchKeyword = caseSensitive ? normalized : normalized.toLowerCase();
                        matchesKeyword = searchText.contains(searchKeyword);
                    }
                }
                
                if (matchesKeyword) {
                    if (content.length() > 0) {
                        content.append("\n");
                    }
                    content.append(entry.message);
                    matches++;
                }
            }
            
            final String finalContent = content.toString().trim();
            final int finalMatches = matches;
            final boolean hasMore = startIndex > 0;
            
            Platform.runLater(() -> {
                if (hasMore && !normalized.isEmpty()) {
                    codeArea.replaceText("(仅显示最新 " + MAX_DISPLAY_ENTRIES + " 条日志的搜索结果，共找到 " + finalMatches + " 条匹配)\n\n" + finalContent);
                } else if (hasMore) {
                    codeArea.replaceText("(仅显示最新 " + MAX_DISPLAY_ENTRIES + " 条日志，共 " + executionLogs.size() + " 条)\n\n" + finalContent);
                } else {
                    codeArea.replaceText(finalContent);
                }
                appendedCount = executionLogs.size();
                filteredCount = finalMatches;
                
                if (!normalized.isEmpty() && finalMatches == 0) {
                    codeArea.replaceText("未找到匹配的日志记录\n尝试缩短搜索词或更改日志级别筛选条件。");
                }
            });
        }, "log-render-thread").start();
    }
    
    private List<LogEntry> getExecutionLogs() {
        List<LogEntry> executionLogs = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (entry.isExecutionLog) {
                executionLogs.add(entry);
            }
        }
        return executionLogs;
    }
    
    private void updateFilteredCount(String normalized, List<LogEntry> executionLogs) {
        Platform.runLater(() -> {
            if (normalized.isEmpty()) {
                filteredCount = executionLogs.size();
            } else {
                filteredCount = 0;
                for (LogEntry entry : executionLogs) {
                    if (entry.matches(normalized)) {
                        filteredCount++;
                    }
                }
            }
        });
    }
    
    public void scrollToTop() {
        Platform.runLater(() -> {
            codeArea.moveTo(0);
            codeArea.requestFollowCaret();
            codeArea.showParagraphAtTop(0);
            autoScrollToBottom = false;
        });
    }
    
    public VirtualizedScrollPane<CodeArea> getScrollPane() {
        return scrollPane;
    }
    
    public CodeArea getCodeArea() {
        return codeArea;
    }
    
    public List<LogEntry> getEntries() {
        return entries;
    }
    
    // 过滤相关方法
    public void setEnabledLevels(Set<String> levels) {
        this.enabledLevels = levels != null ? new HashSet<>(levels) : new HashSet<>(Arrays.asList("ALL"));
    }
    
    public Set<String> getEnabledLevels() {
        return enabledLevels;
    }
    
    public void setRegexEnabled(boolean enabled) {
        this.regexEnabled = enabled;
    }
    
    public void setCaseSensitive(boolean sensitive) {
        this.caseSensitive = sensitive;
    }
    
    public void setPauseUpdates(boolean pause) {
        this.pauseUpdates = pause;
    }
    
    public boolean isPauseUpdates() {
        return pauseUpdates;
    }
    
    /**
     * 获取日志统计信息
     */
    public LogStatistics getLogStatistics() {
        LogStatistics stats = new LogStatistics();
        List<LogEntry> executionLogs = getExecutionLogs();
        
        for (LogEntry entry : executionLogs) {
            String level = entry.level != null ? entry.level : "TEXT";
            stats.totalCount++;
            
            switch (level) {
                case "ERROR":
                    stats.errorCount++;
                    break;
                case "WARN":
                    stats.warnCount++;
                    break;
                case "INFO":
                    stats.infoCount++;
                    break;
                case "DEBUG":
                    stats.debugCount++;
                    break;
                case "SUCCESS":
                    stats.successCount++;
                    break;
            }
            
            // 更新时间范围
            LocalDateTime entryTime = entry.getTimestamp();
            if (entryTime != null) {
                if (stats.earliestTime == null || entryTime.isBefore(stats.earliestTime)) {
                    stats.earliestTime = entryTime;
                }
                if (stats.latestTime == null || entryTime.isAfter(stats.latestTime)) {
                    stats.latestTime = entryTime;
                }
            }
        }
        
        return stats;
    }
    
    /**
     * 日志统计信息
     */
    public static class LogStatistics {
        public int totalCount = 0;
        public int errorCount = 0;
        public int warnCount = 0;
        public int infoCount = 0;
        public int debugCount = 0;
        public int successCount = 0;
        public LocalDateTime earliestTime;
        public LocalDateTime latestTime;
    }
    
    /**
     * 日志条目
     */
    public static class LogEntry {
        public final String timestamp;
        public final String icon;
        public final String level;
        public final String levelColor;
        public final String message;
        public final String messageColor;
        public final boolean raw;
        public final boolean isExecutionLog;
        
        public LogEntry(String timestamp, String icon, String level, String levelColor,
                       String message, String messageColor, boolean raw, boolean isExecutionLog) {
            this.timestamp = timestamp;
            this.icon = icon;
            this.level = level;
            this.levelColor = levelColor;
            this.message = message;
            this.messageColor = messageColor;
            this.raw = raw;
            this.isExecutionLog = isExecutionLog;
        }
        
        public boolean matches(String keywordLower) {
            if (keywordLower == null || keywordLower.isEmpty()) {
                return true;
            }
            return getSearchableText().toLowerCase().contains(keywordLower);
        }
        
        public String getSearchableText() {
            String baseMessage = message == null ? "" : message;
            return raw ? baseMessage :
                ((timestamp != null ? timestamp : "") + " " + 
                 (level != null ? level : "") + " " + baseMessage);
        }
        
        public LocalDateTime getTimestamp() {
            if (timestamp == null || timestamp.isEmpty()) {
                return null;
            }
            try {
                // 解析时间戳格式: yyyy-MM-dd HH:mm:ss.SSS
                return LocalDateTime.parse(timestamp, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            } catch (Exception e) {
                return null;
            }
        }
    }
}

