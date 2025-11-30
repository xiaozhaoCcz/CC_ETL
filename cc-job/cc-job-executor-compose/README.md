# CC-Job 任务组编排执行器 (Executor-Compose)

## 📖 简介

`cc-job-executor-compose` 是一个独立的任务组编排执行器模块，专门负责任务组的依赖分析、拓扑排序和并行/串行执行。

### 为什么需要这个模块？

在原架构中，任务组编排逻辑（`JobGroupXxlJob`）放在 `cc-job-admin` 模块中，存在以下问题：

| 问题 | 影响 |
|------|------|
| **职责混乱** | Admin 既负责调度管理，又负责执行编排 |
| **耦合度高** | 编排逻辑与 Admin 服务紧密耦合 |
| **扩展性差** | 无法独立升级或替换编排引擎 |
| **部署不灵活** | Admin 和执行器无法完全分离部署 |

### 新架构优势

```
原架构：
┌─────────────────┐
│   cc-job-admin  │ ← JobGroupXxlJob（任务组编排）
│  （调度管理）    │ ← 调度逻辑
└────────┬────────┘
         │ 触发
         ▼
┌─────────────────┐
│  cc-job-executor│ ← 单个任务执行
└─────────────────┘

新架构：
┌─────────────────┐
│   cc-job-admin  │ ← 纯调度管理
└────────┬────────┘
         │ 触发
         ▼
┌─────────────────────────┐
│ cc-job-executor-compose │ ← 任务组编排
│  （编排执行器）          │
└────────┬────────────────┘
         │ 触发子任务
         ▼
┌─────────────────┐
│  cc-job-executor│ ← 单个任务执行
└─────────────────┘
```

**优势：**
- ✅ **职责清晰**：Admin 专注调度，Executor-Compose 专注编排
- ✅ **低耦合**：通过 API 通信，解耦模块依赖
- ✅ **易扩展**：编排逻辑可独立升级
- ✅ **灵活部署**：可独立部署和扩展

---

## 🏗️ 架构设计

### 核心组件

```
cc-job-executor-compose/
├── engine/                    # 编排引擎
│   ├── Async.java             # 拓扑排序执行器
│   ├── callback/              # 回调接口
│   ├── worker/                # Worker 组件
│   ├── wrapper/               # 任务包装器
│   └── constant/              # 常量定义
├── handler/                   # 执行器处理器
│   └── JobGroupExecutor.java # 主执行器
├── client/                    # API 客户端
│   └── AdminApiClient.java   # Admin API 客户端
├── config/                    # 配置类
│   └── XxlJobConfig.java     # XXL-Job 配置
└── CcJobExecutorComposeApplication.java  # 启动类
```

### 执行流程

```
1. Admin 触发任务组执行
   ↓
2. Executor-Compose 接收请求（@XxlJob）
   ↓
3. 通过 API 获取任务组信息（节点 + 边）
   ↓
4. 构建任务依赖图
   ↓
5. 拓扑排序，生成执行层级
   ↓
6. 并行/串行执行任务
   ↓
7. 上报执行状态到 Admin
   ↓
8. 完成执行，清理资源
```

---

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- 运行中的 `cc-job-admin` 服务

### 2. 配置文件

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8500

cc-job:
  # Admin 地址（必须配置）
  admin:
    address: http://localhost:8989/xxl-job-admin
  
  # 访问令牌（需要与 Admin 配置一致）
  access-token: default_token
  
  # 执行器配置
  executor:
    # 执行器名称（需要在 Admin 中注册）
    appname: cc-job-executor-compose
    # 执行器端口
    port: 10001
    # 日志路径
    logpath: /data/applogs/cc-job/executor-compose
    # 日志保留天数
    logretentiondays: 30
```

### 3. 编译打包

```bash
# 进入项目根目录
cd cc-job

# 编译模块
mvn clean package -pl cc-job-executor-compose -am -DskipTests

# 打包结果
ls cc-job-executor-compose/target/cc-job-executor-compose.jar
```

### 4. 启动服务

```bash
# 方式一：使用 Maven
mvn spring-boot:run -pl cc-job-executor-compose

# 方式二：使用 JAR 包
java -jar cc-job-executor-compose/target/cc-job-executor-compose.jar

# 方式三：指定配置文件
java -jar cc-job-executor-compose.jar --spring.profiles.active=prod
```

### 5. 验证启动

访问健康检查接口：

```bash
curl http://localhost:8500/actuator/health
```

预期响应：

```json
{
  "status": "UP"
}
```

---

## ⚙️ 配置说明

### 核心配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `cc-job.admin.address` | Admin 地址 | 必填 |
| `cc-job.access-token` | 访问令牌 | `default_token` |
| `cc-job.executor.appname` | 执行器名称 | `cc-job-executor-compose` |
| `cc-job.executor.port` | 执行器端口 | `10001` |
| `cc-job.executor.logpath` | 日志路径 | `/data/applogs/cc-job/executor-compose` |

### 环境配置

- **开发环境**：`application-dev.yml`
- **生产环境**：`application-prod.yml`

切换环境：

```bash
java -jar cc-job-executor-compose.jar --spring.profiles.active=dev
```

---

## 🔌 Admin API 接口要求

为了使 Executor-Compose 能够正常工作，Admin 需要提供以下 REST API 接口：

### 1. 获取任务信息

```http
GET /api/job/info/{jobId}
Authorization: {accessToken}

