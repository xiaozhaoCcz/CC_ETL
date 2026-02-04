package com.cc.job.gui.view;

import com.cc.job.gui.model.ProcessNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

/**
 * 节点样式设置对话框
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class NodeStyleDialog extends Dialog<Void> {
    
    private ProcessNode node;
    private ColorPicker colorPicker;
    private TextField widthField;
    private TextField heightField;
    private ToggleGroup borderStyleGroup;
    private RadioButton solidRadio;
    private RadioButton dashedRadio;
    private RadioButton dottedRadio;
    private Slider borderWidthSlider;
    private Label borderWidthLabel;
    
    public NodeStyleDialog(Stage owner, ProcessNode node) {
        if (owner == null) {
            throw new IllegalArgumentException("owner Stage cannot be null");
        }
        this.node = node;
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("设置节点样式");
        setHeaderText("为节点设置颜色、大小和边框样式");
        
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
                applyStyles();
            }
            return null;
        });
    }
    
    private VBox createContent() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setPrefWidth(450);
        
        // 颜色选择
        Label colorLabel = new Label("节点颜色：");
        colorLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        colorPicker = new ColorPicker();
        colorPicker.setPrefWidth(400);
        
        // 如果节点已有颜色，设置到颜色选择器
        if (node != null && node.getCurrentColor() != null && !node.getCurrentColor().isEmpty()) {
            try {
                Color color = Color.web(node.getCurrentColor());
                colorPicker.setValue(color);
            } catch (Exception e) {
                // 如果颜色格式无效，使用默认颜色
                colorPicker.setValue(Color.web("#2563EB"));
            }
        } else {
            // 默认颜色
            colorPicker.setValue(Color.web("#2563EB"));
        }
        
        // 大小设置
        Label sizeLabel = new Label("节点大小：");
        sizeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        HBox sizeBox = new HBox(10);
        sizeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label widthLabel = new Label("宽度：");
        widthField = new TextField();
        widthField.setPrefWidth(100);
        widthField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 180.0, change -> {
            String text = change.getControlNewText();
            if (text.isEmpty()) {
                return change;
            }
            try {
                double value = Double.parseDouble(text);
                if (value >= 80 && value <= 800) {
                    return change;
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
            return null;
        }));
        
        Label heightLabel = new Label("高度：");
        heightField = new TextField();
        heightField.setPrefWidth(100);
        heightField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 80.0, change -> {
            String text = change.getControlNewText();
            if (text.isEmpty()) {
                return change;
            }
            try {
                double value = Double.parseDouble(text);
                if (value >= 40 && value <= 400) {
                    return change;
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
            return null;
        }));
        
        // 如果节点已有大小，设置到输入框
        if (node != null) {
            double width = node.getNodeWidth();
            double height = node.getNodeHeight();
            widthField.setText(String.valueOf(width));
            heightField.setText(String.valueOf(height));
        }
        
        sizeBox.getChildren().addAll(widthLabel, widthField, heightLabel, heightField);
        
        // 边框样式
        Label borderStyleLabel = new Label("边框样式：");
        borderStyleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        borderStyleGroup = new ToggleGroup();
        
        solidRadio = new RadioButton("实线");
        solidRadio.setToggleGroup(borderStyleGroup);
        solidRadio.setSelected(true);
        
        dashedRadio = new RadioButton("虚线");
        dashedRadio.setToggleGroup(borderStyleGroup);
        
        dottedRadio = new RadioButton("点线");
        dottedRadio.setToggleGroup(borderStyleGroup);
        
        VBox borderStyleBox = new VBox(10);
        borderStyleBox.getChildren().addAll(solidRadio, dashedRadio, dottedRadio);
        
        // 如果节点已有边框样式，选中对应的单选按钮
        if (node != null) {
            ProcessNode.BorderStyle currentStyle = node.getBorderStyle();
            if (currentStyle == ProcessNode.BorderStyle.DASHED) {
                dashedRadio.setSelected(true);
            } else if (currentStyle == ProcessNode.BorderStyle.DOTTED) {
                dottedRadio.setSelected(true);
            } else {
                solidRadio.setSelected(true);
            }
        }
        
        // 边框粗细
        Label borderWidthLabelTitle = new Label("边框粗细：");
        borderWidthLabelTitle.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        HBox borderWidthBox = new HBox(10);
        borderWidthBox.setAlignment(Pos.CENTER_LEFT);
        
        borderWidthSlider = new Slider(1, 10, 2);
        borderWidthSlider.setPrefWidth(200);
        borderWidthSlider.setShowTickLabels(true);
        borderWidthSlider.setShowTickMarks(true);
        borderWidthSlider.setMajorTickUnit(1);
        borderWidthSlider.setMinorTickCount(0);
        borderWidthSlider.setSnapToTicks(true);
        
        borderWidthLabel = new Label("2.0");
        borderWidthLabel.setStyle("-fx-font-size: 12; -fx-min-width: 40;");
        
        // 如果节点已有边框粗细，设置到滑块
        if (node != null) {
            double borderWidth = node.getBorderWidth();
            borderWidthSlider.setValue(borderWidth);
            borderWidthLabel.setText(String.format("%.1f", borderWidth));
        }
        
        // 滑块值变化时更新标签
        borderWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            borderWidthLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });
        
        borderWidthBox.getChildren().addAll(borderWidthSlider, borderWidthLabel);
        
        container.getChildren().addAll(
            colorLabel, colorPicker,
            sizeLabel, sizeBox,
            borderStyleLabel, borderStyleBox,
            borderWidthLabelTitle, borderWidthBox
        );
        
        return container;
    }
    
    private void applyStyles() {
        if (node == null) return;
        
        // 应用颜色
        if (colorPicker != null && colorPicker.getValue() != null) {
            Color color = colorPicker.getValue();
            String colorHex = String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
            node.setNodeColor(colorHex);
        }
        
        // 应用大小
        try {
            double width = Double.parseDouble(widthField.getText());
            double height = Double.parseDouble(heightField.getText());
            // 限制范围
            width = Math.max(80, Math.min(800, width));
            height = Math.max(40, Math.min(400, height));
            node.setNodeSize(width, height);
        } catch (NumberFormatException e) {
            // 忽略无效输入
        }
        
        // 应用边框样式
        RadioButton selected = (RadioButton) borderStyleGroup.getSelectedToggle();
        if (selected != null) {
            ProcessNode.BorderStyle style;
            if (selected == dashedRadio) {
                style = ProcessNode.BorderStyle.DASHED;
            } else if (selected == dottedRadio) {
                style = ProcessNode.BorderStyle.DOTTED;
            } else {
                style = ProcessNode.BorderStyle.SOLID;
            }
            node.setBorderStyle(style);
        }
        
        // 应用边框粗细
        double borderWidth = borderWidthSlider.getValue();
        node.setBorderWidth(borderWidth);
    }
    
    /**
     * 获取节点颜色（返回十六进制颜色字符串）
     */
    public String getNodeColor() {
        if (colorPicker != null && colorPicker.getValue() != null) {
            Color color = colorPicker.getValue();
            return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        }
        return "#2563EB"; // 默认颜色
    }
    
    /**
     * 获取节点宽度
     */
    public double getNodeWidth() {
        try {
            return Double.parseDouble(widthField.getText());
        } catch (NumberFormatException e) {
            return 180.0;
        }
    }
    
    /**
     * 获取节点高度
     */
    public double getNodeHeight() {
        try {
            return Double.parseDouble(heightField.getText());
        } catch (NumberFormatException e) {
            return 80.0;
        }
    }
    
    /**
     * 获取边框样式
     */
    public ProcessNode.BorderStyle getBorderStyle() {
        RadioButton selected = (RadioButton) borderStyleGroup.getSelectedToggle();
        if (selected == dashedRadio) {
            return ProcessNode.BorderStyle.DASHED;
        } else if (selected == dottedRadio) {
            return ProcessNode.BorderStyle.DOTTED;
        } else {
            return ProcessNode.BorderStyle.SOLID;
        }
    }
    
    /**
     * 获取边框粗细
     */
    public double getBorderWidth() {
        return borderWidthSlider.getValue();
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
