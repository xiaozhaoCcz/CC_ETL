package com.cc.job.gui.view.component;

import com.cc.job.gui.service.ParameterAutocompleteService;
import com.cc.job.gui.util.ParameterInputStyleUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能参数输入控件
 * 
 * <p>支持自动补全、标签样式显示、智能删除和转义功能
 * 
 * @author cc-job-team
 * @since 2026-01-06
 */
public class SmartParameterInput extends TextArea {
    
    
    // 参数表达式模式：#{jobdesc}.{attr}
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("#\\{([^}]+)\\}\\.([^\\s}]+)");
    
    private final ParameterAutocompleteService autocompleteService;
    private Popup autocompletePopup;
    private ListView<String> suggestionListView;
    private ObservableList<String> suggestions;
    private int selectedIndex = -1;
    
    // 当前补全状态
    private AutocompleteState autocompleteState = AutocompleteState.NONE;
    private String currentPrefix = "";
    private int triggerPosition = -1;
    
    // 任务组ID（可选，用于获取任务组中的节点）
    private Long taskGroupId;
    
    /**
     * 自动补全状态
     */
    private enum AutocompleteState {
        NONE,           // 无补全
        JOB_DESC,       // 任务描述补全
        ATTRIBUTE       // 属性补全
    }
    
    public SmartParameterInput() {
        this.autocompleteService = new ParameterAutocompleteService();
        this.suggestions = FXCollections.observableArrayList();
        this.autocompletePopup = new Popup();
        
        initialize();
    }
    
    /**
     * 设置任务组ID
     */
    public void setTaskGroupId(Long taskGroupId) {
        this.taskGroupId = taskGroupId;
    }
    
