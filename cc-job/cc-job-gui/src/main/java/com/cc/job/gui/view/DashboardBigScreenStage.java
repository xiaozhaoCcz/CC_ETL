package com.cc.job.gui.view;

import com.cc.job.gui.service.DashboardService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import com.cc.job.xo.model.vo.DashboardTrendItemVO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 统计大屏：全屏/大窗口展示核心指标与趋势图
 */
public class DashboardBigScreenStage extends Stage {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DashboardService dashboardService = new DashboardService();
    private final Label taskGroupLabel = new Label("0");
    private final Label jobCountLabel = new Label("0");
    private final Label successLabel = new Label("0");
    private final Label failLabel = new Label("0");
    private final Label runningLabel = new Label("0");
    private final Label trendTitle = new Label("近7日执行趋势");
    private BarChart<String, Number> barChart;

    public DashboardBigScreenStage(Stage owner) {
        setTitle("Cc-ETL 统计大屏");
        initOwner(owner);
        setMaximized(true);
        setMinWidth(800);
        setMinHeight(600);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-big-screen");
        root.setPadding(new Insets(24));
        String css = ThemeManager.getInstance().getStylesheetUrl();
        if (css != null) root.getStylesheets().add(css);

        VBox top = new VBox(16);
        Label title = new Label("Cc-ETL 任务统计大屏");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");
        Button refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> loadData());
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getChildren().addAll(title, refreshBtn);
        top.getChildren().add(topBar);
        root.setTop(top);

        GridPane cards = new GridPane();
        cards.setHgap(24);
        cards.setVgap(24);
        cards.setAlignment(Pos.CENTER);
        addCard(cards, 0, 0, "任务组数", taskGroupLabel);
        addCard(cards, 1, 0, "任务数", jobCountLabel);
        addCard(cards, 2, 0, "执行成功", successLabel);
        addCard(cards, 3, 0, "执行失败", failLabel);
        addCard(cards, 4, 0, "运行中", runningLabel);
        root.setCenter(cards);

        VBox chartBox = new VBox(12);
        chartBox.setPadding(new Insets(24, 0, 0, 0));
        trendTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("按日统计");
        barChart.setLegendVisible(true);
        barChart.setPrefHeight(320);
        barChart.getStyleClass().add("dashboard-big-chart");
        chartBox.getChildren().addAll(trendTitle, barChart);
        root.setBottom(chartBox);

        Scene scene = new Scene(root, 1000, 700);
        setScene(scene);
        loadData();
    }

    private void addCard(GridPane grid, int col, int row, String title, Label valueLabel) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: rgba(128,128,128,0.1); -fx-background-radius: 8;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14px; -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");
        card.getChildren().addAll(t, valueLabel);
        grid.add(card, col, row);
    }

    private void loadData() {
        String start = LocalDate.now().minusDays(6).atStartOfDay().format(ISO_LOCAL);
        String end = LocalDate.now().atTime(23, 59, 59).format(ISO_LOCAL);
        new Thread(() -> {
            try {
                DashboardStatsVO stats = dashboardService.getStats(start, end, null);
                List<DashboardTrendItemVO> trend = dashboardService.getTrend(start, end, null);
                Platform.runLater(() -> {
                    applyStats(stats);
                    applyTrend(trend);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    successLabel.setText("--");
                    failLabel.setText("--");
                    runningLabel.setText("--");
                });
            }
        }, "dashboard-bigscreen-load").start();
    }

    private void applyStats(DashboardStatsVO stats) {
        taskGroupLabel.setText(String.valueOf(stats.getTaskGroupCount() != null ? stats.getTaskGroupCount() : 0));
        jobCountLabel.setText(String.valueOf(stats.getJobCount() != null ? stats.getJobCount() : 0));
        successLabel.setText(String.valueOf(stats.getLogSuccessCount() != null ? stats.getLogSuccessCount() : 0));
        failLabel.setText(String.valueOf(stats.getLogFailCount() != null ? stats.getLogFailCount() : 0));
        runningLabel.setText(String.valueOf(stats.getLogRunningCount() != null ? stats.getLogRunningCount() : 0));
    }

    private void applyTrend(List<DashboardTrendItemVO> trend) {
        if (trend == null || trend.isEmpty()) return;
        XYChart.Series<String, Number> successSeries = new XYChart.Series<>();
        successSeries.setName("成功");
        XYChart.Series<String, Number> failSeries = new XYChart.Series<>();
        failSeries.setName("失败");
        for (DashboardTrendItemVO item : trend) {
            successSeries.getData().add(new XYChart.Data<>(item.getDate(), item.getSuccessCount() != null ? item.getSuccessCount() : 0));
            failSeries.getData().add(new XYChart.Data<>(item.getDate(), item.getFailCount() != null ? item.getFailCount() : 0));
        }
        barChart.getData().clear();
        barChart.getData().addAll(successSeries, failSeries);
    }
}
