package com.cc.job.gui.view;

import com.cc.job.gui.service.JobJdbcDatasourceService;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 新建/编辑普通任务对话框
 * 用于导航栏"新增任务"功能，包含调度配置和子任务ID
 */
public class NewJobDialog extends Dialog<JobInfoForm> {
    
    private static final Logger logger = LoggerFactory.getLogger(NewJobDialog.class);
    
    private JobInfoForm formData;
    
    // 基础配置
    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField authorField;
    private TextField alarmEmailField;
    
    // 调度配置
    private ComboBox<ScheduleType> scheduleTypeCombo;
    private TextField scheduleConfField;
    private Label scheduleConfLabel;
    
    // 任务配置
    private ComboBox<GlueType> glueTypeCombo;
    private Label executorHandlerLabel;
    private TextField executorHandlerField;
    private ComboBox<JobJdbcDatasource> datasourceCombo;
    private Button glueIdeButton;
    private TextArea executorParamArea;
    private Label executorParamLabel;
    
    private JobJdbcDatasourceService datasourceService;
    private Long pendingDatasourceId;
    private TextArea glueEditorArea;
    private String glueRemark;
    private GlueType lastGlueType = null;
    
    // 高级配置
    private ComboBox<RouteStrategy> routeStrategyCombo;
    private ComboBox<MisfireStrategy> misfireStrategyCombo;
    private ComboBox<BlockStrategy> blockStrategyCombo;
    private TextField childJobIdField;
    private TextField executorTimeoutField;
    private TextField executorFailRetryCountField;
    
    private ButtonType saveButtonType;
    private ButtonType cancelButtonType;
    
    // 高级配置容器
    private VBox advancedSection;
    private boolean advancedSectionVisible = false;
    
    public NewJobDialog(Stage owner, JobInfoForm editData, List<JobGroup> jobGroupList) {
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
        saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, saveButtonType);
        
        // 设置样式
        styleDialog();
        
