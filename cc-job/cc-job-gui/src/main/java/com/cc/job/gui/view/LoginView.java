package com.cc.job.gui.view;

import com.cc.job.gui.service.LoginService;
import com.cc.job.gui.util.StyleUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * 登录视图 - 现代化设计
 */
public class LoginView extends StackPane {
    
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;
    private Label errorLabel;
    private ProgressIndicator loadingIndicator;
    private Consumer<LoginService.LoginResult> onLoginSuccess;
    private Runnable onRegister;
    private final LoginService loginService;
    
    public LoginView() {
        this.loginService = new LoginService();
        initializeUI();
    }
    
    private void initializeUI() {
        // 设置背景渐变
        setStyle(
            "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);"
        );
        
        // 创建登录卡片
        VBox loginCard = createLoginCard();
        
        // 添加装饰元素
        Pane decorations = createDecorations();
        
        getChildren().addAll(decorations, loginCard);
    }
    
    /**
     * 创建背景装饰元素
     */
    private Pane decorations() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);
        
        // 添加一些圆形装饰
        Circle circle1 = new Circle(100);
        circle1.setFill(Color.web("#FFFFFF", 0.1));
        circle1.setLayoutX(-50);
        circle1.setLayoutY(100);
        
        Circle circle2 = new Circle(150);
        circle2.setFill(Color.web("#FFFFFF", 0.08));
        circle2.setLayoutX(700);
        circle2.setLayoutY(500);
        
        Circle circle3 = new Circle(80);
        circle3.setFill(Color.web("#FFFFFF", 0.12));
        circle3.setLayoutX(200);
        circle3.setLayoutY(450);
        
        pane.getChildren().addAll(circle1, circle2, circle3);
        
        return pane;
    }
    
    /**
     * 创建装饰背景
     */
    private Pane createDecorations() {
        Pane decorPane = new Pane();
        decorPane.setMouseTransparent(true);
        
        // 创建几个半透明的圆形作为装饰
        for (int i = 0; i < 5; i++) {
            Circle circle = new Circle(50 + i * 30);
            circle.setFill(Color.web("#FFFFFF", 0.05 + i * 0.02));
            circle.setLayoutX(Math.random() * 800);
            circle.setLayoutY(Math.random() * 600);
            decorPane.getChildren().add(circle);
        }
        
        return decorPane;
    }
    
    /**
     * 创建登录卡片（去掉背景板）
     */
    private VBox createLoginCard() {
        VBox card = new VBox(24);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setMaxHeight(550);
        card.setPadding(new Insets(48, 48, 48, 48));
        card.setStyle(
            "-fx-background-color: transparent;"
        );
        
        // Logo/图标区域
        VBox logoArea = createLogoArea();
        
        // 标题（改为深色，提高对比度）
        Label titleLabel = new Label("欢迎回来");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1F2937")); // 深灰色，在浅色渐变背景上清晰可见
        
        Label subtitleLabel = new Label("登录以继续使用 CC_ETL");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setTextFill(Color.web("#4B5563")); // 中灰色，清晰可读
        
        // 表单区域
        VBox formArea = createFormArea();
        
        // 错误提示
        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(320);
        errorLabel.setStyle(
            "-fx-background-color: #FEE2E2; " +
            "-fx-text-fill: #DC2626; " +
            "-fx-padding: 12 16; " +
            "-fx-background-radius: 8; " +
            "-fx-font-size: 13px;"
        );
        
        // 登录按钮
        HBox buttonArea = createButtonArea();
        
        // 底部注册区域
        HBox registerArea = createRegisterArea();
        
        card.getChildren().addAll(
            logoArea,
            titleLabel,
            subtitleLabel,
            formArea,
            errorLabel,
            buttonArea,
            registerArea
        );
        
        return card;
    }
    
    /**
     * 创建Logo区域
     */
    private VBox createLogoArea() {
        VBox logoBox = new VBox(8);
        logoBox.setAlignment(Pos.CENTER);
        
        // 创建一个简单的Logo图标
        StackPane logoIcon = new StackPane();
        logoIcon.setPrefSize(72, 72);
        logoIcon.setStyle(
            "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); " +
            "-fx-background-radius: 16; " +
            "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 12, 0, 0, 4);"
        );
        
        Label logoText = new Label("N");
        logoText.setFont(Font.font("System", FontWeight.BOLD, 36));
        logoText.setTextFill(Color.WHITE);
        
        logoIcon.getChildren().add(logoText);
        logoBox.getChildren().add(logoIcon);
        
        return logoBox;
    }
    
    /**
     * 创建表单区域
     */
    private VBox createFormArea() {
        VBox form = new VBox(16);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(320);
        
        // 用户名输入框（标签改为深色）
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("用户名");
        usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        usernameLabel.setTextFill(Color.web("#374151")); // 深灰色，清晰可读
        
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setPrefHeight(44);
        usernameField.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 12 16; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: #111827;"
        );
        
        // 聚焦时的样式
        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                usernameField.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: #667EEA; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 12 16; " +
                    "-fx-font-size: 14px; " +
                    "-fx-text-fill: #111827; " +
                    "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.25), 8, 0, 0, 0);"
                );
            } else {
                usernameField.setStyle(
                    "-fx-background-color: #F9FAFB; " +
                    "-fx-border-color: #E5E7EB; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 12 16; " +
                    "-fx-font-size: 14px; " +
                    "-fx-text-fill: #111827;"
                );
            }
        });
        
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        // 密码输入框（标签改为深色）
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("密码");
        passwordLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        passwordLabel.setTextFill(Color.web("#374151")); // 深灰色，清晰可读
        
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefHeight(44);
        passwordField.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 12 16; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: #111827;"
        );
        
        // 聚焦时的样式
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                passwordField.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: #667EEA; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 12 16; " +
                    "-fx-font-size: 14px; " +
                    "-fx-text-fill: #111827; " +
                    "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.25), 8, 0, 0, 0);"
                );
            } else {
                passwordField.setStyle(
                    "-fx-background-color: #F9FAFB; " +
                    "-fx-border-color: #E5E7EB; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 12 16; " +
                    "-fx-font-size: 14px; " +
                    "-fx-text-fill: #111827;"
                );
            }
        });
        
        // 按Enter键登录
        passwordField.setOnAction(e -> handleLogin());
        
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        form.getChildren().addAll(usernameBox, passwordBox);
        
        return form;
    }
    
    /**
     * 创建按钮区域
     */
    private HBox createButtonArea() {
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(320);
        
        // 登录按钮
        loginButton = new Button("登录");
        loginButton.setPrefHeight(44);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        loginButton.setStyle(
            "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); " +
            "-fx-text-fill: black; " +
            "-fx-font-size: 15px; " +
            "-fx-font-weight: 600; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 8, 0, 0, 2);"
        );
        
        // 悬停效果
        loginButton.setOnMouseEntered(e -> {
            loginButton.setStyle(
                "-fx-background-color: linear-gradient(135deg, #5568D3 0%, #6941C6 100%); " +
                "-fx-text-fill: black; " +
                "-fx-font-size: 15px; " +
                "-fx-font-weight: 600; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.6), 12, 0, 0, 4);"
            );
        });
        
        loginButton.setOnMouseExited(e -> {
            if (!loginButton.isDisabled()) {
                loginButton.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); " +
                    "-fx-text-fill: black; " +
                    "-fx-font-size: 15px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 8, 0, 0, 2);"
                );
            }
        });
        
        loginButton.setOnAction(e -> handleLogin());
        
        // 加载指示器
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(24, 24);
        loadingIndicator.setVisible(false);
        
        buttonBox.getChildren().addAll(loginButton);
        
        return buttonBox;
    }
    
    /**
     * 处理登录
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // 验证输入
        if (username.isEmpty()) {
            showError("请输入用户名");
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("请输入密码");
            passwordField.requestFocus();
            return;
        }
        
        // 显示加载状态
        setLoading(true);
        hideError();
        
        // 异步执行登录
        new Thread(() -> {
            try {
                LoginService.LoginResult result = loginService.login(username, password);
                
                Platform.runLater(() -> {
                    setLoading(false);
                    
                    if (result.isSuccess()) {
                        // 登录成功
                        if (onLoginSuccess != null) {
                            onLoginSuccess.accept(result);
                        }
                    } else {
                        // 登录失败
                        showError(result.getMessage());
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("登录失败: " + e.getMessage());
                    passwordField.clear();
                    passwordField.requestFocus();
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    /**
     * 隐藏错误信息
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }
    
    /**
     * 设置加载状态
     */
    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        
        if (loading) {
            loginButton.setText("登录中...");
            loginButton.setStyle(
                "-fx-background-color: #9CA3AF; " +
                "-fx-text-fill: black; " +
                "-fx-font-size: 15px; " +
                "-fx-font-weight: 600; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: default;"
            );
        } else {
            loginButton.setText("登录");
            loginButton.setStyle(
                "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); " +
                "-fx-text-fill: black; " +
                "-fx-font-size: 15px; " +
                "-fx-font-weight: 600; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 8, 0, 0, 2);"
            );
        }
    }
    
    /**
     * 创建注册区域
     */
    private HBox createRegisterArea() {
        HBox registerArea = new HBox(5);
        registerArea.setAlignment(Pos.CENTER);
        
        Label text = new Label("还没有账号？");
        text.setFont(Font.font("System", 13));
        text.setTextFill(Color.web("#4B5563")); // 中灰色，清晰可读
        
        registerButton = new Button("立即注册");
        registerButton.setFont(Font.font("System", FontWeight.BOLD, 13));
        registerButton.setTextFill(Color.web("#667EEA"));
        registerButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-cursor: hand; " +
            "-fx-underline: true;"
        );
        
        registerButton.setOnMouseEntered(e -> registerButton.setTextFill(Color.web("#5568D3")));
        registerButton.setOnMouseExited(e -> registerButton.setTextFill(Color.web("#667EEA")));
        
        registerButton.setOnAction(e -> {
            if (onRegister != null) {
                onRegister.run();
            }
        });
        
        registerArea.getChildren().addAll(text, registerButton);
        
        return registerArea;
    }
    
    /**
     * 设置登录成功回调
     */
    public void setOnLoginSuccess(Consumer<LoginService.LoginResult> callback) {
        this.onLoginSuccess = callback;
    }
    
    /**
     * 设置注册回调
     */
    public void setOnRegister(Runnable callback) {
        this.onRegister = callback;
    }
}

