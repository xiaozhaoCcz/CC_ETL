package com.cc.job.gui;

import com.cc.job.gui.util.SessionManager;
import com.cc.job.gui.view.LoginView;
import com.cc.job.gui.view.RegisterView;
import com.cc.job.gui.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * JavaFX流程节点编辑器应用程序主入口
 */
public class CcJobGuiApplication extends Application {
    
    private Stage primaryStage;
    private Stage loginStage;
    
    @Override
    public void init() throws Exception {
        super.init();
        System.out.println("=================================");
        System.out.println("NodeFx 初始化中...");
        System.out.println("=================================");
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        try {
            // 尝试从本地文件加载会话
            boolean sessionLoaded = SessionManager.getInstance().loadSessionFromFile();
            
            if (sessionLoaded) {
                // 如果会话加载成功，直接显示主窗口
                System.out.println("🎉 自动登录成功，跳过登录界面");
                showMainWindow();
            } else {
                // 否则显示登录窗口
                showLoginWindow();
            }
            
        } catch (Exception e) {
            System.err.println("启动失败：" + e.getMessage());
            e.printStackTrace();
            Platform.exit();
            System.exit(1);
        }
    }
    
    /**
     * 显示登录窗口
     */
    private void showLoginWindow() {
        System.out.println("正在创建登录界面...");
        
        // 创建登录视图
        LoginView loginView = new LoginView();
        
        // 设置登录成功回调
        loginView.setOnLoginSuccess(result -> {
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
            Platform.runLater(() -> showMainWindow());
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
            System.err.println("⚠ 样式表加载失败: " + e.getMessage());
        }
        
        // 创建登录窗口
        loginStage = new Stage();
        loginStage.setTitle("NodeFx - 用户登录");
        loginStage.setScene(loginScene);
        loginStage.setResizable(false);
        loginStage.initStyle(StageStyle.UNDECORATED); // 无边框窗口
        
        // 窗口关闭事件
        loginStage.setOnCloseRequest(event -> {
            System.out.println("用户取消登录，退出应用");
            Platform.exit();
            System.exit(0);
        });
        
        // 显示登录窗口
        loginStage.show();
        
        System.out.println("✓ 登录界面已显示");
    }
    
    /**
     * 显示注册窗口
     */
    private void showRegisterWindow() {
        System.out.println("正在创建注册界面...");
        
        // 创建注册视图
        RegisterView registerView = new RegisterView();
        
        // 设置注册成功回调
        registerView.setOnRegisterSuccess(result -> {
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
            System.err.println("⚠ 样式表加载失败: " + e.getMessage());
        }
        
        // 更新窗口内容
        loginStage.setTitle("NodeFx - 用户注册");
        loginStage.setScene(registerScene);
        
        System.out.println("✓ 注册界面已显示");
    }
    
    /**
     * 显示主窗口
     */
    private void showMainWindow() {
        try {
            System.out.println("正在创建主界面...");
            
            // 创建主视图
            MainView mainView = new MainView();
            
            // 创建场景
            Scene scene = new Scene(mainView, 1400, 900);
            
            // 加载全局CSS样式
            try {
                String css = getClass().getResource("/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
                System.out.println("✓ 样式表加载成功");
            } catch (Exception e) {
                System.err.println("⚠ 样式表加载失败: " + e.getMessage());
            }
            
            // 设置窗口标题，显示用户名
            String username = SessionManager.getInstance().getUsername();
            primaryStage.setTitle("NodeFx - 流程节点编辑器 [" + username + "]");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            // 窗口关闭事件
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("正在关闭应用...");
                
                // ⭐ 重要：关闭前立即同步所有待更新的节点状态到数据库
                System.out.println("⏳ 正在同步节点状态到数据库...");
                com.cc.job.gui.util.NodeStatusSyncManager.getInstance().shutdown();
                System.out.println("✓ 节点状态已同步");
                
                // 退出登录
                SessionManager.getInstance().logout();
                
                Platform.exit();
                System.exit(0);
            });
            
            // 显示窗口
            primaryStage.show();
            
            System.out.println("=================================");
            System.out.println("✓ NodeFx 启动成功！");
            System.out.println("=================================");
            System.out.println("");
            System.out.println("功能说明:");
            System.out.println("  - 拖动节点：左键按住节点拖动");
            System.out.println("  - 创建连接：按住节点上的连接点拖到另一节点");
            System.out.println("  - 右键菜单：右键点击节点或连线查看选项");
            System.out.println("  - 删除节点：右键节点 → 删除节点");
            System.out.println("  - 删除连线：右键连线 → 删除连线");
            System.out.println("  - 更改样式：右键连线 → 更改样式");
            System.out.println("=================================");
            
        } catch (Exception e) {
            System.err.println("主界面创建失败：" + e.getMessage());
            e.printStackTrace();
            Platform.exit();
            System.exit(1);
        }
    }
    
    @Override
    public void stop() throws Exception {
        System.out.println("NodeFx 正在停止...");
        
        // ⭐ 确保所有待更新的节点状态都已同步到数据库
        try {
            com.cc.job.gui.util.NodeStatusSyncManager.getInstance().shutdown();
            System.out.println("✓ 节点状态同步完成");
        } catch (Exception e) {
            System.err.println("⚠ 节点状态同步失败: " + e.getMessage());
        }
        
        System.out.println("NodeFx 已停止");
        super.stop();
    }
    
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("javafx.macosx.embedded", "false");
        System.setProperty("glass.accessible.force", "false");
        
        System.out.println("=================================");
        System.out.println("启动 NodeFx 应用程序...");
        System.out.println("Java 版本: " + System.getProperty("java.version"));
        System.out.println("JavaFX 版本: 21.0.1");
        System.out.println("操作系统: " + System.getProperty("os.name"));
        System.out.println("=================================");
        
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("应用启动失败：" + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
