package com.example.nodefx;

import com.example.nodefx.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX流程节点编辑器应用程序主入口
 */
public class NodeFxApplication extends Application {
    
    @Override
    public void init() throws Exception {
        super.init();
        System.out.println("=================================");
        System.out.println("NodeFx 初始化中...");
        System.out.println("=================================");
    }
    
    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("正在创建主界面...");
            
            // 创建主视图
            MainView mainView = new MainView();
            
            // 创建场景
            Scene scene = new Scene(mainView, 1400, 900);
            
            // 设置窗口
            primaryStage.setTitle("NodeFx - 流程节点编辑器");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            // 窗口关闭事件
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("正在关闭应用...");
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
            System.err.println("启动失败：" + e.getMessage());
            e.printStackTrace();
            Platform.exit();
            System.exit(1);
        }
    }
    
    @Override
    public void stop() throws Exception {
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
