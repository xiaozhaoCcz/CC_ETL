# 任务组编排执行器 - 完整实现说明

## 📖 概述

本文档说明了任务组编排执行器的完整实现，包括所有高级特性。

---

## ✨ 完整实现的功能

### 1. 任务触发服务 (`JobTriggerService`)

**文件**: `src/main/java/com/cc/job/executor/compose/service/JobTriggerService.java`

**功能**:
- ✅ **获取执行器组信息** - 从 Admin 获取执行器地址列表
- ✅ **路由策略支持** - FIRST、LAST、ROUND、RANDOM、SHARDING_BROADCAST
- ✅ **分片广播** - 向所有执行器发送分片任务
- ✅ **执行器客户端缓存** - 复用 `ExecutorBiz` 客户端
- ✅ **任务触发** - 调用执行器的 `run` 方法
- ✅ **失败处理** - 根据阻塞策略决定是否抛出异常

**核心方法**:

```java
// 触发任务执行
public boolean triggerJob(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId)

// 分片广播触发
private boolean triggerShardingBroadcast(...)

// 普通路由触发
private boolean triggerNormal(...)

// 创建触发参数
private TriggerParam createTriggerParam(...)

// 执行触发
private boolean doTrigger(...)

// 路由选择执行器地址
private String selectAddress(...)
```

**路由策略**:

| 策略 | 说明 | 实现 |
|------|------|------|
| `FIRST` | 第一个 | 使用列表第一个地址 |
| `LAST` | 最后一个 | 使用列表最后一个地址 |
| `ROUND` | 轮询 | 使用任务ID取模 |
| `RANDOM` | 随机 | 随机选择 |
| `SHARDING_BROADCAST` | 分片广播 | 向所有执行器发送 |

---

### 2. 任务执行监听器 (`JobExecutionMonitor`)

**文件**: `src/main/java/com/cc/job/executor/compose/service/JobExecutionMonitor.java`

**功能**:
- ✅ **监听任务执行状态** - 循环检查任务是否完成
- ✅ **处理任务完成** - 区分成功和失败
- ✅ **重试逻辑** - 根据重试次数决定是否重试
- ✅ **可中断** - 支持停止监听

**核心方法**:

```java
// 监听任务执行
@Override
public String call()

// 停止监听
public void stopMonitoring()

// 处理任务完成
private String handleJobCompletion(boolean success, int currentRetryCount)
```

**监听流程**:

```
1. 循环检查 JOB_RESULTS 映射
   ↓
2. 发现任务结果
   ↓
3. 处理任务完成
   ├─ 成功 → 返回 SUCCESS
   └─ 失败 → 检查重试次数
      ├─ 未达到最大重试 → 返回 FAIL_RETRY
      └─ 已达到最大重试 → 返回 FAIL_COMPLETE
```

---

### 3. 完整任务组执行器 (`JobGroupExecutorComplete`)

**文件**: `src/main/java/com/cc/job/executor/compose/handler/JobGroupExecutorComplete.java`

**功能**:
- ✅ **任务组编排** - 拓扑排序、并行/串行执行
- ✅ **暂停/恢复** - 支持任务暂停和恢复
- ✅ **任务触发** - 集成 `JobTriggerService`
- ✅ **状态监听** - 集成 `JobExecutionMonitor`
- ✅ **超时控制** - 支持任务和监听超时
- ✅ **重试机制** - 支持任务失败重试
- ✅ **状态上报** - 实时上报任务状态到 Admin
- ✅ **资源清理** - 执行完成后清理资源

**核心方法**:

```java
// 任务组执行入口
@XxlJob("runJobGroupHandler")
public void execute()

// 构建 WorkerWrapper 列表
private List<WorkerWrapper<Long, String>> buildWorkerWrappers(...)

// 执行任务（完整实现）
private String executeTaskComplete(XxlJobContext, JobNode, JobInfo, String, int)

// 处理任务暂停
private void handlePause(JobInfo jobInfo)

// 监听任务执行状态
private String monitorTaskExecution(JobNode, JobInfo, String, int)

// 上报任务状态
private void reportStatus(Long jobId, String randomId, Integer status, String message)

// 清理资源
private void cleanup(long jobId, String randomId)

// 停止任务组执行
public void stopJobGroup(Long jobId, String randomId)
```

**执行流程**:

