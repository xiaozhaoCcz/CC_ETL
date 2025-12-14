package com.cc.job.gui.view;

import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * 节点详情对话框
 */
public class NodeDetailsDialog extends Dialog<Void> {
    
    private final JobInfoService jobInfoService = new JobInfoService();
    private final Long jobId;
    private final Long taskGroupId;
    private final String nodeName;
    private final String nodeId;
    
    public NodeDetailsDialog(Stage ownerStage, Long jobId, Long taskGroupId, String nodeName, String nodeId) {
        this.jobId = jobId;
        this.taskGroupId = taskGroupId;
        this.nodeName = nodeName;
        this.nodeId = nodeId;
        
        setTitle("节点详情");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);
        
        styleDialog();
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + StyleUtil.BG_SECONDARY + ";");
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);
        grid.setPadding(new Insets(16));
        
        // 基本信息
        addLabel(grid, "节点名称:", 0, 0);
        Label nodeNameLabel = addValueLabel(grid, nodeName != null ? nodeName : "未知", 1, 0);
        
        addLabel(grid, "节点ID:", 0, 1);
        Label nodeIdLabel = addValueLabel(grid, nodeId != null ? nodeId : "未知", 1, 1);
        
        addLabel(grid, "任务ID:", 0, 2);
        Label jobIdLabel = addValueLabel(grid, jobId != null ? String.valueOf(jobId) : "未知", 1, 2);
        
        addLabel(grid, "任务组ID:", 0, 3);
        Label taskGroupIdLabel = addValueLabel(grid, taskGroupId != null ? String.valueOf(taskGroupId) : "未知", 1, 3);
        
        addLabel(grid, "下一次运行时间:", 0, 4);
        Label nextTriggerTimeLabel = addValueLabel(grid, "加载中...", 1, 4);
        nextTriggerTimeLabel.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
        
        root.getChildren().add(grid);
        
        // 关闭按钮
        Button closeBtn = new Button("关闭");
        closeBtn.setStyle(StyleUtil.primaryButton());
        closeBtn.setOnAction(e -> close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(16, 0, 0, 0));
        root.getChildren().add(buttonBox);
        
        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // 加载详细信息
        loadDetails(nextTriggerTimeLabel);
    }
    
    private void styleDialog() {
        getDialogPane().setPrefSize(500, 350);
        getDialogPane().setMinWidth(500);
        getDialogPane().setMinHeight(350);
        setResizable(false);
        
        getDialogPane().setStyle(
            "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
            "-fx-background-radius: " + StyleUtil.RADIUS_LG + "; " +
            "-fx-border-radius: " + StyleUtil.RADIUS_LG + ";"
        );
    }
    
    private Label addLabel(GridPane grid, String text, int col, int row) {
        Label label = new Label(text);
        label.setStyle(StyleUtil.body() + "-fx-font-weight: 600; -fx-text-fill: #6B7280;");
        grid.add(label, col, row);
        return label;
    }
    
    private Label addValueLabel(GridPane grid, String text, int col, int row) {
        Label label = new Label(text);
        label.setStyle(StyleUtil.body() + "-fx-text-fill: #111827;");
        grid.add(label, col, row);
        return label;
    }
    
    private void loadDetails(Label nextTriggerTimeLabel) {
        if (jobId == null) {
            Platform.runLater(() -> nextTriggerTimeLabel.setText("无法获取（任务ID为空）"));
            return;
        }
        
        new Thread(() -> {
            try {
                JobInfoForm formData = jobInfoService.getJobNodeFormData(jobId);
                if (formData != null) {
                    String scheduleType = formData.getScheduleType();
                    String scheduleConf = formData.getScheduleConf();
                    
                    if (scheduleType != null && scheduleConf != null && !scheduleType.isEmpty() && !scheduleConf.isEmpty()) {
                        List<String> nextTimes = jobInfoService.getNextTriggerTime(scheduleType, scheduleConf);
                        if (nextTimes != null && !nextTimes.isEmpty()) {
                            String nextTime = nextTimes.get(0);
                            Platform.runLater(() -> nextTriggerTimeLabel.setText(nextTime));
                        } else {
                            Platform.runLater(() -> nextTriggerTimeLabel.setText("无"));
                        }
                    } else {
                        Platform.runLater(() -> nextTriggerTimeLabel.setText("未配置调度"));
                    }
                } else {
                    Platform.runLater(() -> nextTriggerTimeLabel.setText("无法获取节点数据"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    nextTriggerTimeLabel.setText("获取失败: " + e.getMessage());
                    nextTriggerTimeLabel.setStyle("-fx-text-fill: #EF4444;");
                });
            }
        }).start();
    }
}

