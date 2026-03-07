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
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import org.apache.commons.text.StringEscapeUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.layout.FlowPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 展示任务日志列表
 */
public class ShowJobLogListDialog extends Dialog<Void> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobLogService jobLogService = new JobLogService();
    private final JobGroupService jobGroupService = new JobGroupService();

    private TableView<JobLogVO> tableView;
    private ComboBox<JobGroup> jobGroupCombo;
    private ComboBox<String> logStatusCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private TextField keywordField;

    private Label totalLabel;
    private TextField pageField;
    private ComboBox<Integer> pageSizeBox;
    private Button prevBtn;
    private Button nextBtn;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int pageNum = 1;
    private int pageSize = 10;
    private long total = 0;

    // 任务ID，用于过滤特定任务的日志
    private Long jobId;

    public ShowJobLogListDialog(Stage ownerStage) {
        this(ownerStage, null);
    }

    public ShowJobLogListDialog(Stage ownerStage, Long jobId) {
        this.jobId = jobId;
        setTitle("任务日志");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

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

        String dialogCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            getDialogPane().getStylesheets().add(dialogCss);
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(1400);
                stage.setMinHeight(800);

                String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                if (css != null && !css.isEmpty()) {
                    stage.getScene().getStylesheets().add(css);
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
        grid.getStyleClass().add("dialog-section");
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 16, 16, 16));

        String labelStyle = StyleUtil.bodyFontOnly();

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

        keywordField = new TextField();
        keywordField.setPromptText("执行结果关键词");
        keywordField.setPrefWidth(120);

        Button searchBtn = new Button("搜索");
        searchBtn.getStyleClass().add("dialog-button-primary");
        searchBtn.setOnAction(e -> {
            pageNum = 1;
            loadPage(true);
        });

        Button resetBtn = new Button("重置");
        resetBtn.getStyleClass().add("dialog-button-secondary");
        resetBtn.setOnAction(e -> {
            jobGroupCombo.getSelectionModel().selectFirst();
            logStatusCombo.getSelectionModel().selectFirst();
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            if (keywordField != null) keywordField.clear();
            pageNum = 1;
            loadPage(true);
        });

        Button clearBtn = new Button("清理");
        clearBtn.getStyleClass().add("dialog-button-error");
        clearBtn.setOnAction(e -> handleClear());

        Button archiveBtn = new Button("归档");
        archiveBtn.getStyleClass().add("dialog-button-secondary");
        archiveBtn.setOnAction(e -> handleArchive());

        Label executorLabel = new Label("执行器");
        executorLabel.setStyle(labelStyle);
        Label keywordLabel = new Label("关键词");
        keywordLabel.setStyle(labelStyle);
        Label statusLabel = new Label("任务状态");
        statusLabel.setStyle(labelStyle);
        Label timeLabel = new Label("调度时间");
        timeLabel.setStyle(labelStyle);

        grid.add(executorLabel, 0, 0);
        grid.add(jobGroupCombo, 1, 0);
        grid.add(keywordLabel, 2, 0);
        grid.add(keywordField, 3, 0);
        grid.add(statusLabel, 4, 0);
        grid.add(logStatusCombo, 5, 0);
        grid.add(timeLabel, 6, 0);
        grid.add(startDatePicker, 7, 0);
        grid.add(new Label("至"), 8, 0);
        grid.add(endDatePicker, 9, 0);

        HBox btnBox = new HBox(10, searchBtn, resetBtn, clearBtn, archiveBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(btnBox, 10, 0);

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
        jobIdCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getJobId() != null ? String.valueOf(c.getValue().getJobId()) : ""));

        // 任务名称列
        TableColumn<JobLogVO, String> jobDescCol = new TableColumn<>("任务名称");
        jobDescCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getJobDesc())));

        // 调度时间列
        TableColumn<JobLogVO, String> triggerTimeCol = new TableColumn<>("调度时间");
        triggerTimeCol.setCellValueFactory(c -> new SimpleStringProperty(formatTime(c.getValue().getTriggerTime())));

        // 调度结果列
        TableColumn<JobLogVO, String> triggerCodeCol = new TableColumn<>("调度结果");
        triggerCodeCol.setCellValueFactory(c -> new SimpleStringProperty(
                mapTriggerCode(c.getValue().getTriggerCode())));

        // 调度备注列
        TableColumn<JobLogVO, String> triggerMsgCol = new TableColumn<>("调度备注");
        triggerMsgCol.setCellValueFactory(c -> {
            String msg = safe(c.getValue().getTriggerMsg());
            return new SimpleStringProperty(msg != null && !msg.isEmpty() ? "查看" : "");
        });
        triggerMsgCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("查看");
            {
                viewLink.setStyle("-fx-text-fill: " + StyleUtil.linkPrimaryColor() + ";");
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
        handleTimeCol.setCellValueFactory(c -> new SimpleStringProperty(formatTime(c.getValue().getHandleTime())));

        // 执行结果列
        TableColumn<JobLogVO, String> handleCodeCol = new TableColumn<>("执行结果");
        handleCodeCol.setCellValueFactory(c -> new SimpleStringProperty(
                mapHandleCode(c.getValue().getHandleCode())));

        // 节点状态列（只有任务组任务才显示）
        TableColumn<JobLogVO, String> nodeStatusCol = new TableColumn<>("节点状态");
        nodeStatusCol.setCellValueFactory(c -> {
            JobLogVO item = c.getValue();
            // 只有任务组任务（jobType == 2）且有 nodeStatus 时才显示
            if (item.getJobType() != null && item.getJobType() == 2
                    && item.getNodeStatus() != null && !item.getNodeStatus().trim().isEmpty()) {
                return new SimpleStringProperty("查看");
            }
            return new SimpleStringProperty("");
        });
        nodeStatusCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("查看");
            {
                viewLink.setStyle("-fx-text-fill: " + StyleUtil.linkPrimaryColor() + ";");
                viewLink.setOnAction(e -> {
                    JobLogVO item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        handleViewNodeStatus(item);
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
        nodeStatusCol.setPrefWidth(100);
        nodeStatusCol.setMinWidth(100);

        // 操作列
        TableColumn<JobLogVO, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink logLink = new Hyperlink("执行日志");
            {
                logLink.setStyle("-fx-text-fill: " + StyleUtil.linkPrimaryColor() + ";");
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
                triggerCodeCol, triggerMsgCol, handleTimeCol, handleCodeCol, nodeStatusCol, actionCol);

        return tableView;
    }

    private Node createPagerBar() {
        totalLabel = new Label("共 0 条");
        totalLabel.setStyle(StyleUtil.bodyFontOnly());

        prevBtn = new Button("上一页");
        prevBtn.getStyleClass().add("dialog-button-secondary");
        nextBtn = new Button("下一页");
        nextBtn.getStyleClass().add("dialog-button-secondary");
        
        pageField = new TextField(String.valueOf(pageNum));
        pageField.setPrefWidth(60);
        pageField.getStyleClass().add("dialog-search-field");
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
        pageSizeLabel.setStyle(StyleUtil.bodyFontOnly());
        Label pageLabel = new Label("页码");
        pageLabel.setStyle(StyleUtil.bodyFontOnly());

        HBox pager = new HBox(12,
                totalLabel,
                pageSizeLabel, pageSizeBox,
                prevBtn,
                pageLabel, pageField,
                nextBtn
        );
        pager.getStyleClass().add("dialog-section");
        pager.setAlignment(Pos.CENTER_LEFT);
        pager.setPadding(new Insets(16, 16, 16, 16));
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
        
        // 如果指定了任务ID，则只查询该任务的日志
        if (jobId != null) {
            query.setJobId(jobId);
        }
        
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
        if (keywordField != null && keywordField.getText() != null && !keywordField.getText().trim().isEmpty()) {
            query.setKeyword(keywordField.getText().trim());
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

    private void handleViewNodeStatus(JobLogVO item) {
        if (item == null) return;
        
        String nodeStatusJson = item.getNodeStatus();
        if (nodeStatusJson == null || nodeStatusJson.trim().isEmpty()) {
            showInfo("暂无节点状态记录");
            return;
        }
        
        // 显示节点状态对话框
        showNodeStatusDialog(item, nodeStatusJson);
    }

    private void showNodeStatusDialog(JobLogVO item, String nodeStatusJson) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("节点执行状态");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(900);
        content.setPrefHeight(600);

        // 标题信息
        Label titleLabel = new Label("任务组: " + safe(item.getJobDesc()));
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");
        
        Label timeLabel = new Label("执行时间: " + formatTime(item.getHandleTime()));
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleUtil.textSecondaryColor() + ";");

        // 节点状态网格
        FlowPane nodeStatusGrid = createNodeStatusGrid(nodeStatusJson);

        ScrollPane scrollPane = new ScrollPane(nodeStatusGrid);
        scrollPane.getStyleClass().add("dialog-section");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(500);

        content.getChildren().addAll(titleLabel, timeLabel, scrollPane);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * 创建节点状态网格
     */
    private FlowPane createNodeStatusGrid(String nodeStatusJson) {
        FlowPane grid = new FlowPane(12, 12);
        grid.setPrefWrapLength(850);
        grid.setPadding(new Insets(10));
        
        if (nodeStatusJson == null || nodeStatusJson.trim().isEmpty()) {
            Label emptyLabel = new Label("暂无节点状态记录");
            emptyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleUtil.textSecondaryColor() + ";");
            grid.getChildren().add(emptyLabel);
            return grid;
        }
        
        try {
            JsonObject nodeStatusObj = JsonParser.parseString(nodeStatusJson).getAsJsonObject();
            
            for (Map.Entry<String, com.google.gson.JsonElement> entry : nodeStatusObj.entrySet()) {
                String nodeId = entry.getKey();
                JsonObject nodeInfo = entry.getValue().getAsJsonObject();
                
                String nodeName = nodeInfo.has("jobDesc") ? 
                    nodeInfo.get("jobDesc").getAsString() : ("节点 " + nodeId);
                int status = nodeInfo.has("status") ? 
                    nodeInfo.get("status").getAsInt() : -1;
                
                VBox nodeCard = createNodeCard(nodeName, status);
                grid.getChildren().add(nodeCard);
            }
        } catch (Exception e) {
            logger.error("解析节点状态JSON失败", e);
            Label errorLabel = new Label("解析节点状态失败: " + e.getMessage());
            errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444;");
            grid.getChildren().add(errorLabel);
        }
        
        return grid;
    }
    
    /**
     * 创建单个节点卡片
     */
    private VBox createNodeCard(String nodeName, int status) {
        VBox nodeCard = new VBox(8);
        nodeCard.setPadding(new Insets(12));
        nodeCard.setPrefWidth(180);
        if (StyleUtil.isDarkTheme()) {
            nodeCard.setStyle(
                "-fx-background-color: #2D2D30; " +
                "-fx-border-color: #3C3C3C; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6;"
            );
        } else {
            nodeCard.setStyle(
                "-fx-background-color: #F9FAFB; " +
                "-fx-border-color: #E5E7EB; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6;"
            );
        }
        
        Label nameLabel = new Label(nodeName);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");
        nameLabel.setWrapText(true);
        
        Label statusLabel = createStatusLabel(status);
        
        nodeCard.getChildren().addAll(nameLabel, statusLabel);
        return nodeCard;
    }
    
    /**
     * 创建状态标签
     */
    private Label createStatusLabel(int status) {
        Label label = new Label();
        if (status == 1) {
            label.setText("成功");
            label.setStyle(
                "-fx-background-color: #D1FAE5; " +
                "-fx-text-fill: #065F46; " +
                "-fx-padding: 4 12 4 12; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600;"
            );
        } else if (status == 0) {
            label.setText("失败");
            label.setStyle(
                "-fx-background-color: #FEE2E2; " +
                "-fx-text-fill: #991B1B; " +
                "-fx-padding: 4 12 4 12; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600;"
            );
        } else if (status == 2) {
            label.setText("运行中");
            label.setStyle(
                "-fx-background-color: #DBEAFE; " +
                "-fx-text-fill: #1E40AF; " +
                "-fx-padding: 4 12 4 12; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600;"
            );
        } else {
            label.setText("未知");
            label.setStyle(
                "-fx-background-color: #F3F4F6; " +
                "-fx-text-fill: #6B7280; " +
                "-fx-padding: 4 12 4 12; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600;"
            );
        }
        return label;
    }

    /**
     * 执行日志对话框 - 带行号、语法高亮、自动刷新、全屏功能
     */
    private void showExecutionLogDialog(JobLogVO item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("执行日志 - " + safe(item.getJobDesc()));
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        // 状态变量
        AtomicBoolean isFullscreen = new AtomicBoolean(false);
        AtomicBoolean isAutoRefresh = new AtomicBoolean(true);
        AtomicBoolean isLogEnd = new AtomicBoolean(false);
        AtomicInteger fromLineNum = new AtomicInteger(0);
        AtomicInteger pullFailCount = new AtomicInteger(0);
        Timer[] refreshTimer = new Timer[1];

        // 创建CodeArea并配置
        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(false);
        codeArea.setWrapText(false);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setStyle(
            "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
            "-fx-font-size: 13px;"
        );

        // 添加样式类
        codeArea.getStyleClass().add("log-code-area");

        // 右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setOnAction(e -> {
            String text = codeArea.getSelectedText();
            if (text == null || text.isEmpty()) {
                text = codeArea.getText();
            }
            if (text != null && !text.isEmpty()) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
            }
        });
        MenuItem selectAllItem = new MenuItem("全选");
        selectAllItem.setOnAction(e -> codeArea.selectAll());
        contextMenu.getItems().addAll(copyItem, selectAllItem);
        codeArea.setContextMenu(contextMenu);

        // 使用VirtualizedScrollPane包装
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setStyle(StyleUtil.dialogScrollPaneBackgroundStyle());
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 工具栏
        Button refreshBtn = new Button("刷新");
        refreshBtn.getStyleClass().add("dialog-button-secondary");
        refreshBtn.setOnAction(e -> {
            fromLineNum.set(0);
            pullFailCount.set(0);
            isLogEnd.set(false);
            codeArea.clear();
            loadLogContentAsync(item.getId(), codeArea, fromLineNum, pullFailCount, isLogEnd, scrollPane);
        });

        ToggleButton autoRefreshBtn = new ToggleButton("自动刷新");
        autoRefreshBtn.setSelected(true);
        autoRefreshBtn.getStyleClass().add("dialog-button-secondary");
        autoRefreshBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isAutoRefresh.set(newVal);
            if (newVal) {
                autoRefreshBtn.setText("自动刷新中");
            } else {
                autoRefreshBtn.setText("自动刷新");
            }
        });

        Button scrollTopBtn = new Button("↑ 顶部");
        scrollTopBtn.getStyleClass().add("dialog-button-secondary");
        scrollTopBtn.setOnAction(e -> {
            codeArea.moveTo(0);
            codeArea.requestFollowCaret();
        });

        Button scrollBottomBtn = new Button("↓ 底部");
        scrollBottomBtn.getStyleClass().add("dialog-button-secondary");
        scrollBottomBtn.setOnAction(e -> {
            codeArea.moveTo(codeArea.getLength());
            codeArea.requestFollowCaret();
        });

        // 全屏按钮
        Button fullscreenBtn = new Button("全屏");
        fullscreenBtn.getStyleClass().add("dialog-button-secondary");
        fullscreenBtn.setOnAction(e -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            if (stage != null) {
                if (isFullscreen.get()) {
                    // 退出全屏
                    stage.setWidth(1000);
                    stage.setHeight(700);
                    stage.centerOnScreen();
                    fullscreenBtn.setText("全屏");
                    isFullscreen.set(false);
                } else {
                    // 进入全屏
                    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                    stage.setX(screenBounds.getMinX());
                    stage.setY(screenBounds.getMinY());
                    stage.setWidth(screenBounds.getWidth());
                    stage.setHeight(screenBounds.getHeight());
                    fullscreenBtn.setText("退出全屏");
                    isFullscreen.set(true);
                }
            }
        });

        // 状态标签
        Label statusLabel = new Label("正在加载...");
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, refreshBtn, autoRefreshBtn, scrollTopBtn, scrollBottomBtn, fullscreenBtn, spacer, statusLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(1000);
        content.setPrefHeight(700);
        content.getChildren().addAll(toolbar, scrollPane);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);

        // 添加当前主题样式
        String dialogCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            dialog.getDialogPane().getStylesheets().add(dialogCss);
        }

        // 初始加载日志
        loadLogContentAsync(item.getId(), codeArea, fromLineNum, pullFailCount, isLogEnd, scrollPane);

        // 启动自动刷新定时器（延迟2000ms后开始，避免与初始加载重复）
        refreshTimer[0] = new Timer("log-refresh-timer", true);
        refreshTimer[0].scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isAutoRefresh.get() && !isLogEnd.get()) {
                    loadLogContentAsync(item.getId(), codeArea, fromLineNum, pullFailCount, isLogEnd, scrollPane);
                }
                
                // 更新状态
                Platform.runLater(() -> {
                    if (isLogEnd.get()) {
                        statusLabel.setText("日志加载完成 [Rolling Log Finish]");
                        statusLabel.setTextFill(Color.web("#10B981"));
                    } else if (pullFailCount.get() > 20) {
                        statusLabel.setText("日志拉取超时，已停止刷新");
                        statusLabel.setTextFill(Color.web("#EF4444"));
                        isLogEnd.set(true);
                    } else {
                        statusLabel.setText("正在监听日志... 行数: " + codeArea.getParagraphs().size());
                    }
                });
            }
        }, 2000, 2000);

        // 对话框关闭时停止定时器
        dialog.setOnCloseRequest(e -> {
            if (refreshTimer[0] != null) {
                refreshTimer[0].cancel();
            }
        });

        dialog.showAndWait();

        // 确保关闭定时器
        if (refreshTimer[0] != null) {
            refreshTimer[0].cancel();
        }
    }

    /**
     * 异步加载日志内容
     */
    private void loadLogContentAsync(Long logId, CodeArea codeArea, AtomicInteger fromLineNum, 
                                      AtomicInteger pullFailCount, AtomicBoolean isLogEnd,
                                      VirtualizedScrollPane<CodeArea> scrollPane) {
        new Thread(() -> {
            try {
                JobLogService.LogDetailResponse response = jobLogService.getLogDetail(logId, fromLineNum.get());
                if (response.isSuccess() && response.getContent() != null) {
                    JobLogService.LogContent logContent = response.getContent();
                    
                    // 检查行号是否匹配
                    if (fromLineNum.get() != logContent.getFromLineNum()) {
                        logger.debug("pullLog fromLineNum not match");
                        pullFailCount.incrementAndGet();
                        return;
                    }
                    
                    // 检查是否已到达末尾
                    if (fromLineNum.get() > logContent.getToLineNum()) {
                        if (logContent.isEnd()) {
                            isLogEnd.set(true);
                        }
                        return;
                    }
                    
                    String newContent = logContent.getLogContent();
                    if (newContent != null && !newContent.isEmpty()) {
                        // 解码HTML实体
                        String decodedContent = decodeHtmlEntities(newContent);
                        
                        Platform.runLater(() -> {
                            // 追加内容
                            String currentText = codeArea.getText();
                            if (!currentText.isEmpty() && !currentText.endsWith("\n")) {
                                codeArea.appendText("\n");
                            }
                            codeArea.appendText(decodedContent);
                            
                            // 应用语法高亮
                            applyLogHighlighting(codeArea);
                            
                            // 滚动到底部
                            codeArea.moveTo(codeArea.getLength());
                            codeArea.requestFollowCaret();
                        });
                        
                        // 更新行号
                        fromLineNum.set(logContent.getToLineNum() + 1);
                        pullFailCount.set(0);
                    }
                    
                    // 检查是否结束
                    if (logContent.isEnd()) {
                        isLogEnd.set(true);
                    }
                } else {
                    pullFailCount.incrementAndGet();
                    if (response != null && response.getMsg() != null) {
                        logger.warn("加载日志失败: " + response.getMsg());
                    }
                }
            } catch (Exception ex) {
                pullFailCount.incrementAndGet();
                logger.error("加载日志异常", ex);
            }
        }, "load-log-content").start();
    }

    /**
     * 解码HTML实体
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) return "";
        try {
            String decoded = StringEscapeUtils.unescapeHtml4(text);
            // 替换 <br> 标签为换行符
            decoded = decoded.replace("<br>", "\n")
                             .replace("<br/>", "\n")
                             .replace("<br />", "\n");
            return decoded;
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * 应用日志语法高亮
     */
    private void applyLogHighlighting(CodeArea codeArea) {
        String text = codeArea.getText();
        if (text == null || text.isEmpty()) return;

        try {
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            
            // 错误关键词模式
            Pattern errorPattern = Pattern.compile(
                "(?i)(error|exception|fail|failed|failure|caused by|RuntimeException|NullPointerException|" +
                "IllegalArgumentException|IOException|SQLException|执行结果:失败|任务执行失败|\\[×\\])"
            );
            
            // 警告关键词模式
            Pattern warnPattern = Pattern.compile(
                "(?i)(warn|warning|⚠|警告)"
            );
            
            // 成功关键词模式
            Pattern successPattern = Pattern.compile(
                "(?i)(success|成功|执行结果:成功|任务执行成功|\\[✓\\]|completed|finish)"
            );
            
            // 时间戳模式
            Pattern timestampPattern = Pattern.compile(
                "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}"
            );

            int lastEnd = 0;
            String[] lines = text.split("\n");
            int currentPos = 0;
            
            for (String line : lines) {
                String lowerLine = line.toLowerCase();
                int lineStart = currentPos;
                int lineEnd = currentPos + line.length();
                
                // 判断整行是否为错误/警告/成功行
                boolean isErrorLine = errorPattern.matcher(line).find();
                boolean isWarnLine = !isErrorLine && warnPattern.matcher(line).find();
                boolean isSuccessLine = !isErrorLine && !isWarnLine && successPattern.matcher(line).find();
                
                if (lineStart > lastEnd) {
                    spansBuilder.add(Collections.emptyList(), lineStart - lastEnd);
                }
                
                if (isErrorLine) {
                    spansBuilder.add(Collections.singleton("log-error"), line.length());
                } else if (isWarnLine) {
                    spansBuilder.add(Collections.singleton("log-warn"), line.length());
                } else if (isSuccessLine) {
                    spansBuilder.add(Collections.singleton("log-success"), line.length());
                } else {
                    spansBuilder.add(Collections.emptyList(), line.length());
                }
                
                lastEnd = lineEnd;
                currentPos = lineEnd + 1; // +1 for newline
            }
            
            // 处理剩余文本
            if (lastEnd < text.length()) {
                spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
            }
            
            StyleSpans<Collection<String>> styleSpans = spansBuilder.create();
            if (styleSpans.length() > 0) {
                codeArea.setStyleSpans(0, styleSpans);
            }
        } catch (Exception e) {
            logger.debug("应用语法高亮失败", e);
        }
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
                    if (jobId != null) {
                        query.setJobId(jobId);
                    }
                    JobGroup selectedGroup = jobGroupCombo.getSelectionModel().getSelectedItem();
                    if (selectedGroup != null) {
                        query.setJobGroup(selectedGroup.getId());
                    }
                    if (keywordField != null && keywordField.getText() != null && !keywordField.getText().trim().isEmpty()) {
                        query.setKeyword(keywordField.getText().trim());
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

    private void handleArchive() {
        TextInputDialog input = new TextInputDialog("90");
        input.setTitle("日志归档");
        input.setHeaderText("删除早于指定天数的日志");
        input.setContentText("保留最近 N 天：");
        input.initOwner(getDialogPane().getScene().getWindow());
        input.showAndWait().ifPresent(s -> {
            int days;
            try {
                days = Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                showError("输入错误", "请输入有效天数（数字）");
                return;
            }
            if (days < 1) {
                showError("输入错误", "保留天数至少为 1");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "将删除 " + days + " 天前的所有日志，是否继续？",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("确认归档");
            confirm.initOwner(getDialogPane().getScene().getWindow());
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    runAsync("归档", () -> {
                        int deleted = jobLogService.archiveLogs(days);
                        return "已归档（删除）" + deleted + " 条日志";
                    });
                }
            });
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

    /**
     * 格式化时间字符串为 yyyy-MM-dd HH:mm:ss 格式
     */
    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return "";
        }
        
        String trimmed = timeStr.trim();
        
        try {
            // 移除毫秒部分（如果有）
            String timeWithoutMillis = trimmed.split("\\.")[0];
            
            // 尝试解析 ISO 格式（如 2025-12-21T16:15:42）
            if (timeWithoutMillis.contains("T")) {
                LocalDateTime dateTime = LocalDateTime.parse(timeWithoutMillis);
                return dateTime.format(DATE_FORMATTER);
            }
            
            // 尝试解析标准格式（如 2025-12-21 16:15:42）
            // 如果已经是目标格式，直接返回
            if (timeWithoutMillis.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return timeWithoutMillis;
            }
            
            // 尝试用标准格式解析
            LocalDateTime dateTime = LocalDateTime.parse(timeWithoutMillis, DATE_FORMATTER);
            return dateTime.format(DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // 如果解析失败，返回原始字符串
            logger.debug("时间格式化失败: " + timeStr, e);
            return trimmed;
        }
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

