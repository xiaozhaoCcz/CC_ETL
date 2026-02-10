package com.cc.job.gui.view;

import com.cc.job.gui.service.JobJdbcDatasourceService;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.gui.view.component.SmartParameterInput;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.form.JobInfoForm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 新建/编辑任务节点对话框
 */
public class NewJobNodeDialog extends Dialog<JobInfoForm> {
    
    private static final Logger logger = LoggerFactory.getLogger(NewJobNodeDialog.class);
    
    private JobInfoForm formData;
    private Long parentJobId; // 父任务组ID
    
    // 表单字段
    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField authorField;
    private TextField alarmEmailField;
    
    private ComboBox<GlueType> glueTypeCombo;
    private Label executorHandlerLabel; // JobHandler/数据库标签
    private TextField executorHandlerField;
    private ComboBox<JobJdbcDatasource> datasourceCombo; // SQL模式下的数据库下拉框
    private Button glueIdeButton; // GLUE模式下的按钮
    private Button configDataxButton; // DataX模式下的「配置datax」按钮
    private Button expandDataxJsonButton; // DataX模式下 dataxJson 的「放大编辑」按钮
    private Label executorParamLabel;
    private SmartParameterInput executorParamArea;
    private ComboBox<String> reqTypeCombo;
    private TextField reqUrlField;
    private Label reqBodyLabel;
    private SmartParameterInput reqBodyArea;
    private ParameterTable bodyTable;
    
    private JobJdbcDatasourceService datasourceService;
    
    private Long pendingDatasourceId; // 待设置的数据源ID（在数据源列表加载完成后设置）
    
    private TextField executorTimeoutField;
    private ComboBox<RouteStrategy> routeStrategyCombo;
    private ComboBox<BlockStrategy> blockStrategyCombo;
    private ComboBox<FailStrategy> failStrategyCombo;
    private Spinner<Integer> executorFailRetryCountSpinner;
    
    // 高级配置中需要根据节点类型切换显示的组件
    private Label failStrategyLabel;
    private TextArea glueEditorArea;
    private String glueRemark; // 存储 GLUE 备注
    private GlueType lastGlueType = null; // 记录上一次的GLUE类型，用于检测类型变化
    
    private ButtonType saveButtonType;
    private ButtonType cancelButtonType;
    
    // 高级配置容器（包括高级配置、GLUE脚本、API配置）
    private VBox advancedSection;
    //private VBox glueSection;
    private VBox apiSection;
    private boolean advancedSectionVisible = false;
    
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>(){}.getType();

    public NewJobNodeDialog(Stage owner, Long parentJobId, JobInfoForm editData, List<JobGroup> jobGroupList) {
        this.parentJobId = parentJobId;
        this.formData = editData != null ? editData : new JobInfoForm();
        this.datasourceService = new JobJdbcDatasourceService();

        // 初始化 glueEditorArea
        glueEditorArea = new TextArea();
        glueEditorArea.setPrefRowCount(12);
        glueEditorArea.setWrapText(true);
        glueEditorArea.setPromptText("请输入 GLUE 脚本内容");

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(editData == null ? "新增任务" : "编辑任务");
        setHeaderText(null);
        
        // 创建对话框内容
        VBox content = createContent(jobGroupList);
        getDialogPane().setContent(content);
        
        // 添加按钮
        saveButtonType = new ButtonType(editData == null ? "创建" : "保存", ButtonBar.ButtonData.OK_DONE);
        cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, saveButtonType);
        
        // 设置样式
        styleDialog();
        
        // 填充数据（编辑模式）
        if (editData != null) {
            fillFormData(editData, jobGroupList);
            // 编辑模式下，如果有高级配置数据，自动展开高级配置
            if (hasAdvancedConfig(editData)) {
                Platform.runLater(() -> {
                    toggleAdvancedSection();
                });
            }
        }
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                if (validateForm()) {
                    return collectFormData();
                } else {
                    // 验证失败，返回null并阻止对话框关闭
                    return null;
                }
            }
            return null;
        });
        
        // 拦截保存按钮的点击事件，验证失败时阻止对话框关闭
        Platform.runLater(() -> {
            Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
            if (saveButton != null) {
                // 拦截ActionEvent（按钮的默认行为），验证失败时阻止对话框关闭
                saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                    if (!validateForm()) {
                        event.consume(); // 验证失败，阻止事件传播，防止对话框关闭
                    }
                });
            }
        });
        
        // 监听GlueType变化
        glueTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            // 如果GLUE类型发生变化，且都是需要GLUE源码的类型，则清空旧代码并加载新类型的默认模板
            if (oldVal != null && newVal != null && 
                oldVal.requiresGlueSource() && newVal.requiresGlueSource() &&
                !oldVal.getType().equals(newVal.getType()) &&
                glueEditorArea != null) {
                // GLUE类型改变了，清空旧代码
                glueEditorArea.clear();
            }
            lastGlueType = newVal;
            updateFieldsForGlueType(newVal);
        });
        
        // 初始化字段可见性
        if (glueTypeCombo.getValue() != null) {
            lastGlueType = glueTypeCombo.getValue(); // 初始化时记录GLUE类型
            updateFieldsForGlueType(glueTypeCombo.getValue());
        }
    }
    
    /**
     * 创建对话框内容
     */
    private VBox createContent(List<JobGroup> jobGroupList) {
        VBox container = new VBox(15);
        container.getStyleClass().add("dialog-content-root");
        container.setPadding(new Insets(20));
        container.setPrefWidth(700);
        // 初始高度较小，只显示基本信息和执行配置
        container.setPrefHeight(400);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(10));
        
        // 基本信息
        formContent.getChildren().add(createBasicSection(jobGroupList));
        
        // 执行配置（包含高级配置链接）
        formContent.getChildren().add(createExecutionSection());
        
        // 高级配置（默认隐藏）
        advancedSection = createAdvancedSection();
        advancedSection.setVisible(false);
        advancedSection.setManaged(false);
        formContent.getChildren().add(advancedSection);
        
        // GLUE脚本（默认隐藏，根据运行模式动态显示）
