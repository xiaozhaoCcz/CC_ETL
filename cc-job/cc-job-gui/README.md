# NodeFx - JavaFX流程节点编辑器

一个基于JavaFX开发的流程节点可视化编辑器，支持节点拖拽、连线、任务执行等功能。

## 🎉 最新功能

### ✨ 任务执行功能（NEW!）
- ✅ **运行任务组**：点击工具栏"▶️ 运行"按钮执行任务
- ✅ **实时日志**：实时获取并显示任务执行日志
- ✅ **雪花算法ID**：使用雪花算法生成唯一执行批次ID
- ✅ **日志轮询**：自动轮询获取任务执行日志
- ✅ **状态管理**：跟踪任务运行状态和执行进度
- ✅ **错误处理**：完善的错误处理和异常提示

## 功能特性

### 基础功能
- ✅ **节点拖拽**：鼠标拖拽节点自由移动位置
- ✅ **节点连线**：点击节点连接点并拖动到另一个节点创建连接
- ✅ **贝塞尔曲线**：使用优雅的曲线展示节点之间的连接关系
- ✅ **悬停效果**：鼠标悬停时高亮显示连接点和连线
- ✅ **动态箭头**：连接线自动计算箭头方向

### 高级功能
- ✅ **树形导航**：左侧显示任务组树形结构
- ✅ **导航栏切换**：顶部标签页快速切换任务组
- ✅ **后端集成**：从后端 API 动态加载数据
- ✅ **搜索过滤**：支持搜索任务组和节点
- ✅ **小地图**：显示画布缩略图，方便导航
- ✅ **日志监控**：底部显示实时日志信息

### 任务执行
- ✅ **触发执行**：点击运行按钮触发任务组执行
- ✅ **批次管理**：使用雪花算法生成唯一批次ID
- ✅ **日志获取**：定时轮询获取任务执行日志
- ✅ **状态追踪**：实时显示任务执行状态
- ✅ **自动结束**：任务完成后自动停止日志轮询

## 项目结构

```
nodeFx/
├── pom.xml                           # Maven配置文件
├── README.md                         # 项目说明文档
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── nodefx/
        │               ├── NodeFxApplication.java      # 应用程序入口
        │               ├── model/                      # 数据模型
        │               │   ├── ProcessNode.java       # 流程节点
        │               │   ├── NodeConnection.java    # 节点连接
        │               │   ├── JobComposeData.java    # 任务组合数据
        │               │   └── TreeNodeData.java      # 树节点数据
        │               ├── service/                    # 服务层
        │               │   ├── BaseService.java       # 基础服务
        │               │   ├── JobPartService.java    # 任务分区服务
        │               │   ├── JobInfoService.java    # 任务信息服务（NEW!）
        │               │   └── JobLogService.java     # 任务日志服务（NEW!）
        │               ├── util/                       # 工具类
        │               │   ├── ApiUtil.java           # API工具
        │               │   ├── AppConfig.java         # 应用配置
        │               │   ├── DetachablePanel.java   # 可分离面板
        │               │   └── SnowflakeIdGenerator.java # 雪花算法ID生成器（NEW!）
        │               └── view/                       # 视图组件
        │                   ├── MainView.java          # 主界面
        │                   ├── NodeCanvas.java        # 画布区域
        │                   ├── TopToolBar.java        # 顶部工具栏
        │                   ├── TaskTreeView.java      # 任务树视图
        │                   ├── TaskNavigationBar.java # 任务导航栏
        │                   ├── LogPanel.java          # 日志面板
        │                   ├── MiniMapView.java       # 小地图
        │                   └── CollapsedSidebar.java  # 折叠侧边栏
        └── resources/
            └── application.properties              # 配置文件
```

## 环境要求

- Java 17 或更高版本（推荐）
- Maven 3.6 或更高版本
- JavaFX 21.0.1
- 后端服务（cc-job-admin）运行在 http://localhost:8080

## 安装与运行

### 1. 启动后端服务

任务执行功能需要后端服务支持，请先启动后端服务：

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

1. **选择任务组**
   - 在左侧树形导航中点击任务组
   - 画布会自动加载该任务组的节点和连接
   - 顶部导航栏会显示当前选中的任务组

2. **拖拽节点**
   - 将鼠标悬停在节点上
   - 按住鼠标左键拖动节点到任意位置
   - 释放鼠标完成移动

3. **创建连接**
   - 将鼠标悬停在节点上，会显示4个连接点（上下左右）
   - 点击并按住某个连接点
   - 拖动鼠标到目标节点
   - 释放鼠标在目标节点上完成连接

4. **执行任务**
   - 确保已选择一个任务组
   - 点击顶部工具栏的"▶️ 运行"按钮
   - 底部日志面板会实时显示执行日志
   - 任务完成后会自动停止

5. **查看日志**
   - 底部日志面板实时显示任务执行日志
   - 支持自动滚动到最新日志
   - 可以导出日志或清空日志

### 任务执行流程

1. **选择任务组**
   ```
   点击左侧树形导航 → 选择任务组 → 画布加载节点
   ```

