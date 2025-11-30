# 任务组控制架构设计

## 📖 总体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Web 前端界面                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ 暂停按钮  │  │ 恢复按钮  │  │ 停止按钮  │  │ 查询按钮  │            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
└───────┼─────────────┼─────────────┼─────────────┼──────────────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Admin 模块（端口 8480）                           │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │          JobGroupControlController                             │ │
│  │  /admin/jobgroup/control/pause/{jobId}                         │ │
│  │  /admin/jobgroup/control/resume/{jobId}                        │ │
│  │  /admin/jobgroup/control/stop?jobId=&randomId=                 │ │
│  │  /admin/jobgroup/control/status?jobId=&randomId=               │ │
│  └─────────────────────┬──────────────────────────────────────────┘ │
│                        │                                             │
│                        ▼                                             │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │          JobGroupControlService                                │ │
│  │  • pauseJob(jobId)                                             │ │
│  │  • resumeJob(jobId)                                            │ │
│  │  • stopJobGroup(jobId, randomId)                               │ │
│  │  • isJobGroupRunning(jobId, randomId)                          │ │
│  └───────────┬────────────────────────────────────────────────────┘ │
│              │                                                        │
└──────────────┼────────────────────────────────────────────────────────┘
               │
               │  1. 更新数据库状态           2. HTTP API 调用
               ▼                             ▼
       ┌────────────────┐           ┌────────────────────────┐
       │  MySQL 数据库   │           │  HTTP 请求              │
       │                │           │  POST /api/jobgroup/   │
       │  job_info      │           │       stop             │
       │  - is_pause    │           │  GET  /api/jobgroup/   │
       │                │           │       status           │
       └────────────────┘           └──────────┬─────────────┘
               ▲                              │
               │                              │
               │                              │
               │ 3. 轮询检查暂停状态              │
               │                              │
┌──────────────┴──────────────────────────────┼──────────────────────┐
│            Executor-Compose 模块（端口 8500）│                       │
│                                            ▼                       │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │       JobGroupControlController                            │   │
│  │  /api/jobgroup/stop                                        │   │
│  │  /api/jobgroup/status                                      │   │
│  │  /api/jobgroup/running                                     │   │
│  └─────────────────────┬──────────────────────────────────────┘   │
│                        │                                           │
│                        ▼                                           │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │       JobGroupExecutorComplete                             │   │
│  │  • stopJobGroup(jobId, randomId)                           │   │
│  │  • isJobGroupRunning(jobId, randomId)                      │   │
│  │  • getAllRunningJobGroups()                                │   │
│  │                                                            │   │
│  │  内存状态管理:                                               │   │
│  │  - RUNNING_JOBS: Map<String, List<WorkerWrapper>>         │   │
│  │  - STOP_MAP: Map<String, Boolean>                         │   │
│  └────────────────────┬───────────────────────────────────────┘   │
│                       │                                            │
│                       ▼                                            │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │  任务执行流程                                                 │  │
│  │  1. handlePause() - 检查暂停状态 ───┐                         │  │
│  │  2. triggerJob() - 触发任务          │                        │  │
│  │  3. monitorTaskExecution() - 监控   │                        │  │
│  │                                     │                        │  │
│  │     每5秒查询数据库 is_pause ◄───────┘                         │  │
│  │     • is_pause = 1: 继续等待                                  │  │
│  │     • is_pause = 0: 恢复执行                                  │  │
│  │     • 超时: 抛出异常                                          │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 暂停/恢复流程

### 暂停流程