//        glueSection = createGlueSection();
//        glueSection.setVisible(false);
//        glueSection.setManaged(false);
//        formContent.getChildren().add(glueSection);
        
        // API配置（默认隐藏，根据运行模式动态显示）
        apiSection = createApiSection();
        apiSection.setVisible(false);
        apiSection.setManaged(false);
        formContent.getChildren().add(apiSection);
        
        scrollPane.setContent(formContent);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        container.getChildren().add(scrollPane);
        
        return container;
    }
    
    /**
     * 创建基本信息部分
     */
    private VBox createBasicSection(List<JobGroup> jobGroupList) {
        VBox section = createSection("基本信息", IconUtil.fileIcon());
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 执行器
        Label jobGroupLabel = createFormLabel("执行器", true);
        jobGroupCombo = new ComboBox<>();
        jobGroupCombo.setPrefWidth(300);
        jobGroupCombo.setPromptText("请选择执行器");
        jobGroupCombo.getItems().addAll(jobGroupList);
        jobGroupCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        jobGroupCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        
        // 负责人
        Label authorLabel = createFormLabel("负责人", true);
        authorField = new TextField();
        authorField.setPrefWidth(300);
        authorField.setPromptText("请输入负责人姓名");
        
        // 任务描述
        Label jobDescLabel = createFormLabel("任务描述", true);
        jobDescField = new TextField();
        jobDescField.setPrefWidth(615);
        jobDescField.setPromptText("请输入任务描述");
        
        // 报警邮件
        Label alarmEmailLabel = createFormLabel("报警邮件", false);
        alarmEmailField = new TextField();
        alarmEmailField.setPrefWidth(615);
        alarmEmailField.setPromptText("请输入报警邮件地址，多个用逗号分隔");
        
        grid.add(jobGroupLabel, 0, 0);
        grid.add(jobGroupCombo, 1, 0);
        grid.add(authorLabel, 2, 0);
        grid.add(authorField, 3, 0);
        
        grid.add(jobDescLabel, 0, 1);
        grid.add(jobDescField, 1, 1, 3, 1);
        
        grid.add(alarmEmailLabel, 0, 2);
        grid.add(alarmEmailField, 1, 2, 3, 1);
        
        section.getChildren().add(grid);
        return section;
    }
    
    /**
     * 创建执行配置部分
     */
    private VBox createExecutionSection() {
        VBox section = createSection("执行配置", IconUtil.settingsIcon());
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 运行模式
        Label glueTypeLabel = createFormLabel("运行模式", true);
        glueTypeCombo = new ComboBox<>();
        glueTypeCombo.setPrefWidth(300);
        glueTypeCombo.setPromptText("请选择运行模式");
        glueTypeCombo.getItems().addAll(GlueType.values());
        glueTypeCombo.setValue(GlueType.BEAN);
        
        // JobHandler/数据库标签（根据运行模式动态切换）
        Label executorHandlerLabel = createFormLabel("JobHandler", true);
        executorHandlerField = new TextField();
        executorHandlerField.setPrefWidth(300);
        executorHandlerField.setPromptText("请输入JobHandler名称");
        
        // 数据库下拉框（SQL模式下使用）
        datasourceCombo = new ComboBox<>();
        datasourceCombo.setPrefWidth(300);
        datasourceCombo.setPromptText("请选择数据库");
        datasourceCombo.setVisible(false);
        datasourceCombo.setManaged(false);
        // 设置下拉框显示文本
        datasourceCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(JobJdbcDatasource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDatabaseName());
            }
        });
        datasourceCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JobJdbcDatasource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDatabaseName());
            }
        });
        // 异步加载数据源列表
        loadDatasourceList();
        
        // GLUE IDE 按钮（GLUE模式下使用）
        glueIdeButton = new Button("GLUE IDE");
        glueIdeButton.setPrefWidth(300);
        glueIdeButton.setStyle(
            "-fx-background-color: #2563EB; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 20 8 20; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        glueIdeButton.setOnAction(e -> openGlueIdeDialog());
        glueIdeButton.setVisible(false);
        glueIdeButton.setManaged(false);
        
        // 配置 datax 按钮（DataX 模式下使用）
        configDataxButton = new Button("配置datax");
        configDataxButton.setPrefWidth(300);
        configDataxButton.setStyle(
            "-fx-background-color: #2563EB; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 20 8 20; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        configDataxButton.setOnAction(e -> openDataxConfigDialog());
        configDataxButton.setVisible(false);
        configDataxButton.setManaged(false);
        
        // 任务参数
        executorParamLabel = createFormLabel("任务参数", false);
        executorParamArea = new SmartParameterInput();
        executorParamArea.setPrefWidth(615);
        executorParamArea.setPrefRowCount(4);
        executorParamArea.setPromptText("输入参数，使用 #{任务描述}.属性 引用其他任务的结果，使用 `# 输入普通#号");
        executorParamArea.setWrapText(true);
        
        expandDataxJsonButton = new Button();
        FontIcon expandIcon = IconUtil.expandIcon();
        expandIcon.setIconSize(18);
        expandDataxJsonButton.setGraphic(expandIcon);
        expandDataxJsonButton.setTooltip(new Tooltip("放大编辑"));
        expandDataxJsonButton.getStyleClass().add("dialog-button-secondary");
        expandDataxJsonButton.setOnAction(e -> openDataxJsonExpandDialog());
        expandDataxJsonButton.setVisible(false);
        expandDataxJsonButton.setManaged(false);
        
        HBox paramAreaContainer = new HBox(8);
        paramAreaContainer.setAlignment(Pos.CENTER_LEFT);
        paramAreaContainer.getChildren().addAll(executorParamArea, expandDataxJsonButton);
        HBox.setHgrow(executorParamArea, Priority.ALWAYS);
        
        grid.add(glueTypeLabel, 0, 0);
        grid.add(glueTypeCombo, 1, 0);
        grid.add(executorHandlerLabel, 2, 0);
        // 使用 StackPane 来切换显示输入框、数据库下拉框或按钮
        StackPane handlerContainer = new StackPane();
        handlerContainer.getChildren().addAll(executorHandlerField, datasourceCombo, glueIdeButton, configDataxButton);
        grid.add(handlerContainer, 3, 0);
        
        // 保存标签引用以便后续更新
        this.executorHandlerLabel = executorHandlerLabel;
        
        grid.add(executorParamLabel, 0, 1);
        grid.add(paramAreaContainer, 1, 1, 3, 1);
        
        // 设置任务组ID（用于自动补全）
        if (parentJobId != null) {
            executorParamArea.setTaskGroupId(parentJobId);
        }
        
        // 添加高级配置链接
        HBox linkContainer = new HBox(5);
        linkContainer.setPadding(new Insets(10, 15, 0, 15));
        linkContainer.setAlignment(Pos.CENTER_LEFT);
        
        Hyperlink advancedLink = new Hyperlink("高级配置");
        advancedLink.setStyle(
            "-fx-text-fill: #2563EB; " +
            "-fx-font-size: 13; " +
            "-fx-underline: true; " +
            "-fx-cursor: hand;"
        );
        advancedLink.setOnAction(e -> toggleAdvancedSection());
        
        linkContainer.getChildren().add(advancedLink);
        
        section.getChildren().add(grid);
        section.getChildren().add(linkContainer);
        return section;
    }
    
    /**
     * 切换高级配置显示/隐藏
     */
    private void toggleAdvancedSection() {
        advancedSectionVisible = !advancedSectionVisible;
        advancedSection.setVisible(advancedSectionVisible);
        advancedSection.setManaged(advancedSectionVisible);
        
        // 动态调整窗口大小
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                if (advancedSectionVisible) {
                    // 展开高级配置时，扩大窗口
                    getDialogPane().setPrefHeight(600);
                    stage.setMinHeight(600);
                } else {
                    // 收起高级配置时，缩小窗口
                    getDialogPane().setPrefHeight(450);
                    stage.setMinHeight(450);
                }
                // 让窗口自动适应内容（延迟执行，确保布局已完成）
                Platform.runLater(() -> {
                    stage.sizeToScene();
                });
            }
        });
    }
    

    private VBox createAdvancedSection() {
        VBox section = createSection("高级配置", IconUtil.wrenchIcon());
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 路由策略
        Label routeStrategyLabel = createFormLabel("路由策略", true);
        routeStrategyCombo = new ComboBox<>();
        routeStrategyCombo.setPrefWidth(300);
        routeStrategyCombo.setPromptText("请选择路由策略");
        routeStrategyCombo.getItems().addAll(RouteStrategy.values());
        routeStrategyCombo.setValue(RouteStrategy.FIRST);
        
        // 阻塞处理策略
        Label blockStrategyLabel = createFormLabel("阻塞处理策略", true);
        blockStrategyCombo = new ComboBox<>();
        blockStrategyCombo.setPrefWidth(300);
        blockStrategyCombo.setPromptText("请选择阻塞策略");
        blockStrategyCombo.getItems().addAll(BlockStrategy.values());
        blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
        
        // 失败策略（任务组内子节点显示）
        failStrategyLabel = createFormLabel("失败策略", true);
        failStrategyCombo = new ComboBox<>();
        failStrategyCombo.setPrefWidth(300);
        failStrategyCombo.setPromptText("请选择失败策略");
        failStrategyCombo.getItems().addAll(FailStrategy.values());
        failStrategyCombo.setValue(FailStrategy.JOB_FAIL);

        
        // 任务超时时间
        Label timeoutLabel = createFormLabel("任务超时时间(秒)", false);
        executorTimeoutField = new TextField();
        executorTimeoutField.setPrefWidth(300);
        executorTimeoutField.setPromptText("单位：秒");
        executorTimeoutField.setText("300");
        
        // 失败重试次数
        Label retryLabel = createFormLabel("失败重试次数", false);
        executorFailRetryCountSpinner = new Spinner<>(0, 10, 0);
        executorFailRetryCountSpinner.setPrefWidth(300);
        executorFailRetryCountSpinner.setEditable(true);
        
        // 第一行：路由策略 + 失败策略
        grid.add(routeStrategyLabel, 0, 0);
        grid.add(routeStrategyCombo, 1, 0);
        grid.add(failStrategyLabel, 2, 0);
        grid.add(failStrategyCombo, 3, 0);

        
        // 第二行：任务超时时间 + 阻塞处理策略
        grid.add(timeoutLabel, 0, 1);
        grid.add(executorTimeoutField, 1, 1);
        grid.add(blockStrategyLabel, 2, 1);
        grid.add(blockStrategyCombo, 3, 1);
        
        // 第三行：失败重试次数
        grid.add(retryLabel, 0, 2);
        grid.add(executorFailRetryCountSpinner, 1, 2);
        
        section.getChildren().add(grid);
        return section;
    }

