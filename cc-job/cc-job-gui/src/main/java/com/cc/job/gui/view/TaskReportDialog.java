package com.cc.job.gui.view;

import com.cc.job.gui.service.DashboardService;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务报表对话框：筛选栏 + 图表展示执行结果分布（运行成功/失败/运行中）
 */
public class TaskReportDialog extends Dialog<Void> {

    private static final Logger logger = LoggerFactory.getLogger(TaskReportDialog.class);
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DashboardService dashboardService = new DashboardService();
    private final JobInfoService jobInfoService = new JobInfoService();

    private StackPane centerStack;
    private VBox loadingPane;
    private VBox errorPane;
    private VBox contentPane;
    private Label errorLabel;
    private Button retryButton;

    private ComboBox<String> timeRangeCombo;
    private ComboBox<String> taskGroupCombo;
    private List<JobInfo> taskGroupList = new ArrayList<>();
    private PieChart pieChart;
    private Label summaryLabel;

    public TaskReportDialog(Stage ownerStage) {
        setTitle("任务报表");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

        VBox top = new VBox(12);
        top.getChildren().addAll(createTitleBar(), createFilterBar());
        root.setTop(top);

        centerStack = new StackPane();
        centerStack.setMinSize(500, 320);
        loadingPane = createLoadingPane();
        errorPane = createErrorPane();
        contentPane = createContentPane();
        centerStack.getChildren().add(loadingPane);
        root.setCenter(centerStack);

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        loadTaskGroups();
        loadData();
    }