    /**
     * 初始化控件
     */
    private void initialize() {
        // 设置样式
        setWrapText(true);
        
        // 创建自动补全列表
        suggestionListView = new ListView<>(suggestions);
        suggestionListView.setPrefWidth(300);
        suggestionListView.setPrefHeight(200);
        suggestionListView.setStyle(ParameterInputStyleUtil.AUTOCOMPLETE_POPUP_STYLE);
        
        // 设置列表项样式
        suggestionListView.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setStyle(ParameterInputStyleUtil.AUTOCOMPLETE_ITEM_STYLE);
                }
            }
        });
        
        // 列表项点击事件
        suggestionListView.setOnMouseClicked(e -> {
            String selected = suggestionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                insertSuggestion(selected);
                hideAutocomplete();
            }
        });
        
        // 将列表添加到弹出窗口
        VBox popupContent = new VBox();
        popupContent.getChildren().add(suggestionListView);
        popupContent.setStyle(ParameterInputStyleUtil.AUTOCOMPLETE_POPUP_STYLE);
        autocompletePopup.getContent().add(popupContent);
        
        // 监听文本变化
        textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> checkAutocompleteTrigger());
        });
        
        // 监听键盘事件
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        
        // 监听焦点变化
        focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                hideAutocomplete();
            }
        });
    }
    
    /**
     * 检查是否需要触发自动补全
     */
    private void checkAutocompleteTrigger() {
        String text = getText();
        int caretPosition = getCaretPosition();
        
        if (text == null || text.isEmpty()) {
            hideAutocomplete();
            return;
        }
        
        // 检查转义字符
        if (isEscaped(caretPosition)) {
            hideAutocomplete();
            return;
        }
        
        // 检查是否输入了 #
        if (caretPosition > 0 && text.charAt(caretPosition - 1) == '#') {
            // 检查是否是转义的 #
            if (caretPosition > 1 && text.charAt(caretPosition - 2) == '`') {
                hideAutocomplete();
                return;
            }
            
            // 触发任务描述补全
            triggerJobDescAutocomplete(caretPosition);
            return;
        }
        
        // 检查是否输入了 .（可能是属性补全）
        if (caretPosition > 0 && text.charAt(caretPosition - 1) == '.') {
            // 检查前面是否有 #{jobdesc} 模式
            String beforeDot = text.substring(0, caretPosition - 1);
            Matcher matcher = Pattern.compile("#\\{([^}]+)\\}$").matcher(beforeDot);
            if (matcher.find()) {
                String jobDesc = matcher.group(1);
                triggerAttributeAutocomplete(caretPosition, jobDesc);
                return;
            }
        }
        
        // 如果正在输入任务描述或属性，继续显示补全
        if (autocompleteState == AutocompleteState.JOB_DESC) {
            updateJobDescAutocomplete();
        } else if (autocompleteState == AutocompleteState.ATTRIBUTE) {
            updateAttributeAutocomplete();
        } else {
            hideAutocomplete();
        }
    }
    
    /**
     * 检查当前位置是否是转义的 #
     */
    private boolean isEscaped(int position) {
        String text = getText();
        if (position < 2 || text == null) {
            return false;
        }
        return text.charAt(position - 1) == '#' && text.charAt(position - 2) == '`';
    }
    
    /**
     * 触发任务描述补全
     */
    private void triggerJobDescAutocomplete(int position) {
        autocompleteState = AutocompleteState.JOB_DESC;
        triggerPosition = position;
        currentPrefix = "";
        
        // 异步加载任务描述列表
        CompletableFuture.supplyAsync(() -> {
            return autocompleteService.getJobDescSuggestions("", taskGroupId);
        }).thenAccept(jobDescs -> {
            Platform.runLater(() -> {
                suggestions.setAll(jobDescs);
                if (!jobDescs.isEmpty()) {
                    showAutocomplete();
                }
            });
        });
    }
    
    /**
     * 更新任务描述补全
     */
    private void updateJobDescAutocomplete() {
        String text = getText();
        int caretPosition = getCaretPosition();
        
        if (text == null || caretPosition <= triggerPosition) {
            hideAutocomplete();
            return;
        }
        
        // 提取当前输入的前缀（在 # 之后）
        String afterHash = text.substring(triggerPosition, caretPosition);
        
        // 检查是否输入了 {，开始提取任务描述
        int braceStart = afterHash.indexOf('{');
        if (braceStart >= 0) {
            int braceEnd = afterHash.indexOf('}', braceStart);
            if (braceEnd < 0) {
                // 还没有闭合，提取当前前缀
                currentPrefix = afterHash.substring(braceStart + 1);
            } else {
                // 已经闭合，检查是否有 .
                if (caretPosition < text.length() && text.charAt(caretPosition) == '.') {
                    // 准备属性补全
                    String jobDesc = afterHash.substring(braceStart + 1, braceEnd);
                    triggerAttributeAutocomplete(caretPosition + 1, jobDesc);
                    return;
                } else {
                    hideAutocomplete();
                    return;
                }
            }
        } else {
            currentPrefix = afterHash;
        }
        
        // 过滤建议
        CompletableFuture.supplyAsync(() -> {
            return autocompleteService.filterSuggestions(
                    autocompleteService.getJobDescSuggestions("", taskGroupId), 
                    currentPrefix);
        }).thenAccept(filtered -> {
            Platform.runLater(() -> {
                suggestions.setAll(filtered);
                if (filtered.isEmpty()) {
                    hideAutocomplete();
                } else {
                    showAutocomplete();
                }
            });
        });
    }
    
    /**
     * 触发属性补全
     */
    private void triggerAttributeAutocomplete(int position, String jobDesc) {
        autocompleteState = AutocompleteState.ATTRIBUTE;
        triggerPosition = position;
        currentPrefix = "";
        
        // 异步加载属性列表
        CompletableFuture.supplyAsync(() -> {
            return autocompleteService.getAttributeSuggestions("", jobDesc, taskGroupId);
        }).thenAccept(attributes -> {
            Platform.runLater(() -> {
                suggestions.setAll(attributes);
                if (!attributes.isEmpty()) {
                    showAutocomplete();
                }
            });
        });
    }
    
    /**
     * 更新属性补全
     */
    private void updateAttributeAutocomplete() {
        String text = getText();
        int caretPosition = getCaretPosition();
        
        if (text == null || caretPosition <= triggerPosition) {
            hideAutocomplete();
            return;
        }
        
        // 提取当前输入的前缀（在 . 之后）
        currentPrefix = text.substring(triggerPosition, caretPosition);
        
        // 检查是否输入了空格或其他字符，结束补全
        if (!currentPrefix.isEmpty() && 
            (currentPrefix.contains(" ") || currentPrefix.contains("\n") || 
             currentPrefix.contains("\t"))) {
            hideAutocomplete();
            return;
        }
        
        // 获取任务描述
        String beforeDot = text.substring(0, triggerPosition - 1);
        Matcher matcher = Pattern.compile("#\\{([^}]+)\\}$").matcher(beforeDot);
        String jobDesc = matcher.find() ? matcher.group(1) : "";
        
        // 过滤建议
        CompletableFuture.supplyAsync(() -> {
            return autocompleteService.filterSuggestions(
                    autocompleteService.getAttributeSuggestions("", jobDesc, taskGroupId),
                    currentPrefix);
        }).thenAccept(filtered -> {
            Platform.runLater(() -> {
                suggestions.setAll(filtered);
                if (filtered.isEmpty()) {
                    hideAutocomplete();
                } else {
                    showAutocomplete();
                }
            });
        });
    }
    
    /**
     * 显示自动补全弹出窗口
     */
    private void showAutocomplete() {
        if (autocompletePopup.isShowing()) {
            return;
        }
        
        Bounds bounds = localToScreen(getBoundsInLocal());
        autocompletePopup.show(this, bounds.getMinX(), bounds.getMaxY() + 2);
        
        // 重置选中索引
        selectedIndex = -1;
        suggestionListView.getSelectionModel().clearSelection();
    }
    
    /**
     * 隐藏自动补全弹出窗口
     */
    private void hideAutocomplete() {
        if (autocompletePopup.isShowing()) {
            autocompletePopup.hide();
        }
        autocompleteState = AutocompleteState.NONE;
        selectedIndex = -1;
    }
    
    /**
     * 插入建议
     */
    private void insertSuggestion(String suggestion) {
        String text = getText();
        int caretPosition = getCaretPosition();
        
        if (autocompleteState == AutocompleteState.JOB_DESC) {
            // 插入任务描述
            // 找到 # 的位置
            int hashPos = text.lastIndexOf('#', caretPosition - 1);
            if (hashPos >= 0) {
                // 替换从 # 到当前位置的内容
                String before = text.substring(0, hashPos + 1);
                String after = text.substring(caretPosition);
                
                // 检查是否已经有 {
                if (before.endsWith("#")) {
                    setText(before + "{" + suggestion + "}" + after);
                    positionCaret(hashPos + 1 + suggestion.length() + 2);
                } else {
                    // 已经有 {，替换其中的内容
                    int braceStart = before.indexOf('{', hashPos);
                    if (braceStart >= 0) {
                        String beforeBrace = before.substring(0, braceStart + 1);
                        setText(beforeBrace + suggestion + "}" + after);
                        positionCaret(beforeBrace.length() + suggestion.length() + 1);
                    }
                }
            }
        } else if (autocompleteState == AutocompleteState.ATTRIBUTE) {
            // 插入属性
            int dotPos = text.lastIndexOf('.', caretPosition - 1);
            if (dotPos >= 0) {
                String before = text.substring(0, dotPos + 1);
                String after = text.substring(caretPosition);
                setText(before + suggestion + after);
                positionCaret(dotPos + 1 + suggestion.length());
            }
        }
        
        hideAutocomplete();
    }
    
    /**
     * 处理键盘事件
     */
    private void handleKeyPressed(KeyEvent event) {
        if (autocompletePopup.isShowing()) {
            if (event.getCode() == KeyCode.UP) {
                event.consume();
                navigateSuggestions(-1);
            } else if (event.getCode() == KeyCode.DOWN) {
                event.consume();
                navigateSuggestions(1);
            } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                event.consume();
                String selected = suggestionListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    insertSuggestion(selected);
                } else if (!suggestions.isEmpty()) {
                    insertSuggestion(suggestions.get(0));
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                hideAutocomplete();
            }
        } else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
            // 智能删除
            handleSmartDelete(event);
        }
    }
    
    /**
     * 导航建议列表
     */
    private void navigateSuggestions(int direction) {
        if (suggestions.isEmpty()) {
            return;
        }
        
        selectedIndex += direction;
        if (selectedIndex < 0) {
            selectedIndex = suggestions.size() - 1;
        } else if (selectedIndex >= suggestions.size()) {
            selectedIndex = 0;
        }
        
        suggestionListView.getSelectionModel().select(selectedIndex);
        suggestionListView.scrollTo(selectedIndex);
    }
    
    /**
     * 智能删除处理
     */
    private void handleSmartDelete(KeyEvent event) {
        String text = getText();
        int caretPosition = getCaretPosition();
        
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // 检查光标位置是否在参数表达式内
        Matcher matcher = PARAMETER_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            if (caretPosition > start && caretPosition <= end) {
                // 在表达式内，删除整个表达式
                event.consume();
                
                String before = text.substring(0, start);
                String after = text.substring(end);
                setText(before + after);
                positionCaret(start);
                
                return;
            }
        }
        
        // 检查是否在属性部分（.后面的部分）
        int dotPos = text.lastIndexOf('.', caretPosition - 1);
        if (dotPos >= 0) {
            // 检查 . 前面是否有 #{jobdesc} 模式
            String beforeDot = text.substring(0, dotPos);
            Matcher beforeMatcher = Pattern.compile("#\\{([^}]+)\\}$").matcher(beforeDot);
            if (beforeMatcher.find()) {
                // 在属性部分，删除整个属性
                int attrStart = dotPos + 1;
                int attrEnd = caretPosition;
                
                // 找到属性的结束位置（空格、换行或字符串结束）
                for (int i = caretPosition; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == ' ' || c == '\n' || c == '\t' || c == '}' || c == '.') {
                        attrEnd = i;
                        break;
                    }
                }
                
                if (attrEnd > attrStart) {
                    event.consume();
                    String before = text.substring(0, attrStart);
                    String after = text.substring(attrEnd);
                    setText(before + after);
                    positionCaret(attrStart);
                    return;
                }
            }
        }
    }
    
    /**
     * 获取实际文本（处理转义）
     * 将 `# 转换为普通 #
     */
    public String getActualText() {
        String text = super.getText();
        if (text == null) {
            return null;
        }
        // 将 `# 转换为 #
        return text.replace("`#", "#");
    }
    
    /**
     * 设置文本（处理转义）
     * 将普通 # 转换为 `#（如果不在表达式中）
     */
    public void setTextWithEscape(String text) {
        if (text == null) {
            super.setText(null);
            return;
        }
        
        // 先处理转义：将 `# 转换为临时标记
        text = text.replace("`#", "\u0001ESCAPE_HASH\u0001");
        
        // 将表达式中的 # 保持不变，其他 # 转换为 `#
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PARAMETER_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (matcher.find()) {
            // 添加表达式之前的内容，将 # 转换为 `#
            String before = text.substring(lastEnd, matcher.start());
            before = before.replace("#", "`#");
            sb.append(before);
            
            // 添加表达式（保持不变）
            sb.append(matcher.group());
            
            lastEnd = matcher.end();
        }
        
        // 添加剩余内容
        String remaining = text.substring(lastEnd);
        remaining = remaining.replace("#", "`#");
        sb.append(remaining);
        
        // 恢复转义标记
        String result = sb.toString().replace("\u0001ESCAPE_HASH\u0001", "`#");
        
        super.setText(result);
    }
}

