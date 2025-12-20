package com.cc.job.gui.view;

import com.cc.job.gui.service.JobDataxService;
import com.cc.job.gui.service.JobJdbcDatasourceService;
import com.cc.job.gui.util.StyleUtil;
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

    // Step 1 - Reader配置
    private ComboBox<String> readerDsTypeCombo;
    private ComboBox<JobJdbcDatasource> readerDatasourceCombo;
    private ListView<String> readerTableList;
    private TextArea readerSqlArea;
    private ListView<String> readerColumnList;
    private ComboBox<String> incrTypeCombo;

    // Step 2 - Writer配置
    private ComboBox<String> writerDsTypeCombo;
    private ComboBox<JobJdbcDatasource> writerDatasourceCombo;
    private ListView<String> writerTableList;
    private TextArea writerSqlArea;
    private ListView<String> writerColumnList;
    private ComboBox<String> writeModeCombo;

    // Step 3 - 结果
    private TextArea jsonResultArea;

    // 所有数据源
    private List<JobJdbcDatasource> allDatasources = new ArrayList<>();

    private static final String[] DS_TYPES = {"MYSQL", "ORACLE", "POSTGRESQL"};
    private static final String[] WRITE_MODES = {"insert", "update", "replace"};

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
        getDialogPane().setStyle(
                "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_LG + ";"
        );
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                try {
                    String css = getClass().getResource("/styles.css").toExternalForm();
                    stage.getScene().getStylesheets().add(css);
                } catch (Exception ignored) {}
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
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(16));

        String labelStyle = StyleUtil.body();

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

        // 表列表
        Label tableLabel = new Label("数据表");
        tableLabel.setStyle(labelStyle);
        readerTableList = new ListView<>();
        readerTableList.setPrefHeight(120);
        readerTableList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        readerTableList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) loadReaderColumns();
        });

        // SQL
        Label sqlLabel = new Label("SQL (可选)");
        sqlLabel.setStyle(labelStyle);
        readerSqlArea = new TextArea();
        readerSqlArea.setPrefRowCount(3);
        readerSqlArea.setPromptText("使用多表查询时，columns可留空");

        Button parseSqlBtn = new Button("SQL解析");
        parseSqlBtn.setStyle(StyleUtil.successButton());
        parseSqlBtn.setOnAction(e -> loadReaderColumns());

        // 字段列表
        Label columnLabel = new Label("表字段");
        columnLabel.setStyle(labelStyle);
        readerColumnList = new ListView<>();
        readerColumnList.setPrefHeight(120);
        readerColumnList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button selectAllBtn = new Button("全选");
        selectAllBtn.setOnAction(e -> readerColumnList.getSelectionModel().selectAll());

        // 增量类型
        Label incrLabel = new Label("增量备份");
        incrLabel.setStyle(labelStyle);
        incrTypeCombo = new ComboBox<>(FXCollections.observableArrayList("全量", "增量"));
        incrTypeCombo.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.add(dsTypeLabel, 0, 0);
        grid.add(readerDsTypeCombo, 1, 0);
        grid.add(dsLabel, 0, 1);
        grid.add(readerDatasourceCombo, 1, 1);
        grid.add(tableLabel, 0, 2);
        grid.add(readerTableList, 1, 2);
        grid.add(sqlLabel, 0, 3);
        HBox sqlBox = new HBox(8, readerSqlArea, parseSqlBtn);
        HBox.setHgrow(readerSqlArea, Priority.ALWAYS);
        grid.add(sqlBox, 1, 3);
        grid.add(columnLabel, 0, 4);
        VBox colBox = new VBox(4, readerColumnList, selectAllBtn);
        grid.add(colBox, 1, 4);
        grid.add(incrLabel, 0, 5);
        grid.add(incrTypeCombo, 1, 5);

        pane.getChildren().add(grid);
        return new ScrollPane(pane);
    }

    private Node createWriterPane() {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(16));

        String labelStyle = StyleUtil.body();

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

        // 表列表
        Label tableLabel = new Label("数据表");
        tableLabel.setStyle(labelStyle);
        writerTableList = new ListView<>();
        writerTableList.setPrefHeight(120);
        writerTableList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        writerTableList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) loadWriterColumns();
        });

        // SQL
        Label sqlLabel = new Label("SQL (可选)");
        sqlLabel.setStyle(labelStyle);
        writerSqlArea = new TextArea();
        writerSqlArea.setPrefRowCount(3);

        Button parseSqlBtn = new Button("SQL解析");
        parseSqlBtn.setStyle(StyleUtil.successButton());
        parseSqlBtn.setOnAction(e -> loadWriterColumns());

        // 字段列表
        Label columnLabel = new Label("表字段");
        columnLabel.setStyle(labelStyle);
        writerColumnList = new ListView<>();
        writerColumnList.setPrefHeight(120);
        writerColumnList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button selectAllBtn = new Button("全选");
        selectAllBtn.setOnAction(e -> writerColumnList.getSelectionModel().selectAll());

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
        grid.add(writerTableList, 1, 2);
        grid.add(sqlLabel, 0, 3);
        HBox sqlBox = new HBox(8, writerSqlArea, parseSqlBtn);
        HBox.setHgrow(writerSqlArea, Priority.ALWAYS);
        grid.add(sqlBox, 1, 3);
        grid.add(columnLabel, 0, 4);
        VBox colBox = new VBox(4, writerColumnList, selectAllBtn);
        grid.add(colBox, 1, 4);
        grid.add(modeLabel, 0, 5);
        grid.add(writeModeCombo, 1, 5);

        pane.getChildren().add(grid);
        return new ScrollPane(pane);
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
            List<String> selectedCols = new ArrayList<>(readerColumnList.getSelectionModel().getSelectedItems());
            if (selectedCols.isEmpty() && isBlank(readerSqlArea.getText())) {
                showError("验证失败", "请选择要同步的数据列或输入SQL");
                return;
            }
            currentStep++;
            updateStepView();
        } else if (currentStep == 1) {
            // 验证Writer配置
            List<String> selectedCols = new ArrayList<>(writerColumnList.getSelectionModel().getSelectedItems());
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

                List<String> readerCols = new ArrayList<>(readerColumnList.getSelectionModel().getSelectedItems());
                List<String> writerCols = new ArrayList<>(writerColumnList.getSelectionModel().getSelectedItems());

                // 构建Reader参数
                JobDataxService.DataXParams readerParams = new JobDataxService.DataXParams();
                readerParams.setColumns(readerCols);
                readerParams.setSourceType(readerDs.getDatasource());
                readerParams.setUsername(readerDs.getJdbcUsername());
                readerParams.setPassword(readerDs.getJdbcPassword());
                readerParams.setDbName(readerDs.getDatabaseName());
                readerParams.setTableName(readerTableList.getSelectionModel().getSelectedItem());
                String[] readerIpPort = parseIpPort(readerDs.getJdbcUrl());
                readerParams.setIp(readerIpPort[0]);
                readerParams.setPort(readerIpPort[1]);
                readerParams.setQuerySql(readerSqlArea.getText());
                readerParams.setType(0);
                readerParams.setIncrType("全量".equals(incrTypeCombo.getValue()) ? 0 : 1);

                // 构建Writer参数
                JobDataxService.DataXParams writerParams = new JobDataxService.DataXParams();
                writerParams.setColumns(writerCols);
                writerParams.setSourceType(writerDs.getDatasource());
                writerParams.setUsername(writerDs.getJdbcUsername());
                writerParams.setPassword(writerDs.getJdbcPassword());
                writerParams.setDbName(writerDs.getDatabaseName());
                writerParams.setTableName(writerTableList.getSelectionModel().getSelectedItem());
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
        if (readerTableList != null) readerTableList.getItems().clear();
        if (readerColumnList != null) readerColumnList.getItems().clear();
        if (readerSqlArea != null) readerSqlArea.clear();
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
        if (ds == null) return;
        new Thread(() -> {
            try {
                List<String> tables = dataxService.getTables(ds.getId());
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
                List<String> tables = dataxService.getTables(ds.getId());
                Platform.runLater(() -> writerTableList.setItems(FXCollections.observableArrayList(tables)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("加载表失败", e.getMessage()));
            }
        }).start();
    }

    private void loadReaderColumns() {
        JobJdbcDatasource ds = readerDatasourceCombo.getValue();
        if (ds == null) return;
        String table = readerTableList.getSelectionModel().getSelectedItem();
        String sql = readerSqlArea.getText();
        new Thread(() -> {
            try {
                List<String> columns = dataxService.getColumns(ds.getId(), table, sql);
                Platform.runLater(() -> readerColumnList.setItems(FXCollections.observableArrayList(columns)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("加载字段失败", e.getMessage()));
            }
        }).start();
    }

    private void loadWriterColumns() {
        JobJdbcDatasource ds = writerDatasourceCombo.getValue();
        if (ds == null) return;
        String table = writerTableList.getSelectionModel().getSelectedItem();
        String sql = writerSqlArea.getText();
        new Thread(() -> {
            try {
                List<String> columns = dataxService.getColumns(ds.getId(), table, sql);
                Platform.runLater(() -> writerColumnList.setItems(FXCollections.observableArrayList(columns)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("加载字段失败", e.getMessage()));
            }
        }).start();
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
