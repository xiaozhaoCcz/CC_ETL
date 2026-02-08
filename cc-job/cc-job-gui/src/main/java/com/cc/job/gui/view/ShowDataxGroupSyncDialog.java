package com.cc.job.gui.view;

import com.cc.job.gui.service.JobDataxService;
import com.cc.job.gui.service.JobGroupService;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.service.JobJdbcDatasourceService;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.model.datax.DataxTable;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.form.JobInfoForm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 多数据源同步对话框 - 支持多表批量同步
 */
public class ShowDataxGroupSyncDialog extends Dialog<Void> {

    private final JobJdbcDatasourceService datasourceService = new JobJdbcDatasourceService();
    private final JobDataxService dataxService = new JobDataxService();
    private final JobGroupService jobGroupService = new JobGroupService();
    private final JobInfoService jobInfoService = new JobInfoService();
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int currentStep = 0;
    private StackPane contentPane;
    private Button prevBtn, nextBtn;
    private Label step1Label, step2Label, step3Label;

    // Step 1 - Reader配置（多表）
    private ComboBox<String> readerDsTypeCombo;
    private ComboBox<JobJdbcDatasource> readerDatasourceCombo;
    private ListView<String> readerTableList;
    private ComboBox<String> incrTypeCombo;
    private VBox incrConfigBox;
    private ComboBox<String> incrModeCombo;
    private TextField incrColumnField;
    private TextField incrInitValueField;
    private ComboBox<String> incrTimeFormatCombo;

    // Step 2 - Writer配置（多表）
    private ComboBox<String> writerDsTypeCombo;
    private ComboBox<JobJdbcDatasource> writerDatasourceCombo;
    private ListView<String> writerTableList;
    private ComboBox<String> writeModeCombo;

    // Step 3 - 结果
    private TextArea jsonResultArea;

    // Step 3 - 基本信息与调度（与单数据源一致）
    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField authorField;
    private TextField alarmEmailField;
    private ComboBox<NewJobDialog.ScheduleType> scheduleTypeCombo;
    private TextField scheduleConfField;
    private Label scheduleConfLabel;
    private ComboBox<NewJobDialog.RouteStrategy> routeStrategyCombo;
    private ComboBox<NewJobDialog.MisfireStrategy> misfireStrategyCombo;
    private ComboBox<NewJobDialog.BlockStrategy> blockStrategyCombo;
    private TextField childJobIdField;
    private TextField executorTimeoutField;
    private TextField executorFailRetryCountField;
    private VBox advancedSection;
    private boolean advancedSectionVisible = false;

    private List<JobJdbcDatasource> allDatasources = new ArrayList<>();

    private static final String[] DS_TYPES = {"MYSQL", "ORACLE", "POSTGRESQL"};
    private static final String[] WRITE_MODES = {"insert", "update", "replace"};
    private static final String[] INCR_MODES = {"ID自增", "时间自增"};
    private static final String[] TIME_FORMATS = {"YYYY-MM-DD HH:mm:ss", "YYYY-MM-DD", "YYYY/MM/DD HH:mm:ss", "YYYY/MM/DD"};

    public ShowDataxGroupSyncDialog(Stage ownerStage) {
        setTitle("多数据源同步");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

        root.getChildren().addAll(createStepIndicator(), createContentPane(), createButtonBar());

        getDialogPane().setContent(root);

        loadDatasources();
        updateStepView();
    }

