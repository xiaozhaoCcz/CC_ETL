# Cc_ETL Wiki

## 项目概述

Cc_ETL 是在 XXL-Job 基础上深度定制的可视化任务调度平台，提供拖拽式任务编排、调度监控、数据同步、失败告警等能力。平台通过 Web、桌面端和后端服务协同工作，实现任务的全生命周期管理，适用于数据同步、批处理、接口定时调用等场景。

## 核心模块

| 模块 | 说明 |
| --- | --- |
| `cc-job/cc-job-admin` | 调度中心，负责任务注册、调度、日志查询与告警。 |
| `cc-job/cc-job-core` | 核心调度逻辑与公共组件，实现触发器、执行器通信。 |
| `cc-job/cc-job-executor` | 默认执行器实现，支持 Bean、API、SQL、Shell 等任务类型。 |
| `cc-job/cc-job-executor-samples` | 执行器接入示例，帮助快速集成自定义业务。 |
| `cc-job/cc-job-xo` | 数据访问层，定义实体、Mapper 与数据库交互逻辑。 |
| `cc-job-web` | Web 管理端，提供任务配置、编排、运行监控等可视化能力。 |
| `cc_job_pc` | Electron 桌面端，便于本地运维或内网使用。 |
| `doc/` | 本地说明文档、示例截图、数据库初始化脚本等辅助资料。 |

## 系统架构

- **数据库**：MySQL（执行 `doc/cc_etl.sql` 初始化）。  
- **后端**：Spring Boot + XXL-Job 改造版，暴露调度接口与 REST API。  
- **前端**：Vue3 + Vite 构建的管理界面，Electron 封装桌面端。  
- **执行器**：通过 HTTP 与调度中心通信，支持自定义任务逻辑与 DataX 数据交换。  
- **WebSocket**：实时推送任务状态与日志。

## 环境要求

- JDK 8+
- Maven 3.6+
- Node.js 16+（推荐使用 pnpm 或 npm）
- MySQL 5.7/8.0
- Git

## 快速开始

### 1. 克隆项目

```bash
git clone https://gitee.com/xiaozhao/Cc_ETL.git
cd Cc_ETL
```

### 2. 初始化数据库

```bash
mysql -u root -p < doc/cc_etl.sql
```

> 提前在 MySQL 中创建目标库（如 `cc_etl`），并修改 SQL 文件中的库名或连接配置。

### 3. 启动调度中心

```bash
cd cc-job/cc-job-admin
mvn spring-boot:run
```

默认访问地址：`http://localhost:8989/cc-job-admin`，账号密码参考配置文件或数据库初始化数据。

### 4. 启动执行器示例

```bash
cd ../cc-job-executor-samples/cc-job-executor-sample-springboot
mvn spring-boot:run
```

确认 `application.properties` 中的 `xxl.job.admin.addresses` 指向调度中心地址。

### 5. 启动 Web 管理端

```bash
cd ../../../cc-job-web
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`，配置执行器、数据源、任务节点并进行编排。

### 6. 启动桌面端（可选）

```bash
cd ../cc_job_pc
npm install
npm run dev
```

或使用 `npm run build && npm run build:electron` 生成打包产物。

## 常见任务编排流程

1. 在 Web 管理端配置执行器、数据源和运行参数。  
2. 新建任务分区、任务组，选择目标执行器。  
3. 通过拖拽或面板添加节点，设置类型（Bean、API、DataX、SQL、Shell 等）。  
4. 配置依赖关系、运行策略（串行、并行、失败重试、超时处理等）。  
5. 提交任务组并触发运行，实时查看日志和状态。  
6. 结合告警配置，将异常通知到邮件或自定义渠道。

## 目录结构速览

```text
Cc_ETL/
├─ cc-job/                 后端源码（调度中心、执行器、核心模块）
├─ cc-job-web/             Web 管理端
├─ cc_job_pc/              桌面端
├─ doc/                    文档与素材
├─ logs/                   日志输出样例
└─ README.md               总览与快速导航
```

## 运维与监控

- 调度中心支持任务日志查看、手动触发、暂停、失败重试。  
- 建议开启 `xxl.job.executor.logretentiondays` 日志清理，避免磁盘占满。  
- 可在 `cc-job-admin` 中配置报警联系人，或接入自定义告警服务。  
- 对批量任务建议结合分布式锁或幂等设计，避免重复执行。

## 常见问题

- **端口冲突**：调度中心默认使用 `8989`，执行器默认 `10000`，启动前确认未被占用。  
- **无可用执行器**：检查执行器是否注册成功以及访问令牌配置是否一致。  
- **任务失败重试失效**：确认节点配置中已开启重试且未超过阈值。  
- **DataX 任务执行失败**：核对 JSON 配置与数据源连通性，关注日志详细信息。

## 贡献指南

1. Fork 仓库并拉取最新 `main` 分支。  
2. 新建特性分支，完成开发后运行对应模块的测试或构建。  
3. 提交 PR 前附上变更说明，必要时更新文档与截图。  
4. 如涉及执行器扩展，请补充示例或测试用例。

## 许可协议

项目遵循 MIT License，详情见仓库根目录的 `LICENSE` 文件。


