package com.cc.job.gui.view;

import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private Label executorParamLabel;
    private TextArea executorParamArea;
    private VBox apiSettingContainer;
    private ComboBox<String> reqTypeCombo;
    private TextField reqUrlField;
    private ParameterTable headerTable;
    private ParameterTable bodyTable;
    
    private TextField executorTimeoutField;
    private ComboBox<BlockStrategy> blockStrategyCombo;
    private Spinner<Integer> executorFailRetryCountSpinner;
    private VBox glueSettingContainer;
    private TextArea glueEditorArea;
    
    private ButtonType saveButtonType;
    private ButtonType cancelButtonType;
    
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>(){}.getType();

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
        formContent.getChildren().add(createGlueSection());
        formContent.getChildren().add(createApiSection());
        
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
        VBox section = createSection("⚙️ 执行配置");
        
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
        
        // JobHandler
        Label executorHandlerLabel = createFormLabel("JobHandler", true);
        executorHandlerField = new TextField();
        executorHandlerField.setPrefWidth(300);
        executorHandlerField.setPromptText("请输入JobHandler名称");
        
        // 任务参数
        executorParamLabel = createFormLabel("任务参数", false);
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
        Label timeoutLabel = createFormLabel("任务超时时间(秒)", false);
        executorTimeoutField = new TextField();
        executorTimeoutField.setPrefWidth(300);
        executorTimeoutField.setPromptText("单位：秒");
        executorTimeoutField.setText("300");
        
        // 任务失败策略
        Label blockStrategyLabel = createFormLabel("任务失败策略", true);
        blockStrategyCombo = new ComboBox<>();
        blockStrategyCombo.setPrefWidth(300);
        blockStrategyCombo.setPromptText("请选择失败策略");
        blockStrategyCombo.getItems().addAll(BlockStrategy.values());
        blockStrategyCombo.setValue(BlockStrategy.SERIAL_EXECUTION);
        
        // 任务重试次数
        Label retryLabel = createFormLabel("任务重试次数", false);
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

    private VBox createGlueSection() {
        glueSettingContainer = createSection("🧩 GLUE 脚本");
        glueSettingContainer.setVisible(false);

        glueEditorArea = new TextArea();
        glueEditorArea.setPrefRowCount(12);
        glueEditorArea.setWrapText(true);
        glueEditorArea.setPromptText("请输入 GLUE 脚本内容");

        glueSettingContainer.getChildren().add(glueEditorArea);
        return glueSettingContainer;
    }

    private VBox createApiSection() {
        apiSettingContainer = createSection("🌐 API 配置");
        apiSettingContainer.setVisible(false);

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

        grid.add(reqTypeLabel, 0, 0);
        grid.add(reqTypeCombo, 1, 0);
        grid.add(reqUrlLabel, 0, 1);
        grid.add(reqUrlField, 1, 1, 3, 1);

        headerTable = new ParameterTable("请求头");
        bodyTable = new ParameterTable("请求体参数");

        apiSettingContainer.getChildren().addAll(grid, headerTable, bodyTable);
        return apiSettingContainer;
    }

    private void toggleExecutorParamArea(boolean visible) {
        executorParamLabel.setVisible(visible);
        executorParamLabel.setManaged(visible);
        executorParamArea.setVisible(visible);
        executorParamArea.setManaged(visible);
    }

    /**
     * 根据GlueType更新字段可见性
     */
    private void updateFieldsForGlueType(GlueType glueType) {
        switch (glueType) {
            case BEAN -> {
                executorHandlerField.setDisable(false);
                executorHandlerField.setPromptText("请输入JobHandler名称");
                executorParamArea.setPromptText("请输入任务参数");
                toggleExecutorParamArea(true);
            }
            case SQL -> {
                executorHandlerField.setDisable(true);
                executorHandlerField.setText("runJobJdbcXxlJob");
                executorParamArea.setPromptText("请输入SQL语句");
                toggleExecutorParamArea(true);
            }
            case API -> {
                executorHandlerField.setDisable(true);
                executorHandlerField.setText("runApiHandler");
                executorParamArea.clear();
                toggleExecutorParamArea(false);
                headerTable.ensureAtLeastOneRow();
                bodyTable.ensureAtLeastOneRow();
            }
            default -> {
                executorHandlerField.setDisable(true);
                executorHandlerField.clear();
                executorParamArea.setPromptText("请输入任务参数");
                toggleExecutorParamArea(true);
            }
        }
        glueSettingContainer.setVisible(glueType.requiresGlueSource());
        glueSettingContainer.setManaged(glueType.requiresGlueSource());
        apiSettingContainer.setVisible(glueType == GlueType.API);
        apiSettingContainer.setManaged(glueType == GlueType.API);
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
        updateFieldsForGlueType(glueType);

        if (data.getExecutorHandler() != null) {
            executorHandlerField.setText(data.getExecutorHandler());
        }

        if (glueType.requiresGlueSource() && data.getGlueSource() != null) {
            glueEditorArea.setText(data.getGlueSource());
        }

        if (glueType == GlueType.API) {
            if (data.getReqType() != null) {
                reqTypeCombo.setValue(data.getReqType());
            }
            if (data.getReqUrl() != null) {
                reqUrlField.setText(data.getReqUrl());
            }
            headerTable.setData(data.getReqHeader());
            bodyTable.setData(data.getExecutorParam());
        } else {
            if (data.getExecutorParam() != null) {
                executorParamArea.setText(data.getExecutorParam());
            }
            headerTable.setData(null);
            bodyTable.setData(null);
        }

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
        
        switch (glueType) {
            case SQL -> form.setExecutorHandler("runJobJdbcXxlJob");
            case API -> form.setExecutorHandler("runApiHandler");
            default -> form.setExecutorHandler(executorHandlerField.getText().trim());
        }

        if (glueType.requiresGlueSource()) {
            form.setGlueSource(glueEditorArea.getText());
        } else {
            form.setGlueSource(null);
        }

        if (glueType == GlueType.API) {
            String reqType = reqTypeCombo.getValue();
            form.setReqType(reqType != null ? reqType : "GET");
            String reqUrl = reqUrlField.getText() != null ? reqUrlField.getText().trim() : "";
            form.setReqUrl(reqUrl);
            form.setReqHeader(headerTable.toJson());
            form.setExecutorParam(bodyTable.toJson());
        } else {
            form.setReqType(null);
            form.setReqUrl(null);
            form.setReqHeader(null);
            form.setExecutorParam(executorParamArea.getText().trim());
        }
        
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
        GlueType glueType = glueTypeCombo.getValue();
        if (glueType == GlueType.BEAN && executorHandlerField.getText().trim().isEmpty()) {
            errors.append("• 请输入JobHandler\n");
        }
        if (glueType != null && glueType.requiresGlueSource() && (glueEditorArea.getText() == null || glueEditorArea.getText().trim().isEmpty())) {
            errors.append("• 请填写GLUE脚本内容\n");
        }
        if (glueType == GlueType.API) {
            if (reqUrlField.getText() == null || reqUrlField.getText().trim().isEmpty()) {
                errors.append("• 请填写API请求地址\n");
            }
            if (headerTable.isEmpty() && bodyTable.isEmpty()) {
                errors.append("• 请至少配置一个请求头或请求参数\n");
            }
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
        getDialogPane().setMinWidth(720);
        getDialogPane().setMinHeight(520);
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
                stage.setMinWidth(720);
                stage.setMinHeight(520);
            }
        });
    }

    private static class ParameterTable extends VBox {
        private final TableView<ParamItem> tableView;
        private final ObservableList<ParamItem> items = FXCollections.observableArrayList();

        ParameterTable(String title) {
            setSpacing(8);
            setPadding(new Insets(12));
            setStyle("-fx-background-color: linear-gradient(145deg,#6847FF,#8A6BFF); -fx-border-radius: 10; -fx-background-radius: 10;");

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13; -fx-font-weight: bold;");

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
                    deleteButton.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 4 10; -fx-background-radius: 4; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 4; -fx-cursor: hand;");
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
            addButton.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 14; -fx-background-radius: 4; -fx-border-color: rgba(255,255,255,0.4); -fx-border-radius: 4; -fx-cursor: hand;");
            addButton.setOnAction(e -> addRow("", ""));

            getChildren().addAll(titleLabel, tableView, addButton);
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
        GLUE_GROOVY("GLUE_GROOVY", "GLUE(Java)", true, false),
        GLUE_SHELL("GLUE_SHELL", "GLUE(Shell)", true, false),
        GLUE_PYTHON("GLUE_PYTHON", "GLUE(Python)", true, false),
        GLUE_PHP("GLUE_PHP", "GLUE(PHP)", true, false),
        GLUE_NODEJS("GLUE_NODEJS", "GLUE(Nodejs)", true, false),
        GLUE_POWERSHELL("GLUE_POWERSHELL", "GLUE(PowerShell)", true, false);
        
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

