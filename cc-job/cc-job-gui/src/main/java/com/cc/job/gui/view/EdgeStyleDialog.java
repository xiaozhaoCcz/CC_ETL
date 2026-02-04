package com.cc.job.gui.view;

import com.cc.job.gui.model.NodeConnection;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 连线样式设置对话框
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class EdgeStyleDialog extends Dialog<NodeConnection.EdgeStyle> {
    
    private NodeConnection connection;
    private ToggleGroup styleGroup;
    private RadioButton solidRadio;
    private RadioButton dashedRadio;
    private RadioButton dottedRadio;
    private TextField labelTextField;
    private ColorPicker colorPicker;
    
    public EdgeStyleDialog(Stage owner, NodeConnection connection) {
        if (owner == null) {
            throw new IllegalArgumentException("owner Stage cannot be null");
        }
        this.connection = connection; // connection 可以为 null，在 createContent 中处理
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("设置连线样式");
        setHeaderText("为连线设置样式和标签");
        
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
                RadioButton selected = (RadioButton) styleGroup.getSelectedToggle();
                if (selected == null) {
                    return null; // 如果没有选择，返回 null
                }
                if (selected == solidRadio) {
                    return NodeConnection.EdgeStyle.SOLID;
                } else if (selected == dashedRadio) {
                    return NodeConnection.EdgeStyle.DASHED;
                } else if (selected == dottedRadio) {
                    return NodeConnection.EdgeStyle.DOTTED;
                }
            }
            return null;
        });
    }
    
    private VBox createContent() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setPrefWidth(400);
        
        // 样式选择
        Label styleLabel = new Label("连线样式：");
        styleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        styleGroup = new ToggleGroup();
        
        solidRadio = new RadioButton("实线");
        solidRadio.setToggleGroup(styleGroup);
        solidRadio.setSelected(true);
        
        dashedRadio = new RadioButton("虚线");
        dashedRadio.setToggleGroup(styleGroup);
        
        dottedRadio = new RadioButton("点线");
        dottedRadio.setToggleGroup(styleGroup);
        
        VBox styleBox = new VBox(10);
        styleBox.getChildren().addAll(solidRadio, dashedRadio, dottedRadio);
        
        // 颜色选择
        Label colorLabel = new Label("连线颜色：");
        colorLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        colorPicker = new ColorPicker();
        colorPicker.setPrefWidth(350);
        
        // 如果连线已有颜色，设置到颜色选择器
        if (connection != null && connection.getEdgeColor() != null && !connection.getEdgeColor().isEmpty()) {
            try {
                Color color = Color.web(connection.getEdgeColor());
                colorPicker.setValue(color);
            } catch (Exception e) {
                // 如果颜色格式无效，使用默认颜色
                colorPicker.setValue(Color.web("#374151"));
            }
        } else {
            // 默认颜色
            colorPicker.setValue(Color.web("#374151"));
        }
        
        // 标签输入
        Label labelLabel = new Label("连线标签：");
        labelLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        labelTextField = new TextField();
        labelTextField.setPromptText("输入连线标签（可选）");
        labelTextField.setPrefWidth(350);
        
        // 如果连线已有标签，填充到输入框
        if (connection != null && connection.getLabelText() != null && !connection.getLabelText().isEmpty()) {
            labelTextField.setText(connection.getLabelText());
        }
        
        // 如果连线已有样式，选中对应的单选按钮
        if (connection != null) {
            NodeConnection.EdgeStyle currentStyle = connection.getEdgeStyle();
            if (currentStyle == NodeConnection.EdgeStyle.DASHED) {
                dashedRadio.setSelected(true);
            } else if (currentStyle == NodeConnection.EdgeStyle.DOTTED) {
                dottedRadio.setSelected(true);
            } else {
                solidRadio.setSelected(true);
            }
        }
        
        container.getChildren().addAll(styleLabel, styleBox, colorLabel, colorPicker, labelLabel, labelTextField);
        
        return container;
    }
    
    /**
     * 获取标签文本
     */
    public String getLabelText() {
        return labelTextField != null ? labelTextField.getText() : "";
    }
    
    /**
     * 获取连线颜色（返回十六进制颜色字符串）
     */
    public String getEdgeColor() {
        if (colorPicker != null && colorPicker.getValue() != null) {
            Color color = colorPicker.getValue();
            // 转换为十六进制格式 #RRGGBB
            return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        }
        return "#374151"; // 默认颜色
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
