# 02-第一个JavaFX程序

## 2.1 HelloWorld程序

让我们从最简单的JavaFX程序开始，理解其基本结构。

### 2.1.1 基本代码

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloWorld extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 创建一个标签
        Label label = new Label("Hello, JavaFX!");
        
        // 创建一个布局容器
        StackPane root = new StackPane();
        root.getChildren().add(label);
        
        // 创建场景
        Scene scene = new Scene(root, 400, 300);
        
        // 设置舞台
        primaryStage.setTitle("我的第一个JavaFX程序");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 2.1.2 运行结果

运行程序后，将看到一个400x300像素的窗口，中央显示"Hello, JavaFX!"文本。

## 2.2 程序结构分析

### 2.2.1 继承Application类

```java
public class HelloWorld extends Application
```

所有JavaFX应用程序都必须继承 `javafx.application.Application` 类。

### 2.2.2 重写start方法

```java
@Override
public void start(Stage primaryStage) {
    // 应用程序的主要逻辑
}
```

`start()` 方法是JavaFX应用的入口点，相当于GUI版本的 `main()` 方法。

**参数说明**：
- `primaryStage`：主舞台（Stage），由JavaFX运行时自动创建

### 2.2.3 创建UI组件

```java
Label label = new Label("Hello, JavaFX!");
```

创建一个标签控件，用于显示文本。

### 2.2.4 创建布局容器

```java
StackPane root = new StackPane();
root.getChildren().add(label);
```

- `StackPane`：堆叠布局，子组件按照添加顺序堆叠
- `getChildren().add()`：将标签添加到布局中

### 2.2.5 创建场景

```java
Scene scene = new Scene(root, 400, 300);
```

**Scene** 是容纳所有UI内容的容器。
- 参数1：根节点（root）
- 参数2：宽度（400像素）
- 参数3：高度（300像素）

### 2.2.6 配置并显示舞台

```java
primaryStage.setTitle("我的第一个JavaFX程序");
primaryStage.setScene(scene);
primaryStage.show();
```

- `setTitle()`：设置窗口标题
- `setScene()`：将场景设置到舞台上
- `show()`：显示窗口

### 2.2.7 启动应用

```java
public static void main(String[] args) {
    launch(args);
}
```

`launch()` 方法启动JavaFX应用程序，它会：
1. 创建Application实例
2. 调用 `init()` 方法（如果重写）
3. 调用 `start()` 方法
4. 等待应用程序结束
5. 调用 `stop()` 方法（如果重写）

## 2.3 JavaFX应用生命周期

JavaFX应用程序有三个主要的生命周期方法：

### 2.3.1 完整生命周期示例

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class LifecycleDemo extends Application {

    /**
     * 初始化方法，在start()之前调用
     * 运行在启动线程，不是JavaFX应用线程
     * 可以在这里进行耗时的初始化操作
     */
    @Override
    public void init() throws Exception {
        super.init();
        System.out.println("1. init() 方法被调用");
        System.out.println("   线程: " + Thread.currentThread().getName());
        // 可以在这里加载配置、初始化数据等
    }

    /**
     * 启动方法，应用程序的主入口
     * 运行在JavaFX应用线程
     * 必须重写此方法
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("2. start() 方法被调用");
        System.out.println("   线程: " + Thread.currentThread().getName());
        
        Label label = new Label("JavaFX 生命周期演示");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 200);
        
        primaryStage.setTitle("生命周期演示");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("   窗口关闭请求");
        });
        primaryStage.show();
    }

    /**
     * 停止方法，在应用程序关闭时调用
     * 运行在JavaFX应用线程
     * 可以在这里进行清理操作
     */
    @Override
    public void stop() throws Exception {
        System.out.println("3. stop() 方法被调用");
        System.out.println("   线程: " + Thread.currentThread().getName());
        // 可以在这里关闭资源、保存数据等
        super.stop();
    }

    public static void main(String[] args) {
        System.out.println("0. main() 方法被调用");
        launch(args);
        System.out.println("4. launch() 方法返回，应用程序结束");
    }
}
```

### 2.3.2 生命周期执行顺序

运行上述程序，控制台输出：

```
0. main() 方法被调用
1. init() 方法被调用
   线程: JavaFX-Launcher
2. start() 方法被调用
   线程: JavaFX Application Thread
   窗口关闭请求
3. stop() 方法被调用
   线程: JavaFX Application Thread
4. launch() 方法返回，应用程序结束
```

**生命周期流程图**：

```
main()
  ↓
launch()
  ↓
init()      [可选，运行在后台线程]
  ↓
