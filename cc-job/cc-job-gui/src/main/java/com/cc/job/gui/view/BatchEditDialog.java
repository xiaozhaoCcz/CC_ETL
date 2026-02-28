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
import java.util.Set;
import java.util.function.Consumer;

/**
 * 批量编辑节点对话框
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class BatchEditDialog extends Dialog<BatchEditDialog.BatchEditResult> {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchEditDialog.class);
    
    private Set<ProcessNode> selectedNodes;
    
    // 编辑选项
    private RadioButton replaceNameRadio;
    private RadioButton modifyPropertyRadio;
    private RadioButton toggleEnabledRadio;
    private RadioButton deleteNodesRadio;
    
    private ToggleGroup operationGroup;
    
    // 替换名称相关
    private TextField findTextField;
    private TextField replaceTextField;
    
    // 修改属性相关
    private ComboBox<String> propertyCombo;
    private TextField propertyValueField;
    
    public BatchEditDialog(Stage owner, Set<ProcessNode> selectedNodes) {
        this.selectedNodes = selectedNodes != null ? selectedNodes : new java.util.HashSet<>();
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("批量编辑节点");
        setHeaderText("已选中 " + this.selectedNodes.size() + " 个节点，选择要执行的操作");
        
        // 创建对话框内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        // 添加按钮
        ButtonType applyButtonType = new ButtonType("应用", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, applyButtonType);
        
        // 设置样式
        styleDialog();
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == applyButtonType) {
                return collectResult();
            }
            return null;
        });
    }
    
    private VBox createContent() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setPrefWidth(500);
        
        // 操作选择
        Label operationLabel = new Label("选择操作：");
        operationLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        operationGroup = new ToggleGroup();
        
        replaceNameRadio = new RadioButton("批量替换节点名称");
        replaceNameRadio.setToggleGroup(operationGroup);
        replaceNameRadio.setSelected(true);
        
        modifyPropertyRadio = new RadioButton("批量修改属性");
        modifyPropertyRadio.setToggleGroup(operationGroup);
        
        toggleEnabledRadio = new RadioButton("批量启用/禁用");
        toggleEnabledRadio.setToggleGroup(operationGroup);
        
        deleteNodesRadio = new RadioButton("批量删除节点");
        deleteNodesRadio.setToggleGroup(operationGroup);
        deleteNodesRadio.setStyle("-fx-text-fill: #EF4444;");
        
        VBox operationBox = new VBox(10);
        operationBox.getChildren().addAll(replaceNameRadio, modifyPropertyRadio, toggleEnabledRadio, deleteNodesRadio);
        
        // 替换名称选项
        VBox replaceNameBox = new VBox(10);
        replaceNameBox.setVisible(true);
        replaceNameBox.setManaged(true);
        
        Label findLabel = new Label("查找：");
        findTextField = new TextField();
        findTextField.setPromptText("输入要查找的文本");
        findTextField.setPrefWidth(400);
        
        Label replaceLabel = new Label("替换为：");
        replaceTextField = new TextField();
        replaceTextField.setPromptText("输入替换后的文本");
        replaceTextField.setPrefWidth(400);
        
        replaceNameBox.getChildren().addAll(findLabel, findTextField, replaceLabel, replaceTextField);
        
        // 修改属性选项
        VBox modifyPropertyBox = new VBox(10);
        modifyPropertyBox.setVisible(false);
        modifyPropertyBox.setManaged(false);
        
        Label propertyLabel = new Label("属性：");
        propertyCombo = new ComboBox<>();
        propertyCombo.setPrefWidth(400);
        propertyCombo.getItems().addAll("执行器", "路由策略", "阻塞策略", "失败策略");
        propertyCombo.setPromptText("选择要修改的属性");
        
        Label valueLabel = new Label("值：");
        propertyValueField = new TextField();
        propertyValueField.setPromptText("输入新值");
        propertyValueField.setPrefWidth(400);
        
        modifyPropertyBox.getChildren().addAll(propertyLabel, propertyCombo, valueLabel, propertyValueField);
        
        // 启用/禁用选项
        VBox toggleBox = new VBox(10);
        toggleBox.setVisible(false);
        toggleBox.setManaged(false);
        
        Label toggleLabel = new Label("操作：");
        ComboBox<String> toggleCombo = new ComboBox<>();
        toggleCombo.setPrefWidth(400);
        toggleCombo.getItems().addAll("启用", "禁用");
        toggleCombo.setValue("启用");
        toggleBox.getChildren().addAll(toggleLabel, toggleCombo);
        
        // 删除确认
        VBox deleteBox = new VBox(10);
        deleteBox.setVisible(false);
        deleteBox.setManaged(false);
        
        Label deleteWarningLabel = new Label("⚠ 警告：此操作将永久删除选中的 " + selectedNodes.size() + " 个节点及其所有连接！");
        deleteWarningLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 600;");
        deleteBox.getChildren().add(deleteWarningLabel);
        
        // 根据选择的操作显示/隐藏相应的选项
        operationGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            replaceNameBox.setVisible(replaceNameRadio.isSelected());
            replaceNameBox.setManaged(replaceNameRadio.isSelected());
            modifyPropertyBox.setVisible(modifyPropertyRadio.isSelected());
            modifyPropertyBox.setManaged(modifyPropertyRadio.isSelected());
            toggleBox.setVisible(toggleEnabledRadio.isSelected());
            toggleBox.setManaged(toggleEnabledRadio.isSelected());
            deleteBox.setVisible(deleteNodesRadio.isSelected());
            deleteBox.setManaged(deleteNodesRadio.isSelected());
        });
        
        container.getChildren().addAll(
            operationLabel,
            operationBox,
            replaceNameBox,
            modifyPropertyBox,
            toggleBox,
            deleteBox
        );
        
        return container;
    }
    
    private BatchEditResult collectResult() {
        BatchEditResult result = new BatchEditResult();
        result.setSelectedNodes(new ArrayList<>(selectedNodes));
        
        if (replaceNameRadio.isSelected()) {
            result.setOperation(BatchEditOperation.REPLACE_NAME);
            result.setFindText(findTextField.getText());
            result.setReplaceText(replaceTextField.getText());
        } else if (modifyPropertyRadio.isSelected()) {
            result.setOperation(BatchEditOperation.MODIFY_PROPERTY);
            result.setPropertyName(propertyCombo.getValue());
            result.setPropertyValue(propertyValueField.getText());
        } else if (toggleEnabledRadio.isSelected()) {
            result.setOperation(BatchEditOperation.TOGGLE_ENABLED);
            // 这里需要从UI获取启用/禁用状态，暂时设为启用
            result.setEnabled(true);
        } else if (deleteNodesRadio.isSelected()) {
            result.setOperation(BatchEditOperation.DELETE);
        }
        
        return result;
    }
    
    private void styleDialog() {
        String cssUrl = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            getDialogPane().getStylesheets().add(cssUrl);
        }
    }
    
    /**
     * 批量编辑操作类型
     */
    public enum BatchEditOperation {
        REPLACE_NAME,      // 替换名称
        MODIFY_PROPERTY,   // 修改属性
        TOGGLE_ENABLED,    // 启用/禁用
        DELETE             // 删除
    }
    
    /**
     * 批量编辑结果
     */
    public static class BatchEditResult {
        private BatchEditOperation operation;
        private List<ProcessNode> selectedNodes;
        private String findText;
        private String replaceText;
        private String propertyName;
        private String propertyValue;
        private Boolean enabled;
        
        public BatchEditOperation getOperation() {
            return operation;
        }
        
        public void setOperation(BatchEditOperation operation) {
            this.operation = operation;
        }
        
        public List<ProcessNode> getSelectedNodes() {
            return selectedNodes;
        }
        
        public void setSelectedNodes(List<ProcessNode> selectedNodes) {
            this.selectedNodes = selectedNodes;
        }
        
        public String getFindText() {
            return findText;
        }
        
        public void setFindText(String findText) {
            this.findText = findText;
        }
        
        public String getReplaceText() {
            return replaceText;
        }
        
        public void setReplaceText(String replaceText) {
            this.replaceText = replaceText;
        }
        
        public String getPropertyName() {
            return propertyName;
        }
        
        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }
        
        public String getPropertyValue() {
            return propertyValue;
        }
        
        public void setPropertyValue(String propertyValue) {
            this.propertyValue = propertyValue;
        }
        
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
