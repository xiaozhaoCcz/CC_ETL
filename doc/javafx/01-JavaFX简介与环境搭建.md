# 01-JavaFX简介与环境搭建

## 1.1 JavaFX是什么

JavaFX 是一个用于构建富客户端应用程序的现代化图形用户界面（GUI）框架。它是Oracle公司推出的用于替代Swing的下一代Java桌面应用程序开发平台。

### 主要特点

1. **现代化的UI框架**：提供丰富的UI控件和精美的视觉效果
2. **跨平台支持**：一次编写，到处运行（Windows、macOS、Linux）
3. **硬件加速**：利用GPU加速，提供流畅的动画和过渡效果
4. **CSS样式支持**：类似Web开发的样式定制方式
5. **FXML支持**：使用XML描述界面，实现界面与逻辑分离
6. **丰富的媒体支持**：内置音频、视频、图像处理能力

## 1.2 JavaFX的历史

- **2008年**：JavaFX 1.0发布，使用JavaFX Script语言
- **2011年**：JavaFX 2.0发布，改用纯Java API
- **2014年**：JavaFX 8随Java 8发布，集成到JDK中
- **2018年**：JavaFX 11从JDK中分离，成为独立项目OpenJFX
- **至今**：持续更新，最新版本支持Java 17及以上

## 1.3 JavaFX与Swing的比较

| 特性 | JavaFX | Swing |
|------|--------|-------|
| 发布时间 | 2008年 | 1998年 |
| 外观 | 现代化、美观 | 传统、较为古板 |
| 硬件加速 | 支持GPU加速 | 不支持 |
| CSS样式 | 支持 | 不支持 |
| 动画效果 | 内置丰富动画API | 需要自己实现 |
| 多媒体 | 原生支持 | 需要第三方库 |
| FXML | 支持界面描述语言 | 不支持 |
| 学习曲线 | 相对陡峭 | 相对简单 |
| 社区支持 | 活跃 | 维护状态 |

## 1.4 开发环境搭建

### 1.4.1 系统要求

- **JDK版本**：Java 11 或更高版本（推荐Java 17 LTS）
- **内存**：至少4GB RAM（推荐8GB以上）
- **硬盘空间**：至少2GB可用空间
- **操作系统**：Windows 7+、macOS 10.10+、Linux（主流发行版）

### 1.4.2 安装JDK

#### 方式一：安装标准JDK（不含JavaFX）

