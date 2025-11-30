# Admin 模块控制任务组功能解决方案总结

## 📋 问题

**原问题**: 既然任务组编排执行器（executor-compose）已经独立出去了，Admin 模块如何实现暂停/恢复/停止任务组运行的功能？

**核心挑战**: 
- Admin 无法直接访问 Executor-Compose 的内存变量（如 `STOP_MAP`、`RUNNING_JOBS`）
- 需要跨模块通信机制

---

## 💡 解决方案

### 整体架构

```
┌──────────┐         API 调用          ┌──────────────────┐
│  Admin   │ ─────────────────────────▶│ Executor-Compose │
│  模块    │                           │     模块          │
└────┬─────┘                           └─────────┬────────┘
     │                                           │
     │ 更新数据库                                 │ 轮询检查
     ▼                                           ▼
┌──────────────┐                         ┌────────────┐
│   MySQL      │◀────────────────────────│ 内存状态    │
│  job_info    │     查询 is_pause       │ RUNNING_   │
│  is_pause    │                         │ JOBS       │
└──────────────┘                         └────────────┘
```

### 三种控制方式

| 操作 | 实现机制 | 响应速度 | 是否可恢复 |
|------|---------|---------|-----------|
| **暂停** | Admin 更新数据库 `is_pause=1`<br/>Executor 轮询检查，进入等待 | 慢（5秒轮询） | ✅ 可恢复 |
| **恢复** | Admin 更新数据库 `is_pause=0`<br/>Executor 检测到后继续执行 | 慢（5秒轮询） | - |
| **停止** | Admin 调用 Executor API<br/>Executor 调用 `Async.stopWork()` | 快（立即响应） | ❌ 不可恢复 |

---

## 🔧 实现细节

### 1. Executor-Compose 端

#### ① 暴露控制 API

**文件**: `cc-job-executor-compose/src/main/java/com/cc/job/executor/compose/controller/JobGroupControlController.java`

**接口**:
- `POST /api/jobgroup/stop` - 停止任务组
- `GET /api/jobgroup/status` - 查询任务组状态
- `GET /api/jobgroup/running` - 获取所有运行中的任务组

#### ② 任务组状态管理

**文件**: `cc-job-executor-compose/src/main/java/com/cc/job/executor/compose/handler/JobGroupExecutorComplete.java`

**方法**:
```java
// 停止任务组
public void stopJobGroup(Long jobId, String randomId)

// 查询是否运行中
public boolean isJobGroupRunning(Long jobId, String randomId)

// 获取所有运行中的任务组
public Map<String, Boolean> getAllRunningJobGroups()
```

#### ③ 暂停检查机制

在 `executeTaskComplete()` 中的 `handlePause()` 方法：

```java
private void handlePause(JobInfo jobInfo) {
    long startTime = System.currentTimeMillis();
    
    // 循环检查暂停状态
    while (true) {
        // 从 Admin 获取最新状态
        JobInfo latestJobInfo = adminApiClient.getJobInfo(jobInfo.getId());
        
        if (latestJobInfo.getIsPause() == 0) {
            break;  // 恢复执行
        }
        
        // 检查超时
        if (System.currentTimeMillis() - startTime > MAX_PAUSE_WAIT_TIME) {
            throw new RuntimeException("任务暂停超时");
        }
        
        Thread.sleep(5000);  // 等待5秒后再次检查
    }
}
```

---

### 2. Admin 端

#### ① 控制服务

**文件**: `cc-job-admin/src/main/java/com/cc/job/admin/task/service/JobGroupControlService.java`

**方法**:

```java
// 暂停任务
public boolean pauseJob(Long jobId) {
    // 更新数据库 is_pause = 1
    jobInfoService.update(
        new LambdaUpdateWrapper<JobInfo>()
            .eq(JobInfo::getId, jobId)
            .set(JobInfo::getIsPause, 1)
    );
}

// 恢复任务
public boolean resumeJob(Long jobId) {
    // 更新数据库 is_pause = 0
    jobInfoService.update(
        new LambdaUpdateWrapper<JobInfo>()
            .eq(JobInfo::getId, jobId)
            .set(JobInfo::getIsPause, 0)
    );
}

// 停止任务组
public boolean stopJobGroup(Long jobId, String randomId) {
    // 1. 更新数据库状态
    pauseJob(jobId);
    
    // 2. 调用 Executor-Compose API
    String url = executorComposeAddress + "/api/jobgroup/stop";
    HttpResponse response = HttpRequest.post(url)
        .form("jobId", jobId)
        .form("randomId", randomId)
        .execute();
    
    return response.isOk();
}
```

#### ② 控制器

**文件**: `cc-job-admin/src/main/java/com/cc/job/admin/task/controller/JobGroupControlController.java`

**接口**:
- `POST /admin/jobgroup/control/pause/{jobId}` - 暂停任务
- `POST /admin/jobgroup/control/resume/{jobId}` - 恢复任务
- `POST /admin/jobgroup/control/stop` - 停止任务组
- `GET /admin/jobgroup/control/status` - 查询状态

#### ③ 配置

**文件**: `cc-job-admin/src/main/resources/application.yml`

```yaml
cc-job:
  executor-compose:
    address: http://localhost:8500
```

---

## 📊 完整流程示例

### 暂停流程

```
1. 用户点击"暂停"按钮
   ↓
2. 前端调用: POST /admin/jobgroup/control/pause/1
   ↓
3. Admin 更新数据库: UPDATE job_info SET is_pause = 1 WHERE id = 1
   ↓
4. Executor-Compose 执行到 handlePause()
   ↓
5. 查询数据库: adminApiClient.getJobInfo(1)
   ↓
6. 检测到 is_pause = 1，进入等待循环
   ↓
7. 每5秒检查一次，直到恢复或超时
```

