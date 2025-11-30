# 任务组控制功能快速参考

## 🚀 快速使用

### 前端调用示例

```javascript
// 1. 暂停任务
axios.post('/admin/jobgroup/control/pause/1')
  .then(res => console.log('暂停成功'))
  .catch(err => console.error('暂停失败', err));

// 2. 恢复任务
axios.post('/admin/jobgroup/control/resume/1')
  .then(res => console.log('恢复成功'))
  .catch(err => console.error('恢复失败', err));

// 3. 停止任务组
axios.post('/admin/jobgroup/control/stop', null, {
  params: { jobId: 1, randomId: 'xxx-xxx-xxx' }
})
  .then(res => console.log('停止成功'))
  .catch(err => console.error('停止失败', err));

// 4. 查询状态
axios.get('/admin/jobgroup/control/status', {
  params: { jobId: 1, randomId: 'xxx-xxx-xxx' }
})
  .then(res => console.log('运行中:', res.data.data))
  .catch(err => console.error('查询失败', err));
```

---

## 📋 API 接口速查

### Admin 端接口

| 接口 | 方法 | 参数 | 说明 |
|------|------|------|------|
| `/admin/jobgroup/control/pause/{jobId}` | POST | jobId | 暂停任务 |
| `/admin/jobgroup/control/resume/{jobId}` | POST | jobId | 恢复任务 |
| `/admin/jobgroup/control/stop` | POST | jobId, randomId | 停止任务组 |
| `/admin/jobgroup/control/status` | GET | jobId, randomId | 查询状态 |

### Executor-Compose 端接口

| 接口 | 方法 | 参数 | 说明 |
|------|------|------|------|
| `/api/jobgroup/stop` | POST | jobId, randomId | 停止任务组 |
| `/api/jobgroup/status` | GET | jobId, randomId | 查询状态 |
| `/api/jobgroup/running` | GET | - | 所有运行中的任务组 |
| `/api/jobgroup/health` | GET | - | 健康检查 |

---

## 🔧 后端代码示例

### Admin 端

#### 控制服务

```java
@Service
public class JobGroupControlService {
    
    @Value("${cc-job.executor-compose.address}")
    private String executorComposeAddress;
    
    // 暂停任务
    public boolean pauseJob(Long jobId) {
        return jobInfoService.update(
            new LambdaUpdateWrapper<JobInfo>()
                .eq(JobInfo::getId, jobId)
                .set(JobInfo::getIsPause, 1)
        );
    }
    
    // 恢复任务
    public boolean resumeJob(Long jobId) {
        return jobInfoService.update(
            new LambdaUpdateWrapper<JobInfo>()
                .eq(JobInfo::getId, jobId)
                .set(JobInfo::getIsPause, 0)
        );
    }
    
    // 停止任务组
    public boolean stopJobGroup(Long jobId, String randomId) {
        String url = executorComposeAddress + "/api/jobgroup/stop";
        HttpResponse response = HttpRequest.post(url)
            .form("jobId", jobId)
            .form("randomId", randomId)
            .execute();
        return response.isOk();
    }
}
```

#### 控制器

```java
@RestController
@RequestMapping("/admin/jobgroup/control")
public class JobGroupControlController {
    
    @PostMapping("/pause/{jobId}")
    public Result<Void> pauseJob(@PathVariable Long jobId) {
        return jobGroupControlService.pauseJob(jobId) 
            ? Result.success() 
            : Result.error("暂停失败");
    }
    
    @PostMapping("/resume/{jobId}")
    public Result<Void> resumeJob(@PathVariable Long jobId) {
        return jobGroupControlService.resumeJob(jobId) 
            ? Result.success() 
            : Result.error("恢复失败");
    }
    
    @PostMapping("/stop")
    public Result<Void> stopJobGroup(
        @RequestParam Long jobId, 
        @RequestParam String randomId) {
        return jobGroupControlService.stopJobGroup(jobId, randomId) 
            ? Result.success() 
            : Result.error("停止失败");
    }
}
```

---

### Executor-Compose 端

#### 控制器

