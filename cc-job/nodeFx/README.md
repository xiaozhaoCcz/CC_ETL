# NodeFx - JavaFX流程节点编辑器

一个基于JavaFX开发的流程节点可视化编辑器，支持节点拖拽、连线等功能。

## 功能特性

- ✅ **节点拖拽**：鼠标拖拽节点自由移动位置
- ✅ **节点连线**：点击节点连接点并拖动到另一个节点创建连接
- ✅ **贝塞尔曲线**：使用优雅的曲线展示节点之间的连接关系
- ✅ **悬停效果**：鼠标悬停时高亮显示连接点和连线
- ✅ **动态箭头**：连接线自动计算箭头方向
- ✅ **日志监控**：底部显示操作日志
- ✅ **工具栏**：支持添加节点、清空画布等操作
- ✅ **树形导航**：左侧显示任务组树形结构（NEW!）
- ✅ **后端集成**：从后端 API 动态加载数据（NEW!）
- ✅ **搜索过滤**：支持搜索任务组和节点（NEW!）

## 项目结构

```
nodeFx/
├── pom.xml                           # Maven配置文件
├── README.md                         # 项目说明文档
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── nodefx/
                        ├── NodeFxApplication.java      # 应用程序入口
                        ├── model/
                        │   ├── ProcessNode.java        # 流程节点类
                        │   └── NodeConnection.java     # 节点连接线类
                        └── view/
                            ├── NodeCanvas.java         # 画布区域
                            └── MainView.java           # 主界面
```

## 环境要求

- Java 17 或更高版本（推荐）
- Maven 3.6 或更高版本
- JavaFX 21.0.1
- 后端服务（cc-job-admin）运行在 http://localhost:8080

## 安装与运行

### 1. 启动后端服务（可选）

如果需要加载真实数据，请先启动后端服务：

```bash
cd ../cc-job-admin
mvn spring-boot:run
```

确认服务运行在 http://localhost:8080

### 2. 配置后端地址（可选）

编辑 `src/main/resources/application.properties`：

```properties
api.base.url=http://localhost:8080
api.connect.timeout=10
api.read.timeout=30
```

### 3. 编译项目

```bash
mvn clean compile
```

### 4. 运行项目

```bash
mvn javafx:run
```

或使用快速启动脚本：

```bash
./quick-start.sh
```

## 使用说明

### 基本操作

1. **拖拽节点**
   - 将鼠标悬停在节点上
   - 按住鼠标左键拖动节点到任意位置
   - 释放鼠标完成移动

2. **创建连接**
   - 将鼠标悬停在节点上，会显示4个连接点（上下左右）
   - 点击并按住某个连接点
   - 拖动鼠标到目标节点
   - 释放鼠标在目标节点上完成连接

3. **添加新节点**
   - 点击顶部工具栏的"➕ 添加节点"按钮
   - 在弹出对话框中输入节点处理器名称
   - 点击确定，新节点会被添加到画布上

4. **清空画布**
   - 点击顶部工具栏的"🗑️ 清空画布"按钮
   - 确认操作后，所有节点和连接将被清除
   - 系统会自动重新加载示例节点

### 界面说明

- **顶部标签栏**：显示当前项目名称和工具栏
- **中央画布**：流程节点编辑区域，支持滚动查看
- **底部日志**：实时显示操作日志和系统消息

## 技术实现

### 核心类说明

#### ProcessNode（流程节点）
- 继承自 `VBox`，使用JavaFX布局容器
- 实现节点拖拽功能
- 提供4个连接点（上下左右）
- 支持鼠标悬停效果

#### NodeConnection（节点连接）
- 使用 `CubicCurve` 绘制贝塞尔曲线
- 使用 `Polygon` 绘制箭头
- 通过属性绑定实现动态更新
- 自动计算箭头旋转角度

#### NodeCanvas（画布）
- 继承自 `Pane`，作为节点容器
- 管理所有节点和连接
- 实现临时连线的绘制逻辑
- 处理节点间的连接创建

#### MainView（主界面）
- 继承自 `BorderPane`，使用边界布局
- 组织顶部工具栏、中央画布、底部日志
- 提供添加节点、清空画布等功能
- 实时记录操作日志

## 自定义开发

### 修改节点样式

在 `ProcessNode.java` 的 `initializeUI()` 方法中修改样式：

