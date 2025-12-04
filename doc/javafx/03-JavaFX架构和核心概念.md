# 03-JavaFX架构和核心概念

## 3.1 JavaFX架构概述

JavaFX采用现代化的分层架构设计，充分利用硬件加速，提供流畅的用户体验。

### 3.1.1 架构层次图

```
┌─────────────────────────────────────────┐
│          JavaFX Public APIs             │  (应用层)
│  ┌──────────────────────────────────┐  │
│  │ javafx.stage, javafx.scene, ...  │  │
│  └──────────────────────────────────┘  │
├─────────────────────────────────────────┤
│         Scene Graph (场景图)            │  (内容层)
│  ┌──────────────────────────────────┐  │
│  │  Node树形结构、CSS、动画         │  │
│  └──────────────────────────────────┘  │
├─────────────────────────────────────────┤
│      Prism (图形渲染引擎)               │  (渲染层)
│  ┌──────────────────────────────────┐  │
│  │  硬件加速渲染（OpenGL/DirectX）  │  │
│  └──────────────────────────────────┘  │
├─────────────────────────────────────────┤
│      Glass (窗口工具包)                 │  (系统层)
│  ┌──────────────────────────────────┐  │
│  │  平台相关窗口管理、事件处理      │  │
│  └──────────────────────────────────┘  │
├─────────────────────────────────────────┤
│          Media & Web                    │  (媒体层)
│  ┌──────────────────────────────────┐  │
│  │  GStreamer、WebKit               │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### 3.1.2 核心组件说明

1. **Scene Graph（场景图）**：树形的节点结构，表示UI的所有元素
2. **Prism**：高性能图形渲染引擎，支持2D/3D硬件加速
3. **Glass**：平台相关的窗口工具包，处理本地事件
4. **Media**：多媒体播放引擎，基于GStreamer
5. **Web**：Web渲染引擎，基于WebKit

## 3.2 场景图（Scene Graph）

场景图是JavaFX最核心的概念，它是一个树形的数据结构，代表了应用程序UI的所有可视内容。

### 3.2.1 场景图结构

```
Stage (窗口)
  └── Scene (场景)
        └── Root Node (根节点 - Parent)
              ├── Node 1 (Parent)
              │     ├── Node 1.1 (Leaf)
              │     ├── Node 1.2 (Leaf)
              │     └── Node 1.3 (Parent)
              │           └── Node 1.3.1 (Leaf)
              └── Node 2 (Leaf)
```

### 3.2.2 场景图示例

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SceneGraphDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 创建场景图
        // 根节点：BorderPane
        BorderPane root = new BorderPane();
        
        // 顶部区域：HBox包含标题
        Label title = new Label("场景图演示");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox topBox = new HBox(title);
        topBox.setStyle("-fx-padding: 10; -fx-background-color: lightblue;");
        
        // 中心区域：VBox包含多个按钮
        VBox centerBox = new VBox(10);
        centerBox.setStyle("-fx-padding: 20; -fx-alignment: center;");
        Button btn1 = new Button("按钮 1");
        Button btn2 = new Button("按钮 2");
        Button btn3 = new Button("按钮 3");
        centerBox.getChildren().addAll(btn1, btn2, btn3);
        
        // 底部区域：状态标签
        Label statusLabel = new Label("就绪");
        HBox bottomBox = new HBox(statusLabel);
        bottomBox.setStyle("-fx-padding: 5; -fx-background-color: lightgray;");
        
        // 组装场景图
        root.setTop(topBox);
        root.setCenter(centerBox);
        root.setBottom(bottomBox);
        
        // 打印场景图结构
        printSceneGraph(root, 0);
        
        // 创建场景并显示
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Scene Graph Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * 递归打印场景图结构
     */
    private void printSceneGraph(javafx.scene.Node node, int level) {
        String indent = "  ".repeat(level);
        System.out.println(indent + node.getClass().getSimpleName());
        
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                printSceneGraph(child, level + 1);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

**输出的场景图结构**：

```
BorderPane
  HBox
    Label
  VBox
    Button
    Button
    Button
  HBox
    Label