start()     [必须，运行在JavaFX线程]
  ↓
[应用程序运行中...]
  ↓
stop()      [可选，运行在JavaFX线程]
  ↓
退出
```

## 2.4 JavaFX程序的关键组件

### 2.4.1 Stage（舞台）

**Stage** 是JavaFX应用程序的顶级容器，代表一个窗口。

```java
// Stage 常用方法
primaryStage.setTitle("窗口标题");           // 设置标题
primaryStage.setWidth(600);                  // 设置宽度
primaryStage.setHeight(400);                 // 设置高度
primaryStage.setResizable(false);            // 禁止调整大小
primaryStage.setMaximized(true);             // 最大化
primaryStage.setFullScreen(true);            // 全屏
primaryStage.initStyle(StageStyle.UTILITY);  // 设置窗口样式
```

**Stage样式**：

```java
import javafx.stage.StageStyle;

// 不同的窗口样式
primaryStage.initStyle(StageStyle.DECORATED);    // 默认，带装饰
primaryStage.initStyle(StageStyle.UNDECORATED);  // 无装饰
primaryStage.initStyle(StageStyle.TRANSPARENT);  // 透明
primaryStage.initStyle(StageStyle.UTILITY);      // 工具窗口
primaryStage.initStyle(StageStyle.UNIFIED);      // 统一样式（macOS）
```

### 2.4.2 Scene（场景）

**Scene** 是容纳UI内容的容器，一个Stage可以切换多个Scene。

```java
// Scene 构造方法
Scene scene1 = new Scene(root);                    // 自动大小
Scene scene2 = new Scene(root, 800, 600);         // 指定大小
Scene scene3 = new Scene(root, Color.LIGHTBLUE);  // 指定背景色

// Scene 常用方法
scene.setFill(Color.LIGHTGRAY);                   // 设置背景色
scene.getStylesheets().add("style.css");          // 加载CSS
```

### 2.4.3 Node（节点）

**Node** 是场景图中所有可视元素的基类，包括控件、布局、形状等。

节点层次结构：

```
Scene
  └── Root Node (Parent)
        ├── Node 1 (Parent)
        │     ├── Node 1.1 (Leaf)
        │     └── Node 1.2 (Leaf)
        └── Node 2 (Leaf)
