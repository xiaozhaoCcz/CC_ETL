# 11-CSS样式

## 11.1 CSS简介

JavaFX支持CSS（层叠样式表）来定义UI的外观，语法类似Web CSS，但使用`-fx-`前缀。

### 11.1.1 CSS的优势

- **样式与逻辑分离**：外观不硬编码在Java代码中
- **统一风格**：一处定义，多处使用
- **易于维护**：修改样式不需要重新编译
- **主题切换**：轻松实现深色/浅色主题

## 11.2 应用CSS的三种方式

### 11.2.1 内联样式

```java
package com.example.javafx.css;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class InlineStyleDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        // 内联样式
        Label titleLabel = new Label("标题");
        titleLabel.setStyle(
            "-fx-font-size: 24px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #2196F3;"
        );
        
        Button styledButton = new Button("样式按钮");
        styledButton.setStyle(
            "-fx-background-color: #4CAF50; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-padding: 10 20; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        );
        
        // 悬停效果（内联样式无法设置伪类）
        styledButton.setOnMouseEntered(e -> 
            styledButton.setStyle(
                "-fx-background-color: #45a049; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 5;"
            )
        );
        
        styledButton.setOnMouseExited(e -> 
            styledButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 5;"
            )
        );
        
        root.getChildren().addAll(titleLabel, styledButton);
        
        Scene scene = new Scene(root, 350, 200);
        primaryStage.setTitle("内联样式示例");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 11.2.2 外部CSS文件

**style.css**:
```css
/* 根节点样式 */
.root {
    -fx-background-color: #f5f5f5;
}

/* 标题样式 */
.title {
    -fx-font-size: 24px;
    -fx-font-weight: bold;
    -fx-text-fill: #2196F3;
}

/* 主按钮样式 */
.primary-button {
    -fx-background-color: #4CAF50;
    -fx-text-fill: white;
    -fx-font-size: 16px;
    -fx-padding: 10 20;
    -fx-background-radius: 5;
    -fx-cursor: hand;
}

.primary-button:hover {
    -fx-background-color: #45a049;
}

.primary-button:pressed {
    -fx-background-color: #3d8b40;
}

/* 危险按钮 */
.danger-button {
    -fx-background-color: #f44336;
    -fx-text-fill: white;
    -fx-font-size: 16px;
    -fx-padding: 10 20;
    -fx-background-radius: 5;
}

.danger-button:hover {
    -fx-background-color: #da190b;
}
```

```java
package com.example.javafx.css;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExternalCSSDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label("外部CSS样式");
        titleLabel.getStyleClass().add("title");
        
        Button primaryBtn = new Button("主按钮");
        primaryBtn.getStyleClass().add("primary-button");
        
        Button dangerBtn = new Button("危险按钮");
        dangerBtn.getStyleClass().add("danger-button");
        
        root.getChildren().addAll(titleLabel, primaryBtn, dangerBtn);
        
        Scene scene = new Scene(root, 350, 200);
        
        // 加载外部CSS文件
        scene.getStylesheets().add(
            getClass().getResource("/css/style.css").toExternalForm()
        );
        
        primaryStage.setTitle("外部CSS示例");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 11.3 CSS选择器

### 11.3.1 选择器类型

```css
/* 类型选择器 - 选择所有Button */
.button {
    -fx-background-color: lightblue;
}

/* 类选择器 - 选择特定样式类 */
.my-button {
    -fx-background-color: lightgreen;
}

/* ID选择器 - 选择特定ID */
#submit-button {
    -fx-background-color: orange;
}

/* 后代选择器 */
.vbox .button {
    -fx-font-size: 14px;
}

/* 子选择器 */
.vbox > .button {
    -fx-padding: 10;
}

/* 伪类选择器 */
.button:hover {
    -fx-background-color: yellow;
}

.button:pressed {
    -fx-background-color: red;
}

.button:disabled {
    -fx-opacity: 0.5;
}

.button:focused {
    -fx-border-color: blue;
    -fx-border-width: 2;
}
```

### 11.3.2 选择器示例