```
1. 接收任务组执行请求
   ↓
2. 从 Admin 获取任务组信息（节点 + 边）
   ↓
3. 构建任务依赖图
   ↓
4. 创建 WorkerWrapper 列表
   ├─ 为每个节点创建 Worker
   ├─ 设置 IWorker（执行逻辑）
   └─ 设置 ICallback（回调逻辑）
   ↓
5. 调用 Async 引擎执行拓扑排序
   ↓
6. 每个任务执行时：
   ├─ 检查暂停状态
   ├─ 触发任务执行（JobTriggerService）
   ├─ 监听任务执行状态（JobExecutionMonitor）
   ├─ 处理超时和重试
   └─ 上报任务状态
   ↓
7. 清理资源
```

---

## 🔧 关键技术实现

### 1. 暂停/恢复机制

```java
private void handlePause(JobInfo jobInfo) {
    // 1. 重新获取最新的任务信息
    JobInfo latestJobInfo = adminApiClient.getJobInfo(jobInfo.getId());
    
    // 2. 检查暂停状态
    boolean isPause = latestJobInfo.getIsPause() != null && latestJobInfo.getIsPause() == 1;
    
    // 3. 循环等待恢复，最多等待超时时间或5分钟
    while (isPause) {
        if (elapsed >= timeout) {
            break; // 超时退出
        }
        
        TimeUnit.SECONDS.sleep(5); // 每5秒检查一次
        
        // 重新检查暂停状态
        JobInfo checkJobInfo = adminApiClient.getJobInfo(jobInfo.getId());
        if (checkJobInfo != null) {
            isPause = checkJobInfo.getIsPause() != null && checkJobInfo.getIsPause() == 1;
        }
    }
}
```

**优势**:
- ✅ 实时检查暂停状态
- ✅ 支持超时保护
- ✅ 可随时恢复执行

---

### 2. 任务触发机制

```java
public boolean triggerJob(XxlJobContext xxlJobContext, JobInfo jobInfo, String randomId) {
    // 1. 获取执行器组信息
    JobGroup group = adminApiClient.getJobGroup(jobInfo.getJobGroup());
    List<String> registryList = group.getRegistryList();
    
    // 2. 根据路由策略触发任务
    if ("SHARDING_BROADCAST".equals(routeStrategy)) {
        // 分片广播：向所有执行器发送任务
        return triggerShardingBroadcast(...);
    } else {
        // 普通路由：选择一个执行器
        return triggerNormal(...);
    }
}
```

**支持的任务类型**:
- ✅ Bean 任务（通过 Handler）
- ✅ Shell 脚本（通过 GLUE_SHELL）
- ✅ Python 脚本（通过 GLUE_PYTHON）
- ✅ HTTP 任务（通过 HTTP）
- ✅ DataX 任务（通过 DATAX Handler）

---

### 3. 状态监听机制

```java
private String monitorTaskExecution(JobNode node, JobInfo jobInfo, String randomId, int retryCount) {
    // 1. 创建监听器
    JobExecutionMonitor monitor = new JobExecutionMonitor(jobInfo, node, randomId, JOB_RESULTS, retryCount);
    
    // 2. 在新线程中启动监听
    FutureTask<String> futureTask = new FutureTask<>(monitor);
    Thread monitorThread = new Thread(futureTask);
    monitorThread.start();
    
    // 3. 等待任务完成，支持超时
    if (jobInfo.getExecutorTimeout() > 0) {
        result = futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.SECONDS);
    } else {
        result = futureTask.get();
    }
    
    return result;
}
```

**优势**:
- ✅ 独立线程监听，不阻塞主流程
- ✅ 支持超时控制
- ✅ 可中断监听

---

### 4. 重试机制

```java
private String handleJobCompletion(boolean success, int currentRetryCount) {
    if (success) {
        return JobConstant.SUCCESS;
    } else {
        if (currentRetryCount < jobInfo.getExecutorFailRetryCount()) {
            // 未达到最大重试次数，返回 FAIL_RETRY
            return JobConstant.FAIL_RETRY;
        } else {
            // 已达到最大重试次数，返回 FAIL_COMPLETE
            return JobConstant.FAIL_COMPLETE;
        }
    }
}
```

**重试策略**:
- Async 引擎接收到 `FAIL_RETRY` 后会自动重试
- 每次重试会增加 `retryCount` 计数
- 达到最大重试次数后返回 `FAIL_COMPLETE`

---

## 📊 状态码说明

