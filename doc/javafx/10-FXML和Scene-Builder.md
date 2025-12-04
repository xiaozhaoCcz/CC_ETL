# 10-FXML和Scene Builder

## 10.1 FXML简介

FXML是一种基于XML的标记语言，用于定义JavaFX用户界面。它实现了界面和逻辑的分离，类似于HTML之于JavaScript。

### 10.1.1 FXML的优势

- **界面与逻辑分离**：UI设计和业务逻辑独立
- **可视化设计**：可使用Scene Builder可视化工具
- **易于维护**：修改UI不需要重新编译Java代码
- **团队协作**：设计师和开发者可并行工作

## 10.2 第一个FXML应用

### 10.2.1 创建FXML文件

`sample.fxml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.layout.VBox?>

<VBox alignment="CENTER" spacing="20.0" xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.javafx.fxml.SampleController">
    <padding>
        <Insets bottom="20.0" left="20.0" right="20.0" top="20.0"/>
    </padding>

    <Label fx:id="welcomeLabel" text="欢迎使用FXML！" 
           style="-fx-font-size: 18px; -fx-font-weight: bold;"/>
    
    <Button fx:id="clickButton" text="点击我" onAction="#handleButtonClick"/>
    
    <Label fx:id="resultLabel" text="点击次数: 0"/>
</VBox>
```

### 10.2.2 创建Controller类

```java
package com.example.javafx.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SampleController {
    
    @FXML
    private Label welcomeLabel;
    
    @FXML
    private Label resultLabel;
    
    private int clickCount = 0;
    
    @FXML
    private void initialize() {
        // 初始化方法，在FXML加载后自动调用
        System.out.println("Controller初始化完成");
    }
    
    @FXML
    private void handleButtonClick() {
        clickCount++;
        resultLabel.setText("点击次数: " + clickCount);
        System.out.println("按钮被点击了 " + clickCount + " 次");
    }
}
```

### 10.2.3 加载FXML

```java
package com.example.javafx.fxml;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FXMLApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载FXML文件
        Parent root = FXMLLoader.load(
            getClass().getResource("/fxml/sample.fxml")
        );
        
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("FXML示例");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 10.3 FXML语法详解

### 10.3.1 基本元素

```xml
<!-- 导入类 -->
<?import javafx.scene.control.Button?>
<?import javafx.scene.layout.VBox?>

<!-- 根元素 -->
<VBox xmlns:fx="http://javafx.com/fxml">
    <!-- 子元素 -->
    <Button text="按钮"/>
</VBox>
```

### 10.3.2 属性设置

```xml
<!-- 简单属性 -->
<Button text="点击我" disable="false"/>

<!-- 对象属性 -->
<VBox>
    <padding>
        <Insets top="10" right="10" bottom="10" left="10"/>
    </padding>
</VBox>

<!-- 集合属性 -->
<MenuBar>
    <menus>
        <Menu text="文件">
            <items>
                <MenuItem text="新建"/>
                <MenuItem text="打开"/>
            </items>
        </Menu>
    </menus>
</MenuBar>
```

### 10.3.3 fx:id 和 @FXML

```xml
<!-- FXML中定义ID -->
<Button fx:id="myButton" text="按钮"/>
<Label fx:id="myLabel" text="标签"/>
```

```java
// Controller中注入
public class MyController {
    @FXML
    private Button myButton;
    
    @FXML
    private Label myLabel;
}
```

### 10.3.4 事件处理

```xml
<!-- 方式1: 使用onAction -->
<Button text="点击" onAction="#handleClick"/>

<!-- 方式2: 使用脚本 -->
<Button text="点击">
    <onAction>
        <fx:script>
            System.out.println("按钮被点击");
        </fx:script>
    </onAction>
</Button>
```

## 10.4 完整示例：登录界面

### 10.4.1 login.fxml

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml" 
            fx:controller="com.example.javafx.fxml.LoginController"
            prefWidth="400" prefHeight="300">
    
    <center>
        <GridPane alignment="CENTER" hgap="10" vgap="10">
            <padding>
                <Insets top="25" right="25" bottom="25" left="25"/>
            </padding>

            <!-- 标题 -->
            <Label text="用户登录" GridPane.columnIndex="0" GridPane.rowIndex="0" 
                   GridPane.columnSpan="2"
                   style="-fx-font-size: 24px; -fx-font-weight: bold;"/>

            <!-- 用户名 -->
            <Label text="用户名:" GridPane.columnIndex="0" GridPane.rowIndex="1"/>
            <TextField fx:id="usernameField" GridPane.columnIndex="1" GridPane.rowIndex="1"
                      promptText="请输入用户名"/>

            <!-- 密码 -->
            <Label text="密码:" GridPane.columnIndex="0" GridPane.rowIndex="2"/>
            <PasswordField fx:id="passwordField" GridPane.columnIndex="1" GridPane.rowIndex="2"
                          promptText="请输入密码"/>

            <!-- 记住我 -->
            <CheckBox fx:id="rememberCheckBox" text="记住我" 
                     GridPane.columnIndex="1" GridPane.rowIndex="3"/>

            <!-- 按钮 -->
            <HBox spacing="10" GridPane.columnIndex="1" GridPane.rowIndex="4">
                <Button text="登录" onAction="#handleLogin" defaultButton="true"
                       style="-fx-background-color: #4CAF50; -fx-text-fill: white;"/>
                <Button text="取消" onAction="#handleCancel"/>
            </HBox>

            <!-- 消息标签 -->
            <Label fx:id="messageLabel" GridPane.columnIndex="0" GridPane.rowIndex="5"
                   GridPane.columnSpan="2" 
                   style="-fx-text-fill: red;"/>
        </GridPane>
    </center>
</BorderPane>
```