//    private VBox createGlueSection() {
//        glueSection = createSection("GLUE 脚本", IconUtil.fileIcon());
//        glueSection.setVisible(false);
//
//        glueEditorArea = new TextArea();
//        glueEditorArea.setPrefRowCount(12);
//        glueEditorArea.setWrapText(true);
//        glueEditorArea.setPromptText("请输入 GLUE 脚本内容");
//
//        glueSection.getChildren().add(glueEditorArea);
//        return glueSection;
//    }

    private VBox createApiSection() {
        apiSection = createSection("API 配置", IconUtil.windowIcon());
        apiSection.setVisible(false);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));

        Label reqTypeLabel = createFormLabel("请求类型", true);
        reqTypeCombo = new ComboBox<>();
        reqTypeCombo.getItems().addAll("GET", "POST", "PUT", "DELETE");
        reqTypeCombo.setValue("GET");
        reqTypeCombo.setPrefWidth(150);

        Label reqUrlLabel = createFormLabel("请求地址", true);
        reqUrlField = new TextField();
        reqUrlField.setPrefWidth(400);
        reqUrlField.setPromptText("请输入请求地址");

        // 请求体（POST/PUT时显示）
        reqBodyLabel = createFormLabel("请求体", false);
        reqBodyArea = new com.cc.job.gui.view.component.SmartParameterInput();
        reqBodyArea.setPrefWidth(615);
        reqBodyArea.setPrefRowCount(6);
        reqBodyArea.setPromptText("请输入JSON格式的请求体，使用 #{任务描述}.属性 引用其他任务的结果，使用 `# 输入普通#号");
        reqBodyArea.setWrapText(true);
        reqBodyArea.setVisible(false);
        reqBodyArea.setManaged(false);
        
        // 设置任务组ID（用于自动补全）
        if (parentJobId != null) {
            reqBodyArea.setTaskGroupId(parentJobId);
        }

        grid.add(reqTypeLabel, 0, 0);
        grid.add(reqTypeCombo, 1, 0);
        grid.add(reqUrlLabel, 0, 1);
        grid.add(reqUrlField, 1, 1, 3, 1);
        grid.add(reqBodyLabel, 0, 2);
        grid.add(reqBodyArea, 1, 2, 3, 1);

        // 监听请求类型变化，显示/隐藏请求体
        reqTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRequestBodyVisibility(newVal);
        });

        bodyTable = new ParameterTable("请求参数");

        apiSection.getChildren().addAll(grid, bodyTable);
        return apiSection;
    }

    private void toggleExecutorParamArea(boolean visible) {
        executorParamLabel.setVisible(visible);
        executorParamLabel.setManaged(visible);
        executorParamArea.setVisible(visible);
        executorParamArea.setManaged(visible);
    }
    
    /**
     * 根据请求类型更新请求体输入框的可见性
     */
    private void updateRequestBodyVisibility(String reqType) {
        boolean shouldShow = "POST".equals(reqType) || "PUT".equals(reqType);
        if (reqBodyLabel != null) {
            reqBodyLabel.setVisible(shouldShow);
            reqBodyLabel.setManaged(shouldShow);
        }
        if (reqBodyArea != null) {
            reqBodyArea.setVisible(shouldShow);
            reqBodyArea.setManaged(shouldShow);
        }
    }

    /**
     * 根据GlueType更新字段可见性
     */
    private void updateFieldsForGlueType(GlueType glueType) {
        if (glueType == null) return;
        boolean isGlueMode = glueType.requiresGlueSource();
        
        switch (glueType) {
            case BEAN -> {
                executorHandlerField.setVisible(true);
                executorHandlerField.setManaged(true);
                executorHandlerField.setDisable(false);
                executorHandlerField.setPromptText("请输入JobHandler名称");
                datasourceCombo.setVisible(false);
                datasourceCombo.setManaged(false);
                glueIdeButton.setVisible(false);
                glueIdeButton.setManaged(false);
                configDataxButton.setVisible(false);
                configDataxButton.setManaged(false);
                executorParamArea.setPromptText("请输入任务参数");
                executorParamArea.setPrefRowCount(4);
                expandDataxJsonButton.setVisible(false);
                expandDataxJsonButton.setManaged(false);
                toggleExecutorParamArea(true);
                executorParamLabel.setText("任务参数");
                executorParamLabel.setGraphic(null);
                executorParamArea.setPrefRowCount(4);
                expandDataxJsonButton.setVisible(false);
                expandDataxJsonButton.setManaged(false);
                updateHandlerLabel("JobHandler");
            }
            case SQL -> {
                // SQL模式：显示数据库下拉框，隐藏JobHandler输入框
                executorHandlerField.setVisible(false);
                executorHandlerField.setManaged(false);
                datasourceCombo.setVisible(true);
                datasourceCombo.setManaged(true);
                glueIdeButton.setVisible(false);
                glueIdeButton.setManaged(false);
                configDataxButton.setVisible(false);
                configDataxButton.setManaged(false);
                executorParamArea.setPromptText("请输入SQL语句");
                toggleExecutorParamArea(true);
                executorParamLabel.setText("任务参数");
                executorParamLabel.setGraphic(null);
                executorParamArea.setPrefRowCount(4);
                expandDataxJsonButton.setVisible(false);
                expandDataxJsonButton.setManaged(false);
                updateHandlerLabel("数据库");
            }
            case API -> {
                executorHandlerField.setVisible(true);
                executorHandlerField.setManaged(true);
                executorHandlerField.setDisable(true);
                executorHandlerField.setText("runApiHandler");
                datasourceCombo.setVisible(false);
                datasourceCombo.setManaged(false);
                glueIdeButton.setVisible(false);
                glueIdeButton.setManaged(false);
                configDataxButton.setVisible(false);
                configDataxButton.setManaged(false);
                executorParamArea.setPrefRowCount(4);
                expandDataxJsonButton.setVisible(false);
                expandDataxJsonButton.setManaged(false);
                executorParamArea.clear();
                toggleExecutorParamArea(false);
                bodyTable.ensureAtLeastOneRow();
                // 更新标签文本
                updateHandlerLabel("JobHandler");
                // 根据当前请求类型显示/隐藏请求体
                if (reqTypeCombo != null) {
                    updateRequestBodyVisibility(reqTypeCombo.getValue());
                }
            }
            case DATAX -> {
                datasourceCombo.setVisible(false);
                datasourceCombo.setManaged(false);
                glueIdeButton.setVisible(false);
                glueIdeButton.setManaged(false);
                boolean isEditDatax = formData.getId() != null;
                if (isEditDatax) {
                    configDataxButton.setVisible(false);
                    configDataxButton.setManaged(false);
                    executorHandlerField.setVisible(true);
                    executorHandlerField.setManaged(true);
                    executorHandlerField.setDisable(true);
                    executorHandlerField.setText("runDataxHandler");
                } else {
                    configDataxButton.setVisible(true);
                    configDataxButton.setManaged(true);
                    executorHandlerField.setVisible(false);
                    executorHandlerField.setManaged(false);
                }
                executorParamArea.setPromptText("DataX JSON 配置，可点击「配置datax」生成");
                executorParamArea.setPrefRowCount(8);
                expandDataxJsonButton.setVisible(true);
                expandDataxJsonButton.setManaged(true);
                toggleExecutorParamArea(true);
                executorParamLabel.setText("dataxJson");
                executorParamLabel.setGraphic(null);
                updateHandlerLabel("JobHandler");
            }
            default -> {
                // GLUE 模式：显示按钮，隐藏输入框
                configDataxButton.setVisible(false);
                configDataxButton.setManaged(false);
                if (isGlueMode) {
                    executorHandlerField.setVisible(false);
                    executorHandlerField.setManaged(false);
                    datasourceCombo.setVisible(false);
                    datasourceCombo.setManaged(false);
                    glueIdeButton.setVisible(true);
                    glueIdeButton.setManaged(true);
                    
                    // 如果代码为空，自动加载默认模板
                    if (glueEditorArea != null) {
                        String currentCode = glueEditorArea.getText();
                        if (currentCode == null || currentCode.trim().isEmpty()) {
                            String defaultTemplate = com.cc.job.gui.util.GlueTemplateUtil.getDefaultTemplate(glueType.getType());
                            if (defaultTemplate != null && !defaultTemplate.trim().isEmpty()) {
                                glueEditorArea.setText(defaultTemplate);
                            }
                        }
                    }
                } else {
                    executorHandlerField.setVisible(true);
                    executorHandlerField.setManaged(true);
                    executorHandlerField.setDisable(true);
                    executorHandlerField.clear();
                    datasourceCombo.setVisible(false);
                    datasourceCombo.setManaged(false);
                    glueIdeButton.setVisible(false);
                    glueIdeButton.setManaged(false);
                    // 更新标签文本
                    updateHandlerLabel("JobHandler");
                }
                executorParamArea.setPromptText("请输入任务参数");
                executorParamArea.setPrefRowCount(4);
                executorParamLabel.setText("任务参数");
                executorParamLabel.setGraphic(null);
                expandDataxJsonButton.setVisible(false);
                expandDataxJsonButton.setManaged(false);
                toggleExecutorParamArea(true);
            }
        }
        // GLUE脚本和API配置根据运行模式动态显示，但不影响高级配置的显示状态
//        glueSection.setVisible(glueType.requiresGlueSource());
//        glueSection.setManaged(glueType.requiresGlueSource());
        apiSection.setVisible(glueType == GlueType.API);
        apiSection.setManaged(glueType == GlueType.API);
    }
    
    /**
     * 更新JobHandler/数据库标签文本
     */
    private void updateHandlerLabel(String text) {
        if (executorHandlerLabel != null) {
            executorHandlerLabel.setText(text);
            // 如果是必填项，添加星号
            if (text.equals("数据库") || text.equals("JobHandler")) {
                HBox labelBox = new HBox(2);
                labelBox.setAlignment(Pos.CENTER_LEFT);
                
                Label textLabel = new Label(text);
                textLabel.setStyle("-fx-font-size: 13;");
                
                Label starLabel = new Label("*");
                starLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
                
                labelBox.getChildren().addAll(textLabel, starLabel);
                executorHandlerLabel.setGraphic(labelBox);
                executorHandlerLabel.setText("");
                executorHandlerLabel.setContentDisplay(ContentDisplay.LEFT);
                executorHandlerLabel.setTooltip(new Tooltip(text + " *"));
            }
        }
    }
    
    /**
     * 异步加载数据源列表
     */
    private void loadDatasourceList() {
        new Thread(() -> {
            try {
                List<JobJdbcDatasource> datasourceList = datasourceService.getDatasourceList();
                Platform.runLater(() -> {
                    datasourceCombo.getItems().clear();
                    datasourceCombo.getItems().addAll(datasourceList);
                    
                    // 如果有待设置的数据源ID，现在设置它
                    if (pendingDatasourceId != null) {
                        setDatasourceValue(pendingDatasourceId);
                        pendingDatasourceId = null;
                    }
                });
            } catch (Exception e) {
                logger.error("加载数据源列表失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("加载失败");
                    alert.setHeaderText("无法加载数据源列表");
                    alert.setContentText("请检查网络连接或后端服务是否正常");
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    /**
     * 设置数据源下拉框的值
     */
    private void setDatasourceValue(Long datasourceId) {
        if (datasourceId == null || datasourceCombo == null) {
            return;
        }
        
        datasourceCombo.getItems().stream()
            .filter(ds -> ds.getId().equals(datasourceId))
            .findFirst()
            .ifPresent(datasourceCombo::setValue);
    }
    
    /**
     * 打开 GLUE IDE 对话框
     */
    private void openGlueIdeDialog() {
        Stage ownerStage = (Stage) getDialogPane().getScene().getWindow();
        
        // 获取当前 GLUE 类型
        GlueType currentGlueType = glueTypeCombo.getValue();
        if (currentGlueType == null || !currentGlueType.requiresGlueSource()) {
            return; // 不是GLUE模式，不打开对话框
        }
        
        // 获取当前 GLUE 代码和备注
        String currentCode = glueEditorArea.getText();
        String currentRemark = glueRemark != null ? glueRemark : "";
        
        // 检查代码是否与当前GLUE类型匹配
        // 如果代码为空，或者GLUE类型发生了变化，加载对应类型的默认模板
        boolean shouldLoadTemplate = false;
        if (currentCode == null || currentCode.trim().isEmpty()) {
            shouldLoadTemplate = true;
        } else if (lastGlueType != null && !lastGlueType.getType().equals(currentGlueType.getType())) {
            // GLUE类型发生了变化，应该加载新类型的默认模板
            shouldLoadTemplate = true;
        }
        
        if (shouldLoadTemplate) {
            String defaultTemplate = com.cc.job.gui.util.GlueTemplateUtil.getDefaultTemplate(currentGlueType.getType());
            if (defaultTemplate != null && !defaultTemplate.trim().isEmpty()) {
                currentCode = defaultTemplate;
                // 同时更新到编辑区域
                glueEditorArea.setText(defaultTemplate);
            }
        }
        
        // 获取任务ID（如果是编辑模式）
        Long taskId = formData.getId();
        
        // 传递当前GLUE类型，用于过滤历史记录
        String currentGlueTypeStr = currentGlueType.getType();
        GlueIdeDialog dialog = new GlueIdeDialog(ownerStage, taskId, currentCode, currentRemark, currentGlueTypeStr);
        dialog.showAndWait();
        
        // 对话框关闭后，同步代码和备注
        String newCode = dialog.getCode();
        String newRemark = dialog.getRemark();
        if (newCode != null && !newCode.equals(currentCode)) {
            glueEditorArea.setText(newCode);
        }
        if (newRemark != null) {
            glueRemark = newRemark;
        }
        
        // 更新记录的GLUE类型
        lastGlueType = currentGlueType;
    }
    
    /**
     * 打开数据源同步对话框，仅配置 DataX JSON 并带回节点参数
     */
    private void openDataxConfigDialog() {
        Stage ownerStage = (Stage) getDialogPane().getScene().getWindow();
        String initialJson = executorParamArea.getActualText() != null ? executorParamArea.getActualText().trim() : "";
        ShowDataxSyncDialog dialog = new ShowDataxSyncDialog(ownerStage, initialJson, json -> {
            if (json != null && !json.trim().isEmpty()) {
                Platform.runLater(() -> executorParamArea.replaceText(0, executorParamArea.getLength(), json));
            }
        });
        dialog.showAndWait();
    }
    
    /**
     * 打开 dataxJson 放大编辑弹窗：大号 CodeArea、行号、JSON 高亮、确定时回写
     */
    private void openDataxJsonExpandDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("dataxJson - 放大编辑");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(true);
        codeArea.setWrapText(false);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("log-code-area");
        codeArea.getStyleClass().add("json-editor-area");
        codeArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 13px;");
        String currentText = executorParamArea.getActualText();
        codeArea.replaceText(0, codeArea.getLength(), currentText != null ? currentText : "");

        ContextMenu ctx = new ContextMenu();
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setOnAction(e -> {
            String text = codeArea.getSelectedText();
            if (text == null || text.isEmpty()) text = codeArea.getText();
            if (text != null && !text.isEmpty()) {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        MenuItem selectAllItem = new MenuItem("全选");
        selectAllItem.setOnAction(e -> codeArea.selectAll());
        ctx.getItems().addAll(copyItem, selectAllItem);
        codeArea.setContextMenu(ctx);

        codeArea.textProperty().addListener((obs, oldVal, newVal) -> applyJsonHighlightingForDatax(codeArea));

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setStyle(StyleUtil.dialogScrollPaneBackgroundStyle());
        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.setPrefWidth(900);
        content.setPrefHeight(600);
        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(920, 640);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? codeArea.getText() : null);

        String dialogCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            dialog.getDialogPane().getStylesheets().add(dialogCss);
        }

        applyJsonHighlightingForDatax(codeArea);

        dialog.showAndWait().ifPresent(result -> {
            if (result != null) {
                executorParamArea.replaceText(0, executorParamArea.getLength(), result);
            }
        });
    }

    /**
     * 为 CodeArea 应用 JSON 语法高亮（用于 dataxJson 放大编辑）
     */
    private void applyJsonHighlightingForDatax(CodeArea codeArea) {
        String text = codeArea.getText();
        if (text == null || text.isEmpty()) return;
        try {
            Pattern pattern = Pattern.compile("(\"(?:[^\"\\\\]|\\\\.)*\")|(-?\\d+\\.?\\d*([eE][+-]?\\d+)?)|(true|false)|(null)|([{}\\[\\]:,])");
            Matcher matcher = pattern.matcher(text);
            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            int lastEnd = 0;
            while (matcher.find()) {
                if (matcher.start() > lastEnd) {
                    builder.add(Collections.emptyList(), matcher.start() - lastEnd);
                }
                int len = matcher.group(0).length();
                if (matcher.group(1) != null) {
                    builder.add(Collections.singleton("json-string"), len);
                } else if (matcher.group(2) != null) {
                    builder.add(Collections.singleton("json-number"), len);
                } else if (matcher.group(4) != null) {
                    builder.add(Collections.singleton("json-boolean"), len);
                } else if (matcher.group(5) != null) {
                    builder.add(Collections.singleton("json-null"), len);
                } else if (matcher.group(6) != null) {
                    builder.add(Collections.singleton("json-bracket"), len);
                } else {
                    builder.add(Collections.emptyList(), len);
                }
                lastEnd = matcher.end();
            }
            if (lastEnd < text.length()) {
                builder.add(Collections.emptyList(), text.length() - lastEnd);
            }
            StyleSpans<Collection<String>> spans = builder.create();
            if (spans.length() > 0) {
                codeArea.setStyleSpans(0, spans);
            }
        } catch (Exception ignored) {
            // 高亮失败时保持原样
        }
    }
    
    /**
     * 创建section容器
     */
    private VBox createSection(String title, FontIcon icon) {
        VBox section = new VBox(10);
        section.getStyleClass().add("dialog-section");
        
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 5, 0));
        
        // 设置图标样式
        if (icon != null) {
            icon.setIconSize(18);
            icon.setIconColor(Color.web("#6B7280"));
        }
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold;"
        );
        
        if (icon != null) {
            titleBox.getChildren().add(icon);
        }
        titleBox.getChildren().add(titleLabel);
        
        section.getChildren().add(titleBox);
        return section;
    }
    
    /**
     * 创建表单标签
     */
    private Label createFormLabel(String text, boolean required) {
        Label label = new Label();
        label.setMinWidth(140);
        label.setPrefWidth(140);
        label.setMaxWidth(140);
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        
        if (required) {
            // 必填项：使用HBox来组合文本和红色星号
            HBox labelBox = new HBox(2);
            labelBox.setAlignment(Pos.CENTER_LEFT);
            
            Label textLabel = new Label(text);
            textLabel.setStyle("-fx-font-size: 13;");
            
            Label starLabel = new Label("*");
            starLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
            
            labelBox.getChildren().addAll(textLabel, starLabel);
            
            // 使用自定义图形节点
            label.setGraphic(labelBox);
            label.setText("");
            label.setContentDisplay(ContentDisplay.LEFT);
            label.setTooltip(new Tooltip(text + " *"));
        } else {
            label.setText(text);
            label.setStyle("-fx-font-size: 13;");
            label.setTooltip(new Tooltip(text));
        }
        
        return label;
    }
    
    /**
     * 检查是否有高级配置数据
     */
    private boolean hasAdvancedConfig(JobInfoForm data) {
        // 检查是否有非默认值的高级配置
        if (data.getExecutorRouteStrategy() != null && !data.getExecutorRouteStrategy().equals("FIRST")) {
            return true;
        }
        // 调度过期策略固定为DO_NOTHING，不需要检查
        if (data.getExecutorBlockStrategy() != null && !data.getExecutorBlockStrategy().equals("SERIAL_EXECUTION")) {
            return true;
        }
        if (data.getExecutorTimeout() != null && data.getExecutorTimeout() != 300) {
            return true;
        }
        if (data.getExecutorFailRetryCount() != null && data.getExecutorFailRetryCount() != 0) {
            return true;
        }
        // 普通任务/任务组：检查子任务ID
        if (parentJobId == null && data.getChildJobId() != null && !data.getChildJobId().trim().isEmpty()) {
            return true;
        }
        // 任务组内子节点：检查失败策略
        if (parentJobId != null && data.getFailStrategy() != null && !data.getFailStrategy().equals("JOB_FAIL")) {
            return true;
        }
        return false;
    }
    
    /**
     * 填充表单数据（编辑模式）
     */
    private void fillFormData(JobInfoForm data, List<JobGroup> jobGroupList) {
        if (data.getJobGroup() != null) {
            jobGroupList.stream()
                .filter(jg -> jg.getId().equals(data.getJobGroup()))
                .findFirst()
                .ifPresent(jobGroupCombo::setValue);
        }
        if (data.getJobDesc() != null) jobDescField.setText(data.getJobDesc());
        if (data.getAuthor() != null) authorField.setText(data.getAuthor());
        if (data.getAlarmEmail() != null) alarmEmailField.setText(data.getAlarmEmail());

        GlueType glueType = GlueType.fromType(data.getGlueType());
        glueTypeCombo.setValue(glueType);
        lastGlueType = glueType; // 记录初始的GLUE类型
        updateFieldsForGlueType(glueType);

        if (data.getExecutorHandler() != null) {
            executorHandlerField.setText(data.getExecutorHandler());
        }
        
        // SQL模式：设置数据源下拉框的值
        if (glueType == GlueType.SQL && data.getJdbcDatasourceId() != null) {
            // 如果数据源列表已经加载，直接设置值；否则保存待设置的数据源ID
            if (!datasourceCombo.getItems().isEmpty()) {
                // 数据源列表已加载，直接设置
                setDatasourceValue(data.getJdbcDatasourceId());
            } else {
                // 数据源列表还未加载，保存待设置的数据源ID
                pendingDatasourceId = data.getJdbcDatasourceId();
            }
        }

        if (glueType.requiresGlueSource() && data.getGlueSource() != null) {
            glueEditorArea.setText(data.getGlueSource());
        }
        if (data.getGlueRemark() != null) {
            glueRemark = data.getGlueRemark();
        }

        if (glueType == GlueType.API) {
            if (data.getReqType() != null) {
                reqTypeCombo.setValue(data.getReqType());
                // 设置请求类型后，更新请求体可见性
                updateRequestBodyVisibility(data.getReqType());
            }
            if (data.getReqUrl() != null) {
                reqUrlField.setText(data.getReqUrl());
            }
            if (data.getReqBody() != null) {
                reqBodyArea.setTextWithEscape(data.getReqBody());
            }
            // 只使用一个参数表格，优先使用executorParam，如果没有则使用reqHeader
            String paramData = data.getExecutorParam();
            if (paramData == null || paramData.trim().isEmpty()) {
                paramData = data.getReqHeader();
            }
            bodyTable.setData(paramData);
        } else {
            if (data.getExecutorParam() != null) {
                executorParamArea.setTextWithEscape(data.getExecutorParam());
            }
            bodyTable.setData(null);
        }

        if (data.getExecutorRouteStrategy() != null) {
            try {
                routeStrategyCombo.setValue(RouteStrategy.valueOf(data.getExecutorRouteStrategy()));
            } catch (Exception e) {
                routeStrategyCombo.setValue(RouteStrategy.FIRST);
            }
        }
        // 调度过期策略固定为DO_NOTHING，不需要从表单数据填充
        if (data.getExecutorBlockStrategy() != null) {
            try {
                blockStrategyCombo.setValue(BlockStrategy.valueOf(data.getExecutorBlockStrategy()));
            } catch (Exception e) {
                blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
            }
        }
        if (data.getExecutorTimeout() != null) executorTimeoutField.setText(String.valueOf(data.getExecutorTimeout()));
        if (data.getExecutorFailRetryCount() != null) executorFailRetryCountSpinner.getValueFactory().setValue(data.getExecutorFailRetryCount());

        // 任务组内子节点：填充失败策略
        if (data.getFailStrategy() != null) {
            try {
                failStrategyCombo.setValue(FailStrategy.valueOf(data.getFailStrategy()));
            } catch (Exception e) {
                failStrategyCombo.setValue(FailStrategy.JOB_FAIL);
            }
        }
    }
    
    /**
     * 收集表单数据
     */
    private JobInfoForm collectFormData() {
        JobInfoForm form = new JobInfoForm();
        
        if (formData.getId() != null) {
            form.setId(formData.getId());
        }
        
        // 基本信息
        form.setParentId(parentJobId);
        form.setJobGroup(jobGroupCombo.getValue() != null ? jobGroupCombo.getValue().getId() : null);
        form.setJobDesc(jobDescField.getText().trim());
        form.setAuthor(authorField.getText().trim());
        form.setAlarmEmail(alarmEmailField.getText().trim());
        
        // 执行配置
        GlueType glueType = glueTypeCombo.getValue();
        form.setGlueType(glueType.getType());
        
        switch (glueType) {
            case SQL -> {
                form.setExecutorHandler("runJobJdbcXxlJob");
                // 设置数据源ID
                if (datasourceCombo.getValue() != null) {
                    form.setJdbcDatasourceId(datasourceCombo.getValue().getId());
                }
            }
            case API -> form.setExecutorHandler("runApiHandler");
            case DATAX -> form.setExecutorHandler("runDataxHandler");
            default -> form.setExecutorHandler(executorHandlerField.getText().trim());
        }

        if (glueType.requiresGlueSource()) {
            String glueSource = glueEditorArea.getText();
            // 如果为空字符串，设置为 null，避免传递空字符串
            form.setGlueSource(glueSource != null && !glueSource.trim().isEmpty() ? glueSource : null);
            // 设置 GLUE 备注
            form.setGlueRemark(glueRemark != null && !glueRemark.trim().isEmpty() ? glueRemark.trim() : null);
        } else {
            form.setGlueSource(null);
            form.setGlueRemark(null);
        }

        if (glueType == GlueType.API) {
            String reqType = reqTypeCombo.getValue();
            form.setReqType(reqType != null ? reqType : "GET");
            String reqUrl = reqUrlField.getText() != null ? reqUrlField.getText().trim() : "";
            form.setReqUrl(reqUrl);
            // 如果是POST或PUT，设置请求体
            if ("POST".equals(reqType) || "PUT".equals(reqType)) {
                String reqBody = reqBodyArea.getActualText() != null ? reqBodyArea.getActualText().trim() : "";
                form.setReqBody(reqBody.isEmpty() ? null : reqBody);
            } else {
                form.setReqBody(null);
            }
            form.setReqHeader(null); // 不再使用请求头表格
            form.setExecutorParam(bodyTable.toJson());
        } else {
            form.setReqType(null);
            form.setReqUrl(null);
            form.setReqHeader(null);
            form.setExecutorParam(executorParamArea.getActualText() != null ? 
                    executorParamArea.getActualText().trim() : "");
        }
        
        // 高级配置
        form.setExecutorRouteStrategy(routeStrategyCombo.getValue().getType());
        // 调度过期策略固定为DO_NOTHING
        form.setMisfireStrategy(MisfireStrategy.DO_NOTHING.getType());
        form.setExecutorBlockStrategy(blockStrategyCombo.getValue().getType());
        
        try {
            form.setExecutorTimeout(Integer.parseInt(executorTimeoutField.getText().trim()));
        } catch (NumberFormatException e) {
            form.setExecutorTimeout(300);
        }
        
        form.setExecutorFailRetryCount(executorFailRetryCountSpinner.getValue());

        // 任务组内子节点：设置失败策略
        form.setFailStrategy(failStrategyCombo.getValue().getType());
        form.setChildJobId(null); // 子节点不需要子任务ID

        // 固定字段
        form.setScheduleType("NONE");
        form.setJobType(0); // 普通任务节点

        return form;
    }
    
    /**
     * 验证表单
     */
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        if (jobGroupCombo.getValue() == null) {
            errors.append("• 请选择执行器\n");
        }
        if (jobDescField.getText().trim().isEmpty()) {
            errors.append("• 请输入任务描述\n");
        }
        if (authorField.getText().trim().isEmpty()) {
            errors.append("• 请输入负责人\n");
        }
        if (glueTypeCombo.getValue() == null) {
            errors.append("• 请选择运行模式\n");
        }
        GlueType glueType = glueTypeCombo.getValue();
        if (glueType == GlueType.BEAN && executorHandlerField.getText().trim().isEmpty()) {
            errors.append("• 请输入JobHandler\n");
        }
        if (glueType == GlueType.SQL && datasourceCombo.getValue() == null) {
            errors.append("• 请选择数据库\n");
        }
        if (glueType != null && glueType.requiresGlueSource() && (glueEditorArea.getText() == null || glueEditorArea.getText().trim().isEmpty())) {
            errors.append("• 请填写GLUE脚本内容\n");
        }
        if (glueType == GlueType.API) {
            if (reqUrlField.getText() == null || reqUrlField.getText().trim().isEmpty()) {
                errors.append("• 请填写API请求地址\n");
            }
            if (bodyTable.isEmpty()) {
                errors.append("• 请至少配置一个请求参数\n");
            }
        }
        if (glueType == GlueType.DATAX) {
            String param = executorParamArea.getActualText();
            if (param == null || param.trim().isEmpty()) {
                errors.append("• 请配置 datax 或填写 dataxJson\n");
            }
        }
        if (routeStrategyCombo.getValue() == null) {
            errors.append("• 请选择路由策略\n");
        }
        if (blockStrategyCombo.getValue() == null) {
            errors.append("• 请选择阻塞处理策略\n");
        }
        // 任务组内子节点需要验证失败策略
        if (parentJobId != null && failStrategyCombo.getValue() == null) {
            errors.append("• 请选择失败策略\n");
        }
        
        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("验证失败");
            alert.setHeaderText("请完善以下必填项：");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }
        
        return true;
    }
    
    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        getDialogPane().setPrefWidth(750);
        // 初始高度较小，只显示基本信息和执行配置
        getDialogPane().setPrefHeight(450);
        getDialogPane().setMinWidth(720);
        getDialogPane().setMinHeight(450); // 最小高度也相应减小
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);
        
        String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (css != null && !css.isEmpty()) {
            getDialogPane().getStylesheets().add(css);
        }
        
        
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
                stage.setMinWidth(720);
                // 初始最小高度较小
                stage.setMinHeight(450);
            }
        });
    }

    private static class ParameterTable extends VBox {
        private final TableView<ParamItem> tableView;
        private final ObservableList<ParamItem> items = FXCollections.observableArrayList();

        ParameterTable(String title) {
            setSpacing(8);
            setPadding(new Insets(12));
            boolean dark = StyleUtil.isDarkTheme();
            if (dark) {
                setStyle("-fx-background-color: #2D2D30; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #3C3C3C; -fx-border-width: 1;");
            } else {
                setStyle("-fx-background-color: linear-gradient(145deg,#6847FF,#8A6BFF); -fx-border-radius: 10; -fx-background-radius: 10;");
            }

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: " + (dark ? "#D4D4D4" : "rgba(255,255,255,0.9)") + "; -fx-font-size: 13; -fx-font-weight: bold;");

            tableView = new TableView<>();
            tableView.setEditable(true);
            tableView.setItems(items);
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            tableView.setPlaceholder(new Label("暂无数据，点击“新增参数”添加"));
            tableView.setPrefHeight(160);

            TableColumn<ParamItem, String> keyColumn = new TableColumn<>("参数名称");
            keyColumn.setCellValueFactory(cell -> cell.getValue().keyProperty());
            keyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            keyColumn.setOnEditCommit(event -> event.getRowValue().setKey(event.getNewValue()));

            TableColumn<ParamItem, String> valueColumn = new TableColumn<>("参数值");
            valueColumn.setCellValueFactory(cell -> cell.getValue().valueProperty());
            valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            valueColumn.setOnEditCommit(event -> event.getRowValue().setValue(event.getNewValue()));

            TableColumn<ParamItem, Void> actionColumn = new TableColumn<>("操作");
            actionColumn.setPrefWidth(80);
            actionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button deleteButton = new Button("删除");
                {
                    // 使用红色背景，使按钮更明显
                    deleteButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 4 10; -fx-background-radius: 4; -fx-border-color: #CC0000; -fx-border-radius: 4; -fx-cursor: hand;");
                    deleteButton.setOnMouseEntered(e -> deleteButton.setStyle("-fx-background-color: #FF6666; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 4 10; -fx-background-radius: 4; -fx-border-color: #CC0000; -fx-border-radius: 4; -fx-cursor: hand;"));
                    deleteButton.setOnMouseExited(e -> deleteButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 4 10; -fx-background-radius: 4; -fx-border-color: #CC0000; -fx-border-radius: 4; -fx-cursor: hand;"));
                    deleteButton.setOnAction(e -> {
                        ParamItem item = getTableView().getItems().get(getIndex());
                        items.remove(item);
                        ensureAtLeastOneRow();
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : deleteButton);
                }
            });

            tableView.getColumns().add(keyColumn);
            tableView.getColumns().add(valueColumn);
            tableView.getColumns().add(actionColumn);

            Button addButton = new Button("+ 新增参数");
            if (dark) {
                addButton.setStyle("-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 14; -fx-background-radius: 4; -fx-border-color: #094771; -fx-border-radius: 4; -fx-cursor: hand;");
                addButton.setOnMouseEntered(e -> addButton.setStyle("-fx-background-color: #1E88E5; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 14; -fx-background-radius: 4; -fx-border-color: #094771; -fx-border-radius: 4; -fx-cursor: hand;"));
                addButton.setOnMouseExited(e -> addButton.setStyle("-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 14; -fx-background-radius: 4; -fx-border-color: #094771; -fx-border-radius: 4; -fx-cursor: hand;"));
            } else {
                addButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 14; -fx-background-radius: 4; -fx-border-color: #357ABD; -fx-border-radius: 4; -fx-cursor: hand;");
                addButton.setOnMouseEntered(e -> addButton.setStyle("-fx-background-color: #5BA0F2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 14; -fx-background-radius: 4; -fx-border-color: #357ABD; -fx-border-radius: 4; -fx-cursor: hand;"));
                addButton.setOnMouseExited(e -> addButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 14; -fx-background-radius: 4; -fx-border-color: #357ABD; -fx-border-radius: 4; -fx-cursor: hand;"));
            }
            addButton.setOnAction(e -> addRow("", ""));

            // 将新增按钮放在表格上方
            HBox buttonContainer = new HBox();
            buttonContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            buttonContainer.getChildren().add(addButton);

            getChildren().addAll(titleLabel, buttonContainer, tableView);
            ensureAtLeastOneRow();
        }

        void ensureAtLeastOneRow() {
            if (items.isEmpty()) {
                addRow("", "");
            }
        }

        private void addRow(String key, String value) {
            items.add(new ParamItem(key, value));
        }

        void setData(String json) {
            items.clear();
            if (json != null && !json.trim().isEmpty()) {
                try {
                    Map<String, String> map = GSON.fromJson(json, MAP_TYPE);
                    if (map != null) {
                        map.forEach(this::addRow);
                    }
                } catch (Exception ignored) {
                }
            }
            ensureAtLeastOneRow();
        }

        String toJson() {
            Map<String, String> map = new LinkedHashMap<>();
            for (ParamItem item : items) {
                String key = item.getKey().trim();
                if (!key.isEmpty()) {
                    map.put(key, item.getValue());
                }
            }
            return map.isEmpty() ? null : GSON.toJson(map);
        }

        boolean isEmpty() {
            for (ParamItem item : items) {
                if (!item.getKey().trim().isEmpty() || !item.getValue().trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class ParamItem {
        private final SimpleStringProperty key = new SimpleStringProperty("");
        private final SimpleStringProperty value = new SimpleStringProperty("");

        ParamItem(String key, String value) {
            this.key.set(key != null ? key : "");
            this.value.set(value != null ? value : "");
        }

        String getKey() {
            return key.get() != null ? key.get() : "";
        }

        void setKey(String key) {
            this.key.set(key != null ? key : "");
        }

        String getValue() {
            return value.get() != null ? value.get() : "";
        }

        void setValue(String value) {
            this.value.set(value != null ? value : "");
        }

        SimpleStringProperty keyProperty() {
            return key;
        }

        SimpleStringProperty valueProperty() {
            return value;
        }
    }

    // 枚举类型定义
    public enum GlueType {
        BEAN("BEAN", "BEAN", false, false),
        API("API", "API", false, true),
        SQL("SQL", "SQL", false, false),
        DATAX("DATAX", "datax任务", false, false),
        GLUE_GROOVY("GLUE_GROOVY", "GLUE(Java)", true, false),
        GLUE_SHELL("GLUE_SHELL", "GLUE(Shell)", true, false),
        GLUE_PYTHON("GLUE_PYTHON", "GLUE(Python)", true, false),
        GLUE_PHP("GLUE_PHP", "GLUE(PHP)", true, false),
        GLUE_NODEJS("GLUE_NODEJS", "GLUE(Nodejs)", true, false),
        GLUE_POWERSHELL("GLUE_POWERSHELL", "GLUE(PowerShell)", true, false),
        GLUE_CSHARP("GLUE_CSHARP", "GLUE(C#)", true, false);
        
        private final String type;
        private final String title;
        private final boolean requiresGlueSource;
        private final boolean apiMode;
        
        GlueType(String type, String title, boolean requiresGlueSource, boolean apiMode) {
            this.type = type;
            this.title = title;
            this.requiresGlueSource = requiresGlueSource;
            this.apiMode = apiMode;
        }
        
        public String getType() { return type; }
        public boolean requiresGlueSource() { return requiresGlueSource; }
        public boolean isApi() { return apiMode; }
        
        public static GlueType fromType(String type) {
            if (type != null) {
                for (GlueType value : values()) {
                    if (value.type.equalsIgnoreCase(type)) {
                        return value;
                    }
                }
            }
            return BEAN;
        }
        
        @Override
        public String toString() { return title; }
    }
    
    public enum RouteStrategy {
        FIRST("FIRST", "第一个"),
        LAST("LAST", "最后一个"),
        ROUND("ROUND", "轮询"),
        RANDOM("RANDOM", "随机"),
        CONSISTENT_HASH("CONSISTENT_HASH", "一致性哈希"),
        LEAST_FREQUENTLY_USED("LEAST_FREQUENTLY_USED", "最不经常使用"),
        LEAST_RECENTLY_USED("LEAST_RECENTLY_USED", "最近最久未使用"),
        FAILOVER("FAILOVER", "故障转移"),
        BUSYOVER("BUSYOVER", "忙碌转移"),
        SHARDING_BROADCAST("SHARDING_BROADCAST", "分片广播");
        
        private final String type;
        private final String title;
        
        RouteStrategy(String type, String title) {
            this.type = type;
            this.title = title;
        }
        
        public String getType() { return type; }
        @Override
        public String toString() { return title; }
    }
    
    public enum MisfireStrategy {
        DO_NOTHING("DO_NOTHING", "忽略"),
        FIRE_ONCE_NOW("FIRE_ONCE_NOW", "立即执行一次");
        
        private final String type;
        private final String title;
        
        MisfireStrategy(String type, String title) {
            this.type = type;
            this.title = title;
        }
        
        public String getType() { return type; }
        @Override
        public String toString() { return title; }
    }
    
    public enum FailStrategy {
        DO_NOTHING("DO_NOTHING", "忽略"),
        JOB_FAIL("JOB_FAIL", "失败");
        
        private final String type;
        private final String title;
        
        FailStrategy(String type, String title) {
            this.type = type;
            this.title = title;
        }
        
        public String getType() { return type; }
        @Override
        public String toString() { return title; }
    }
    
    public enum BlockStrategy {
        SERIAL_EXECUTION("SERIAL_EXECUTION", "单机串行"),
        DISCARD_LATER("DISCARD_LATER", "丢弃后续调度"),
        COVER_EARLY("COVER_EARLY", "覆盖之前调度");
        
        private final String type;
        private final String title;
        
        BlockStrategy(String type, String title) {
            this.type = type;
            this.title = title;
        }
        
        public String getType() { return type; }
        @Override
        public String toString() { return title; }
    }
}

