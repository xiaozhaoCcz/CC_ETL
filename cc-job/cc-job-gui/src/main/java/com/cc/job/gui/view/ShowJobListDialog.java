package com.cc.job.gui.view;

import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 展示任务列表
 */
public class ShowJobListDialog extends Dialog<Void> {

    private final JobInfoService jobInfoService = new JobInfoService();

    private TableView<JobInfoVO> tableView;
    private TextField jobDescField;
    private TextField handlerField;
    private TextField authorField;
    private ComboBox<String> statusCombo;
    private TextField jobGroupField;

    private Label totalLabel;
    private TextField pageField;
    private ComboBox<Integer> pageSizeBox;
    private Button prevBtn;
    private Button nextBtn;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int pageNum = 1;
    private int pageSize = 10;
    private long total = 0;

    public ShowJobListDialog(Stage ownerStage) {
        setTitle("任务列表");
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

        // 初始加载
        loadPage(true);
    }

    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        getDialogPane().setPrefSize(1080, 680);
        // 设置对话框大小
        getDialogPane().setPrefWidth(1080);
        getDialogPane().setPrefHeight(580);
        getDialogPane().setMinWidth(1080);
        getDialogPane().setMinHeight(580);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);

        // 设置对话框样式 - 与主页面背景色一致
        getDialogPane().setStyle(
                "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                        "-fx-background-radius: " + StyleUtil.RADIUS_LG + "; " +
                        "-fx-border-radius: " + StyleUtil.RADIUS_LG + ";"
        );

        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(1080);
                stage.setMinHeight(580);
                
                // 加载全局CSS样式
                try {
                    String css = getClass().getResource("/styles.css").toExternalForm();
                    stage.getScene().getStylesheets().add(css);
                } catch (Exception e) {
                    // CSS文件加载失败，忽略
                }
                
                stage.setOnCloseRequest(event -> {
                    // 这里可以添加关闭前的确认逻辑，例如：
                    // if (!dataIsSaved) {
                    //     event.consume(); // 阻止关闭
                    //     showSaveDialog();
                    // }
                    close(); // 调用Dialog的close方法
                });
            }
        });
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

        // 创建标签样式
        String labelStyle = StyleUtil.body();

        jobGroupField = new TextField();
        jobGroupField.setPromptText("执行器ID");
        jobGroupField.setStyle(StyleUtil.searchField());

        jobDescField = new TextField();
        jobDescField.setPromptText("任务描述");
        jobDescField.setStyle(StyleUtil.searchField());

        handlerField = new TextField();
        handlerField.setPromptText("JobHandler");
        handlerField.setStyle(StyleUtil.searchField());

        authorField = new TextField();
        authorField.setPromptText("负责人");
        authorField.setStyle(StyleUtil.searchField());

        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("全部", "运行", "停止");
        statusCombo.getSelectionModel().selectFirst();

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
            jobGroupField.clear();
            jobDescField.clear();
            handlerField.clear();
            authorField.clear();
            statusCombo.getSelectionModel().selectFirst();
            pageNum = 1;
            loadPage(true);
        });

        Label executorLabel = new Label("执行器");
        executorLabel.setStyle(labelStyle);
        Label statusLabel = new Label("任务状态");
        statusLabel.setStyle(labelStyle);
        Label descLabel = new Label("任务描述");
        descLabel.setStyle(labelStyle);
        Label authorLabel = new Label("负责人");
        authorLabel.setStyle(labelStyle);
        Label handlerLabel = new Label("JobHandler");
        handlerLabel.setStyle(labelStyle);

        int col = 0;
        grid.add(executorLabel, col++, 0);
        grid.add(jobGroupField, col++, 0);
        grid.add(statusLabel, col++, 0);
        grid.add(statusCombo, col++, 0);
        grid.add(descLabel, col++, 0);
        grid.add(jobDescField, col++, 0);

        grid.add(authorLabel, 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(handlerLabel, 2, 1);
        grid.add(handlerField, 3, 1);

        HBox btnBox = new HBox(10, searchBtn, resetBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(btnBox, 4, 1);

        return grid;
    }

    @SuppressWarnings("unchecked")
    private Node createTable() {
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // 表格样式通过CSS类自动应用，与主页面一致

        TableColumn<JobInfoVO, Number> idxCol = new TableColumn<>("序号");
        idxCol.setCellValueFactory(c -> Bindings.createIntegerBinding(
                () -> tableView.getItems().indexOf(c.getValue()) + 1));
        idxCol.setMaxWidth(80);

        TableColumn<JobInfoVO, String> descCol = new TableColumn<>("任务描述");
        descCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getJobDesc())));

        TableColumn<JobInfoVO, String> scheduleTypeCol = new TableColumn<>("调度类型");
        scheduleTypeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getScheduleType())));

        TableColumn<JobInfoVO, String> scheduleConfCol = new TableColumn<>("调度配置");
        scheduleConfCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getScheduleConf())));

        TableColumn<JobInfoVO, String> routeCol = new TableColumn<>("路由策略");
        routeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getExecutorRouteStrategy())));

        TableColumn<JobInfoVO, String> handlerCol = new TableColumn<>("JobHandler");
        handlerCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getExecutorHandler())));

        TableColumn<JobInfoVO, String> authorCol = new TableColumn<>("负责人");
        authorCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getAuthor())));

        TableColumn<JobInfoVO, String> typeCol = new TableColumn<>("任务类型");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(mapJobType(c.getValue().getJobType())));

        TableColumn<JobInfoVO, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(mapStatus(c.getValue().getTriggerStatus())));

        TableColumn<JobInfoVO, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final MenuButton actionMenuBtn = new MenuButton("操作");
            // 初始化菜单项
            MenuItem runItem = new MenuItem("执行一次");
            MenuItem startItem = new MenuItem("启动");
            MenuItem stopItem = new MenuItem("停止");
            // 保存对当前行数据的引用
            {
                // 将菜单项添加到菜单按钮
                actionMenuBtn.getItems().addAll(runItem, startItem, stopItem);
                // 可选：设置按钮宽度，使其更紧凑
                actionMenuBtn.setPrefWidth(80);
                // 应用按钮样式
                actionMenuBtn.setStyle(StyleUtil.secondaryButton());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                // 关键：每次都从表格获取当前数据，不依赖成员变量
                final JobInfoVO currentJob = getTableView().getItems().get(getIndex());

                if (currentJob == null) {
                    setGraphic(null);
                    return;
                }

                // 根据状态设置菜单项禁用状态
                boolean running = currentJob.getTriggerStatus() != null && currentJob.getTriggerStatus() == 1;
                startItem.setDisable(running);
                stopItem.setDisable(!running);

                // 关键：在updateItem中重新绑定事件处理器，确保每次使用正确的currentJob
                runItem.setOnAction(e -> handleRun(currentJob));
                startItem.setOnAction(e -> handleStart(currentJob));
                stopItem.setOnAction(e -> handleStop(currentJob));

                setGraphic(actionMenuBtn);
            }
        });
        actionCol.setPrefWidth(100); // 比原来的220px窄了很多
        actionCol.setMinWidth(100);

        tableView.getColumns().addAll(idxCol, descCol, scheduleTypeCol, scheduleConfCol,
                routeCol, handlerCol, authorCol, typeCol, statusCol, actionCol);

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

        JobInfoQuery query = new JobInfoQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        if (!isBlank(jobGroupField.getText())) {
            try {
                query.setJobGroup(Long.parseLong(jobGroupField.getText().trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (!isBlank(jobDescField.getText())) {
            query.setJobDesc(jobDescField.getText().trim());
        }
        if (!isBlank(handlerField.getText())) {
            query.setExecutorHandler(handlerField.getText().trim());
        }
        if (!isBlank(authorField.getText())) {
            query.setAuthor(authorField.getText().trim());
        }
        String status = statusCombo.getValue();
        if ("运行".equals(status)) {
            query.setTriggerStatus(1);
        } else if ("停止".equals(status)) {
            query.setTriggerStatus(0);
        }

        new Thread(() -> {
            try {
                PageResult<JobInfoVO> page = jobInfoService.getJobInfoPage(query);
                List<JobInfoVO> list = page != null && page.getData() != null
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
                    showError("加载任务列表失败", ex.getMessage());
                });
            } finally {
                loading.set(false);
            }
        }, "load-job-list").start();
    }

    private void updatePagerButtons() {
        long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
        prevBtn.setDisable(pageNum <= 1);
        nextBtn.setDisable(pageNum >= maxPage || maxPage == 0);
    }

    private void handleRun(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        runAsync("执行一次", () -> {
            jobInfoService.triggerOnce(job.getId(), job.getExecutorParam());
            return "触发成功";
        });
    }

    private void handleStart(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        runAsync("启动任务", () -> {
            boolean ok = jobInfoService.startJob(job.getId());
            return ok ? "启动成功" : "启动失败";
        });
    }

    private void handleStop(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        runAsync("停止任务", () -> {
            boolean ok = jobInfoService.stopJob(job.getId());
            return ok ? "停止成功" : "停止失败";
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
                Platform.runLater(() -> showError(title + "失败", ex.getMessage()));
            } finally {
                if (loading.get()) {
                    loading.set(false);
                }
            }
        }, "job-action").start();
    }

    private String mapStatus(Integer status) {
        if (status == null) return "未知";
        return status == 1 ? "运行" : "停止";
    }

    private String mapJobType(Integer type) {
        if (type == null) return "任务";
        return switch (type) {
            case 0 -> "任务组";
            case 2 -> "任务组";
            default -> "任务";
        };
    }

    private String safe(String v) {
        return v == null ? "" : v;
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