### 恢复流程

```
1. 用户点击"恢复"按钮
   ↓
2. 前端调用: POST /admin/jobgroup/control/resume/1
   ↓
3. Admin 更新数据库: UPDATE job_info SET is_pause = 0 WHERE id = 1
   ↓
4. Executor-Compose 下次检查时检测到 is_pause = 0
   ↓
5. 跳出等待循环，继续执行任务
```

### 停止流程

```
1. 用户点击"停止"按钮
   ↓
2. 前端调用: POST /admin/jobgroup/control/stop?jobId=1&randomId=xxx
   ↓
3. Admin 调用: POST http://localhost:8500/api/jobgroup/stop
   ↓
4. Executor-Compose 接收请求
   ↓
5. 调用: Async.stopWork(workerWrappers)
   ↓
6. 所有任务被设置为失败状态
   ↓
7. 清理: RUNNING_JOBS.remove(executeKey)
   ↓
8. 上报状态: adminApiClient.reportStatus(...)
```

---

## 📁 实现文件清单

### Executor-Compose 模块

| 文件 | 说明 | 行数 |
|------|------|------|
| `controller/JobGroupControlController.java` | 控制接口 | ~150 |
| `handler/JobGroupExecutorComplete.java` | 新增状态管理方法 | +30 |

### Admin 模块（需要实现）

| 文件 | 说明 | 行数 |
|------|------|------|
| `service/JobGroupControlService.java` | 控制服务 | ~200 |
| `controller/JobGroupControlController.java` | 控制器 | ~100 |
| `resources/application.yml` | 配置 | +5 |

### 文档

| 文件 | 说明 |
|------|------|
| `doc/ADMIN_JOBGROUP_CONTROL_IMPLEMENTATION.md` | 详细实现指南 |
| `doc/JOBGROUP_CONTROL_ARCHITECTURE.md` | 架构设计文档 |
| `doc/ADMIN_CONTROL_SOLUTION_SUMMARY.md` | 本文档（总结） |

---

## 🎯 核心要点

### ✅ 优势

1. **解耦合**: Admin 和 Executor-Compose 通过 API 和数据库通信
2. **灵活性**: 支持暂停/恢复/停止三种控制方式
3. **可扩展**: 易于添加新的控制功能
4. **高可用**: 即使 Admin 宕机，Executor-Compose 仍可继续执行

### ⚠️ 注意事项

1. **暂停延迟**: 轮询机制导致最多5秒延迟
2. **网络依赖**: 停止操作依赖网络通信
3. **超时设置**: 暂停最多等待5分钟（可配置）
4. **多实例**: 需要考虑负载均衡

---

## 🚀 部署步骤

### 1. 更新 Executor-Compose

```bash
cd cc-job/cc-job-executor-compose
mvn clean package -DskipTests
java -jar target/cc-job-executor-compose.jar
```

**验证**:
```bash
curl http://localhost:8500/api/jobgroup/health
```

### 2. 实现 Admin 端代码

按照 `ADMIN_JOBGROUP_CONTROL_IMPLEMENTATION.md` 实现：
- `JobGroupControlService.java`
- `JobGroupControlController.java`
- 配置 `application.yml`

### 3. 测试

#### 暂停测试
```bash
curl -X POST http://localhost:8480/admin/jobgroup/control/pause/1
```

#### 恢复测试
```bash
curl -X POST http://localhost:8480/admin/jobgroup/control/resume/1
```

#### 停止测试
```bash
curl -X POST "http://localhost:8480/admin/jobgroup/control/stop?jobId=1&randomId=xxx-xxx-xxx"
```

#### 查询测试
```bash
curl "http://localhost:8480/admin/jobgroup/control/status?jobId=1&randomId=xxx-xxx-xxx"
```

---

## 📚 相关文档

- **实现指南**: [ADMIN_JOBGROUP_CONTROL_IMPLEMENTATION.md](./ADMIN_JOBGROUP_CONTROL_IMPLEMENTATION.md)
- **架构设计**: [JOBGROUP_CONTROL_ARCHITECTURE.md](./JOBGROUP_CONTROL_ARCHITECTURE.md)
- **完整实现**: [COMPLETE_IMPLEMENTATION.md](../cc-job/cc-job-executor-compose/COMPLETE_IMPLEMENTATION.md)
- **迁移指南**: [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)

---

## 💬 常见问题

### Q1: 暂停后多久能生效？

**A**: 最多5秒（轮询间隔）。如果任务正在执行，会在下一个任务开始前暂停。

### Q2: 停止和暂停有什么区别？

**A**: 
- **暂停**: 任务进入等待状态，可以恢复
- **停止**: 任务被强制终止，无法恢复

### Q3: 如果网络不通，停止功能会失效吗？

**A**: 是的。停止功能依赖 HTTP 调用。建议：
- 配置健康检查
- 使用消息队列（MQ）作为备用方案

### Q4: 多实例部署如何处理？

**A**: 
- 方案1: 使用负载均衡器统一入口
- 方案2: 记录任务组在哪个实例运行，直接调用该实例

### Q5: 暂停会影响性能吗？

**A**: 轮询检查对性能影响很小（5秒一次），但会增加数据库查询。建议使用缓存优化。

---

**最后更新**: 2025-11-30  
**作者**: CC-ETL Team
