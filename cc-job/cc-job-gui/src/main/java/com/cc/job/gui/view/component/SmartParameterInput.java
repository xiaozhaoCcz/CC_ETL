package com.cc.job.gui.view.component;

import com.cc.job.gui.service.ParameterAutocompleteService;
import com.cc.job.gui.util.ParameterInputStyleUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 智能参数输入控件
 * 
 * <p>支持自动补全、语法高亮、智能删除和转义功能
 * 
 * @author cc-job-team
 * @since 2026-01-06
 */
public class SmartParameterInput extends CodeArea {
    
    
    // 参数表达式模式：#{jobdesc}.{attr}
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("#\\{([^}]+)\\}\\.([^\\s}]+)");
    
    private final ParameterAutocompleteService autocompleteService;
    private Popup autocompletePopup;
    private ListView<ParameterAutocompleteService.SuggestionItem> suggestionListView;
    private ObservableList<ParameterAutocompleteService.SuggestionItem> suggestions;
    private int selectedIndex = -1;
    
    // 当前补全状态
    private AutocompleteState autocompleteState = AutocompleteState.NONE;
    private String currentPrefix = "";
    private int triggerPosition = -1;
    
    // 当前属性路径（用于链式调用，如 "data" 或 "data.range"）
    private String currentAttributePath = "";
    
    // 任务组ID（可选，用于获取任务组中的节点）
    private Long taskGroupId;
    
    // 提示文本（CodeArea不支持promptText，但为了兼容性保留）
    private String promptText;
    
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
     * 设置提示文本（兼容方法）
     * 注意：CodeArea 不支持 promptText，此方法仅用于兼容性，不会实际显示提示文本
     */
    public void setPromptText(String promptText) {
        this.promptText = promptText;
        // CodeArea 不支持 promptText，但我们可以通过其他方式实现占位符
        // 这里暂时只存储，不实际显示
    }
    
    /**
     * 获取提示文本
     */
    public String getPromptText() {
        return promptText;
    }
    
    /**
     * 设置首选行数（兼容方法）
     * CodeArea 不支持 prefRowCount，通过设置 prefHeight 来实现类似效果
     */
    public void setPrefRowCount(int rows) {
        // 估算每行高度（包括行间距），大约 20-25 像素
        double estimatedRowHeight = 22.0;
        setPrefHeight(rows * estimatedRowHeight);
    }
    
    /**
     * 初始化控件
     */
    private void initialize() {
        // 设置样式
        setWrapText(true);
        
        // 加载样式表，确保语法高亮样式可用
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            getStylesheets().add(css);
        } catch (Exception e) {
            // 如果样式表加载失败，记录但不影响功能
            System.err.println("加载样式表失败: " + e.getMessage());
        }
        
        // 添加样式类，用于语法高亮
        getStyleClass().add("parameter-input");
        
        // 应用输入框基础样式
        setStyle(ParameterInputStyleUtil.INPUT_FIELD_STYLE + " " + 
                 ParameterInputStyleUtil.SELECTION_BACKGROUND_STYLE);
        
