# Admin 任务组控制功能实现指南

## 📖 概述

由于任务组编排执行器（executor-compose）已经独立出去，Admin 无法直接访问其内存变量。本文档说明如何在 Admin 中实现暂停/恢复/停止任务组的功能。

---

## 🏗️ 架构设计

### 控制流程

```
┌─────────────────┐
│   用户操作      │
│  (Web界面)      │
└────────┬────────┘
         │ 1. 暂停/恢复/停止
         ▼
┌─────────────────────────┐
│  Admin 控制器            │
│  JobGroupControlService │
└────────┬────────────────┘
         │ 2. 更新数据库状态
         ▼
┌─────────────────────────┐
│  数据库                  │
│  job_info.is_pause      │
└─────────────────────────┘
         │ 3. 调用 API
         ▼
┌─────────────────────────┐
│  Executor-Compose       │
│  /api/jobgroup/stop     │
└─────────────────────────┘
```

### 三种控制方式

| 操作 | 实现方式 | 说明 |
|------|---------|------|
| **暂停** | 更新数据库 `is_pause=1` | Executor 轮询检查，自动等待 |
| **恢复** | 更新数据库 `is_pause=0` | Executor 检测到恢复后继续执行 |
| **停止** | 调用 Executor API | 立即停止任务组执行 |

---

## 💻 Admin 端实现

### 1. 创建任务组控制服务

**文件**: `cc-job-admin/src/main/java/com/cc/job/admin/task/service/JobGroupControlService.java`

```java
package com.cc.job.admin.task.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.cc.job.xo.model.entity.JobInfo;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务组控制服务
 * 
 * @author xiaozhao
 */
@Service
public class JobGroupControlService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupControlService.class);
    
    private final JobInfoService jobInfoService;
    
    @Value("${cc-job.executor-compose.address:http://localhost:8500}")
    private String executorComposeAddress;
    
    public JobGroupControlService(JobInfoService jobInfoService) {
        this.jobInfoService = jobInfoService;
    }
    
    /**
     * 暂停任务（组）
     * 
     * @param jobId 任务ID
     * @return 是否成功
     */
    public boolean pauseJob(Long jobId) {
        logger.info("[JobGroupControl] 暂停任务 - jobId: {}", jobId);
        
        try {
            // 更新数据库：设置暂停标志
            boolean updated = jobInfoService.update(
                new LambdaUpdateWrapper<JobInfo>()
                    .eq(JobInfo::getId, jobId)
                    .set(JobInfo::getIsPause, 1)
            );
            
            if (updated) {
                logger.info("[JobGroupControl] 任务暂停成功 - jobId: {}", jobId);
                return true;
            } else {
                logger.warn("[JobGroupControl] 任务暂停失败，更新数据库失败 - jobId: {}", jobId);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("[JobGroupControl] 暂停任务异常 - jobId: {}", jobId, e);
            return false;
        }
    }
    
    /**
     * 恢复任务（组）
     * 
     * @param jobId 任务ID
     * @return 是否成功
     */
    public boolean resumeJob(Long jobId) {
        logger.info("[JobGroupControl] 恢复任务 - jobId: {}", jobId);
        
        try {
            // 更新数据库：取消暂停标志
            boolean updated = jobInfoService.update(
                new LambdaUpdateWrapper<JobInfo>()
                    .eq(JobInfo::getId, jobId)
                    .set(JobInfo::getIsPause, 0)
            );
            
            if (updated) {
                logger.info("[JobGroupControl] 任务恢复成功 - jobId: {}", jobId);
                return true;
            } else {
                logger.warn("[JobGroupControl] 任务恢复失败，更新数据库失败 - jobId: {}", jobId);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("[JobGroupControl] 恢复任务异常 - jobId: {}", jobId, e);
            return false;
        }
    }
    
    /**
     * 停止任务组执行
     * 
     * @param jobId 任务组ID
     * @param randomId 批次ID
     * @return 是否成功
     */
    public boolean stopJobGroup(Long jobId, String randomId) {
        logger.info("[JobGroupControl] 停止任务组 - jobId: {}, randomId: {}", jobId, randomId);
        
        try {
            // 1. 更新数据库状态（可选，用于记录）
            jobInfoService.update(
                new LambdaUpdateWrapper<JobInfo>()
                    .eq(JobInfo::getId, jobId)
                    .set(JobInfo::getIsPause, 1)
            );
            
            // 2. 调用 Executor-Compose API 停止任务组
            String url = executorComposeAddress + "/api/jobgroup/stop";
            
            Map<String, Object> params = new HashMap<>();
            params.put("jobId", jobId);
            params.put("randomId", randomId);
            
            HttpResponse response = HttpRequest.post(url)
                    .form(params)
                    .timeout(10000)
                    .execute();
            
            if (response.isOk()) {
                Map<String, Object> result = JSONUtil.toBean(response.body(), Map.class);
                Integer code = (Integer) result.get("code");
                
                if (code != null && code == 200) {
                    logger.info("[JobGroupControl] 任务组停止成功 - jobId: {}, randomId: {}", jobId, randomId);
                    return true;
                } else {
                    logger.warn("[JobGroupControl] 任务组停止失败 - jobId: {}, response: {}", 
                            jobId, response.body());
                    return false;
                }
            } else {
                logger.error("[JobGroupControl] 调用 Executor-Compose API 失败 - status: {}, body: {}", 
                        response.getStatus(), response.body());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("[JobGroupControl] 停止任务组异常 - jobId: {}, randomId: {}", jobId, randomId, e);
            return false;
        }
    }
    
    /**
     * 查询任务组状态
     * 
     * @param jobId 任务组ID
     * @param randomId 批次ID
     * @return 是否正在运行
     */
    public boolean isJobGroupRunning(Long jobId, String randomId) {
        try {
            String url = executorComposeAddress + "/api/jobgroup/status";
            
            HttpResponse response = HttpRequest.get(url)
                    .form("jobId", jobId)
                    .form("randomId", randomId)
                    .timeout(5000)
                    .execute();
            
            if (response.isOk()) {
                Map<String, Object> result = JSONUtil.toBean(response.body(), Map.class);
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                
                if (data != null) {
                    return (Boolean) data.get("isRunning");
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("[JobGroupControl] 查询任务组状态异常 - jobId: {}, randomId: {}", jobId, randomId, e);
            return false;
        }
    }
}
```

