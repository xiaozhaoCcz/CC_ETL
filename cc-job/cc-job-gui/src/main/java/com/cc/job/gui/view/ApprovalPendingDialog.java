package com.cc.job.gui.view;

import com.cc.job.gui.service.JobApprovalService;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.model.entity.JobApprovalPending;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 待办审批对话框：列表展示待审批项，支持通过/拒绝
 */
public class ApprovalPendingDialog extends Dialog<Void> {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Stage owner;
    private final JobApprovalService approvalService = new JobApprovalService();
    private TableView<JobApprovalPending> table;
    private Label statusLabel;

    public ApprovalPendingDialog(Stage owner) {
        this.owner = owner;
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("待办审批");
        setHeaderText("当前用户的待审批项（执行到审批节点时产生）");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Button refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> loadPending());
        statusLabel = new Label("点击刷新加载列表");
        HBox topBar = new HBox(10, refreshBtn, statusLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setPlaceholder(new Label("暂无待办"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<JobApprovalPending, String> colId = new TableColumn<>("ID");
        colId.setPrefWidth(60);
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId() != null ? c.getValue().getId().toString() : ""));

        TableColumn<JobApprovalPending, String> colJobId = new TableColumn<>("任务组ID");
        colJobId.setPrefWidth(80);
        colJobId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getJobId() != null ? c.getValue().getJobId().toString() : ""));

        TableColumn<JobApprovalPending, String> colNodeId = new TableColumn<>("节点ID");
        colNodeId.setPrefWidth(80);
        colNodeId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNodeId() != null ? c.getValue().getNodeId().toString() : ""));

        TableColumn<JobApprovalPending, String> colBatch = new TableColumn<>("批次");
        colBatch.setPrefWidth(120);
        colBatch.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBatchId() != null ? c.getValue().getBatchId() : ""));

        TableColumn<JobApprovalPending, String> colDeadline = new TableColumn<>("截止时间");
        colDeadline.setPrefWidth(130);
        colDeadline.setCellValueFactory(c -> {
            if (c.getValue().getWaitDeadline() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(c.getValue().getWaitDeadline().format(DT_FMT));
        });

        TableColumn<JobApprovalPending, String> colCreate = new TableColumn<>("创建时间");
        colCreate.setPrefWidth(130);
        colCreate.setCellValueFactory(c -> {
            if (c.getValue().getCreateTime() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(c.getValue().getCreateTime().format(DT_FMT));
        });

        TableColumn<JobApprovalPending, Void> colAction = new TableColumn<>("操作");
        colAction.setPrefWidth(140);
        colAction.setCellFactory(tc -> new TableCell<>() {
            private final Button approveBtn = new Button("通过");
            private final Button rejectBtn = new Button("拒绝");
            {
                approveBtn.getStyleClass().add("button-success");
                rejectBtn.getStyleClass().add("button-danger");
                approveBtn.setOnAction(e -> {
                    JobApprovalPending p = getTableRow().getItem();
                    if (p != null) doApprove(p.getId());
                });
                rejectBtn.setOnAction(e -> {
                    JobApprovalPending p = getTableRow().getItem();
                    if (p != null) doReject(p.getId());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(6, approveBtn, rejectBtn));
                }
            }
        });

        table.getColumns().addAll(colId, colJobId, colNodeId, colBatch, colDeadline, colCreate, colAction);

        root.getChildren().addAll(topBar, table);
        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        String cssUrl = ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            getDialogPane().getStylesheets().add(cssUrl);
        }

        setResultConverter(bt -> null);

        // 打开对话框时自动加载一次列表
        Platform.runLater(this::loadPending);
    }

    private void loadPending() {
        statusLabel.setText("加载中...");
        new Thread(() -> {
            try {
                List<JobApprovalPending> list = approvalService.listPending(null);
                Platform.runLater(() -> {
                    table.getItems().clear();
                    if (list != null && !list.isEmpty()) {
                        table.getItems().addAll(list);
                        statusLabel.setText("共 " + list.size() + " 条待办");
                    } else {
                        statusLabel.setText("暂无待办（若已执行到审批节点仍看不到，请确认当前用户是否在节点的审批人列表中）");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("加载失败");
                    NotificationToast.showError("加载待办列表失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void doApprove(Long id) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("审批通过");
        inputDialog.setHeaderText("可选：填写备注");
        inputDialog.setContentText("备注：");
        Optional<String> remark = inputDialog.showAndWait();
        if (remark.isEmpty()) return; // 用户取消
        final String remarkStr = remark.get().trim().isEmpty() ? null : remark.get().trim();
        statusLabel.setText("处理中...");
        new Thread(() -> {
            try {
                approvalService.approve(id, remarkStr);
                Platform.runLater(() -> {
                    NotificationToast.showSuccess("已通过");
                    loadPending();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("");
                    NotificationToast.showError("通过失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void doReject(Long id) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("审批拒绝");
        inputDialog.setHeaderText("可选：填写拒绝原因");
        inputDialog.setContentText("原因：");
        Optional<String> remark = inputDialog.showAndWait();
        if (remark.isEmpty()) return; // 用户取消
        final String remarkStr = remark.get().trim().isEmpty() ? null : remark.get().trim();
        statusLabel.setText("处理中...");
        new Thread(() -> {
            try {
                approvalService.reject(id, remarkStr);
                Platform.runLater(() -> {
                    NotificationToast.showSuccess("已拒绝");
                    loadPending();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("");
                    NotificationToast.showError("拒绝失败: " + e.getMessage());
                });
            }
        }).start();
    }
}
