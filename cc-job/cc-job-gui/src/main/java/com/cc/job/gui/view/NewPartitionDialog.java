package com.cc.job.gui.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 新建/编辑分区对话框
 */
public class NewPartitionDialog extends Dialog<String> {
    
    private TextField partitionNameField;
    private ButtonType confirmButtonType;
    private ButtonType cancelButtonType;
    private Long partitionId; // 编辑模式下的分区ID
    
    /**
     * 新建分区对话框构造函数
     */
    public NewPartitionDialog(Stage owner) {
        this(owner, null, null);
    }
    
    /**
     * 编辑分区对话框构造函数
     * @param owner 父窗口
     * @param partitionId 分区ID（编辑模式）
     * @param partitionName 分区名称（编辑模式）
     */
    public NewPartitionDialog(Stage owner, Long partitionId, String partitionName) {
        this.partitionId = partitionId;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(partitionId == null ? "新建分区" : "编辑分区");
        setHeaderText(null);
        
        // 创建对话框内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        // 如果是编辑模式，填充现有名称
        if (partitionId != null && partitionName != null) {
            partitionNameField.setText(partitionName);
        }
        
        // 添加按钮
        confirmButtonType = new ButtonType(partitionId == null ? "确认" : "保存", ButtonBar.ButtonData.OK_DONE);
        cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        
        // 设置样式
        styleDialog();
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == confirmButtonType) {
                String partitionNameResult = partitionNameField.getText().trim();
                if (partitionNameResult.isEmpty()) {
                    return null;
                }
                return partitionNameResult;
            }
            return null;
        });
        
        // 确认按钮验证
        Button confirmButton = (Button) getDialogPane().lookupButton(confirmButtonType);
        // 编辑模式下，如果名称不为空，按钮应该可用
        confirmButton.setDisable(partitionNameField.getText().trim().isEmpty());
        
        // 监听输入框变化
        partitionNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            confirmButton.setDisable(newValue.trim().isEmpty());
        });
        
        // 设置初始焦点
        partitionNameField.requestFocus();
    }
    
    /**
     * 创建对话框内容
     */
    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);
        
        // 标题
        Label titleLabel = new Label("任务分区名称");
        titleLabel.setStyle(
            "-fx-font-size: 14; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1F2937;"
        );
        
        // 输入框
        partitionNameField = new TextField();
        partitionNameField.setPromptText("请输入分区名称");
        partitionNameField.setPrefWidth(400);
        partitionNameField.setStyle(
            "-fx-font-size: 13; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        
        // 提示信息
        Label hintLabel = new Label("💡 提示：分区用于组织和管理任务组");
        hintLabel.setStyle(
            "-fx-font-size: 12; " +
            "-fx-text-fill: #6B7280;"
        );
        
        content.getChildren().addAll(titleLabel, partitionNameField, hintLabel);
        
        return content;
    }
    
    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        // 设置对话框大小
        getDialogPane().setPrefWidth(450);
        getDialogPane().setPrefHeight(200);
        getDialogPane().setMinWidth(360);
        getDialogPane().setMinHeight(180);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);
        
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            getDialogPane().getStylesheets().add(css);
        } catch (Exception e) { /* 样式表加载失败时忽略 */ }
        
        // 设置对话框样式
        getDialogPane().setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8;"
        );
        
        // 样式化确认按钮
        Button confirmButton = (Button) getDialogPane().lookupButton(confirmButtonType);
        if (confirmButton != null) {
            confirmButton.setStyle(
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
        
        // 样式化取消按钮
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
                stage.setMinWidth(360);
                stage.setMinHeight(180);
            }
        });
    }
}

