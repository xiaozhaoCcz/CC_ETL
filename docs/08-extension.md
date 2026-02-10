# 项目扩展 (Project Extension)

本章说明如何在 Cc-ETL 中扩展自定义任务类型（Handler）、DataX Reader/Writer、编排策略，以及配置与二次开发建议。

---

## 添加自定义 JobHandler（Executor）

要在 **cc-job-executor** 中支持新的任务类型，可新增一个 Handler，供 XXL-Job 调度时按 `JobHandler` 名称调用。

### 步骤概要

1. **在 cc-job-executor 中实现处理器类**
   - 使用 XXL-Job 的 **@XxlJob("handlerName")** 注解，方法内通过 **XxlJobHelper.getJobId()**、**TriggerParam** 等获取任务参数。
   - 调用你自己的执行逻辑（如调用外部服务、执行脚本、读写数据等），最后用 **XxlJobHelper.log()**、**XxlJobContext** 等回写日志与结果。

2. **注册为 Spring Bean**
   - 使用 **@Component**（或 **@Service**）让 Spring 扫描并注册；确保该 Handler 所在包被 Executor 启动类扫描到。

3. **在 Admin 与 PC 端配置任务时选择该 Handler**
   - 在任务配置中，“JobHandler” 填写你注解中的名称（如 `runApiHandler`、`runDataxHandler`）；若需在 PC 端“运行模式”中展示，需在前端或配置中增加对应选项。

### 参考现有实现

- **ApiHandler**：`@XxlJob("runApiHandler")`，内部使用 **HttpTaskExecutor** 执行 HTTP 请求。
- **JobJdbcHandler**：执行 SQL，使用 **JdbcTaskExecutor**、**JdbcCommand**。
- **DataxHandler**：执行 DataX，使用 **DataxTaskExecutor**、**DataxProcessRunner**。

可仿照上述类，新建包与类，注入自己的业务逻辑类，并保持与 XXL-Job 的入参、日志、结果约定一致。

---

## 扩展 DataX Reader/Writer

### Admin 侧

- **Reader**：在 `cc-job-admin` 的 **task/datax/reader** 包下，参考 **MysqlReader**、**OracleReader**、**PostgreSqlReader** 实现新的 Reader 类（继承或复用 **BaseRW** 等），在 **DataxService** 中根据数据源类型选择并调用，生成 DataX Job 的 reader 配置。
- **Writer**：在 **task/datax/writer** 下同样增加新 Writer 实现，并在 DataxService 中组装到 Job JSON。
- 若新数据源需要单独的表/列查询接口，可在 **JobDataxController** 或 **DataxService** 中扩展（如按数据源类型路由到不同查询逻辑）。

### Executor 侧

- **DataxCommandBuilder** / **DataxProcessRunner**：若 Job JSON 完全由 Admin 下发，Executor 侧可能只需透传；若 Executor 也会本地生成或补全 JSON，可在 **DataxCommandBuilder** 中增加对新 Reader/Writer 类型的支持，或从任务参数中解析并写入对应配置块。
- **DataxUtils**：可增加新数据源相关的工具方法（如默认参数、路径转换等）。

确保 Admin 与 Executor 对同一任务类型生成的 DataX Job 结构一致，避免执行时报错。

---

## 编排策略扩展（Executor-Compose）

- **TaskGroupOrchestrator**、**TaskExecutor**、**Async**、**WorkerWrapper** 决定了“按图执行”与“拓扑并行”的流程；若需改变执行策略（如限流、优先级、重试策略），可在这些类或它们调用的服务中扩展。
- **ConditionEvaluator**：条件节点的表达式求值；若要支持更多语法或函数，可扩展 **ConditionEvaluator** 或接入脚本引擎。
- **ParameterResolver**：节点参数解析（如引用上游输出）；可在此增加新的参数来源或格式。
- **路由策略**：若任务组内子任务仍通过 Admin 触发到 Executor，路由逻辑主要在 Admin 的 **ExecutorRouter** 与 **route/strategy** 下；Compose 侧若也有“选哪台 Executor”的逻辑，需在对应调用处扩展。

---

## 配置与扩展点

- **路由策略**：Admin 的 **ExecutorRouter** + **route/strategy** 包下已有多种策略（第一个、最后一个、轮询、随机、一致性哈希、LFU、LRU、故障转移、忙碌转移、分片广播）；新增策略可实现同一接口并注册到路由选择逻辑中。
- **告警**：任务失败告警在 Admin 的 **task/alarm** 包（如 **JobAlarmer**、**EmailJobAlarm**）；可实现 **JobAlarm** 接口或新增告警渠道（短信、Webhook 等），并在调度/回调流程中触发。
- **日志与回调**：**job_log**、**job_node_result** 的写入在 Admin 的 **JobLogHelper**、**JobCompleteHelper** 及 Executor 回调接口中；若需对接外部日志系统或审计，可在这些位置增加旁路逻辑。

---

## 二次开发建议与规范

- **代码规范**：建议遵循阿里巴巴 Java 开发规范；关键类与方法使用 JavaDoc 注释；包名小写、类名大驼峰。
- **分支与提交**：功能分支使用 `feature/xxx`，修复使用 `fix/xxx`；提交信息建议使用 `type(scope): description`（如 feat(executor): 添加某某 Handler）。
- **测试**：新 Handler、新 DataX 类型、编排逻辑变更建议补充单元测试或集成测试；PC 端新功能可参考 [GUI 功能测试文档](../doc/测试文档/GUI功能测试文档.md) 补充用例。
- **文档**：新接口、新配置项、新运行模式建议同步更新 [后台手册](05-backend-manual.md)、[前端手册](06-frontend-manual.md) 或 [组件文档](07-components.md)。

更完整的贡献流程、分支管理、提交规范与行为准则见项目 [README - 贡献指南](../README.md#-贡献指南)。

---

[返回文档首页](README.md) | [上一章：组件文档](07-components.md) | [下一章：视频教程与更新日志](09-video-and-changelog.md)