Response:
{
  "id": 1,
  "jobDesc": "任务组名称",
  "jobType": 2,
  "executorTimeout": 300,
  "executorFailRetryCount": 3,
  ...
}
```

### 2. 获取任务节点

```http
GET /api/job/nodes/{jobId}
Authorization: {accessToken}

Response: [
  {
    "id": 1,
    "jobId": 10,
    "jobParentId": 1,
    "nodeInDegree": 0,
    "nodeOutDegree": 2,
    ...
  },
  ...
]
```

### 3. 获取任务边

```http
GET /api/job/edges/{jobId}
Authorization: {accessToken}

Response: [
  {
    "id": 1,
    "fromNodeId": 1,
    "endNodeId": 2,
    "jobParentId": 1,
    ...
  },
  ...
]
```

### 4. 上报任务状态

```http
POST /api/job/status
Authorization: {accessToken}
Content-Type: application/json

Request:
{
  "jobId": 1,
  "randomId": "uuid-xxx",
  "status": 2,
  "message": "任务开始执行"
}

Response:
{
  "code": 200,
  "message": "success"
}
```

**状态码说明：**
- `0` - 失败
- `1` - 成功
- `2` - 执行中
- `5` - 完成

### 5. 获取快照（可选）

```http
GET /api/job/snapshot/{jobId}/{randomId}
Authorization: {accessToken}

Response:
{
  "nodesJson": "[...]",
  "edgesJson": "[...]"
}
```

---

## 📝 使用示例

### 1. 在 Admin 中注册执行器

登录 Admin 管理界面，进入"执行器管理"，添加新执行器：

- **AppName**: `cc-job-executor-compose`
- **名称**: `任务组编排执行器`
- **注册方式**: `自动注册`
- **机器地址**: 启动后自动注册

### 2. 创建任务组

在 Admin 中创建任务组，选择执行器为 `cc-job-executor-compose`，设置执行处理器为 `runJobGroupHandler`。

### 3. 添加任务节点

在任务组中添加多个任务节点，设置节点间的依赖关系。

### 4. 触发执行

点击"执行"按钮，Executor-Compose 会：
1. 接收执行请求
2. 获取任务组信息
3. 构建依赖图
4. 拓扑排序执行
5. 上报执行状态

---

## 🔧 开发指南

### 扩展任务执行逻辑

`JobGroupExecutor.executeTask()` 方法是任务执行的入口，可以根据任务类型实现不同的执行逻辑：

```java
private String executeTask(Long jobId, JobNode node, String randomId) {
    JobInfo jobInfo = adminApiClient.getJobInfo(jobId);
    
    switch (jobInfo.getGlueType()) {
        case "BEAN":
            return executeBeanTask(jobInfo, node, randomId);
        case "GLUE_SHELL":
            return executeShellTask(jobInfo, node, randomId);
        case "HTTP":
            return executeHttpTask(jobInfo, node, randomId);
        case "DATAX":
            return executeDataxTask(jobInfo, node, randomId);
        default:
            logger.warn("未知任务类型: {}", jobInfo.getGlueType());
            return JobConstant.FAIL_RETRY;
    }
}
```

### 自定义编排策略

可以继承 `Async` 类，实现自定义的编排策略：

```java
public class CustomAsync extends Async {
    
    @Override
    protected void doWorkWrappers(...) {
        // 自定义编排逻辑
    }
}
```

---

## ⚠️ 注意事项

### 1. 端口冲突

- 服务端口：`8500`（可修改）
- 执行器端口：`10001`（不能为 `9999`）

确保端口未被占用。

### 2. 日志路径

日志路径必须可写，建议使用绝对路径：

```yaml
cc-job:
  executor:
    logpath: /data/applogs/cc-job/executor-compose
```

### 3. 访问令牌

`access-token` 必须与 Admin 配置一致，否则无法通信。

### 4. Admin 地址

Admin 地址必须可访问，格式为：`http://host:port/xxl-job-admin`

---

## 🐛 故障排查

### 问题 1：无法注册到 Admin

**原因：**
- Admin 地址配置错误
- 访问令牌不一致
- 网络不通

**解决：**
1. 检查配置文件中的 `cc-job.admin.address`
2. 确认 `access-token` 与 Admin 一致
3. 使用 `curl` 测试 Admin 地址是否可访问

### 问题 2：获取任务信息失败

**原因：**
- Admin 未提供 API 接口
- 接口路径不匹配

**解决：**
1. 检查 Admin 是否实现了必需的 API 接口
2. 查看 Executor-Compose 日志，确认请求的 URL
3. 在 Admin 端添加接口实现

### 问题 3：任务执行失败

**原因：**
- 任务节点配置错误
- 依赖关系存在循环
- 超时时间设置过短

**解决：**
1. 检查任务组的节点和边配置
2. 使用拓扑排序验证是否存在循环依赖
3. 适当延长超时时间

---

## 📚 相关文档

- [项目架构分析](../doc/architecture-analysis.md)
- [迁移指南](../doc/migration-guide.md)
- [API 接口文档](../doc/api-specification.md)

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📄 License

本项目遵循 [MIT License](../LICENSE)。

---

**最后更新**：2025-11-30  
**维护者**：CC-ETL Team