    private void styleDialog() {
        getDialogPane().setPrefSize(780, 520);
        getDialogPane().setMinWidth(560);
        getDialogPane().setMinHeight(420);
        setResizable(true);
        String dialogCss = ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null) {
            getDialogPane().getStylesheets().add(dialogCss);
        }
    }

    private HBox createTitleBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("任务报表");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");

        Button refreshButton = new Button("刷新", IconUtil.refreshIcon());
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> loadData());

        Button exportButton = new Button("导出 Excel");
        exportButton.getStyleClass().add("secondary-button");
        exportButton.setOnAction(e -> exportExcel());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(titleLabel, refreshButton, exportButton, spacer);
        return bar;
    }

    private HBox createFilterBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label("时间范围：");
        timeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");

        timeRangeCombo = new ComboBox<>(FXCollections.observableArrayList("全部", "今日", "近7天", "近30天"));
        timeRangeCombo.setValue("全部");
        timeRangeCombo.setPrefWidth(120);
        timeRangeCombo.setOnAction(e -> loadData());

        Label groupLabel = new Label("任务组：");
        groupLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");

        taskGroupCombo = new ComboBox<>();
        taskGroupCombo.setPrefWidth(200);
        taskGroupCombo.getItems().add("全部");
        taskGroupCombo.setValue("全部");
        taskGroupCombo.setOnAction(e -> loadData());

        bar.getChildren().addAll(timeLabel, timeRangeCombo, groupLabel, taskGroupCombo);
        return bar;
    }

    private void loadTaskGroups() {
        new Thread(() -> {
            try {
                List<JobInfo> list = jobInfoService.getJobInfoList(2);
                Platform.runLater(() -> {
                    taskGroupList.clear();
                    if (list != null) {
                        taskGroupList.addAll(list);
                    }
                    taskGroupCombo.getItems().clear();
                    taskGroupCombo.getItems().add("全部");
                    for (JobInfo info : taskGroupList) {
                        String name = info.getJobDesc() != null ? info.getJobDesc() : ("ID:" + info.getId());
                        taskGroupCombo.getItems().add(name);
                    }
                    taskGroupCombo.setValue("全部");
                });
            } catch (Exception e) {
                logger.debug("加载任务组列表失败", e);
            }
        }, "load-task-groups").start();
    }

    private VBox createLoadingPane() {
        VBox pane = new VBox(12);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(40));

        ProgressIndicator progress = new ProgressIndicator(-1);
        progress.setMaxSize(48, 48);
        Label label = new Label("加载中...");
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
        pane.getChildren().addAll(progress, label);
        return pane;
    }

    private VBox createErrorPane() {
        VBox pane = new VBox(16);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(40));

        errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(360);
        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + StyleUtil.ERROR + ";");

        retryButton = new Button("重试");
        retryButton.getStyleClass().add("primary-button");
        retryButton.setOnAction(e -> loadData());

        pane.getChildren().addAll(errorLabel, retryButton);
        return pane;
    }

    private VBox createContentPane() {
        VBox vbox = new VBox(16);
        vbox.setPadding(new Insets(8, 0, 0, 0));

        summaryLabel = new Label();
        summaryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("运行成功", 0),
                new PieChart.Data("运行失败", 0),
                new PieChart.Data("运行中", 0)
        );
        pieChart = new PieChart(pieData);
        pieChart.setTitle("执行结果分布");
        pieChart.setLegendVisible(true);
        pieChart.setPrefSize(340, 260);
        pieChart.getStyleClass().add("task-report-pie-chart");

        HBox chartBox = new HBox(24);
        chartBox.setAlignment(Pos.CENTER_LEFT);
        chartBox.getChildren().add(pieChart);

        vbox.getChildren().addAll(summaryLabel, chartBox);
        return vbox;
    }

    private void loadData() {
        showLoading();
        String range = timeRangeCombo != null ? timeRangeCombo.getValue() : "全部";
        String start = null;
        String end = null;
        if (range != null && !"全部".equals(range)) {
            LocalDate now = LocalDate.now();
            if ("今日".equals(range)) {
                start = now.atStartOfDay().format(ISO_LOCAL);
                end = now.atTime(23, 59, 59).format(ISO_LOCAL);
            } else if ("近7天".equals(range)) {
                start = now.minusDays(6).atStartOfDay().format(ISO_LOCAL);
                end = now.atTime(23, 59, 59).format(ISO_LOCAL);
            } else if ("近30天".equals(range)) {
                start = now.minusDays(29).atStartOfDay().format(ISO_LOCAL);
                end = now.atTime(23, 59, 59).format(ISO_LOCAL);
            }
        }
        Long jobId = null;
        if (taskGroupCombo != null && taskGroupCombo.getValue() != null && !"全部".equals(taskGroupCombo.getValue())) {
            int idx = taskGroupCombo.getItems().indexOf(taskGroupCombo.getValue());
            if (idx > 0 && idx - 1 < taskGroupList.size()) {
                jobId = taskGroupList.get(idx - 1).getId();
            }
        }

        final String fStart = start;
        final String fEnd = end;
        final Long fJobId = jobId;

        new Thread(() -> {
            try {
                DashboardStatsVO stats = dashboardService.getStats(fStart, fEnd, fJobId);
                Platform.runLater(() -> applyStats(stats));
            } catch (IOException e) {
                logger.warn("获取任务报表统计失败", e);
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }, "task-report-load").start();
    }

    private void showLoading() {
        centerStack.getChildren().clear();
        centerStack.getChildren().add(loadingPane);
    }

    private void showError(String message) {
        centerStack.getChildren().clear();
        errorLabel.setText(message != null ? message : "加载失败");
        centerStack.getChildren().add(errorPane);
    }

    private void applyStats(DashboardStatsVO stats) {
        centerStack.getChildren().clear();
        centerStack.getChildren().add(contentPane);

        long tg = stats.getTaskGroupCount() != null ? stats.getTaskGroupCount() : 0;
        long jc = stats.getJobCount() != null ? stats.getJobCount() : 0;
        summaryLabel.setText("任务组总数： " + tg + "  |  任务总数： " + jc);

        long success = stats.getLogSuccessCount() != null ? stats.getLogSuccessCount() : 0;
        long fail = stats.getLogFailCount() != null ? stats.getLogFailCount() : 0;
        long running = stats.getLogRunningCount() != null ? stats.getLogRunningCount() : 0;

        ObservableList<PieChart.Data> pieData = pieChart.getData();
        pieData.get(0).setPieValue(success);
        pieData.get(1).setPieValue(fail);
        pieData.get(2).setPieValue(running);
    }

    private void exportExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出任务报表");
        chooser.setInitialFileName("任务报表_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        String range = timeRangeCombo != null ? timeRangeCombo.getValue() : "全部";
        String start = null;
        String end = null;
        if (range != null && !"全部".equals(range)) {
            LocalDate now = LocalDate.now();
            if ("今日".equals(range)) {
                start = now.atStartOfDay().format(ISO_LOCAL);
                end = now.atTime(23, 59, 59).format(ISO_LOCAL);
            } else if ("近7天".equals(range)) {
                start = now.minusDays(6).atStartOfDay().format(ISO_LOCAL);
                end = now.atTime(23, 59, 59).format(ISO_LOCAL);
            } else if ("近30天".equals(range)) {
                start = now.minusDays(29).atStartOfDay().format(ISO_LOCAL);
                end = now.atTime(23, 59, 59).format(ISO_LOCAL);
            }
        }
        Long jobId = null;
        if (taskGroupCombo != null && taskGroupCombo.getValue() != null && !"全部".equals(taskGroupCombo.getValue())) {
            int idx = taskGroupCombo.getItems().indexOf(taskGroupCombo.getValue());
            if (idx > 0 && idx - 1 < taskGroupList.size()) {
                jobId = taskGroupList.get(idx - 1).getId();
            }
        }
        final String fStart = start;
        final String fEnd = end;
        final Long fJobId = jobId;
        final Path path = file.toPath();

        new Thread(() -> {
            try {
                dashboardService.downloadExport(path, fStart, fEnd, fJobId);
                Platform.runLater(() -> com.cc.job.gui.util.NotificationToast.showSuccess("导出成功：" + path));
            } catch (IOException ex) {
                logger.warn("导出 Excel 失败", ex);
                Platform.runLater(() -> com.cc.job.gui.util.NotificationToast.showError("导出失败：" + ex.getMessage()));
            }
        }, "task-report-export").start();
    }
}
