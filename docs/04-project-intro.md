# 项目介绍 (Project Introduction)

本章介绍 Cc-ETL 的仓库文件结构、后端与前端模块结构、核心技术及技术栈。

---

## 文件结构

仓库根目录概览：

| 路径 | 说明 |
|------|------|
| **cc-job/** | 主工程目录，包含所有后端与 PC 端模块 |
| **doc/** | 项目文档与脚本：`cc_etl.sql` 数据库脚本、`image/` / `imageGui/` 截图、`测试文档/` GUI 测试文档 |
| **README.md** | 项目说明、快速开始、模块说明、技术栈、常见问题 |
| **LICENSE** | 许可证（MIT） |

---

## 后端结构

以下为 `cc-job` 下后端相关模块的树状结构及职责说明（端口与职责标注在括号内）。

```text
cc-job/                              # 父 POM，依赖管理、模块聚合
├── cc-job-admin/                    # 调度中心 [8989]
│   ├── task/controller/             # REST API：任务、任务组、节点、边、数据源、DataX、认证、SSE、WebSocket 等
│   ├── task/service/                # 业务服务：JobInfo、JobGroup、JobNode、JobEdge、Datax、用户等
│   ├── task/scheduler/              # 调度器（XxlJobScheduler）
│   ├── task/trigger/                # 触发器（XxlJobTrigger）
│   ├── task/route/                  # 执行器路由策略
│   ├── task/datax/                  # DataX Reader/Writer（Mysql、Oracle、PostgreSql）
│   ├── task/thread/                 # 调度线程、日志、注册、完成回调等
│   ├── config/                     # 配置类、MyBatis、Web、XXL-Job
│   └── ...
├── cc-job-core/                     # XXL-Job 核心（调度内核、RPC、模型）
├── cc-job-executor/                 # 单任务执行器 [8400 / 10000]
│   ├── handler/                    # ApiHandler、JobJdbcHandler、DataxHandler 等
│   ├── core/service/               # HttpTaskExecutor、JdbcTaskExecutor、DataxTaskExecutor、DataX 命令构建与执行
│   └── ...
├── cc-job-executor-compose/         # 任务组编排执行器 [8500 / 15000]
│   ├── core/                       # 编排核心：TaskGraphBuilder、TaskDependencyBuilder、TaskGroupOrchestrator、TaskExecutor 等
│   ├── controller/                 # 任务回调、任务组控制接口
│   ├── client/                     # Admin API 客户端
│   └── ...
├── cc-job-xo/                       # 数据与模型层
│   ├── entity/                     # 实体：JobInfo、JobGroup、JobNode、JobEdge、JobLog、JobUser 等
│   ├── mapper/                     # MyBatis Mapper
│   ├── model/dto、vo、form、query、result  # DTO、VO、表单、查询、结果对象
│   └── ...
├── cc-job-gui/                      # PC 端 JavaFX 应用（见下方「前端结构」）
└── pom.xml
```

---

## 前端结构（PC 端）

当前管理界面以 **PC 端（cc-job-gui）** 为主，无独立前端工程；结构如下。

```text
cc-job-gui/
├── view/                            # 界面与对话框
│   ├── MainView.java                # 主界面布局
│   ├── TopToolBar.java              # 顶部工具栏
│   ├── NodeCanvas.java              # 画布
│   ├── TaskTreeView.java            # 树形视图
│   ├── TaskNavigationBar.java      # 任务导航栏
│   ├── LogPanel.java                # 日志面板
│   ├── MiniMapView.java             # 小地图
│   ├── NewJobDialog、NewJobGroupDialog、NewJobNodeDialog  # 新建任务/任务组/节点
│   ├── ShowJobListDialog、ShowExecutorListDialog、ShowDatasourceListDialog  # 任务/执行器/数据源列表
│   ├── ShowDataxSyncDialog、ShowDataxGroupSyncDialog     # DataX 同步
│   ├── GlueIdeDialog、NodeDetailsDialog  # GLUE 编辑器、节点详情
│   └── ...
├── manager/                         # 业务与画布管理
│   ├── CanvasNodeManager、CanvasConnectionManager、CanvasDataLoader、CanvasLayoutManager
│   ├── CanvasSelectionManager、CanvasExportManager、CanvasTemplateManager
│   ├── TreeDataManager、TreeContextMenuManager、TreeSearchManager
│   ├── TaskExecutionManager、LogContentManager、LogTabManager
│   ├── NodeOperationManager、NodeTemplateManager、CycleDetectionManager
│   └── ...
├── service/                         # 与后端 API 交互
│   ├── LoginService、JobGroupService、JobInfoService、JobLogService
│   ├── JobJdbcDatasourceService、JobDataxService、JobCanvasBookmarkApiService
│   ├── SSEService、WebSocketService、DashboardService
│   └── ...
├── model/                            # 界面数据模型
│   ├── ProcessNode、ConditionNode、NodeConnection、JobComposeData
│   ├── GroupContainer、TreeNodeData、RunningJobGroup
│   └── ...
├── history/                         # 撤销/重做
│   ├── UndoRedoManager、CanvasAction、NodeStyleSnapshot、EdgeStyleSnapshot
│   └── ...
├── infrastructure/                  # 基础设施
│   ├── client/（ApiClient、HttpClientFactory）
│   ├── config/（ApplicationProperties）
│   └── exception/（ApiException）
└── util/                            # 工具与主题
    ├── ThemeManager、StyleUtil、IconUtil、NotificationToast
    ├── ConfigManager、SessionManager、SnowflakeIdGenerator
    └── ...
```

---

## 核心技术

| 领域 | 技术/能力 | 说明 |
|------|------------|------|
| **任务调度** | XXL-Job | 兼容其调度模型，Cron 触发、执行器路由、回调、日志 |
| **任务编排** | 拓扑排序与 DAG | Executor-Compose 根据节点与边构建依赖图，按层级并行/串行执行 |
| **数据同步** | DataX | Admin 侧生成 Reader/Writer 配置，Executor 侧调用 DataX 执行同步 |
| **实时推送** | SSE / WebSocket | 节点执行状态、日志等通过 SSE 或 WebSocket 推送到 PC 端 |

---

## 后端技术栈

| 技术组件 | 版本 | 用途说明 |
|----------|------|----------|
| Java | 17+ | 运行环境 |
| Spring Boot | 3.0.7+ | 应用框架 |
| Spring MVC | - | Web 与 REST API |
| MyBatis Plus | 3.5.3+ | 持久层 |
| XXL-Job | 2.x | 调度内核 |
| DataX | 最新版 | 数据同步（可选） |
| Druid | 1.2.x | 连接池 |
| MySQL | 5.7+ | 主库 |
| Knife4j | 4.5.0 | API 文档（可选） |
| Hutool、Gson、OkHttp 等 | 见父 POM | 工具与 HTTP 客户端 |

---

## 前端技术栈（PC 端）

| 技术组件 | 版本 | 用途说明 |
|----------|------|----------|
| JavaFX | 17+ | 桌面 GUI |
| Java | 17+ | 与后端一致 |
| Maven | 3.6+ | 构建与依赖 |
| Scene Builder | 可选 | 界面设计 |

---

[返回文档首页](README.md) | [上一章：环境部署](03-environment.md) | [下一章：后台手册](05-backend-manual.md)
