package com.cc.job.gui.view;

import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * 新建/编辑任务组对话框
 */
public class NewJobGroupDialog extends Dialog<JobInfoForm> {
    
    private JobInfoForm formData;
    private Long jobPartId;
    
    // 表单字段
    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField authorField;
    private TextField alarmEmailField;
    
    private ComboBox<ScheduleType> scheduleTypeCombo;
    private TextField scheduleConfField;
    
    private ComboBox<RouteStrategy> routeStrategyCombo;
    private ComboBox<MisfireStrategy> misfireStrategyCombo;
    private ComboBox<BlockStrategy> blockStrategyCombo;
    private TextField executorTimeoutField;
    private TextField executorFailRetryCountField;
    private TextField childJobidField;
    
    private ButtonType saveButtonType;
    private ButtonType cancelButtonType;
    
    public NewJobGroupDialog(Stage owner, Long jobPartId, JobInfoForm editData, List<JobGroup> jobGroupList) {
        this.jobPartId = jobPartId;
        this.formData = editData != null ? editData : new JobInfoForm();
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(editData == null ? "新建任务组" : "编辑任务组");
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
        
        // 设置验证
        setupValidation();
    }
    
    /**
     * 创建对话框内容
     */
    private VBox createContent(List<JobGroup> jobGroupList) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(800);
        container.setPrefHeight(600);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(10));
        
        // 基础配置
        formContent.getChildren().add(createBasicSection(jobGroupList));
        
        // 调度配置
        formContent.getChildren().add(createScheduleSection());
        
        // 高级配置
        formContent.getChildren().add(createAdvancedSection());
        
        scrollPane.setContent(formContent);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        container.getChildren().add(scrollPane);
        
        return container;
    }
    
    /**
     * 创建基础配置部分
     */
    private VBox createBasicSection(List<JobGroup> jobGroupList) {
        VBox section = createSection("⚙️ 基础配置");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 执行器
        Label jobGroupLabel = createFormLabel("执行器", true);
        jobGroupCombo = new ComboBox<>();
        jobGroupCombo.setPrefWidth(350);
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
        
        // 任务描述
        Label jobDescLabel = createFormLabel("任务描述", true);
        jobDescField = new TextField();
        jobDescField.setPrefWidth(350);
        jobDescField.setPromptText("请输入任务描述");
        
        // 负责人
        Label authorLabel = createFormLabel("负责人", true);
        authorField = new TextField();
        authorField.setPrefWidth(350);
        authorField.setPromptText("请输入负责人");
        
        // 报警邮件
        Label alarmEmailLabel = createFormLabel("报警邮件", false);
        alarmEmailField = new TextField();
        alarmEmailField.setPrefWidth(350);
        alarmEmailField.setPromptText("请输入报警邮件地址");
        
        grid.add(jobGroupLabel, 0, 0);
        grid.add(jobGroupCombo, 1, 0);
        grid.add(authorLabel, 2, 0);
        grid.add(authorField, 3, 0);
        
        grid.add(jobDescLabel, 0, 1);
        grid.add(jobDescField, 1, 1);
        grid.add(alarmEmailLabel, 2, 1);
        grid.add(alarmEmailField, 3, 1);
        
        section.getChildren().add(grid);
        return section;
    }
    
    /**
     * 创建调度配置部分
     */
    private VBox createScheduleSection() {
        VBox section = createSection("🕐 调度配置");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 调度类型
        Label scheduleTypeLabel = createFormLabel("调度类型", true);
        scheduleTypeCombo = new ComboBox<>();
        scheduleTypeCombo.setPrefWidth(350);
        scheduleTypeCombo.setPromptText("请选择调度类型");
        scheduleTypeCombo.getItems().addAll(ScheduleType.values());
        scheduleTypeCombo.setValue(ScheduleType.CRON);
        
        // CRON表达式
        Label scheduleConfLabel = createFormLabel("CRON表达式", true);
        scheduleConfField = new TextField();
        scheduleConfField.setPrefWidth(350);
        scheduleConfField.setPromptText("请输入cron表达式");
        scheduleConfField.setText("0 0 0 * * ?"); // 默认值
        
        grid.add(scheduleTypeLabel, 0, 0);
        grid.add(scheduleTypeCombo, 1, 0);
        grid.add(scheduleConfLabel, 2, 0);
        grid.add(scheduleConfField, 3, 0);
        
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
        
        // 路由策略
        Label routeStrategyLabel = createFormLabel("路由策略", true);
        routeStrategyCombo = new ComboBox<>();
        routeStrategyCombo.setPrefWidth(350);
        routeStrategyCombo.setPromptText("请选择路由策略");
        routeStrategyCombo.getItems().addAll(RouteStrategy.values());
        routeStrategyCombo.setValue(RouteStrategy.FIRST);
        
        // 子任务ID
        Label childJobidLabel = createFormLabel("子任务ID", false);
        childJobidField = new TextField();
        childJobidField.setPrefWidth(350);
        childJobidField.setPromptText("请输入子任务ID");
        
        // 调度过期策略
        Label misfireStrategyLabel = createFormLabel("调度过期策略", true);
        misfireStrategyCombo = new ComboBox<>();
        misfireStrategyCombo.setPrefWidth(350);
        misfireStrategyCombo.setPromptText("请选择过期策略");
        misfireStrategyCombo.getItems().addAll(MisfireStrategy.values());
        misfireStrategyCombo.setValue(MisfireStrategy.DO_NOTHING);
        
        // 阻塞处理策略
        Label blockStrategyLabel = createFormLabel("阻塞处理策略", true);
        blockStrategyCombo = new ComboBox<>();
        blockStrategyCombo.setPrefWidth(350);
        blockStrategyCombo.setPromptText("请选择阻塞策略");
        blockStrategyCombo.getItems().addAll(BlockStrategy.values());
        blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
        
        // 任务超时时间
        Label timeoutLabel = createFormLabel("任务超时时间(秒)", false);
        executorTimeoutField = new TextField();
        executorTimeoutField.setPrefWidth(350);
        executorTimeoutField.setPromptText("请输入超时时间（秒）");
        executorTimeoutField.setText("300"); // 默认300秒
        
        // 失败重试次数
        Label retryLabel = createFormLabel("失败重试次数", false);
        executorFailRetryCountField = new TextField();
        executorFailRetryCountField.setPrefWidth(350);
        executorFailRetryCountField.setPromptText("请输入重试次数");
        executorFailRetryCountField.setText("0"); // 默认0次
        
        grid.add(routeStrategyLabel, 0, 0);
        grid.add(routeStrategyCombo, 1, 0);
        grid.add(childJobidLabel, 2, 0);
        grid.add(childJobidField, 3, 0);
        
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
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1F2937;"
        );
        
        section.getChildren().add(titleLabel);
        return section;
    }
    
    /**
     * 创建表单标签
     */
    private Label createFormLabel(String text, boolean required) {
        String displayText = required ? text + " *" : text;
        Label label = new Label(displayText);
        label.setStyle("-fx-text-fill: #374151; -fx-font-size: 13;");
        label.setMinWidth(140);
        label.setPrefWidth(140);
        label.setMaxWidth(140);
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setTooltip(new Tooltip(displayText));
        return label;
    }
    
    /**
     * 填充表单数据（编辑模式）
     */
    private void fillFormData(JobInfoForm data, List<JobGroup> jobGroupList) {
        // 填充基础配置
        if (data.getJobGroup() != null) {
            jobGroupList.stream()
                .filter(jg -> jg.getId().equals(data.getJobGroup()))
                .findFirst()
                .ifPresent(jobGroupCombo::setValue);
        }
        if (data.getJobDesc() != null) jobDescField.setText(data.getJobDesc());
        if (data.getAuthor() != null) authorField.setText(data.getAuthor());
        if (data.getAlarmEmail() != null) alarmEmailField.setText(data.getAlarmEmail());
        
        // 填充调度配置
        if (data.getScheduleType() != null) {
            try {
                scheduleTypeCombo.setValue(ScheduleType.valueOf(data.getScheduleType()));
            } catch (Exception e) {
                scheduleTypeCombo.setValue(ScheduleType.CRON);
            }
        }
        if (data.getScheduleConf() != null) scheduleConfField.setText(data.getScheduleConf());
        
        // 填充高级配置
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
        if (data.getChildJobid() != null) childJobidField.setText(data.getChildJobid());
    }
    
    /**
     * 收集表单数据
     */
    private JobInfoForm collectFormData() {
        JobInfoForm form = new JobInfoForm();
        
        // 设置ID（编辑模式）
        if (formData.getId() != null) {
            form.setId(formData.getId());
        }
        
        // 基础配置
        form.setJobPartId(jobPartId.intValue());
        form.setJobGroup(jobGroupCombo.getValue() != null ? jobGroupCombo.getValue().getId() : null);
        form.setJobDesc(jobDescField.getText().trim());
        form.setAuthor(authorField.getText().trim());
        form.setAlarmEmail(alarmEmailField.getText().trim());
        
        // 调度配置
        form.setScheduleType(scheduleTypeCombo.getValue().getType());
        form.setScheduleConf(scheduleConfField.getText().trim());
        
        // 高级配置
        form.setExecutorRouteStrategy(routeStrategyCombo.getValue().getType());
        form.setMisfireStrategy(misfireStrategyCombo.getValue().getType());
        form.setExecutorBlockStrategy(blockStrategyCombo.getValue().getType());
        
        try {
            form.setExecutorTimeout(Integer.parseInt(executorTimeoutField.getText().trim()));
        } catch (NumberFormatException e) {
            form.setExecutorTimeout(300);
        }
        
        try {
            form.setExecutorFailRetryCount(Integer.parseInt(executorFailRetryCountField.getText().trim()));
        } catch (NumberFormatException e) {
            form.setExecutorFailRetryCount(0);
        }
        
        form.setChildJobid(childJobidField.getText().trim());
        
        // 固定字段
        form.setGlueType("BEAN");
        form.setExecutorHandler("runJobGroupXxlJob"); // 任务组的固定Handler
        form.setJobType(1); // 任务组类型
        
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
        if (scheduleTypeCombo.getValue() == null) {
            errors.append("• 请选择调度类型\n");
        }
        if (scheduleConfField.getText().trim().isEmpty()) {
            errors.append("• 请输入CRON表达式\n");
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
    
    /**
     * 设置验证
     */
    private void setupValidation() {
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        if (saveButton != null) {
            // 可以添加实时验证逻辑
        }
    }
    
    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        getDialogPane().setPrefWidth(850);
        getDialogPane().setPrefHeight(650);
        getDialogPane().setMinWidth(780);
        getDialogPane().setMinHeight(560);
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
                "-fx-background-color: #10B981; " +
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
        
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(780);
                stage.setMinHeight(560);
            }
        });
    }
    
    // 枚举类型定义
    public enum ScheduleType {
        CRON("CRON", "CRON"),
        NONE("NONE", "无"),
        FIX_RATE("FIX_RATE", "固定速度");
        
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
    
    public enum RouteStrategy {
        FIRST("FIRST", "第一个"),
        LAST("LAST", "最后一个"),
        ROUND("ROUND", "轮询"),
        RANDOM("RANDOM", "随机"),
        CONSISTENT_HASH("CONSISTENT_HASH", "一致性哈希"),
        LEASTY_FREQUENTY_USED("LEASTY_FREQUENTY_USED", "最不经常使用"),
        LEASTY_RECENTLY_USED("LEASTY_RECENTLY_USED", "最近最久未使用"),
        FAILOVER("FAILOVER", "故障转移"),
        BUSYOVER("BUSYOVER", "忙碌转移"),
        SHARDING_BORADCAST("SHARDING_BORADCAST", "分片广播");
        
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