```java
@RestController
@RequestMapping("/api/jobgroup")
public class JobGroupControlController {
    
    private final JobGroupExecutorComplete jobGroupExecutor;
    
    @PostMapping("/stop")
    public Map<String, Object> stopJobGroup(
        @RequestParam Long jobId, 
        @RequestParam String randomId) {
        
        jobGroupExecutor.stopJobGroup(jobId, randomId);
        
        return Map.of(
            "code", 200,
            "message", "任务组停止成功"
        );
    }
    
    @GetMapping("/status")
    public Map<String, Object> getJobGroupStatus(
        @RequestParam Long jobId, 
        @RequestParam String randomId) {
        
        boolean isRunning = jobGroupExecutor.isJobGroupRunning(jobId, randomId);
        
        return Map.of(
            "code", 200,
            "data", Map.of(
                "jobId", jobId,
                "randomId", randomId,
                "isRunning", isRunning
            )
        );
    }
}
```

#### 执行器

```java
public class JobGroupExecutorComplete {
    
    // 停止任务组
    public void stopJobGroup(Long jobId, String randomId) {
        String executeKey = buildExecuteKey(jobId, randomId);
        List<WorkerWrapper<Long, String>> wrappers = RUNNING_JOBS.get(executeKey);
        
        if (wrappers != null) {
            Async.stopWork((List<WorkerWrapper>) (List<?>) wrappers);
            RUNNING_JOBS.remove(executeKey);
            reportStatus(jobId, randomId, 0, "任务组已被停止");
        }
    }
    
    // 查询是否运行中
    public boolean isJobGroupRunning(Long jobId, String randomId) {
        String executeKey = buildExecuteKey(jobId, randomId);
        return RUNNING_JOBS.containsKey(executeKey);
    }
    
    // 暂停检查
    private void handlePause(JobInfo jobInfo) {
        long startTime = System.currentTimeMillis();
        
        while (true) {
            JobInfo latest = adminApiClient.getJobInfo(jobInfo.getId());
            
            if (latest.getIsPause() == 0) {
                break;  // 恢复执行
            }
            
            if (System.currentTimeMillis() - startTime > MAX_PAUSE_WAIT_TIME) {
                throw new RuntimeException("任务暂停超时");
            }
            
            TimeUnit.SECONDS.sleep(5);  // 5秒检查一次
        }
    }
}
```

---

## ⚙️ 配置速查

### Admin 配置

```yaml
# application.yml
cc-job:
  executor-compose:
    address: http://localhost:8500  # Executor-Compose 地址
    timeout: 10000                  # 超时时间（毫秒）
```

### Executor-Compose 配置

```yaml
# application.yml
server:
  port: 8500

cc-job:
  admin:
    address: http://localhost:8480  # Admin 地址
    access-token: your-token        # 访问令牌
  
  pause-check:
    interval: 5000                  # 暂停检查间隔（毫秒）
    max-wait: 300000                # 最大等待时间（5分钟）
```

---

## 📊 三种控制方式对比

| 操作 | 暂停 | 恢复 | 停止 |
|------|------|------|------|
| **接口** | `/pause/{jobId}` | `/resume/{jobId}` | `/stop?jobId=&randomId=` |
| **实现** | 更新数据库 `is_pause=1` | 更新数据库 `is_pause=0` | 调用 Executor API |
| **速度** | 慢（5秒轮询） | 慢（5秒轮询） | 快（立即响应） |
| **可恢复** | ✅ | ✅ | ❌ |
| **场景** | 临时暂停 | 恢复执行 | 彻底停止 |

---

## 🔍 故障排查

### 1. 暂停不生效

**检查**:
```sql
-- 查看数据库状态
SELECT id, job_name, is_pause FROM job_info WHERE id = 1;
```

**确认**:
- `is_pause` 是否为 1
- Executor-Compose 是否正常运行
- 日志中是否有检查暂停的记录

---

### 2. 停止失败

**检查**:
```bash
# 测试 Executor-Compose 是否可访问
curl http://localhost:8500/api/jobgroup/health

# 查看 Admin 配置
cat cc-job-admin/src/main/resources/application.yml | grep executor-compose

# 查看 Admin 日志
tail -f logs/admin.log | grep JobGroupControl
```

