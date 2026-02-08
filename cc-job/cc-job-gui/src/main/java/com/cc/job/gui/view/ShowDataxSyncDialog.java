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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据源同步对话框 - 三步向导
 */
public class ShowDataxSyncDialog extends Dialog<Void> {

    private final JobJdbcDatasourceService datasourceService = new JobJdbcDatasourceService();
    private final JobDataxService dataxService = new JobDataxService();
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int currentStep = 0;
    private StackPane contentPane;
    private Button prevBtn, nextBtn;
    private Label step1Label, step2Label, step3Label;

    /** 步骤面板缓存，避免「上一步」时重新创建导致数据丢失 */
    private Node readerPaneCache;
    private Node writerPaneCache;
    private Node resultPaneCache;

    // Step 1 - Reader配置
    private ComboBox<String> readerDsTypeCombo;
    private ComboBox<JobJdbcDatasource> readerDatasourceCombo;
    private HBox readerTableRadioBox;  // 数据表单选框容器（多列布局）
    private ToggleGroup readerTableToggleGroup;  // 数据表单选组
    private TextArea readerSqlArea;
    private HBox readerColumnCheckBox;  // 表字段多选框容器（多列布局）
    private ScrollPane readerColumnScrollPane;  // 表字段滚动容器
    private ComboBox<String> incrTypeCombo;  // 增量类型：全量/增量
    private VBox incrConfigBox;  // 增量配置容器
    private ComboBox<String> incrModeCombo;  // 增量模式：ID自增/时间自增
    private TextField incrColumnField;  // 增量字段名
    private TextField incrInitValueField;  // 增量初始值
    private ComboBox<String> incrTimeFormatCombo;  // 时间格式（仅时间自增时显示）

    // Step 2 - Writer配置
    private ComboBox<String> writerDsTypeCombo;
    private ComboBox<JobJdbcDatasource> writerDatasourceCombo;
    private HBox writerTableRadioBox;  // 数据表单选框容器（多列布局）
    private ToggleGroup writerTableToggleGroup;  // 数据表单选组
    private TextArea writerSqlArea;
    private HBox writerColumnCheckBox;  // 表字段多选框容器（多列布局）
    private ScrollPane writerColumnScrollPane;  // 表字段滚动容器
    private ComboBox<String> writeModeCombo;

    // Step 3 - 结果
    private TextArea jsonResultArea;

    // 所有数据源
    private List<JobJdbcDatasource> allDatasources = new ArrayList<>();

    private static final String[] DS_TYPES = {"MYSQL", "ORACLE", "POSTGRESQL"};
    private static final String[] WRITE_MODES = {"insert", "update", "replace"};
    private static final String[] INCR_MODES = {"ID自增", "时间自增"};
    private static final String[] TIME_FORMATS = {"YYYY-MM-DD HH:mm:ss", "YYYY-MM-DD", "YYYY/MM/DD HH:mm:ss", "YYYY/MM/DD"};
    /** 数据表/表字段区域固定高度（约6行），保证一列完整可见且仅横向滚动 */
    private static final double TABLE_AND_COLUMN_AREA_HEIGHT = 165;
    /** 数据表/表字段每列宽度，避免表名与字段名被裁切 */
    private static final double TABLE_AND_COLUMN_COLUMN_WIDTH = 165;
    
    // 存储表数据，用于单选
    private List<DataxTable> readerTables = new ArrayList<>();
    private List<DataxTable> writerTables = new ArrayList<>();
    // 存储字段数据，用于多选
    private List<String> readerColumns = new ArrayList<>();
    private List<String> writerColumns = new ArrayList<>();
    private final JobGroupService jobGroupService;
    private final JobInfoService jobInfoService = new JobInfoService();
    // 表单字段
    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField authorField;
    private TextField alarmEmailField;

    // 调度配置
    private ComboBox<NewJobDialog.ScheduleType> scheduleTypeCombo;
    private TextField scheduleConfField;
    private Label scheduleConfLabel;

    // 高级配置
    private ComboBox<NewJobDialog.RouteStrategy> routeStrategyCombo;
    private ComboBox<NewJobDialog.MisfireStrategy> misfireStrategyCombo;
    private ComboBox<NewJobDialog.BlockStrategy> blockStrategyCombo;
    private TextField childJobIdField;
    private TextField executorTimeoutField;
    private TextField executorFailRetryCountField;

    // 高级配置容器
    private VBox advancedSection;
    private boolean advancedSectionVisible = false;

    public ShowDataxSyncDialog(Stage ownerStage) {
        this.jobGroupService = new JobGroupService();
        setTitle("数据源同步");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

        root.getChildren().addAll(createStepIndicator(), createContentPane(), createButtonBar());

        getDialogPane().setContent(root);

        // 加载数据源
        loadDatasources();
        updateStepView();
    }

    private void styleDialog() {
        getDialogPane().setPrefSize(1000, 950);
        getDialogPane().setMinWidth(900);
        getDialogPane().setMinHeight(950);
        setResizable(true);
        String dialogCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            getDialogPane().getStylesheets().add(dialogCss);
        }
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                // 设置关闭事件处理
                stage.setOnCloseRequest(event -> {
                    close();
                });
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

        step1Label = createStepLabel("1. Reader配置", true);
        step2Label = createStepLabel("2. Writer配置", false);
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
        updateStepLabelStyle(step1Label, currentStep == 0);
        updateStepLabelStyle(step2Label, currentStep == 1);
        updateStepLabelStyle(step3Label, currentStep == 2);

        prevBtn.setDisable(currentStep == 0);
        nextBtn.setText(currentStep == 2 ? "确认" : "下一步");

        if (readerPaneCache == null) readerPaneCache = createReaderPane();
        if (writerPaneCache == null) writerPaneCache = createWriterPane();
        if (resultPaneCache == null) resultPaneCache = createResultPane();

