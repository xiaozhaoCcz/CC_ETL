package com.cc.job.gui;

import com.cc.job.gui.service.SSEService;
import com.cc.job.gui.util.NodeStatusSyncManager;
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
 * @author xiaozhao
 */
public class CcJobGuiApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(CcJobGuiApplication.class);
    
    private Stage primaryStage;
    private Stage loginStage;
    
    @Override
    public void init() throws Exception {
        super.init();
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        try {
            // 尝试从本地文件加载会话
            boolean sessionLoaded = SessionManager.getInstance().loadSessionFromFile();
            
            if (sessionLoaded && SessionManager.getInstance().isLoggedIn()) {
                // 如果会话加载成功且用户已登录，直接显示主窗口
                String username = SessionManager.getInstance().getUsername();
                String token = SessionManager.getInstance().getToken();
                if (username != null && !username.isEmpty() && token != null && !token.isEmpty()) {
                    showMainWindow();
                } else {
                    // 会话数据不完整，显示登录窗口
                    SessionManager.getInstance().logout();
                    showLoginWindow();
                }
            } else {
                // 否则显示登录窗口
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
        
        // 按当前主题加载 CSS
        try {
            String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
            if (css != null && !css.isEmpty()) {
                loginScene.getStylesheets().add(css);
            }
        } catch (Exception e) {
        }
        
        // 创建登录窗口（支持缩放，便于自适应不同屏幕）
        loginStage = new Stage();
        loginStage.setTitle("CcETL - 用户登录");
        loginStage.setScene(loginScene);
        loginStage.setResizable(true);
        loginStage.setMinWidth(400);
        loginStage.setMinHeight(500);
        loginStage.initStyle(StageStyle.UNDECORATED); // 无边框窗口
        
        // 窗口关闭事件
        loginStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        
        // 显示登录窗口
        loginStage.show();
    }
    
    /**
     * 显示注册窗口
     */
    private void showRegisterWindow() {
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
        
        // 按当前主题加载 CSS
        try {
            String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
            if (css != null && !css.isEmpty()) {
                registerScene.getStylesheets().add(css);
            }
        } catch (Exception e) {
        }
        
        // 更新窗口内容
        loginStage.setTitle("CcETL - 用户注册");
        loginStage.setScene(registerScene);
    }
    
    /**
     * 显示主窗口
     */
    private void showMainWindow() {
        try {
            // 创建主视图
            MainView mainView = new MainView();
            mainView.setHostServices(getHostServices());
            
            // 创建场景
            Scene scene = new Scene(mainView, 1920, 1080);
            
            // 按当前主题加载 CSS（与配置/ThemeManager 一致）
            try {
                String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                if (css != null && !css.isEmpty()) {
                    scene.getStylesheets().add(css);
                }
            } catch (Exception e) {
            }
            
            // 设置窗口标题，显示用户名
            String username = SessionManager.getInstance().getUsername();
            primaryStage.setTitle("CcETL - 流程节点编辑器 [" + username + "]");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            // 窗口关闭事件
            primaryStage.setOnCloseRequest(event -> {
                // ⭐ 立即阻止窗口关闭事件传播，避免JavaFX等待
                event.consume();
                
                // ⭐ 完全异步化关闭流程，不阻塞UI线程
                new Thread(() -> {
                    try {
                        // 1. 清理MainView的资源（停止所有Timer和SSE连接）
                        try {
                            MainView mainViewInstance = (MainView) scene.getRoot();
                            if (mainViewInstance != null) {
                                mainViewInstance.cleanup();
                            }
                        } catch (Exception e) {
                            logger.debug("清理MainView失败: {}", e.getMessage());
                        }
                        
                        // 2. 断开所有SSE连接（快速操作，双重保险）
                        SSEService.getInstance().disconnectAll();
                        // 关闭SSE清理线程池
                        SSEService.getInstance().shutdown();
                        
                        // 3. 快速关闭NodeStatusSyncManager（已优化，最多阻塞1秒）
                        NodeStatusSyncManager.getInstance().shutdown();
                        
                        // 4. 退出登录（快速操作）
                        SessionManager.getInstance().logout();
                        
                    } catch (Exception e) {
                        logger.error("关闭窗口时发生错误: {}", e.getMessage(), e);
                    } finally {
                        // 5. 强制退出（不等待任何操作）
                        // 使用Platform.runLater确保在JavaFX线程中执行
                        Platform.runLater(() -> {
                            Platform.exit();
                            System.exit(0);
                        });
                    }
                }, "Shutdown-Thread").start();
            });
            
            // 显示窗口
            primaryStage.show();
            
        } catch (Exception e) {
            logger.error("主界面创建失败：{}", e.getMessage(), e);
            Platform.exit();
            System.exit(1);
        }
    }
    
    @Override
    public void stop() throws Exception {
        // ⭐ 确保所有待更新的节点状态都已同步到数据库
        try {
            NodeStatusSyncManager.getInstance().shutdown();
        } catch (Exception e) {
        }
        
        super.stop();
    }
    
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("javafx.macosx.embedded", "false");
        System.setProperty("glass.accessible.force", "false");
        
        try {
            launch(args);
        } catch (Exception e) {
            logger.error("应用启动失败：{}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
