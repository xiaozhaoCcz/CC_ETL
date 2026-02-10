# 组件文档 (Component Documentation)

本章介绍三个核心组件：**cc-job-executor**（单任务执行器）、**cc-job-executor-compose**（任务组编排执行器）、以及 **DataX 集成**（Admin + Executor 侧）。

---

## cc-job-executor（单任务执行器）

### 职责

- 向 Admin 注册，接收调度请求，执行**单任务**（非任务组）。
- 根据任务类型调用对应 **JobHandler**，执行完成后回调 Admin（结果、日志等）。
- 支持 BEAN、API、SQL、DataX 及各类 GLUE 任务（GLUE 由 XXL-Job 内核或扩展执行）。

### Handler 列表

| Handler | 说明 |
|---------|------|
| **ApiHandler** | HTTP/HTTPS 任务，内部使用 **HttpTaskExecutor** 发起请求。 |
| **JobJdbcHandler** | SQL 任务，使用 **JdbcTaskExecutor**、**JdbcCommand** 执行 SQL（多数据源、参数化）。 |
| **DataxHandler** | DataX 同步任务，使用 **DataxTaskExecutor**、**DataxCommandBuilder**、**DataxProcessRunner** 生成 JSON 并调用 DataX 进程。 |

BEAN 与 GLUE 类任务由 XXL-Job 内核在 Executor 内调度执行（如 GLUE 通过脚本引擎执行）。

### 核心类

- **JdbcCommand**：封装 JDBC 执行（连接、语句、参数、结果）。
- **HttpTaskExecutor**：OkHttp 等发起 HTTP 请求，处理超时与响应。
- **JdbcTaskExecutor**：按数据源执行 SQL，支持查询与更新。
- **DataxTaskExecutor**：组装 DataX 参数、调用 **DataxProcessRunner** 执行 `datax.py`，并处理日志与退出码。
- **DataxCommandBuilder**：根据任务参数与 Admin 下发的配置构建 DataX Job JSON 或命令行参数。
- **IncrementalDataRefresher**：增量数据刷新相关逻辑（若使用增量同步）。

---

## cc-job-executor-compose（任务组编排执行器）

### 职责

- 作为**任务组**专用执行器注册到 Admin；Admin 触发任务组时，将请求发往 Compose。
- 从 Admin 拉取任务组的 **节点（job_node）** 与 **边（job_edge）**，构建依赖图，进行**拓扑排序**，按层级并行/串行执行。
- 每个节点对应一个子任务（job_info）：Compose 通过 XXL-Job 或 HTTP 触发 **cc-job-executor** 执行该任务，并接收回调或轮询状态；支持**条件节点**（ConditionNode）的表达式求值决定分支。
- 将节点执行状态通过 Admin 的接口（如 **/api/v1/jobInfos/status**）上报，供 PC 端 SSE 展示。

### 与 Admin / Executor 的协作

```
Admin 触发任务组（job_group）
    → Executor-Compose 接收 XXL-Job 回调
    → AdminApiClient 拉取 getJobCompose、nodes、edges
    → TaskGraphBuilder 构建图，TaskDependencyBuilder 构建依赖
    → TaskGroupOrchestrator 编排：Async 拓扑执行 WorkerWrapper
    → 每个 Wrapper 内通过 JobGroupUtils 等触发 Executor 执行单任务
    → 上报节点状态到 Admin（saveNodeStatus / status）
    → Admin 通过 SSE 推送到 PC 端
```

### 核心类

| 类 | 说明 |
|----|------|
| **TaskGroupOrchestrator** | 编排入口：拉取任务组数据、构建图、创建 WorkerWrapper、调用 Async 执行、上报状态、处理条件节点。 |
| **TaskGraphBuilder** | 根据节点与边构建内存中的 DAG。 |
| **TaskDependencyBuilder** | 解析依赖关系，为拓扑排序提供入度/层级等信息。 |
| **TaskWrapperFactory** | 将 JobNode 封装为 **WorkerWrapper**（含执行逻辑与回调）。 |
| **TaskExecutor** | 具体执行单节点任务（如通过 HTTP 触发 Executor 或调用 XXL-Job 触发）。 |
| **ConditionEvaluator** | 条件节点表达式求值，决定下游分支是否执行。 |
| **ParameterResolver** | 节点参数解析（如引用上游节点输出）。 |
| **ResultStorageService** / **HistoricalDataLoader** | 节点结果存储与历史加载，供下游或条件节点使用。 |
| **DataContextManager** / **DataContext** | 执行期上下文与数据传递。 |
| **JobCallbackController** | 接收 Executor 或内部回调（如任务完成），更新状态并驱动后续节点。 |

拓扑与并行/串行由 **Async** 及 **WorkerWrapper** 的依赖关系决定：同一层级可并行，不同层级按依赖顺序执行。

---

## DataX 集成

### 整体流程

1. **配置数据源**：在 PC 端或通过 API 维护 **job_jdbc_datasource**（MySQL、Oracle、PostgreSQL 等）。
2. **生成 JSON**：PC 端或接口选择 Reader/Writer 表与列，调用 Admin 的 **DataxController**（getTables、getColumns、getJson、batchBuildJson）生成 DataX Job JSON。
3. **任务执行**：任务类型为 DataX 时，Admin 将任务下发给 **cc-job-executor**；Executor 的 **DataxHandler** 使用 **DataxTaskExecutor**，通过 **DataxCommandBuilder** 生成或取得 Job JSON，由 **DataxProcessRunner** 调用系统已安装的 **datax.py** 执行 JSON，并读取 stdout/stderr 写回日志与结果。

### Admin 侧

- **DataxService** / **DataxServiceImpl**：封装表/列查询、JSON 构建；调用 **reader**、**writer** 包下的实现。
- **DataxUtils**：通用工具（如 JSON 拼接、字段映射）。
- **datax/reader**：**MysqlReader**、**OracleReader**、**PostgreSqlReader** 等，生成 Reader 配置。
- **datax/writer**：**MysqlWriter**、**OracleWriter**、**PostgreSqlWriter** 等，生成 Writer 配置。
- **BaseRW**：Reader/Writer 公共基类或配置模板。

### Executor 侧

- **DataxCommandBuilder**：根据任务参数与（可选）Admin 下发的配置，生成 DataX 使用的 Job JSON 或命令行参数。
- **DataxProcessRunner**：启动外部 DataX 进程（指定 `pypath`、`jsonpath` 等），等待结束，收集输出。
- **DataxTaskExecutor**：协调 Builder 与 Runner，处理超时与异常，将执行结果与日志回写。
- **DataxUtils**：Executor 内 DataX 相关工具。

### 支持的数据源

- **MySQL**、**Oracle**、**PostgreSQL**（Reader/Writer 在 Admin 中有对应实现）；其他数据源可在 Admin/Executor 中扩展 Reader/Writer 或 JSON 模板。

---

[返回文档首页](README.md) | [上一章：前端手册](06-frontend-manual.md) | [下一章：项目扩展](08-extension.md)