| 状态码 | 说明 | 场景 |
|--------|------|------|
| `2` | 执行中 | 任务开始执行时 |
| `1` | 成功 | 任务执行成功时 |
| `0` | 失败 | 任务执行失败时 |
| `5` | 完成 | 任务组执行完成时 |
| `9` | 预测 | 任务运行时间预测 |

---

## 🔌 Admin API 接口要求

为了支持完整功能，Admin 需要提供以下额外接口：

### 1. 获取执行器组信息

```http
GET /api/job/group/{jobGroupId}
Authorization: {accessToken}

Response:
{
  "id": 1,
  "appname": "xxl-job-executor",
  "title": "示例执行器",
  "addressType": 0,
  "addressList": null,
  "registryList": ["http://192.168.1.100:9999", "http://192.168.1.101:9999"],
  ...
}
```

### 2. 更新任务信息（暂停/恢复）

```http
PUT /api/job/info/{jobId}
Authorization: {accessToken}
Content-Type: application/json

Request:
{
  "isPause": 1  // 0=正常，1=暂停
}

Response:
{
  "code": 200,
  "message": "success"
}
```

---

## 🚀 使用示例

### 1. 普通任务执行

```java
// 在 Admin 中创建任务组，设置执行器为 cc-job-executor-compose
// 设置 JobHandler 为：runJobGroupHandler
// 触发执行
```

### 2. 分片广播任务

```java
// 在任务配置中设置：
// 路由策略 = SHARDING_BROADCAST
// 执行器会自动向所有执行器发送分片任务
```

### 3. 暂停和恢复

```java
// 暂停任务
PUT /api/job/info/1
{
  "isPause": 1
}

// 恢复任务
PUT /api/job/info/1
{
  "isPause": 0
}
```

### 4. 任务重试

```java
// 在任务配置中设置：
// 失败重试次数 = 3
// 任务失败后会自动重试3次
```

---

## ⚠️ 注意事项

### 1. 执行器客户端缓存

`JobTriggerService` 会缓存 `ExecutorBiz` 客户端，避免重复创建。如果执行器地址变更，需要重启 Executor-Compose。

### 2. 任务结果映射

`JOB_RESULTS` 映射用于传递任务执行结果，执行完成后会自动清理。如果任务异常退出，可能会残留数据，定期清理即可。

### 3. 超时控制

- 任务执行超时：由 `jobInfo.getExecutorTimeout()` 控制
- 监听超时：同样由 `jobInfo.getExecutorTimeout()` 控制
- 暂停等待超时：最多等待超时时间或5分钟

### 4. 线程安全

所有共享数据结构（`RUNNING_JOBS`、`JOB_RESULTS`）都使用了 `ConcurrentHashMap`，保证线程安全。

---

## 📝 对比简化版

### 简化版 (`JobGroupExecutor`)

- ✅ 基本的任务编排
- ✅ 拓扑排序执行
- ❌ 任务触发（模拟实现）
- ❌ 状态监听（无监听）
- ❌ 暂停/恢复
- ❌ 超时控制
- ❌ 重试机制

### 完整版 (`JobGroupExecutorComplete`)

- ✅ 完整的任务编排
- ✅ 拓扑排序执行
- ✅ **真实的任务触发**（集成 XXL-Job 触发器）
- ✅ **状态监听**（实时监听任务状态）
- ✅ **暂停/恢复**（支持暂停和恢复）
- ✅ **超时控制**（任务和监听超时）
- ✅ **重试机制**（支持失败重试）
- ✅ **状态上报**（实时上报到 Admin）
- ✅ **资源清理**（自动清理资源）

---

## 🎯 推荐使用

在生产环境中，强烈推荐使用 **`JobGroupExecutorComplete`**，因为它提供了完整的功能和健壮性。

**配置方式**:

```yaml
cc-job:
  executor:
    # 使用完整版的 JobHandler
    job-handler: runJobGroupHandler
```

在 Admin 中创建任务组时，设置 JobHandler 为 `runJobGroupHandler` 即可使用完整版。

---

## 📚 相关文档

- [README](./README.md) - 模块使用说明
- [MIGRATION_GUIDE](../doc/MIGRATION_GUIDE.md) - 迁移指南
- [REFACTORING_SUMMARY](../doc/REFACTORING_SUMMARY.md) - 重构总结

---

**最后更新**: 2025-11-30  
**作者**: CC-ETL Team
