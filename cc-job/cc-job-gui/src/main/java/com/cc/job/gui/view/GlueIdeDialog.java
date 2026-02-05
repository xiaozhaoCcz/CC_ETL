package com.cc.job.gui.view;

import com.cc.job.gui.service.JobInfoService;
import com.cc.job.xo.model.entity.JobLogglue;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GLUE IDE 对话框
 * 用于编辑和保存 GLUE 源代码
 */
public class GlueIdeDialog extends Dialog<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(GlueIdeDialog.class);
    
    private final Long taskId;
    private final String glueType; // GLUE类型，用于过滤历史记录
    private final JobInfoService jobInfoService;
    
    private CodeArea codeEditorArea;
    private TextField remarkField;
    private ComboBox<JobLogglue> historyCombo;
    private ObservableList<JobLogglue> historyList;
    
    private String initialCode;
    private String initialRemark;
    
    // 语法高亮模式（支持多种语言）
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("\\b(if|else|for|while|do|switch|case|break|continue|return|class|public|private|protected|static|final|void|int|String|boolean|true|false|null|new|this|super|extends|implements|import|package|try|catch|finally|throw|throws)\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//[^\n]*|/\\*(.|\\R)*?\\*/");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b");
    
    private ButtonType saveButtonType;
    private ButtonType cancelButtonType;
    
    public GlueIdeDialog(Stage owner, Long taskId, String initialCode, String initialRemark) {
        this(owner, taskId, initialCode, initialRemark, null);
    }
    
    public GlueIdeDialog(Stage owner, Long taskId, String initialCode, String initialRemark, String glueType) {
        this.taskId = taskId;
        this.glueType = glueType;
        this.initialCode = initialCode;
        this.initialRemark = initialRemark;
        this.jobInfoService = new JobInfoService();
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("GLUE IDE");
        setHeaderText(null);
        
        // 创建对话框内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        // 添加按钮
        saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, saveButtonType);
        
        // 设置样式
        styleDialog();
        
        // 填充初始数据
        if (initialCode != null) {
            codeEditorArea.replaceText(initialCode);
            Platform.runLater(() -> {
                codeEditorArea.setStyleSpans(0, computeHighlighting(initialCode));
            });
        }
        if (initialRemark != null) {
            remarkField.setText(initialRemark);
        }
        
        // 加载历史记录
        loadHistory();
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                // 只验证备注，不保存到数据库，代码会通过 getCode() 返回给调用者
                if (!validateBeforeClose()) {
                    return null; // 验证失败，不关闭对话框
                }
            }
            return null;
        });
        
        // 监听历史记录选择
        historyCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String code = newVal.getGlueSource() != null ? newVal.getGlueSource() : "";
                codeEditorArea.replaceText(code);
                Platform.runLater(() -> {
                    codeEditorArea.setStyleSpans(0, computeHighlighting(code));
                });
                remarkField.setText(newVal.getGlueRemark() != null ? newVal.getGlueRemark() : "");
            }
        });
    }
    
    /**
     * 计算语法高亮
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        if (text == null || text.isEmpty()) {
            spansBuilder.add(Collections.emptyList(), 0);
            return spansBuilder.create();
        }
        
        Matcher matcher = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN.pattern() + ")" +
            "|(?<STRING>" + STRING_PATTERN.pattern() + ")" +
            "|(?<NUMBER>" + NUMBER_PATTERN.pattern() + ")" +
            "|(?<COMMENT>" + COMMENT_PATTERN.pattern() + ")" +
            "|(?<CLASS>" + CLASS_PATTERN.pattern() + ")"
        ).matcher(text);
        
        int lastKwEnd = 0;
        while (matcher.find()) {
            String styleClass = null;
            if (matcher.group("KEYWORD") != null) {
                styleClass = "keyword";
            } else if (matcher.group("STRING") != null) {
                styleClass = "string";
            } else if (matcher.group("NUMBER") != null) {
                styleClass = "number";
            } else if (matcher.group("COMMENT") != null) {
                styleClass = "comment";
            } else if (matcher.group("CLASS") != null) {
                styleClass = "class-name";
            }
            
            if (styleClass != null) {
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
                lastKwEnd = matcher.end();
            }
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    
    /**
     * 创建对话框内容
     */
    private VBox createContent() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(800);
        container.setPrefHeight(600);
        
        // 代码编辑器
        Label codeLabel = new Label("代码编辑区");
        codeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        
        // 使用 CodeArea 实现语法高亮
        codeEditorArea = new CodeArea();
        codeEditorArea.setPrefHeight(500); // 设置高度而不是行数
        codeEditorArea.setWrapText(false);
        
        // 设置行号
        codeEditorArea.setParagraphGraphicFactory(LineNumberFactory.get(codeEditorArea));
        
        // 优化代码编辑器样式：黑色背景，白色字体，等宽字体
        codeEditorArea.setStyle(
            "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
            "-fx-font-size: 14; " +
            "-fx-background-color: #000000; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-border-color: #333333; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        
        // 设置代码区域样式
        String cssUrl = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            codeEditorArea.getStylesheets().add(cssUrl);
        }
        codeEditorArea.getStyleClass().add("code-editor");
        
        // 监听文本变化，实现语法高亮
        codeEditorArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                codeEditorArea.setStyleSpans(0, computeHighlighting(newText));
            });
        });
        
        // 初始高亮
        if (initialCode != null && !initialCode.isEmpty()) {
            Platform.runLater(() -> {
                codeEditorArea.setStyleSpans(0, computeHighlighting(initialCode));
            });
        }
        
        // 备注输入框
        Label remarkLabel = new Label("备注*");
        remarkLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
        
        remarkField = new TextField();
        remarkField.setPrefWidth(600);
        remarkField.setPromptText("请输入备注");
        
        // 选择历史下拉框
        Label historyLabel = new Label("选择历史");
        historyLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
        
        historyList = FXCollections.observableArrayList();
        historyCombo = new ComboBox<>(historyList);
        historyCombo.setPrefWidth(600);
        historyCombo.setPromptText("请选择历史记录");
        historyCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(JobLogglue item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String remark = item.getGlueRemark();
                    if (remark == null || remark.trim().isEmpty()) {
                        remark = "无备注";
                    }
                    setText(remark);
                }
            }
        });
        historyCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JobLogglue item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String remark = item.getGlueRemark();
                    if (remark == null || remark.trim().isEmpty()) {
                        remark = "无备注";
                    }
                    setText(remark);
                }
            }
        });
        
        // 布局：备注和选择历史在同一行
        GridPane bottomGrid = new GridPane();
        bottomGrid.setHgap(15);
        bottomGrid.setVgap(10);
        bottomGrid.setPadding(new Insets(10, 0, 0, 0));
        
        // 备注输入框
        remarkField.setPrefWidth(350);
        bottomGrid.add(remarkLabel, 0, 0);
        bottomGrid.add(remarkField, 1, 0);
        
        // 选择历史下拉框
        historyCombo.setPrefWidth(350);
        bottomGrid.add(historyLabel, 2, 0);
        bottomGrid.add(historyCombo, 3, 0);
        
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));
        bottomBox.getChildren().add(bottomGrid);
        
        container.getChildren().addAll(codeLabel, codeEditorArea, bottomBox);
        
        return container;
    }
    
    /**
     * 加载历史记录
     */
    private void loadHistory() {
        if (taskId == null) {
            // 新建任务时，历史记录下拉框禁用
            historyCombo.setDisable(true);
            historyCombo.setPromptText("请先保存任务后再查看历史记录");
            return;
        }
        
        new Thread(() -> {
            try {
                // 根据GLUE类型过滤历史记录
                List<JobLogglue> history = jobInfoService.getGlueList(taskId, glueType);
                Platform.runLater(() -> {
                    historyList.clear();
                    if (history != null && !history.isEmpty()) {
                        historyList.addAll(history);
                    }
                    historyCombo.setDisable(false);
                });
            } catch (IOException e) {
                logger.error("加载 GLUE 历史记录失败", e);
                Platform.runLater(() -> {
                    historyCombo.setDisable(true);
                    // 不显示错误提示，只是禁用下拉框
                });
            }
        }).start();
    }
    
    /**
     * 关闭前验证（不保存到数据库，只验证输入）
     * @return 验证是否通过
     */
    private boolean validateBeforeClose() {
        // 备注可以为空，因为最终保存时会在任务编辑页面一起保存
        // 这里不做强制验证，允许用户先编辑代码，稍后在任务编辑页面保存时再填写备注
        return true;
    }
    
    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        getDialogPane().setPrefWidth(850);
        getDialogPane().setPrefHeight(650);
        getDialogPane().setMinWidth(800);
        getDialogPane().setMinHeight(600);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);
        
        
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        if (saveButton != null) {
            saveButton.setStyle(
                "-fx-background-color: #2563EB; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 8 20 8 20; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand;"
            );
        }
        
        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        if (cancelButton != null) {
            cancelButton.setStyle(
                "-fx-background-color: #F3F4F6; " +
                "-fx-text-fill: #374151; " +
                "-fx-font-size: 13; " +
                "-fx-padding: 8 20 8 20; " +
                "-fx-border-color: #D1D5DB; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand;"
            );
        }
        
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(800);
                stage.setMinHeight(600);
            }
        });
    }
    
    /**
     * 获取当前代码内容
     */
    public String getCode() {
        return codeEditorArea.getText();
    }
    
    /**
     * 获取当前备注
     */
    public String getRemark() {
        return remarkField.getText();
    }
}

