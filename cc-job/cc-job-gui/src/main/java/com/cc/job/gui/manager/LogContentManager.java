package com.cc.job.gui.manager;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.List;

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
    private boolean autoScrollToBottom = true;
    
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
                    javafx.animation.Timeline scrollTimeline = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(
                            javafx.util.Duration.millis(50),
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
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        lastSearchKeyword = normalized;
        
        List<LogEntry> executionLogs = getExecutionLogs();
        
        if (executionLogs.isEmpty()) {
            filteredCount = 0;
            appendedCount = 0;
            codeArea.replaceText(normalized.isEmpty() ? 
                "日志暂未产生\n启动任务组或执行操作后，这里会实时展示运行信息。" :
                "暂无日志可供搜索\n调整关键字或过滤条件后重试。");
            return;
        }
        
        StringBuilder content = new StringBuilder();
        int matches = 0;
        
        for (LogEntry entry : executionLogs) {
            if (normalized.isEmpty() || entry.matches(normalized)) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(entry.message);
                matches++;
            }
        }
        
        codeArea.replaceText(content.toString().trim());
        appendedCount = executionLogs.size();
        filteredCount = normalized.isEmpty() ? executionLogs.size() : matches;
        
        if (!normalized.isEmpty() && matches == 0) {
            codeArea.replaceText("未找到匹配的日志记录\n尝试缩短搜索词或更改日志级别筛选条件。");
        }
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
            String baseMessage = message == null ? "" : message;
            String target = raw ? baseMessage :
                ((timestamp != null ? timestamp : "") + " " + 
                 (level != null ? level : "") + " " + baseMessage);
            return target != null && target.toLowerCase().contains(keywordLower);
        }
    }
}