```

## 3.3 节点（Node）体系

Node是场景图中所有元素的基类，理解Node的继承体系对于掌握JavaFX至关重要。

### 3.3.1 Node类层次结构

```
java.lang.Object
  └── javafx.scene.Node (抽象基类)
        ├── javafx.scene.Parent (可包含子节点)
        │     ├── javafx.scene.layout.Region (可调整大小的容器)
        │     │     ├── javafx.scene.layout.Pane (布局容器)
        │     │     │     ├── BorderPane
        │     │     │     ├── HBox / VBox
        │     │     │     ├── GridPane
        │     │     │     ├── StackPane
        │     │     │     └── ...
        │     │     ├── javafx.scene.control.Control (控件基类)
        │     │     │     ├── Button
        │     │     │     ├── Label
        │     │     │     ├── TextField
        │     │     │     └── ...
        │     │     └── ...
        │     ├── javafx.scene.Group (固定大小容器)
        │     └── javafx.scene.web.WebView
        ├── javafx.scene.shape.Shape (形状)
        │     ├── Circle
        │     ├── Rectangle
        │     ├── Line
        │     └── ...
        ├── javafx.scene.image.ImageView (图像)
        ├── javafx.scene.canvas.Canvas (画布)
        ├── javafx.scene.media.MediaView (媒体)
        └── ...