```
用户操作
   │
   │ 1. 点击"暂停"按钮
   ▼
Admin 控制器
   │
   │ POST /admin/jobgroup/control/pause/{jobId}
   ▼
Admin 服务
   │
   │ jobGroupControlService.pauseJob(jobId)
   ▼
更新数据库
   │
   │ UPDATE job_info SET is_pause = 1 WHERE id = {jobId}
   ▼
   ✓ 暂停标记已设置
   
   
Executor-Compose 执行流程
   │
   │ executeTaskComplete()
   ▼
检查暂停状态
   │
   │ handlePause()
   │ ├─ adminApiClient.getJobInfo(jobId)
   │ ├─ 检查 jobInfo.getIsPause()
   │ └─ is_pause = 1?
   │
   ├──[是]──▶ 进入等待循环
   │          │
   │          │ 每5秒检查一次
   │          │ ├─ adminApiClient.getJobInfo(jobId)
   │          │ ├─ is_pause 还是 1? ──[是]──▶ 继续等待
   │          │ │                           │
   │          │ └─ is_pause 变成 0? ──[是]──▶ 跳出循环，继续执行
   │          │
   │          └─ 等待时间 > 超时? ──[是]──▶ 抛出超时异常
   │
   └──[否]──▶ 继续执行任务
```

### 恢复流程

```
用户操作
   │
   │ 2. 点击"恢复"按钮
   ▼
Admin 控制器
   │
   │ POST /admin/jobgroup/control/resume/{jobId}
   ▼
Admin 服务
   │
   │ jobGroupControlService.resumeJob(jobId)
   ▼
更新数据库
   │
   │ UPDATE job_info SET is_pause = 0 WHERE id = {jobId}
   ▼
   ✓ 恢复标记已设置


Executor-Compose 等待循环
   │
   │ handlePause() 等待中...
   ▼
再次检查状态
   │
   │ 每5秒查询一次数据库
   │ adminApiClient.getJobInfo(jobId)
   ▼
检测到 is_pause = 0
   │
   │ 跳出等待循环
   ▼
继续执行任务
   │
   │ jobTriggerService.triggerJob(...)
   │ monitorTaskExecution(...)
   ▼
   ✓ 任务恢复执行
```

---

## 🛑 停止流程

```
用户操作
   │
   │ 3. 点击"停止"按钮
   ▼
Admin 控制器
   │
   │ POST /admin/jobgroup/control/stop?jobId={jobId}&randomId={randomId}
   ▼
Admin 服务
   │
   │ jobGroupControlService.stopJobGroup(jobId, randomId)
   │
   ├─ 1. 更新数据库状态（可选）
   │    UPDATE job_info SET is_pause = 1 WHERE id = {jobId}
   │
   └─ 2. 调用 Executor-Compose API
      │
      │ HTTP POST
      │ http://localhost:8500/api/jobgroup/stop
      │ params: jobId={jobId}&randomId={randomId}
      ▼
Executor-Compose 控制器
   │
   │ JobGroupControlController.stopJobGroup()
   ▼
Executor-Compose 服务
   │
   │ jobGroupExecutor.stopJobGroup(jobId, randomId)
   │
   ├─ 1. 根据 jobId + randomId 查找任务组
   │    String executeKey = jobId + "_" + randomId
   │    List<WorkerWrapper> wrappers = RUNNING_JOBS.get(executeKey)
   │
   ├─ 2. 调用 Async.stopWork(wrappers)
   │    ├─ 设置所有任务的 needStop = true
   │    ├─ 中断正在等待的任务
   │    └─ 将任务结果设置为失败
   │
   ├─ 3. 清理运行中的任务
   │    RUNNING_JOBS.remove(executeKey)
   │
   └─ 4. 上报停止状态
      adminApiClient.reportStatus(jobId, randomId, 0, "任务组已被停止")
      ▼
   ✓ 任务组已停止


正在执行的任务
   │
   │ executeTaskComplete()
   │ ├─ handlePause() ◄── 检测到 needStop = true
   │ ├─ triggerJob()  ◄── 检测到 needStop = true
   │ └─ monitor()     ◄── 检测到 needStop = true
   │
   └─▶ 所有任务抛出 InterruptedException 或直接返回失败
      ▼
   ✓ 任务执行终止
```

---

## 🔍 查询状态流程

