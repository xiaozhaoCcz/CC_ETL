package com.cc.job.gui.view;

import com.cc.job.gui.model.ConditionNode;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 条件节点配置对话框
 */
public class ConditionNodeDialog extends Dialog<ConditionNodeDialog.ConditionData> {
    
    private static final Logger logger = LoggerFactory.getLogger(ConditionNodeDialog.class);
    
    private TextField conditionNameField;
    private ComboBox<ConditionNode.ConditionType> conditionTypeCombo;
    private ComboBox<ConditionNode.ExpressionType> expressionTypeCombo;
    private TextArea simpleExpressionArea;
    private TextArea scriptExpressionArea;
    private Label expressionTypeLabel;
    private VBox simpleExpressionBox;
    private VBox scriptExpressionBox;
    
    private ConditionData conditionData;
    
    public static class ConditionData {
        private String conditionName;
        private ConditionNode.ConditionType conditionType;
        private String conditionExpression;
        private ConditionNode.ExpressionType expressionType;
        
        public ConditionData() {
        }
        
        public ConditionData(String conditionName, String conditionExpression, ConditionNode.ExpressionType expressionType) {
            this.conditionName = conditionName;
            this.conditionExpression = conditionExpression;
            this.expressionType = expressionType;
            this.conditionType = ConditionNode.ConditionType.IF;
        }
        
        public String getConditionName() {
            return conditionName;
        }
        
        public void setConditionName(String conditionName) {
            this.conditionName = conditionName;
        }
        
        public String getConditionExpression() {
            return conditionExpression;
        }
        
        public void setConditionExpression(String conditionExpression) {
            this.conditionExpression = conditionExpression;
        }
        
        public ConditionNode.ExpressionType getExpressionType() {
            return expressionType;
        }
        
        public void setExpressionType(ConditionNode.ExpressionType expressionType) {
            this.expressionType = expressionType;
        }
        
        public ConditionNode.ConditionType getConditionType() {
            return conditionType;
        }
        
        public void setConditionType(ConditionNode.ConditionType conditionType) {
            this.conditionType = conditionType != null ? conditionType : ConditionNode.ConditionType.IF;
        }
    }
    
