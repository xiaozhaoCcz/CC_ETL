# 后台手册 (Backend Manual)

本章介绍 Admin 调度中心、任务与任务组模型、数据源与 DataX、以及系统 REST API、认证与实时推送（SSE/WebSocket）。

---

## 调度中心

### 职责

- **任务与执行器注册**：维护 `job_info`、`job_registry` 等，供调度与路由使用。
- **调度触发**：根据 Cron 或手动触发，将任务下发到对应执行器（单任务到 Executor，任务组到 Executor-Compose）。
- **回调与日志**：接收执行器上报的执行结果、日志，写入 `job_log` 等。
- **监控与告警**：任务执行统计、失败告警（如邮件）。

### 核心类与线程

- **XxlJobScheduler**：调度入口，启动时初始化调度与注册等。
- **JobScheduleHelper**：扫描待触发任务，放入触发池。
- **JobTriggerPoolHelper**：快/慢触发线程池，执行具体触发（调用 **XxlJobTrigger**）。
- **ExecutorRouter**：根据路由策略（第一个、最后一个、轮询、随机、一致性哈希、LFU、LRU、故障转移、忙碌转移、分片广播）从注册表中选执行器地址。
- **CronExpression**：解析 Cron 表达式，计算下次触发时间。

### 触发与失败策略

- 支持 **Cron**、**固定速率** 等；失败策略：**忽略** 或 **失败**（标记失败并可能告警）；阻塞策略：**单机串行**、**丢弃后续**、**覆盖之前**。

---

## 任务与任务组

### 数据模型与表

| 表/概念 | 说明 |
|---------|------|
| **job_info** | 单任务配置：描述、执行器、Cron、运行模式（BEAN/API/SQL/GLUE/DataX 等）、参数、路由/阻塞/失败策略、超时、重试等。 |
| **job_group** | 任务组（一个可编排的“作业”）：关联执行器为 Executor-Compose，对应一个画布。 |
| **job_node** | 任务组内节点：每个节点关联一个 job_info（或为条件节点等），含位置、入度/出度等。 |
| **job_edge** | 任务组内边：from_node_id、end_node_id、job_parent_id（任务组 ID），表示依赖。 |
| **job_compose** | 执行器注册信息：app_name（如 cc-job-executor-compose）、executor_address 等，用于任务组选择执行器。 |

任务组保存时，会校验节点与边的合法性；**validateJobComposeEdge** 用于校验边的合法性（如是否存在循环依赖的检测支持）。

### 任务/任务组相关接口概览

- **JobInfoController**（`/api/v1/jobInfos`）：任务 CRUD、分页/列表、触发、启停、暂停、Cron 下次时间、GLUE 保存与查询；任务组编排：getJobCompose、saveJobCompose、updateJobCompose、stopJobCompose、validateJobComposeEdge；节点/边：saveJobNode、saveJobNodeAndJobEdges、saveJobEdge、addExistingJobToCompose、createConditionNode、updateJobNode、deleteJobNode、updateNodeStatus、batchUpdateNodeStatus；节点/边查询：nodes/{jobId}、edges/{jobId}；状态上报：/status、/batch 等。
- **JobGroupController**（`/api/v1/jobGroups`）：任务组 CRUD、分页、表单、findAddressList（执行器地址列表）、getAllJobGroupList。
- **JobPartController**（`/api/v1/jobParts`）：分区树 getTree、saveJobPart、updateJobPart、deleteJobPart、getChildren；导入导出 exportData、exportTaskGroup、importData、importTaskGroup。

---

## 数据源与 DataX

### 数据源管理

- **job_jdbc_datasource**：存储 JDBC 数据源配置（类型、URL、用户名、密码等）。
- **JobJdbcDatasourceController**（`/api/v1/jobJdbcDatasource`）：分页、列表、增删改、表单、isConnect 测试连接。
- Admin 内 **DataxService**、**DataxUtils** 及 **datax/reader**、**datax/writer**（MysqlReader/Writer、OracleReader/Writer、PostgreSqlReader/Writer）用于生成 DataX JSON 与表/列查询。

### DataX 接口

- **JobDataxController**（`/api/v1/datax`）：
  - **getTables/{id}**：按数据源 ID 获取表列表。
  - **getColumns/{id}**（POST）：获取表列信息。
  - **getJson**（POST）：根据参数生成单任务 DataX JSON。
  - **batchBuildJson**（POST）：批量构建 DataX JSON。

