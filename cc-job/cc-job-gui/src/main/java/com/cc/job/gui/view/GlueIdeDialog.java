package com.cc.job.gui.view;

import com.cc.job.gui.service.JobInfoService;
import com.cc.job.xo.model.entity.JobLogglue;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * GLUE IDE 对话框
 * 用于编辑和保存 GLUE 源代码
 */
public class GlueIdeDialog extends Dialog<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(GlueIdeDialog.class);
    
    private final Long taskId;
    private final JobInfoService jobInfoService;
    
    private TextArea codeEditorArea;
    private TextField remarkField;
    private ComboBox<JobLogglue> historyCombo;
    private ObservableList<JobLogglue> historyList;
    
    private ButtonType saveButtonType;
    private ButtonType cancelButtonType;
    
    public GlueIdeDialog(Stage owner, Long taskId, String initialCode, String initialRemark) {
        this.taskId = taskId;
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
            codeEditorArea.setText(initialCode);
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
                codeEditorArea.setText(newVal.getGlueSource() != null ? newVal.getGlueSource() : "");
                remarkField.setText(newVal.getGlueRemark() != null ? newVal.getGlueRemark() : "");
            }
        });
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
        
        codeEditorArea = new TextArea();
        codeEditorArea.setPrefRowCount(20);
        codeEditorArea.setWrapText(false);
        codeEditorArea.setStyle(
            "-fx-font-family: 'Consolas', 'Monaco', monospace; " +
            "-fx-font-size: 13; " +
            "-fx-background-color: #1E1E1E; " +
            "-fx-text-fill: #D4D4D4; " +
            "-fx-border-color: #3C3C3C; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        codeEditorArea.setPromptText("请输入 GLUE 代码...");
        
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
        
        // 布局
        HBox remarkBox = new HBox(10);
        remarkBox.setAlignment(Pos.CENTER_LEFT);
        remarkBox.getChildren().addAll(remarkLabel, remarkField);
        
        HBox historyBox = new HBox(10);
        historyBox.setAlignment(Pos.CENTER_LEFT);
        historyBox.getChildren().addAll(historyLabel, historyCombo);
        
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));
        bottomBox.getChildren().addAll(remarkBox, historyBox);
        
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
                List<JobLogglue> history = jobInfoService.getGlueList(taskId);
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
        
        getDialogPane().setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8;"
        );
        
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

