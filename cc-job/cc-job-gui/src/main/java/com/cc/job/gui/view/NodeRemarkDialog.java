package com.cc.job.gui.view;

import com.cc.job.gui.model.ProcessNode;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点备注编辑对话框
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class NodeRemarkDialog extends Dialog<String> {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeRemarkDialog.class);
    
    private ProcessNode node;
    private TextArea remarkTextArea;
    
    public NodeRemarkDialog(Stage owner, ProcessNode node) {
        this.node = node;
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("编辑节点备注");
        setHeaderText("为节点 \"" + (node != null ? node.getJobHandlerName() : "") + "\" 添加或编辑备注");
        
        // 创建对话框内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        // 添加按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, saveButtonType);
        
        // 设置样式
        styleDialog();
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return remarkTextArea.getText();
            }
            return null;
        });
    }
    
    private VBox createContent() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(500);
        container.setPrefHeight(300);
        
        // 备注输入区域
        Label remarkLabel = new Label("备注内容：");
        remarkLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        remarkTextArea = new TextArea();
        remarkTextArea.setPromptText("输入节点备注信息...");
        remarkTextArea.setPrefRowCount(8);
        remarkTextArea.setWrapText(true);
        
        // 如果节点已有备注，填充到输入框
        if (node != null && node.getRemark() != null && !node.getRemark().isEmpty()) {
            remarkTextArea.setText(node.getRemark());
        }
        
        container.getChildren().addAll(remarkLabel, remarkTextArea);
        
        return container;
    }
    
    private void styleDialog() {
        getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        getDialogPane().setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1;"
        );
    }
}
