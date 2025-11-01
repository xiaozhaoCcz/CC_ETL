package com.cc.job.gui.view;

import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * 新建/编辑任务节点对话框
 */
public class NewJobNodeDialog extends Dialog<JobInfoForm> {
    
    private JobInfoForm formData;
    private Long parentJobId; // 父任务组ID
    
    // 表单字段
    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField authorField;
    private TextField alarmEmailField;
    
    private ComboBox<GlueType> glueTypeCombo;
    private TextField executorHandlerField;
    private TextArea executorParamArea;
    
    private TextField executorTimeoutField;
    private ComboBox<BlockStrategy> blockStrategyCombo;
    private Spinner<Integer> executorFailRetryCountSpinner;
    
    private ButtonType saveButtonType;
    private ButtonType cancelButtonType;
    
    public NewJobNodeDialog(Stage owner, Long parentJobId, JobInfoForm editData, List<JobGroup> jobGroupList) {
        this.parentJobId = parentJobId;
        this.formData = editData != null ? editData : new JobInfoForm();
        
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
        }
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                if (validateForm()) {
                    return collectFormData();
                }
            }
            return null;
        });
        
        // 监听GlueType变化
        glueTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFieldsForGlueType(newVal);
        });
        
        // 初始化字段可见性
        if (glueTypeCombo.getValue() != null) {
            updateFieldsForGlueType(glueTypeCombo.getValue());
        }
    }
    
    /**
     * 创建对话框内容
     */
    private VBox createContent(List<JobGroup> jobGroupList) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(700);
        container.setPrefHeight(500);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(10));
        
        // 基本信息
        formContent.getChildren().add(createBasicSection(jobGroupList));
        
        // 执行配置
        formContent.getChildren().add(createExecutionSection());
        
        // 高级配置
        formContent.getChildren().add(createAdvancedSection());
        
        scrollPane.setContent(formContent);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        container.getChildren().add(scrollPane);
        
        return container;
    }
    
    /**
     * 创建基本信息部分
     */
    private VBox createBasicSection(List<JobGroup> jobGroupList) {
        VBox section = createSection("📄 基本信息");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 执行器
        Label jobGroupLabel = createRequiredLabel("执行器");
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
        Label authorLabel = createRequiredLabel("负责人");
        authorField = new TextField();
        authorField.setPrefWidth(300);
        authorField.setPromptText("请输入负责人姓名");
        
        // 任务描述
        Label jobDescLabel = createRequiredLabel("任务描述");
        jobDescField = new TextField();
        jobDescField.setPrefWidth(615);
        jobDescField.setPromptText("请输入任务描述");
        
        // 报警邮件
        Label alarmEmailLabel = new Label("报警邮件");
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
        VBox section = createSection("⚙️ 执行配置");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 运行模式
        Label glueTypeLabel = createRequiredLabel("运行模式");
        glueTypeCombo = new ComboBox<>();
        glueTypeCombo.setPrefWidth(300);
        glueTypeCombo.setPromptText("请选择运行模式");
        glueTypeCombo.getItems().addAll(GlueType.values());
        glueTypeCombo.setValue(GlueType.BEAN);
        
        // JobHandler
        Label executorHandlerLabel = createRequiredLabel("JobHandler");
        executorHandlerField = new TextField();
        executorHandlerField.setPrefWidth(300);
        executorHandlerField.setPromptText("请输入JobHandler名称");
        
        // 任务参数
        Label executorParamLabel = new Label("任务参数");
        executorParamArea = new TextArea();
        executorParamArea.setPrefWidth(615);
        executorParamArea.setPrefRowCount(4);
        executorParamArea.setPromptText("请输入任务参数");
        executorParamArea.setWrapText(true);
        
        grid.add(glueTypeLabel, 0, 0);
        grid.add(glueTypeCombo, 1, 0);
        grid.add(executorHandlerLabel, 2, 0);
        grid.add(executorHandlerField, 3, 0);
        
        grid.add(executorParamLabel, 0, 1);
        grid.add(executorParamArea, 1, 1, 3, 1);
        
        section.getChildren().add(grid);
        return section;
    }
    
    /**
     * 创建高级配置部分
     */
    private VBox createAdvancedSection() {
        VBox section = createSection("🔧 高级配置");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 任务超时时间
        Label timeoutLabel = new Label("任务超时时间(秒)");
        executorTimeoutField = new TextField();
        executorTimeoutField.setPrefWidth(300);
        executorTimeoutField.setPromptText("单位：秒");
        executorTimeoutField.setText("300");
        
        // 任务失败策略
        Label blockStrategyLabel = createRequiredLabel("任务失败策略");
        blockStrategyCombo = new ComboBox<>();
        blockStrategyCombo.setPrefWidth(300);
        blockStrategyCombo.setPromptText("请选择失败策略");
        blockStrategyCombo.getItems().addAll(BlockStrategy.values());
        blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
        
        // 任务重试次数
        Label retryLabel = new Label("任务重试次数");
        executorFailRetryCountSpinner = new Spinner<>(0, 10, 0);
        executorFailRetryCountSpinner.setPrefWidth(300);
        executorFailRetryCountSpinner.setEditable(true);
        
        grid.add(timeoutLabel, 0, 0);
        grid.add(executorTimeoutField, 1, 0);
        grid.add(blockStrategyLabel, 2, 0);
        grid.add(blockStrategyCombo, 3, 0);
        
        grid.add(retryLabel, 0, 1);
        grid.add(executorFailRetryCountSpinner, 1, 1);
        
        section.getChildren().add(grid);
        return section;
    }
    
    /**
     * 根据GlueType更新字段可见性
     */
    private void updateFieldsForGlueType(GlueType glueType) {
        if (glueType == GlueType.BEAN) {
            executorHandlerField.setDisable(false);
            executorHandlerField.setPromptText("请输入JobHandler名称");
        } else if (glueType == GlueType.SQL) {
            executorHandlerField.setDisable(true);
            executorHandlerField.setText("runJobJdbcXxlJob");
            executorParamArea.setPromptText("请输入SQL语句");
        } else if (glueType == GlueType.API) {
            executorHandlerField.setDisable(true);
            executorHandlerField.setText("runApiHandler");
            executorParamArea.setPromptText("请输入API请求参数");
        } else {
            executorHandlerField.setDisable(true);
            executorHandlerField.clear();
        }
    }
    
    /**
     * 创建section容器
     */
    private VBox createSection(String title) {
        VBox section = new VBox(10);
        section.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10;"
        );
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 14; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1F2937;"
        );
        
        section.getChildren().add(titleLabel);
        return section;
    }
    
    /**
     * 创建必填标签
     */
    private Label createRequiredLabel(String text) {
        Label label = new Label(text + " *");
        label.setStyle("-fx-text-fill: #374151; -fx-font-size: 13;");
        label.setPrefWidth(120);
        return label;
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
        
        if (data.getGlueType() != null) {
            try {
                glueTypeCombo.setValue(GlueType.valueOf(data.getGlueType()));
            } catch (Exception e) {
                glueTypeCombo.setValue(GlueType.BEAN);
            }
        }
        if (data.getExecutorHandler() != null) executorHandlerField.setText(data.getExecutorHandler());
        if (data.getExecutorParam() != null) executorParamArea.setText(data.getExecutorParam());
        
        if (data.getExecutorBlockStrategy() != null) {
            try {
                blockStrategyCombo.setValue(BlockStrategy.valueOf(data.getExecutorBlockStrategy()));
            } catch (Exception e) {
                blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
            }
        }
        if (data.getExecutorTimeout() != null) executorTimeoutField.setText(String.valueOf(data.getExecutorTimeout()));
        if (data.getExecutorFailRetryCount() != null) executorFailRetryCountSpinner.getValueFactory().setValue(data.getExecutorFailRetryCount());
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
        
        // 根据GlueType设置ExecutorHandler
        if (glueType == GlueType.SQL) {
            form.setExecutorHandler("runJobJdbcXxlJob");
        } else if (glueType == GlueType.API) {
            form.setExecutorHandler("runApiHandler");
        } else {
            form.setExecutorHandler(executorHandlerField.getText().trim());
        }
        
        form.setExecutorParam(executorParamArea.getText().trim());
        
        // 高级配置
        form.setExecutorBlockStrategy(blockStrategyCombo.getValue().getType());
        
        try {
            form.setExecutorTimeout(Integer.parseInt(executorTimeoutField.getText().trim()));
        } catch (NumberFormatException e) {
            form.setExecutorTimeout(300);
        }
        
        form.setExecutorFailRetryCount(executorFailRetryCountSpinner.getValue());
        
        // 固定字段
        form.setMisfireStrategy("DO_NOTHING");
        form.setScheduleType("NONE");
        form.setJobType(0); // 普通任务节点
        form.setExecutorRouteStrategy("FIRST");

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
        if (glueTypeCombo.getValue() == GlueType.BEAN && executorHandlerField.getText().trim().isEmpty()) {
            errors.append("• 请输入JobHandler\n");
        }
        if (blockStrategyCombo.getValue() == null) {
            errors.append("• 请选择任务失败策略\n");
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
        getDialogPane().setPrefHeight(550);
        
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
                "-fx-text-fill: #374151; " +
                "-fx-font-size: 13; " +
                "-fx-padding: 8 20 8 20; " +
                "-fx-border-color: #D1D5DB; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand;"
            );
        }
    }
    
    // 枚举类型定义
    public enum GlueType {
        BEAN("BEAN", "BEAN"),
        API("API", "API"),
        SQL("SQL", "SQL"),
        GLUE_GROOVY("GLUE_GROOVY", "GLUE(Java)"),
        GLUE_SHELL("GLUE_SHELL", "GLUE(Shell)"),
        GLUE_PYTHON("GLUE_PYTHON", "GLUE(Python)"),
        GLUE_PHP("GLUE_PHP", "GLUE(PHP)"),
        GLUE_NODEJS("GLUE_NODEJS", "GLUE(Nodejs)"),
        GLUE_POWERSHELL("GLUE_POWERSHELL", "GLUE(PowerShell)");
        
        private final String type;
        private final String title;
        
        GlueType(String type, String title) {
            this.type = type;
            this.title = title;
        }
        
        public String getType() { return type; }
        @Override
        public String toString() { return title; }
    }
    
    public enum BlockStrategy {
        SERIAL_EXECUTION("SERIAL_EXECUTION", "单机串行"),
        DO_NOTHING("DO_NOTHING", "忽略");
        
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