### 10.4.2 LoginController.java

```java
package com.example.javafx.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberCheckBox;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        System.out.println("登录界面初始化");
        messageLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean remember = rememberCheckBox.isSelected();

        System.out.println("登录尝试:");
        System.out.println("  用户名: " + username);
        System.out.println("  密码: " + password);
        System.out.println("  记住我: " + remember);

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("请填写用户名和密码");
            return;
        }

        // 简单验证（实际应用中应该连接数据库）
        if (username.equals("admin") && password.equals("123456")) {
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("登录成功！");
            
            // 这里可以关闭登录窗口，打开主窗口
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setContentText("欢迎, " + username + "!");
            alert.showAndWait();
        } else {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("用户名或密码错误");
        }
    }

    @FXML
    private void handleCancel() {
        // 获取当前Stage并关闭
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}
```

### 10.4.3 启动类

```java
package com.example.javafx.fxml;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(
            getClass().getResource("/fxml/login.fxml")
        );

        Scene scene = new Scene(root);
        primaryStage.setTitle("用户登录");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 10.5 Scene Builder

Scene Builder是Oracle提供的可视化FXML编辑工具。

### 10.5.1 安装Scene Builder

1. 访问 https://gluonhq.com/products/scene-builder/
2. 下载对应平台的安装包
3. 安装Scene Builder

### 10.5.2 在IDE中集成

**IntelliJ IDEA**:
1. File → Settings → Languages & Frameworks → JavaFX
2. 设置Scene Builder路径
3. 右键.fxml文件 → Open in Scene Builder

**Eclipse**:
1. Window → Preferences → JavaFX
2. 设置Scene Builder路径

### 10.5.3 Scene Builder使用

Scene Builder界面分为四个主要区域：

1. **左侧 - Library（控件库）**：所有可用的JavaFX控件
2. **中央 - Content（内容区）**：可视化编辑区域
3. **右上 - Hierarchy（层次结构）**：组件树
4. **右下 - Inspector（属性面板）**：属性编辑

**基本操作**：
- 从Library拖拽控件到Content
- 在Inspector中设置属性
- 设置fx:id以便在Controller中引用
- 设置onAction等事件处理方法

## 10.6 FXML与Controller通信

### 10.6.1 传递数据到Controller

```java
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/sample.fxml")
        );
        
        Parent root = loader.load();
        
        // 获取Controller并传递数据
        SampleController controller = loader.getController();
        controller.setUserName("张三");
        controller.setUserData(new UserData("张三", 25));
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
```

### 10.6.2 从Controller获取数据

```java
FXMLLoader loader = new FXMLLoader(
    getClass().getResource("/fxml/dialog.fxml")
);

Parent root = loader.load();

DialogController controller = loader.getController();

Stage dialogStage = new Stage();
dialogStage.setScene(new Scene(root));
dialogStage.showAndWait();

// 获取对话框返回的数据
String result = controller.getResult();
System.out.println("用户输入: " + result);
```

## 10.7 多语言支持

FXML支持资源包（ResourceBundle）实现国际化。

### 10.7.1 创建资源文件

`messages_zh_CN.properties`:
```properties
app.title=应用程序
button.login=登录
button.cancel=取消
label.username=用户名
label.password=密码
```

`messages_en_US.properties`:
```properties
app.title=Application
button.login=Login
button.cancel=Cancel
label.username=Username
label.password=Password
```

### 10.7.2 在FXML中使用

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns:fx="http://javafx.com/fxml">
    <Label text="%label.username"/>
    <TextField/>
    <Label text="%label.password"/>
    <PasswordField/>
    <Button text="%button.login"/>
</VBox>
```

### 10.7.3 加载资源包

```java
@Override
public void start(Stage primaryStage) throws Exception {
    ResourceBundle bundle = ResourceBundle.getBundle(
        "i18n.messages",
        new Locale("zh", "CN")
    );
    
    FXMLLoader loader = new FXMLLoader(
        getClass().getResource("/fxml/sample.fxml"),
        bundle
    );
    
    Parent root = loader.load();
    
    Scene scene = new Scene(root);
    primaryStage.setTitle(bundle.getString("app.title"));
    primaryStage.setScene(scene);
    primaryStage.show();
}
```

## 10.8 小结

本章学习了FXML和Scene Builder：

1. ✅ **FXML基础**：XML格式的UI定义
2. ✅ **Controller**：@FXML注解和事件处理
3. ✅ **FXML语法**：元素、属性、集合
4. ✅ **Scene Builder**：可视化FXML编辑工具
5. ✅ **数据传递**：Controller之间的通信
6. ✅ **国际化**：ResourceBundle支持多语言

FXML是JavaFX开发的重要工具，实现了界面与逻辑的优雅分离。

---

**练习题**

1. 使用FXML创建一个完整的注册表单界面
2. 使用Scene Builder设计一个计算器界面
3. 实现一个多语言切换的应用程序
4. 创建一个模态对话框，使用FXML实现，并返回用户选择