同步流程：PC 端或接口选择数据源与表/列 → 调用上述接口生成 JSON → 任务执行时由 Executor 将 JSON 交给 DataX 进程执行（见 [组件文档 - DataX 集成](07-components.md#datax-集成)）。

---

## 系统接口（REST API 清单）

以下按 Controller 分组列出主要接口前缀与方法，便于查阅与对接。实际请求需带认证（如 JWT）及 Base URL（如 `http://host:8989`）。

### 认证

| 前缀 | 方法 | 路径 | 说明 |
|------|------|------|------|
| /api/v1/auth | POST | /login | 登录 |
| /api/v1/auth | POST | /register | 注册 |
| /api/v1/auth | DELETE | /logout | 登出 |

### 仪表盘与菜单

| 前缀 | 方法 | 路径 | 说明 |
|------|------|------|------|
| /api/v1/dashboard | GET | /stats | 统计 |
| /api/v1/menus | GET | /routes | 菜单/路由 |

### 任务（JobInfo）

| 前缀 | 方法 | 路径/说明 |
|------|------|------------|
| /api/v1/jobInfos | GET | /page、/list、/{id}、/{id}/form、/initData |
| /api/v1/jobInfos | POST | 新增、/trigger、saveJobCompose、saveGlueSource、getJobCompose、validateJobComposeEdge、saveJobNode、saveJobNodeAndJobEdges、saveJobEdge、addExistingJobToCompose、createConditionNode、pauseJobs、updateNodeStatus、batchUpdateNodeStatus、/status、/batch |
| /api/v1/jobInfos | PUT | /{id}、updateJobCompose/{id} |
| /api/v1/jobInfos | GET | /startJob/{id}、/stopJob/{id}、/pauseJob/{id}、/nextTriggerTime、/stopJobCompose/{id}/{randomId}、/getJobStatus/{id}、/updateJobNode/{jobId}/{nodeId}、/deleteJobNode/{nodeId}、/nodes/{jobId}、/edges/{jobId} |
| /api/v1/jobInfos | POST | /updateRankTriggerStatus/{id} |
| /api/v1/jobInfos | GET | getGlueList/{id}、getGlueList/{id}/{glueType} |
| /api/v1/jobInfos | DELETE | /{ids} |

### 任务组（JobGroup）

| 前缀 | 方法 | 路径 |
|------|------|------|
| /api/v1/jobGroups | GET | /{id}、/page、/{id}/form、/findAddressList/{id}、/getAllJobGroupList |
| /api/v1/jobGroups | POST | 新增 |
| /api/v1/jobGroups | PUT | /{id} |
| /api/v1/jobGroups | DELETE | /{ids} |

### 分区（JobPart）

| 前缀 | 方法 | 路径/说明 |
|------|------|------------|
| /api/v1/jobParts | GET | getTree、getChildren/{id}/{type}、deleteJobPart/{id}、exportData/{id}、exportTaskGroup/{jobId} |
| /api/v1/jobParts | POST | saveJobPart、updateJobPart、importData、importTaskGroup |

### 日志与节点结果

| 前缀 | 方法 | 路径/说明 |
|------|------|------------|
| /api/v1/jobLogs | GET | /page、/logDetailCat |
| /api/v1/jobLogs | POST | /saveNodeStatus |
| /api/v1/jobLogs | DELETE | 删除 |
| /api/v1/jobNodeResults | POST | /batchSave |
| /api/v1/jobNodeResults | GET | /byBatch、/latestBatchId、/latestFullRunBatchId、/latestByJob |

### 数据源与 DataX

| 前缀 | 方法 | 路径 |
|------|------|------|
| /api/v1/jobJdbcDatasource | GET | /page、/list、/{id}/form |
| /api/v1/jobJdbcDatasource | POST | 新增、/isConnect |
| /api/v1/jobJdbcDatasource | PUT | /{id} |
| /api/v1/jobJdbcDatasource | DELETE | /{ids} |
| /api/v1/datax | GET | /getTables/{id} |
| /api/v1/datax | POST | /getColumns/{id}、/getJson、/batchBuildJson |

### 画布书签与节点模板

| 前缀 | 方法 | 路径 |
|------|------|------|
| /api/v1/jobCanvasBookmarks | GET | 列表、/{id} |
| /api/v1/jobCanvasBookmarks | POST | 新增 |
| /api/v1/jobCanvasBookmarks | DELETE | /{id} |
| /api/v1/jobNodeTemplates | GET | 列表、/{id} |
| /api/v1/jobNodeTemplates | POST | 新增 |
| /api/v1/jobNodeTemplates | PUT | /{id} |
| /api/v1/jobNodeTemplates | DELETE | /{id} |

### 实时与监控

| 前缀 | 方法 | 路径 | 说明 |
|------|------|------|------|
| /api/v1/sse | GET | /nodeStatus/{parentJobId}/{randomId} | SSE 节点状态流 |
| /api/v1/sse | DELETE | /nodeStatus/{parentJobId}/{randomId} | 关闭该 SSE |
| /api/websocket | GET | /stats、/performance、/health | WebSocket 监控 |
| /xxl-job-admin/api | GET | /sse/{jobId}/{randomId}、/sse/stats | XXL-Job 兼容 SSE |
| /xxl-job-admin/api | 多种 | /{uri} | XXL-Job 兼容 API 透传 |

---

## 认证方式

- 登录接口返回 **JWT**（或类似 Token），后续请求在 Header 中携带（如 `Authorization: Bearer <token>`）。
- 登出可依赖前端清除 Token；服务端可做 Token 失效（若已实现）。

---

## SSE 与 WebSocket

- **SSE**（/api/v1/sse/nodeStatus/{parentJobId}/{randomId}）：任务组执行时，按 parentJobId + randomId 推送节点状态变更，PC 端订阅后更新画布节点状态与日志。
- **WebSocket**（/api/websocket/*）：用于监控统计、性能、健康等实时数据（具体报文格式见服务端实现与前端调用）。

---

[返回文档首页](README.md) | [上一章：项目介绍](04-project-intro.md) | [下一章：前端手册](06-frontend-manual.md)
