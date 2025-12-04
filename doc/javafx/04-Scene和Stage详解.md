# 04-Scene和Stage详解

## 4.1 Stage（舞台）详解

Stage是JavaFX应用程序的顶级容器，代表一个窗口。每个JavaFX应用至少有一个主Stage（Primary Stage）。

### 4.1.1 Stage的基本属性

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class StagePropertiesDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // ===== 基本属性 =====
        primaryStage.setTitle("Stage属性演示");
        
        // 窗口尺寸
        primaryStage.setWidth(600);
        primaryStage.setHeight(400);
        
        // 窗口位置
        primaryStage.setX(100);  // 距离屏幕左边距离
        primaryStage.setY(100);  // 距离屏幕顶部距离
        
        // 或者使用居中
        // primaryStage.centerOnScreen();
        
        // 最小/最大尺寸
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(300);
        primaryStage.setMaxWidth(1200);
        primaryStage.setMaxHeight(800);
        
        // 是否可调整大小
        primaryStage.setResizable(true);
        
        // 窗口总在最前
        primaryStage.setAlwaysOnTop(false);
        
        // 窗口图标
        primaryStage.getIcons().add(
            new javafx.scene.image.Image("https://openjfx.io/images/logo.png")
        );
        
        // ===== 窗口状态 =====
        // primaryStage.setMaximized(true);   // 最大化
        // primaryStage.setFullScreen(true);  // 全屏
        // primaryStage.setIconified(true);   // 最小化
        
        // 创建UI
        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        Button printInfoBtn = new Button("打印窗口信息");
        printInfoBtn.setOnAction(e -> printStageInfo(primaryStage));
        
        Button centerBtn = new Button("窗口居中");
        centerBtn.setOnAction(e -> primaryStage.centerOnScreen());
        
        Button maximizeBtn = new Button("最大化/还原");
        maximizeBtn.setOnAction(e -> 
            primaryStage.setMaximized(!primaryStage.isMaximized())
        );
        
        Button fullScreenBtn = new Button("全屏/退出全屏");
        fullScreenBtn.setOnAction(e -> 
            primaryStage.setFullScreen(!primaryStage.isFullScreen())
        );
        
        root.getChildren().addAll(printInfoBtn, centerBtn, maximizeBtn, fullScreenBtn);
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 初始打印信息
        printStageInfo(primaryStage);
    }

    private void printStageInfo(Stage stage) {
        System.out.println("\n=== Stage 信息 ===");
        System.out.println("标题: " + stage.getTitle());
        System.out.println("位置: (" + stage.getX() + ", " + stage.getY() + ")");
        System.out.println("尺寸: " + stage.getWidth() + " x " + stage.getHeight());
        System.out.println("最大化: " + stage.isMaximized());
        System.out.println("全屏: " + stage.isFullScreen());
        System.out.println("最小化: " + stage.isIconified());
        System.out.println("可调整大小: " + stage.isResizable());
        System.out.println("总在最前: " + stage.isAlwaysOnTop());
        System.out.println("显示中: " + stage.isShowing());
        
        // 屏幕信息
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        System.out.println("\n=== 屏幕信息 ===");
        System.out.println("屏幕尺寸: " + screenBounds.getWidth() + " x " + 
                           screenBounds.getHeight());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 4.1.2 Stage样式（StageStyle）

JavaFX提供了多种窗口样式：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class StageStyleDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30;");
        
        Label title = new Label("Stage样式演示");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // 不同样式的按钮
        Button decoratedBtn = new Button("DECORATED（默认）");
        decoratedBtn.setOnAction(e -> openStyledWindow(StageStyle.DECORATED));
        
        Button undecoratedBtn = new Button("UNDECORATED（无装饰）");
        undecoratedBtn.setOnAction(e -> openStyledWindow(StageStyle.UNDECORATED));
        
        Button transparentBtn = new Button("TRANSPARENT（透明）");
        transparentBtn.setOnAction(e -> openStyledWindow(StageStyle.TRANSPARENT));
        
        Button utilityBtn = new Button("UTILITY（工具窗口）");
        utilityBtn.setOnAction(e -> openStyledWindow(StageStyle.UTILITY));
        
        Button unifiedBtn = new Button("UNIFIED（统一样式-macOS）");
        unifiedBtn.setOnAction(e -> openStyledWindow(StageStyle.UNIFIED));
        
        root.getChildren().addAll(title, decoratedBtn, undecoratedBtn, 
                                  transparentBtn, utilityBtn, unifiedBtn);
        
        Scene scene = new Scene(root, 400, 350);
        primaryStage.setTitle("Stage样式演示");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openStyledWindow(StageStyle style) {
        Stage stage = new Stage();
        stage.initStyle(style);  // 必须在show()之前设置
        
        Label label = new Label("样式: " + style);
        label.setStyle("-fx-font-size: 16px;");
        
        Button closeBtn = new Button("关闭");
        closeBtn.setOnAction(e -> stage.close());
        
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 40; -fx-background-color: lightblue;");
        layout.getChildren().addAll(label, closeBtn);
        
        Scene scene = new Scene(layout, 300, 200);
        
        // 透明样式需要透明背景
        if (style == StageStyle.TRANSPARENT) {
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            layout.setStyle("-fx-padding: 40; -fx-background-color: rgba(173, 216, 230, 0.8); " +
                           "-fx-background-radius: 20;");
        }
        
        stage.setScene(scene);
        stage.setTitle(style.toString());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

**StageStyle说明**：

| 样式 | 描述 | 特点 |
|------|------|------|
| DECORATED | 默认样式 | 带标题栏、边框、最小化/最大化/关闭按钮 |
| UNDECORATED | 无装饰 | 无标题栏和边框，可以自定义 |
| TRANSPARENT | 透明 | 窗口背景透明，需配合透明场景 |
| UTILITY | 工具窗口 | 简化的标题栏，适合工具面板 |
| UNIFIED | 统一样式 | macOS专用，标题栏与内容区域融合 |

### 4.1.3 自定义无装饰窗口

创建一个可拖动的自定义窗口：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CustomWindowDemo extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        
        BorderPane root = new BorderPane();
        
        // ===== 自定义标题栏 =====
        HBox titleBar = new HBox();
        titleBar.setStyle("-fx-background-color: #2196F3; -fx-padding: 10;");
        
        Label titleLabel = new Label("自定义窗口");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        // 占位区域（使按钮靠右）
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 窗口控制按钮
        Button minimizeBtn = new Button("_");
        minimizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        minimizeBtn.setOnAction(e -> primaryStage.setIconified(true));
        
        Button maximizeBtn = new Button("□");
        maximizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        maximizeBtn.setOnAction(e -> primaryStage.setMaximized(!primaryStage.isMaximized()));
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> primaryStage.close());
        
        titleBar.getChildren().addAll(titleLabel, spacer, minimizeBtn, maximizeBtn, closeBtn);
        
        // ===== 使标题栏可拖动 =====
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        titleBar.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });
        
        // ===== 内容区域 =====
        Label content = new Label("这是一个自定义的无装饰窗口\n可以拖动标题栏移动窗口");
        content.setStyle("-fx-font-size: 16px;");
        BorderPane.setMargin(content, new Insets(50));
        
        root.setTop(titleBar);
        root.setCenter(content);
        root.setStyle("-fx-border-color: #2196F3; -fx-border-width: 2;");
        
        Scene scene = new Scene(root, 500, 350);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 4.1.4 Stage的模态性（Modality）