2. **执行任务**
   ```
   点击"运行"按钮 → 生成批次ID → 调用后端API → 获取日志ID
   ```

3. **日志监控**
   ```
   启动日志轮询 → 定时获取日志 → 实时显示 → 任务完成自动停止
   ```

### 界面说明

- **顶部菜单栏**：文件、开始、任务组三个菜单
- **工具栏**：
  - 文件操作：新建、打开、保存
  - 编辑操作：撤销、重做
  - 视图操作：放大、缩小、适应窗口
  - 执行操作：运行、清除、设置
- **左侧面板**：
  - 任务树：显示所有任务组和分区
  - 小地图：显示画布缩略图
- **中央画布**：流程节点编辑区域
- **顶部导航栏**：显示已打开的任务组标签
- **底部日志**：实时显示任务执行日志

## 技术实现

### 核心技术栈

- **JavaFX 21.0.1**：UI框架
- **OkHttp 4.12.0**：HTTP客户端
- **Gson 2.10.1**：JSON处理
- **Maven**：项目构建工具

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
- 管理所有子组件的布局
- 实现任务执行逻辑
- 处理日志轮询和状态管理

#### JobInfoService（任务信息服务）
- 调用后端API触发任务执行
- 停止任务组执行
- 获取任务运行状态

#### JobLogService（任务日志服务）
- 获取任务执行日志
- 支持增量日志获取
- 检测日志结束状态

#### SnowflakeIdGenerator（雪花算法）
- 生成唯一的分布式ID
- 用于生成任务执行批次ID
- 支持高并发场景

### 任务执行原理

1. **生成批次ID**
   ```java
   // 使用雪花算法生成唯一ID
   currentRandomId = snowflake.nextIdStr();
   ```

2. **触发任务**
   ```java
   // 调用后端API，传递任务ID和批次ID
   Long logId = jobInfoService.triggerJob(jobId, randomId);
   ```

3. **日志轮询**
   ```java
   // 每2秒轮询一次日志
   logTimer.schedule(task, 1000, 2000);
   ```

4. **增量获取**
   ```java
   // 只获取新增的日志内容
   jobLogService.getLogDetail(logId, fromLineNum);
   ```

5. **自动结束**
   ```java
   // 检测到结束标志或失败次数过多时停止
   if (content.isEnd() || pullFailCount > 20) {
       stopLogPolling();
   }
   ```

## API接口

### 后端API列表

1. **获取任务树**
   - URL: `/api/v1/jobParts/getTree`
   - Method: GET
   - 返回: 任务组树形结构

2. **获取任务组数据**
   - URL: `/api/v1/jobInfos/getJobCompose`
   - Method: POST
   - 参数: `{ "id": jobId, "type": 0 }`
   - 返回: 任务组节点和边数据

3. **触发任务执行**
   - URL: `/api/v1/jobInfos/trigger`
   - Method: POST
   - 参数: `{ "id": jobId, "executorParam": randomId }`
   - 返回: 执行日志ID

4. **获取执行日志**
   - URL: `/api/v1/jobLog/logDetailCat`
   - Method: GET
   - 参数: `id={logId}&fromLineNum={lineNum}`
   - 返回: 增量日志内容

5. **停止任务组**
   - URL: `/api/v1/jobInfos/stopJobCompose/{jobId}/{randomId}`
   - Method: GET
   - 返回: 停止结果

## 常见问题

### Q1: 无法连接后端服务

**A:** 请确认：
1. 后端服务已启动：`mvn spring-boot:run`
2. 服务端口正确：默认8080
3. 配置文件正确：检查 `application.properties`
4. 防火墙未阻止连接

### Q2: 任务无法执行

**A:** 请检查：
1. 是否已选择任务组
2. 任务组是否有节点
3. 后端服务是否正常
4. 查看日志面板的错误信息

### Q3: 日志无法显示

**A:** 请确认：
1. 任务是否已成功触发
2. 是否获取到日志ID
3. 后端日志API是否正常
4. 网络连接是否稳定

### Q4: 如何停止运行中的任务

**A:** 
1. 点击工具栏的"🛑 停止"按钮（即将添加）
2. 或者重启应用程序
3. 或者在后端管理界面停止任务

## 开发计划

### 近期计划
- [ ] 添加停止按钮到工具栏
- [ ] 支持 WebSocket 实时推送日志
- [ ] 节点状态可视化（运行中、完成、失败）
- [ ] 边的动画效果（数据流动）
- [ ] 任务执行进度条

### 远期计划
- [ ] 节点右键菜单（编辑、删除、复制）
- [ ] 画布缩放和平移
- [ ] 节点搜索和定位
- [ ] 导出流程图为图片
- [ ] 历史版本管理
- [ ] 多任务组同时运行
- [ ] 任务组模板功能

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

[MIT License](LICENSE)

## 技术支持

如有问题，请联系：xiaozhao@example.com

---

**最后更新**: 2024-11-01  
**版本**: v1.1.0  
**作者**: xiaozhao