```java
package com.example.javafx.css;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SelectorDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("vbox");  // 添加样式类
        
        // 类型选择器会影响所有Button
        Button button1 = new Button("按钮1");
        
        // 添加自定义样式类
        Button button2 = new Button("按钮2");
        button2.getStyleClass().add("my-button");
        
        // 设置ID
        Button button3 = new Button("提交按钮");
        button3.setId("submit-button");
        
        Button button4 = new Button("禁用按钮");
        button4.setDisable(true);
        
        root.getChildren().addAll(button1, button2, button3, button4);
        
        Scene scene = new Scene(root, 350, 300);
        scene.getStylesheets().add(
            getClass().getResource("/css/selectors.css").toExternalForm()
        );
        
        primaryStage.setTitle("CSS选择器示例");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 11.4 常用CSS属性

### 11.4.1 文本属性

```css
.text-demo {
    /* 字体 */
    -fx-font-family: "Arial";
    -fx-font-size: 16px;
    -fx-font-weight: bold;     /* normal, bold, bolder, lighter */
    -fx-font-style: italic;    /* normal, italic, oblique */
    
    /* 文本颜色 */
    -fx-text-fill: #333333;
    
    /* 文本对齐 */
    -fx-text-alignment: center;  /* left, center, right, justify */
    
    /* 下划线 */
    -fx-underline: true;
    
    /* 删除线 */
    -fx-strikethrough: true;
}
```

### 11.4.2 背景和边框

```css
.background-demo {
    /* 背景颜色 */
    -fx-background-color: #f0f0f0;
    
    /* 渐变背景 */
    -fx-background-color: linear-gradient(to bottom, #667eea, #764ba2);
    
    /* 背景图片 */
    -fx-background-image: url("image.png");
    -fx-background-repeat: no-repeat;
    -fx-background-position: center;
    -fx-background-size: cover;
    
    /* 圆角 */
    -fx-background-radius: 10;
    
    /* 边框 */
    -fx-border-color: #2196F3;
    -fx-border-width: 2;
    -fx-border-radius: 10;
    -fx-border-style: solid;  /* solid, dashed, dotted */
}
```

### 11.4.3 间距和大小

```css
.spacing-demo {
    /* 内边距 */
    -fx-padding: 10;           /* 四边相同 */
    -fx-padding: 10 20;        /* 上下 左右 */
    -fx-padding: 10 20 30 40;  /* 上 右 下 左 */
    
    /* 外边距（在某些布局中有效） */
    -fx-margin: 10;
    
    /* 尺寸 */
    -fx-pref-width: 200;
    -fx-pref-height: 100;
    -fx-min-width: 100;
    -fx-max-width: 300;
}
```

### 11.4.4 效果

```css
.effect-demo {
    /* 透明度 */
    -fx-opacity: 0.8;
    
    /* 阴影 */
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 2, 2);
    
    /* 光标 */
    -fx-cursor: hand;  /* default, hand, wait, text, crosshair */
    
    /* 旋转 */
    -fx-rotate: 45;
    
    /* 缩放 */
    -fx-scale-x: 1.5;
    -fx-scale-y: 1.5;
}
```

## 11.5 完整样式示例

### 11.5.1 modern-theme.css

```css
/* ===== 全局样式 ===== */
.root {
    -fx-background-color: #fafafa;
    -fx-font-family: "Microsoft YaHei", "SimHei", sans-serif;
}

/* ===== 标题样式 ===== */
.title-large {
    -fx-font-size: 32px;
    -fx-font-weight: bold;
    -fx-text-fill: #212121;
}

.title-medium {
    -fx-font-size: 24px;
    -fx-font-weight: bold;
    -fx-text-fill: #424242;
}

.title-small {
    -fx-font-size: 18px;
    -fx-font-weight: bold;
    -fx-text-fill: #616161;
}

/* ===== 按钮样式 ===== */
.button {
    -fx-background-color: #e0e0e0;
    -fx-text-fill: #212121;
    -fx-font-size: 14px;
    -fx-padding: 8 16;
    -fx-background-radius: 4;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);
}

.button:hover {
    -fx-background-color: #d5d5d5;
}

.button:pressed {
    -fx-background-color: #bdbdbd;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);
}

.button:disabled {
    -fx-opacity: 0.5;
}

/* 主要按钮 */
.button-primary {
    -fx-background-color: #2196F3;
    -fx-text-fill: white;
}

.button-primary:hover {
    -fx-background-color: #1976D2;
}

.button-primary:pressed {
    -fx-background-color: #0D47A1;
}

/* 成功按钮 */
.button-success {
    -fx-background-color: #4CAF50;
    -fx-text-fill: white;
}

.button-success:hover {
    -fx-background-color: #388E3C;
}

/* 危险按钮 */
.button-danger {
    -fx-background-color: #F44336;
    -fx-text-fill: white;
}

.button-danger:hover {
    -fx-background-color: #D32F2F;
}

/* ===== 文本框样式 ===== */
.text-field {
    -fx-background-color: white;
    -fx-border-color: #bdbdbd;
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-padding: 8;
    -fx-font-size: 14px;
}

.text-field:focused {
    -fx-border-color: #2196F3;
    -fx-border-width: 2;
    -fx-effect: dropshadow(gaussian, rgba(33, 150, 243, 0.3), 4, 0, 0, 0);
}

