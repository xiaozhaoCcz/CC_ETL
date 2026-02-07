package com.cc.job.gui.view;

import com.cc.job.gui.service.JobDataxService;
import com.cc.job.gui.service.JobJdbcDatasourceService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
        nextBtn.setText(currentStep == 2 ? "复制JSON" : "下一步");

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

        // 增量类型
        Label incrLabel = new Label("增量备份");
        incrLabel.setStyle(labelStyle);
        incrTypeCombo = new ComboBox<>(FXCollections.observableArrayList("全量", "增量"));
        incrTypeCombo.getSelectionModel().selectFirst();
        incrTypeCombo.setOnAction(e -> updateIncrConfigVisibility());

        // 增量配置容器
        incrConfigBox = new VBox(8);
        incrConfigBox.setPadding(new Insets(12));
        incrConfigBox.getStyleClass().add("dialog-section");
        incrConfigBox.setStyle("-fx-background-radius: 8; -fx-border-width: 1; -fx-border-radius: 8;");
        incrConfigBox.setVisible(false);

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
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(16));

        Label label = new Label("生成的批量DataX JSON配置");
        label.setStyle(StyleUtil.bodyFontOnly() + "-fx-font-weight: bold;");

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
            List<String> selectedTables = new ArrayList<>(readerTableList.getSelectionModel().getSelectedItems());
            if (selectedTables.isEmpty()) {
                showError("验证失败", "请选择要读取的数据表");
                return;
            }
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
            List<String> selectedTables = new ArrayList<>(writerTableList.getSelectionModel().getSelectedItems());
            if (selectedTables.isEmpty()) {
                showError("验证失败", "请选择要写入的数据表");
                return;
            }
            generateBatchJson();
            currentStep++;
            updateStepView();
        } else {
            copyToClipboard(jsonResultArea.getText());
            showInfo("已复制到剪贴板");
        }
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
                    int incrType = "全量".equals(incrTypeCombo.getValue()) ? 0 : 1;
                    readerParams.setIncrementType(incrType);
                    if (incrType == 1) {
                        readerParams.setIncrementContent(buildIncrementContent());
                    }

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
//                List<String> tables = dataxService.getTables(ds.getId());
                List<String> tables = List.of();
                Platform.runLater(() -> readerTableList.setItems(FXCollections.observableArrayList(tables)));
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
//                List<String> tables = dataxService.getTables(ds.getId());
                List<String> tables = List.of();
                Platform.runLater(() -> writerTableList.setItems(FXCollections.observableArrayList(tables)));
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