        contentPane.getChildren().clear();
        switch (currentStep) {
            case 0 -> contentPane.getChildren().add(readerPaneCache);
            case 1 -> contentPane.getChildren().add(writerPaneCache);
            case 2 -> contentPane.getChildren().add(resultPaneCache);
        }
    }

    private Node createReaderPane() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("dialog-section");
        HBox.setHgrow(pane, Priority.ALWAYS);

        String labelStyle = StyleUtil.bodyFontOnly() + " -fx-min-width: 100;";

        // 使用GridPane创建两列布局，充分利用空间
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(0));
        HBox.setHgrow(grid, Priority.ALWAYS);

        // 数据源类型和数据源并排显示
        Label dsTypeLabel = new Label("数据源类型");
        dsTypeLabel.setStyle(labelStyle);
        readerDsTypeCombo = new ComboBox<>(FXCollections.observableArrayList(DS_TYPES));
        readerDsTypeCombo.setPromptText("选择数据源类型");
        HBox.setHgrow(readerDsTypeCombo, Priority.ALWAYS);
        readerDsTypeCombo.setOnAction(e -> filterReaderDatasources());
        grid.add(dsTypeLabel, 0, 0);
        grid.add(readerDsTypeCombo, 1, 0);

        Label dsLabel = new Label("数据源");
        dsLabel.setStyle(labelStyle);
        readerDatasourceCombo = new ComboBox<>();
        readerDatasourceCombo.setPromptText("选择数据源");
        HBox.setHgrow(readerDatasourceCombo, Priority.ALWAYS);
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
        grid.add(dsLabel, 0, 1);
        grid.add(readerDatasourceCombo, 1, 1);

        // 数据表 - 单选框（多列布局，每列6条）
        Label tableLabel = new Label("数据表");
        tableLabel.setStyle(labelStyle);
        readerTableToggleGroup = new ToggleGroup();
        HBox tableColumnsContainer = new HBox(12);
        // 默认状态：无边框，只显示提示文字
        tableColumnsContainer.setStyle("-fx-background-color: transparent;");
        // 默认显示提示信息
        Label tablePlaceholder = new Label("请选择数据源");
        tablePlaceholder.setStyle(StyleUtil.bodyFontOnly());
        tablePlaceholder.setAlignment(Pos.CENTER);
        tablePlaceholder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tablePlaceholder, Priority.ALWAYS);
        tableColumnsContainer.getChildren().add(tablePlaceholder);
        readerTableRadioBox = tableColumnsContainer; // 保持兼容性，实际使用tableColumnsContainer
        ScrollPane tableScrollPane = new ScrollPane(tableColumnsContainer);
        tableScrollPane.setFitToHeight(true);
        tableScrollPane.setFitToWidth(false); // 允许横向滚动
        tableScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tableScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        tableScrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        tableScrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        VBox.setVgrow(tableScrollPane, Priority.ALWAYS);
        grid.add(tableLabel, 0, 2);
        grid.add(tableScrollPane, 1, 2);

        // SQL
        Label sqlLabel = new Label("SQL (可选)");
        sqlLabel.setStyle(labelStyle);
        readerSqlArea = new TextArea();
        readerSqlArea.setMinHeight(120);
        readerSqlArea.setPrefRowCount(5);
        readerSqlArea.setPromptText("使用多表查询时，columns可留空");
        readerSqlArea.setWrapText(true);
        VBox.setVgrow(readerSqlArea, Priority.ALWAYS);
        HBox sqlBtnRow = new HBox(8);
        sqlBtnRow.setAlignment(Pos.CENTER_RIGHT);
        Button parseSqlBtn = new Button("SQL解析");
        parseSqlBtn.getStyleClass().add("dialog-button-success");
        parseSqlBtn.setOnAction(e -> loadReaderColumns());
        sqlBtnRow.getChildren().add(parseSqlBtn);
        VBox sqlContainer = new VBox(8, readerSqlArea, sqlBtnRow);
        VBox.setVgrow(sqlContainer, Priority.ALWAYS);
        grid.add(sqlLabel, 0, 3);
        grid.add(sqlContainer, 1, 3);

        // 表字段 - 多选框
        Label columnLabel = new Label("表字段");
        columnLabel.setStyle(labelStyle);
        HBox columnHeader = new HBox(8);
        columnHeader.setAlignment(Pos.CENTER_LEFT);
        Button selectAllBtn = new Button("全选");
        selectAllBtn.getStyleClass().add("dialog-button-secondary");
        selectAllBtn.setPrefWidth(80);
        selectAllBtn.setOnAction(e -> forEachColumnCheckBox(readerColumnCheckBox, cb -> cb.setSelected(true)));
        Button clearAllBtn = new Button("清空");
        clearAllBtn.getStyleClass().add("dialog-button-secondary");
        clearAllBtn.setPrefWidth(80);
        clearAllBtn.setOnAction(e -> forEachColumnCheckBox(readerColumnCheckBox, cb -> cb.setSelected(false)));
        columnHeader.getChildren().addAll(selectAllBtn, clearAllBtn);
        
        // 表字段多列容器（每列6条）
        HBox columnColumnsContainer = new HBox(12);
        columnColumnsContainer.setPadding(new Insets(12, 16, 12, 16));
        // 默认状态：无边框，只显示提示文字
        columnColumnsContainer.setStyle("-fx-background-color: transparent;");
        columnColumnsContainer.setMinHeight(50);
        // 默认显示提示信息
        Label columnPlaceholder = new Label("请选择数据表");
        columnPlaceholder.setStyle(StyleUtil.bodyFontOnly());
        columnPlaceholder.setAlignment(Pos.CENTER);
        columnPlaceholder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(columnPlaceholder, Priority.ALWAYS);
        columnColumnsContainer.getChildren().add(columnPlaceholder);
        readerColumnCheckBox = columnColumnsContainer; // 保持兼容性，实际使用columnColumnsContainer
        readerColumnScrollPane = new ScrollPane(columnColumnsContainer);
        readerColumnScrollPane.setFitToHeight(true);
        readerColumnScrollPane.setFitToWidth(false); // 允许横向滚动
        readerColumnScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        readerColumnScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        readerColumnScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        readerColumnScrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        readerColumnScrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        VBox.setVgrow(readerColumnScrollPane, Priority.ALWAYS);
        VBox columnContainer = new VBox(8, columnHeader, readerColumnScrollPane);
        VBox.setVgrow(columnContainer, Priority.ALWAYS);
        grid.add(columnLabel, 0, 4);
        grid.add(columnContainer, 1, 4);

        // 增量备份配置
        Label incrLabel = new Label("增量备份");
        incrLabel.setStyle(labelStyle);
        incrTypeCombo = new ComboBox<>(FXCollections.observableArrayList("全量", "增量"));
        incrTypeCombo.getSelectionModel().selectFirst();
        HBox.setHgrow(incrTypeCombo, Priority.ALWAYS);
        incrTypeCombo.setOnAction(e -> updateIncrConfigVisibility());
        
        // 增量配置容器
        incrConfigBox = new VBox(8);
        incrConfigBox.setPadding(new Insets(12));
        incrConfigBox.getStyleClass().add("dialog-section");
        incrConfigBox.setStyle("-fx-background-radius: " + StyleUtil.RADIUS_MD + "; -fx-border-width: 1; -fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
        incrConfigBox.setVisible(false);
        
        // 增量模式
        HBox incrModeRow = new HBox(12);
        incrModeRow.setAlignment(Pos.CENTER_LEFT);
        Label incrModeLabel = new Label("增量模式");
        incrModeLabel.setStyle(StyleUtil.bodyFontOnly() + " -fx-min-width: 80;");
        incrModeCombo = new ComboBox<>(FXCollections.observableArrayList(INCR_MODES));
        incrModeCombo.getSelectionModel().selectFirst();
        HBox.setHgrow(incrModeCombo, Priority.ALWAYS);
        incrModeCombo.setOnAction(e -> updateIncrModeConfig());
        incrModeRow.getChildren().addAll(incrModeLabel, incrModeCombo);
        
        // 增量字段
        HBox incrColumnRow = new HBox(12);
        incrColumnRow.setAlignment(Pos.CENTER_LEFT);
        Label incrColumnLabel = new Label("增量字段");
        incrColumnLabel.setStyle(StyleUtil.bodyFontOnly() + " -fx-min-width: 80;");
        incrColumnField = new TextField();
        incrColumnField.setPromptText("请输入字段名，如：id 或 create_time");
        HBox.setHgrow(incrColumnField, Priority.ALWAYS);
        incrColumnRow.getChildren().addAll(incrColumnLabel, incrColumnField);
        
        // 初始值
        HBox incrValueRow = new HBox(12);
        incrValueRow.setAlignment(Pos.CENTER_LEFT);
        Label incrValueLabel = new Label("初始值");
        incrValueLabel.setStyle(StyleUtil.bodyFontOnly() + " -fx-min-width: 80;");
        incrInitValueField = new TextField();
        incrInitValueField.setPromptText("ID自增请输入数字，时间自增请输入时间戳(毫秒)");
        HBox.setHgrow(incrInitValueField, Priority.ALWAYS);
        incrValueRow.getChildren().addAll(incrValueLabel, incrInitValueField);
        
        // 时间格式（仅时间自增时显示）
        HBox incrTimeFormatRow = new HBox(12);
        incrTimeFormatRow.setAlignment(Pos.CENTER_LEFT);
        Label incrTimeFormatLabel = new Label("时间格式");
        incrTimeFormatLabel.setStyle(StyleUtil.bodyFontOnly() + " -fx-min-width: 80;");
        incrTimeFormatCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_FORMATS));
        incrTimeFormatCombo.getSelectionModel().selectFirst();
        HBox.setHgrow(incrTimeFormatCombo, Priority.ALWAYS);
        incrTimeFormatRow.getChildren().addAll(incrTimeFormatLabel, incrTimeFormatCombo);
        incrTimeFormatRow.setVisible(false);
        
        incrConfigBox.getChildren().addAll(incrModeRow, incrColumnRow, incrValueRow, incrTimeFormatRow);
        
        VBox incrContainer = new VBox(8, incrTypeCombo, incrConfigBox);
        VBox.setVgrow(incrContainer, Priority.ALWAYS);
        grid.add(incrLabel, 0, 5);
        grid.add(incrContainer, 1, 5);

        // 设置列宽约束
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        labelCol.setPrefWidth(120);
        labelCol.setHgrow(Priority.NEVER);
        ColumnConstraints contentCol = new ColumnConstraints();
        contentCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, contentCol);

        // 组装布局
        pane.getChildren().add(grid);
        VBox.setVgrow(pane, Priority.ALWAYS);
        
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private Node createWriterPane() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("dialog-section");
        HBox.setHgrow(pane, Priority.ALWAYS);

        String labelStyle = StyleUtil.bodyFontOnly() + " -fx-min-width: 100;";

        // 使用GridPane创建两列布局，充分利用空间
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(0));
        HBox.setHgrow(grid, Priority.ALWAYS);

        // 数据源类型和数据源并排显示
        Label dsTypeLabel = new Label("数据源类型");
        dsTypeLabel.setStyle(labelStyle);
        writerDsTypeCombo = new ComboBox<>(FXCollections.observableArrayList(DS_TYPES));
        writerDsTypeCombo.setPromptText("选择数据源类型");
        HBox.setHgrow(writerDsTypeCombo, Priority.ALWAYS);
        writerDsTypeCombo.setOnAction(e -> filterWriterDatasources());
        grid.add(dsTypeLabel, 0, 0);
        grid.add(writerDsTypeCombo, 1, 0);

        Label dsLabel = new Label("数据源");
        dsLabel.setStyle(labelStyle);
        writerDatasourceCombo = new ComboBox<>();
        writerDatasourceCombo.setPromptText("选择数据源");
        HBox.setHgrow(writerDatasourceCombo, Priority.ALWAYS);
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
        grid.add(dsLabel, 0, 1);
        grid.add(writerDatasourceCombo, 1, 1);

        // 数据表 - 单选框（多列布局，每列6条）
        Label tableLabel = new Label("数据表");
        tableLabel.setStyle(labelStyle);
        writerTableToggleGroup = new ToggleGroup();
        HBox tableColumnsContainer = new HBox(12);
        tableColumnsContainer.setPadding(new Insets(12, 16, 12, 16));
        // 默认状态：无边框，只显示提示文字
        tableColumnsContainer.setStyle("-fx-background-color: transparent;");
        tableColumnsContainer.setMinHeight(50);
        // 默认显示提示信息
        Label tablePlaceholder = new Label("请选择数据源");
        tablePlaceholder.setStyle(StyleUtil.bodyFontOnly());
        tablePlaceholder.setAlignment(Pos.CENTER);
        tablePlaceholder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tablePlaceholder, Priority.ALWAYS);
        tableColumnsContainer.getChildren().add(tablePlaceholder);
        writerTableRadioBox = tableColumnsContainer; // 保持兼容性，实际使用tableColumnsContainer
        ScrollPane tableScrollPane = new ScrollPane(tableColumnsContainer);
        tableScrollPane.setFitToHeight(true);
        tableScrollPane.setFitToWidth(false); // 允许横向滚动
        tableScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tableScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        tableScrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        tableScrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        VBox.setVgrow(tableScrollPane, Priority.ALWAYS);
        grid.add(tableLabel, 0, 2);
        grid.add(tableScrollPane, 1, 2);

        // SQL
        Label sqlLabel = new Label("SQL (可选)");
        sqlLabel.setStyle(labelStyle);
        writerSqlArea = new TextArea();
        writerSqlArea.setMinHeight(120);
        writerSqlArea.setPrefRowCount(3);
        writerSqlArea.setWrapText(true);
        VBox.setVgrow(writerSqlArea, Priority.ALWAYS);
        HBox sqlBtnRow = new HBox(8);
        sqlBtnRow.setAlignment(Pos.CENTER_RIGHT);
        Button parseSqlBtn = new Button("SQL解析");
        parseSqlBtn.getStyleClass().add("dialog-button-success");
        parseSqlBtn.setOnAction(e -> loadWriterColumns());
        sqlBtnRow.getChildren().add(parseSqlBtn);
        VBox sqlContainer = new VBox(8, writerSqlArea, sqlBtnRow);
        VBox.setVgrow(sqlContainer, Priority.ALWAYS);
        grid.add(sqlLabel, 0, 3);
        grid.add(sqlContainer, 1, 3);

        // 表字段 - 多选框
        Label columnLabel = new Label("表字段");
        columnLabel.setStyle(labelStyle);
        HBox columnHeader = new HBox(8);
        columnHeader.setAlignment(Pos.CENTER_LEFT);
        Button selectAllBtn = new Button("全选");
        selectAllBtn.getStyleClass().add("dialog-button-secondary");
        selectAllBtn.setPrefWidth(80);
        selectAllBtn.setOnAction(e -> forEachColumnCheckBox(writerColumnCheckBox, cb -> cb.setSelected(true)));
        Button clearAllBtn = new Button("清空");
        clearAllBtn.getStyleClass().add("dialog-button-secondary");
        clearAllBtn.setPrefWidth(80);
        clearAllBtn.setOnAction(e -> forEachColumnCheckBox(writerColumnCheckBox, cb -> cb.setSelected(false)));
        columnHeader.getChildren().addAll(selectAllBtn, clearAllBtn);
        
        // 表字段多列容器（每列6条）
        HBox columnColumnsContainer = new HBox(12);
        columnColumnsContainer.setPadding(new Insets(12, 16, 12, 16));
        // 默认状态：无边框，只显示提示文字
        columnColumnsContainer.setStyle("-fx-background-color: transparent;");
        columnColumnsContainer.setMinHeight(50);
        // 默认显示提示信息
        Label columnPlaceholder = new Label("请选择数据表");
        columnPlaceholder.setStyle(StyleUtil.bodyFontOnly());
        columnPlaceholder.setAlignment(Pos.CENTER);
        columnPlaceholder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(columnPlaceholder, Priority.ALWAYS);
        columnColumnsContainer.getChildren().add(columnPlaceholder);
        writerColumnCheckBox = columnColumnsContainer; // 保持兼容性，实际使用columnColumnsContainer
        writerColumnScrollPane = new ScrollPane(columnColumnsContainer);
        writerColumnScrollPane.setFitToHeight(true);
        writerColumnScrollPane.setFitToWidth(false); // 允许横向滚动
        writerColumnScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        writerColumnScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        writerColumnScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        writerColumnScrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        writerColumnScrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        VBox.setVgrow(writerColumnScrollPane, Priority.ALWAYS);
        VBox columnContainer = new VBox(8, columnHeader, writerColumnScrollPane);
        VBox.setVgrow(columnContainer, Priority.ALWAYS);
        grid.add(columnLabel, 0, 4);
        grid.add(columnContainer, 1, 4);

        // 写入模式
        Label modeLabel = new Label("写入模式");
        modeLabel.setStyle(labelStyle);
        writeModeCombo = new ComboBox<>(FXCollections.observableArrayList(WRITE_MODES));
        writeModeCombo.getSelectionModel().selectFirst();
        HBox.setHgrow(writeModeCombo, Priority.ALWAYS);
        grid.add(modeLabel, 0, 5);
        grid.add(writeModeCombo, 1, 5);

        // 设置列宽约束
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        labelCol.setPrefWidth(120);
        labelCol.setHgrow(Priority.NEVER);
        ColumnConstraints contentCol = new ColumnConstraints();
        contentCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, contentCol);

        // 组装布局
        pane.getChildren().add(grid);
        VBox.setVgrow(pane, Priority.ALWAYS);
        
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private Node createResultPane() {
        VBox container = new VBox(15);
        container.getStyleClass().add("dialog-content-root");
        container.setPadding(new Insets(20));
        container.setPrefWidth(800);
        container.setPrefHeight(550);

//        ScrollPane scrollPane = new ScrollPane();
//        scrollPane.setFitToWidth(true);
//        scrollPane.setStyle("-fx-background-color: transparent;");
        //创建任务组页面
        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(10));
        formContent.getChildren().add(createBasicSection());

        // 调度配置
        formContent.getChildren().add(createScheduleSection());

        // 高级配置（默认隐藏）
        advancedSection = createAdvancedSection();
        advancedSection.setVisible(false);
        advancedSection.setManaged(false);
        formContent.getChildren().add(advancedSection);

        // JSON 标题行：左侧标题 + 右侧复制JSON、放大按钮
        HBox jsonTitleRow = new HBox(8);
        jsonTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label jsonLabel = new Label("生成的DataX JSON配置");
        jsonLabel.setStyle(StyleUtil.bodyFontOnly() + "-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button copyJsonBtn = new Button("复制JSON");
        copyJsonBtn.getStyleClass().add("dialog-button-secondary");
        copyJsonBtn.setOnAction(e -> {
            copyToClipboard(jsonResultArea.getText());
            showInfo("已复制到剪贴板");
        });
        Button expandJsonBtn = new Button();
        FontIcon expandIcon = IconUtil.expandIcon();
        expandIcon.setIconSize(18);
        expandJsonBtn.setGraphic(expandIcon);
        expandJsonBtn.setTooltip(new Tooltip("放大编辑"));
        expandJsonBtn.getStyleClass().add("dialog-button-secondary");
        expandJsonBtn.setOnAction(e -> showJsonExpandDialog());
        jsonTitleRow.getChildren().addAll(jsonLabel, spacer, copyJsonBtn, expandJsonBtn);

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
        return container;
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
     * 创建基本信息部分
     */
    private VBox createBasicSection() {
        List<JobGroup> jobGroupList;
        try{
            jobGroupList = jobGroupService.getAllJobGroupList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
        scheduleTypeCombo.getItems().addAll(NewJobDialog.ScheduleType.values());
        scheduleTypeCombo.setValue(NewJobDialog.ScheduleType.CRON);

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
                    getDialogPane().setPrefHeight(1000);
                    stage.setMinHeight(1000);
                } else {
                    getDialogPane().setPrefHeight(950);
                    stage.setMinHeight(950);
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
        routeStrategyCombo.getItems().addAll(NewJobDialog.RouteStrategy.values());
        routeStrategyCombo.setValue(NewJobDialog.RouteStrategy.FIRST);

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
        misfireStrategyCombo.getItems().addAll(NewJobDialog.MisfireStrategy.values());
        misfireStrategyCombo.setValue(NewJobDialog.MisfireStrategy.DO_NOTHING);

        // 阻塞处理策略
        Label blockStrategyLabel = createFormLabel("阻塞处理策略", true);
        blockStrategyCombo = new ComboBox<>();
        blockStrategyCombo.setPrefWidth(300);
        blockStrategyCombo.setPromptText("Select");
        blockStrategyCombo.getItems().addAll(NewJobDialog.BlockStrategy.values());
        blockStrategyCombo.setValue(NewJobDialog.BlockStrategy.SERIAL_EXECUTION);

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

    private void handleNext() {
        if (currentStep == 0) {
            // 验证Reader配置
            List<String> selectedCols = getSelectedReaderColumns();
            if (selectedCols.isEmpty() && isBlank(readerSqlArea.getText())) {
                showError("验证失败", "请选择要同步的数据列或输入SQL");
                return;
            }
            // 验证增量配置
            if ("增量".equals(incrTypeCombo.getValue())) {
                if (isBlank(incrColumnField.getText())) {
                    showError("验证失败", "增量同步需要填写增量字段");
                    return;
                }
                if (isBlank(incrInitValueField.getText())) {
                    showError("验证失败", "增量同步需要填写初始值");
                    return;
                }
            }
            currentStep++;
            updateStepView();
        } else if (currentStep == 1) {
            // 验证Writer配置
            List<String> selectedCols = getSelectedWriterColumns();
            if (selectedCols.isEmpty()) {
                showError("验证失败", "请选择要写入的数据列");
                return;
            }
            // 生成JSON
            generateJson();
            currentStep++;
            updateStepView();
        } else {
            // 确认：校验并新增任务
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
            String jsonParam = jsonResultArea.getText() != null ? jsonResultArea.getText().trim() : "";
            if (jsonParam.isEmpty()) {
                showError("验证失败", "生成的 JSON 为空，请先完成 Reader/Writer 配置并生成 JSON");
                return;
            }
            JobInfoForm form = buildDataxJobInfoForm();
            form.setExecutorParam(jsonParam);
            new Thread(() -> {
                try {
                    long id = jobInfoService.saveJobInfo(form);
                    Platform.runLater(() -> {
                        showInfo("任务已成功添加到任务列表，ID: " + id);
                        close();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("保存失败", e.getMessage()));
                }
            }, "datax-save-job").start();
        }
    }

    /**
     * 组装 DataX 任务的 JobInfoForm（不含 executorParam，由调用方设置）
     */
    private JobInfoForm buildDataxJobInfoForm() {
        JobInfoForm form = new JobInfoForm();
        form.setJobGroup(jobGroupCombo.getValue().getId());
        form.setJobDesc(jobDescField.getText() != null ? jobDescField.getText().trim() : "");
        form.setAuthor(authorField.getText() != null ? authorField.getText().trim() : "");
        form.setAlarmEmail(alarmEmailField.getText() != null ? alarmEmailField.getText().trim() : "");
        NewJobDialog.ScheduleType st = scheduleTypeCombo.getValue();
        form.setScheduleType(st != null ? st.getType() : "CRON");
        form.setScheduleConf(scheduleConfField.getText() != null ? scheduleConfField.getText().trim() : "0 0 0 * * ?");
        form.setMisfireStrategy(misfireStrategyCombo.getValue() != null
                ? misfireStrategyCombo.getValue().getType() : "DO_NOTHING");
        form.setExecutorRouteStrategy(routeStrategyCombo.getValue() != null
                ? routeStrategyCombo.getValue().getType() : "FIRST");
        form.setFailStrategy("JOB_FAIL");
        form.setExecutorBlockStrategy(blockStrategyCombo.getValue() != null
                ? blockStrategyCombo.getValue().getType() : "SERIAL_EXECUTION");
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

    private void generateJson() {
        if (loading.getAndSet(true)) return;
        new Thread(() -> {
            try {
                JobJdbcDatasource readerDs = readerDatasourceCombo.getValue();
                JobJdbcDatasource writerDs = writerDatasourceCombo.getValue();
                if (readerDs == null || writerDs == null) {
                    Platform.runLater(() -> showError("错误", "请选择数据源"));
                    return;
                }

                List<String> readerCols = getSelectedReaderColumns();
                List<String> writerCols = getSelectedWriterColumns();

                // 获取选中的表
                DataxTable selectedReaderTable = getSelectedReaderTable();
                DataxTable selectedWriterTable = getSelectedWriterTable();
                if (selectedReaderTable == null || selectedWriterTable == null) {
                    Platform.runLater(() -> showError("错误", "请选择数据表"));
                    return;
                }

                // 构建Reader参数
                JobDataxService.DataXParams readerParams = new JobDataxService.DataXParams();
                readerParams.setColumns(readerCols);
                readerParams.setSourceType(readerDs.getDatasource());
                readerParams.setUsername(readerDs.getJdbcUsername());
                readerParams.setPassword(readerDs.getJdbcPassword());
                readerParams.setDbName(readerDs.getDatabaseName());
                readerParams.setTableName(selectedReaderTable.getTableName());
                readerParams.setSchemaName(selectedReaderTable.getTableSchema());
                String[] readerIpPort = parseIpPort(readerDs.getJdbcUrl());
                readerParams.setIp(readerIpPort[0]);
                readerParams.setPort(readerIpPort[1]);
                readerParams.setQuerySql(readerSqlArea.getText());
                readerParams.setType(0);
                
                // 设置增量类型和内容
                int incrType = "全量".equals(incrTypeCombo.getValue()) ? 0 : 1;
                readerParams.setIncrementType(incrType);
                if (incrType == 1) {
                    // 构建增量内容
                    String incrementContent = buildIncrementContent();
                    readerParams.setIncrementContent(incrementContent);
                }

                // 构建Writer参数
                JobDataxService.DataXParams writerParams = new JobDataxService.DataXParams();
                writerParams.setColumns(writerCols);
                writerParams.setSourceType(writerDs.getDatasource());
                writerParams.setUsername(writerDs.getJdbcUsername());
                writerParams.setPassword(writerDs.getJdbcPassword());
                writerParams.setDbName(writerDs.getDatabaseName());
                writerParams.setTableName(selectedWriterTable.getTableName());
                writerParams.setSchemaName(selectedWriterTable.getTableSchema());
                String[] writerIpPort = parseIpPort(writerDs.getJdbcUrl());
                writerParams.setIp(writerIpPort[0]);
                writerParams.setPort(writerIpPort[1]);
                writerParams.setType(1);
                writerParams.setWriteMode(writeModeCombo.getValue());

                String readerJson = dataxService.getJson(readerParams);
                String writerJson = dataxService.getJson(writerParams);

                String finalJson = buildFinalJson(readerJson, writerJson);
                Platform.runLater(() -> jsonResultArea.setText(formatJson(finalJson)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("生成失败", e.getMessage()));
            } finally {
                loading.set(false);
            }
        }).start();
    }

    private String buildFinalJson(String readerJson, String writerJson) {
        return "{\"job\":{\"content\":[{\"reader\":" + readerJson + ",\"writer\":" + writerJson + 
               "}],\"setting\":{\"speed\":{\"channel\":3,\"byte\":-1},\"errorLimit\":{\"record\":0,\"percentage\":0.02}}}}";
    }

    private String formatJson(String json) {
        // 简单格式化
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Object obj = gson.fromJson(json, Object.class);
            return gson.toJson(obj);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 打开 JSON 放大编辑弹窗：大号 CodeArea、语法高亮、确定时回写主界面
     */
    private void showJsonExpandDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("DataX JSON 配置 - 放大编辑");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(true);
        codeArea.setWrapText(false);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("log-code-area");
        codeArea.getStyleClass().add("json-editor-area");
        codeArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 13px;");
        codeArea.replaceText(jsonResultArea.getText());

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

        codeArea.textProperty().addListener((obs, oldVal, newVal) -> applyJsonHighlighting(codeArea));

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setStyle(com.cc.job.gui.util.StyleUtil.dialogScrollPaneBackgroundStyle());
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

        applyJsonHighlighting(codeArea);

        dialog.showAndWait().ifPresent(result -> {
            if (result != null) jsonResultArea.setText(result);
        });
    }

    /**
     * 为 CodeArea 应用 JSON 语法高亮（key/string/number/boolean/null/括号）
     */
    private void applyJsonHighlighting(CodeArea codeArea) {
        String text = codeArea.getText();
        if (text == null || text.isEmpty()) return;
        try {
            // 按顺序匹配：双引号字符串、数字、true/false、null、键名（引号后跟冒号前的部分在上一段已高亮为 string）
            // 这里用简单正则覆盖整段文本，避免遗漏
            // 分组: 1=字符串 2=数字 3=指数部分 4=true|false 5=null 6=括号/冒号/逗号
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
        if (readerDsTypeCombo != null) readerDsTypeCombo.getSelectionModel().clearSelection();
        if (readerDatasourceCombo != null) readerDatasourceCombo.getItems().clear();
        if (readerTableRadioBox != null) readerTableRadioBox.getChildren().clear();
        if (readerColumnCheckBox != null) readerColumnCheckBox.getChildren().clear();
        if (readerSqlArea != null) readerSqlArea.clear();
        if (incrTypeCombo != null) {
            incrTypeCombo.getSelectionModel().selectFirst();
            updateIncrConfigVisibility();
        }
        if (incrColumnField != null) incrColumnField.clear();
        if (incrInitValueField != null) incrInitValueField.clear();
        readerTables.clear();
        readerColumns.clear();
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
                .filter(ds -> type.equals(ds.getDatasource()))
                .toList();
        readerDatasourceCombo.setItems(FXCollections.observableArrayList(filtered));
    }

    private void filterWriterDatasources() {
        String type = writerDsTypeCombo.getValue();
        if (type == null) return;
        List<JobJdbcDatasource> filtered = allDatasources.stream()
                .filter(ds -> type.equals(ds.getDatasource()))
                .toList();
        writerDatasourceCombo.setItems(FXCollections.observableArrayList(filtered));
    }

    private void loadReaderTables() {
        JobJdbcDatasource ds = readerDatasourceCombo.getValue();
        if (ds == null) {
            // 重置为默认状态
            showTablePlaceholder(readerTableRadioBox, "请选择数据源");
            return;
        }
        new Thread(() -> {
            try {
                List<DataxTable> tables = dataxService.getTables(ds.getId());
                readerTables = tables;
                Platform.runLater(() -> {
                    if (tables.isEmpty()) {
                        // 显示无内容提示，但保持边框样式
                        showTablePlaceholder(readerTableRadioBox, "表中无内容");
                    } else {
                        // 有数据时显示边框和多列布局
                        showTableContent(readerTableRadioBox, tables, readerTableToggleGroup, 
                                table -> loadReaderColumns());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("加载表失败", e.getMessage());
                    showTablePlaceholder(readerTableRadioBox, "加载失败，请重试");
                });
            }
        }).start();
    }

    private void loadWriterTables() {
        JobJdbcDatasource ds = writerDatasourceCombo.getValue();
        if (ds == null) {
            // 重置为默认状态
            showTablePlaceholder(writerTableRadioBox, "请选择数据源");
            return;
        }
        new Thread(() -> {
            try {
                List<DataxTable> tables = dataxService.getTables(ds.getId());
                writerTables = tables;
                Platform.runLater(() -> {
                    if (tables.isEmpty()) {
                        // 显示无内容提示，但保持边框样式
                        showTablePlaceholder(writerTableRadioBox, "表中无内容");
                    } else {
                        // 有数据时显示边框和多列布局
                        showTableContent(writerTableRadioBox, tables, writerTableToggleGroup, 
                                table -> loadWriterColumns());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("加载表失败", e.getMessage());
                    showTablePlaceholder(writerTableRadioBox, "加载失败，请重试");
                });
            }
        }).start();
    }

    private void loadReaderColumns() {
        JobJdbcDatasource ds = readerDatasourceCombo.getValue();
        if (ds == null) {
            showColumnPlaceholder(readerColumnCheckBox, "请选择数据源");
            return;
        }
        DataxTable selectedTable = getSelectedReaderTable();
        if (selectedTable == null) {
            showColumnPlaceholder(readerColumnCheckBox, "请选择数据表");
            return;
        }
        String table = selectedTable.getTableName();
        String schema = selectedTable.getTableSchema();
        String sql = readerSqlArea.getText();
        new Thread(() -> {
            try {
                List<String> columns = dataxService.getColumns(ds.getId(), table, schema, sql);
                readerColumns = columns;
                Platform.runLater(() -> {
                    if (columns.isEmpty()) {
                        // 显示无内容提示，但保持边框样式
                        showColumnPlaceholder(readerColumnCheckBox, "无可用字段");
                    } else {
                        // 有数据时显示边框和多列布局
                        showColumnContent(readerColumnCheckBox, columns);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("加载字段失败", e.getMessage());
                    showColumnPlaceholder(readerColumnCheckBox, "加载失败，请重试");
                });
            }
        }).start();
    }

    private void loadWriterColumns() {
        JobJdbcDatasource ds = writerDatasourceCombo.getValue();
        if (ds == null) {
            showColumnPlaceholder(writerColumnCheckBox, "请选择数据源");
            return;
        }
        DataxTable selectedTable = getSelectedWriterTable();
        if (selectedTable == null) {
            showColumnPlaceholder(writerColumnCheckBox, "请选择数据表");
            return;
        }
        String table = selectedTable.getTableName();
        String schema = selectedTable.getTableSchema();
        String sql = writerSqlArea.getText();
        new Thread(() -> {
            try {
                List<String> columns = dataxService.getColumns(ds.getId(), table, schema, sql);
                writerColumns = columns;
                Platform.runLater(() -> {
                    if (columns.isEmpty()) {
                        // 显示无内容提示，但保持边框样式
                        showColumnPlaceholder(writerColumnCheckBox, "无可用字段");
                    } else {
                        // 有数据时显示边框和多列布局
                        showColumnContent(writerColumnCheckBox, columns);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("加载字段失败", e.getMessage());
                    showColumnPlaceholder(writerColumnCheckBox, "加载失败，请重试");
                });
            }
        }).start();
    }
    
    /** 遍历表字段容器中所有 CheckBox（支持 VBox 下直接 CheckBox 或 HBox 行内的 CheckBox） */
    private void forEachColumnCheckBox(HBox container, java.util.function.Consumer<CheckBox> action) {
        if (container == null) return;
        container.getChildren().forEach(columnNode -> {
            if (columnNode instanceof VBox) {
                ((VBox) columnNode).getChildren().forEach(node -> {
                    if (node instanceof CheckBox) {
                        action.accept((CheckBox) node);
                    } else if (node instanceof HBox) {
                        ((HBox) node).getChildren().forEach(n -> {
                            if (n instanceof CheckBox) action.accept((CheckBox) n);
                        });
                    }
                });
            }
        });
    }

    // 辅助方法：获取选中的Reader表
    private DataxTable getSelectedReaderTable() {
        if (readerTableToggleGroup.getSelectedToggle() != null) {
            return (DataxTable) readerTableToggleGroup.getSelectedToggle().getUserData();
        }
        return null;
    }
    
    // 辅助方法：获取选中的Writer表
    private DataxTable getSelectedWriterTable() {
        if (writerTableToggleGroup.getSelectedToggle() != null) {
            return (DataxTable) writerTableToggleGroup.getSelectedToggle().getUserData();
        }
        return null;
    }
    
    // 辅助方法：获取选中的Reader字段
    private List<String> getSelectedReaderColumns() {
        List<String> selected = new ArrayList<>();
        if (readerColumnCheckBox != null) {
            forEachColumnCheckBox(readerColumnCheckBox, cb -> {
                if (cb.isSelected() && cb.getUserData() != null) {
                    selected.add(cb.getUserData().toString());
                }
            });
        }
        return selected;
    }
    
    // 辅助方法：获取选中的Writer字段
    private List<String> getSelectedWriterColumns() {
        List<String> selected = new ArrayList<>();
        if (writerColumnCheckBox != null) {
            forEachColumnCheckBox(writerColumnCheckBox, cb -> {
                if (cb.isSelected() && cb.getUserData() != null) {
                    selected.add(cb.getUserData().toString());
                }
            });
        }
        return selected;
    }
    
    // 更新增量配置可见性
    private void updateIncrConfigVisibility() {
        boolean isIncremental = "增量".equals(incrTypeCombo.getValue());
        if (incrConfigBox != null) {
            incrConfigBox.setVisible(isIncremental);
            incrConfigBox.setManaged(isIncremental);
        }
        if (isIncremental) {
            updateIncrModeConfig();
        }
    }
    
    // 更新增量模式配置
    private void updateIncrModeConfig() {
        if (incrModeCombo == null || incrConfigBox == null) return;
        boolean isTimeMode = "时间自增".equals(incrModeCombo.getValue());
        // 找到时间格式行（incrConfigBox的最后一个子节点）
        if (incrConfigBox.getChildren().size() > 3) {
            Node timeFormatRow = incrConfigBox.getChildren().get(3);
            timeFormatRow.setVisible(isTimeMode);
            timeFormatRow.setManaged(isTimeMode);
        }
    }
    
    // 显示数据表占位符
    private void showTablePlaceholder(HBox container, String message) {
        container.getChildren().clear();
        container.setPadding(new Insets(12, 16, 12, 16));
        // 根据消息内容决定是否显示边框
        boolean showBorder = !message.equals("请选择数据源");
        if (showBorder) {
            container.getStyleClass().add("dialog-section");
            container.setStyle("-fx-background-radius: " + StyleUtil.RADIUS_MD + "; -fx-border-width: 1; " +
                    "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
            container.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            container.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            if (container.getParent() instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) container.getParent();
                scrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
                scrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            }
        } else {
            container.setStyle("-fx-background-color: transparent;");
            container.setMinHeight(50);
            container.setPrefHeight(50);
            if (container.getParent() instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) container.getParent();
                scrollPane.setMinHeight(50);
                scrollPane.setPrefHeight(50);
            }
        }
        Label placeholder = new Label(message);
        placeholder.setStyle(StyleUtil.bodyFontOnly());
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(placeholder, Priority.ALWAYS);
        container.getChildren().add(placeholder);
    }
    
    // 显示数据表内容（有边框）
    private void showTableContent(HBox container, List<DataxTable> items, ToggleGroup toggleGroup,
                                  java.util.function.Consumer<DataxTable> onSelect) {
        container.getChildren().clear();
        container.setPadding(new Insets(12, 16, 12, 16));
        container.getStyleClass().add("dialog-section");
        container.setStyle("-fx-background-radius: " + StyleUtil.RADIUS_MD + "; -fx-border-width: 1; " +
                "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
        container.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        container.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        // 更新ScrollPane高度
        if (container.getParent() instanceof ScrollPane) {
            ScrollPane scrollPane = (ScrollPane) container.getParent();
            scrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            scrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        }
        createMultiColumnRadioButtons(container, items, toggleGroup, onSelect);
    }
    
    // 显示表字段占位符
    private void showColumnPlaceholder(HBox container, String message) {
        container.getChildren().clear();
        container.setPadding(new Insets(12, 16, 12, 16));
        // 根据消息内容决定是否显示边框
        boolean showBorder = !message.equals("请选择数据源") && !message.equals("请选择数据表");
        if (showBorder) {
            container.getStyleClass().add("dialog-section");
            container.setStyle("-fx-background-radius: " + StyleUtil.RADIUS_MD + "; -fx-border-width: 1; " +
                    "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
            container.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            container.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            if (container.getParent() instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) container.getParent();
                scrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
                scrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            }
        } else {
            container.setStyle("-fx-background-color: transparent;");
            container.setMinHeight(50);
            container.setPrefHeight(50);
            if (container.getParent() instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) container.getParent();
                scrollPane.setMinHeight(50);
                scrollPane.setPrefHeight(50);
            }
        }
        Label placeholder = new Label(message);
        placeholder.setStyle(StyleUtil.bodyFontOnly());
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(placeholder, Priority.ALWAYS);
        container.getChildren().add(placeholder);
    }
    
    // 显示表字段内容（有边框）
    private void showColumnContent(HBox container, List<String> items) {
        container.getChildren().clear();
        container.setPadding(new Insets(12, 16, 12, 16));
        container.getStyleClass().add("dialog-section");
        container.setStyle("-fx-background-radius: " + StyleUtil.RADIUS_MD + "; -fx-border-width: 1; " +
                "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
        container.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        container.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        // 更新ScrollPane高度
        if (container.getParent() instanceof ScrollPane) {
            ScrollPane scrollPane = (ScrollPane) container.getParent();
            scrollPane.setMinHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            scrollPane.setPrefHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
        }
        createMultiColumnCheckBoxes(container, items);
    }
    
    // 创建多列RadioButton布局（每列最多6条）
    private void createMultiColumnRadioButtons(HBox container, List<DataxTable> items, 
                                               ToggleGroup toggleGroup, 
                                               java.util.function.Consumer<DataxTable> onSelect) {
        container.getChildren().clear();
        if (items == null || items.isEmpty()) return;
        final int ITEMS_PER_COLUMN = 6;
        int totalColumns = (items.size() + ITEMS_PER_COLUMN - 1) / ITEMS_PER_COLUMN; // 向上取整
        
        for (int colIndex = 0; colIndex < totalColumns; colIndex++) {
            VBox column = new VBox(6);
            column.setMinWidth(TABLE_AND_COLUMN_COLUMN_WIDTH);
            column.setPrefWidth(TABLE_AND_COLUMN_COLUMN_WIDTH);
            column.setMaxHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            
            int startIndex = colIndex * ITEMS_PER_COLUMN;
            int endIndex = Math.min(startIndex + ITEMS_PER_COLUMN, items.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                DataxTable table = items.get(i);
                String schema = table.getTableSchema() != null ? table.getTableSchema() : "";
                String name = table.getTableName() != null ? table.getTableName() : "";
                String display = (schema.isEmpty() ? "" : schema + ".") + name;
                if (display.isEmpty()) display = "未命名";
                RadioButton radio = new RadioButton();
                radio.setToggleGroup(toggleGroup);
                radio.setUserData(table);
                radio.setStyle("-fx-background-color: transparent;");
                radio.setOnAction(e -> {
                    if (onSelect != null) onSelect.accept(table);
                });
                Label tableLabel = new Label(display);
                tableLabel.setWrapText(true);
                tableLabel.setMinWidth(120);
                tableLabel.setMaxWidth(Double.MAX_VALUE);
                String textFill = StyleUtil.isDarkTheme() ? "#FFFFFF" : "#1F2937";
                tableLabel.setStyle(StyleUtil.bodyFontOnly() + " -fx-background-color: transparent; -fx-text-fill: " + textFill + ";");
                tableLabel.setTooltip(new Tooltip(display));
                HBox.setHgrow(tableLabel, Priority.ALWAYS);
                HBox row = new HBox(8, radio, tableLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMaxWidth(Double.MAX_VALUE);
                row.setOnMouseClicked(e -> {
                    radio.setSelected(true);
                    if (onSelect != null) onSelect.accept(table);
                });
                column.getChildren().add(row);
            }
            
            container.getChildren().add(column);
        }
    }
    
    // 创建多列CheckBox布局（每列最多6条）
    private void createMultiColumnCheckBoxes(HBox container, List<String> items) {
        container.getChildren().clear();
        if (items == null || items.isEmpty()) return;
        
        final int ITEMS_PER_COLUMN = 6;
        int totalColumns = (items.size() + ITEMS_PER_COLUMN - 1) / ITEMS_PER_COLUMN; // 向上取整
        
        for (int colIndex = 0; colIndex < totalColumns; colIndex++) {
            VBox column = new VBox(6);
            column.setMinWidth(TABLE_AND_COLUMN_COLUMN_WIDTH);
            column.setPrefWidth(TABLE_AND_COLUMN_COLUMN_WIDTH);
            column.setMaxHeight(TABLE_AND_COLUMN_AREA_HEIGHT);
            
            int startIndex = colIndex * ITEMS_PER_COLUMN;
            int endIndex = Math.min(startIndex + ITEMS_PER_COLUMN, items.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                String columnName = items.get(i) != null ? items.get(i) : "";
                CheckBox checkBox = new CheckBox();
                checkBox.setUserData(columnName);
                checkBox.setStyle("-fx-background-color: transparent;");
                Label fieldLabel = new Label(columnName);
                fieldLabel.setWrapText(true);
                fieldLabel.setMinWidth(120);
                fieldLabel.setMaxWidth(Double.MAX_VALUE);
                String textFillCol = StyleUtil.isDarkTheme() ? "#FFFFFF" : "#1F2937";
                fieldLabel.setStyle(StyleUtil.bodyFontOnly() + " -fx-background-color: transparent; -fx-text-fill: " + textFillCol + ";");
                HBox.setHgrow(fieldLabel, Priority.ALWAYS);
                HBox row = new HBox(8, checkBox, fieldLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMaxWidth(Double.MAX_VALUE);
                column.getChildren().add(row);
            }
            
            container.getChildren().add(column);
        }
    }
    
    // 构建增量内容JSON
    private String buildIncrementContent() {
        String columnKey = incrColumnField.getText().trim();
        String columnValue = incrInitValueField.getText().trim();
        String mode = incrModeCombo.getValue();
        
        // columnType: 0=ID自增, 1=时间自增
        int columnType = "时间自增".equals(mode) ? 1 : 0;
        String timeFormat = columnType == 1 && incrTimeFormatCombo.getValue() != null 
                ? incrTimeFormatCombo.getValue() : "x";
        
        // 构建DataxColumn对象
        com.google.gson.JsonObject columnObj = new com.google.gson.JsonObject();
        columnObj.addProperty("columnKey", columnKey);
        columnObj.addProperty("columnValue", columnValue);
        columnObj.addProperty("columnParam", columnKey);  // 参数名通常与字段名相同
        columnObj.addProperty("columnTimeFormat", timeFormat);
        columnObj.addProperty("columnType", columnType);
        
        com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
        jsonArray.add(columnObj);
        
        return new com.google.gson.Gson().toJson(jsonArray);
    }

    private void copyToClipboard(String text) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
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
