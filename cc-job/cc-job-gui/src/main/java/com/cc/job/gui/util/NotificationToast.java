package com.cc.job.gui.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局通知提示框工具类
 * 支持在所有页面上显示，并且显示在最上层
 * 居中显示，仅显示警告和错误信息
 * 使用StackPane叠加背景层，确保圆角完全一致
 */
public class NotificationToast {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationToast.class);
    
    // 提示框类型
    public enum NotificationType {
        WARNING,    // 警告
        ERROR       // 错误
    }
    
    // 默认显示时长（毫秒）
    private static final long DEFAULT_DURATION = 4000;
    
    // 提示框宽度
    private static final double WIDTH = 420;
    
    // 统一圆角半径（所有边角都使用这个值）
    private static final double CORNER_RADIUS = 12.0;
    
    /**
     * 显示通知提示框
     * @param message 提示信息
     * @param type 提示类型
     */
    public static void show(String message, NotificationType type) {
        show(message, type, DEFAULT_DURATION);
    }
    
    /**
     * 显示通知提示框
     * @param message 提示信息
     * @param type 提示类型
     * @param duration 显示时长（毫秒），0表示不自动消失
     */
    public static void show(String message, NotificationType type, long duration) {
        Platform.runLater(() -> {
            try {
                // 创建Stage
                Stage stage = new Stage();
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setAlwaysOnTop(true);
                stage.setResizable(false);
                
                // 获取屏幕尺寸并居中显示
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                double screenWidth = bounds.getWidth();
                double screenHeight = bounds.getHeight();
                
                // 计算位置（屏幕中心）
                double x = (screenWidth - WIDTH) / 2;
                double y = screenHeight / 3; // 屏幕上方1/3处
                
                stage.setX(x);
                stage.setY(y);
                
                // 根据类型设置颜色和图标
                String headerBgColor;
                String borderColor;
                String iconText;
                String iconColor;
                String titleText;
                
                switch (type) {
                    case WARNING:
                        headerBgColor = "#EFF6FF";
                        borderColor = "#3B82F6";
                        iconText = "⚠";
                        iconColor = "#2563EB";
                        titleText = "警告";
                        break;
                    case ERROR:
                    default:
                        headerBgColor = "#EFF6FF";
                        borderColor = "#3B82F6";
                        iconText = "✗";
                        iconColor = "#2563EB";
                        titleText = "错误";
                        break;
                }
                
                // 使用StackPane作为根容器，可以叠加多个层
                StackPane rootPane = new StackPane();
                rootPane.setPrefWidth(WIDTH);
                rootPane.setMinWidth(WIDTH);
                rootPane.setMaxWidth(WIDTH);

                
                // 第四层：内容容器（透明背景，不设置圆角）
                VBox contentContainer = new VBox(0);
                contentContainer.setAlignment(Pos.TOP_LEFT);
                contentContainer.setPrefWidth(WIDTH);
                contentContainer.setMinWidth(WIDTH);
                contentContainer.setMaxWidth(WIDTH);
                contentContainer.setStyle("-fx-background-color: transparent;");
                
                // 头部区域（透明背景，不设置圆角）
                HBox headerBox = new HBox(12);
                headerBox.setPadding(new Insets(16, 20, 16, 20));
                headerBox.setAlignment(Pos.CENTER_LEFT);
                headerBox.setStyle("-fx-background-color: transparent;");
                headerBox.setPrefWidth(WIDTH);
                headerBox.setMinWidth(WIDTH);
                headerBox.setMaxWidth(WIDTH);

                // 图标容器
                StackPane iconContainer = new StackPane();
                iconContainer.setMinSize(40, 40);
                iconContainer.setPrefSize(40, 40);
                iconContainer.setMaxSize(40, 40);
                iconContainer.setStyle(
                    "-fx-background-color: " + iconColor + "; " +
                    "-fx-background-radius: 50%;"
                );
                
                Label iconLabel = new Label(iconText);
                iconLabel.setStyle(
                    "-fx-font-size: 22; " +
                    "-fx-text-fill: #FFFFFF; " +
                    "-fx-font-weight: bold;"
                );
                iconContainer.getChildren().add(iconLabel);
                
                // 标题
                Label titleLabel = new Label(titleText);
                titleLabel.setStyle(
                    "-fx-font-size: 18; " +
                    "-fx-text-fill: " + StyleUtil.GRAY_900 + "; " +
                    "-fx-font-weight: bold;"
                );
                
                // 关闭按钮
                Button closeButton = new Button("×");
                closeButton.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-text-fill: " + StyleUtil.GRAY_400 + "; " +
                    "-fx-font-size: 24; " +
                    "-fx-font-weight: normal; " +
                    "-fx-padding: 0; " +
                    "-fx-min-width: 32; " +
                    "-fx-pref-width: 32; " +
                    "-fx-min-height: 32; " +
                    "-fx-pref-height: 32; " +
                    "-fx-cursor: hand;"
                );
                closeButton.setOnMouseEntered(e -> {
                    closeButton.setStyle(
                        "-fx-background-color: rgba(0,0,0,0.08); " +
                        "-fx-text-fill: " + StyleUtil.GRAY_600 + "; " +
                        "-fx-font-size: 24; " +
                        "-fx-font-weight: normal; " +
                        "-fx-padding: 0; " +
                        "-fx-min-width: 32; " +
                        "-fx-pref-width: 32; " +
                        "-fx-min-height: 32; " +
                        "-fx-pref-height: 32; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 50%;"
                    );
                });
                closeButton.setOnMouseExited(e -> {
                    closeButton.setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + StyleUtil.GRAY_400 + "; " +
                        "-fx-font-size: 24; " +
                        "-fx-font-weight: normal; " +
                        "-fx-padding: 0; " +
                        "-fx-min-width: 32; " +
                        "-fx-pref-width: 32; " +
                        "-fx-min-height: 32; " +
                        "-fx-pref-height: 32; " +
                        "-fx-cursor: hand;"
                    );
                });
                closeButton.setOnAction(e -> closeNotification(stage));
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                headerBox.getChildren().addAll(iconContainer, titleLabel, spacer, closeButton);
                
                // 内容区域（透明背景，不设置圆角）
                VBox contentBox = new VBox(12);
                contentBox.setPadding(new Insets(20, 20, 20, 20));
                contentBox.setAlignment(Pos.TOP_LEFT);
                contentBox.setStyle("-fx-background-color: transparent;");
                
                // 消息文本
                Label messageLabel = new Label(message);
                messageLabel.setWrapText(true);
                messageLabel.setStyle(
                    "-fx-font-size: 15; " +
                    "-fx-text-fill: " + StyleUtil.GRAY_700 + "; " +
                    "-fx-font-weight: normal; " +
                    "-fx-line-spacing: 4;"
                );
                contentBox.getChildren().add(messageLabel);
                
                // 组装内容容器
                contentContainer.getChildren().addAll(headerBox, contentBox);
                
                // 组装根容器（按顺序叠加：背景 -> 头部背景 -> 边框 -> 内容）
                rootPane.getChildren().addAll(contentContainer);
                
                // 添加阴影效果
                DropShadow shadow = new DropShadow();
                shadow.setColor(Color.color(0, 0, 0, 0.25));
                shadow.setRadius(20);
                shadow.setOffsetX(0);
                shadow.setOffsetY(8);
                rootPane.setEffect(shadow);
                
                // 创建场景
                Scene scene = new Scene(rootPane);
                scene.setFill(Color.TRANSPARENT);
                stage.setScene(scene);
                
                // 淡入和缩放动画
                rootPane.setOpacity(0);
                rootPane.setScaleX(0.9);
                rootPane.setScaleY(0.9);
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), rootPane);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                
                ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), rootPane);
                scaleIn.setFromX(0.9);
                scaleIn.setToX(1.0);
                scaleIn.setFromY(0.9);
                scaleIn.setToY(1.0);
                
                ParallelTransition enterAnimation = new ParallelTransition(fadeIn, scaleIn);
                enterAnimation.play();
                
                // 显示窗口
                stage.show();
                
                // 自动消失
                if (duration > 0) {
                    Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(duration), e -> {
                            closeNotification(stage);
                        })
                    );
                    timeline.play();
                }
                
            } catch (Exception e) {
                logger.error("显示通知提示框失败: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * 关闭通知提示框
     */
    private static void closeNotification(Stage stage) {
        Platform.runLater(() -> {
            try {
                StackPane container = (StackPane) stage.getScene().getRoot();
                
                // 淡出和缩放动画
                FadeTransition fadeOut = new FadeTransition(Duration.millis(250), container);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                
                ScaleTransition scaleOut = new ScaleTransition(Duration.millis(250), container);
                scaleOut.setFromX(1.0);
                scaleOut.setToX(0.9);
                scaleOut.setFromY(1.0);
                scaleOut.setToY(0.9);
                
                ParallelTransition exitAnimation = new ParallelTransition(fadeOut, scaleOut);
                exitAnimation.setOnFinished(e -> stage.close());
                exitAnimation.play();
                
            } catch (Exception e) {
                logger.error("关闭通知提示框失败: {}", e.getMessage(), e);
                stage.close();
            }
        });
    }
    
    /**
     * 显示警告通知
     */
    public static void showWarning(String message) {
        show(message, NotificationType.WARNING);
    }
    
    /**
     * 显示错误通知
     */
    public static void showError(String message) {
        show(message, NotificationType.ERROR);
    }
}
