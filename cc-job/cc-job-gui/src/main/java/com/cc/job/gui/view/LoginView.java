package com.cc.job.gui.view;

import com.cc.job.gui.service.LoginService;
import com.cc.job.gui.util.ConfigManager;
import com.cc.job.gui.util.ThemeManager;
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
    private TextField serverUrlField;
    private Button loginButton;
    private Button registerButton;
    private Label errorLabel;
    private ProgressIndicator loadingIndicator;
    private Consumer<LoginService.LoginResult> onLoginSuccess;
    private Runnable onRegister;
    private final LoginService loginService;
    private final ConfigManager configManager;
    
    public LoginView() {
        this.loginService = new LoginService();
        this.configManager = ConfigManager.getInstance();
        initializeUI();
    }
    
    private void initializeUI() {
        getStyleClass().add("login-root");
        applyRootBackground();
        
        // 创建登录卡片
        VBox loginCard = createLoginCard();
        
        // 添加装饰元素
        Pane decorations = createDecorations();
        
        getChildren().addAll(decorations, loginCard);
    }
    
    private void applyRootBackground() {
        boolean dark = "dark".equals(ThemeManager.getInstance().getTheme());
        if (dark) {
            setStyle("-fx-background-color: linear-gradient(135deg, #252526 0%, #1E1E1E 100%);");
        } else {
            setStyle("-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);");
        }
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
        card.getStyleClass().add("login-card");
        
        // Logo/图标区域
        VBox logoArea = createLogoArea();
        
        Label titleLabel = new Label("欢迎回来");
        titleLabel.getStyleClass().add("login-title");
        
        Label subtitleLabel = new Label("登录以继续使用 CC_ETL");
        subtitleLabel.getStyleClass().add("login-subtitle");
        
        // 表单区域
        VBox formArea = createFormArea();
        
        // 错误提示
        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(320);
        errorLabel.getStyleClass().add("login-error");
        
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
        
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("用户名");
        usernameLabel.getStyleClass().add("login-label");
        
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setPrefHeight(44);
        usernameField.getStyleClass().add("login-field");
        
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("密码");
        passwordLabel.getStyleClass().add("login-label");
        
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefHeight(44);
        passwordField.getStyleClass().add("login-field");
        passwordField.setOnAction(e -> handleLogin());
        
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        VBox serverUrlBox = new VBox(8);
        Label serverUrlLabel = new Label("后台地址");
        serverUrlLabel.getStyleClass().add("login-label");
        
        serverUrlField = new TextField();
        serverUrlField.setPromptText("请输入后台地址，如: http://localhost:8989");
        serverUrlField.setPrefHeight(44);
        String savedUrl = configManager.getBaseUrl();
        serverUrlField.setText(savedUrl != null ? savedUrl : "http://localhost:8989");
        serverUrlField.getStyleClass().add("login-field");
        
        serverUrlBox.getChildren().addAll(serverUrlLabel, serverUrlField);
        
        form.getChildren().addAll(usernameBox, passwordBox, serverUrlBox);
        
        return form;
    }
    
    /**
     * 创建按钮区域
     */
    private HBox createButtonArea() {
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(320);
        
        loginButton = new Button("登录");
        loginButton.setPrefHeight(44);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        loginButton.getStyleClass().add("login-primary-btn");
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
        String serverUrl = serverUrlField.getText().trim();
        
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
        
        if (serverUrl.isEmpty()) {
            showError("请输入后台地址");
            serverUrlField.requestFocus();
            return;
        }
        
        // 验证URL格式
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            showError("后台地址格式错误，应以 http:// 或 https:// 开头");
            serverUrlField.requestFocus();
            return;
        }
        
        // 保存后台地址配置
        configManager.setBaseUrl(serverUrl);
        
        // 显示加载状态
        setLoading(true);
        hideError();
        
        // 异步执行登录
        new Thread(() -> {
            try {
                // 使用新的后台地址登录
                LoginService.LoginResult result = loginService.login(username, password, serverUrl);
                
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
        serverUrlField.setDisable(loading);
        
        if (loading) {
            loginButton.setText("登录中...");
            loginButton.getStyleClass().add("login-primary-btn-loading");
        } else {
            loginButton.setText("登录");
            loginButton.getStyleClass().remove("login-primary-btn-loading");
        }
    }
    
    /**
     * 创建注册区域
     */
    private HBox createRegisterArea() {
        HBox registerArea = new HBox(5);
        registerArea.setAlignment(Pos.CENTER);
        
        Label text = new Label("还没有账号？");
        text.getStyleClass().add("login-subtitle");
        
        registerButton = new Button("立即注册");
        registerButton.getStyleClass().add("login-link");
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