**确认**:
- Executor-Compose 是否启动
- 网络是否通畅
- `randomId` 是否正确

---

### 3. 状态查询不准确

**检查**:
```bash
# 直接调用 Executor-Compose API
curl "http://localhost:8500/api/jobgroup/status?jobId=1&randomId=xxx-xxx-xxx"

# 查看所有运行中的任务组
curl http://localhost:8500/api/jobgroup/running
```

**确认**:
- 任务组是否真的在运行
- `executeKey` 是否正确（`jobId + "_" + randomId`）

---

## 🧪 测试脚本

### 完整测试流程

```bash
#!/bin/bash

# 配置
ADMIN_URL="http://localhost:8480"
EXECUTOR_URL="http://localhost:8500"
JOB_ID=1
RANDOM_ID="test-random-id-123"

echo "=== 1. 检查健康状态 ==="
curl -X GET "$EXECUTOR_URL/api/jobgroup/health"
echo -e "\n"

echo "=== 2. 暂停任务 ==="
curl -X POST "$ADMIN_URL/admin/jobgroup/control/pause/$JOB_ID"
echo -e "\n"

sleep 2

echo "=== 3. 查询状态（暂停后） ==="
curl -X GET "$ADMIN_URL/admin/jobgroup/control/status?jobId=$JOB_ID&randomId=$RANDOM_ID"
echo -e "\n"

sleep 5

echo "=== 4. 恢复任务 ==="
curl -X POST "$ADMIN_URL/admin/jobgroup/control/resume/$JOB_ID"
echo -e "\n"

sleep 2

echo "=== 5. 停止任务组 ==="
curl -X POST "$ADMIN_URL/admin/jobgroup/control/stop?jobId=$JOB_ID&randomId=$RANDOM_ID"
echo -e "\n"

echo "=== 6. 查询状态（停止后） ==="
curl -X GET "$ADMIN_URL/admin/jobgroup/control/status?jobId=$JOB_ID&randomId=$RANDOM_ID"
echo -e "\n"

echo "=== 测试完成 ==="
```

---

## 📦 依赖清单

### Maven 依赖

**Admin 端**:
```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-http</artifactId>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-json</artifactId>
</dependency>
```

**Executor-Compose 端**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## 🎯 最佳实践

### 1. 优先使用暂停/恢复

- ✅ 不影响已完成的任务
- ✅ 可以继续执行
- ❌ 避免不必要的停止操作

---

### 2. 停止操作需谨慎

```java
// 前端添加二次确认
if (confirm('确定要停止任务组吗？此操作不可恢复！')) {
    stopJobGroup(jobId, randomId);
}
```

---

### 3. 添加操作日志

```java
public boolean pauseJob(Long jobId) {
    // 记录操作日志
    operationLogService.log(
        OperationType.PAUSE_JOB,
        jobId,
        getCurrentUser(),
        "暂停任务组: " + jobId
    );
    
    // 执行暂停
    return jobInfoService.update(...);
}
```

---

### 4. 定期清理内存

```java
@Scheduled(cron = "0 0 * * * ?")  // 每小时执行
public void cleanupOldJobGroups() {
    Map<String, Boolean> runningJobs = jobGroupExecutor.getAllRunningJobGroups();
    
    for (String executeKey : runningJobs.keySet()) {
        // 检查是否超过最大运行时间
        // 如果超过，则清理
    }
}
```

---

## 📚 相关文档

| 文档 | 说明 |
|------|------|
| [ADMIN_CONTROL_SOLUTION_SUMMARY.md](./ADMIN_CONTROL_SOLUTION_SUMMARY.md) | 解决方案总结 |
| [ADMIN_JOBGROUP_CONTROL_IMPLEMENTATION.md](./ADMIN_JOBGROUP_CONTROL_IMPLEMENTATION.md) | 详细实现指南 |
| [JOBGROUP_CONTROL_ARCHITECTURE.md](./JOBGROUP_CONTROL_ARCHITECTURE.md) | 架构设计文档 |

---

**最后更新**: 2025-11-30  
**版本**: v1.0