    public ConditionNodeDialog(Stage owner, ConditionData editData) {
        this.conditionData = editData != null ? editData : new ConditionData();
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(editData == null ? "新建条件节点" : "编辑条件节点");
        setHeaderText(null);
        
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        ButtonType saveButtonType = new ButtonType(editData == null ? "创建" : "保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, saveButtonType);
        
        styleDialog();
        
        if (editData != null) {
            fillFormData(editData);
        }
        
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                if (validateForm()) {
                    return collectFormData();
                } else {
                    return null;
                }
            }
            return null;
        });
        
        javafx.application.Platform.runLater(() -> {
            Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
            if (saveButton != null) {
                saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                    if (!validateForm()) {
                        event.consume();
                    }
                });
            }
        });
        
        // 监听表达式类型变化
        expressionTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateExpressionFields(newVal);
        });
        
        // 初始化字段可见性
        if (expressionTypeCombo.getValue() != null) {
            updateExpressionFields(expressionTypeCombo.getValue());
        }
    }
    
    private VBox createContent() {
        VBox container = new VBox(20);
        container.getStyleClass().add("dialog-content-root");
        container.setPadding(new Insets(20));
        container.setPrefWidth(600);
        container.setPrefHeight(500);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(10));
        
        // 基本信息部分
        formContent.getChildren().add(createBasicSection());
        
        // 条件表达式部分
        formContent.getChildren().add(createExpressionSection());
        
        scrollPane.setContent(formContent);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        container.getChildren().add(scrollPane);
        
        return container;
    }
    
    private VBox createBasicSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("dialog-section");
        
        Label titleLabel = new Label("基本信息");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        // 条件节点名称
        Label nameLabel = new Label("条件节点名称：");
        nameLabel.setStyle("-fx-font-size: 12;");
        conditionNameField = new TextField();
        conditionNameField.setPromptText("请输入条件节点名称");
        conditionNameField.setStyle("-fx-font-size: 12;");
        
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        nameBox.getChildren().addAll(nameLabel, conditionNameField);
        HBox.setHgrow(conditionNameField, Priority.ALWAYS);
        
        // 条件节点类型
        Label typeLabel = new Label("节点类型：");
        typeLabel.setStyle("-fx-font-size: 12;");
        conditionTypeCombo = new ComboBox<>();
        conditionTypeCombo.getItems().addAll(ConditionNode.ConditionType.IF, 
                                              ConditionNode.ConditionType.WHILE, 
                                              ConditionNode.ConditionType.FOREACH);
        conditionTypeCombo.setValue(ConditionNode.ConditionType.IF);
        conditionTypeCombo.setStyle("-fx-font-size: 12;");
        
        // 设置显示文本
        conditionTypeCombo.setCellFactory(param -> new ListCell<ConditionNode.ConditionType>() {
            @Override
            protected void updateItem(ConditionNode.ConditionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case IF:
                            setText("IF (条件判断)");
                            break;
                        case WHILE:
                            setText("WHILE (循环)");
                            break;
                        case FOREACH:
                            setText("FOREACH (遍历)");
                            break;
                        default:
                            setText(item.toString());
                    }
                }
            }
        });
        conditionTypeCombo.setButtonCell(new ListCell<ConditionNode.ConditionType>() {
            @Override
            protected void updateItem(ConditionNode.ConditionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case IF:
                            setText("IF (条件判断)");
                            break;
                        case WHILE:
                            setText("WHILE (循环)");
                            break;
                        case FOREACH:
                            setText("FOREACH (遍历)");
                            break;
                        default:
                            setText(item.toString());
                    }
                }
            }
        });
        
        HBox typeBox = new HBox(10);
        typeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        typeBox.getChildren().addAll(typeLabel, conditionTypeCombo);
        HBox.setHgrow(conditionTypeCombo, Priority.ALWAYS);
        
        section.getChildren().addAll(titleLabel, nameBox, typeBox);
        
        return section;
    }
    
    private VBox createExpressionSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("dialog-section");
        
        Label titleLabel = new Label("条件表达式");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        // 表达式类型选择
        Label typeLabel = new Label("表达式类型：");
        typeLabel.setStyle("-fx-font-size: 12;");
        expressionTypeCombo = new ComboBox<>();
        expressionTypeCombo.getItems().addAll(ConditionNode.ExpressionType.SIMPLE, ConditionNode.ExpressionType.SCRIPT);
        expressionTypeCombo.setValue(ConditionNode.ExpressionType.SIMPLE);
        expressionTypeCombo.setStyle("-fx-font-size: 12;");
        
        // 设置显示文本
        expressionTypeCombo.setCellFactory(param -> new ListCell<ConditionNode.ExpressionType>() {
            @Override
            protected void updateItem(ConditionNode.ExpressionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item == ConditionNode.ExpressionType.SIMPLE ? "简单表达式" : "脚本表达式");
                }
            }
        });
        expressionTypeCombo.setButtonCell(new ListCell<ConditionNode.ExpressionType>() {
            @Override
            protected void updateItem(ConditionNode.ExpressionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item == ConditionNode.ExpressionType.SIMPLE ? "简单表达式" : "脚本表达式");
                }
            }
        });
        
        HBox typeBox = new HBox(10);
        typeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        typeBox.getChildren().addAll(typeLabel, expressionTypeCombo);
        
        // 表达式类型说明
        expressionTypeLabel = new Label();
        expressionTypeLabel.setStyle("-fx-font-size: 11; -fx-wrap-text: true;");
        updateExpressionTypeDescription(ConditionNode.ExpressionType.SIMPLE);
        
        // 简单表达式输入框
        simpleExpressionBox = new VBox(5);
        Label simpleLabel = new Label("简单表达式：");
        simpleLabel.setStyle("-fx-font-size: 12;");
        simpleExpressionArea = new TextArea();
        simpleExpressionArea.setPromptText("例如：变量 > 100 或 status == \"success\"");
        simpleExpressionArea.setPrefRowCount(4);
        simpleExpressionArea.setWrapText(true);
        simpleExpressionArea.setStyle("-fx-font-size: 12; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        simpleExpressionBox.getChildren().addAll(simpleLabel, simpleExpressionArea);
        
        // 脚本表达式输入框
        scriptExpressionBox = new VBox(5);
        Label scriptLabel = new Label("脚本表达式：");
        scriptLabel.setStyle("-fx-font-size: 12;");
        scriptExpressionArea = new TextArea();
        scriptExpressionArea.setPromptText("例如：if (变量 > 100 && status == \"success\") { return true; } else { return false; }");
        scriptExpressionArea.setPrefRowCount(8);
        scriptExpressionArea.setWrapText(true);
        scriptExpressionArea.setStyle("-fx-font-size: 12; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        scriptExpressionBox.getChildren().addAll(scriptLabel, scriptExpressionArea);
        
        section.getChildren().addAll(titleLabel, typeBox, expressionTypeLabel, simpleExpressionBox, scriptExpressionBox);
        
        return section;
    }
    
    private void updateExpressionTypeDescription(ConditionNode.ExpressionType type) {
        if (type == ConditionNode.ExpressionType.SIMPLE) {
            expressionTypeLabel.setText("简单表达式：支持基本的比较运算符（>, <, ==, !=, >=, <=）和逻辑运算符（&&, ||）。例如：变量 > 100");
        } else {
            expressionTypeLabel.setText("脚本表达式：支持完整的脚本逻辑，可以使用if-else、循环等复杂结构。例如：if (变量 > 100 && status == \"success\") { return true; }");
        }
    }
    
    private void updateExpressionFields(ConditionNode.ExpressionType type) {
        updateExpressionTypeDescription(type);
        if (type == ConditionNode.ExpressionType.SIMPLE) {
            simpleExpressionBox.setVisible(true);
            simpleExpressionBox.setManaged(true);
            scriptExpressionBox.setVisible(false);
            scriptExpressionBox.setManaged(false);
        } else {
            simpleExpressionBox.setVisible(false);
            simpleExpressionBox.setManaged(false);
            scriptExpressionBox.setVisible(true);
            scriptExpressionBox.setManaged(true);
        }
    }
    
    private void fillFormData(ConditionData data) {
        if (data.getConditionName() != null) {
            conditionNameField.setText(data.getConditionName());
        }
        if (data.getConditionType() != null) {
            conditionTypeCombo.setValue(data.getConditionType());
        }
        if (data.getExpressionType() != null) {
            expressionTypeCombo.setValue(data.getExpressionType());
            updateExpressionFields(data.getExpressionType());
        }
        if (data.getConditionExpression() != null) {
            if (data.getExpressionType() == ConditionNode.ExpressionType.SIMPLE) {
                simpleExpressionArea.setText(data.getConditionExpression());
            } else {
                scriptExpressionArea.setText(data.getConditionExpression());
            }
        }
    }
    
    private boolean validateForm() {
        if (conditionNameField.getText() == null || conditionNameField.getText().trim().isEmpty()) {
            showError("请输入条件节点名称");
            return false;
        }
        
        ConditionNode.ExpressionType type = expressionTypeCombo.getValue();
        if (type == null) {
            showError("请选择表达式类型");
            return false;
        }
        
        String expression = null;
        if (type == ConditionNode.ExpressionType.SIMPLE) {
            expression = simpleExpressionArea.getText();
        } else {
            expression = scriptExpressionArea.getText();
        }
        
        if (expression == null || expression.trim().isEmpty()) {
            showError("请输入条件表达式");
            return false;
        }
        
        return true;
    }
    
    private ConditionData collectFormData() {
        ConditionData data = new ConditionData();
        data.setConditionName(conditionNameField.getText().trim());
        data.setConditionType(conditionTypeCombo.getValue());
        data.setExpressionType(expressionTypeCombo.getValue());
        
        if (data.getExpressionType() == ConditionNode.ExpressionType.SIMPLE) {
            data.setConditionExpression(simpleExpressionArea.getText().trim());
        } else {
            data.setConditionExpression(scriptExpressionArea.getText().trim());
        }
        
        return data;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("验证失败");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void styleDialog() {
        String cssUrl = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            getDialogPane().getStylesheets().add(cssUrl);
        }
    }
}

