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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;

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
            "-fx-background-color: #FAFAFA; " +
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
        scrollPane.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 6;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 工具栏
        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle(StyleUtil.secondaryButton());
        refreshBtn.setOnAction(e -> {
            fromLineNum.set(0);
            pullFailCount.set(0);
            isLogEnd.set(false);
            codeArea.clear();
            loadLogContentAsync(item.getId(), codeArea, fromLineNum, pullFailCount, isLogEnd, scrollPane);
        });

        ToggleButton autoRefreshBtn = new ToggleButton("自动刷新");
        autoRefreshBtn.setSelected(true);
        autoRefreshBtn.setStyle(StyleUtil.secondaryButton());
        autoRefreshBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isAutoRefresh.set(newVal);
            if (newVal) {
                autoRefreshBtn.setText("自动刷新中");
            } else {
                autoRefreshBtn.setText("自动刷新");
            }
        });

        Button scrollTopBtn = new Button("↑ 顶部");
        scrollTopBtn.setStyle(StyleUtil.secondaryButton());
        scrollTopBtn.setOnAction(e -> {
            codeArea.moveTo(0);
            codeArea.requestFollowCaret();
        });

        Button scrollBottomBtn = new Button("↓ 底部");
        scrollBottomBtn.setStyle(StyleUtil.secondaryButton());
        scrollBottomBtn.setOnAction(e -> {
            codeArea.moveTo(codeArea.getLength());
            codeArea.requestFollowCaret();
        });

        // 全屏按钮
        Button fullscreenBtn = new Button("全屏");
        fullscreenBtn.setStyle(StyleUtil.secondaryButton());
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
        statusLabel.setTextFill(Color.web(StyleUtil.GRAY_500));

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

        // 添加CSS样式
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception ignored) {}

        // 初始加载日志
        loadLogContentAsync(item.getId(), codeArea, fromLineNum, pullFailCount, isLogEnd, scrollPane);

        // 启动自动刷新定时器
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
                        statusLabel.setTextFill(Color.web(StyleUtil.GRAY_500));
                    }
                });
            }
        }, 0, 2000);

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

