package com.cc.job.gui.view;

import com.cc.job.gui.service.JobLogService;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.vo.JobLogVO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 节点历史执行状态页面
 * 
 * <p>展示任务组内所有节点的历史执行状态信息
 *
 * @author cc-job-team
 */
public class NodeHistoryView extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeHistoryView.class);
    
    private final Long taskGroupId;
    private final String taskGroupName;
    private final JobLogService jobLogService;
    
    private ScrollPane scrollPane;
    private VBox contentContainer;
    private Label statusLabel;
    private Label pageLabel;
    private Button nextButton;
    
    private int currentPage = 1;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public NodeHistoryView(Long taskGroupId, String taskGroupName) {
        this.taskGroupId = taskGroupId;
        this.taskGroupName = taskGroupName;
        this.jobLogService = new JobLogService();
        
        initializeUI();
        loadData();
    }
    
    private void initializeUI() {
        setSpacing(0);
        setStyle("-fx-background-color: #FFFFFF;");
        
        // 标题栏
        HBox titleBar = createTitleBar();
        
        // 工具栏
        HBox toolBar = createToolBar();
        
        // 内容容器（卡片布局）
        contentContainer = new VBox(16);
        contentContainer.setPadding(new Insets(20));
        contentContainer.setStyle("-fx-background-color: #F9FAFB;");
        
        scrollPane = new ScrollPane(contentContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #F9FAFB;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // 分页栏
        HBox paginationBar = createPaginationBar();
        
        getChildren().addAll(titleBar, toolBar, scrollPane, paginationBar);
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(12);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(16, 20, 16, 20));
        titleBar.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");
        
        Label titleLabel = new Label("节点执行历史");
        titleLabel.setStyle(StyleUtil.subtitle() + "-fx-font-weight: 700;");
        
        Label groupLabel = new Label("任务组: " + taskGroupName);
        groupLabel.setStyle(StyleUtil.body() + "-fx-text-fill: #6B7280;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        statusLabel = new Label("");
        statusLabel.setStyle(StyleUtil.caption() + "-fx-text-fill: #9CA3AF;");
        
        titleBar.getChildren().addAll(titleLabel, groupLabel, spacer, statusLabel);
        return titleBar;
    }
    
    private HBox createToolBar() {
        HBox toolBar = new HBox(8);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(12, 20, 12, 20));
        toolBar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");
        
        Button refreshButton = createToolButton("刷新", IconUtil.refreshIcon());
        refreshButton.setOnAction(e -> loadData());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label tipsLabel = new Label("提示：显示该任务组每次执行时所有节点的执行状态");
        tipsLabel.setStyle(StyleUtil.caption() + "-fx-text-fill: #9CA3AF;");
        
        toolBar.getChildren().addAll(refreshButton, spacer, tipsLabel);
        return toolBar;
    }
    
    private Button createToolButton(String text, javafx.scene.Node icon) {
        Button button = new Button(text, icon);
        button.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: #F3F4F6; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #9CA3AF; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        return button;
    }
    
    /**
     * 创建执行记录卡片
     */
    private VBox createExecutionCard(JobLogVO log) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );
        
        // 卡片头部：执行时间和总体状态
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label timeLabel = new Label("执行时间: " + formatTime(log.getTriggerTime()));
        timeLabel.setStyle(StyleUtil.body() + "-fx-font-weight: 600; -fx-text-fill: #1F2937;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 总体执行状态
        Label overallStatus = createStatusLabel(getOverallStatus(log));
        header.getChildren().addAll(timeLabel, spacer, overallStatus);
        
        // 节点状态网格
        FlowPane nodeGrid = createNodeStatusGrid(log);
        
        card.getChildren().addAll(header, nodeGrid);
        return card;
    }
    
    /**
     * 创建节点状态网格
     */
    private FlowPane createNodeStatusGrid(JobLogVO log) {
        FlowPane grid = new FlowPane(12, 12);
        grid.setPrefWrapLength(800);
        
        String nodeStatusJson = log.getNodeStatus();
        if (nodeStatusJson == null || nodeStatusJson.trim().isEmpty()) {
            Label emptyLabel = new Label("暂无节点状态记录");
            emptyLabel.setStyle(StyleUtil.caption() + "-fx-text-fill: #9CA3AF;");
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
            errorLabel.setStyle(StyleUtil.caption() + "-fx-text-fill: #EF4444;");
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
        nodeCard.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );
        
        Label nameLabel = new Label(nodeName);
        nameLabel.setStyle(StyleUtil.body() + "-fx-font-weight: 500; -fx-text-fill: #374151;");
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
     * 获取总体执行状态
     */
    private int getOverallStatus(JobLogVO log) {
        if (log.getHandleCode() != null) {
            if (log.getHandleCode() == 200) {
                return 1; // 成功
            } else if (log.getHandleCode() == 500) {
                return 0; // 失败
            } else if (log.getHandleCode() == 0) {
                return 2; // 运行中
            }
        }
        return -1; // 未知
    }
    
    private HBox createPaginationBar() {
        HBox paginationBar = new HBox(16);
        paginationBar.setAlignment(Pos.CENTER);
        paginationBar.setPadding(new Insets(12, 20, 12, 20));
        paginationBar.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-width: 1 0 0 0;");
        
        Button prevButton = new Button("上一页");
        prevButton.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadData();
            }
        });
        
        pageLabel = new Label("第 " + currentPage + " 页");
        pageLabel.setStyle(StyleUtil.body() + "-fx-text-fill: #6B7280;");
        
        nextButton = new Button("下一页");
        nextButton.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #374151; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadData();
            }
        });
        
        paginationBar.getChildren().addAll(prevButton, pageLabel, nextButton);
        return paginationBar;
    }
    
    private void loadData() {
        statusLabel.setText("加载中...");
        contentContainer.getChildren().clear();
        
        new Thread(() -> {
            try {
                PageResult<JobLogVO> result = jobLogService.getNodeHistoryLogs(
                    taskGroupId, currentPage, PAGE_SIZE);
                
                Platform.runLater(() -> {
                    if (result != null && result.getData() != null && result.getData().getList() != null 
                        && !result.getData().getList().isEmpty()) {
                        contentContainer.getChildren().clear();
                        
                        for (JobLogVO log : result.getData().getList()) {
                            VBox card = createExecutionCard(log);
                            contentContainer.getChildren().add(card);
                        }
                        
                        long total = result.getData().getTotal();
                        totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
                        statusLabel.setText(String.format("共 %d 条记录，第 %d/%d 页", 
                                                         total, currentPage, totalPages));
                        // 更新分页标签
                        if (pageLabel != null) {
                            pageLabel.setText("第 " + currentPage + " 页");
                        }
                    } else {
                        Label emptyLabel = new Label("暂无执行记录");
                        emptyLabel.setStyle(StyleUtil.body() + "-fx-text-fill: #9CA3AF; -fx-padding: 40;");
                        emptyLabel.setAlignment(Pos.CENTER);
                        contentContainer.getChildren().add(emptyLabel);
                        statusLabel.setText("无数据");
                    }
                });
                
            } catch (IOException e) {
                logger.error("加载节点历史数据失败", e);
                Platform.runLater(() -> {
                    statusLabel.setText("加载失败: " + e.getMessage());
                    showError("加载失败", "无法加载节点历史数据: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 格式化时间字符串为 yyyy-MM-dd HH:mm:ss 格式
     */
    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return "未知";
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
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