---

### 2. 创建控制器

**文件**: `cc-job-admin/src/main/java/com/cc/job/admin/task/controller/JobGroupControlController.java`

```java
package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.service.JobGroupControlService;
import com.cc.job.xo.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 任务组控制 API
 * 
 * @author xiaozhao
 */
@Tag(name = "任务组控制")
@RestController
@RequestMapping("/admin/jobgroup/control")
@RequiredArgsConstructor
public class JobGroupControlController {
    
    private final JobGroupControlService jobGroupControlService;
    
    /**
     * 暂停任务（组）
     */
    @Operation(summary = "暂停任务")
    @PostMapping("/pause/{jobId}")
    public Result<Void> pauseJob(@PathVariable Long jobId) {
        boolean success = jobGroupControlService.pauseJob(jobId);
        return success ? Result.success() : Result.error("暂停任务失败");
    }
    
    /**
     * 恢复任务（组）
     */
    @Operation(summary = "恢复任务")
    @PostMapping("/resume/{jobId}")
    public Result<Void> resumeJob(@PathVariable Long jobId) {
        boolean success = jobGroupControlService.resumeJob(jobId);
        return success ? Result.success() : Result.error("恢复任务失败");
    }
    
    /**
     * 停止任务组执行
     */
    @Operation(summary = "停止任务组")
    @PostMapping("/stop")
    public Result<Void> stopJobGroup(@RequestParam Long jobId, 
                                     @RequestParam String randomId) {
        boolean success = jobGroupControlService.stopJobGroup(jobId, randomId);
        return success ? Result.success() : Result.error("停止任务组失败");
    }
    
    /**
     * 查询任务组状态
     */
    @Operation(summary = "查询任务组状态")
    @GetMapping("/status")
    public Result<Boolean> getStatus(@RequestParam Long jobId, 
                                     @RequestParam String randomId) {
        boolean isRunning = jobGroupControlService.isJobGroupRunning(jobId, randomId);
        return Result.success(isRunning);
    }
}
```

---

### 3. 配置文件

**文件**: `cc-job-admin/src/main/resources/application.yml`

```yaml
cc-job:
  # Executor-Compose 地址配置
  executor-compose:
    address: http://localhost:8500
```

---

## 🔧 使用方法

### 1. 暂停任务组

**场景**: 任务组正在执行，希望暂停（不是停止）

**操作**:

```http
POST /admin/jobgroup/control/pause/1

Response:
{
  "code": 200,
  "message": "success"
}
```

**效果**:
- Admin 更新数据库 `job_info.is_pause = 1`
- Executor-Compose 在执行每个任务前检查暂停状态
- 如果检测到暂停，则等待恢复（最多等待超时时间）

---

### 2. 恢复任务组

**场景**: 任务组已暂停，希望恢复执行

**操作**:

```http
POST /admin/jobgroup/control/resume/1

Response:
{
  "code": 200,
  "message": "success"
}
```

