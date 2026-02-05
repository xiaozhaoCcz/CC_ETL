package com.cc.job.gui.view;

import com.cc.job.gui.manager.CanvasExportManager;
import com.cc.job.gui.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 画布导出设置对话框
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class CanvasExportDialog extends Dialog<File> {
    
    private static final Logger logger = LoggerFactory.getLogger(CanvasExportDialog.class);
    
    private CanvasExportManager.ExportConfig config;
    private File selectedFile;
    
    // 控件
    private ComboBox<CanvasExportManager.ExportFormat> formatCombo;
    private Slider scaleSlider;
    private Label scaleLabel;
    private CheckBox includeBackgroundCheck;
    private ColorPicker backgroundColorPicker;
    private RadioButton exportAllRadio;
    private RadioButton exportSelectionRadio;
    private ToggleGroup exportAreaGroup;
    
    public CanvasExportDialog(Stage owner) {
        this.config = new CanvasExportManager.ExportConfig();
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("导出画布");
        setHeaderText("设置导出选项");
        
        // 创建对话框内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        // 添加按钮
        ButtonType exportButtonType = new ButtonType("导出", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, exportButtonType);
        
        // 设置样式与主题
        styleDialog();
        String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (css != null && !css.isEmpty()) {
            getDialogPane().getStylesheets().add(css);
        }
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == exportButtonType) {
                // 选择保存位置
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("保存画布");
                
                // 设置文件扩展名过滤器
                CanvasExportManager.ExportFormat format = formatCombo != null ? formatCombo.getValue() : CanvasExportManager.ExportFormat.PNG;
                if (format != null) {
                    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                        format.getDescription(),
                        "*." + format.getExtension()
                    );
                    fileChooser.getExtensionFilters().add(filter);
                    fileChooser.setSelectedExtensionFilter(filter);
                }
                
                File file = fileChooser.showSaveDialog(owner);
                if (file != null) {
                    // 更新配置
                    updateConfig();
                    selectedFile = file;
                    return file;
                }
            }
            return null;
        });
    }
    
    private VBox createContent() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(500);
        
        // 格式选择
        Label formatLabel = new Label("导出格式：");
        formatLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(
            CanvasExportManager.ExportFormat.PNG,
            CanvasExportManager.ExportFormat.JPEG,
            CanvasExportManager.ExportFormat.SVG
        );
        formatCombo.setValue(CanvasExportManager.ExportFormat.PNG);
        formatCombo.setPrefWidth(200);
        
        // 缩放比例
        Label scaleTitleLabel = new Label("缩放比例：");
        scaleTitleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        HBox scaleBox = new HBox(10);
        scaleBox.setAlignment(Pos.CENTER_LEFT);
        
        scaleSlider = new Slider(0.5, 3.0, 1.0);
        scaleSlider.setShowTickLabels(true);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setMajorTickUnit(0.5);
        scaleSlider.setPrefWidth(300);
        HBox.setHgrow(scaleSlider, Priority.ALWAYS);
        
        scaleLabel = new Label("100%");
        scaleLabel.setPrefWidth(60);
        
        scaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scaleLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
            config.setScale(newVal.doubleValue());
        });
        
        scaleBox.getChildren().addAll(scaleSlider, scaleLabel);
        
        // 背景选项
        includeBackgroundCheck = new CheckBox("包含背景");
        includeBackgroundCheck.setSelected(true);
        includeBackgroundCheck.setOnAction(e -> config.setIncludeBackground(includeBackgroundCheck.isSelected()));
        
        Label bgColorLabel = new Label("背景颜色：");
        backgroundColorPicker = new ColorPicker(Color.WHITE);
        backgroundColorPicker.setOnAction(e -> config.setBackgroundColor(backgroundColorPicker.getValue()));
        
        // 导出区域
        Label areaLabel = new Label("导出区域：");
        areaLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        exportAreaGroup = new ToggleGroup();
        exportAllRadio = new RadioButton("整个画布");
        exportAllRadio.setToggleGroup(exportAreaGroup);
        exportAllRadio.setSelected(true);
        
        exportSelectionRadio = new RadioButton("选中区域");
        exportSelectionRadio.setToggleGroup(exportAreaGroup);
        exportSelectionRadio.setDisable(true); // 暂时禁用，后续可以实现
        
        VBox areaBox = new VBox(5);
        areaBox.getChildren().addAll(exportAllRadio, exportSelectionRadio);
        
        container.getChildren().addAll(
            formatLabel,
            formatCombo,
            scaleTitleLabel,
            scaleBox,
            includeBackgroundCheck,
            bgColorLabel,
            backgroundColorPicker,
            areaLabel,
            areaBox
        );
        
        return container;
    }
    
    private void updateConfig() {
        if (formatCombo.getValue() != null) {
            config.setFormat(formatCombo.getValue());
        }
        config.setScale(scaleSlider.getValue());
        config.setIncludeBackground(includeBackgroundCheck.isSelected());
        config.setBackgroundColor(backgroundColorPicker.getValue());
    }
    
    /**
     * 获取导出配置
     */
    public CanvasExportManager.ExportConfig getConfig() {
        updateConfig();
        return config;
    }
    
    private void styleDialog() {
    }
}
