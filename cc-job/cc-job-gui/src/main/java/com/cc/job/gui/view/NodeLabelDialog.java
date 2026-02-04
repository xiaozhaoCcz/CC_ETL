package com.cc.job.gui.view;

import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.IconUtil;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 节点标签编辑对话框
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class NodeLabelDialog extends Dialog<List<String>> {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeLabelDialog.class);
    
    private ProcessNode node;
    private ListView<String> tagListView;
    private TextField tagInputField;
    private ObservableList<String> tags;
    
    // 预定义标签颜色
    private static final String[] PREDEFINED_TAGS = {
        "重要", "测试", "生产", "待优化", "已完成", "进行中", "阻塞", "紧急"
    };
    
    public NodeLabelDialog(Stage owner, ProcessNode node) {
        this.node = node;
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("编辑节点标签");
        setHeaderText("为节点 \"" + (node != null ? node.getJobHandlerName() : "") + "\" 添加或管理标签");
        
        // 初始化标签列表
        if (node != null) {
            tags = FXCollections.observableArrayList(node.getTags());
        } else {
            tags = FXCollections.observableArrayList();
        }
        
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
                return new ArrayList<>(tags);
            }
            return null;
        });
    }
    
    private VBox createContent() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(500);
        container.setPrefHeight(400);
        
        // 标签列表
        Label tagListLabel = new Label("当前标签：");
        tagListLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        tagListView = new ListView<>(tags);
        tagListView.setPrefHeight(200);
        tagListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); // 清空样式
                } else {
                    setText(item);
                    // 根据选中状态设置不同的样式
                    if (isSelected()) {
                        // 选中状态：深蓝色背景，白色文字，带边框
                        setStyle(
                            "-fx-font-size: 12; " +
                            "-fx-padding: 4 8 4 8; " +
                            "-fx-background-color: #2563EB; " +
                            "-fx-background-radius: 4; " +
                            "-fx-text-fill: #FFFFFF; " +
                            "-fx-border-color: #1D4ED8; " +
                            "-fx-border-radius: 4; " +
                            "-fx-border-width: 2;"
                        );
                    } else {
                        // 未选中状态：浅蓝色背景，蓝色文字
                        setStyle(
                            "-fx-font-size: 12; " +
                            "-fx-padding: 4 8 4 8; " +
                            "-fx-background-color: #EEF2FF; " +
                            "-fx-background-radius: 4; " +
                            "-fx-text-fill: #2563EB;"
                        );
                    }
                }
            }
        });
        
        // 添加标签输入区域
        Label addTagLabel = new Label("添加标签：");
        addTagLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        HBox addTagBox = new HBox(10);
        addTagBox.setAlignment(Pos.CENTER_LEFT);
        
        tagInputField = new TextField();
        tagInputField.setPromptText("输入标签名称，按回车添加");
        tagInputField.setPrefWidth(300);
        HBox.setHgrow(tagInputField, Priority.ALWAYS);
        
        // 回车添加标签
        tagInputField.setOnAction(e -> addTag());
        
        Button addButton = new Button("添加");
        addButton.setOnAction(e -> addTag());
        
        // 预定义标签按钮
        Label predefinedLabel = new Label("快速添加：");
        predefinedLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6B7280;");
        
        FlowPane predefinedTagsPane = new FlowPane(8, 8);
        predefinedTagsPane.setPrefWrapLength(450);
        for (String predefinedTag : PREDEFINED_TAGS) {
            Button tagButton = new Button(predefinedTag);
            tagButton.setStyle(
                "-fx-font-size: 11; " +
                "-fx-padding: 4 12 4 12; " +
                "-fx-background-color: #F3F4F6; " +
                "-fx-background-radius: 12; " +
                "-fx-text-fill: #374151; " +
                "-fx-border-color: #D1D5DB; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 1;"
            );
            tagButton.setOnAction(e -> {
                if (!tags.contains(predefinedTag)) {
                    tags.add(predefinedTag);
                    tagListView.scrollTo(tags.size() - 1);
                }
            });
            predefinedTagsPane.getChildren().add(tagButton);
        }
        
        addTagBox.getChildren().addAll(tagInputField, addButton);
        
        // 删除按钮
        Button deleteButton = new Button("删除选中");
        deleteButton.setStyle("-fx-text-fill: #EF4444;");
        deleteButton.setOnAction(e -> {
            String selected = tagListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                tags.remove(selected);
            }
        });
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(deleteButton);
        
        container.getChildren().addAll(
            tagListLabel,
            tagListView,
            addTagLabel,
            addTagBox,
            predefinedLabel,
            predefinedTagsPane,
            buttonBox
        );
        
        return container;
    }
    
    private void addTag() {
        String tagText = tagInputField.getText().trim();
        if (tagText.isEmpty()) {
            return;
        }
        
        if (tags.contains(tagText)) {
            // 标签已存在，显示提示
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("标签 \"" + tagText + "\" 已存在");
            alert.showAndWait();
            return;
        }
        
        tags.add(tagText);
        tagInputField.clear();
        tagListView.scrollTo(tags.size() - 1);
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