```
用户操作
   │
   │ 4. 点击"查询状态"按钮
   ▼
Admin 控制器
   │
   │ GET /admin/jobgroup/control/status?jobId={jobId}&randomId={randomId}
   ▼
Admin 服务
   │
   │ jobGroupControlService.isJobGroupRunning(jobId, randomId)
   │
   └─ HTTP GET
      │ http://localhost:8500/api/jobgroup/status
      │ params: jobId={jobId}&randomId={randomId}
      ▼
Executor-Compose 控制器
   │
   │ JobGroupControlController.getJobGroupStatus()
   ▼
Executor-Compose 服务
   │
   │ jobGroupExecutor.isJobGroupRunning(jobId, randomId)
   │
   └─ 查询内存状态
      │ String executeKey = jobId + "_" + randomId
      │ boolean isRunning = RUNNING_JOBS.containsKey(executeKey)
      ▼
返回结果
   │
   │ {
   │   "code": 200,
   │   "data": {
   │     "jobId": 1,
   │     "randomId": "xxx-xxx-xxx",
   │     "isRunning": true
   │   }
   │ }
   ▼
   ✓ 状态查询完成
```

---

## 📊 核心数据结构

### Admin 模块

#### JobInfo 表（MySQL）

```sql
CREATE TABLE job_info (
    id BIGINT PRIMARY KEY,
    job_name VARCHAR(255),
    is_pause TINYINT DEFAULT 0,  -- 0=运行中, 1=暂停
    -- 其他字段...
);
```

### Executor-Compose 模块

#### 内存状态管理

```java
// 1. 运行中的任务组
private static final Map<String, List<WorkerWrapper<Long, String>>> RUNNING_JOBS 
    = new ConcurrentHashMap<>();

// Key: jobId + "_" + randomId
// Value: 该任务组的所有 WorkerWrapper

// 示例:
// "1_abc-def-123" -> [WorkerWrapper1, WorkerWrapper2, WorkerWrapper3]


// 2. 停止标记（由 Async 内部管理）
// WorkerWrapper.needStop = true  // 标记任务需要停止
```

---

## ⚙️ 配置说明

### Admin 配置

**application.yml**

```yaml
cc-job:
  executor-compose:
    # Executor-Compose 的地址
    address: http://localhost:8500
    # 连接超时时间（毫秒）
    timeout: 10000
    # 启用健康检查
    health-check:
      enabled: true
      interval: 30000  # 30秒检查一次
```

### Executor-Compose 配置

**application.yml**

```yaml
server:
  port: 8500

cc-job:
  admin:
    # Admin 的地址（用于调用 API 获取任务信息）
    address: http://localhost:8480
    access-token: your-access-token
  
  # 暂停检查配置
  pause-check:
    interval: 5000      # 检查间隔（毫秒）
    max-wait: 300000    # 最大等待时间（5分钟）
```

---

## 🎯 使用场景

### 场景 1：长时间运行的任务组，需要临时暂停

**背景**: 任务组正在执行，预计还需要 30 分钟完成，但现在需要临时维护数据库。

**操作流程**:
1. 用户点击"暂停"按钮
2. Admin 更新数据库 `is_pause = 1`
3. Executor-Compose 检测到暂停，进入等待状态
4. 管理员进行数据库维护
5. 维护完成后，用户点击"恢复"按钮
6. Admin 更新数据库 `is_pause = 0`
7. Executor-Compose 检测到恢复，继续执行

**优点**: 不需要重新执行已完成的任务，节省时间。

---

### 场景 2：任务组执行异常，需要立即停止

**背景**: 任务组执行过程中发现配置错误，需要立即停止，避免产生脏数据。

**操作流程**:
1. 用户点击"停止"按钮
2. Admin 调用 Executor-Compose API
3. Executor-Compose 立即停止所有任务
4. 清理相关资源
5. 用户修正配置
6. 重新触发任务组

**优点**: 立即停止，避免错误扩散。

---

### 场景 3：定期检查任务组状态

**背景**: 监控系统需要定期检查任务组是否正在运行，以便进行告警。

**操作流程**:
1. 监控系统定时调用状态查询 API
2. 获取任务组运行状态
3. 如果运行时间过长，发送告警通知

