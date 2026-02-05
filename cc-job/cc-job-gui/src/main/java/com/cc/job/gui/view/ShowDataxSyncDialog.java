package com.cc.job.gui.view;

import com.cc.job.gui.service.JobDataxService;
import com.cc.job.gui.service.JobJdbcDatasourceService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.model.datax.DataxTable;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
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
    
    // 存储表数据，用于单选
    private List<DataxTable> readerTables = new ArrayList<>();
    private List<DataxTable> writerTables = new ArrayList<>();
    // 存储字段数据，用于多选
    private List<String> readerColumns = new ArrayList<>();
    private List<String> writerColumns = new ArrayList<>();

    public ShowDataxSyncDialog(Stage ownerStage) {
        setTitle("数据源同步");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        VBox root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + StyleUtil.BG_SECONDARY + ";");

        root.getChildren().addAll(createStepIndicator(), createContentPane(), createButtonBar());

        getDialogPane().setContent(root);

        // 加载数据源
        loadDatasources();
        updateStepView();
    }

    private void styleDialog() {
        getDialogPane().setPrefSize(1000, 750);
        getDialogPane().setMinWidth(900);
        getDialogPane().setMinHeight(700);
        setResizable(true);
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
        steps.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + "; -fx-background-radius: 8;");

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
            label.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + StyleUtil.PRIMARY + ";");
        } else {
            label.setStyle("-fx-font-size: 14; -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
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
        contentPane.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + "; -fx-background-radius: 8;");
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        return contentPane;
    }

    private HBox createButtonBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(16));

        prevBtn = new Button("上一步");
        prevBtn.setStyle(StyleUtil.secondaryButton());
        prevBtn.setOnAction(e -> {
            if (currentStep > 0) {
                currentStep--;
                updateStepView();
            }
        });

        nextBtn = new Button("下一步");
        nextBtn.setStyle(StyleUtil.primaryButton());
        StyleUtil.applyPrimaryButtonHover(nextBtn);
        nextBtn.setOnAction(e -> handleNext());

        Button resetBtn = new Button("重置");
        resetBtn.setStyle(StyleUtil.secondaryButton());
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
        nextBtn.setText(currentStep == 2 ? "复制JSON" : "下一步");

        switch (currentStep) {
            case 0 -> contentPane.getChildren().add(createReaderPane());
            case 1 -> contentPane.getChildren().add(createWriterPane());
            case 2 -> contentPane.getChildren().add(createResultPane());
        }
    }

    private Node createReaderPane() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + ";");
        HBox.setHgrow(pane, Priority.ALWAYS);

        String labelStyle = StyleUtil.body() + " -fx-min-width: 100;";

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

        // 数据表 - 单选框（多列布局，每列8条）
        Label tableLabel = new Label("数据表");
        tableLabel.setStyle(labelStyle);
        readerTableToggleGroup = new ToggleGroup();
        HBox tableColumnsContainer = new HBox(12);
        tableColumnsContainer.setPadding(new Insets(12, 16, 12, 16));
        // 默认状态：无边框，只显示提示文字
        tableColumnsContainer.setStyle("-fx-background-color: transparent;");
        tableColumnsContainer.setMinHeight(50);
        // 默认显示提示信息
        Label tablePlaceholder = new Label("请选择数据源");
        tablePlaceholder.setStyle(StyleUtil.body() + " -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
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
        tableScrollPane.setMinHeight(50); // 默认较小高度
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
        parseSqlBtn.setStyle(StyleUtil.successButton());
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
        selectAllBtn.setStyle(StyleUtil.secondaryButton());
        selectAllBtn.setPrefWidth(80);
        selectAllBtn.setOnAction(e -> {
            readerColumnCheckBox.getChildren().forEach(columnNode -> {
                if (columnNode instanceof VBox) {
                    ((VBox) columnNode).getChildren().forEach(node -> {
                        if (node instanceof CheckBox) {
                            ((CheckBox) node).setSelected(true);
                        }
                    });
                }
            });
        });
        Button clearAllBtn = new Button("清空");
        clearAllBtn.setStyle(StyleUtil.secondaryButton());
        clearAllBtn.setPrefWidth(80);
        clearAllBtn.setOnAction(e -> {
            readerColumnCheckBox.getChildren().forEach(columnNode -> {
                if (columnNode instanceof VBox) {
                    ((VBox) columnNode).getChildren().forEach(node -> {
                        if (node instanceof CheckBox) {
                            ((CheckBox) node).setSelected(false);
                        }
                    });
                }
            });
        });
        columnHeader.getChildren().addAll(selectAllBtn, clearAllBtn);
        
        // 表字段多列容器（每列8条）
        HBox columnColumnsContainer = new HBox(12);
        columnColumnsContainer.setPadding(new Insets(12, 16, 12, 16));
        // 默认状态：无边框，只显示提示文字
        columnColumnsContainer.setStyle("-fx-background-color: transparent;");
        columnColumnsContainer.setMinHeight(50);
        // 默认显示提示信息
        Label columnPlaceholder = new Label("请选择数据表");
        columnPlaceholder.setStyle(StyleUtil.body() + " -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
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
        readerColumnScrollPane.setMinHeight(50); // 默认较小高度
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
        incrConfigBox.setStyle("-fx-background-color: " + StyleUtil.BG_SECONDARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_MD + "; " +
                "-fx-border-color: " + StyleUtil.GRAY_300 + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
        incrConfigBox.setVisible(false);
        
        // 增量模式
        HBox incrModeRow = new HBox(12);
        incrModeRow.setAlignment(Pos.CENTER_LEFT);
        Label incrModeLabel = new Label("增量模式");
        incrModeLabel.setStyle(StyleUtil.body() + " -fx-min-width: 80;");
        incrModeCombo = new ComboBox<>(FXCollections.observableArrayList(INCR_MODES));
        incrModeCombo.getSelectionModel().selectFirst();
        HBox.setHgrow(incrModeCombo, Priority.ALWAYS);
        incrModeCombo.setOnAction(e -> updateIncrModeConfig());
        incrModeRow.getChildren().addAll(incrModeLabel, incrModeCombo);
        
        // 增量字段
        HBox incrColumnRow = new HBox(12);
        incrColumnRow.setAlignment(Pos.CENTER_LEFT);
        Label incrColumnLabel = new Label("增量字段");
        incrColumnLabel.setStyle(StyleUtil.body() + " -fx-min-width: 80;");
        incrColumnField = new TextField();
        incrColumnField.setPromptText("请输入字段名，如：id 或 create_time");
        HBox.setHgrow(incrColumnField, Priority.ALWAYS);
        incrColumnRow.getChildren().addAll(incrColumnLabel, incrColumnField);
        
        // 初始值
        HBox incrValueRow = new HBox(12);
        incrValueRow.setAlignment(Pos.CENTER_LEFT);
        Label incrValueLabel = new Label("初始值");
        incrValueLabel.setStyle(StyleUtil.body() + " -fx-min-width: 80;");
        incrInitValueField = new TextField();
        incrInitValueField.setPromptText("ID自增请输入数字，时间自增请输入时间戳(毫秒)");
        HBox.setHgrow(incrInitValueField, Priority.ALWAYS);
        incrValueRow.getChildren().addAll(incrValueLabel, incrInitValueField);
        
        // 时间格式（仅时间自增时显示）
        HBox incrTimeFormatRow = new HBox(12);
        incrTimeFormatRow.setAlignment(Pos.CENTER_LEFT);
        Label incrTimeFormatLabel = new Label("时间格式");
        incrTimeFormatLabel.setStyle(StyleUtil.body() + " -fx-min-width: 80;");
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
        pane.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + ";");
        HBox.setHgrow(pane, Priority.ALWAYS);

        String labelStyle = StyleUtil.body() + " -fx-min-width: 100;";

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

        // 数据表 - 单选框（多列布局，每列8条）
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
        tablePlaceholder.setStyle(StyleUtil.body() + " -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
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
        tableScrollPane.setMinHeight(50); // 默认较小高度
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
        parseSqlBtn.setStyle(StyleUtil.successButton());
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
        selectAllBtn.setStyle(StyleUtil.secondaryButton());
        selectAllBtn.setPrefWidth(80);
        selectAllBtn.setOnAction(e -> {
            writerColumnCheckBox.getChildren().forEach(columnNode -> {
                if (columnNode instanceof VBox) {
                    ((VBox) columnNode).getChildren().forEach(node -> {
                        if (node instanceof CheckBox) {
                            ((CheckBox) node).setSelected(true);
                        }
                    });
                }
            });
        });
        Button clearAllBtn = new Button("清空");
        clearAllBtn.setStyle(StyleUtil.secondaryButton());
        clearAllBtn.setPrefWidth(80);
        clearAllBtn.setOnAction(e -> {
            writerColumnCheckBox.getChildren().forEach(columnNode -> {
                if (columnNode instanceof VBox) {
                    ((VBox) columnNode).getChildren().forEach(node -> {
                        if (node instanceof CheckBox) {
                            ((CheckBox) node).setSelected(false);
                        }
                    });
                }
            });
        });
        columnHeader.getChildren().addAll(selectAllBtn, clearAllBtn);
        
        // 表字段多列容器（每列8条）
        HBox columnColumnsContainer = new HBox(12);
        columnColumnsContainer.setPadding(new Insets(12, 16, 12, 16));
        // 默认状态：无边框，只显示提示文字
        columnColumnsContainer.setStyle("-fx-background-color: transparent;");
        columnColumnsContainer.setMinHeight(50);
        // 默认显示提示信息
        Label columnPlaceholder = new Label("请选择数据表");
        columnPlaceholder.setStyle(StyleUtil.body() + " -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
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
        writerColumnScrollPane.setMinHeight(50); // 默认较小高度
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
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(16));

        Label label = new Label("生成的DataX JSON配置");
        label.setStyle(StyleUtil.body() + "-fx-font-weight: bold;");

        jsonResultArea = new TextArea();
        jsonResultArea.setEditable(false);
        jsonResultArea.setWrapText(true);
        jsonResultArea.setPrefRowCount(20);
        jsonResultArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace;");

        VBox.setVgrow(jsonResultArea, Priority.ALWAYS);
        pane.getChildren().addAll(label, jsonResultArea);
        return pane;
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
            // 复制JSON
            copyToClipboard(jsonResultArea.getText());
            showInfo("已复制到剪贴板");
        }
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
                readerParams.setIncrType(incrType);
                if (incrType == 1) {
                    // 构建增量内容
                    String incrementContent = buildIncrementContent();
                    readerParams.setIncrContent(incrementContent);
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
            readerColumnCheckBox.getChildren().forEach(columnNode -> {
                if (columnNode instanceof VBox) {
                    ((VBox) columnNode).getChildren().forEach(node -> {
                        if (node instanceof CheckBox) {
                            CheckBox cb = (CheckBox) node;
                            if (cb.isSelected()) {
                                selected.add(cb.getText());
                            }
                        }
                    });
                }
            });
        }
        return selected;
    }
    
    // 辅助方法：获取选中的Writer字段
    private List<String> getSelectedWriterColumns() {
        List<String> selected = new ArrayList<>();
        if (writerColumnCheckBox != null) {
            writerColumnCheckBox.getChildren().forEach(columnNode -> {
                if (columnNode instanceof VBox) {
                    ((VBox) columnNode).getChildren().forEach(node -> {
                        if (node instanceof CheckBox) {
                            CheckBox cb = (CheckBox) node;
                            if (cb.isSelected()) {
                                selected.add(cb.getText());
                            }
                        }
                    });
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
            container.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                    "-fx-background-radius: " + StyleUtil.RADIUS_MD + "; " +
                    "-fx-border-color: " + StyleUtil.GRAY_300 + "; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
            container.setMinHeight(220);
            container.setPrefHeight(220);
            if (container.getParent() instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) container.getParent();
                scrollPane.setMinHeight(220);
                scrollPane.setPrefHeight(220);
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
        placeholder.setStyle(StyleUtil.body() + " -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
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
        container.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_MD + "; " +
                "-fx-border-color: " + StyleUtil.GRAY_300 + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
        container.setMinHeight(220);
        container.setPrefHeight(220);
        // 更新ScrollPane高度
        if (container.getParent() instanceof ScrollPane) {
            ScrollPane scrollPane = (ScrollPane) container.getParent();
            scrollPane.setMinHeight(220);
            scrollPane.setPrefHeight(220);
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
            container.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                    "-fx-background-radius: " + StyleUtil.RADIUS_MD + "; " +
                    "-fx-border-color: " + StyleUtil.GRAY_300 + "; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
            container.setMinHeight(220);
            container.setPrefHeight(220);
            if (container.getParent() instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) container.getParent();
                scrollPane.setMinHeight(220);
                scrollPane.setPrefHeight(220);
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
        placeholder.setStyle(StyleUtil.body() + " -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(placeholder, Priority.ALWAYS);
        container.getChildren().add(placeholder);
    }
    
    // 显示表字段内容（有边框）
    private void showColumnContent(HBox container, List<String> items) {
        container.getChildren().clear();
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_MD + "; " +
                "-fx-border-color: " + StyleUtil.GRAY_300 + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: " + StyleUtil.RADIUS_MD + ";");
        container.setMinHeight(220);
        container.setPrefHeight(220);
        // 更新ScrollPane高度
        if (container.getParent() instanceof ScrollPane) {
            ScrollPane scrollPane = (ScrollPane) container.getParent();
            scrollPane.setMinHeight(220);
            scrollPane.setPrefHeight(220);
        }
        createMultiColumnCheckBoxes(container, items);
    }
    
    // 创建多列RadioButton布局（每列最多8条）
    private void createMultiColumnRadioButtons(HBox container, List<DataxTable> items, 
                                               ToggleGroup toggleGroup, 
                                               java.util.function.Consumer<DataxTable> onSelect) {
        container.getChildren().clear();
        if (items == null || items.isEmpty()) return;
        
        final int ITEMS_PER_COLUMN = 8;
        int totalColumns = (items.size() + ITEMS_PER_COLUMN - 1) / ITEMS_PER_COLUMN; // 向上取整
        
        for (int colIndex = 0; colIndex < totalColumns; colIndex++) {
            VBox column = new VBox(6);
            column.setMinWidth(180); // 每列最小宽度
            column.setPrefWidth(180);
            
            int startIndex = colIndex * ITEMS_PER_COLUMN;
            int endIndex = Math.min(startIndex + ITEMS_PER_COLUMN, items.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                DataxTable table = items.get(i);
                RadioButton radio = new RadioButton(
                        (table.getTableSchema() != null && !table.getTableSchema().isEmpty() 
                                ? table.getTableSchema() + "." : "") + table.getTableName());
                radio.setToggleGroup(toggleGroup);
                radio.setUserData(table);
                radio.setStyle(StyleUtil.body() + " -fx-background-color: transparent;"); // 移除背景颜色
                radio.setOnAction(e -> {
                    if (onSelect != null) {
                        onSelect.accept(table);
                    }
                });
                column.getChildren().add(radio);
            }
            
            container.getChildren().add(column);
        }
    }
    
    // 创建多列CheckBox布局（每列最多8条）
    private void createMultiColumnCheckBoxes(HBox container, List<String> items) {
        container.getChildren().clear();
        if (items == null || items.isEmpty()) return;
        
        final int ITEMS_PER_COLUMN = 8;
        int totalColumns = (items.size() + ITEMS_PER_COLUMN - 1) / ITEMS_PER_COLUMN; // 向上取整
        
        for (int colIndex = 0; colIndex < totalColumns; colIndex++) {
            VBox column = new VBox(6);
            column.setMinWidth(180); // 每列最小宽度
            column.setPrefWidth(180);
            
            int startIndex = colIndex * ITEMS_PER_COLUMN;
            int endIndex = Math.min(startIndex + ITEMS_PER_COLUMN, items.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                String columnName = items.get(i);
                CheckBox checkBox = new CheckBox(columnName);
                checkBox.setStyle(StyleUtil.body() + " -fx-background-color: transparent;"); // 移除背景颜色
                column.getChildren().add(checkBox);
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