/* ===== 标签样式 ===== */
.label {
    -fx-text-fill: #424242;
    -fx-font-size: 14px;
}

/* ===== 面板样式 ===== */
.card {
    -fx-background-color: white;
    -fx-background-radius: 8;
    -fx-padding: 20;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);
}

/* ===== 列表样式 ===== */
.list-view {
    -fx-background-color: white;
    -fx-border-color: #e0e0e0;
    -fx-border-width: 1;
}

.list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: #424242;
    -fx-padding: 8;
}

.list-cell:selected {
    -fx-background-color: #E3F2FD;
    -fx-text-fill: #1976D2;
}

.list-cell:hover {
    -fx-background-color: #F5F5F5;
}

/* ===== 表格样式 ===== */
.table-view {
    -fx-background-color: white;
    -fx-border-color: #e0e0e0;
    -fx-border-width: 1;
}

.table-view .column-header {
    -fx-background-color: #f5f5f5;
    -fx-text-fill: #212121;
    -fx-font-weight: bold;
}

.table-row-cell {
    -fx-background-color: white;
}

.table-row-cell:selected {
    -fx-background-color: #E3F2FD;
}

.table-row-cell:hover {
    -fx-background-color: #F5F5F5;
}
```

### 11.5.2 应用主题

```java
package com.example.javafx.css;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ModernThemeDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // 顶部
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(20));
        Label title = new Label("现代化主题演示");
        title.getStyleClass().add("title-large");
        topBox.getChildren().add(title);
        root.setTop(topBox);
        
        // 中心 - 卡片
        VBox centerBox = new VBox(15);
        centerBox.setPadding(new Insets(20));
        
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        
        Label cardTitle = new Label("登录表单");
        cardTitle.getStyleClass().add("title-medium");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("用户名");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("密码");
        
        HBox buttonBox = new HBox(10);
        Button loginBtn = new Button("登录");
        loginBtn.getStyleClass().addAll("button-primary");
        
        Button cancelBtn = new Button("取消");
        
        Button dangerBtn = new Button("删除");
        dangerBtn.getStyleClass().add("button-danger");
        
        buttonBox.getChildren().addAll(loginBtn, cancelBtn, dangerBtn);
        
        card.getChildren().addAll(cardTitle, usernameField, 
                                  passwordField, buttonBox);
        centerBox.getChildren().add(card);
        root.setCenter(centerBox);
        
        Scene scene = new Scene(root, 500, 400);
        
        // 加载现代主题
        scene.getStylesheets().add(
            getClass().getResource("/css/modern-theme.css").toExternalForm()
        );
        
        primaryStage.setTitle("现代化主题");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 11.6 主题切换

```java
package com.example.javafx.css;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ThemeSwitchDemo extends Application {

    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        
        Label title = new Label("主题切换演示");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        TextField textField = new TextField("示例文本");
        
        Button button = new Button("示例按钮");
        
        ComboBox<String> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("浅色主题", "深色主题", "蓝色主题");
        themeCombo.setValue("浅色主题");
        
        themeCombo.setOnAction(e -> {
            String theme = themeCombo.getValue();
            switchTheme(theme);
        });
        
        root.getChildren().addAll(title, textField, button, 
                                 new Separator(), 
                                 new Label("选择主题:"), themeCombo);
        
        scene = new Scene(root, 400, 350);
        
        // 默认加载浅色主题
        switchTheme("浅色主题");
        
        primaryStage.setTitle("主题切换");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void switchTheme(String theme) {
        scene.getStylesheets().clear();
        
        String cssFile = switch (theme) {
            case "深色主题" -> "/css/dark-theme.css";
            case "蓝色主题" -> "/css/blue-theme.css";
            default -> "/css/light-theme.css";
        };
        
        scene.getStylesheets().add(
            getClass().getResource(cssFile).toExternalForm()
        );
        
        System.out.println("切换到: " + theme);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 11.7 小结

本章学习了JavaFX的CSS样式系统：

1. ✅ **应用方式**：内联、样式类、外部CSS
2. ✅ **选择器**：类型、类、ID、伪类选择器
3. ✅ **常用属性**：文本、背景、边框、间距
4. ✅ **主题设计**：完整的主题CSS文件
5. ✅ **主题切换**：动态加载不同CSS文件

CSS是JavaFX美化界面的强大工具，合理使用可以创建专业美观的应用。

---

**练习题**

1. 创建一个完整的深色主题CSS文件
2. 实现一个Material Design风格的按钮样式
3. 创建可切换的主题系统（浅色/深色/自定义）
4. 使用CSS实现一个漂亮的登录界面

