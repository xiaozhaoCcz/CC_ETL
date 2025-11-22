package com.cc.job.gui.view;

import com.cc.job.gui.service.LoginService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 注册视图 - 现代化设计
 */
public class RegisterView extends StackPane {
    
    private static final Logger logger = LoggerFactory.getLogger(RegisterView.class);
    
    private TextField usernameField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Button registerButton;
    private Button backButton;
    private Label errorLabel;
    private ProgressIndicator loadingIndicator;
    private Consumer<LoginService.LoginResult> onRegisterSuccess;
    private Runnable onBack;
    private final LoginService loginService;
    
    public RegisterView() {
        this.loginService = new LoginService();
        initializeUI();
    }
    
    private void initializeUI() {
        // 设置背景渐变
        setStyle(
            "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);"
        );
        
        // 创建注册卡片
        VBox registerCard = createRegisterCard();
        
        // 添加装饰元素
        Pane decorations = createDecorations();
        
        getChildren().addAll(decorations, registerCard);
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
     * 创建注册卡片（去掉背景板）
     */
    private VBox createRegisterCard() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(450);
        card.setMaxHeight(650);
        card.setStyle(
            "-fx-background-color: transparent;"
        );
        
        
        // Logo
        StackPane logo = createLogo();
        
        // 标题（改为深色，提高对比度）
        Label titleLabel = new Label("创建新账号");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1F2937")); // 深灰色，在浅色渐变背景上清晰可见
        
        Label subtitleLabel = new Label("填写以下信息完成注册");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.setTextFill(Color.web("#4B5563")); // 中灰色，清晰可读
        
        // 输入区域
        VBox inputArea = createInputArea();
        
        // 错误提示
        errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#EF4444"));
        errorLabel.setFont(Font.font("System", 12));
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(370);
        
        // 加载指示器
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(30, 30);
        loadingIndicator.setVisible(false);
        
        // 注册按钮
        registerButton = createRegisterButton();
        
        // 返回登录链接
        HBox backArea = createBackArea();
        
        // 创建按钮容器，减小与输入框的间距
        VBox buttonContainer = new VBox(10);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(errorLabel, loadingIndicator, registerButton);
        
        // 设置按钮容器与输入区域之间的间距更小
        VBox.setMargin(buttonContainer, new Insets(10, 0, 0, 0));
        
        card.getChildren().addAll(
            logo,
            titleLabel,
            subtitleLabel,
            inputArea,
            buttonContainer,
            backArea
        );
        
        return card;
    }
    
    /**
     * 创建Logo
     */
    private StackPane createLogo() {
        StackPane logoPane = new StackPane();
        logoPane.setMinSize(80, 80);
        logoPane.setMaxSize(80, 80);
        logoPane.setStyle(
            "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); " +
            "-fx-background-radius: 40; " +
            "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 10, 0, 0, 5);"
        );
        
        Label logoText = new Label("N");
        logoText.setFont(Font.font("System", FontWeight.BOLD, 40));
        logoText.setTextFill(Color.WHITE);
        
        logoPane.getChildren().add(logoText);
        
        return logoPane;
    }
    
    /**
     * 创建输入区域
     */
    private VBox createInputArea() {
        VBox inputArea = new VBox(15);
        inputArea.setAlignment(Pos.CENTER);
        
        // 用户名输入
        VBox usernameBox = createInputBox(
            "用户名",
            "请输入用户名（3-20个字符）",
            false
        );
        usernameField = (TextField) ((VBox) usernameBox.getChildren().get(1)).getChildren().get(0);
        
        // 密码输入
        VBox passwordBox = createInputBox(
            "密码",
            "请输入密码（至少6位）",
            true
        );
        passwordField = (PasswordField) ((VBox) passwordBox.getChildren().get(1)).getChildren().get(0);
        
        // 确认密码输入
        VBox confirmPasswordBox = createInputBox(
            "确认密码",
            "请再次输入密码",
            true
        );
        confirmPasswordField = (PasswordField) ((VBox) confirmPasswordBox.getChildren().get(1)).getChildren().get(0);
        
        inputArea.getChildren().addAll(usernameBox, passwordBox, confirmPasswordBox);
        
        return inputArea;
    }
    
