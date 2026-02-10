# 环境部署 (Environment Deployment)

本章说明 Cc-ETL 的运行环境要求、数据库初始化、各服务配置与启动方式，以及可选的 DataX 配置。

---

## 环境要求

| 环境 | 版本要求 | 说明 |
|------|----------|------|
| **JDK** | 17+ | 推荐 JDK 17 或更高，PC 端 JavaFX 需 17+ |
| **Maven** | 3.6+ | 用于构建 Java 项目 |
| **MySQL** | 5.7+ | 推荐 MySQL 8.0+，存储任务与执行记录 |
| **Python** | 3.6+ | 仅在使用 DataX 功能时需要 |

> 当前以 PC 端为主，无需 Node.js/pnpm；若后续恢复 Web 端再按需安装。

---

## 1. 数据库初始化

### 1.1 创建数据库

请与各模块 `application.yml` 中的 `spring.datasource.url` 使用同一库名（如 `cc_job_admin`）：

```sql
CREATE DATABASE `cc_job_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 1.2 执行初始化脚本

在项目根目录执行：

```bash
# 方式一：MySQL 命令行（请将库名改为你创建的库名）
mysql -u root -p cc_job_admin < doc/cc_etl.sql

# 方式二：在 MySQL 客户端中打开并执行 doc/cc_etl.sql
```

脚本位置：[doc/cc_etl.sql](../doc/cc_etl.sql)。

---

## 2. 后端配置与启动

### 2.1 编译项目

```bash
cd cc-job
mvn clean package -DskipTests
```

### 2.2 Admin 管理端配置

编辑 `cc-job/cc-job-admin/src/main/resources/application.yml`：

- **server.port**：HTTP 端口，默认 `8989`。
- **spring.datasource**：`url`、`username`、`password` 指向上述 MySQL 库。
- **xxl.job**：`accessToken` 与各 Executor 保持一致；`logpath` 必填，建议与 Executor 可访问路径一致；`logretentiondays` 日志保留天数。

示例（仅关键片段）：

```yaml
server:
  port: 8989

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/cc_job_admin?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password

xxl:
  job:
    i18n: zh_CN
    accessToken: default_token
    logpath: /path/to/logs
    logretentiondays: 30
```

### 2.3 Executor 执行器配置

编辑 `cc-job/cc-job-executor/cc-job-executor-samples/cc-job-executor-sample-springboot/src/main/resources/application.yml`（或你实际使用的 profile）：

- **xxl.job.admin.addresses**：Admin 地址，如 `http://127.0.0.1:8989/xxl-job-admin`。
- **xxl.job.accessToken**：与 Admin 一致。
- **xxl.job.executor**：`appname`（在 Admin 中注册的执行器名）、`port`（执行器端口，如 10000）、`logpath`（必填）。

示例：

```yaml
server:
  port: 8400

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cc_job_admin?...
    username: your_username
    password: your_password

xxl:
  job:
    admin:
      addresses: http://127.0.0.1:8989/xxl-job-admin
    accessToken: default_token
    executor:
      appname: xxl-job-executor-sample
      port: 10000
      logpath: /path/to/logs
      logretentiondays: 30
```

### 2.4 Executor-Compose 编排执行器配置

任务组编排需单独部署 **cc-job-executor-compose**。编辑其 `application.yml`（如 `cc-job/cc-job-executor-compose/src/main/resources/application.yml`）：

- **cc-job.job.admin.addresses**：Admin 地址，与上面一致。
- **cc-job.job.accessToken**：与 Admin 一致。
- **cc-job.job.executor**：`appname` 建议为 `cc-job-executor-compose`，`port` 为执行器端口（如 15000），`logpath` 必填。

示例：

```yaml
cc-job:
  job:
    admin:
      addresses: http://127.0.0.1:8989/xxl-job-admin
    executor:
      appname: cc-job-executor-compose
      port: 15000
      logpath: /path/to/logs
      logretentiondays: 30
    accessToken: default_token
```

更多说明见 [cc-job-executor-compose/README.md](../cc-job/cc-job-executor-compose/README.md)。

### 2.5 启动顺序与验证

1. 启动 **Admin**：运行 `cc-job-admin` 主类或 `mvn spring-boot:run -pl cc-job-admin`。  
   验证：访问 `http://localhost:8989/xxl-job-admin`（若提供 Web 页）或健康检查接口。

2. 启动 **Executor**：运行 executor-sample-springboot 主类。  
   验证：在 Admin 执行器管理中看到该执行器注册。

3. 启动 **Executor-Compose**（需任务组功能时）：运行 compose 模块主类。  
   验证：在 Admin 中看到 `cc-job-executor-compose` 执行器。

---

## 3. PC 端启动

PC 端为 JavaFX 应用，需配置 **JavaFX 模块路径** 后运行主类。

### 3.1 VM 参数示例（Mac，按本机 Maven 仓库路径修改）

```text
--module-path
/Users/你的用户名/.m2/repository/org/openjfx/javafx-controls/21.0.1/javafx-controls-21.0.1-mac-aarch64.jar:...（javafx-graphics、javafx-base、javafx-fxml 等同版本路径）
--add-modules
javafx.controls,javafx.fxml
```

### 3.2 运行方式

- **IDE**：在 `cc-job-gui` 模块中运行主类 `CcJobGuiApplication`，并加上上述 VM 参数。
- **命令行**：  
  `cd cc-job/cc-job-gui` → `mvn clean package` →  
  `java --module-path ... --add-modules javafx.controls,javafx.fxml -jar target/cc-job-gui-*.jar`

### 3.3 使用前配置

PC 端需能访问 Admin 的 API 地址（一般在设置或配置文件中配置 Base URL），并完成登录后使用任务编排、任务列表、数据源管理等功能。

---

## 4. DataX 配置（可选）

仅在使用 DataX 数据同步任务时需要。

### 4.1 安装 DataX

- 下载：如 `wget https://datax-opensource.oss-cn-hangzhou.aliyuncs.com/202308/datax.tar.gz`，解压。
- 测试：`python ./bin/datax.py ./job/job.json`。

### 4.2 Executor 中配置 DataX 路径

在 **cc-job-executor** 使用的配置文件中增加：

```yaml
cc-job:
  executor:
    jsonpath: /path/to/datax/json   # DataX JSON 临时文件目录
    pypath: /path/to/datax/bin/datax.py   # datax.py 脚本路径
```

确保执行器进程有权限读写 `jsonpath`，且环境可执行 `pypath` 对应的 Python 与 DataX。

---

## 5. 验证清单

| 步骤 | 验证项 |
|------|--------|
| 数据库 | 建库并执行 `doc/cc_etl.sql` 无报错 |
| Admin | 服务启动成功，可访问配置的端口 |
| Executor | 启动后在 Admin 执行器列表中可见 |
| Executor-Compose | 启动后在 Admin 中可见，任务组可选择该执行器 |
| PC 端 | 启动后能打开界面、配置 API 地址并登录 |
| DataX（可选） | Executor 中配置 jsonpath/pypath 后，DataX 任务可正常触发 |

---

[返回文档首页](README.md) | [上一章：快速了解](02-quick-start.md) | [下一章：项目介绍](04-project-intro.md)