模态窗口会阻塞其他窗口的交互：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ModalityDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30;");
        
        Label title = new Label("主窗口");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        Button noneBtn = new Button("打开非模态窗口");
        noneBtn.setOnAction(e -> 
            openModalWindow(primaryStage, Modality.NONE, "非模态窗口\n可以操作其他窗口")
        );
        
        Button windowModalBtn = new Button("打开窗口模态窗口");
        windowModalBtn.setOnAction(e -> 
            openModalWindow(primaryStage, Modality.WINDOW_MODAL, 
                           "窗口模态\n必须先关闭此窗口才能操作主窗口")
        );
        
        Button appModalBtn = new Button("打开应用模态窗口");
        appModalBtn.setOnAction(e -> 
            openModalWindow(primaryStage, Modality.APPLICATION_MODAL, 
                           "应用模态\n必须先关闭此窗口才能操作应用的任何窗口")
        );
        
        root.getChildren().addAll(title, noneBtn, windowModalBtn, appModalBtn);
        
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("模态性演示");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openModalWindow(Stage owner, Modality modality, String message) {
        Stage modalStage = new Stage();
        modalStage.initModality(modality);  // 设置模态性
        modalStage.initOwner(owner);         // 设置所有者
        
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 14px; -fx-text-alignment: center;");
        
        Button closeBtn = new Button("关闭");
        closeBtn.setOnAction(e -> modalStage.close());
        
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 30;");
        layout.getChildren().addAll(label, closeBtn);
        
        Scene scene = new Scene(layout, 350, 200);
        modalStage.setScene(scene);
        modalStage.setTitle(modality.toString());
        
        if (modality == Modality.APPLICATION_MODAL || modality == Modality.WINDOW_MODAL) {
            modalStage.showAndWait();  // 阻塞等待
        } else {
            modalStage.show();         // 非阻塞
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

**Modality类型**：

| 类型 | 描述 | 使用场景 |
|------|------|----------|
| NONE | 非模态 | 独立窗口，如工具面板 |
| WINDOW_MODAL | 窗口模态 | 对话框，阻塞父窗口 |
| APPLICATION_MODAL | 应用模态 | 关键对话框，阻塞整个应用 |

## 4.2 Scene（场景）详解

Scene是容纳UI内容的容器，连接Stage和场景图的根节点。

### 4.2.1 Scene的基本属性

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ScenePropertiesDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label infoLabel = new Label("场景信息将显示在控制台");
        
        Button printBtn = new Button("打印场景信息");
        
        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");
        root.getChildren().addAll(infoLabel, printBtn);
        
        // 创建场景
        Scene scene = new Scene(root, 600, 400);
        
        // 场景属性
        scene.setFill(Color.LIGHTGRAY);  // 背景颜色
        scene.setCursor(Cursor.HAND);     // 鼠标光标
        
        // 加载CSS样式表
        // scene.getStylesheets().add("styles/main.css");
        
        printBtn.setOnAction(e -> printSceneInfo(scene));
        
        primaryStage.setTitle("Scene属性演示");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 初始打印
        printSceneInfo(scene);
        
        // 监听场景尺寸变化
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("场景宽度变化: " + oldVal + " -> " + newVal);
        });
        
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("场景高度变化: " + oldVal + " -> " + newVal);
        });
    }

    private void printSceneInfo(Scene scene) {
        System.out.println("\n=== Scene 信息 ===");
        System.out.println("尺寸: " + scene.getWidth() + " x " + scene.getHeight());
        System.out.println("背景色: " + scene.getFill());
        System.out.println("光标: " + scene.getCursor());
        System.out.println("根节点: " + scene.getRoot().getClass().getSimpleName());
        System.out.println("样式表: " + scene.getStylesheets());
        System.out.println("聚焦节点: " + scene.getFocusOwner());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 4.2.2 场景切换

一个Stage可以切换不同的Scene：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class SceneSwitchDemo extends Application {

    private Scene scene1, scene2, scene3;
    private int currentScene = 1;

    @Override
    public void start(Stage primaryStage) {
        // 场景1 - 蓝色主题
        scene1 = createScene("场景 1", Color.LIGHTBLUE, primaryStage);
        
        // 场景2 - 绿色主题
        scene2 = createScene("场景 2", Color.LIGHTGREEN, primaryStage);
        
        // 场景3 - 粉色主题
        scene3 = createScene("场景 3", Color.LIGHTPINK, primaryStage);
        
        primaryStage.setTitle("场景切换演示");
        primaryStage.setScene(scene1);
        primaryStage.setWidth(400);
        primaryStage.setHeight(300);
        primaryStage.show();
    }

    private Scene createScene(String title, Color bgColor, Stage stage) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Button nextBtn = new Button("下一个场景");
        nextBtn.setOnAction(e -> switchToNextScene(stage));
        
        Button prevBtn = new Button("上一个场景");
        prevBtn.setOnAction(e -> switchToPrevScene(stage));
        
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(label, nextBtn, prevBtn);
        
        Scene scene = new Scene(layout, 400, 300);
        scene.setFill(bgColor);
        
        return scene;
    }

    private void switchToNextScene(Stage stage) {
        currentScene = (currentScene % 3) + 1;
        switch (currentScene) {
            case 1: stage.setScene(scene1); break;
            case 2: stage.setScene(scene2); break;
            case 3: stage.setScene(scene3); break;
        }
    }

    private void switchToPrevScene(Stage stage) {
        currentScene = (currentScene + 1) % 3 + 1;
        switch (currentScene) {
            case 1: stage.setScene(scene1); break;
            case 2: stage.setScene(scene2); break;
            case 3: stage.setScene(scene3); break;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 4.2.3 场景快照（截图）

可以对场景或节点进行截图：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class SceneSnapshotDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label title = new Label("场景截图演示");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        Button snapshotBtn = new Button("截图并保存");
        snapshotBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        
        Label statusLabel = new Label("点击按钮进行截图");
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 40; -fx-background-color: linear-gradient(to bottom, #667eea, #764ba2);");
        root.getChildren().addAll(title, snapshotBtn, statusLabel);
        
        snapshotBtn.setOnAction(e -> {
            try {
                // 截图
                WritableImage snapshot = root.snapshot(new SnapshotParameters(), null);
                
                // 保存为PNG文件
                File file = new File("scene-snapshot.png");
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                
                statusLabel.setText("截图已保存: " + file.getAbsolutePath());
                System.out.println("截图保存成功: " + file.getAbsolutePath());
            } catch (IOException ex) {
                statusLabel.setText("保存失败: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        Scene scene = new Scene(root, 500, 350);
        primaryStage.setTitle("Scene Snapshot Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 4.3 窗口事件处理

Stage和Scene都支持各种事件监听。

### 4.3.1 窗口生命周期事件

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class WindowEventsDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("窗口事件演示\n查看控制台输出");
        label.setStyle("-fx-font-size: 16px;");
        
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 300);
        
        // ===== Stage事件 =====
        
        // 窗口显示事件
        primaryStage.setOnShowing(e -> 
            System.out.println("事件: 窗口即将显示")
        );
        
        primaryStage.setOnShown(e -> 
            System.out.println("事件: 窗口已显示")
        );
        
        // 窗口隐藏事件
        primaryStage.setOnHiding(e -> 
            System.out.println("事件: 窗口即将隐藏")
        );
        
        primaryStage.setOnHidden(e -> 
            System.out.println("事件: 窗口已隐藏")
        );
        
        // 窗口关闭请求
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("事件: 收到关闭请求");
            // 可以阻止关闭
            // e.consume();
            // Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要关闭吗？");
            // if (alert.showAndWait().get() != ButtonType.OK) {
            //     e.consume();
            // }
        });
        
        // ===== Scene事件 =====
        
        // 鼠标进入/离开场景
        scene.setOnMouseEntered(e -> 
            System.out.println("鼠标进入场景: (" + e.getSceneX() + ", " + e.getSceneY() + ")")
        );
        
        scene.setOnMouseExited(e -> 
            System.out.println("鼠标离开场景")
        );
        
        primaryStage.setTitle("窗口事件演示");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 4.4 多显示器支持

JavaFX提供了Screen类来处理多显示器场景：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MultiScreenDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        
        Button refreshBtn = new Button("刷新屏幕信息");
        refreshBtn.setOnAction(e -> displayScreenInfo(textArea));
        
        BorderPane root = new BorderPane();
        root.setCenter(textArea);
        root.setBottom(refreshBtn);
        
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("多显示器支持");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 初始显示
        displayScreenInfo(textArea);
    }

    private void displayScreenInfo(TextArea textArea) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 显示器信息 ===\n\n");
        
        // 获取所有屏幕
        var screens = Screen.getScreens();
        sb.append("检测到 ").append(screens.size()).append(" 个显示器\n\n");
        
        for (int i = 0; i < screens.size(); i++) {
            Screen screen = screens.get(i);
            sb.append("显示器 ").append(i + 1).append(":\n");
            
            // 屏幕边界
            Rectangle2D bounds = screen.getBounds();
            sb.append("  完整边界: ")
              .append((int)bounds.getWidth()).append(" x ").append((int)bounds.getHeight())
              .append(" at (").append((int)bounds.getMinX()).append(", ")
              .append((int)bounds.getMinY()).append(")\n");
            
            // 可视边界（去除任务栏等）
            Rectangle2D visualBounds = screen.getVisualBounds();
            sb.append("  可视边界: ")
              .append((int)visualBounds.getWidth()).append(" x ")
              .append((int)visualBounds.getHeight())
              .append(" at (").append((int)visualBounds.getMinX()).append(", ")
              .append((int)visualBounds.getMinY()).append(")\n");
            
            // DPI
            sb.append("  DPI: ").append(screen.getDpi()).append("\n");
            
            // 是否主屏幕
            if (screen == Screen.getPrimary()) {
                sb.append("  [主显示器]\n");
            }
            
            sb.append("\n");
        }
        
        textArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 4.5 小结

本章深入学习了Stage和Scene：

1. ✅ **Stage属性**：位置、尺寸、样式、模态性
2. ✅ **StageStyle**：5种窗口样式
3. ✅ **自定义窗口**：创建可拖动的无装饰窗口
4. ✅ **Scene属性**：背景、光标、样式表
5. ✅ **场景切换**：在多个场景间灵活切换
6. ✅ **事件处理**：窗口生命周期事件
7. ✅ **多显示器**：处理多显示器场景

下一章，我们将学习JavaFX的布局管理器。

---

**练习题**

1. 创建一个带自定义标题栏的窗口，可以拖动、最小化、最大化和关闭
2. 实现一个应用，包含三个不同主题的场景，可以在它们之间切换
3. 创建一个模态对话框，显示"确定"和"取消"按钮，并返回用户的选择
4. 编写程序显示所有连接的显示器信息，并提供按钮将窗口移动到不同显示器