    /**
     * 创建输入框
     */
    private VBox createInputBox(String label, String prompt, boolean isPassword) {
        VBox box = new VBox(8);
        
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("System", FontWeight.BOLD, 13));
        labelNode.setTextFill(Color.web("#374151")); // 深灰色，清晰可读
        
        VBox inputWrapper = new VBox();
        inputWrapper.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );
        inputWrapper.setPadding(new Insets(8, 15, 8, 15));
        
        TextField field;
        if (isPassword) {
            field = new PasswordField();
        } else {
            field = new TextField();
        }
        field.setPromptText(prompt);
        field.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-font-size: 14; " +
            "-fx-text-fill: #1F2937;"
        );
        field.setPrefWidth(370);
        
        // 焦点样式
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                inputWrapper.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: #667EEA; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8;"
                );
            } else {
                inputWrapper.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: #E5E7EB; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8;"
                );
            }
        });
        
        inputWrapper.getChildren().add(field);
        box.getChildren().addAll(labelNode, inputWrapper);
        
        return box;
    }
    
    /**
     * 创建注册按钮
     */
    private Button createRegisterButton() {
        Button button = new Button("注册");
        button.setPrefWidth(370);
        button.setPrefHeight(45);
        button.setFont(Font.font("System", FontWeight.BOLD, 15));
        button.setStyle(
            "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); " +
            "-fx-text-fill: black; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 8, 0, 0, 3);"
        );
        
        // 悬停效果
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: linear-gradient(135deg, #5568D3 0%, #63408A 100%); " +
            "-fx-text-fill: black; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.5), 12, 0, 0, 5);"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); " +
            "-fx-text-fill: black; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 8, 0, 0, 3);"
        ));
        
        button.setOnAction(e -> handleRegister());
        
        return button;
    }
    
    /**
     * 创建返回登录区域
     */
    private HBox createBackArea() {
        HBox backArea = new HBox(5);
        backArea.setAlignment(Pos.CENTER);
        
        Label text = new Label("已有账号？");
        text.setFont(Font.font("System", 13));
        text.setTextFill(Color.web("#4B5563")); // 中灰色，清晰可读
        
        backButton = new Button("立即登录");
        backButton.setFont(Font.font("System", FontWeight.BOLD, 13));
        backButton.setTextFill(Color.web("#667EEA"));
        backButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-width: 0; " +
            "-fx-cursor: hand; " +
            "-fx-underline: true;"
        );
        
        backButton.setOnMouseEntered(e -> backButton.setTextFill(Color.web("#5568D3")));
        backButton.setOnMouseExited(e -> backButton.setTextFill(Color.web("#667EEA")));
        
        backButton.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });
        
        backArea.getChildren().addAll(text, backButton);
        
        return backArea;
    }
    
    /**
     * 处理注册
     */
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // 清除之前的错误消息
        hideError();
        
        // 输入验证
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("请填写所有字段");
            return;
        }
        
        if (username.length() < 3 || username.length() > 20) {
            showError("用户名长度必须在3-20个字符之间");
            return;
        }
        
        if (password.length() < 6) {
            showError("密码长度不能少于6位");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("两次输入的密码不一致");
            return;
        }
        
        // 禁用按钮，显示加载
        setLoading(true);
        
        // 在后台线程中调用注册API
        new Thread(() -> {
            try {
                LoginService.LoginResult result = loginService.register(username, password);
                
                Platform.runLater(() -> {
                    setLoading(false);
                    
                    if (result.isSuccess()) {
                        // 注册成功
                        if (onRegisterSuccess != null) {
                            onRegisterSuccess.accept(result);
                        }
                    } else {
                        // 注册失败
                        showError(result.getMessage());
                    }
                });
                
            } catch (Exception ex) {
                logger.error("注册失败: {}", ex.getMessage(), ex);
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("注册失败: " + ex.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 显示错误消息
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    /**
     * 隐藏错误消息
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }
    
    /**
     * 设置加载状态
     */
    private void setLoading(boolean loading) {
        registerButton.setDisable(loading);
        loadingIndicator.setVisible(loading);
        backButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
    }
    
    /**
     * 设置注册成功回调
     */
    public void setOnRegisterSuccess(Consumer<LoginService.LoginResult> callback) {
        this.onRegisterSuccess = callback;
    }
    
    /**
     * 设置返回回调
     */
    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }
}