```java
this.setStyle("-fx-background-color: white; " +
             "-fx-border-color: #8B5CF6; " +
             "-fx-border-width: 2; " +
             "-fx-border-radius: 10; " +
             "-fx-background-radius: 10;");
```

### 修改连接线样式

在 `NodeConnection.java` 的 `initializeConnection()` 方法中修改：

```java
curve.setStroke(Color.web("#374151"));
curve.setStrokeWidth(2);
```

### 扩展功能建议

- 添加节点右键菜单（删除、编辑、复制等）
- 实现连接线的删除功能
- 添加节点类型和图标
- 支持导入导出流程配置（JSON/XML）
- 添加撤销/重做功能
- 实现画布缩放和平移
- 添加节点搜索和过滤
- 支持流程执行和调试

## 运行方式（推荐）

### 方式一：使用启动脚本（推荐）

```bash
# 给脚本添加执行权限（首次运行需要）
chmod +x run.sh

# 运行项目
./run.sh
```

### 方式二：使用Maven命令

```bash
# 确保在有图形界面的终端中运行
mvn javafx:run
```

### 方式三：打包后运行

```bash
# 打包项目
./package.sh

# 运行打包的jar
java --module-path $HOME/.m2/repository/org/openjfx \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/nodeFx-1.0-SNAPSHOT.jar
```

## 常见问题

### 1. 编译错误：找不到符号

**错误信息：** `找不到符号 setStrokeDashArray(int,int)`

**解决方案：** 这个问题已在最新代码中修复。确保使用正确的代码版本。

### 2. 运行错误：Exception in Application start method

**错误信息：** `Process exited with an error: 133`

**可能原因：**
- 在远程服务器或无图形界面环境中运行
- macOS权限问题

**解决方案：**

#### macOS用户：
```bash
# 方法1：在终端中直接运行
./run.sh

# 方法2：如果遇到权限问题，尝试：
# 系统偏好设置 -> 安全性与隐私 -> 允许应用运行
```

#### 远程服务器用户：
JavaFX需要图形界面支持，如果在远程服务器上运行，需要：
```bash
# 使用X11转发（SSH）
ssh -X user@server
./run.sh

# 或使用虚拟显示
export DISPLAY=:0
./run.sh
```

### 3. JavaFX模块未找到

**解决方案：** 确保Maven已下载JavaFX依赖：
```bash
mvn clean install
mvn dependency:resolve
```

### 4. Java版本不兼容

**检查Java版本：**
```bash
java -version
```

**要求：** Java 11 或更高版本

**安装Java：**
```bash
# macOS (使用Homebrew)
brew install openjdk@11

# 或使用最新的LTS版本
brew install openjdk@17
```

### 5. 编译成功但无法启动窗口

**原因：** 可能是在后台运行或图形系统未响应

**解决方案：**
1. 检查是否有Java进程在运行：`ps aux | grep java`
2. 确保系统有图形界面支持
3. 尝试重启终端后再运行

### 6. 窗口显示异常或模糊

**macOS Retina显示屏：**
JavaFX会自动适配高分辨率显示。如果显示异常，可以尝试：
```bash
# 添加JVM参数
java -Dglass.gtk.uiScale=2.0 ...
```

## 许可证

本项目采用 MIT 许可证。

## 作者

NodeFx 流程节点编辑器

## 更新日志

### v1.1.0 (2025-11-01)
- ✅ 新增树形导航面板
- ✅ 集成后端 API 服务
- ✅ 实现动态数据加载
- ✅ 添加搜索过滤功能
- ✅ 支持配置化管理
- ✅ 完善错误处理和降级机制
- 📚 新增详细文档：
  - `树形数据加载说明.md` - 功能使用说明
  - `测试指南.md` - 测试步骤
  - `IMPLEMENTATION_SUMMARY.md` - 实现总结
  - `quick-start.sh` - 快速启动脚本

### v1.0.0 (2025-10-31)
- ✅ 初始版本发布
- ✅ 实现节点拖拽功能
- ✅ 实现节点连线功能
- ✅ 添加日志监控面板
- ✅ 提供示例流程节点

## 相关文档

- 📖 [树形数据加载说明](树形数据加载说明.md) - 详细的功能使用说明
- 🧪 [测试指南](测试指南.md) - 完整的测试步骤和验证
- 📋 [实现总结](IMPLEMENTATION_SUMMARY.md) - 技术实现细节
- 🚀 [快速启动脚本](quick-start.sh) - 一键启动工具