```

### 3.3.2 Node的核心属性

所有Node都具有以下核心属性：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class NodePropertiesDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Button button = new Button("示例按钮");
        
        // ===== 位置和尺寸 =====
        button.setLayoutX(100);           // X坐标
        button.setLayoutY(50);            // Y坐标
        button.setPrefWidth(200);         // 首选宽度
        button.setPrefHeight(50);         // 首选高度
        
        // ===== 变换 =====
        button.setTranslateX(20);         // X平移
        button.setTranslateY(10);         // Y平移
        button.setScaleX(1.2);            // X缩放
        button.setScaleY(1.2);            // Y缩放
        button.setRotate(10);             // 旋转角度
        
        // ===== 透明度和可见性 =====
        button.setOpacity(0.9);           // 透明度 (0.0-1.0)
        button.setVisible(true);          // 是否可见
        
        // ===== 鼠标和焦点 =====
        button.setDisable(false);         // 是否禁用
        button.setMouseTransparent(false); // 是否鼠标穿透
        button.setFocusTraversable(true); // 是否可获得焦点
        
        // ===== 样式 =====
        button.setStyle(
            "-fx-background-color: #4CAF50; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px;"
        );
        
        // ===== ID和样式类 =====
        button.setId("myButton");
        button.getStyleClass().add("custom-button");
        
        // ===== 效果 =====
        // 阴影效果
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setRadius(10);
        shadow.setOffsetX(3);
        shadow.setOffsetY(3);
        button.setEffect(shadow);
        
        // 创建根节点（不使用LayoutX/Y）
        StackPane root = new StackPane(button);
        
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Node 属性演示");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 打印节点信息
        printNodeInfo(button);
    }

    private void printNodeInfo(javafx.scene.Node node) {
        System.out.println("=== Node 信息 ===");
        System.out.println("类型: " + node.getClass().getSimpleName());
        System.out.println("ID: " + node.getId());
        System.out.println("样式类: " + node.getStyleClass());
        System.out.println("位置: (" + node.getLayoutX() + ", " + node.getLayoutY() + ")");
        System.out.println("尺寸: " + node.getBoundsInLocal().getWidth() + " x " + 
                           node.getBoundsInLocal().getHeight());
        System.out.println("透明度: " + node.getOpacity());
        System.out.println("可见: " + node.isVisible());
        System.out.println("禁用: " + node.isDisabled());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 3.3.3 Parent节点

**Parent** 是所有可包含子节点的容器的基类。

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ParentNodeDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Parent节点：VBox
        VBox parent = new VBox(10);
        parent.setStyle("-fx-padding: 20; -fx-background-color: lightyellow;");
        
        // 添加子节点
        parent.getChildren().add(new Label("标签1"));
        parent.getChildren().add(new Button("按钮1"));
        parent.getChildren().addAll(
            new Label("标签2"),
            new Button("按钮2")
        );
        
        // 访问子节点
        System.out.println("子节点数量: " + parent.getChildren().size());
        
        // 遍历子节点
        for (javafx.scene.Node child : parent.getChildren()) {
            System.out.println("子节点: " + child.getClass().getSimpleName());
        }
        
        // 移除子节点
        // parent.getChildren().remove(0);  // 移除第一个
        // parent.getChildren().clear();    // 移除所有
        
        // 查找子节点
        javafx.scene.Node firstButton = parent.getChildren().stream()
            .filter(node -> node instanceof Button)
            .findFirst()
            .orElse(null);
        System.out.println("找到的第一个按钮: " + 
                           (firstButton != null ? ((Button)firstButton).getText() : "无"));
        
        Scene scene = new Scene(parent, 300, 250);
        primaryStage.setTitle("Parent Node Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 3.3.4 Group节点

**Group** 是一个特殊的容器，它会根据子节点自动调整大小。

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class GroupDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 创建Group
        Group group = new Group();
        
        // 添加形状
        Rectangle rect = new Rectangle(50, 50, 100, 80);
        rect.setFill(Color.LIGHTBLUE);
        
        Circle circle = new Circle(200, 90, 40);
        circle.setFill(Color.LIGHTCORAL);
        
        group.getChildren().addAll(rect, circle);
        
        // Group会自动调整边界以适应所有子节点
        System.out.println("Group边界: " + group.getBoundsInLocal());
        
        Scene scene = new Scene(group, 400, 300);
        scene.setFill(Color.LIGHTYELLOW);
        primaryStage.setTitle("Group Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 3.4 JavaFX线程模型

JavaFX采用单线程模型，所有UI操作必须在**JavaFX Application Thread**上执行。

### 3.4.1 线程规则

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ThreadModelDemo extends Application {

    private Label statusLabel;
    private int count = 0;

    @Override
    public void start(Stage primaryStage) {
        statusLabel = new Label("状态: 就绪");
        
        // 正确方式：在JavaFX线程上更新UI
        Button correctButton = new Button("正确的方式");
        correctButton.setOnAction(e -> {
            System.out.println("当前线程: " + Thread.currentThread().getName());
            // 直接更新UI（已在JavaFX线程）
            statusLabel.setText("状态: 更新成功 " + (++count));
        });
        
        // 错误方式演示（仅用于说明，不要在实际代码中这样做）
        Button wrongButton = new Button("错误的方式（新线程更新UI）");
        wrongButton.setOnAction(e -> {
            new Thread(() -> {
                System.out.println("当前线程: " + Thread.currentThread().getName());
                // 错误！在非JavaFX线程更新UI
                try {
                    statusLabel.setText("这会导致问题！");
                } catch (Exception ex) {
                    System.err.println("错误: " + ex.getMessage());
                }
            }).start();
        });
        
        // 正确方式：使用Platform.runLater()
        Button platformButton = new Button("后台任务+Platform.runLater()");
        platformButton.setOnAction(e -> {
            statusLabel.setText("状态: 处理中...");
            
            // 在新线程执行耗时任务
            new Thread(() -> {
                try {
                    System.out.println("后台线程: " + Thread.currentThread().getName());
                    // 模拟耗时操作
                    Thread.sleep(2000);
                    
                    // 使用Platform.runLater()在JavaFX线程更新UI
                    Platform.runLater(() -> {
                        System.out.println("更新UI线程: " + Thread.currentThread().getName());
                        statusLabel.setText("状态: 处理完成！" + (++count));
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
        
        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20;");
        root.getChildren().addAll(statusLabel, correctButton, wrongButton, platformButton);
        
        Scene scene = new Scene(root, 400, 250);
        primaryStage.setTitle("线程模型演示");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 3.4.2 线程模型关键点

1. **JavaFX Application Thread**：唯一可以访问活跃场景图的线程
2. **Platform.runLater()**：将任务提交到JavaFX线程执行
3. **后台任务**：耗时操作应在后台线程执行，完成后通过`runLater()`更新UI
4. **Thread Safety**：JavaFX的Scene Graph不是线程安全的

### 3.4.3 检查当前线程

```java
// 检查是否在JavaFX Application Thread
if (Platform.isFxApplicationThread()) {
    System.out.println("在JavaFX线程");
} else {
    System.out.println("不在JavaFX线程");
}
```

## 3.5 坐标系统

JavaFX使用多种坐标系统来定位和变换节点。

### 3.5.1 坐标系统类型

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CoordinateSystemDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Button button = new Button("示例按钮");
        button.setTranslateX(50);
        button.setTranslateY(30);
        button.setScaleX(1.5);
        button.setScaleY(1.5);
        
        StackPane root = new StackPane(button);
        Scene scene = new Scene(root, 400, 300);
        
        primaryStage.setTitle("坐标系统演示");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 不同坐标系统的边界
        System.out.println("=== 坐标系统 ===");
        
        // 1. 本地坐标系（Local Bounds）
        Bounds localBounds = button.getBoundsInLocal();
        System.out.println("本地边界: " + localBounds);
        System.out.println("  宽度: " + localBounds.getWidth());
        System.out.println("  高度: " + localBounds.getHeight());
        
        // 2. 父坐标系（Parent Bounds）
        Bounds parentBounds = button.getBoundsInParent();
        System.out.println("父坐标系边界: " + parentBounds);
        System.out.println("  包含变换后的位置和大小");
        
        // 3. 场景坐标系（Scene Bounds）
        Bounds sceneBounds = button.localToScene(button.getBoundsInLocal());
        System.out.println("场景坐标系边界: " + sceneBounds);
        
        // 坐标转换
        javafx.geometry.Point2D localPoint = new javafx.geometry.Point2D(0, 0);
        javafx.geometry.Point2D scenePoint = button.localToScene(localPoint);
        System.out.println("\n本地坐标(0,0)转换为场景坐标: " + scenePoint);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 3.5.2 坐标系统说明

1. **Local Bounds（本地边界）**：
   - 节点自身的坐标系
   - 不受变换影响
   - 起点为(0, 0)

2. **Parent Bounds（父边界）**：
   - 相对于父节点的坐标系
   - 包含所有变换（平移、旋转、缩放）

3. **Scene Bounds（场景边界）**：
   - 相对于场景的坐标系
   - 全局坐标

## 3.6 小结

本章深入学习了JavaFX的架构和核心概念：

1. ✅ **架构层次**：从API层到渲染层的完整架构
2. ✅ **场景图**：树形结构的UI表示
3. ✅ **Node体系**：所有可视元素的基类及继承关系
4. ✅ **线程模型**：单线程UI模型和`Platform.runLater()`
5. ✅ **坐标系统**：Local、Parent、Scene三种坐标系

理解这些核心概念是掌握JavaFX的基础，为后续学习复杂功能打下坚实基础。

---

**练习题**

1. 创建一个程序，打印出完整的场景图结构（包括层级关系）
2. 实现一个按钮，点击后在后台线程模拟耗时操作（3秒），然后更新UI显示结果
3. 创建一个形状，显示其在不同坐标系统下的边界信息
4. 尝试在非JavaFX线程更新UI，观察会发生什么（注意：这可能导致异常）