        // 监听焦点变化以应用焦点样式
        focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                setStyle(ParameterInputStyleUtil.INPUT_FIELD_STYLE + " " + 
                        ParameterInputStyleUtil.INPUT_FIELD_FOCUSED_STYLE + " " +
                        ParameterInputStyleUtil.SELECTION_BACKGROUND_STYLE);
            } else {
                setStyle(ParameterInputStyleUtil.INPUT_FIELD_STYLE + " " + 
                        ParameterInputStyleUtil.SELECTION_BACKGROUND_STYLE);
            }
        });
        
        // 创建自动补全列表
        suggestionListView = new ListView<>(suggestions);
        suggestionListView.setPrefWidth(300);
        suggestionListView.setPrefHeight(200);
        suggestionListView.setStyle(ParameterInputStyleUtil.AUTOCOMPLETE_POPUP_STYLE);
        
        // 设置列表项样式（显示节点名称和类型标签）
        suggestionListView.setCellFactory(listView -> new ListCell<ParameterAutocompleteService.SuggestionItem>() {
            @Override
            protected void updateItem(ParameterAutocompleteService.SuggestionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    // 创建 HBox 容器
                    HBox container = new HBox(8);
                    container.setAlignment(Pos.CENTER_LEFT);
                    
                    // 节点名称标签
                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle(ParameterInputStyleUtil.AUTOCOMPLETE_ITEM_STYLE);
                    container.getChildren().add(nameLabel);
                    
                    // 节点类型标签（如果类型不为空）
                    if (item.getType() != null && !item.getType().isEmpty()) {
                        Label typeLabel = new Label("[" + item.getType() + "]");
                        String typeStyle = getNodeTypeStyle(item.getType());
                        typeLabel.setStyle(typeStyle);
                        container.getChildren().add(typeLabel);
                    }
                    setGraphic(container);
                    setText(null); // 清空文本，使用图形
                    setStyle(ParameterInputStyleUtil.AUTOCOMPLETE_ITEM_STYLE);
                }
            }
        });
        
        // 列表项点击事件
        suggestionListView.setOnMouseClicked(e -> {
            ParameterAutocompleteService.SuggestionItem selected = suggestionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                insertSuggestion(selected.getName());
                hideAutocomplete();
            }
        });
        
        // 将列表添加到弹出窗口
        VBox popupContent = new VBox();
        popupContent.getChildren().add(suggestionListView);
        popupContent.setStyle(ParameterInputStyleUtil.AUTOCOMPLETE_POPUP_STYLE);
        autocompletePopup.getContent().add(popupContent);
        
        // 监听文本变化（用于自动补全和语法高亮）
        // CodeArea 的 textProperty() 仍然可用
        textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                checkAutocompleteTrigger();
                // 延迟应用语法高亮，避免频繁更新
                Platform.runLater(() -> applySyntaxHighlighting());
            });
        });
        
        // 初始应用语法高亮
        Platform.runLater(() -> applySyntaxHighlighting());
        
        // 监听键盘事件
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        
        // 监听焦点变化（隐藏自动补全）
        focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                hideAutocomplete();
            }
        });
    }
    
    /**
     * 应用语法高亮
     */
    private void applySyntaxHighlighting() {
        String text = getText();
        if (text == null || text.isEmpty()) {
            // 清空样式
            try {
                StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
                spansBuilder.add(Collections.emptyList(), 0);
                setStyleSpans(0, spansBuilder.create());
            } catch (Exception e) {
                // 忽略错误
            }
            return;
        }
        
        try {
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            
            // 参数表达式模式：#{jobdesc}.{attr} 或 #{jobdesc}.{attr}.{subAttr} 等
            // 支持方法调用：#{jobdesc}.data.get(0)
            // 匹配完整的参数表达式，包括嵌套属性和方法调用
            // 改进的正则：匹配 #{node} 后面至少有一个 .attr 的情况
            // 支持：#{node}.attr, #{node}.attr1.attr2, #{node}.data.get(0) 等
            Pattern paramPattern = Pattern.compile("#\\{([^}]+)\\}(\\.[^\\s}()]+(?:\\([^)]*\\))?)+");
            
            int lastEnd = 0;
            Matcher matcher = paramPattern.matcher(text);
            
            while (matcher.find()) {
                // 检查是否是转义的 #（前面有 `）
                int matchStart = matcher.start();
                if (matchStart > 0 && text.charAt(matchStart - 1) == '`') {
                    // 这是转义的 #，不进行高亮
                    if (matchStart > lastEnd) {
                        spansBuilder.add(Collections.emptyList(), matchStart - lastEnd);
                    }
                    spansBuilder.add(Collections.emptyList(), matcher.end() - matchStart);
                    lastEnd = matcher.end();
                    continue;
                }
                
                // 添加普通文本（在参数表达式之前）
                if (matcher.start() > lastEnd) {
                    spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);
                }
                
                // 添加高亮的参数表达式
                // 使用 parameter-expression 样式类
                spansBuilder.add(Collections.singleton("parameter-expression"), 
                        matcher.end() - matcher.start());
                lastEnd = matcher.end();
            }
            
            // 添加剩余文本
            if (lastEnd < text.length()) {
                spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
            }
            
            StyleSpans<Collection<String>> styleSpans = spansBuilder.create();
            if (styleSpans.length() > 0) {
                setStyleSpans(0, styleSpans);
            }
        } catch (Exception e) {
            // 忽略语法高亮错误，不影响功能
            // 如果高亮失败，至少确保文本可以正常显示
        }
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
            // 检查前面是否有 #{jobdesc} 模式，支持链式调用
            String beforeDot = text.substring(0, caretPosition - 1);
            Matcher matcher = Pattern.compile("#\\{([^}]+)\\}(.*)$").matcher(beforeDot);
            if (matcher.find()) {
                String jobDesc = matcher.group(1);
                String attributePath = matcher.group(2); // 可能是 "data" 或 "data.range" 等
                // 移除开头的点号（如果有）
                if (attributePath.startsWith(".")) {
                    attributePath = attributePath.substring(1);
                }
                triggerAttributeAutocomplete(caretPosition, jobDesc, attributePath);
                return;
            }
        }
        
        // 检查是否在输入属性（在 . 之后输入字符）
        if (autocompleteState == AutocompleteState.ATTRIBUTE && caretPosition > triggerPosition) {
            // 检查是否还在属性路径中（没有遇到空格、换行等）
            String afterTrigger = text.substring(triggerPosition, caretPosition);
            if (!afterTrigger.contains(" ") && !afterTrigger.contains("\n") && !afterTrigger.contains("\t")) {
                updateAttributeAutocomplete();
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
        
        // 异步加载任务描述列表（包含类型信息）
        CompletableFuture.supplyAsync(() -> {
            return autocompleteService.getJobDescSuggestionItems("", taskGroupId);
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
        
        // 过滤建议（包含类型信息）
        CompletableFuture.supplyAsync(() -> {
            List<ParameterAutocompleteService.SuggestionItem> allItems = 
                    autocompleteService.getJobDescSuggestionItems("", taskGroupId);
            return filterSuggestionItems(allItems, currentPrefix);
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
     * @param position 光标位置
     * @param jobDesc 任务描述
     * @param attributePath 属性路径（如 "data" 或 "data.range"），如果为空则补全第一级属性
     */
    private void triggerAttributeAutocomplete(int position, String jobDesc, String attributePath) {
        autocompleteState = AutocompleteState.ATTRIBUTE;
        triggerPosition = position;
        currentPrefix = "";
        currentAttributePath = attributePath != null ? attributePath : "";
        
        // 异步加载属性列表
        final String pathToUse = currentAttributePath;
        CompletableFuture.supplyAsync(() -> {
            List<String> attributes = autocompleteService.getAttributeSuggestions("", jobDesc, taskGroupId, pathToUse);
            return attributes.stream()
                    .map(attr -> new ParameterAutocompleteService.SuggestionItem(attr, ""))
                    .collect(Collectors.toList());
        }).thenAccept(attributeItems -> {
            Platform.runLater(() -> {
                suggestions.setAll(attributeItems);
                if (!attributeItems.isEmpty()) {
                    showAutocomplete();
                } else {
                    // 如果没有建议，隐藏补全框
                    hideAutocomplete();
                }
            });
        });
    }
    
    /**
     * 触发属性补全（兼容旧方法，用于第一级属性补全）
     */
    private void triggerAttributeAutocomplete(int position, String jobDesc) {
        triggerAttributeAutocomplete(position, jobDesc, "");
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
        
        // 获取任务描述和属性路径
        // triggerPosition 是 . 的位置，所以 beforeTrigger 是 #{node}.data 这样的格式
        String beforeTrigger = text.substring(0, triggerPosition - 1);
        Matcher matcher = Pattern.compile("#\\{([^}]+)\\}(.*)$").matcher(beforeTrigger);
        String jobDesc = "";
        String attributePath = currentAttributePath;
        
        if (matcher.find()) {
            jobDesc = matcher.group(1);
            String pathPart = matcher.group(2);
            // 如果路径部分不为空，提取属性路径（去掉开头的点号）
            if (pathPart != null && !pathPart.isEmpty()) {
                if (pathPart.startsWith(".")) {
                    pathPart = pathPart.substring(1);
                }
                // pathPart 现在应该是 "data" 或 "data.range" 这样的格式
                // 如果 currentPrefix 不为空，说明用户正在输入，需要从 pathPart 中移除 currentPrefix
                if (!pathPart.isEmpty()) {
                    if (currentPrefix.isEmpty()) {
                        // 用户刚输入 .，pathPart 就是完整的属性路径
                        attributePath = pathPart;
                    } else {
                        // 用户正在输入属性名，pathPart 包含完整路径，需要移除当前前缀
                        // 例如：pathPart = "data.ran", currentPrefix = "ran", attributePath 应该是 "data"
                        if (pathPart.endsWith(currentPrefix)) {
                            attributePath = pathPart.substring(0, pathPart.length() - currentPrefix.length());
                            // 移除末尾的点号（如果有）
                            if (attributePath.endsWith(".")) {
                                attributePath = attributePath.substring(0, attributePath.length() - 1);
                            }
                        } else {
                            // 如果格式不匹配，使用 pathPart 作为属性路径
                            attributePath = pathPart;
                        }
                    }
                }
            }
        }
        
        // 过滤建议（转换为 SuggestionItem，类型为空）
        // 将这些变量声明为 final，以便在 lambda 表达式中使用
        final String prefixToUse = currentPrefix;
        final String pathToUse = attributePath != null ? attributePath : "";
        final String jobDescFinal = jobDesc != null ? jobDesc : "";
        CompletableFuture.supplyAsync(() -> {
            List<String> attributes = autocompleteService.getAttributeSuggestions(prefixToUse, jobDescFinal, taskGroupId, pathToUse);
            List<ParameterAutocompleteService.SuggestionItem> attributeItems = attributes.stream()
                    .map(attr -> new ParameterAutocompleteService.SuggestionItem(attr, ""))
                    .collect(Collectors.toList());
            return filterSuggestionItems(attributeItems, prefixToUse);
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
                    String newText = before + "{" + suggestion + "}" + after;
                    replaceText(0, getLength(), newText);
                    moveTo(hashPos + 1 + suggestion.length() + 2);
                } else {
                    // 已经有 {，替换其中的内容
                    int braceStart = before.indexOf('{', hashPos);
                    if (braceStart >= 0) {
                        String beforeBrace = before.substring(0, braceStart + 1);
                        String newText = beforeBrace + suggestion + "}" + after;
                        replaceText(0, getLength(), newText);
                        moveTo(beforeBrace.length() + suggestion.length() + 1);
                    }
                }
            }
        } else if (autocompleteState == AutocompleteState.ATTRIBUTE) {
            // 插入属性（支持链式调用）
            // 找到最近的 . 位置（可能是 #{node}. 或 #{node}.data.）
            int dotPos = text.lastIndexOf('.', caretPosition - 1);
            if (dotPos >= 0) {
                String before = text.substring(0, dotPos + 1);
                String after = text.substring(caretPosition);
                String newText = before + suggestion + after;
                replaceText(0, getLength(), newText);
                // 移动光标到插入的文本之后
                moveTo(dotPos + 1 + suggestion.length());
            } else {
                // 如果没有找到 .，可能是在 #{node} 之后直接输入属性
                // 查找 #{node} 的位置
                String beforeTrigger = text.substring(0, triggerPosition - 1);
                Matcher matcher = Pattern.compile("#\\{([^}]+)\\}$").matcher(beforeTrigger);
                if (matcher.find()) {
                    int nodeEnd = matcher.end();
                    String before = text.substring(0, nodeEnd);
                    String after = text.substring(caretPosition);
                    String newText = before + "." + suggestion + after;
                    replaceText(0, getLength(), newText);
                    moveTo(nodeEnd + 1 + suggestion.length());
                }
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
                ParameterAutocompleteService.SuggestionItem selected = suggestionListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    insertSuggestion(selected.getName());
                } else if (!suggestions.isEmpty()) {
                    insertSuggestion(suggestions.get(0).getName());
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
        boolean isBackspace = event.getCode() == KeyCode.BACK_SPACE;
        
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // 根据删除方向调整光标位置
        int checkPosition = isBackspace ? caretPosition - 1 : caretPosition;
        if (checkPosition < 0 || checkPosition >= text.length()) {
            return;
        }
        
        // 场景1: 检查是否在属性部分（支持链式调用，如 #{jobdesc}.data.get 中的 get 部分）
        // 查找光标前最近的 . 号（可能是第一级或链式调用的任何一级）
        int dotPos = -1;
        for (int i = checkPosition; i >= 0; i--) {
            if (i < text.length() && text.charAt(i) == '.') {
                // 检查 . 前面是否有 #{jobdesc} 模式（支持链式调用）
                String beforeDot = text.substring(0, i);
                // 匹配 #{jobdesc} 或 #{jobdesc}.attr1.attr2 等链式调用
                Matcher beforeMatcher = Pattern.compile("#\\{([^}]+)\\}(\\.[^\\s}()]+)*$").matcher(beforeDot);
                if (beforeMatcher.find()) {
                    dotPos = i;
                    break;
                }
            }
        }
        
        if (dotPos >= 0) {
            // 在属性部分，只删除当前属性（不删除前面的路径）
            int attrStart = dotPos + 1;
            int attrEnd = isBackspace ? caretPosition : caretPosition + 1;
            
            // 找到当前属性的结束位置（空格、换行、制表符、}、. 或字符串结束）
            if (!isBackspace) {
                // 向前查找属性结束位置
                for (int i = caretPosition + 1; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == ' ' || c == '\n' || c == '\t' || c == '}' || c == '.') {
                        attrEnd = i;
                        break;
                    }
                }
            } else {
                // Backspace：需要找到当前属性的开始位置（从光标向前查找）
                // 属性名可能包含字母、数字、下划线，还可能包含方法调用的括号
                for (int i = caretPosition - 1; i >= attrStart; i--) {
                    char c = text.charAt(i);
                    // 如果遇到非属性字符（空格、换行、制表符、}、.），说明属性开始位置在 i+1
                    if (c == ' ' || c == '\n' || c == '\t' || c == '}' || c == '.') {
                        attrStart = i + 1;
                        break;
                    }
                }
            }
            
            // 确保在属性范围内
            if (attrEnd > attrStart && checkPosition >= attrStart && checkPosition <= attrEnd) {
                event.consume();
                String before = text.substring(0, attrStart);
                String after = text.substring(attrEnd);
                String newText = before + after;
                replaceText(0, getLength(), newText);
                moveTo(attrStart);
                return;
            }
        }
        
        // 场景2: 检查是否在 #{jobdesc} 的 } 后面或 } 前面
        // 查找光标前最近的 # 号
        int hashPos = -1;
        for (int i = checkPosition; i >= 0; i--) {
            if (i < text.length() && text.charAt(i) == '#') {
                // 检查是否是转义的 #
                if (i > 0 && text.charAt(i - 1) == '`') {
                    continue;
                }
                hashPos = i;
                break;
            }
        }
        
        if (hashPos >= 0) {
            // 检查 # 后面是否有 {jobdesc} 模式
            String afterHash = text.substring(hashPos);
            Matcher braceMatcher = Pattern.compile("#\\{([^}]*)\\??\\}").matcher(afterHash);
            if (braceMatcher.find()) {
                int braceStart = hashPos + braceMatcher.start() + 1; // { 的位置
                int braceEnd = hashPos + braceMatcher.end() - 1; // } 的位置
                
                // 检查光标是否在 } 后面（紧挨着）或 } 前面（在 {jobdesc} 内部）
                if ((isBackspace && caretPosition == braceEnd + 1) || 
                    (checkPosition >= braceStart && checkPosition <= braceEnd)) {
                    // 删除整个 {jobdesc}，保留 #
                    event.consume();
                    String before = text.substring(0, hashPos + 1);
                    String after = text.substring(braceEnd + 1);
                    String newText = before + after;
                    replaceText(0, getLength(), newText);
                    moveTo(hashPos + 1);
                    return;
                }
            }
        }
        
        // 场景3: 检查是否在链式调用的 . 后面（如 #{jobdesc}.data. 或 #{jobdesc}.data.get.）
        if (checkPosition > 0 && text.charAt(checkPosition - 1) == '.') {
            String beforeDot = text.substring(0, checkPosition - 1);
            
            // 检查是否是链式调用的中间层级（如 #{jobdesc}.data.）
            // 匹配 #{jobdesc}.attr1.attr2 等链式调用
            Matcher chainMatcher = Pattern.compile("#\\{([^}]+)\\}(\\.[^\\s}()]+)+$").matcher(beforeDot);
            if (chainMatcher.find()) {
                // 是链式调用，找到最近的 . 位置，删除最后一个属性及其点号
                int lastDotPos = beforeDot.lastIndexOf('.');
                if (lastDotPos >= 0) {
                    // 检查最后一个 . 前面是否有 #{jobdesc} 模式
                    String beforeLastDot = beforeDot.substring(0, lastDotPos);
                    Matcher beforeLastDotMatcher = Pattern.compile("#\\{([^}]+)\\}(\\.[^\\s}()]+)*$").matcher(beforeLastDot);
                    if (beforeLastDotMatcher.find()) {
                        // 删除最后一个属性及其点号（如 .get）
                        event.consume();
                        String before = text.substring(0, lastDotPos + 1);
                        String after = text.substring(checkPosition);
                        String newText = before + after;
                        replaceText(0, getLength(), newText);
                        moveTo(lastDotPos + 1);
                        return;
                    }
                }
            }
            
            // 检查 . 前面是否直接是 #{jobdesc} 模式（第一级属性）
            Matcher firstLevelMatcher = Pattern.compile("#\\{([^}]+)\\}$").matcher(beforeDot);
            if (firstLevelMatcher.find()) {
                // 删除 . 和前面的 {jobdesc}，保留 #
                event.consume();
                int hashPos2 = firstLevelMatcher.start();
                String before = text.substring(0, hashPos2 + 1);
                String after = text.substring(checkPosition);
                String newText = before + after;
                replaceText(0, getLength(), newText);
                moveTo(hashPos2 + 1);
                return;
            }
        }
    }
    
    /**
     * 获取节点类型对应的样式
     */
    private String getNodeTypeStyle(String type) {
        if (type == null) {
            return ParameterInputStyleUtil.NODE_TYPE_OTHER_STYLE;
        }
        return switch (type.toUpperCase()) {
            case "SQL" -> ParameterInputStyleUtil.NODE_TYPE_SQL_STYLE;
            case "API" -> ParameterInputStyleUtil.NODE_TYPE_API_STYLE;
            case "BEAN" -> ParameterInputStyleUtil.NODE_TYPE_BEAN_STYLE;
            default -> ParameterInputStyleUtil.NODE_TYPE_OTHER_STYLE;
        };
    }
    
    /**
     * 过滤建议项列表
     */
    private List<ParameterAutocompleteService.SuggestionItem> filterSuggestionItems(
            List<ParameterAutocompleteService.SuggestionItem> items, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return items;
        }
        String lowerPrefix = prefix.toLowerCase();
        return items.stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerPrefix))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 获取实际文本（处理转义）
     * 将 `# 转换为普通 #
     */
    public String getActualText() {
        String text = getText();
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
            replaceText(0, getLength(), "");
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
        
        replaceText(0, getLength(), result);
    }
}

