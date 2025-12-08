package com.cc.job.gui.view;

import com.cc.job.gui.service.JobGroupService;
import com.cc.job.gui.service.JobLogService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 展示任务日志列表
 */
public class ShowJobLogListDialog extends Dialog<Void> {

    private final JobLogService jobLogService = new JobLogService();
    private final JobGroupService jobGroupService = new JobGroupService();

    private TableView<JobLogVO> tableView;
    private ComboBox<JobGroup> jobGroupCombo;
    private ComboBox<String> logStatusCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    private Label totalLabel;
    private TextField pageField;
    private ComboBox<Integer> pageSizeBox;
    private Button prevBtn;
    private Button nextBtn;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int pageNum = 1;
    private int pageSize = 10;
    private long total = 0;

    public ShowJobLogListDialog(Stage ownerStage) {
        setTitle("任务日志");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + StyleUtil.BG_SECONDARY + ";");

        root.setTop(createFilterBar());
        root.setCenter(createTable());
        root.setBottom(createPagerBar());

        getDialogPane().setContent(root);

        // 加载执行器列表
        loadJobGroups();
        // 初始加载
        loadPage(true);
    }

    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        getDialogPane().setPrefSize(1400, 800);
        getDialogPane().setPrefWidth(1400);
        getDialogPane().setPrefHeight(800);
        getDialogPane().setMinWidth(1400);
        getDialogPane().setMinHeight(800);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);

        getDialogPane().setStyle(
                "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                        "-fx-background-radius: " + StyleUtil.RADIUS_LG + "; " +
                        "-fx-border-radius: " + StyleUtil.RADIUS_LG + ";"
        );

        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(1400);
                stage.setMinHeight(800);
                
                try {
                    String css = getClass().getResource("/styles.css").toExternalForm();
                    stage.getScene().getStylesheets().add(css);
                } catch (Exception e) {
                    // CSS文件加载失败，忽略
                }
                
                stage.setOnCloseRequest(event -> close());
            }
        });
    }

    private void loadJobGroups() {
        new Thread(() -> {
            try {
                List<JobGroup> groups = jobGroupService.getAllJobGroupList();
                Platform.runLater(() -> {
                    jobGroupCombo.getItems().clear();
                    jobGroupCombo.getItems().add(null); // 全部选项
                    jobGroupCombo.getItems().addAll(groups);
                    jobGroupCombo.getSelectionModel().selectFirst();
                });
            } catch (Exception ex) {
                logger.error("加载执行器列表失败", ex);
            }
        }, "load-job-groups").start();
    }

    private Node createFilterBar() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 16, 16, 16));
        grid.setStyle(
                "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_LG + "; " +
                "-fx-effect: " + StyleUtil.SHADOW_SM + ";"
        );

        String labelStyle = StyleUtil.body();

        // 执行器下拉框
        jobGroupCombo = new ComboBox<>();
        jobGroupCombo.setCellFactory(listView -> new ListCell<JobGroup>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("全部");
                } else {
                    setText(item.getTitle());
                }
            }
        });
        jobGroupCombo.setButtonCell(new ListCell<JobGroup>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("全部");
                } else {
                    setText(item.getTitle());
                }
            }
        });

        // 任务状态下拉框
        logStatusCombo = new ComboBox<>();
        logStatusCombo.getItems().addAll("全部", "成功", "失败", "进行中");
        logStatusCombo.getSelectionModel().selectFirst();

        // 调度时间范围
        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();

        Button searchBtn = new Button("搜索");
        searchBtn.setStyle(StyleUtil.primaryButton());
        StyleUtil.applyPrimaryButtonHover(searchBtn);
        searchBtn.setOnAction(e -> {
            pageNum = 1;
            loadPage(true);
        });

        Button resetBtn = new Button("重置");
        resetBtn.setStyle(StyleUtil.secondaryButton());
        resetBtn.setOnAction(e -> {
            jobGroupCombo.getSelectionModel().selectFirst();
            logStatusCombo.getSelectionModel().selectFirst();
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            pageNum = 1;
            loadPage(true);
        });

        Button clearBtn = new Button("清理");
        clearBtn.setStyle(StyleUtil.errorButton());
        StyleUtil.applyErrorButtonHover(clearBtn);
        clearBtn.setOnAction(e -> handleClear());

        Label executorLabel = new Label("执行器");
        executorLabel.setStyle(labelStyle);
        Label statusLabel = new Label("任务状态");
        statusLabel.setStyle(labelStyle);
        Label timeLabel = new Label("调度时间");
        timeLabel.setStyle(labelStyle);

        grid.add(executorLabel, 0, 0);
        grid.add(jobGroupCombo, 1, 0);
        grid.add(statusLabel, 2, 0);
        grid.add(logStatusCombo, 3, 0);
        grid.add(timeLabel, 4, 0);
        grid.add(startDatePicker, 5, 0);
        grid.add(new Label("至"), 6, 0);
        grid.add(endDatePicker, 7, 0);

        HBox btnBox = new HBox(10, searchBtn, resetBtn, clearBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(btnBox, 8, 0);

        return grid;
    }

    @SuppressWarnings("unchecked")
    private Node createTable() {
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // 序号列
        TableColumn<JobLogVO, Number> idxCol = new TableColumn<>("序号");
        idxCol.setCellValueFactory(c -> Bindings.createIntegerBinding(
                () -> tableView.getItems().indexOf(c.getValue()) + 1 + (pageNum - 1) * pageSize));
        idxCol.setMaxWidth(80);

        // 任务id列
        TableColumn<JobLogVO, String> jobIdCol = new TableColumn<>("任务id");
        jobIdCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getJobId() != null ? String.valueOf(c.getValue().getJobId()) : ""));

        // 任务名称列
        TableColumn<JobLogVO, String> jobDescCol = new TableColumn<>("任务名称");
        jobDescCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getJobDesc())));

        // 调度时间列
        TableColumn<JobLogVO, String> triggerTimeCol = new TableColumn<>("调度时间");
        triggerTimeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getTriggerTime())));

        // 调度结果列
        TableColumn<JobLogVO, String> triggerCodeCol = new TableColumn<>("调度结果");
        triggerCodeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                mapTriggerCode(c.getValue().getTriggerCode())));

        // 调度备注列
        TableColumn<JobLogVO, String> triggerMsgCol = new TableColumn<>("调度备注");
        triggerMsgCol.setCellValueFactory(c -> {
            String msg = safe(c.getValue().getTriggerMsg());
            return new javafx.beans.property.SimpleStringProperty(msg != null && !msg.isEmpty() ? "查看" : "");
        });
        triggerMsgCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("查看");
            {
                viewLink.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                viewLink.setOnAction(e -> {
                    JobLogVO item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        handleViewTriggerMsg(item);
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    setText(empty ? null : "");
                } else {
                    setGraphic(viewLink);
                    setText(null);
                }
            }
        });

        // 执行时间列
        TableColumn<JobLogVO, String> handleTimeCol = new TableColumn<>("执行时间");
        handleTimeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getHandleTime())));

        // 执行结果列
        TableColumn<JobLogVO, String> handleCodeCol = new TableColumn<>("执行结果");
        handleCodeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                mapHandleCode(c.getValue().getHandleCode())));

        // 操作列
        TableColumn<JobLogVO, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink logLink = new Hyperlink("执行日志");
            {
                logLink.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                logLink.setOnAction(e -> {
                    JobLogVO item = getTableView().getItems().get(getIndex());
                    if (item != null && item.getId() != null) {
                        handleViewLog(item);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(logLink);
                }
            }
        });
        actionCol.setPrefWidth(100);
        actionCol.setMinWidth(100);

        tableView.getColumns().addAll(idxCol, jobIdCol, jobDescCol, triggerTimeCol, 
                triggerCodeCol, triggerMsgCol, handleTimeCol, handleCodeCol, actionCol);

        return tableView;
    }

    private Node createPagerBar() {
        totalLabel = new Label("共 0 条");
        totalLabel.setStyle(StyleUtil.body());

        prevBtn = new Button("上一页");
        prevBtn.setStyle(StyleUtil.secondaryButton());
        nextBtn = new Button("下一页");
        nextBtn.setStyle(StyleUtil.secondaryButton());
        
        pageField = new TextField(String.valueOf(pageNum));
        pageField.setPrefWidth(60);
        pageField.setStyle(StyleUtil.searchField());
        pageField.setOnAction(e -> {
            try {
                int p = Integer.parseInt(pageField.getText().trim());
                pageNum = Math.max(p, 1);
                loadPage(false);
            } catch (NumberFormatException ex) {
                pageField.setText(String.valueOf(pageNum));
            }
        });

        pageSizeBox = new ComboBox<>(FXCollections.observableArrayList(10, 20, 50));
        pageSizeBox.getSelectionModel().select(Integer.valueOf(pageSize));
        pageSizeBox.setOnAction(e -> {
            Integer size = pageSizeBox.getValue();
            if (size != null) {
                pageSize = size;
                pageNum = 1;
                loadPage(false);
            }
        });

        prevBtn.setOnAction(e -> {
            if (pageNum > 1) {
                pageNum--;
                loadPage(false);
            }
        });
        nextBtn.setOnAction(e -> {
            long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
            if (pageNum < maxPage) {
                pageNum++;
                loadPage(false);
            }
        });

        Label pageSizeLabel = new Label("每页");
        pageSizeLabel.setStyle(StyleUtil.body());
        Label pageLabel = new Label("页码");
        pageLabel.setStyle(StyleUtil.body());

        HBox pager = new HBox(12,
                totalLabel,
                pageSizeLabel, pageSizeBox,
                prevBtn,
                pageLabel, pageField,
                nextBtn
        );
        pager.setAlignment(Pos.CENTER_LEFT);
        pager.setPadding(new Insets(16, 16, 16, 16));
        pager.setStyle(
                "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_LG + "; " +
                "-fx-effect: " + StyleUtil.SHADOW_SM + ";"
        );
        return pager;
    }

    private void loadPage(boolean showLoading) {
        if (loading.getAndSet(true)) {
            return;
        }
        if (showLoading) {
            totalLabel.setText("加载中...");
        }

        JobLogQuery query = new JobLogQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        JobGroup selectedGroup = jobGroupCombo.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            query.setJobGroup(selectedGroup.getId());
        }
        
        String status = logStatusCombo.getValue();
        if ("成功".equals(status)) {
            query.setLogStatus(1);
        } else if ("失败".equals(status)) {
            query.setLogStatus(2);
        } else if ("进行中".equals(status)) {
            query.setLogStatus(3);
        }
        
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            String[] filterTime = new String[]{
                    start.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    end.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            };
            query.setFilterTime(filterTime);
        }

        new Thread(() -> {
            try {
                PageResult<JobLogVO> page = jobLogService.getJobLogPage(query);
                List<JobLogVO> list = page != null && page.getData() != null
                        ? page.getData().getList() : Collections.emptyList();
                total = page != null && page.getData() != null ? page.getData().getTotal() : 0;
                Platform.runLater(() -> {
                    tableView.setItems(FXCollections.observableArrayList(list));
                    totalLabel.setText("共 " + total + " 条，当前页 " + pageNum);
                    pageField.setText(String.valueOf(pageNum));
                    updatePagerButtons();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    totalLabel.setText("加载失败: " + ex.getMessage());
                    showError("加载日志列表失败", ex.getMessage());
                });
            } finally {
                loading.set(false);
            }
        }, "load-job-log-list").start();
    }

    private void updatePagerButtons() {
        long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
        prevBtn.setDisable(pageNum <= 1);
        nextBtn.setDisable(pageNum >= maxPage || maxPage == 0);
    }

    private void handleViewTriggerMsg(JobLogVO item) {
        if (item == null) return;
        
        String triggerMsg = safe(item.getTriggerMsg());
        if (triggerMsg == null || triggerMsg.isEmpty()) {
            showInfo("暂无调度备注");
            return;
        }
        
        // 显示调度备注对话框
        showTriggerMsgDialog(item, triggerMsg);
    }

    private void showTriggerMsgDialog(JobLogVO item, String triggerMsg) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("调度备注");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(700);
        content.setPrefHeight(500);

        // 使用TextArea显示调度备注内容，使用等宽字体
        TextArea textArea = new TextArea(triggerMsg);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(450);

        content.getChildren().add(scrollPane);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleViewLog(JobLogVO item) {
        if (item == null || item.getId() == null) return;
        
        // 显示执行日志对话框
        showExecutionLogDialog(item);
    }

    private void showExecutionLogDialog(JobLogVO item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("执行日志 - " + safe(item.getJobDesc()));
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(800);
        content.setPrefHeight(600);

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        ScrollPane scrollPane = new ScrollPane(logArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(550);

        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle(StyleUtil.secondaryButton());
        refreshBtn.setOnAction(e -> loadLogContent(item.getId(), logArea, 0));

        HBox buttonBox = new HBox(10, refreshBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(buttonBox, scrollPane);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // 初始加载日志
        loadLogContent(item.getId(), logArea, 0);
        
        dialog.showAndWait();
    }

    private void loadLogContent(Long logId, TextArea logArea, int fromLineNum) {
        logArea.setText("加载中...");
        new Thread(() -> {
            try {
                JobLogService.LogDetailResponse response = jobLogService.getLogDetail(logId, fromLineNum);
                if (response.isSuccess() && response.getContent() != null) {
                    String existingText = logArea.getText();
                    if ("加载中...".equals(existingText)) {
                        existingText = "";
                    }
                    String newContent = response.getContent().getLogContent();
                    String finalText = existingText + (existingText.isEmpty() ? "" : "\n") + newContent;
                    
                    Platform.runLater(() -> {
                        logArea.setText(finalText);
                        // 滚动到底部
                        logArea.positionCaret(finalText.length());
                    });
                    
                    // 如果还有更多日志，继续加载
                    if (!response.getContent().isEnd()) {
                        loadLogContent(logId, logArea, response.getContent().getToLineNum());
                    }
                } else {
                    Platform.runLater(() -> logArea.setText("加载失败: " + (response.getMsg() != null ? response.getMsg() : "未知错误")));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> logArea.setText("加载失败: " + ex.getMessage()));
            }
        }, "load-log-content").start();
    }

    private void handleClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "确定要清理日志吗？此操作将根据当前筛选条件删除日志。", 
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("确认清理");
        confirm.initOwner(getDialogPane().getScene().getWindow());
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.YES) {
                runAsync("清理日志", () -> {
                    JobLogQuery query = new JobLogQuery();
                    JobGroup selectedGroup = jobGroupCombo.getSelectionModel().getSelectedItem();
                    if (selectedGroup != null) {
                        query.setJobGroup(selectedGroup.getId());
                    }
                    String status = logStatusCombo.getValue();
                    if ("成功".equals(status)) {
                        query.setLogStatus(1);
                    } else if ("失败".equals(status)) {
                        query.setLogStatus(2);
                    } else if ("进行中".equals(status)) {
                        query.setLogStatus(3);
                    }
                    if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                        LocalDate start = startDatePicker.getValue();
                        LocalDate end = endDatePicker.getValue();
                        String[] filterTime = new String[]{
                                start.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                end.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        };
                        query.setFilterTime(filterTime);
                    }
                    boolean ok = jobLogService.deleteJobLogs(query);
                    return ok ? "清理成功" : "清理失败";
                });
            }
        });
    }

    private void runAsync(String title, java.util.concurrent.Callable<String> task) {
        if (loading.getAndSet(true)) {
            return;
        }
        totalLabel.setText(title + "...");
        new Thread(() -> {
            try {
                String msg = task.call();
                Platform.runLater(() -> {
                    showInfo(msg);
                    loading.set(false);
                    loadPage(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError(title + "失败", ex.getMessage());
                    loading.set(false);
                });
            }
        }, "job-log-action").start();
    }

    private String mapTriggerCode(Integer code) {
        if (code == null) return "";
        return code == 200 ? "成功" : "失败";
    }

    private String mapHandleCode(Integer code) {
        if (code == null) return "";
        return code == 200 ? "成功" : (code == 500 ? "失败" : "进行中");
    }

    private String safe(String v) {
        return v == null ? "" : v;
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
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ShowJobLogListDialog.class);
}

