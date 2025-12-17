# 🚦 Cc-ETL

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-orange.svg" alt="Java Version">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.0+-brightgreen.svg" alt="Spring Boot Version">
  <img src="https://img.shields.io/badge/Vue-3.0+-brightgreen.svg" alt="Vue Version">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey.svg" alt="Platform">
</p>

<p align="center"><b>基于 XXL-Job 深度改造的可视化任务调度平台</b></p>
<p align="center"><b>支持拖拽式任务编排、DataX 数据同步、多端管理界面</b></p>

---

## 📋 目录

- 📖 [项目简介](#-项目简介)
- 🔗 [项目地址](#-项目地址)
- ✨ [核心特性](#-核心特性)
- 📋 [使用场景](#-使用场景)
- 🖼️ [项目展示](#-项目展示)
- 💻 [PC端功能](#pc端功能介绍)
- 🚀 [快速开始](#-快速开始)
- 🧩 [模块说明](#-模块说明)
- 🛠️ [技术栈](#-技术栈)
- 📚 [项目文档](#-项目文档)
- ❓ [常见问题](#-常见问题)
- 🤝 [贡献指南](#-贡献指南)
- 📞 [技术支持](#-技术支持)
- 🙏 [致谢](#-致谢)
- 📄 [许可证](#-license)

---

## 📖 项目简介

Cc-ETL 是一款基于 XXL-Job 深度改造的可视化定时任务调度平台，专为数据集成和任务编排而设计。平台通过可视化拖拽界面实现复杂任务流程的编排，支持多种任务类型，并深度集成 DataX 实现高效的数据同步能力。

**目前项目还在测试阶段，还有很多功能未开发，还存在很多优化修复的地方，想学习可以下载看看**

### 🎯 项目定位

- **数据集成平台**：专业的数据管道工具，支持多数据源间的全量/增量同步
- **任务调度中心**：提供企业级的任务编排与调度能力，支持复杂的工作流
- **可视化运维工具**：双端（Web + PC）界面设计，提供直观的操作体验
- **企业级解决方案**：支持高可用集群部署，满足生产环境严苛要求

### ⭐ 项目优势

1. **🚀 零中间件依赖**：仅需 MySQL 数据库即可完整部署，极大降低运维复杂度
2. **🎨 可视化编排**：拖拽式流程设计，无需编程基础即可构建复杂任务链
3. **💻 多端协同**：Web 端轻量管理 + PC 端深度运维，满足不同使用场景
4. **🔧 功能完备**：支持 Bean、API、SQL、Shell、DataX 等多种任务类型
5. **⚡ 高性能设计**：基于 XXL-Job 内核，支持海量任务并发处理
6. **🔒 高可用保障**：支持集群部署、故障转移、负载均衡

### 🏆 项目亮点

- ✅ **开源项目**：基于 MIT 协议，完全开源免费
- ✅ **社区活跃**：持续更新迭代，积极响应用户反馈

---


### 核心组件

#### 🎯 Admin 管理端 (cc-job-admin)
- **任务注册中心**：负责任务和执行器的注册管理
- **调度中心**：处理任务调度逻辑和触发规则
- **监控中心**：提供任务执行状态监控和统计
- **Web 管理界面**：可视化任务配置和管理界面

#### ⚙️ Executor任务编排执行器 (cc-job-executor-compose)
- **任务执行引擎**：任务编排执行器
- **任务调度算法**：使用拓扑排序和图算法解决任务依赖

#### ⚙️ Executor 执行器 (cc-job-executor)
- **任务执行引擎**：实际执行各类任务的核心组件
- **多任务支持**：Bean 方法、HTTP API、SQL 脚本、Shell 命令、DataX 同步
- **状态管理**：任务执行状态跟踪和结果回调
- **日志收集**：执行过程日志收集和存储

#### 🌐 前端界面
- **Web 端**：基于 Vue 3 + Element Plus 的现代化 Web 界面
- **PC 端**：基于 JavaFX 的桌面应用程序，提供离线使用能力

### 数据流向

1. **任务配置** → Web/PC 端配置任务 → 存储到 MySQL 数据库
2. **任务调度** → Admin 根据 cron 表达式触发任务 → 分发到 Executor
3. **任务执行** → Executor 执行具体任务逻辑 → 返回执行结果
4. **状态监控** → 执行状态实时反馈 → Web/PC 端展示执行进度

---

## 📋 使用场景

### 🏢 企业应用场景

#### 1. **数据仓库 ETL 流程**
```
数据源同步 → 数据清洗 → 数据转换 → 加载到数据仓库
```
- 支持 MySQL、Oracle、PostgreSQL 等主流数据库
- 可视化配置复杂的 ETL 流程
- 支持全量和增量同步模式

#### 2. **业务数据处理**
```
用户行为收集 → 数据分析 → 报表生成 → 邮件推送
```
- 定时执行数据统计任务
- 生成各类业务报表
- 通过邮件或 webhook 推送结果

#### 3. **系统运维自动化**
```
日志清理 → 备份检查 → 健康监控 → 告警通知
```
- 自动化执行系统维护任务
- 监控系统运行状态
- 异常情况及时告警

#### 4. **API 数据集成**
```
第三方 API 调用 → 数据解析 → 本地存储 → 数据同步
```
- 定时拉取第三方系统数据
- API 数据集成和同步
- 支持 RESTful 和 SOAP 接口

### 🎓 学习与开发场景

#### 1. **任务调度学习**
- 学习分布式任务调度原理
- 掌握 cron 表达式使用
- 理解任务编排和依赖管理

#### 2. **数据集成实践**
- 学习 DataX 数据同步工具
- 掌握多数据源集成技术
- 实践 ETL 流程设计

#### 3. **全栈开发体验**
- 前后端分离开发模式
- 桌面应用开发经验
- 企业级应用架构设计

---

## 🔗 项目地址

- **GitHub**：https://github.com/xiaozhaoCcz/CC_ETL
- **Gitee**：https://gitee.com/xzjsccz/Cc_ETL

---

## ✨ 核心特性

### 🎨 可视化任务编排

- **拖拽式设计**：直观的流程图设计界面，支持节点拖拽和连接
- **复杂工作流**：支持串行、并行、条件分支等多种执行模式
- **依赖管理**：智能的任务依赖分析和拓扑排序
- **实时预览**：任务执行流程实时可视化展示

### 🔄 任务调度引擎

- **兼容 XXL-Job**：完全兼容 XXL-Job 所有原生功能
- **Cron 表达式**：支持标准的 Cron 定时任务配置
- **高级调度**：支持固定速率、固定延迟等多种调度策略
- **动态任务**：支持动态添加、修改、删除任务

### 🔗 数据集成能力

- **DataX 集成**：深度集成阿里巴巴 DataX 数据同步工具
- **多数据源支持**：
  - 🗄️ **MySQL**：全量/增量同步，表结构映射
  - 🏢 **Oracle**：企业级数据库同步支持
  - 🐘 **PostgreSQL**：开源数据库集成
  - 更多数据源持续扩展中...
- **同步模式**：全量同步、增量同步、条件同步
- **性能优化**：多线程并行处理，断点续传

### 🛠️ 多种任务类型

- **🅱️ Bean 任务**：执行 Spring 容器中的 Bean 方法
- **🌐 API 任务**：HTTP/HTTPS 接口调用，支持 RESTful
- **🗃️ SQL 任务**：执行 SQL 脚本和存储过程
- **🐚 Shell 任务**：执行系统命令和脚本
- **🔄 DataX 任务**：专业数据同步任务

### 📊 监控与运维

- **实时监控**：任务执行状态实时跟踪
- **执行日志**：详细的执行过程日志记录
- **性能统计**：任务执行时间、成功率等指标统计
- **告警通知**：支持邮件、webhook 等告警方式

### 🖥️ 多端管理界面

- **Web 端**：现代化 Web 界面，基于 Vue 3 + Element Plus
- **PC 端**：桌面应用程序，基于 JavaFX，支持离线使用
- **响应式设计**：适配不同屏幕尺寸和设备

### 🏢 企业级特性

- **高可用部署**：支持集群模式，自动故障转移
- **权限管理**：任务和用户的权限控制体系
- **审计日志**：完整的操作审计和日志记录
- **扩展性**：插件化架构，支持自定义扩展

---
## 🚀 发展规划

### ✅ 已完成功能

#### 核心功能
- ✅ **任务编排引擎重构**：提升性能和可维护性
- ✅ **可视化任务编排**：拖拽式界面设计
- ✅ **多任务类型支持**：Bean、API、SQL、Shell、DataX
- ✅ **DataX 数据同步**：集成专业数据同步工具
- ✅ **双端界面支持**：Web 端 + PC 端

#### 技术优化
- ✅ **Spring Boot 3.0**：升级到最新稳定版本
- ✅ **前端技术栈升级**：Vue 3 + TypeScript
- ✅ **数据库设计优化**：高效的数据存储结构

### 🔄 正在进行

#### 界面优化
- 🔄 **调度中心页面迁移**：功能逐步迁移到 PC 端(重点)
- 🔄 **用户体验提升**：界面交互和视觉设计优化

#### 功能增强
- 🔄 **权限管理系统**：用户和任务权限控制
- 🔄 **条件节点功能**：支持任务流程条件判断(重点)

### 📋 未来规划

#### 数据源扩展
- 📋 **更多数据源支持**：Redis、MongoDB、ES 等
- 📋 **云数据库集成**：阿里云、腾讯云、AWS 等
- 📋 **大数据平台**：Hadoop、Spark 生态集成

#### 高级特性
- 📋 **工作流引擎升级**：支持更复杂的流程控制
- 📋 **监控告警增强**：多渠道告警和智能监控
- 📋 **性能优化**：高并发场景优化和缓存机制

#### 生态建设
- 📋 **插件市场**：支持第三方插件扩展
- 📋 **API 生态**：完善 REST API 和 SDK
- 📋 **多语言支持**：国际化支持

#### 文档与社区
- 📋 **完善 API 文档**：详细的接口文档和示例
- 📋 **最佳实践指南**：使用案例和解决方案
- 📋 **视频教程**：操作指南和教学视频
- 📋 **开发者文档**：架构设计和开发指南

---

## 🖼️ 项目展示

### 🎯 界面预览

#### Web 端管理界面
<div align="center">
  <img src="./doc/image/img-md/02.png" width="45%" alt="Web端主界面"/>
  <img src="./doc/image/img-md/03.png" width="45%" alt="Web端主界面"/>
</div>

#### PC 端桌面应用
<div align="center">
  <img src="./doc/image/01.png" width="80%" alt="PC端界面"/>
</div>

### ⚙️ 核心功能展示

#### 1. 执行器与数据源配置
<div align="center">
  <img src="./doc/image/img-md/03.png" width="45%" alt="执行器配置"/>
  <img src="./doc/image/img-md/04.png" width="45%" alt="数据源配置"/>
</div>

#### 2. 可视化任务编排
<div align="center">
 <img src="./doc/image/01.png" width="45%" alt="任务执行监控"/>
  <img src="./doc/image/03.png" width="45%" alt="任务编排界面"/>
</div>

### 📋 使用流程

1. **🏗️ 环境搭建**
   - 配置数据库和后端服务
   - 启动 Admin 管理端和 Executor 执行器

2. **⚙️ 系统配置**
   - 在 Web 端配置执行器连接信息
   - 设置数据源和连接参数

3. **🎨 任务设计**
   - 创建任务分区和任务组
   - 使用拖拽界面设计任务流程
   - 配置任务参数和依赖关系

4. **▶️ 执行监控**
   - 手动或定时触发任务执行
   - 实时查看执行日志和状态
   - 监控任务执行进度和结果

### 🎬 演示视频

> 📹 **项目演示视频**：详细的操作演示和功能介绍
>
> [在线观看](https://www.bilibili.com/video/BV1TBCkBzEex/?spm_id_from=333.1387.upload.video_card.click) 

---
## PC端功能文档介绍
- [本地文档](/doc/cc-job)
  - [pc端](doc/cc-job/pc端功能介绍.md)
此文档是旧的pc端文档，新版本使用JavaFX写的，文档是适用的，新文档编写中.....
<div align="center">
  <img src="./doc/image/img-md/15.png" width="80%"/>
</div>

## 🚀 快速开始

### 📋 环境要求

在开始之前，请确保您的环境满足以下要求：

| 环境 | 版本要求 | 说明 |
|------|---------|------|
| **JDK** | 17+ | 推荐使用 JDK 17 或更高版本 |
| **Maven** | 3.6+ | 用于构建 Java 项目 |
| **MySQL** | 5.7+ | 推荐使用 MySQL 8.0+ |
| **Node.js** | 18+ | Web 前端需要（注意：20.6.0 版本不可用） |
| **pnpm** | 最新版 | 前端包管理工具 |
| **Python** | 3.6+ | DataX 需要（如果使用 DataX 功能） |

### 1️⃣ 数据库初始化

#### 1.1 创建数据库

```sql
CREATE DATABASE `cc_job_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 1.2 执行初始化脚本

执行项目根目录下的数据库脚本：

```bash
# 方式一：使用 MySQL 命令行
mysql -u root -p cc_job_admin < doc/cc_etl.sql

# 方式二：直接在 MySQL 客户端中执行
# 打开 doc/cc_etl.sql 文件，复制内容到 MySQL 客户端执行
```

### 2️⃣ 后端配置与启动

#### 2.1 编译项目

```bash
# 进入项目根目录
cd cc-job

# 编译项目
mvn clean package -DskipTests
```

#### 2.2 配置说明

#### 🛠️ 管理端配置（Admin）

编辑 `cc-job/cc-job-admin/src/main/resources/application.yml`：

```yaml
server:
  port: 8989

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/cc_job_admin?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: your_username  # 修改为您的数据库用户名
    password: your_password  # 修改为您的数据库密码

xxl:
  job:
    i18n: zh_CN
    accessToken: default_token
    triggerpool:
      fast:
        max: 200
      slow:
        max: 200
    logretentiondays: 30
    logpath: /path/to/logs  # 日志文件路径，必须配置，建议与 executor 保持一致
```

#### ⚙️ 执行器配置

编辑 `cc-job/cc-job-executor-samples/cc-job-executor-sample-springboot/src/main/resources/application.yml`：

```yaml
server:
  port: 8400

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/cc_job_admin?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password

xxl:
  job:
    admin:
      addresses: http://127.0.0.1:8989/xxl-job-admin
    accessToken: default_token
    executor:
      appname: xxl-job-executor-sample
      address:
      ip:
      port: 10000  # 执行器端口，不能为 9999
      logpath: /path/to/logs  # 日志路径，必须配置
      logretentiondays: 30
```


#### 2.3 启动后端服务

**启动 Admin 服务**：

```bash
# 方式一：使用 IDE 运行
# 运行 cc-job-admin 模块的启动类

# 方式二：使用命令行
cd cc-job/cc-job-admin
mvn spring-boot:run
```

**启动 Executor 服务**：

```bash
# 方式一：使用 IDE 运行
# 运行 cc-job-executor-sample-springboot 模块的启动类

# 方式二：使用命令行
cd cc-job/cc-job-executor-samples/cc-job-executor-sample-springboot
mvn spring-boot:run
```

**验证启动**：

- Admin 服务：访问 `http://localhost:8989/xxl-job-admin`
- Executor 会自动注册到 Admin，可在 Web 端查看

### 3️⃣ 前端配置与启动

#### 3.1 Web 端配置

```bash
# 进入 Web 前端目录
cd cc-job-web

# 安装 pnpm（如果未安装）
npm install pnpm -g

# 设置镜像源（可选，加速下载）
pnpm config set registry https://registry.npmmirror.com

# 安装依赖
pnpm install
```

#### 3.2 配置后端 API 地址

编辑 `cc-job-web/.env.development`：

```bash
# 后端 API 地址
VITE_APP_API_URL=http://localhost:8989
```

#### 3.3 启动 Web 端

```bash
# 开发环境运行
pnpm run dev

# 访问地址：http://localhost:5173
```

#### 3.4 生产环境构建

```bash
# 构建生产版本
pnpm run build

# 构建产物在 dist 目录
```

### 4️⃣ PC 端启动（可选）

PC 端为 JavaFX 应用，直接运行主类即可：

```bash
cd cc-job/cc-job-gui
mvn clean package
java -jar target/cc-job-gui.jar
```

### 5️⃣ DataX 配置（可选，如使用数据同步功能）

#### 5.1 下载 DataX

```bash
wget https://datax-opensource.oss-cn-hangzhou.aliyuncs.com/202308/datax.tar.gz
tar -zxvf datax.tar.gz
```

#### 5.2 测试 DataX

```bash
cd datax
python ./bin/datax.py ./job/job.json
```

#### 5.3 配置 Executor 中的 DataX 路径

在 Executor 配置文件中添加：

```yaml
cc-job:
  executor:
    jsonpath: /path/to/datax/json  # DataX JSON 文件临时存放路径
    pypath: /path/to/datax/bin/datax.py  # DataX Python 脚本路径
```

---

## 🧩 模块说明

| 🧩 模块 | 📋 说明 |
|------|------|
| 💻 **cc_job_pc** | 桌面端管理界面，便于本地运维和管理 |
| 🔄 **cc-job/cc-async-tool** | 异步任务调度工具，支持任务重复调用与超时处理(已经移除，会出现线程爆炸增长的情况，已经对编排工具进行重写并集成到cc-job-admin模块中) |
| 🗂️ **cc-job/cc-job-admin** | 任务注册中心，负责任务与任务组的注册管理 |
| 🧠 **cc-job/cc-job-core** | 任务执行核心模块，负责调度和执行任务 |
| 🏃 **cc-job/cc-job-executor** | 任务执行器，支持多种任务类型（DataX、API、JDBC等） |
| 🧪 **cc-job/cc-job-executor-samples** | 执行器示例工程，便于开发和测试 |
| 🗄️ **cc-job/cc-job-xo** | 数据存储与实体映射模块 |
| 🌐 **cc-job-web** | Web 端管理界面，提供可视化任务管理与监控 |

---

## 📚 项目文档

### 在线文档

- [在线文档](http://175.178.249.190/blog/post/298)

### 本地文档

> 📝 **提示**：此项目文档比较旧，但是够用，新文档编写中.....

- [本地文档](/doc/cc-job)
  - [01 项目介绍](doc/cc-job/01项目介绍.md)
    ![image-20241219212046125](./doc/cc-job/images/01-ccjob.png)
  - [02 快速开始](doc/cc-job/02快速开始.md)
    ![image-20241219212046125](./doc/cc-job/images/02-ccjob.png)
  - [03 功能介绍](doc/cc-job/03功能介绍.md)
    ![image-20241219212046125](./doc/cc-job/images/03-ccjob.png)
  - [PC端功能介绍](doc/cc-job/pc端功能介绍.md)

## 🛠️ 技术栈

### ⚙️ 后端技术栈

| 技术组件 | 版本 | 用途说明 |
|---------|------|---------|
| **Java** | 17+ | 运行环境，采用最新的 LTS 版本，提供优秀的性能和安全性 |
| **Spring Boot** | 3.0.7+ | 企业级应用框架，提供快速开发和自动配置能力 |
| **Spring MVC** | - | Web 框架，处理 HTTP 请求和响应 |
| **MyBatis Plus** | 3.5.3+ | 持久层框架，简化数据库操作，支持代码生成 |
| **XXL-Job** | 2.x | 分布式任务调度平台，提供核心调度能力 |
| **DataX** | 最新版 | 异构数据源离线同步工具，支持多种数据源 |
| **Druid** | 1.2.x | 数据库连接池，提供连接管理和监控 |
| **MySQL** | 5.7+ | 主数据库，存储任务配置和执行记录 |
| **Redis** | 可选 | 缓存层，提升系统性能和并发能力 |

### 🌐 前端技术栈（Web 端）

| 技术组件 | 版本 | 用途说明 |
|---------|------|---------|
| **Vue.js** | 3.x | 渐进式前端框架，提供响应式数据绑定 |
| **TypeScript** | 5.x | 类型安全的 JavaScript 超集，提升代码质量 |
| **Element Plus** | 2.x | Vue 3 企业级组件库，提供丰富的 UI 组件 |
| **Vue Flow** | 1.x | 流程图可视化组件，支持拖拽式任务编排 |
| **Vite** | 4.x | 新一代前端构建工具，提供快速的开发体验 |
| **Vue Router** | 4.x | 单页面应用路由管理 |
| **Pinia** | - | Vue 3 状态管理库，替代 Vuex |
| **Axios** | 1.x | HTTP 客户端，用于 API 调用 |

### 💻 前端技术栈（PC 端）

| 技术组件 | 版本 | 用途说明 |
|---------|------|---------|
| **JavaFX** | 17+ | Java 桌面应用框架，提供现代化 GUI |
| **Java** | 17+ | 运行环境，与后端保持一致的技术栈 |
| **Maven** | 3.6+ | 项目构建和依赖管理工具 |
| **Scene Builder** | 可选 | JavaFX UI 设计工具，可视化界面设计 |

### 🔧 开发工具链

| 工具类型 | 工具名称 | 用途说明 |
|---------|---------|---------|
| **构建工具** | Maven 3.6+ | 项目构建、依赖管理和打包 |
| **前端包管理** | pnpm | 高效的包管理工具，磁盘空间节省 |
| **版本控制** | Git | 代码版本控制和团队协作 |
| **IDE** | IntelliJ IDEA | Java 开发集成环境 |
| **代码检查** | Checkstyle | 代码规范检查工具 |

### 📊 系统要求

| 环境类型 | 最低配置 | 推荐配置 |
|---------|---------|---------|
| **操作系统** | Windows 10+ / macOS 10.15+ / Linux | Windows 11 / macOS 12+ / Ubuntu 20.04+ |
| **内存** | 4GB | 8GB+ |
| **磁盘空间** | 5GB | 20GB+ |
| **网络** | 100Mbps | 1Gbps |




---

## ❓ 常见问题

### 部署相关问题

#### Q1: Admin 启动失败，提示数据库连接错误？

**A**: 检查以下几点：
1. 数据库是否已启动
2. 数据库连接信息是否正确（用户名、密码、地址、端口）
3. 数据库用户是否有足够权限
4. 数据库是否已初始化（执行了初始化脚本）
5. 时区设置是否正确（建议使用 `Asia/Shanghai`）

#### Q2: Executor 无法注册到 Admin？

**A**: 检查以下几点：
1. Admin 服务是否已启动
2. Admin 地址配置是否正确（`admin.addresses`）
3. 网络是否连通（可以 ping 或 telnet 测试）
4. AccessToken 是否一致（admin 和 executor 必须相同）
5. Executor 端口是否被占用
6. 防火墙是否阻止连接

#### Q3: DataX 任务执行失败？

**A**: 检查以下几点：
1. DataX 是否已正确安装
2. `pypath` 配置是否正确（指向 DataX 的 Python 脚本路径）
3. `jsonpath` 目录是否存在且有写权限
4. 数据源连接信息是否正确
5. Reader 和 Writer 的表结构是否匹配
6. 检查执行器日志中的详细错误信息
7. Python 环境是否正确（需要 Python 3.6+）

#### Q4: Web 端无法访问？

**A**: 检查以下几点：
1. Web 前端是否已启动（`pnpm run dev`）
2. 前端端口是否被占用（默认 5173）
3. 后端 API 地址配置是否正确（`.env.development` 中的 `VITE_APP_API_URL`）
4. 浏览器控制台是否有错误信息
5. 检查跨域配置是否正确
6. 后端服务是否正常运行

#### Q5: 任务组执行失败，提示拓扑排序错误？

**A**: 检查以下几点：
1. 任务组中是否存在循环依赖
2. 任务节点是否都已正确关联任务
3. 任务依赖关系是否正确配置
4. 检查任务组 JSON 配置是否完整
5. 任务组是否选择了正确的执行器（必须选择"任务集执行器"）

### 配置相关问题

#### Q6: 如何配置多个执行器？

**A**: 
1. 复制 Executor 模块配置
2. 修改每个 Executor 的 `appname` 和 `port`（确保端口不冲突）
3. 确保所有 Executor 的 `admin.addresses` 指向同一个 Admin
4. 确保所有 Executor 的 `accessToken` 与 Admin 一致
5. 启动多个 Executor 实例

#### Q7: 如何配置集群部署？

**A**: 
1. Admin 可以部署多个实例，共享同一个数据库
2. Executor 可以部署多个实例，实现负载均衡
3. 使用 Nginx 做负载均衡（可选）
4. 确保所有实例的配置一致（特别是 `accessToken`）
5. 确保日志路径可访问（如果使用共享存储）

### 功能使用问题

#### Q8: 如何实现任务失败后发送告警？

**A**: 
1. 在任务配置中启用"失败告警"
2. 配置告警通知方式（邮件/短信/Webhook）
3. 设置告警规则和接收人
4. 任务失败时会自动触发告警

#### Q9: 如何导出/导入任务组配置？

**A**:
1. **导出**：在任务组管理页面点击"导出"，生成 JSON 文件
2. **导入**：在任务组管理页面点击"导入"，选择 JSON 文件
3. 导入后检查任务依赖关系是否正确
4. 确保导入的任务组中的任务都已存在

### 开发相关问题

#### Q10: 如何添加新的任务类型？

**A**:
1. 在 `cc-job-executor` 模块中实现任务处理器
2. 继承 `AbstractJobHandler` 类
3. 在 Spring 容器中注册处理器
4. 在前端界面中添加对应的配置选项

#### Q11: 如何自定义告警通知？

**A**:
1. 实现 `IAlarmService` 接口
2. 配置告警渠道（邮件、短信、Webhook 等）
3. 在任务配置中选择告警方式
4. 测试告警功能是否正常工作

#### Q12: 项目如何进行性能调优？

**A**:
1. **数据库优化**：添加合适的索引，避免慢查询
2. **连接池配置**：调整 Druid 连接池参数
3. **JVM 调优**：配置合适的堆内存和 GC 参数
4. **缓存策略**：使用 Redis 缓存热点数据
5. **异步处理**：对耗时操作使用异步处理

### 升级与维护

#### Q13: 如何从旧版本升级？

**A**:
1. 备份数据库和配置文件
2. 下载新版本代码
3. 更新数据库结构（检查升级脚本）
4. 更新配置文件
5. 逐步重启服务，验证功能正常

#### Q14: 如何监控系统运行状态？

**A**:
1. 使用内置的监控页面查看任务执行统计
2. 配置日志收集和分析
3. 设置关键指标监控（CPU、内存、磁盘等）
4. 配置告警规则，及时发现问题

### 故障排除

#### Q15: 任务执行卡住怎么办？

**A**:
1. 检查执行器是否正常运行
2. 查看任务执行日志，定位问题
3. 检查网络连接是否正常
4. 重启执行器服务
5. 如果问题持续，联系技术支持

#### Q16: 数据同步速度慢怎么优化？

**A**:
1. 调整 DataX 并发度参数
2. 优化数据源查询条件
3. 检查网络带宽是否充足
4. 考虑分批处理大表数据
5. 使用增量同步替代全量同步

---

## 🤝 贡献指南

我们非常欢迎社区贡献者参与项目开发！无论您是修复 bug、添加功能、改进文档，还是提供建议，都能帮助项目变得更好。

### 📋 贡献类型

- 🐛 **Bug 修复**：修复已知的问题和缺陷
- ✨ **新功能**：添加新的功能和特性
- 📚 **文档改进**：完善文档、添加示例和教程
- 🎨 **界面优化**：改进用户界面和用户体验
- 🧪 **测试完善**：添加或改进测试用例
- 🔧 **工具改进**：优化构建脚本、开发工具等

### 🚀 开发流程

#### 1. 环境准备

在开始贡献之前，请确保您的开发环境满足以下要求：

```bash
# 克隆项目
git clone https://github.com/xiaozhaoCcz/CC_ETL.git
cd CC_ETL

# 后端环境要求
- JDK 17+
- Maven 3.6+
- MySQL 5.7+

# 前端环境要求
- Node.js 18+
- pnpm 最新版
```

#### 2. 开发规范

##### 分支管理
```bash
# 创建特性分支
git checkout -b feature/your-feature-name
# 或修复分支
git checkout -b fix/issue-number-description
# 或文档分支
git checkout -b docs/update-documentation
```

##### 提交规范
```bash
# 提交格式
git commit -m "type(scope): description"

# 类型说明
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或工具配置更新
```

#### 3. 代码规范

##### Java 后端规范
- 遵循阿里巴巴 Java 开发规范
- 使用 Lombok 简化代码
- 包名统一使用小写，类名使用 Pascal 命名
- 方法注释使用 JavaDoc 格式

##### 前端规范
- 使用 TypeScript 进行类型检查
- 组件命名使用 Pascal 命名
- 文件命名使用 kebab-case
- 提交前运行 ESLint 检查

##### 数据库规范
- 表名使用下划线分隔的小写
- 字段名使用下划线分隔的小写
- 必须添加字段注释
- 索引命名规范：idx_表名_字段名

### 🧪 测试要求

- 新功能必须包含单元测试
- 修改现有功能需要更新相关测试
- 提交前确保所有测试通过

```bash
# 运行后端测试
cd cc-job
mvn test

# 运行前端测试
cd cc-job-web
pnpm test
```

### 📝 提交 Pull Request

1. **Fork 项目** 到您的 GitHub 账户
2. **创建分支** 并进行开发
3. **提交更改** 并推送至您的仓库
4. **创建 PR**：
   - 标题清晰描述变更内容
   - 详细描述变更原因和影响
   - 关联相关 Issue（如有）
   - 请求 Review

### 🐛 报告问题

提交 Issue 时请提供以下信息：

#### Bug 报告
- **标题**：清晰描述问题
- **环境信息**：
  - 操作系统版本
  - Java 版本：`java -version`
  - 项目版本或 commit hash
- **复现步骤**：
  - 详细的操作步骤
  - 预期行为 vs 实际行为
- **错误日志**：完整的错误堆栈信息
- **截图**：界面问题请提供截图

#### 功能请求
- **标题**：描述所需功能
- **背景**：为什么需要这个功能
- **实现建议**：可选的实现方案
- **影响范围**：影响哪些用户或场景

### 🎯 行为准则

- 尊重所有贡献者，保持友好的沟通环境
- 代码审查注重建设性反馈
- 优先考虑项目的整体利益
- 及时响应 Issue 和 PR

### 📞 联系我们

- **项目维护者**：Cc-ETL Team
- **讨论群**：暂无
- **邮箱**：项目邮箱（待添加）

感谢您的贡献！🎉

---

## 📞 技术支持

### 获取帮助

如果您在使用过程中遇到问题，可以通过以下方式获取帮助：

1. **查看文档**：首先查阅本文档的"常见问题"部分
2. **GitHub Issues**：在项目仓库提交 Issue，描述问题详情
3. **在线文档**：查看[在线文档](http://175.178.249.190/blog/post/298)

### 反馈建议

如果您有任何建议或想法，欢迎：
- 提交 Issue 讨论
- 提交 Pull Request
- 分享使用经验

---

## 🙏 致谢

感谢以下优秀的开源项目：

- **XXL-Job**：感谢 XXL-Job 提供的优秀任务调度框架
- **DataX**：感谢阿里巴巴 DataX 项目提供的数据同步工具
- **Vue 3**：感谢 Vue.js 团队提供的优秀前端框架
- **Element Plus**：感谢 Element Plus 提供的组件库
- **JavaFX**：感谢 JavaFX 提供的桌面应用框架

---

## 📄 License

本项目遵循 [MIT License](./LICENSE)。

---

### 💝 赞助支持

如果您觉得这个项目对您有价值，欢迎通过以下方式支持项目发展：

- ⭐ **GitHub Star**：给项目点个星，增加项目曝光度
- 🍴 **Fork & Contribute**：参与项目贡献，共同改进
- 📢 **分享传播**：将项目分享给更多需要的开发者
- 💬 **反馈建议**：提出宝贵意见，帮助项目改进

### 📞 联系我们

- **项目主页**：[GitHub](https://github.com/xiaozhaoCcz/CC_ETL) | [Gitee](https://gitee.com/xzjsccz/Cc_ETL)
- **在线文档**：[http://175.178.249.190/blog/post/298](http://175.178.249.190/blog/post/298)
- **Issue 反馈**：在 GitHub/Gitee 上提交 Issue
- **讨论交流**：暂无官方讨论群，欢迎在 Issue 中交流

---

## 📈 项目统计

[![GitHub stars](https://img.shields.io/github/stars/xiaozhaoCcz/CC_ETL.svg?style=social)](https://github.com/xiaozhaoCcz/CC_ETL)
[![GitHub forks](https://img.shields.io/github/forks/xiaozhaoCcz/CC_ETL.svg?style=social)](https://github.com/xiaozhaoCcz/CC_ETL)
[![GitHub issues](https://img.shields.io/github/issues/xiaozhaoCcz/CC_ETL.svg)](https://github.com/xiaozhaoCcz/CC_ETL/issues)
[![GitHub license](https://img.shields.io/github/license/xiaozhaoCcz/CC_ETL.svg)](https://github.com/xiaozhaoCcz/CC_ETL/blob/master/LICENSE)

---

**最后更新**：2025-01-07  
**维护者**：Cc-ETL Team  
**版本**：v2.0.0

---

<p align="center">Made with ❤️ by the Cc-ETL Team</p>

如需更多帮助或有任何建议，欢迎提交 Issue 或 PR！🚀