```java
@Scheduled(cron = "0 */5 * * * ?")  // 每5分钟检查一次
public void checkJobGroupStatus() {
    List<JobExecutionLog> runningLogs = getRunningLogs();
    
    for (JobExecutionLog log : runningLogs) {
        boolean isRunning = jobGroupControlService.isJobGroupRunning(
            log.getJobId(), 
            log.getRandomId()
        );
        
        if (isRunning) {
            long runningTime = System.currentTimeMillis() - log.getStartTime();
            if (runningTime > 3600000) {  // 超过1小时
                sendAlert("任务组运行时间过长: " + log.getJobId());
            }
        }
    }
}
```

---

## 🔐 安全性考虑

### 1. 权限控制

确保只有授权用户才能暂停/停止任务组：

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/stop")
public Result<Void> stopJobGroup(...) {
    // ...
}
```

### 2. 操作日志

记录所有控制操作：

```java
public boolean pauseJob(Long jobId) {
    // 记录操作日志
    operationLogService.log(
        "PAUSE_JOB",
        jobId,
        SecurityUtils.getCurrentUser()
    );
    
    // 执行暂停操作
    // ...
}
```

### 3. 防止误操作

停止操作需要二次确认：

```javascript
// 前端代码
function stopJobGroup(jobId, randomId) {
    if (confirm('确定要停止任务组吗？此操作不可恢复！')) {
        axios.post('/admin/jobgroup/control/stop', {
            jobId: jobId,
            randomId: randomId
        });
    }
}
```

---

## 🚀 性能优化

### 1. 缓存任务组状态

避免频繁查询数据库：

```java
@Cacheable(value = "jobGroupStatus", key = "#jobId")
public JobInfo getJobInfo(Long jobId) {
    return jobInfoMapper.selectById(jobId);
}

@CacheEvict(value = "jobGroupStatus", key = "#jobId")
public boolean pauseJob(Long jobId) {
    // 更新数据库
    // ...
}
```

### 2. 批量查询状态

一次查询多个任务组的状态：

```java
@GetMapping("/status/batch")
public Result<Map<String, Boolean>> getStatusBatch(@RequestBody List<StatusRequest> requests) {
    Map<String, Boolean> result = new HashMap<>();
    
    for (StatusRequest request : requests) {
        boolean isRunning = jobGroupControlService.isJobGroupRunning(
            request.getJobId(), 
            request.getRandomId()
        );
        result.put(request.getJobId() + "_" + request.getRandomId(), isRunning);
    }
    
    return Result.success(result);
}
```

### 3. 异步通知

使用消息队列实现异步通知：

```java
public boolean stopJobGroup(Long jobId, String randomId) {
    // 发送 MQ 消息
    rabbitTemplate.convertAndSend(
        "jobgroup.control",
        new StopJobGroupMessage(jobId, randomId)
    );
    
    return true;
}

// Executor-Compose 监听消息
@RabbitListener(queues = "jobgroup.control")
public void handleStopMessage(StopJobGroupMessage message) {
    jobGroupExecutor.stopJobGroup(message.getJobId(), message.getRandomId());
}
```

---

## 📝 总结

### 核心要点

1. **暂停/恢复**: 通过数据库状态 + 轮询检查实现
2. **停止**: 通过 API 调用 + 内存状态管理实现
3. **查询**: 通过 API 调用 + 内存状态查询实现

### 优势

- ✅ **解耦合**: Admin 和 Executor-Compose 通过 API 通信
- ✅ **灵活性**: 支持暂停/恢复/停止多种控制方式
- ✅ **可扩展**: 易于添加新的控制功能
- ✅ **高可用**: 即使 Admin 宕机，Executor-Compose 仍可继续执行

### 注意事项

- ⚠️ **网络延迟**: API 调用存在延迟，停止操作不是绝对实时
- ⚠️ **暂停超时**: 避免无限等待，设置合理的超时时间
- ⚠️ **多实例部署**: 需要考虑负载均衡和服务发现

---

**最后更新**: 2025-11-30  
**作者**: CC-ETL Team