1. 访问 [Oracle JDK下载页](https://www.oracle.com/java/technologies/downloads/) 或 [OpenJDK](https://adoptium.net/)
2. 下载适合您系统的JDK安装包
3. 运行安装程序，按提示完成安装

#### 方式二：安装包含JavaFX的JDK

1. 访问 [Azul Zulu下载页](https://www.azul.com/downloads/?package=jdk-fx)
2. 选择 "JDK FX" 版本（已包含JavaFX）
3. 下载并安装

#### 验证JDK安装

打开终端或命令提示符，输入：

```bash
java -version
javac -version
```

应该看到类似输出：

```
java version "17.0.2" 2022-01-18 LTS
Java(TM) SE Runtime Environment (build 17.0.2+8-LTS-86)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.2+8-LTS-86, mixed mode, sharing)
```

### 1.4.3 下载JavaFX SDK

如果使用的是不包含JavaFX的JDK，需要单独下载JavaFX SDK：

1. 访问 [JavaFX官网](https://gluonhq.com/products/javafx/)
2. 下载对应平台的SDK
3. 解压到指定目录，例如：`C:\javafx-sdk-21` 或 `/usr/local/javafx-sdk-21`

目录结构如下：

```
javafx-sdk-21/
├── legal/
├── lib/
│   ├── javafx.base.jar
│   ├── javafx.controls.jar
│   ├── javafx.fxml.jar
│   ├── javafx.graphics.jar
│   ├── javafx.media.jar
│   ├── javafx.swing.jar
│   ├── javafx.web.jar
│   └── (平台相关的本地库文件)
└── src.zip
```

## 1.5 配置IDE

### 1.5.1 IntelliJ IDEA 配置

#### 步骤1：创建新项目

1. 打开IntelliJ IDEA
2. 选择 **File** → **New** → **Project**
3. 选择 **JavaFX** 作为项目类型
4. 设置项目名称和位置
5. 选择JDK版本（Java 11+）
6. 点击 **Create**

#### 步骤2：配置JavaFX库（如果JDK不包含JavaFX）

1. **File** → **Project Structure** → **Libraries**
2. 点击 **+** → **Java**
3. 选择JavaFX SDK的 `lib` 目录
4. 点击 **OK**

#### 步骤3：配置VM选项

1. **Run** → **Edit Configurations**
2. 在 **VM options** 中添加：

```
--module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml
```

macOS/Linux:

```
--module-path "/usr/local/javafx-sdk-21/lib" --add-modules javafx.controls,javafx.fxml
```

#### 步骤4：使用Maven管理依赖（推荐）

创建 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>javafx-demo</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <javafx.version>21</javafx.version>
    </properties>

    <dependencies>
        <!-- JavaFX Controls -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- JavaFX FXML -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- JavaFX Media (可选) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-media</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- JavaFX Web (可选) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
            <version>${javafx.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>com.example.MainApp</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

使用Maven后，可以直接运行：

```bash
mvn javafx:run
```

### 1.5.2 Eclipse 配置

#### 步骤1：安装e(fx)clipse插件

1. **Help** → **Eclipse Marketplace**
2. 搜索 "e(fx)clipse"
3. 安装插件并重启Eclipse

#### 步骤2：创建JavaFX项目

1. **File** → **New** → **Other** → **JavaFX** → **JavaFX Project**
2. 设置项目名称
3. 选择JDK
4. 点击 **Finish**

#### 步骤3：配置JavaFX库

1. 右键项目 → **Build Path** → **Configure Build Path**
2. **Libraries** → **Add External JARs**
3. 选择JavaFX SDK的 `lib` 目录下的所有jar文件
4. 点击 **Apply and Close**

#### 步骤4：配置运行参数

1. **Run** → **Run Configurations**
2. 选择你的JavaFX应用
3. **Arguments** → **VM arguments**，添加：

```
--module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml
```

### 1.5.3 VS Code 配置

#### 步骤1：安装扩展

安装以下扩展：
- Extension Pack for Java
- JavaFX Support

#### 步骤2：配置 launch.json

创建 `.vscode/launch.json`：

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch JavaFX App",
            "request": "launch",
            "mainClass": "com.example.MainApp",
            "vmArgs": "--module-path \"C:/javafx-sdk-21/lib\" --add-modules javafx.controls,javafx.fxml"
        }
    ]
}
```

## 1.6 验证安装

创建一个简单的测试程序：

```java
// MainApp.java
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("JavaFX 环境搭建成功！");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 300);
        
        primaryStage.setTitle("JavaFX Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

运行程序，如果看到窗口显示"JavaFX 环境搭建成功！"，说明环境配置正确。

## 1.7 常见问题

### 问题1：Error: JavaFX runtime components are missing

**原因**：JDK不包含JavaFX，或者没有正确配置模块路径。

**解决方案**：
- 使用Maven管理依赖（推荐）
- 或者在VM options中添加模块路径

### 问题2：Module javafx.controls not found

**原因**：模块路径配置错误。

**解决方案**：
- 检查JavaFX SDK路径是否正确
- 确保使用双引号包裹路径（如果路径包含空格）

### 问题3：Graphics Device initialization failed

**原因**：图形驱动问题或硬件加速问题。

**解决方案**：
在VM options中添加：

```
-Dprism.order=sw
```

这将禁用硬件加速，使用软件渲染。

### 问题4：macOS上运行报错

**原因**：macOS的安全限制。

**解决方案**：
在VM options中添加：

```
-XstartOnFirstThread
```

## 1.8 推荐学习资源

### 官方资源

- **官方文档**：https://openjfx.io/
- **JavaFX API文档**：https://openjfx.io/javadoc/21/
- **官方教程**：https://docs.oracle.com/javafx/

### 社区资源

- **GitHub仓库**：https://github.com/openjfx
- **Stack Overflow**：搜索 [javafx] 标签
- **Reddit**：r/JavaFX 社区

### 推荐书籍

- 《JavaFX 8权威指南》
- 《Pro JavaFX 9》
- 《Learn JavaFX 17》

## 1.9 小结

本章介绍了JavaFX的基本概念、特点和环境搭建步骤。主要内容包括：

1. ✅ JavaFX是现代化的Java桌面应用开发框架
2. ✅ 支持CSS、FXML、硬件加速等现代特性
3. ✅ 可以使用标准JDK+JavaFX SDK，或使用包含JavaFX的JDK
4. ✅ 推荐使用Maven管理JavaFX依赖
5. ✅ 主流IDE（IntelliJ IDEA、Eclipse、VS Code）都支持JavaFX开发

下一章，我们将创建第一个JavaFX程序，深入了解JavaFX应用的结构和生命周期。

---

**练习题**

1. 在你的计算机上搭建JavaFX开发环境
2. 创建并运行本章的测试程序
3. 尝试修改窗口标题和标签文本
4. 如果遇到问题，参考常见问题部分进行排查