    private void styleDialog() {
        getDialogPane().setPrefSize(1000, 700);
        getDialogPane().setMinWidth(900);
        getDialogPane().setMinHeight(650);
        setResizable(true);
        String dialogCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            getDialogPane().getStylesheets().add(dialogCss);
        }
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                if (css != null && !css.isEmpty()) {
                    stage.getScene().getStylesheets().add(css);
                }
            }
        });
    }

    private HBox createStepIndicator() {
        HBox steps = new HBox(40);
        steps.setAlignment(Pos.CENTER);
        steps.setPadding(new Insets(16));
        steps.getStyleClass().add("dialog-section");
        steps.setStyle("-fx-background-radius: 8;");

        step1Label = createStepLabel("1. 读取表配置", true);
        step2Label = createStepLabel("2. 写入表配置", false);
        step3Label = createStepLabel("3. 生成JSON", false);

        steps.getChildren().addAll(step1Label, createStepLine(), step2Label, createStepLine(), step3Label);
        return steps;
    }

    private Label createStepLabel(String text, boolean active) {
        Label label = new Label(text);
        updateStepLabelStyle(label, active);
        return label;
    }

    private void updateStepLabelStyle(Label label, boolean active) {
        if (active) {
            label.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + StyleUtil.linkPrimaryColor() + ";");
        } else {
            label.setStyle("-fx-font-size: 14; -fx-text-fill: " + StyleUtil.textSecondaryColor() + ";");
        }
    }

    private Region createStepLine() {
        Region line = new Region();
        line.setPrefSize(60, 2);
        line.setStyle("-fx-background-color: " + StyleUtil.BORDER + ";");
        return line;
    }

    private StackPane createContentPane() {
        contentPane = new StackPane();
        contentPane.setPadding(new Insets(16));
        contentPane.getStyleClass().add("dialog-section");
        contentPane.setStyle("-fx-background-radius: 8;");
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        return contentPane;
    }

    private HBox createButtonBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(16));

        prevBtn = new Button("上一步");
        prevBtn.getStyleClass().add("dialog-button-secondary");
        prevBtn.setOnAction(e -> {
            if (currentStep > 0) {
                currentStep--;
                updateStepView();
            }
        });

        nextBtn = new Button("下一步");
        nextBtn.getStyleClass().add("dialog-button-primary");
        nextBtn.setOnAction(e -> handleNext());

        Button resetBtn = new Button("重置");
        resetBtn.getStyleClass().add("dialog-button-secondary");
        resetBtn.setOnAction(e -> handleReset());

        bar.getChildren().addAll(resetBtn, prevBtn, nextBtn);
        return bar;
    }

    private void updateStepView() {
        contentPane.getChildren().clear();
        updateStepLabelStyle(step1Label, currentStep == 0);
        updateStepLabelStyle(step2Label, currentStep == 1);
        updateStepLabelStyle(step3Label, currentStep == 2);

        prevBtn.setDisable(currentStep == 0);
        nextBtn.setText(currentStep == 2 ? "确认" : "下一步");

        switch (currentStep) {
            case 0 -> contentPane.getChildren().add(createReaderPane());
            case 1 -> contentPane.getChildren().add(createWriterPane());
            case 2 -> contentPane.getChildren().add(createResultPane());
        }
    }

    private Node createReaderPane() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(16));

        String labelStyle = StyleUtil.bodyFontOnly();

        Label title = new Label("选择读取数据源和表（支持多选）");
        title.setStyle(StyleUtil.bodyFontOnly() + "-fx-font-weight: bold; -fx-font-size: 14;");

        // 数据源类型
        Label dsTypeLabel = new Label("数据源类型");
        dsTypeLabel.setStyle(labelStyle);
        readerDsTypeCombo = new ComboBox<>(FXCollections.observableArrayList(DS_TYPES));
        readerDsTypeCombo.setPromptText("选择数据源类型");
        readerDsTypeCombo.setOnAction(e -> filterReaderDatasources());

        // 数据源
        Label dsLabel = new Label("数据源");
        dsLabel.setStyle(labelStyle);
        readerDatasourceCombo = new ComboBox<>();
        readerDatasourceCombo.setPromptText("选择数据源");
        readerDatasourceCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JobJdbcDatasource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDatasourceName() + ":" + item.getDatabaseName());
            }
        });
        readerDatasourceCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JobJdbcDatasource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDatasourceName() + ":" + item.getDatabaseName());
            }
        });
        readerDatasourceCombo.setOnAction(e -> loadReaderTables());

        // 表列表（多选）
        Label tableLabel = new Label("数据表（按住Ctrl多选）");
        tableLabel.setStyle(labelStyle);
        readerTableList = new ListView<>();
        readerTableList.setPrefHeight(250);
        readerTableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button selectAllBtn = new Button("全选");
        selectAllBtn.setOnAction(e -> readerTableList.getSelectionModel().selectAll());
        Button clearBtn = new Button("清空");
        clearBtn.setOnAction(e -> readerTableList.getSelectionModel().clearSelection());

        // 多数据源仅支持全量，不展示增量选项
        Label incrLabel = new Label("同步模式");
        incrLabel.setStyle(labelStyle);
        incrTypeCombo = new ComboBox<>(FXCollections.observableArrayList("全量"));
        incrTypeCombo.getSelectionModel().selectFirst();

        // 增量配置容器（多数据源不支持增量，始终隐藏）
        incrConfigBox = new VBox(8);
        incrConfigBox.setPadding(new Insets(12));
        incrConfigBox.getStyleClass().add("dialog-section");
        incrConfigBox.setStyle("-fx-background-radius: 8; -fx-border-width: 1; -fx-border-radius: 8;");
        incrConfigBox.setVisible(false);
        incrConfigBox.setManaged(false);

        Label incrModeLabel = new Label("增量模式");
        incrModeLabel.setStyle(labelStyle + " -fx-min-width: 80;");
        incrModeCombo = new ComboBox<>(FXCollections.observableArrayList(INCR_MODES));
        incrModeCombo.getSelectionModel().selectFirst();
        incrModeCombo.setOnAction(e -> updateIncrModeConfig());
        HBox incrModeRow = new HBox(12);
        incrModeRow.setAlignment(Pos.CENTER_LEFT);
        incrModeRow.getChildren().addAll(incrModeLabel, incrModeCombo);

        Label incrColumnLabel = new Label("增量字段");
        incrColumnLabel.setStyle(labelStyle + " -fx-min-width: 80;");
        incrColumnField = new TextField();
        incrColumnField.setPromptText("如：id 或 create_time");
        HBox incrColumnRow = new HBox(12);
        incrColumnRow.setAlignment(Pos.CENTER_LEFT);
        incrColumnRow.getChildren().addAll(incrColumnLabel, incrColumnField);
        HBox.setHgrow(incrColumnField, Priority.ALWAYS);

        Label incrValueLabel = new Label("初始值");
        incrValueLabel.setStyle(labelStyle + " -fx-min-width: 80;");
        incrInitValueField = new TextField();
        incrInitValueField.setPromptText("ID自增填数字，时间自增填时间戳(毫秒)");
        HBox incrValueRow = new HBox(12);
        incrValueRow.setAlignment(Pos.CENTER_LEFT);
        incrValueRow.getChildren().addAll(incrValueLabel, incrInitValueField);
        HBox.setHgrow(incrInitValueField, Priority.ALWAYS);

        Label incrTimeFormatLabel = new Label("时间格式");
        incrTimeFormatLabel.setStyle(labelStyle + " -fx-min-width: 80;");
        incrTimeFormatCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_FORMATS));
        incrTimeFormatCombo.getSelectionModel().selectFirst();
        HBox incrTimeFormatRow = new HBox(12);
        incrTimeFormatRow.setAlignment(Pos.CENTER_LEFT);
        incrTimeFormatRow.getChildren().addAll(incrTimeFormatLabel, incrTimeFormatCombo);
        incrTimeFormatRow.setVisible(false);

        incrConfigBox.getChildren().addAll(incrModeRow, incrColumnRow, incrValueRow, incrTimeFormatRow);
        VBox incrContainer = new VBox(8, incrTypeCombo, incrConfigBox);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.add(dsTypeLabel, 0, 0);
        grid.add(readerDsTypeCombo, 1, 0);
        grid.add(dsLabel, 0, 1);
        grid.add(readerDatasourceCombo, 1, 1);
        grid.add(tableLabel, 0, 2);
        VBox tableBox = new VBox(8, readerTableList, new HBox(8, selectAllBtn, clearBtn));
        grid.add(tableBox, 1, 2);
        grid.add(incrLabel, 0, 3);
        grid.add(incrContainer, 1, 3);

        pane.getChildren().addAll(title, grid);
        return new ScrollPane(pane);
    }

    private Node createWriterPane() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(16));

        String labelStyle = StyleUtil.bodyFontOnly();

        Label title = new Label("选择写入数据源和表（支持多选）");
        title.setStyle(StyleUtil.bodyFontOnly() + "-fx-font-weight: bold; -fx-font-size: 14;");

        // 数据源类型
        Label dsTypeLabel = new Label("数据源类型");
        dsTypeLabel.setStyle(labelStyle);
        writerDsTypeCombo = new ComboBox<>(FXCollections.observableArrayList(DS_TYPES));
        writerDsTypeCombo.setPromptText("选择数据源类型");
        writerDsTypeCombo.setOnAction(e -> filterWriterDatasources());

        // 数据源
        Label dsLabel = new Label("数据源");
        dsLabel.setStyle(labelStyle);
        writerDatasourceCombo = new ComboBox<>();
        writerDatasourceCombo.setPromptText("选择数据源");
        writerDatasourceCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JobJdbcDatasource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDatasourceName() + ":" + item.getDatabaseName());
            }
        });
        writerDatasourceCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JobJdbcDatasource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDatasourceName() + ":" + item.getDatabaseName());
            }
        });
        writerDatasourceCombo.setOnAction(e -> loadWriterTables());

        // 表列表（多选）
        Label tableLabel = new Label("数据表（按住Ctrl多选）");
        tableLabel.setStyle(labelStyle);
        writerTableList = new ListView<>();
        writerTableList.setPrefHeight(250);
        writerTableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button selectAllBtn = new Button("全选");
        selectAllBtn.setOnAction(e -> writerTableList.getSelectionModel().selectAll());
        Button clearBtn = new Button("清空");
        clearBtn.setOnAction(e -> writerTableList.getSelectionModel().clearSelection());

        // 写入模式
        Label modeLabel = new Label("写入模式");
        modeLabel.setStyle(labelStyle);
        writeModeCombo = new ComboBox<>(FXCollections.observableArrayList(WRITE_MODES));
        writeModeCombo.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.add(dsTypeLabel, 0, 0);
        grid.add(writerDsTypeCombo, 1, 0);
        grid.add(dsLabel, 0, 1);
        grid.add(writerDatasourceCombo, 1, 1);
        grid.add(tableLabel, 0, 2);
        VBox tableBox = new VBox(8, writerTableList, new HBox(8, selectAllBtn, clearBtn));
        grid.add(tableBox, 1, 2);
        grid.add(modeLabel, 0, 3);
        grid.add(writeModeCombo, 1, 3);

        pane.getChildren().addAll(title, grid);
        return new ScrollPane(pane);
    }

    private Node createResultPane() {
        VBox container = new VBox(15);
        container.getStyleClass().add("dialog-content-root");
        container.setPadding(new Insets(20));
        container.setPrefWidth(800);
        container.setPrefHeight(550);

        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(10));
        formContent.getChildren().add(createBasicSection());
        formContent.getChildren().add(createScheduleSection());
        advancedSection = createAdvancedSection();
        advancedSection.setVisible(false);
        advancedSection.setManaged(false);
        formContent.getChildren().add(advancedSection);

        HBox jsonTitleRow = new HBox(8);
        jsonTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label jsonLabel = new Label("生成的批量DataX JSON配置");
        jsonLabel.setStyle(StyleUtil.bodyFontOnly() + "-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button copyJsonBtn = new Button("复制JSON");
        copyJsonBtn.getStyleClass().add("dialog-button-secondary");
        copyJsonBtn.setOnAction(e -> {
            copyToClipboard(jsonResultArea.getText());
            showInfo("已复制到剪贴板");
        });
        jsonTitleRow.getChildren().addAll(jsonLabel, spacer, copyJsonBtn);

        VBox pane = new VBox(12);
        pane.setPadding(new Insets(16));
        jsonResultArea = new TextArea();
        jsonResultArea.setEditable(false);
        jsonResultArea.setWrapText(true);
        jsonResultArea.setPrefRowCount(20);
        jsonResultArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px;");
        VBox.setVgrow(jsonResultArea, Priority.ALWAYS);
        pane.getChildren().addAll(jsonTitleRow, jsonResultArea);

        container.getChildren().addAll(formContent, pane);
        VBox.setVgrow(container, Priority.ALWAYS);
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private VBox createSection(String title, FontIcon icon) {
        VBox section = new VBox(10);
        section.getStyleClass().add("dialog-section");
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 5, 0));
        if (icon != null) {
            icon.setIconSize(18);
            icon.setIconColor(Color.web("#6B7280"));
        }
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        if (icon != null) titleBox.getChildren().add(icon);
        titleBox.getChildren().add(titleLabel);
        section.getChildren().add(titleBox);
        return section;
    }

    private Label createFormLabel(String text, boolean required) {
        Label label = new Label();
        label.setMinWidth(140);
        label.setPrefWidth(140);
        label.setMaxWidth(140);
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (required) {
            HBox labelBox = new HBox(2);
            labelBox.setAlignment(Pos.CENTER_LEFT);
            Label textLabel = new Label(text);
            textLabel.setStyle("-fx-font-size: 13;");
            Label starLabel = new Label("*");
            starLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
            labelBox.getChildren().addAll(textLabel, starLabel);
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

    private VBox createBasicSection() {
        List<JobGroup> jobGroupList;
        try {
            jobGroupList = jobGroupService.getAllJobGroupList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        VBox section = createSection("基本信息", IconUtil.fileIcon());
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
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
        Label authorLabel = createFormLabel("负责人", true);
        authorField = new TextField();
        authorField.setPrefWidth(300);
        authorField.setPromptText("请输入负责人姓名");
        Label jobDescLabel = createFormLabel("任务描述", true);
        jobDescField = new TextField();
        jobDescField.setPrefWidth(615);
        jobDescField.setPromptText("请输入任务描述");
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

    private VBox createScheduleSection() {
        VBox section = createSection("调度配置", IconUtil.clockIcon());
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        Label scheduleTypeLabel = createFormLabel("调度类型", true);
        scheduleTypeCombo = new ComboBox<>();
        scheduleTypeCombo.setPrefWidth(300);
        scheduleTypeCombo.setPromptText("Select");
        scheduleTypeCombo.getItems().addAll(NewJobDialog.ScheduleType.values());
        scheduleTypeCombo.setValue(NewJobDialog.ScheduleType.CRON);
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
        HBox linkContainer = new HBox(5);
        linkContainer.setPadding(new Insets(10, 15, 0, 15));
        linkContainer.setAlignment(Pos.CENTER_LEFT);
        Hyperlink advancedLink = new Hyperlink("高级配置");
        advancedLink.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 13; -fx-underline: true; -fx-cursor: hand;");
        advancedLink.setOnAction(e -> toggleAdvancedSection());
        linkContainer.getChildren().add(advancedLink);
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
                getDialogPane().setPrefHeight(advancedSectionVisible ? 1000 : 700);
                stage.setMinHeight(advancedSectionVisible ? 1000 : 650);
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
        Label routeStrategyLabel = createFormLabel("路由策略", true);
        routeStrategyCombo = new ComboBox<>();
        routeStrategyCombo.setPrefWidth(300);
        routeStrategyCombo.setPromptText("请选择");
        routeStrategyCombo.getItems().addAll(NewJobDialog.RouteStrategy.values());
        routeStrategyCombo.setValue(NewJobDialog.RouteStrategy.FIRST);
        Label childJobIdLabel = createFormLabel("子任务id", false);
        childJobIdField = new TextField();
        childJobIdField.setPrefWidth(300);
        childJobIdField.setPromptText("多个子任务使用逗号分隔");
        Label misfireStrategyLabel = createFormLabel("调度过期策略", true);
        misfireStrategyCombo = new ComboBox<>();
        misfireStrategyCombo.setPrefWidth(300);
        misfireStrategyCombo.setPromptText("Select");
        misfireStrategyCombo.getItems().addAll(NewJobDialog.MisfireStrategy.values());
        misfireStrategyCombo.setValue(NewJobDialog.MisfireStrategy.DO_NOTHING);
        Label blockStrategyLabel = createFormLabel("阻塞处理策略", true);
        blockStrategyCombo = new ComboBox<>();
        blockStrategyCombo.setPrefWidth(300);
        blockStrategyCombo.setPromptText("Select");
        blockStrategyCombo.getItems().addAll(NewJobDialog.BlockStrategy.values());
        blockStrategyCombo.setValue(NewJobDialog.BlockStrategy.SERIAL_EXECUTION);
        Label timeoutLabel = createFormLabel("任务超时时间", false);
        executorTimeoutField = new TextField();
        executorTimeoutField.setPrefWidth(300);
        Label retryLabel = createFormLabel("失败重试次数", false);
        executorFailRetryCountField = new TextField();
        executorFailRetryCountField.setPrefWidth(300);
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

    private void handleNext() {
        if (currentStep == 0) {
            List<String> selectedTables = new ArrayList<>(readerTableList.getSelectionModel().getSelectedItems());
            if (selectedTables.isEmpty()) {
                showError("验证失败", "请选择要读取的数据表");
                return;
            }
            currentStep++;
            updateStepView();
        } else if (currentStep == 1) {
            List<String> selectedTables = new ArrayList<>(writerTableList.getSelectionModel().getSelectedItems());
            if (selectedTables.isEmpty()) {
                showError("验证失败", "请选择要写入的数据表");
                return;
            }
            generateBatchJson();
            currentStep++;
            updateStepView();
        } else {
            // 确认：校验并按表对批量创建任务
            if (jobGroupCombo.getValue() == null) {
                showError("验证失败", "请选择执行器");
                return;
            }
            String author = authorField.getText() != null ? authorField.getText().trim() : "";
            String jobDesc = jobDescField.getText() != null ? jobDescField.getText().trim() : "";
            if (jobDesc.isEmpty()) {
                showError("验证失败", "请输入任务描述");
                return;
            }
            if (author.isEmpty()) {
                showError("验证失败", "请输入负责人姓名");
                return;
            }
            NewJobDialog.ScheduleType scheduleType = scheduleTypeCombo.getValue();
            if (scheduleType != null && scheduleType != NewJobDialog.ScheduleType.NONE) {
                String scheduleConf = scheduleConfField.getText() != null ? scheduleConfField.getText().trim() : "";
                if (scheduleConf.isEmpty()) {
                    showError("验证失败", "请填写调度配置（Cron 或固定速率秒数）");
                    return;
                }
            }
            List<String> readerTables = new ArrayList<>(readerTableList.getSelectionModel().getSelectedItems());
            List<String> writerTables = new ArrayList<>(writerTableList.getSelectionModel().getSelectedItems());
            int size = Math.min(readerTables.size(), writerTables.size());
            if (size == 0) {
                showError("验证失败", "请至少选择一对读表与写表");
                return;
            }
            new Thread(() -> {
                int created = 0;
                try {
                    for (int i = 0; i < size; i++) {
                        String readerTable = readerTables.get(i);
                        String writerTable = writerTables.get(i);
                        String jsonParam = buildSinglePairJson(readerTable, writerTable);
                        JobInfoForm form = buildDataxJobInfoForm();
                        form.setExecutorParam(jsonParam);
                        form.setJobDesc(jobDesc + " - " + readerTable + "->" + writerTable);
                        jobInfoService.saveJobInfo(form);
                        created++;
                    }
                    int finalCreated = created;
                    Platform.runLater(() -> {
                        showInfo("已成功创建 " + finalCreated + " 个任务");
                        close();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("保存失败", e.getMessage()));
                }
            }, "datax-group-save-jobs").start();
        }
    }

    /**
     * 生成单对表的 DataX job JSON（一个 content 元素），多数据源仅全量
     */
    private String buildSinglePairJson(String readerTable, String writerTable) throws Exception {
        JobJdbcDatasource readerDs = readerDatasourceCombo.getValue();
        JobJdbcDatasource writerDs = writerDatasourceCombo.getValue();
        if (readerDs == null || writerDs == null) {
            throw new IllegalStateException("请选择数据源");
        }
        List<String> readerCols = dataxService.getColumns(readerDs.getId(), readerTable, null, null);
        List<String> writerCols = dataxService.getColumns(writerDs.getId(), writerTable, null, null);
        JobDataxService.DataXParams readerParams = new JobDataxService.DataXParams();
        readerParams.setColumns(readerCols);
        readerParams.setSourceType(readerDs.getDatasource());
        readerParams.setUsername(readerDs.getJdbcUsername());
        readerParams.setPassword(readerDs.getJdbcPassword());
        readerParams.setDbName(readerDs.getDatabaseName());
        readerParams.setTableName(readerTable);
        String[] readerIpPort = parseIpPort(readerDs.getJdbcUrl());
        readerParams.setIp(readerIpPort[0]);
        readerParams.setPort(readerIpPort[1]);
        readerParams.setType(0);
        readerParams.setIncrementType(0);
        JobDataxService.DataXParams writerParams = new JobDataxService.DataXParams();
        writerParams.setColumns(writerCols);
        writerParams.setSourceType(writerDs.getDatasource());
        writerParams.setUsername(writerDs.getJdbcUsername());
        writerParams.setPassword(writerDs.getJdbcPassword());
        writerParams.setDbName(writerDs.getDatabaseName());
        writerParams.setTableName(writerTable);
        String[] writerIpPort = parseIpPort(writerDs.getJdbcUrl());
        writerParams.setIp(writerIpPort[0]);
        writerParams.setPort(writerIpPort[1]);
        writerParams.setType(1);
        writerParams.setWriteMode(writeModeCombo.getValue());
        String readerJson = dataxService.getJson(readerParams);
        String writerJson = dataxService.getJson(writerParams);
        String singleContent = "{\"reader\":" + readerJson + ",\"writer\":" + writerJson + "}";
        return "{\"job\":{\"content\":[" + singleContent + "],\"setting\":{\"speed\":{\"channel\":3,\"byte\":-1},\"errorLimit\":{\"record\":0,\"percentage\":0.02}}}}";
    }

    private JobInfoForm buildDataxJobInfoForm() {
        JobInfoForm form = new JobInfoForm();
        form.setJobGroup(jobGroupCombo.getValue().getId());
        form.setJobDesc(jobDescField.getText() != null ? jobDescField.getText().trim() : "");
        form.setAuthor(authorField.getText() != null ? authorField.getText().trim() : "");
        form.setAlarmEmail(alarmEmailField.getText() != null ? alarmEmailField.getText().trim() : "");
        NewJobDialog.ScheduleType st = scheduleTypeCombo.getValue();
        form.setScheduleType(st != null ? st.getType() : "CRON");
        form.setScheduleConf(scheduleConfField.getText() != null ? scheduleConfField.getText().trim() : "0 0 0 * * ?");
        form.setMisfireStrategy(misfireStrategyCombo.getValue() != null ? misfireStrategyCombo.getValue().getType() : "DO_NOTHING");
        form.setExecutorRouteStrategy(routeStrategyCombo.getValue() != null ? routeStrategyCombo.getValue().getType() : "FIRST");
        form.setFailStrategy("JOB_FAIL");
        form.setExecutorBlockStrategy(blockStrategyCombo.getValue() != null ? blockStrategyCombo.getValue().getType() : "SERIAL_EXECUTION");
        form.setGlueType("DATAX");
        form.setExecutorHandler("runDataxHandler");
        form.setJobType(0);
        String timeoutStr = executorTimeoutField.getText() != null ? executorTimeoutField.getText().trim() : "";
        if (!timeoutStr.isEmpty()) {
            try {
                form.setExecutorTimeout(Integer.parseInt(timeoutStr));
            } catch (NumberFormatException ignored) { }
        }
        String retryStr = executorFailRetryCountField.getText() != null ? executorFailRetryCountField.getText().trim() : "";
        if (!retryStr.isEmpty()) {
            try {
                form.setExecutorFailRetryCount(Integer.parseInt(retryStr));
            } catch (NumberFormatException ignored) { }
        }
        String childIds = childJobIdField.getText() != null ? childJobIdField.getText().trim() : "";
        if (!childIds.isEmpty()) {
            form.setChildJobId(childIds);
        }
        return form;
    }

    private void generateBatchJson() {
        if (loading.getAndSet(true)) return;
        new Thread(() -> {
            try {
                JobJdbcDatasource readerDs = readerDatasourceCombo.getValue();
                JobJdbcDatasource writerDs = writerDatasourceCombo.getValue();
                if (readerDs == null || writerDs == null) {
                    Platform.runLater(() -> showError("错误", "请选择数据源"));
                    return;
                }

                List<String> readerTables = new ArrayList<>(readerTableList.getSelectionModel().getSelectedItems());
                List<String> writerTables = new ArrayList<>(writerTableList.getSelectionModel().getSelectedItems());

                StringBuilder contentBuilder = new StringBuilder();
                int size = Math.min(readerTables.size(), writerTables.size());

                for (int i = 0; i < size; i++) {
                    String readerTable = readerTables.get(i);
                    String writerTable = writerTables.get(i);

                    // 获取字段
                    List<String> readerCols = dataxService.getColumns(readerDs.getId(), readerTable, null, null);
                    List<String> writerCols = dataxService.getColumns(writerDs.getId(), writerTable, null, null);

                    // 构建Reader参数
                    JobDataxService.DataXParams readerParams = new JobDataxService.DataXParams();
                    readerParams.setColumns(readerCols);
                    readerParams.setSourceType(readerDs.getDatasource());
                    readerParams.setUsername(readerDs.getJdbcUsername());
                    readerParams.setPassword(readerDs.getJdbcPassword());
                    readerParams.setDbName(readerDs.getDatabaseName());
                    readerParams.setTableName(readerTable);
                    String[] readerIpPort = parseIpPort(readerDs.getJdbcUrl());
                    readerParams.setIp(readerIpPort[0]);
                    readerParams.setPort(readerIpPort[1]);
                    readerParams.setType(0);
                    readerParams.setIncrementType(0); // 多数据源仅支持全量

                    // 构建Writer参数
                    JobDataxService.DataXParams writerParams = new JobDataxService.DataXParams();
                    writerParams.setColumns(writerCols);
                    writerParams.setSourceType(writerDs.getDatasource());
                    writerParams.setUsername(writerDs.getJdbcUsername());
                    writerParams.setPassword(writerDs.getJdbcPassword());
                    writerParams.setDbName(writerDs.getDatabaseName());
                    writerParams.setTableName(writerTable);
                    String[] writerIpPort = parseIpPort(writerDs.getJdbcUrl());
                    writerParams.setIp(writerIpPort[0]);
                    writerParams.setPort(writerIpPort[1]);
                    writerParams.setType(1);
                    writerParams.setWriteMode(writeModeCombo.getValue());

                    String readerJson = dataxService.getJson(readerParams);
                    String writerJson = dataxService.getJson(writerParams);

                    if (i > 0) contentBuilder.append(",");
                    contentBuilder.append("{\"reader\":").append(readerJson)
                            .append(",\"writer\":").append(writerJson).append("}");
                }

                String finalJson = "{\"job\":{\"content\":[" + contentBuilder.toString() +
                        "],\"setting\":{\"speed\":{\"channel\":3,\"byte\":-1},\"errorLimit\":{\"record\":0,\"percentage\":0.02}}}}";

                Platform.runLater(() -> jsonResultArea.setText(formatJson(finalJson)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("生成失败", e.getMessage()));
            } finally {
                loading.set(false);
            }
        }).start();
    }

    private String formatJson(String json) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Object obj = gson.fromJson(json, Object.class);
            return gson.toJson(obj);
        } catch (Exception e) {
            return json;
        }
    }

    private String[] parseIpPort(String jdbcUrl) {
        Pattern pattern = Pattern.compile("//([^:]+):([^/]+)");
        Matcher matcher = pattern.matcher(jdbcUrl);
        if (matcher.find()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return new String[]{"localhost", "3306"};
    }

    private void handleReset() {
        currentStep = 0;
        updateStepView();
        if (incrTypeCombo != null) incrTypeCombo.getSelectionModel().selectFirst();
        if (incrConfigBox != null) {
            updateIncrConfigVisibility();
            if (incrColumnField != null) incrColumnField.clear();
            if (incrInitValueField != null) incrInitValueField.clear();
        }
    }

    private void updateIncrConfigVisibility() {
        boolean isIncremental = "增量".equals(incrTypeCombo != null ? incrTypeCombo.getValue() : null);
        if (incrConfigBox != null) {
            incrConfigBox.setVisible(isIncremental);
            incrConfigBox.setManaged(isIncremental);
        }
    }

    private void updateIncrModeConfig() {
        if (incrModeCombo == null || incrConfigBox == null || incrConfigBox.getChildren().size() <= 3) return;
        boolean isTimeMode = "时间自增".equals(incrModeCombo.getValue());
        Node timeFormatRow = incrConfigBox.getChildren().get(3);
        timeFormatRow.setVisible(isTimeMode);
        timeFormatRow.setManaged(isTimeMode);
    }

    private String buildIncrementContent() {
        String columnKey = incrColumnField.getText().trim();
        String columnValue = incrInitValueField.getText().trim();
        String mode = incrModeCombo.getValue();
        int columnType = "时间自增".equals(mode) ? 1 : 0;
        String timeFormat = columnType == 1 && incrTimeFormatCombo.getValue() != null
                ? incrTimeFormatCombo.getValue() : "x";
        JsonObject columnObj = new JsonObject();
        columnObj.addProperty("columnKey", columnKey);
        columnObj.addProperty("columnValue", columnValue);
        columnObj.addProperty("columnParam", columnKey);
        columnObj.addProperty("columnTimeFormat", timeFormat);
        columnObj.addProperty("columnType", columnType);
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(columnObj);
        return new Gson().toJson(jsonArray);
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private void loadDatasources() {
        new Thread(() -> {
            try {
                allDatasources = datasourceService.getDatasourceList();
            } catch (Exception e) {
                Platform.runLater(() -> showError("加载数据源失败", e.getMessage()));
            }
        }).start();
    }

    private void filterReaderDatasources() {
        String type = readerDsTypeCombo.getValue();
        if (type == null) return;
        List<JobJdbcDatasource> filtered = allDatasources.stream()
                .filter(ds -> type.equals(ds.getDatasource())).toList();
        readerDatasourceCombo.setItems(FXCollections.observableArrayList(filtered));
    }

    private void filterWriterDatasources() {
        String type = writerDsTypeCombo.getValue();
        if (type == null) return;
        List<JobJdbcDatasource> filtered = allDatasources.stream()
                .filter(ds -> type.equals(ds.getDatasource())).toList();
        writerDatasourceCombo.setItems(FXCollections.observableArrayList(filtered));
    }

    private void loadReaderTables() {
        JobJdbcDatasource ds = readerDatasourceCombo.getValue();
        if (ds == null) return;
        new Thread(() -> {
            try {
                List<DataxTable> tableList = dataxService.getTables(ds.getId());
                List<String> tableNames = (tableList != null ? tableList : List.<DataxTable>of())
                        .stream()
                        .map(DataxTable::getTableName)
                        .toList();
                Platform.runLater(() -> readerTableList.setItems(FXCollections.observableArrayList(tableNames)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("加载表失败", e.getMessage()));
            }
        }).start();
    }

    private void loadWriterTables() {
        JobJdbcDatasource ds = writerDatasourceCombo.getValue();
        if (ds == null) return;
        new Thread(() -> {
            try {
                List<DataxTable> tableList = dataxService.getTables(ds.getId());
                List<String> tableNames = (tableList != null ? tableList : List.<DataxTable>of())
                        .stream()
                        .map(DataxTable::getTableName)
                        .toList();
                Platform.runLater(() -> writerTableList.setItems(FXCollections.observableArrayList(tableNames)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("加载表失败", e.getMessage()));
            }
        }).start();
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle("提示");
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
}