```

## 2.5 改进的HelloWorld

让我们创建一个更实用的例子：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ImprovedHelloWorld extends Application {

    private int clickCount = 0;
    private Label label;

    @Override
    public void start(Stage primaryStage) {
        // 创建标签
        label = new Label("欢迎使用 JavaFX！");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // 创建计数标签
        Label countLabel = new Label("按钮点击次数: 0");
        
        // 创建按钮
        Button button = new Button("点击我");
        button.setOnAction(event -> {
            clickCount++;
            countLabel.setText("按钮点击次数: " + clickCount);
            
            // 改变问候语
            switch (clickCount % 3) {
                case 0:
                    label.setText("欢迎使用 JavaFX！");
                    break;
                case 1:
                    label.setText("Hello, JavaFX!");
                    break;
                case 2:
                    label.setText("你好，JavaFX！");
                    break;
            }
        });
        
        // 创建退出按钮
        Button exitButton = new Button("退出");
        exitButton.setOnAction(event -> primaryStage.close());
        
        // 创建垂直布局
        VBox root = new VBox(15);  // 15像素间距
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(label, countLabel, button, exitButton);
        
        // 创建场景
        Scene scene = new Scene(root, 400, 250);
        
        // 设置舞台
        primaryStage.setTitle("改进的 HelloWorld");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("应用程序关闭，总共点击了 " + clickCount + " 次");
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 2.5.1 程序特点

1. **交互性**：按钮点击会更新界面
2. **状态管理**：使用实例变量跟踪点击次数
3. **事件处理**：使用Lambda表达式处理按钮事件
4. **样式设置**：使用内联CSS设置字体样式
5. **布局管理**：使用VBox垂直布局，设置间距和边距

## 2.6 使用多个场景

一个应用可以有多个场景，并在它们之间切换：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MultiSceneDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 场景1
        Label label1 = new Label("这是场景 1");
        Button toScene2 = new Button("切换到场景2");
        VBox layout1 = new VBox(20);
        layout1.setAlignment(Pos.CENTER);
        layout1.getChildren().addAll(label1, toScene2);
        Scene scene1 = new Scene(layout1, 400, 300);
        
        // 场景2
        Label label2 = new Label("这是场景 2");
        Button toScene1 = new Button("返回场景1");
        VBox layout2 = new VBox(20);
        layout2.setAlignment(Pos.CENTER);
        layout2.getChildren().addAll(label2, toScene1);
        Scene scene2 = new Scene(layout2, 400, 300);
        
        // 按钮事件：切换场景
        toScene2.setOnAction(e -> primaryStage.setScene(scene2));
        toScene1.setOnAction(e -> primaryStage.setScene(scene1));
        
        // 显示窗口
        primaryStage.setTitle("多场景演示");
        primaryStage.setScene(scene1);  // 初始场景
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 2.7 创建多个窗口

JavaFX应用可以创建多个Stage（窗口）：

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

public class MultiWindowDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 主窗口
        Label label = new Label("这是主窗口");
        
        Button openNewWindow = new Button("打开新窗口");
        openNewWindow.setOnAction(e -> openNewWindow());
        
        Button openModalWindow = new Button("打开模态窗口");
        openModalWindow.setOnAction(e -> openModalWindow(primaryStage));
        
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(label, openNewWindow, openModalWindow);
        
        Scene scene = new Scene(layout, 400, 250);
        primaryStage.setTitle("主窗口");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * 打开普通新窗口（非模态）
     */
    private void openNewWindow() {
        Stage newStage = new Stage();
        
        Label label = new Label("这是一个新窗口");
        Button closeButton = new Button("关闭");
        closeButton.setOnAction(e -> newStage.close());
        
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(label, closeButton);
        
        Scene scene = new Scene(layout, 300, 200);
        newStage.setTitle("新窗口");
        newStage.setScene(scene);
        newStage.show();
    }

    /**
     * 打开模态窗口（阻塞父窗口）
     */
    private void openModalWindow(Stage owner) {
        Stage modalStage = new Stage();
        
        // 设置模态性
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initOwner(owner);
        
        Label label = new Label("这是模态窗口\n必须先关闭此窗口才能操作主窗口");
        label.setStyle("-fx-text-alignment: center;");
        
        Button closeButton = new Button("确定");
        closeButton.setOnAction(e -> modalStage.close());
        
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(label, closeButton);
        
        Scene scene = new Scene(layout, 350, 150);
        modalStage.setTitle("模态窗口");
        modalStage.setScene(scene);
        modalStage.showAndWait();  // 阻塞等待
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 2.7.1 模态性（Modality）

JavaFX支持三种模态性：

```java
// NONE：非模态，不阻塞任何窗口
stage.initModality(Modality.NONE);

// WINDOW_MODAL：窗口模态，阻塞所有者窗口
stage.initModality(Modality.WINDOW_MODAL);

// APPLICATION_MODAL：应用模态，阻塞整个应用
stage.initModality(Modality.APPLICATION_MODAL);
```

## 2.8 命令行参数处理

JavaFX应用可以接收并处理命令行参数：

```java
package com.example.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CommandLineDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 获取命令行参数
        Parameters params = getParameters();
        
        // 原始参数
        String rawArgs = String.join(", ", params.getRaw());
        
        // 命名参数
        String namedArgs = params.getNamed().toString();
        
        // 未命名参数
        String unnamedArgs = params.getUnnamed().toString();
        
        // 显示参数
        Label rawLabel = new Label("原始参数: " + rawArgs);
        Label namedLabel = new Label("命名参数: " + namedArgs);
        Label unnamedLabel = new Label("未命名参数: " + unnamedArgs);
        
        VBox root = new VBox(10);
        root.getChildren().addAll(rawLabel, namedLabel, unnamedLabel);
        
        Scene scene = new Scene(root, 500, 200);
        primaryStage.setTitle("命令行参数演示");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

运行示例：

```bash
java CommandLineDemo arg1 arg2 --name=John --age=25
```

## 2.9 小结

本章学习了JavaFX程序的基本结构：

1. ✅ **Application类**：所有JavaFX应用的基类
2. ✅ **生命周期**：`init()` → `start()` → `stop()`
3. ✅ **三大组件**：Stage（舞台）、Scene（场景）、Node（节点）
4. ✅ **事件处理**：使用Lambda表达式处理用户交互
5. ✅ **多场景/多窗口**：灵活的窗口管理
6. ✅ **模态窗口**：控制窗口的交互行为

下一章，我们将深入学习JavaFX的架构和核心概念。

---

**练习题**

1. 创建一个简单的计数器应用，包含"增加"、"减少"、"重置"三个按钮
2. 实现一个登录界面，点击"登录"按钮后打开新窗口显示"登录成功"
3. 创建一个应用，在三个不同的场景之间循环切换
4. 尝试修改窗口样式（StageStyle），观察不同效果

