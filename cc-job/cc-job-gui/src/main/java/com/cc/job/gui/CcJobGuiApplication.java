package com.cc.job.gui;

import atlantafx.base.theme.PrimerLight;
import com.cc.job.gui.util.SessionManager;
import com.cc.job.gui.view.LoginView;
import com.cc.job.gui.view.RegisterView;
import com.cc.job.gui.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX流程节点编辑器应用程序主入口
 */
public class CcJobGuiApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(CcJobGuiApplication.class);
    
    private Stage primaryStage;
    private Stage loginStage;
    
    @Override
    public void init() throws Exception {
        super.init();
        logger.info("=================================");
        logger.info("NodeFx 初始化中...");
        logger.info("=================================");
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        
        try {
            // 尝试从本地文件加载会话
            boolean sessionLoaded = SessionManager.getInstance().loadSessionFromFile();
            
            if (sessionLoaded && SessionManager.getInstance().isLoggedIn()) {
                // 如果会话加载成功且用户已登录，直接显示主窗口
                String username = SessionManager.getInstance().getUsername();
                String token = SessionManager.getInstance().getToken();
                if (username != null && !username.isEmpty() && token != null && !token.isEmpty()) {
                    logger.info("🎉 自动登录成功，跳过登录界面");
                    logger.debug("  用户: {}", username);
                    showMainWindow();
                } else {
                    // 会话数据不完整，显示登录窗口
                    logger.warn("⚠ 会话数据不完整，需要重新登录");
                    SessionManager.getInstance().logout();
                    showLoginWindow();
                }
            } else {
                // 否则显示登录窗口
                logger.debug("未找到有效会话，显示登录界面");
                showLoginWindow();
            }
            
        } catch (Exception e) {
            logger.error("启动失败：{}", e.getMessage(), e);
            Platform.exit();
            System.exit(1);
        }
    }
    
    /**
     * 显示登录窗口
     */
    private void showLoginWindow() {
        logger.debug("正在创建登录界面...");
        
        // 创建登录视图
        LoginView loginView = new LoginView();
        
        // 设置登录成功回调
        loginView.setOnLoginSuccess(result -> {
            // 验证登录结果
            if (result == null || !result.isSuccess() || result.getToken() == null || result.getToken().isEmpty()) {
                logger.error("✗ 登录结果无效，无法进入主页面");
                return;
            }
            
            // 保存会话信息
            SessionManager.getInstance().login(
                result.getToken(),
                result.getUserId(),
                result.getUsername()
            );
            
            // 关闭登录窗口
            if (loginStage != null) {
                loginStage.close();
            }
            
            // 显示主窗口
            Platform.runLater(this::showMainWindow);
        });
        
        // 设置注册回调
        loginView.setOnRegister(() -> {
            // 切换到注册界面
            showRegisterWindow();
        });
        
        // 创建登录场景
        Scene loginScene = new Scene(loginView, 900, 600);
        
        // 加载全局CSS样式
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            loginScene.getStylesheets().add(css);
        } catch (Exception e) {
            logger.warn("⚠ 样式表加载失败: {}", e.getMessage());
        }
        
        // 创建登录窗口
        loginStage = new Stage();
        loginStage.setTitle("NodeFx - 用户登录");
        loginStage.setScene(loginScene);
        loginStage.setResizable(false);
        loginStage.initStyle(StageStyle.UNDECORATED); // 无边框窗口
        
        // 窗口关闭事件
        loginStage.setOnCloseRequest(event -> {
            logger.info("用户取消登录，退出应用");
            Platform.exit();
            System.exit(0);
        });
        
        // 显示登录窗口
        loginStage.show();
        
        logger.info("✓ 登录界面已显示");
    }
    
    /**
     * 显示注册窗口
     */
    private void showRegisterWindow() {
        logger.debug("正在创建注册界面...");
        
        // 创建注册视图
        RegisterView registerView = new RegisterView();
        
        // 设置注册成功回调
        registerView.setOnRegisterSuccess(result -> {
            // 验证注册结果
            if (result == null || !result.isSuccess() || result.getToken() == null || result.getToken().isEmpty()) {
                logger.error("✗ 注册结果无效，无法进入主页面");
                return;
            }
            
            // 保存会话信息
            SessionManager.getInstance().login(
                result.getToken(),
                result.getUserId(),
                result.getUsername()
            );
            
            // 关闭注册窗口
            if (loginStage != null) {
                loginStage.close();
            }
            
            // 显示主窗口
            Platform.runLater(() -> showMainWindow());
        });
        
        // 设置返回回调
        registerView.setOnBack(() -> {
            // 返回登录界面
            showLoginWindow();
        });
        
        // 创建注册场景
        Scene registerScene = new Scene(registerView, 900, 600);
        
        // 加载全局CSS样式
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            registerScene.getStylesheets().add(css);
        } catch (Exception e) {
            logger.warn("⚠ 样式表加载失败: {}", e.getMessage());
        }
        
        // 更新窗口内容
        loginStage.setTitle("NodeFx - 用户注册");
        loginStage.setScene(registerScene);
        
        logger.info("✓ 注册界面已显示");
    }
    
    /**
     * 显示主窗口
     */
    private void showMainWindow() {
        try {
            logger.debug("正在创建主界面...");
            
            // 创建主视图
            MainView mainView = new MainView();
            
            // 创建场景
            Scene scene = new Scene(mainView, 1400, 900);
            
            // 加载全局CSS样式
            try {
                String css = getClass().getResource("/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
                logger.debug("✓ 样式表加载成功");
            } catch (Exception e) {
                logger.warn("⚠ 样式表加载失败: {}", e.getMessage());
            }
            
            // 设置窗口标题，显示用户名
            String username = SessionManager.getInstance().getUsername();
            primaryStage.setTitle("CC_ETL - 流程节点编辑器 [" + username + "]");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            // 窗口关闭事件
            primaryStage.setOnCloseRequest(event -> {
                logger.info("正在关闭应用...");
                
                // ⭐ 重要：关闭前立即同步所有待更新的节点状态到数据库
                logger.info("⏳ 正在同步节点状态到数据库...");
                com.cc.job.gui.util.NodeStatusSyncManager.getInstance().shutdown();
                logger.info("✓ 节点状态已同步");
                
                // 退出登录
                SessionManager.getInstance().logout();
                
                Platform.exit();
                System.exit(0);
            });
            
            // 显示窗口
            primaryStage.show();
            
            logger.info("=================================");
            logger.info("✓ NodeFx 启动成功！");
            logger.info("=================================");
            logger.info("");
            logger.info("功能说明:");
            logger.info("  - 拖动节点：左键按住节点拖动");
            logger.info("  - 创建连接：按住节点上的连接点拖到另一节点");
            logger.info("  - 右键菜单：右键点击节点或连线查看选项");
            logger.info("  - 删除节点：右键节点 → 删除节点");
            logger.info("  - 删除连线：右键连线 → 删除连线");
            logger.info("  - 更改样式：右键连线 → 更改样式");
            logger.info("=================================");
            
        } catch (Exception e) {
            logger.error("主界面创建失败：{}", e.getMessage(), e);
            Platform.exit();
            System.exit(1);
        }
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("NodeFx 正在停止...");
        
        // ⭐ 确保所有待更新的节点状态都已同步到数据库
        try {
            com.cc.job.gui.util.NodeStatusSyncManager.getInstance().shutdown();
            logger.info("✓ 节点状态同步完成");
        } catch (Exception e) {
            logger.warn("⚠ 节点状态同步失败: {}", e.getMessage(), e);
        }
        
        logger.info("NodeFx 已停止");
        super.stop();
    }
    
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("javafx.macosx.embedded", "false");
        System.setProperty("glass.accessible.force", "false");
        
        Logger logger = LoggerFactory.getLogger(CcJobGuiApplication.class);
        logger.info("=================================");
        logger.info("启动 NodeFx 应用程序...");
        logger.info("Java 版本: {}", System.getProperty("java.version"));
        logger.info("JavaFX 版本: 21.0.1");
        logger.info("操作系统: {}", System.getProperty("os.name"));
        logger.info("=================================");
        
        try {
            launch(args);
        } catch (Exception e) {
            logger.error("应用启动失败：{}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
