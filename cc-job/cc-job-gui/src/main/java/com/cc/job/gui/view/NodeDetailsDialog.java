package com.cc.job.gui.view;

import com.cc.job.gui.manager.TaskExecutionManager;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.model.entity.JobNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 节点详情对话框
 */
public class NodeDetailsDialog extends Dialog<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeDetailsDialog.class);
    
    private final JobInfoService jobInfoService = new JobInfoService();
    private final TaskExecutionManager taskExecutionManager;
    private final Long jobId;
    private final Long taskGroupId;
    private final String nodeName;
    private final String nodeId;
    
    public NodeDetailsDialog(Stage ownerStage, Long jobId, Long taskGroupId, String nodeName, String nodeId, TaskExecutionManager taskExecutionManager) {
        this.jobId = jobId;
        this.taskGroupId = taskGroupId;
        this.nodeName = nodeName;
        this.nodeId = nodeId;
        this.taskExecutionManager = taskExecutionManager;
        
        setTitle("节点详情");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);
        
        styleDialog();
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("dialog-content-root");
        
        // 创建滚动面板以容纳更多内容
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(16));
        
        // 任务基本信息区域
        Label taskInfoTitle = new Label("任务基本信息");
        taskInfoTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        contentBox.getChildren().add(taskInfoTitle);
        
        GridPane taskGrid = new GridPane();
        taskGrid.setHgap(20);
        taskGrid.setVgap(12);
        taskGrid.setPadding(new Insets(8, 0, 0, 0));
        
        int row = 0;
        addLabel(taskGrid, "任务ID:", 0, row);
        Label jobIdLabel = addValueLabel(taskGrid, jobId != null ? String.valueOf(jobId) : "未知", 1, row++);
        
        addLabel(taskGrid, "任务描述:", 0, row);
        Label jobDescLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "执行器Handler:", 0, row);
        Label executorHandlerLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "执行器参数:", 0, row);
        Label executorParamLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "作者:", 0, row);
        Label authorLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "调度类型:", 0, row);
        Label scheduleTypeLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "调度配置:", 0, row);
        Label scheduleConfLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "执行器路由策略:", 0, row);
        Label executorRouteStrategyLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "失败策略:", 0, row);
        Label failStrategyLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "阻塞处理策略:", 0, row);
        Label executorBlockStrategyLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "执行超时时间(秒):", 0, row);
        Label executorTimeoutLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "失败重试次数:", 0, row);
        Label executorFailRetryCountLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "GLUE类型:", 0, row);
        Label glueTypeLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "最近运行耗时(ms):", 0, row);
        Label runTimeLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "暂停状态:", 0, row);
        Label pauseStatusLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        
        addLabel(taskGrid, "下一次运行时间:", 0, row);
        Label nextTriggerTimeLabel = addValueLabel(taskGrid, "加载中...", 1, row++);
        nextTriggerTimeLabel.setStyle("-fx-text-fill: " + StyleUtil.linkPrimaryColor() + ";");
        
        contentBox.getChildren().add(taskGrid);
        
        // 节点信息区域
        Label nodeInfoTitle = new Label("节点信息");
        nodeInfoTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 16 0 0 0;");
        contentBox.getChildren().add(nodeInfoTitle);
        
        GridPane nodeGrid = new GridPane();
        nodeGrid.setHgap(20);
        nodeGrid.setVgap(12);
        nodeGrid.setPadding(new Insets(8, 0, 0, 0));
        
        row = 0;
        addLabel(nodeGrid, "节点ID:", 0, row);
        Label nodeIdLabel = addValueLabel(nodeGrid, nodeId != null ? nodeId : "未知", 1, row++);
        
        addLabel(nodeGrid, "节点名称:", 0, row);
        Label nodeNameLabel = addValueLabel(nodeGrid, nodeName != null ? nodeName : "未知", 1, row++);
        
        addLabel(nodeGrid, "任务组ID:", 0, row);
        Label taskGroupIdLabel = addValueLabel(nodeGrid, taskGroupId != null ? String.valueOf(taskGroupId) : "未知", 1, row++);
        
        addLabel(nodeGrid, "节点位置X:", 0, row);
        Label nodePositionXLabel = addValueLabel(nodeGrid, "加载中...", 1, row++);
        
        addLabel(nodeGrid, "节点位置Y:", 0, row);
        Label nodePositionYLabel = addValueLabel(nodeGrid, "加载中...", 1, row++);
        
        addLabel(nodeGrid, "入度:", 0, row);
        Label nodeInDegreeLabel = addValueLabel(nodeGrid, "加载中...", 1, row++);
        
        addLabel(nodeGrid, "出度:", 0, row);
        Label nodeOutDegreeLabel = addValueLabel(nodeGrid, "加载中...", 1, row++);
        
        addLabel(nodeGrid, "排序:", 0, row);
        Label sortLabel = addValueLabel(nodeGrid, "加载中...", 1, row++);
        
        addLabel(nodeGrid, "节点类型:", 0, row);
        Label nodeTypeLabel = addValueLabel(nodeGrid, "加载中...", 1, row++);
        
        addLabel(nodeGrid, "节点运行状态:", 0, row);
        Label triggerStatusLabel = addValueLabel(nodeGrid, "加载中...", 1, row++);
        
        contentBox.getChildren().add(nodeGrid);
        
        // 预测时间区域
        Label predictedTimeTitle = new Label("预测执行时间");
        predictedTimeTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 16 0 0 0;");
        contentBox.getChildren().add(predictedTimeTitle);
        
        GridPane predictedTimeGrid = new GridPane();
        predictedTimeGrid.setHgap(20);
        predictedTimeGrid.setVgap(12);
        predictedTimeGrid.setPadding(new Insets(8, 0, 0, 0));
        
        row = 0;
        addLabel(predictedTimeGrid, "预测到达时间:", 0, row);
        Label predictedArrivalTimeLabel = addValueLabel(predictedTimeGrid, "未启动任务组", 1, row++);
        predictedArrivalTimeLabel.setStyle("-fx-text-fill: " + StyleUtil.linkPrimaryColor() + ";");
        
        addLabel(predictedTimeGrid, "预测完成时间:", 0, row);
        Label predictedFinishTimeLabel = addValueLabel(predictedTimeGrid, "未启动任务组", 1, row++);
        predictedFinishTimeLabel.setStyle("-fx-text-fill: " + StyleUtil.linkPrimaryColor() + ";");

        contentBox.getChildren().add(predictedTimeGrid);
        
        scrollPane.setContent(contentBox);
        root.getChildren().add(scrollPane);

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // 加载详细信息
        loadDetails(jobDescLabel, executorHandlerLabel, executorParamLabel, authorLabel,
                scheduleTypeLabel, scheduleConfLabel, executorRouteStrategyLabel, failStrategyLabel,
                executorBlockStrategyLabel, executorTimeoutLabel, executorFailRetryCountLabel,
                glueTypeLabel, runTimeLabel, pauseStatusLabel, nextTriggerTimeLabel,
                nodePositionXLabel, nodePositionYLabel, nodeInDegreeLabel, nodeOutDegreeLabel,
                sortLabel, nodeTypeLabel, triggerStatusLabel,
                predictedArrivalTimeLabel, predictedFinishTimeLabel);
    }
    
    private void styleDialog() {
        getDialogPane().setPrefSize(700, 800);
        getDialogPane().setMinWidth(700);
        getDialogPane().setMinHeight(600);
        setResizable(true);
        String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (css != null && !css.isEmpty()) {
            getDialogPane().getStylesheets().add(css);
        }
    }
    
    private Label addLabel(GridPane grid, String text, int col, int row) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        grid.add(label, col, row);
        return label;
    }
    
    private Label addValueLabel(GridPane grid, String text, int col, int row) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 400;");
        label.setWrapText(true);
        grid.add(label, col, row);
        return label;
    }
    
    private void loadDetails(Label jobDescLabel, Label executorHandlerLabel, Label executorParamLabel,
                            Label authorLabel, Label scheduleTypeLabel, Label scheduleConfLabel,
                            Label executorRouteStrategyLabel, Label failStrategyLabel,
                            Label executorBlockStrategyLabel, Label executorTimeoutLabel,
                            Label executorFailRetryCountLabel, Label glueTypeLabel,
                            Label runTimeLabel, Label pauseStatusLabel, Label nextTriggerTimeLabel,
                            Label nodePositionXLabel, Label nodePositionYLabel,
                            Label nodeInDegreeLabel, Label nodeOutDegreeLabel,
                            Label sortLabel, Label nodeTypeLabel, Label triggerStatusLabel,
                            Label predictedArrivalTimeLabel, Label predictedFinishTimeLabel) {
        if (jobId == null) {
            Platform.runLater(() -> {
                jobDescLabel.setText("无法获取（任务ID为空）");
                jobDescLabel.setStyle("-fx-text-fill: #EF4444;");
            });
            return;
        }
        
        new Thread(() -> {
            try {
                // 加载任务信息
                JobInfoForm formData = jobInfoService.getJobNodeFormData(jobId);
                if (formData != null) {
                    Platform.runLater(() -> {
                        // 任务基本信息
                        jobDescLabel.setText(formData.getJobDesc() != null ? formData.getJobDesc() : "未设置");
                        executorHandlerLabel.setText(formData.getExecutorHandler() != null ? formData.getExecutorHandler() : "未设置");
                        executorParamLabel.setText(formData.getExecutorParam() != null ? formData.getExecutorParam() : "未设置");
                        authorLabel.setText(formData.getAuthor() != null ? formData.getAuthor() : "未设置");
                        scheduleTypeLabel.setText(formData.getScheduleType() != null ? formData.getScheduleType() : "未设置");
                        scheduleConfLabel.setText(formData.getScheduleConf() != null ? formData.getScheduleConf() : "未设置");
                        executorRouteStrategyLabel.setText(formData.getExecutorRouteStrategy() != null ? formData.getExecutorRouteStrategy() : "未设置");
                        failStrategyLabel.setText(formData.getFailStrategy() != null ? formData.getFailStrategy() : "未设置");
                        executorBlockStrategyLabel.setText(formData.getExecutorBlockStrategy() != null ? formData.getExecutorBlockStrategy() : "未设置");
                        executorTimeoutLabel.setText(formData.getExecutorTimeout() != null ? String.valueOf(formData.getExecutorTimeout()) : "未设置");
                        executorFailRetryCountLabel.setText(formData.getExecutorFailRetryCount() != null ? String.valueOf(formData.getExecutorFailRetryCount()) : "未设置");
                        glueTypeLabel.setText(formData.getGlueType() != null ? formData.getGlueType() : "未设置");
                        runTimeLabel.setText(formData.getRunTime() != null ? String.valueOf(formData.getRunTime()) + " ms" : "未运行");
                        pauseStatusLabel.setText(formData.getPauseStatus() != null ? (formData.getPauseStatus() == 0 ? "运行中" : "已暂停") : "未知");
                        
                        // 下一次运行时间
                        String scheduleType = formData.getScheduleType();
                        String scheduleConf = formData.getScheduleConf();
                        if (scheduleType != null && scheduleConf != null && !scheduleType.isEmpty() && !scheduleConf.isEmpty()) {
                            try {
                                List<String> nextTimes = jobInfoService.getNextTriggerTime(scheduleType, scheduleConf);
                                if (nextTimes != null && !nextTimes.isEmpty()) {
                                    nextTriggerTimeLabel.setText(nextTimes.get(0));
                                } else {
                                    nextTriggerTimeLabel.setText("无");
                                }
                            } catch (Exception e) {
                                nextTriggerTimeLabel.setText("获取失败: " + e.getMessage());
                                nextTriggerTimeLabel.setStyle("-fx-text-fill: #EF4444;");
                            }
                        } else {
                            nextTriggerTimeLabel.setText("未配置调度");
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        jobDescLabel.setText("无法获取节点数据");
                        jobDescLabel.setStyle("-fx-text-fill: #EF4444;");
                    });
                }
                
                // 加载节点信息
                if (taskGroupId != null) {
                    try {
                        List<JobNode> jobNodes = jobInfoService.getJobNodes(taskGroupId);
                        if (jobNodes != null && !jobNodes.isEmpty()) {
                            // 根据节点ID或jobId查找对应的节点
                            JobNode targetNode = null;
                            if (nodeId != null) {
                                try {
                                    Long nodeIdLong = Long.parseLong(nodeId);
                                    for (JobNode node : jobNodes) {
                                        if (node.getId() != null && node.getId().equals(nodeIdLong)) {
                                            targetNode = node;
                                            break;
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    // 如果nodeId不是数字，则通过jobId查找
                                    for (JobNode node : jobNodes) {
                                        if (node.getJobId() != null && node.getJobId().equals(jobId)) {
                                            targetNode = node;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                // 如果没有nodeId，通过jobId查找
                                for (JobNode node : jobNodes) {
                                    if (node.getJobId() != null && node.getJobId().equals(jobId)) {
                                        targetNode = node;
                                        break;
                                    }
                                }
                            }
                            
                            if (targetNode != null) {
                                final JobNode node = targetNode;
                                Platform.runLater(() -> {
                                    nodePositionXLabel.setText(node.getNodePositionX() != null ? String.format("%.2f", node.getNodePositionX()) : "未设置");
                                    nodePositionYLabel.setText(node.getNodePositionY() != null ? String.format("%.2f", node.getNodePositionY()) : "未设置");
                                    nodeInDegreeLabel.setText(node.getNodeInDegree() != null ? String.valueOf(node.getNodeInDegree()) : "0");
                                    nodeOutDegreeLabel.setText(node.getNodeOutDegree() != null ? String.valueOf(node.getNodeOutDegree()) : "0");
                                    sortLabel.setText(node.getSort() != null ? String.valueOf(node.getSort()) : "未设置");
                                    nodeTypeLabel.setText(node.getNodeType() != null ? node.getNodeType() : "未设置");
                                    
                                    // 节点运行状态
                                    if (node.getTriggerStatus() != null) {
                                        String statusText;
                                        switch (node.getTriggerStatus()) {
                                            case -1:
                                                statusText = "未运行";
                                                break;
                                            case 0:
                                                statusText = "失败";
                                                break;
                                            case 1:
                                                statusText = "成功";
                                                break;
                                            case 2:
                                                statusText = "运行中";
                                                break;
                                            default:
                                                statusText = "未知(" + node.getTriggerStatus() + ")";
                                        }
                                        triggerStatusLabel.setText(statusText);
                                    } else {
                                        triggerStatusLabel.setText("未知");
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        logger.error("获取节点信息失败", e);
                    }
                }
                
                // 加载预测时间
                if (taskExecutionManager != null) {
                    Map<String, String[]> predictedTimes = taskExecutionManager.getPredictedNodeTimes();
                    if (predictedTimes != null && !predictedTimes.isEmpty()) {
                        String[] times = null;
                        
                        // 首先尝试通过nodeId直接查找
                        if (nodeId != null) {
                            times = predictedTimes.get(nodeId);
                        }
                        
                        // 如果通过nodeId找不到，尝试通过jobId查找对应的节点ID
                        if (times == null && jobId != null && taskGroupId != null) {
                            try {
                                List<JobNode> jobNodes = jobInfoService.getJobNodes(taskGroupId);
                                if (jobNodes != null) {
                                    // 找到对应的节点
                                    for (JobNode node : jobNodes) {
                                        if (node.getJobId() != null && node.getJobId().equals(jobId)) {
                                            String nodeIdStr = String.valueOf(node.getId());
                                            times = predictedTimes.get(nodeIdStr);
                                            if (times != null) {
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("查找预测时间时获取节点列表失败", e);
                            }
                        }
                        
                        if (times != null && times.length >= 2) {
                            final String arrivalTime = times[0];
                            final String finishTime = times[1];
                            Platform.runLater(() -> {
                                predictedArrivalTimeLabel.setText(arrivalTime != null && !arrivalTime.isEmpty() ? arrivalTime : "未预测");
                                predictedFinishTimeLabel.setText(finishTime != null && !finishTime.isEmpty() ? finishTime : "未预测");
                            });
                        } else {
                            Platform.runLater(() -> {
                                predictedArrivalTimeLabel.setText("未启动任务组或无预测数据");
                                predictedFinishTimeLabel.setText("未启动任务组或无预测数据");
                            });
                        }
                    } else {
                        Platform.runLater(() -> {
                            predictedArrivalTimeLabel.setText("未启动任务组");
                            predictedFinishTimeLabel.setText("未启动任务组");
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        predictedArrivalTimeLabel.setText("未启动任务组");
                        predictedFinishTimeLabel.setText("未启动任务组");
                    });
                }
                
            } catch (Exception e) {
                logger.error("加载节点详情失败", e);
                Platform.runLater(() -> {
                    jobDescLabel.setText("获取失败: " + e.getMessage());
                    jobDescLabel.setStyle("-fx-text-fill: #EF4444;");
                });
            }
        }).start();
    }
}

