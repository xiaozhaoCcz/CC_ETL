package com.cc.job.gui.view;

import com.cc.job.gui.service.DashboardService;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.model.vo.DashboardStatsVO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 任务报表对话框：展示任务组数、任务数、运行成功/失败/运行中统计
 */
public class TaskReportDialog extends Dialog<Void> {

    private static final Logger logger = LoggerFactory.getLogger(TaskReportDialog.class);

    private final DashboardService dashboardService = new DashboardService();

    private StackPane centerStack;
    private VBox loadingPane;
    private VBox errorPane;
    private GridPane statsPane;
    private Label errorLabel;
    private Button retryButton;

    public TaskReportDialog(Stage ownerStage) {
        setTitle("任务报表");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

        root.setTop(createTitleBar());
        centerStack = new StackPane();
        centerStack.setMinSize(400, 280);
        loadingPane = createLoadingPane();
        errorPane = createErrorPane();
        statsPane = createStatsPane();
        centerStack.getChildren().add(loadingPane);
        root.setCenter(centerStack);

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        loadData();
    }

    private void styleDialog() {
        getDialogPane().setPrefSize(720, 420);
        getDialogPane().setMinWidth(520);
        getDialogPane().setMinHeight(360);
        setResizable(true);
        String dialogCss = ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null) {
            getDialogPane().getStylesheets().add(dialogCss);
        }
    }

    private HBox createTitleBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));

        Label titleLabel = new Label("任务报表");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: " + StyleUtil.textPrimaryColor() + ";");

        Button refreshButton = new Button("刷新", IconUtil.refreshIcon());
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> loadData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(titleLabel, refreshButton, spacer);
        return bar;
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

    private GridPane createStatsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(8, 0, 0, 0));

        addStatCard(grid, 0, 0, "任务组总数", "taskGroupCount", "-fx-text-fill: " + StyleUtil.PRIMARY + ";");
        addStatCard(grid, 1, 0, "任务总数", "jobCount", "-fx-text-fill: " + StyleUtil.PRIMARY + ";");
        addStatCard(grid, 2, 0, "运行成功", "logSuccessCount", "-fx-text-fill: " + StyleUtil.SUCCESS + ";");
        addStatCard(grid, 0, 1, "运行失败", "logFailCount", "-fx-text-fill: " + StyleUtil.ERROR + ";");
        addStatCard(grid, 1, 1, "运行中", "logRunningCount", "-fx-text-fill: " + StyleUtil.WARNING + ";");

        return grid;
    }

    private void addStatCard(GridPane grid, int col, int row, String title, String valueKey, String valueStyle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("task-report-card");
        card.setPadding(new Insets(16));
        card.setMinWidth(180);
        card.setStyle("-fx-background-color: " + StyleUtil.BG_SECONDARY + "; -fx-background-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + StyleUtil.TEXT_SECONDARY + ";");

        Label valueLabel = new Label("0");
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; " + valueStyle);
        valueLabel.setUserData(valueKey);

        card.getChildren().addAll(titleLabel, valueLabel);
        grid.add(card, col, row);
    }

    private void loadData() {
        showLoading();
        new Thread(() -> {
            try {
                DashboardStatsVO stats = dashboardService.getStats();
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
        centerStack.getChildren().add(statsPane);

        setCardValue(statsPane, "taskGroupCount", stats.getTaskGroupCount());
        setCardValue(statsPane, "jobCount", stats.getJobCount());
        setCardValue(statsPane, "logSuccessCount", stats.getLogSuccessCount());
        setCardValue(statsPane, "logFailCount", stats.getLogFailCount());
        setCardValue(statsPane, "logRunningCount", stats.getLogRunningCount());
    }

    private void setCardValue(GridPane grid, String key, Long value) {
        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                for (javafx.scene.Node child : card.getChildren()) {
                    if (child instanceof Label && key.equals(child.getUserData())) {
                        ((Label) child).setText(String.valueOf(value != null ? value : 0));
                        return;
                    }
                }
            }
        }
    }
}