        // 填充数据（编辑模式）
        if (editData != null) {
            fillFormData(editData, jobGroupList);
            if (hasAdvancedConfig(editData)) {
                Platform.runLater(this::toggleAdvancedSection);
            }
        }
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                if (validateForm()) {
                    return collectFormData();
                }
                return null;
            }
            return null;
        });
        
        // 拦截保存按钮的点击事件
        Platform.runLater(() -> {
            Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
            if (saveButton != null) {
                saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                    if (!validateForm()) {
                        event.consume();
                    }
                });
            }
        });
        
        // 监听GlueType变化
        glueTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && newVal != null && 
                oldVal.requiresGlueSource() && newVal.requiresGlueSource() &&
                !oldVal.getType().equals(newVal.getType()) &&
                glueEditorArea != null) {
                glueEditorArea.clear();
            }
            lastGlueType = newVal;
            updateFieldsForGlueType(newVal);
        });
        
        // 监听调度类型变化
        scheduleTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateScheduleConfLabel(newVal);
        });
        
        // 初始化字段可见性
        if (glueTypeCombo.getValue() != null) {
            lastGlueType = glueTypeCombo.getValue();
            updateFieldsForGlueType(glueTypeCombo.getValue());
        }
    }
    
    private VBox createContent(List<JobGroup> jobGroupList) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(800);
        container.setPrefHeight(550);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(10));
        
        // 基础配置
        formContent.getChildren().add(createBasicSection(jobGroupList));
        
        // 调度配置
        formContent.getChildren().add(createScheduleSection());
        
        // 任务配置
        formContent.getChildren().add(createTaskSection());
        
        // 高级配置（默认隐藏）
        advancedSection = createAdvancedSection();
        advancedSection.setVisible(false);
        advancedSection.setManaged(false);
        formContent.getChildren().add(advancedSection);
        
        scrollPane.setContent(formContent);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        container.getChildren().add(scrollPane);
        
        return container;
    }
    
    private VBox createBasicSection(List<JobGroup> jobGroupList) {
        VBox section = createSection("基础配置", IconUtil.settingsIcon());
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 执行器
        Label jobGroupLabel = createFormLabel("执行器", true);
        jobGroupCombo = new ComboBox<>();
        jobGroupCombo.setPrefWidth(300);
        jobGroupCombo.setPromptText("选择执行器");
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
        
        // 任务描述
        Label jobDescLabel = createFormLabel("任务描述", true);
        jobDescField = new TextField();
        jobDescField.setPrefWidth(300);
        jobDescField.setPromptText("任务描述");
        
        // 负责人
        Label authorLabel = createFormLabel("负责人", true);
        authorField = new TextField();
        authorField.setPrefWidth(300);
        authorField.setPromptText("输入负责人");
        
        // 报警邮件
        Label alarmEmailLabel = createFormLabel("报警邮件", false);
        alarmEmailField = new TextField();
        alarmEmailField.setPrefWidth(300);
        alarmEmailField.setPromptText("");
        
        grid.add(jobGroupLabel, 0, 0);
        grid.add(jobGroupCombo, 1, 0);
        grid.add(jobDescLabel, 2, 0);
        grid.add(jobDescField, 3, 0);
        
        grid.add(authorLabel, 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(alarmEmailLabel, 2, 1);
        grid.add(alarmEmailField, 3, 1);
        
        section.getChildren().add(grid);
        return section;
    }
    
    private VBox createScheduleSection() {
        VBox section = createSection("调度配置", IconUtil.clockIcon());
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 调度类型
        Label scheduleTypeLabel = createFormLabel("调度类型", true);
        scheduleTypeCombo = new ComboBox<>();
        scheduleTypeCombo.setPrefWidth(300);
        scheduleTypeCombo.setPromptText("Select");
        scheduleTypeCombo.getItems().addAll(ScheduleType.values());
        scheduleTypeCombo.setValue(ScheduleType.CRON);
        
        // CRON表达式/固定速率
        scheduleConfLabel = createFormLabel("Cron", false);
        scheduleConfField = new TextField();
        scheduleConfField.setPrefWidth(300);
        scheduleConfField.setPromptText("请输入Cron表达式");
        scheduleConfField.setText("0 0 0 * * ?");
        
        grid.add(scheduleTypeLabel, 0, 0);
        grid.add(scheduleTypeCombo, 1, 0);
        grid.add(scheduleConfLabel, 2, 0);
        grid.add(scheduleConfField, 3, 0);
        
        section.getChildren().add(grid);
        return section;
    }
    
    private void updateScheduleConfLabel(ScheduleType type) {
        if (type == null) return;
        switch (type) {
            case CRON -> {
                scheduleConfLabel.setText("Cron");
                scheduleConfField.setPromptText("请输入Cron表达式");
                scheduleConfField.setVisible(true);
                scheduleConfField.setManaged(true);
                scheduleConfLabel.setVisible(true);
                scheduleConfLabel.setManaged(true);
            }
            case FIX_RATE -> {
                scheduleConfLabel.setText("固定速率(秒)");
                scheduleConfField.setPromptText("请输入固定速率（秒）");
                scheduleConfField.setVisible(true);
                scheduleConfField.setManaged(true);
                scheduleConfLabel.setVisible(true);
                scheduleConfLabel.setManaged(true);
            }
            case NONE -> {
                scheduleConfField.setVisible(false);
                scheduleConfField.setManaged(false);
                scheduleConfLabel.setVisible(false);
                scheduleConfLabel.setManaged(false);
            }
        }
    }
    
    private VBox createTaskSection() {
        VBox section = createSection("任务配置", IconUtil.fileIcon());
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 运行模式
        Label glueTypeLabel = createFormLabel("运行模式", true);
        glueTypeCombo = new ComboBox<>();
        glueTypeCombo.setPrefWidth(300);
        glueTypeCombo.setPromptText("选择运行模式");
        glueTypeCombo.getItems().addAll(GlueType.values());
        glueTypeCombo.setValue(GlueType.BEAN);
        
        // JobHandler/数据库
        executorHandlerLabel = createFormLabel("JobHandler", true);
        executorHandlerField = new TextField();
        executorHandlerField.setPrefWidth(300);
        executorHandlerField.setPromptText("请输入JobHandler名称");
        
        // 数据库下拉框
        datasourceCombo = new ComboBox<>();
        datasourceCombo.setPrefWidth(300);
        datasourceCombo.setPromptText("请选择数据库");
        datasourceCombo.setVisible(false);
        datasourceCombo.setManaged(false);
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
        loadDatasourceList();
        
        // GLUE IDE 按钮
        glueIdeButton = new Button("GLUE IDE");
        glueIdeButton.setPrefWidth(300);
        glueIdeButton.setStyle(
            "-fx-background-color: #2563EB; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 20; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        glueIdeButton.setOnAction(e -> openGlueIdeDialog());
        glueIdeButton.setVisible(false);
        glueIdeButton.setManaged(false);
        
        // 任务参数
        executorParamLabel = createFormLabel("任务参数", false);
        executorParamArea = new TextArea();
        executorParamArea.setPrefWidth(615);
        executorParamArea.setPrefRowCount(3);
        executorParamArea.setPromptText("");
        executorParamArea.setWrapText(true);
        
        grid.add(glueTypeLabel, 0, 0);
        grid.add(glueTypeCombo, 1, 0);
        
        // 使用 StackPane 来切换显示
        StackPane handlerContainer = new StackPane();
        handlerContainer.getChildren().addAll(executorHandlerField, datasourceCombo, glueIdeButton);
        grid.add(executorHandlerLabel, 2, 0);
        grid.add(handlerContainer, 3, 0);
        
        grid.add(executorParamLabel, 0, 1);
        grid.add(executorParamArea, 1, 1, 3, 1);
        
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
    
    private void toggleAdvancedSection() {
        advancedSectionVisible = !advancedSectionVisible;
        advancedSection.setVisible(advancedSectionVisible);
        advancedSection.setManaged(advancedSectionVisible);
        
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                if (advancedSectionVisible) {
                    getDialogPane().setPrefHeight(700);
                    stage.setMinHeight(700);
                } else {
                    getDialogPane().setPrefHeight(550);
                    stage.setMinHeight(550);
                }
                Platform.runLater(stage::sizeToScene);
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
        routeStrategyCombo.setPromptText("请选择");
        routeStrategyCombo.getItems().addAll(RouteStrategy.values());
        routeStrategyCombo.setValue(RouteStrategy.FIRST);
        
        // 子任务ID
        Label childJobIdLabel = createFormLabel("子任务id", false);
        childJobIdField = new TextField();
        childJobIdField.setPrefWidth(300);
        childJobIdField.setPromptText("多个子任务使用逗号分隔");
        
        // 调度过期策略
        Label misfireStrategyLabel = createFormLabel("调度过期策略", true);
        misfireStrategyCombo = new ComboBox<>();
        misfireStrategyCombo.setPrefWidth(300);
        misfireStrategyCombo.setPromptText("Select");
        misfireStrategyCombo.getItems().addAll(MisfireStrategy.values());
        misfireStrategyCombo.setValue(MisfireStrategy.DO_NOTHING);
        
        // 阻塞处理策略
        Label blockStrategyLabel = createFormLabel("阻塞处理策略", true);
        blockStrategyCombo = new ComboBox<>();
        blockStrategyCombo.setPrefWidth(300);
        blockStrategyCombo.setPromptText("Select");
        blockStrategyCombo.getItems().addAll(BlockStrategy.values());
        blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
        
        // 任务超时时间
        Label timeoutLabel = createFormLabel("任务超时时间", false);
        executorTimeoutField = new TextField();
        executorTimeoutField.setPrefWidth(300);
        executorTimeoutField.setPromptText("");
        
        // 失败重试次数
        Label retryLabel = createFormLabel("失败重试次数", false);
        executorFailRetryCountField = new TextField();
        executorFailRetryCountField.setPrefWidth(300);
        executorFailRetryCountField.setPromptText("");
        executorFailRetryCountField.setText("0");
        
        grid.add(routeStrategyLabel, 0, 0);
        grid.add(routeStrategyCombo, 1, 0);
        grid.add(childJobIdLabel, 2, 0);
        grid.add(childJobIdField, 3, 0);
        
        grid.add(misfireStrategyLabel, 0, 1);
        grid.add(misfireStrategyCombo, 1, 1);
        grid.add(blockStrategyLabel, 2, 1);
        grid.add(blockStrategyCombo, 3, 1);
        
        grid.add(timeoutLabel, 0, 2);
        grid.add(executorTimeoutField, 1, 2);
        grid.add(retryLabel, 2, 2);
        grid.add(executorFailRetryCountField, 3, 2);
        
        section.getChildren().add(grid);
        return section;
    }
    
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
                updateHandlerLabel("JobHandler");
                toggleExecutorParamArea(true);
            }
            case SQL -> {
                executorHandlerField.setVisible(false);
                executorHandlerField.setManaged(false);
                datasourceCombo.setVisible(true);
                datasourceCombo.setManaged(true);
                glueIdeButton.setVisible(false);
                glueIdeButton.setManaged(false);
                updateHandlerLabel("数据库");
                executorParamArea.setPromptText("请输入SQL语句");
                toggleExecutorParamArea(true);
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
                updateHandlerLabel("JobHandler");
                toggleExecutorParamArea(false);
            }
            default -> {
                if (isGlueMode) {
                    executorHandlerField.setVisible(false);
                    executorHandlerField.setManaged(false);
                    datasourceCombo.setVisible(false);
                    datasourceCombo.setManaged(false);
                    glueIdeButton.setVisible(true);
                    glueIdeButton.setManaged(true);
                    
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
                    updateHandlerLabel("JobHandler");
                }
                toggleExecutorParamArea(true);
            }
        }
    }
    
    private void toggleExecutorParamArea(boolean visible) {
        executorParamLabel.setVisible(visible);
        executorParamLabel.setManaged(visible);
        executorParamArea.setVisible(visible);
        executorParamArea.setManaged(visible);
    }
    
    private void updateHandlerLabel(String text) {
        if (executorHandlerLabel != null) {
            HBox labelBox = new HBox(2);
            labelBox.setAlignment(Pos.CENTER_LEFT);
            
            Label textLabel = new Label(text);
            textLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 13;");
            
            Label starLabel = new Label("*");
            starLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold;");
            
            labelBox.getChildren().addAll(textLabel, starLabel);
            executorHandlerLabel.setGraphic(labelBox);
            executorHandlerLabel.setText("");
            executorHandlerLabel.setContentDisplay(ContentDisplay.LEFT);
        }
    }
    
    private void loadDatasourceList() {
        new Thread(() -> {
            try {
                List<JobJdbcDatasource> datasourceList = datasourceService.getDatasourceList();
                Platform.runLater(() -> {
                    datasourceCombo.getItems().clear();
                    datasourceCombo.getItems().addAll(datasourceList);
                    if (pendingDatasourceId != null) {
                        setDatasourceValue(pendingDatasourceId);
                        pendingDatasourceId = null;
                    }
                });
            } catch (Exception e) {
                logger.error("加载数据源列表失败: {}", e.getMessage(), e);
            }
        }).start();
    }
    
    private void setDatasourceValue(Long datasourceId) {
        if (datasourceId == null || datasourceCombo == null) return;
        datasourceCombo.getItems().stream()
            .filter(ds -> ds.getId().equals(datasourceId))
            .findFirst()
            .ifPresent(datasourceCombo::setValue);
    }
    
    private void openGlueIdeDialog() {
        Stage ownerStage = (Stage) getDialogPane().getScene().getWindow();
        GlueType currentGlueType = glueTypeCombo.getValue();
        if (currentGlueType == null || !currentGlueType.requiresGlueSource()) return;
        
        String currentCode = glueEditorArea.getText();
        String currentRemark = glueRemark != null ? glueRemark : "";
        
        if (currentCode == null || currentCode.trim().isEmpty()) {
            String defaultTemplate = com.cc.job.gui.util.GlueTemplateUtil.getDefaultTemplate(currentGlueType.getType());
            if (defaultTemplate != null && !defaultTemplate.trim().isEmpty()) {
                currentCode = defaultTemplate;
                glueEditorArea.setText(defaultTemplate);
            }
        }
        
        Long taskId = formData.getId();
        String currentGlueTypeStr = currentGlueType.getType();
        GlueIdeDialog dialog = new GlueIdeDialog(ownerStage, taskId, currentCode, currentRemark, currentGlueTypeStr);
        dialog.showAndWait();
        
        String newCode = dialog.getCode();
        String newRemark = dialog.getRemark();
        if (newCode != null && !newCode.equals(currentCode)) {
            glueEditorArea.setText(newCode);
        }
        if (newRemark != null) {
            glueRemark = newRemark;
        }
        lastGlueType = currentGlueType;
    }
    
    private VBox createSection(String title, FontIcon icon) {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10;"
        );
        
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 5, 0));
        
        if (icon != null) {
            icon.setIconSize(18);
            icon.setIconColor(Color.web("#6B7280"));
        }
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1F2937;"
        );
        
        if (icon != null) {
            titleBox.getChildren().add(icon);
        }
        titleBox.getChildren().add(titleLabel);
        
        section.getChildren().add(titleBox);
        return section;
    }
    
    private Label createFormLabel(String text, boolean required) {
        Label label = new Label();
        label.setMinWidth(120);
        label.setPrefWidth(120);
        label.setMaxWidth(120);
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        
        if (required) {
            HBox labelBox = new HBox(2);
            labelBox.setAlignment(Pos.CENTER_LEFT);
            
            Label textLabel = new Label(text);
            textLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 13;");
            
            Label starLabel = new Label("*");
            starLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13; -fx-font-weight: bold;");
            
            labelBox.getChildren().addAll(textLabel, starLabel);
            
            label.setGraphic(labelBox);
            label.setText("");
            label.setContentDisplay(ContentDisplay.LEFT);
            label.setTooltip(new Tooltip(text + " *"));
        } else {
            label.setText(text);
            label.setStyle("-fx-text-fill: #374151; -fx-font-size: 13;");
            label.setTooltip(new Tooltip(text));
        }
        
        return label;
    }
    
    private boolean hasAdvancedConfig(JobInfoForm data) {
        if (data.getExecutorRouteStrategy() != null && !data.getExecutorRouteStrategy().equals("FIRST")) {
            return true;
        }
        if (data.getMisfireStrategy() != null && !data.getMisfireStrategy().equals("DO_NOTHING")) {
            return true;
        }
        if (data.getExecutorBlockStrategy() != null && !data.getExecutorBlockStrategy().equals("SERIAL_EXECUTION")) {
            return true;
        }
        if (data.getExecutorTimeout() != null && data.getExecutorTimeout() != 0) {
            return true;
        }
        if (data.getExecutorFailRetryCount() != null && data.getExecutorFailRetryCount() != 0) {
            return true;
        }
        if (data.getChildJobId() != null && !data.getChildJobId().trim().isEmpty()) {
            return true;
        }
        return false;
    }
    
    private void fillFormData(JobInfoForm data, List<JobGroup> jobGroupList) {
        // 基础配置
        if (data.getJobGroup() != null) {
            jobGroupList.stream()
                .filter(jg -> jg.getId().equals(data.getJobGroup()))
                .findFirst()
                .ifPresent(jobGroupCombo::setValue);
        }
        if (data.getJobDesc() != null) jobDescField.setText(data.getJobDesc());
        if (data.getAuthor() != null) authorField.setText(data.getAuthor());
        if (data.getAlarmEmail() != null) alarmEmailField.setText(data.getAlarmEmail());
        
        // 调度配置
        if (data.getScheduleType() != null) {
            try {
                scheduleTypeCombo.setValue(ScheduleType.valueOf(data.getScheduleType()));
            } catch (Exception e) {
                scheduleTypeCombo.setValue(ScheduleType.CRON);
            }
        }
        if (data.getScheduleConf() != null) scheduleConfField.setText(data.getScheduleConf());
        
        // 任务配置
        GlueType glueType = GlueType.fromType(data.getGlueType());
        glueTypeCombo.setValue(glueType);
        lastGlueType = glueType;
        updateFieldsForGlueType(glueType);
        
        if (data.getExecutorHandler() != null) {
            executorHandlerField.setText(data.getExecutorHandler());
        }
        
        if (glueType == GlueType.SQL && data.getJdbcDatasourceId() != null) {
            if (!datasourceCombo.getItems().isEmpty()) {
                setDatasourceValue(data.getJdbcDatasourceId());
            } else {
                pendingDatasourceId = data.getJdbcDatasourceId();
            }
        }
        
        if (glueType.requiresGlueSource() && data.getGlueSource() != null) {
            glueEditorArea.setText(data.getGlueSource());
        }
        if (data.getGlueRemark() != null) {
            glueRemark = data.getGlueRemark();
        }
        
        if (data.getExecutorParam() != null) {
            executorParamArea.setText(data.getExecutorParam());
        }
        
        // 高级配置
        if (data.getExecutorRouteStrategy() != null) {
            try {
                routeStrategyCombo.setValue(RouteStrategy.valueOf(data.getExecutorRouteStrategy()));
            } catch (Exception e) {
                routeStrategyCombo.setValue(RouteStrategy.FIRST);
            }
        }
        if (data.getMisfireStrategy() != null) {
            try {
                misfireStrategyCombo.setValue(MisfireStrategy.valueOf(data.getMisfireStrategy()));
            } catch (Exception e) {
                misfireStrategyCombo.setValue(MisfireStrategy.DO_NOTHING);
            }
        }
        if (data.getExecutorBlockStrategy() != null) {
            try {
                blockStrategyCombo.setValue(BlockStrategy.valueOf(data.getExecutorBlockStrategy()));
            } catch (Exception e) {
                blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
            }
        }
        if (data.getExecutorTimeout() != null) executorTimeoutField.setText(String.valueOf(data.getExecutorTimeout()));
        if (data.getExecutorFailRetryCount() != null) executorFailRetryCountField.setText(String.valueOf(data.getExecutorFailRetryCount()));
        if (data.getChildJobId() != null) childJobIdField.setText(data.getChildJobId());
    }
    
    private JobInfoForm collectFormData() {
        JobInfoForm form = new JobInfoForm();
        
        if (formData.getId() != null) {
            form.setId(formData.getId());
        }
        
        // 基础配置
        form.setJobGroup(jobGroupCombo.getValue() != null ? jobGroupCombo.getValue().getId() : null);
        form.setJobDesc(jobDescField.getText().trim());
        form.setAuthor(authorField.getText().trim());
        form.setAlarmEmail(alarmEmailField.getText().trim());
        
        // 调度配置
        form.setScheduleType(scheduleTypeCombo.getValue().getType());
        if (scheduleTypeCombo.getValue() != ScheduleType.NONE) {
            form.setScheduleConf(scheduleConfField.getText().trim());
        }
        
        // 任务配置
        GlueType glueType = glueTypeCombo.getValue();
        form.setGlueType(glueType.getType());
        
        switch (glueType) {
            case SQL -> {
                form.setExecutorHandler("runJobJdbcXxlJob");
                if (datasourceCombo.getValue() != null) {
                    form.setJdbcDatasourceId(datasourceCombo.getValue().getId());
                }
            }
            case API -> form.setExecutorHandler("runApiHandler");
            default -> form.setExecutorHandler(executorHandlerField.getText().trim());
        }
        
        if (glueType.requiresGlueSource()) {
            String glueSource = glueEditorArea.getText();
            form.setGlueSource(glueSource != null && !glueSource.trim().isEmpty() ? glueSource : null);
            form.setGlueRemark(glueRemark != null && !glueRemark.trim().isEmpty() ? glueRemark.trim() : null);
        } else {
            form.setGlueSource(null);
            form.setGlueRemark(null);
        }
        
        form.setExecutorParam(executorParamArea.getText().trim());
        
        // 高级配置
        form.setExecutorRouteStrategy(routeStrategyCombo.getValue().getType());
        form.setMisfireStrategy(misfireStrategyCombo.getValue().getType());
        form.setExecutorBlockStrategy(blockStrategyCombo.getValue().getType());
        
        try {
            String timeoutText = executorTimeoutField.getText().trim();
            form.setExecutorTimeout(timeoutText.isEmpty() ? 0 : Integer.parseInt(timeoutText));
        } catch (NumberFormatException e) {
            form.setExecutorTimeout(0);
        }
        
        try {
            String retryText = executorFailRetryCountField.getText().trim();
            form.setExecutorFailRetryCount(retryText.isEmpty() ? 0 : Integer.parseInt(retryText));
        } catch (NumberFormatException e) {
            form.setExecutorFailRetryCount(0);
        }
        
        String childJobIdText = childJobIdField.getText();
        form.setChildJobId(childJobIdText != null && !childJobIdText.trim().isEmpty() ? childJobIdText.trim() : null);
        
        // 固定字段
        form.setJobType(0); // 普通任务
        form.setFailStrategy("JOB_FAIL"); // 满足后端校验要求
        
        return form;
    }
    
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
        if (scheduleTypeCombo.getValue() == null) {
            errors.append("• 请选择调度类型\n");
        }
        if (scheduleTypeCombo.getValue() != ScheduleType.NONE && scheduleConfField.getText().trim().isEmpty()) {
            errors.append("• 请输入调度配置\n");
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
        if (glueType != null && glueType.requiresGlueSource() && 
            (glueEditorArea.getText() == null || glueEditorArea.getText().trim().isEmpty())) {
            errors.append("• 请填写GLUE脚本内容\n");
        }
        if (routeStrategyCombo.getValue() == null) {
            errors.append("• 请选择路由策略\n");
        }
        if (misfireStrategyCombo.getValue() == null) {
            errors.append("• 请选择调度过期策略\n");
        }
        if (blockStrategyCombo.getValue() == null) {
            errors.append("• 请选择阻塞处理策略\n");
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
    
    private void styleDialog() {
        getDialogPane().setPrefWidth(850);
        getDialogPane().setPrefHeight(550);
        getDialogPane().setMinWidth(800);
        getDialogPane().setMinHeight(550);
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
                "-fx-padding: 8 20; " +
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
                "-fx-padding: 8 20; " +
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
                stage.setMinHeight(550);
            }
        });
    }
    
    // 枚举类型定义
    public enum ScheduleType {
        CRON("CRON", "CRON"),
        FIX_RATE("FIX_RATE", "固定速度"),
        NONE("NONE", "无");
        
        private final String type;
        private final String title;
        
        ScheduleType(String type, String title) {
            this.type = type;
            this.title = title;
        }
        
        public String getType() { return type; }
        @Override
        public String toString() { return title; }
    }
    
    public enum GlueType {
        BEAN("BEAN", "BEAN", false),
        API("API", "API", false),
        SQL("SQL", "SQL", false),
        GLUE_GROOVY("GLUE_GROOVY", "GLUE(Java)", true),
        GLUE_SHELL("GLUE_SHELL", "GLUE(Shell)", true),
        GLUE_PYTHON("GLUE_PYTHON", "GLUE(Python)", true),
        GLUE_PHP("GLUE_PHP", "GLUE(PHP)", true),
        GLUE_NODEJS("GLUE_NODEJS", "GLUE(Nodejs)", true),
        GLUE_POWERSHELL("GLUE_POWERSHELL", "GLUE(PowerShell)", true);
        
        private final String type;
        private final String title;
        private final boolean requiresGlueSource;
        
        GlueType(String type, String title, boolean requiresGlueSource) {
            this.type = type;
            this.title = title;
            this.requiresGlueSource = requiresGlueSource;
        }
        
        public String getType() { return type; }
        public boolean requiresGlueSource() { return requiresGlueSource; }
        
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