**效果**:
- Admin 更新数据库 `job_info.is_pause = 0`
- Executor-Compose 检测到恢复后继续执行

---

### 3. 停止任务组

**场景**: 任务组正在执行，希望立即停止

**操作**:

```http
POST /admin/jobgroup/control/stop?jobId=1&randomId=xxx-xxx-xxx

Response:
{
  "code": 200,
  "message": "success"
}
```

**效果**:
- Admin 调用 Executor-Compose API
- Executor-Compose 调用 `Async.stopWork()` 停止所有任务
- 清理相关资源

---

### 4. 查询任务组状态

**场景**: 查询任务组是否正在运行

**操作**:

```http
GET /admin/jobgroup/control/status?jobId=1&randomId=xxx-xxx-xxx

Response:
{
  "code": 200,
  "message": "success",
  "data": true  // true=运行中，false=已完成或不存在
}
```

---

## 📊 三种控制方式对比

| 操作 | 实现方式 | 响应速度 | 任务状态 | 适用场景 |
|------|---------|---------|---------|---------|
| **暂停** | 更新数据库 | 慢（轮询检查） | 可恢复 | 临时暂停，稍后恢复 |
| **恢复** | 更新数据库 | 慢（轮询检查） | 继续执行 | 恢复暂停的任务 |
| **停止** | API 调用 | 快（立即停止） | 终止执行 | 彻底停止任务 |

---

## 🔄 完整流程示例

### 场景：任务组执行过程中需要临时暂停

```
1. 用户点击"暂停"按钮
   ↓
2. Admin 调用暂停 API
   POST /admin/jobgroup/control/pause/1
   ↓
3. Admin 更新数据库
   UPDATE job_info SET is_pause = 1 WHERE id = 1
   ↓
4. Executor-Compose 正在执行任务
   检测到 is_pause = 1
   ↓
5. Executor-Compose 进入等待状态
   循环检查 is_pause，每5秒检查一次
   ↓
6. 用户点击"恢复"按钮
   POST /admin/jobgroup/control/resume/1
   ↓
7. Admin 更新数据库
   UPDATE job_info SET is_pause = 0 WHERE id = 1
   ↓
8. Executor-Compose 检测到 is_pause = 0
   恢复执行任务
```

---

### 场景：任务组执行过程中需要彻底停止

```
1. 用户点击"停止"按钮
   ↓
2. Admin 调用停止 API
   POST /admin/jobgroup/control/stop?jobId=1&randomId=xxx
   ↓
3. Admin 调用 Executor-Compose API
   POST http://localhost:8500/api/jobgroup/stop
   ↓
4. Executor-Compose 接收停止请求
   调用 Async.stopWork()
   ↓
5. 所有任务被设置为失败状态
   清理资源
   ↓
6. 任务组执行终止
```

---

## ⚠️ 注意事项

### 1. 暂停 vs 停止

- **暂停**：任务进入等待状态，可以恢复
- **停止**：任务被强制终止，无法恢复

### 2. 暂停超时

暂停最多等待超时时间（默认5分钟或任务超时时间），超时后会自动恢复执行。

### 3. 网络问题

如果 Admin 无法访问 Executor-Compose，停止操作会失败。建议：
- 配置健康检查
- 使用重试机制
- 提供降级方案（只更新数据库）

### 4. 多实例部署

如果 Executor-Compose 部署了多个实例，需要：
- 通过负载均衡器统一入口
- 或者记录任务组在哪个实例上运行，直接调用该实例的 API

---

## 🚀 扩展功能

### 1. 批量停止

```java
@PostMapping("/stop/batch")
public Result<Void> stopJobGroups(@RequestBody List<StopRequest> requests) {
    for (StopRequest request : requests) {
        jobGroupControlService.stopJobGroup(request.getJobId(), request.getRandomId());
    }
    return Result.success();
}
```

### 2. 定时自动恢复

```java
@Scheduled(cron = "0 */5 * * * ?")
public void autoResumeJobs() {
    // 查询暂停超过一定时间的任务，自动恢复
    // ...
}
```

### 3. WebSocket 实时通知

```java
// 当任务组状态变化时，通过 WebSocket 推送给前端
webSocketService.sendMessage("jobgroup-status-change", statusData);
```

---

## 📚 相关文档

- [COMPLETE_IMPLEMENTATION.md](../cc-job/cc-job-executor-compose/COMPLETE_IMPLEMENTATION.md)
- [COMPLETE_IMPLEMENTATION_SUMMARY.md](./COMPLETE_IMPLEMENTATION_SUMMARY.md)
- [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)

---

**最后更新**: 2025-11-30  
**作者**: CC-ETL Team
