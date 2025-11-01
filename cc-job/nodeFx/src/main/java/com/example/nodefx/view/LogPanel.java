package com.example.nodefx.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 专业的日志监控面板
 */
public class LogPanel extends VBox {
    
    private TextArea logArea;
    private Label statusLabel;
    private Label countLabel;
    private Button clearBtn;
    private Button exportBtn;
    private ComboBox<String> filterCombo;
    
    private int logCount = 0;
    private Runnable onDetach;  // 弹出回调
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public LogPanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle("-fx-background-color: #1F2937; -fx-border-color: #374151; -fx-border-width: 1 0 0 0;");
        setPrefHeight(280);
        setMinHeight(280);
        setPadding(new Insets(0));
        
        // 标题栏
        HBox titleBar = createTitleBar();
        
        // 日志显示区域
        logArea = createLogArea();
        VBox.setVgrow(logArea, Priority.ALWAYS);
        
        // 底部状态栏
        HBox statusBar = createStatusBar();
        
        getChildren().addAll(titleBar, logArea, statusBar);
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10, 15, 10, 15));
        titleBar.setStyle("-fx-background-color: #111827;");
        
        // 标题
        Label titleLabel = new Label("📊 日志监控");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#F9FAFB"));
        
        // 状态指示器
        HBox statusIndicator = new HBox(5);
        statusIndicator.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.shape.Circle indicator = new javafx.scene.shape.Circle(4);
        indicator.setFill(Color.web("#10B981"));
        indicator.setEffect(new javafx.scene.effect.DropShadow(5, Color.web("#10B981")));
        
        statusLabel = new Label("就绪");
        statusLabel.setFont(Font.font("System", 11));
        statusLabel.setTextFill(Color.web("#10B981"));
        
        statusIndicator.getChildren().addAll(indicator, statusLabel);
        
        // 过滤器
        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("全部", "信息", "警告", "错误", "调试");
        filterCombo.setValue("全部");
        filterCombo.setStyle(
            "-fx-background-color: #374151; " +
            "-fx-text-fill: #F9FAFB; " +
            "-fx-font-size: 11; " +
            "-fx-pref-width: 100;"
        );
        
        // 空白区域
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 日志计数
        countLabel = new Label("0 条日志");
        countLabel.setFont(Font.font("System", 11));
        countLabel.setTextFill(Color.web("#9CA3AF"));
        
        // 清空按钮
        clearBtn = createToolButton("🗑️ 清空", this::clearLogs);
        
        // 导出按钮
        exportBtn = createToolButton("📥 导出", this::exportLogs);
        
        titleBar.getChildren().addAll(
            titleLabel,
            statusIndicator,
            filterCombo,
            spacer,
            countLabel,
            clearBtn,
            exportBtn
        );
        
        return titleBar;
    }
    
    private TextArea createLogArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle(
            "-fx-control-inner-background: #1F2937; " +
            "-fx-text-fill: #F9FAFB; " +
            "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
            "-fx-font-size: 12; " +
            "-fx-highlight-fill: #374151; " +
            "-fx-highlight-text-fill: #F9FAFB; " +
            "-fx-background-color: #1F2937;"
        );
        
        // 先赋值给成员变量，再调用appendWelcomeMessage
        logArea = area;
        
        // 初始化提示信息
        appendWelcomeMessage();
        
        return area;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setStyle("-fx-background-color: #111827;");
        
        Label infoLabel = new Label("💡 提示: 启动任务后将显示实时日志信息");
        infoLabel.setFont(Font.font("System", 11));
        infoLabel.setTextFill(Color.web("#9CA3AF"));
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label("⏱️ 最后更新: 从未");
        timeLabel.setFont(Font.font("System", 11));
        timeLabel.setTextFill(Color.web("#6B7280"));
        
        statusBar.getChildren().addAll(infoLabel, spacer, timeLabel);
        
        return statusBar;
    }
    
    private Button createToolButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #374151; " +
            "-fx-text-fill: #F9FAFB; " +
            "-fx-font-size: 11; " +
            "-fx-padding: 4 10 4 10; " +
            "-fx-border-radius: 3; " +
            "-fx-background-radius: 3; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #4B5563; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 11; " +
            "-fx-padding: 4 10 4 10; " +
            "-fx-border-radius: 3; " +
            "-fx-background-radius: 3; " +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #374151; " +
            "-fx-text-fill: #F9FAFB; " +
            "-fx-font-size: 11; " +
            "-fx-padding: 4 10 4 10; " +
            "-fx-border-radius: 3; " +
            "-fx-background-radius: 3; " +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnAction(e -> action.run());
        
        return btn;
    }
    
    private void appendWelcomeMessage() {
        logArea.appendText("╔════════════════════════════════════════════════════════════════╗\n");
        logArea.appendText("║                     NodeFx 流程节点编辑器                       ║\n");
        logArea.appendText("║                      日志监控系统 v1.0                         ║\n");
        logArea.appendText("╚════════════════════════════════════════════════════════════════╝\n\n");
        logArea.appendText("📌 系统就绪，等待任务启动...\n");
        logArea.appendText("💡 所有操作日志将实时显示在此处\n\n");
        logArea.appendText("─────────────────────────────────────────────────────────────────\n\n");
    }
    
    /**
     * 添加日志 - INFO级别
     */
    public void info(String message) {
        appendLog("INFO", message, "#10B981");
    }
    
    /**
     * 添加日志 - WARN级别
     */
    public void warn(String message) {
        appendLog("WARN", message, "#F59E0B");
    }
    
    /**
     * 添加日志 - ERROR级别
     */
    public void error(String message) {
        appendLog("ERROR", message, "#EF4444");
    }
    
    /**
     * 添加日志 - DEBUG级别
     */
    public void debug(String message) {
        appendLog("DEBUG", message, "#8B5CF6");
    }
    
    /**
     * 添加日志 - SUCCESS级别
     */
    public void success(String message) {
        appendLog("SUCCESS", message, "#10B981");
    }
    
    /**
     * 直接追加文本（不添加时间戳和格式）
     * 用于显示原始日志内容
     */
    public void appendText(String text) {
        Platform.runLater(() -> {
            logArea.appendText(text);
            
            // 自动滚动到底部
            logArea.setScrollTop(Double.MAX_VALUE);
            
            // 更新状态
            updateStatus("运行中", "#10B981");
        });
    }
    
    private void appendLog(String level, String message, String colorHex) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            String levelPadded = String.format("%-7s", level);
            
            // 根据级别选择图标
            String icon;
            switch (level) {
                case "INFO":
                    icon = "ℹ️";
                    break;
                case "WARN":
                    icon = "⚠️";
                    break;
                case "ERROR":
                    icon = "❌";
                    break;
                case "DEBUG":
                    icon = "🔍";
                    break;
                case "SUCCESS":
                    icon = "✅";
                    break;
                default:
                    icon = "📝";
                    break;
            }
            
            logArea.appendText(String.format("[%s] %s %s │ %s\n", 
                timestamp, icon, levelPadded, message));
            
            logCount++;
            countLabel.setText(logCount + " 条日志");
            
            // 自动滚动到底部
            logArea.setScrollTop(Double.MAX_VALUE);
            
            // 更新状态
            updateStatus("运行中", "#10B981");
        });
    }
    
    /**
     * 清空日志
     */
    public void clearLogs() {
        logArea.clear();
        logCount = 0;
        countLabel.setText("0 条日志");
        appendWelcomeMessage();
        info("日志已清空");
    }
    
    /**
     * 导出日志
     */
    public void exportLogs() {
        // TODO: 实现导出功能
        warn("导出功能开发中...");
    }
    
    /**
     * 更新状态
     */
    public void updateStatus(String status, String colorHex) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            statusLabel.setTextFill(Color.web(colorHex));
        });
    }
    
    /**
     * 获取日志内容
     */
    public String getLogContent() {
        return logArea.getText();
    }
    
    /**
     * 设置弹出回调
     */
    public void setOnDetach(Runnable callback) {
        this.onDetach = callback;
    }
}

